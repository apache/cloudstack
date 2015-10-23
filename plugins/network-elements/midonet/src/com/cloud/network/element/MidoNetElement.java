/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package com.cloud.network.element;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import javax.ejb.Local;
import javax.inject.Inject;
import javax.naming.ConfigurationException;
import javax.ws.rs.core.MultivaluedMap;

import org.apache.cloudstack.framework.config.dao.ConfigurationDao;
import org.apache.log4j.Logger;
import org.midonet.client.MidonetApi;
import org.midonet.client.dto.DtoRule;
import org.midonet.client.dto.DtoRule.DtoRange;
import org.midonet.client.exception.HttpInternalServerError;
import org.midonet.client.resource.Bridge;
import org.midonet.client.resource.BridgePort;
import org.midonet.client.resource.DhcpHost;
import org.midonet.client.resource.DhcpSubnet;
import org.midonet.client.resource.Port;
import org.midonet.client.resource.ResourceCollection;
import org.midonet.client.resource.Route;
import org.midonet.client.resource.Router;
import org.midonet.client.resource.RouterPort;
import org.midonet.client.resource.Rule;
import org.midonet.client.resource.RuleChain;
import org.springframework.stereotype.Component;

import com.cloud.agent.api.to.FirewallRuleTO;
import com.cloud.agent.api.to.PortForwardingRuleTO;
import com.cloud.configuration.Config;
import com.cloud.deploy.DeployDestination;
import com.cloud.exception.ConcurrentOperationException;
import com.cloud.exception.InsufficientCapacityException;
import com.cloud.exception.ResourceUnavailableException;
import com.cloud.network.IpAddress;
import com.cloud.network.Network;
import com.cloud.network.Network.Capability;
import com.cloud.network.Network.Provider;
import com.cloud.network.Network.Service;
import com.cloud.network.NetworkModel;
import com.cloud.network.Networks;
import com.cloud.network.PhysicalNetworkServiceProvider;
import com.cloud.network.PublicIpAddress;
import com.cloud.network.rules.FirewallRule;
import com.cloud.network.rules.PortForwardingRule;
import com.cloud.network.rules.StaticNat;
import com.cloud.network.vpc.VpcManager;
import com.cloud.offering.NetworkOffering;
import com.cloud.user.AccountManager;
import com.cloud.user.AccountVO;
import com.cloud.user.dao.AccountDao;
import com.cloud.utils.Pair;
import com.cloud.utils.component.AdapterBase;
import com.cloud.utils.component.PluggableService;
import com.cloud.utils.net.NetUtils;
import com.cloud.vm.NicProfile;
import com.cloud.vm.NicVO;
import com.cloud.vm.ReservationContext;
import com.cloud.vm.VirtualMachine;
import com.cloud.vm.VirtualMachineProfile;
import com.cloud.vm.dao.NicDao;
import com.sun.jersey.core.util.MultivaluedMapImpl;

@Component
@Local(value = {NetworkElement.class, ConnectivityProvider.class, FirewallServiceProvider.class, SourceNatServiceProvider.class, DhcpServiceProvider.class,
    StaticNatServiceProvider.class, PortForwardingServiceProvider.class, IpDeployer.class})
public class MidoNetElement extends AdapterBase implements ConnectivityProvider, DhcpServiceProvider, SourceNatServiceProvider, StaticNatServiceProvider, IpDeployer,
        PortForwardingServiceProvider, FirewallServiceProvider, PluggableService {

    private static final Logger s_logger = Logger.getLogger(MidoNetElement.class);

    private static final Map<Service, Map<Capability, String>> capabilities = setCapabilities();

    protected UUID _providerRouterId = null;

    protected MidonetApi api;

    private static final Provider MidoNet = new Provider("MidoNet", false);

    public enum RuleChainCode {
        TR_PRE, TR_PRENAT, TR_PREFILTER, TR_POST, ACL_INGRESS, ACL_EGRESS
    }

    @Inject
    ConfigurationDao _configDao;
    @Inject
    protected NicDao _nicDao;
    @Inject
    NetworkModel _networkModel;
    @Inject
    VpcManager _vpcMgr;
    @Inject
    AccountManager _accountMgr;
    @Inject
    AccountDao _accountDao;

    public void setMidonetApi(MidonetApi api) {
        this.api = api;
    }

    public void setAccountDao(AccountDao aDao) {
        this._accountDao = aDao;
    }

    @Override
    public Map<Service, Map<Capability, String>> getCapabilities() {
        return capabilities;
    }

    @Override
    public boolean configure(String name, Map<String, Object> params) throws ConfigurationException {
        super.configure(name, params);

        String routerIdValue = _configDao.getValue(Config.MidoNetProviderRouterId.key());
        if (routerIdValue != null)
            _providerRouterId = UUID.fromString(routerIdValue);

        String value = _configDao.getValue(Config.MidoNetAPIServerAddress.key());

        if (value == null) {
            throw new ConfigurationException("Could not find midonet API location in config");
        }

        if (this.api == null) {
            s_logger.info("midonet API server address is  " + value);
            setMidonetApi(new MidonetApi(value));
            this.api.enableLogging();
        }

        return true;
    }

    public boolean midoInNetwork(Network network) {
        if ((network.getTrafficType() == Networks.TrafficType.Public) && (network.getBroadcastDomainType() == Networks.BroadcastDomainType.Mido)) {
            return true;
        }
        if ((network.getTrafficType() == Networks.TrafficType.Guest) && (network.getBroadcastDomainType() == Networks.BroadcastDomainType.Mido)) {
            return true;
        }
        return false;
    }

    protected boolean canHandle(Network network, Service service) {
        Long physicalNetworkId = _networkModel.getPhysicalNetworkId(network);
        if (physicalNetworkId == null) {
            return false;
        }

        if (!_networkModel.isProviderEnabledInPhysicalNetwork(physicalNetworkId, getProvider().getName())) {
            return false;
        }

        if (service == null) {
            if (!_networkModel.isProviderForNetwork(getProvider(), network.getId())) {
                s_logger.trace("Element " + getProvider().getName() + " is not a provider for the network " + network);
                return false;
            }
        } else {
            if (!_networkModel.isProviderSupportServiceInNetwork(network.getId(), service, getProvider())) {
                s_logger.trace("Element " + getProvider().getName() + " doesn't support service " + service.getName() + " in the network " + network);
                return false;
            }
        }

        return true;
    }

    public void applySourceNat(Router tenantRouter, Router providerRouter, RouterPort tenantUplink, RouterPort providerDownlink, RuleChain pre, RuleChain post,
        PublicIpAddress addr) {

        boolean needAdd = true;
        String SNtag = "/SourceNat";
        String SNkey = "CS_nat";

        String snatIp = addr.getAddress().addr();

        String CsDesc = snatIp + SNtag;

        //determine, by use of the properties, if we already
        //added this rule. If we did, then we can skip it
        //by setting needAdd = false
        for (Rule rule : pre.getRules()) {
            Map<String, String> props = rule.getProperties();
            if (props == null) {
                continue;
            }
            String snatTag = props.get(SNkey);
            if (snatTag == null) {
                continue;
            }

            if (!snatTag.equals(CsDesc)) {
                continue;
            } else {
                needAdd = false;
                break;
            }
        }

        if (needAdd == false) {
            //we found that this rule exists already,
            // so lets skip adding it
            return;
        }

        Map<String, String> ruleProps = new HashMap<String, String>();
        ruleProps.put(SNkey, CsDesc);

        DtoRule.DtoNatTarget[] targets = new DtoRule.DtoNatTarget[] {new DtoRule.DtoNatTarget(snatIp, snatIp, 1, 65535)};

        // Set inbound (reverse SNAT) rule
        pre.addRule()
            .type(DtoRule.RevSNAT)
            .flowAction(DtoRule.Accept)
            .nwDstAddress(snatIp)
            .nwDstLength(32)
            .inPorts(new UUID[] {tenantUplink.getId()})
            .position(1)
            .properties(ruleProps)
            .create();

        // Set outbound (SNAT) rule
        post.addRule()
            .type(DtoRule.SNAT)
            .flowAction(DtoRule.Accept)
            .outPorts(new UUID[] {tenantUplink.getId()})
            .natTargets(targets)
            .position(1)
            .properties(ruleProps)
            .create();

        // Set up default route from tenant router to provider router
        tenantRouter.addRoute()
            .type("Normal")
            .weight(100)
            .srcNetworkAddr("0.0.0.0")
            .srcNetworkLength(0)
            .dstNetworkAddr("0.0.0.0")
            .dstNetworkLength(0)
            .nextHopPort(tenantUplink.getId())
            .create();

        // Set routes for traffic to the SNAT IP to come back from provider router
        providerRouter.addRoute()
            .type("Normal")
            .weight(100)
            .srcNetworkAddr("0.0.0.0")
            .srcNetworkLength(0)
            .dstNetworkAddr(snatIp)
            .dstNetworkLength(32)
            .nextHopPort(providerDownlink.getId())
            .create();

        // Default rule to accept traffic that has been DNATed
        post.addRule().type(DtoRule.RevDNAT).flowAction(DtoRule.Accept).create();
    }

    public String getAccountUuid(Network network) {
        AccountVO acc = _accountDao.findById(network.getAccountId());
        return acc.getUuid();
    }

    public boolean associatePublicIP(Network network, final List<? extends PublicIpAddress> ipAddress) throws ResourceUnavailableException {

        s_logger.debug("associatePublicIP called with network: " + network.toString());
        /*
         * Get Mido Router for this network and set source rules
         * These should only be allocated inside the for loop, because
         * this function could be called as a part of network cleanup. In
         * that case, we do not want to recreate the guest network or
         * any ports.
         */
        boolean resources = false;
        Router tenantRouter = null;
        Router providerRouter = null;
        RouterPort[] ports = null;

        RouterPort tenantUplink = null;
        RouterPort providerDownlink = null;

        RuleChain preNat = null;
        RuleChain post = null;
        String accountIdStr = null;
        String routerName = null;

        // Set Source NAT rules on router
        for (PublicIpAddress ip : ipAddress) {
            // ip is the external one we sourcenat to
            if (ip.isSourceNat()) {
                if (resources == false) {
                    tenantRouter = getOrCreateGuestNetworkRouter(network);
                    providerRouter = api.getRouter(_providerRouterId);
                    ports = getOrCreateProviderRouterPorts(tenantRouter, providerRouter);

                    tenantUplink = ports[0];
                    providerDownlink = ports[1];

                    accountIdStr = getAccountUuid(network);
                    boolean isVpc = getIsVpc(network);
                    long id = getRouterId(network, isVpc);
                    routerName = getRouterName(isVpc, id);

                    preNat = getChain(accountIdStr, routerName, RuleChainCode.TR_PRENAT);
                    post = api.getChain(tenantRouter.getOutboundFilterId());
                    resources = true;
                }

                applySourceNat(tenantRouter, providerRouter,    // Routers
                    tenantUplink, providerDownlink,  // Ports
                    preNat, post,                       // Chains
                    ip);                             // The IP
            }
        }

        return true;
    }

    /**
     * From interface IpDeployer
     *
     * @param network
     * @param ipAddress
     * @param services
     * @return
     * @throws ResourceUnavailableException
     */
    @Override
    public boolean applyIps(Network network, List<? extends PublicIpAddress> ipAddress, Set<Service> services) throws ResourceUnavailableException {

        s_logger.debug("applyIps called with network: " + network.toString());
        if (!this.midoInNetwork(network)) {
            return false;
        }

        boolean canHandle = true;
        for (Service service : services) {
            if (!canHandle(network, service)) {
                canHandle = false;
                break;
            }
        }
        if (canHandle) {
            return associatePublicIP(network, ipAddress);
        } else {
            return false;
        }
    }

    /**
     * From interface SourceNatServiceProvider
     */
    @Override
    public IpDeployer getIpDeployer(Network network) {
        s_logger.debug("getIpDeployer called with network " + network.toString());
        return this;
    }

    /**
     * From interface DHCPServiceProvider
     */
    @Override
    public boolean addDhcpEntry(Network network, NicProfile nic, VirtualMachineProfile vm, DeployDestination dest, ReservationContext context)
        throws ConcurrentOperationException, InsufficientCapacityException, ResourceUnavailableException {

        s_logger.debug("addDhcpEntry called with network: " + network.toString() + " nic: " + nic.toString() + " vm: " + vm.toString());
        if (!this.midoInNetwork(network)) {
            return false;
        }
        if (vm.getType() != VirtualMachine.Type.User) {
            return false;
        }

        // Get MidoNet bridge
        Bridge netBridge = getOrCreateNetworkBridge(network);

        // On bridge, get DHCP subnet (ensure it exists)
        ResourceCollection res = netBridge.getDhcpSubnets();

        DhcpSubnet sub = null;

        if (!res.isEmpty()) {
            sub = (DhcpSubnet)res.get(0);
        } else {
            Pair<String, Integer> cidrInfo = NetUtils.getCidr(network.getCidr());
            sub = netBridge.addDhcpSubnet();

            sub.subnetLength(cidrInfo.second());
            sub.subnetPrefix(cidrInfo.first());
            sub.defaultGateway(network.getGateway());
            List<String> dcs = new ArrayList<String>();
            dcs.add(dest.getDataCenter().getDns1());
            sub.dnsServerAddrs(dcs);

            sub.create();
        }

        // On DHCP subnet, add host using host details
        if (sub == null) {
            s_logger.error("Failed to create DHCP subnet on Midonet bridge");
            return false;
        } else {
            // Check if the host already exists - we may just be restarting an existing VM
            boolean isNewDhcpHost = true;

            for (DhcpHost dhcpHost : sub.getDhcpHosts()) {
                if (dhcpHost.getIpAddr().equals(nic.getIPv4Address())) {
                    isNewDhcpHost = false;
                    break;
                }
            }

            if (isNewDhcpHost) {
                DhcpHost host = sub.addDhcpHost();
                host.ipAddr(nic.getIPv4Address());
                host.macAddr(nic.getMacAddress());
                // This only sets the cloudstack internal name
                host.name(vm.getHostName());

                host.create();
            }
        }

        return true;
    }

    @Override
    public boolean configDhcpSupportForSubnet(Network network, NicProfile nic, VirtualMachineProfile vm, DeployDestination dest, ReservationContext context)
        throws ConcurrentOperationException, InsufficientCapacityException, ResourceUnavailableException {
        return false;  //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public boolean removeDhcpSupportForSubnet(Network network) {
        return false;  //To change body of implemented methods use File | Settings | File Templates.
    }

    private void removeMidonetStaticNAT(RuleChain preFilter, RuleChain preNat, RuleChain postNat, String floatingIp, String fixedIp, Router providerRouter) {

        // Delete filter (firewall) rules for this IP
        for (Rule rule : preFilter.getRules()) {
            String destAddr = rule.getNwDstAddress();
            if (destAddr != null && destAddr.equals(floatingIp)) {
                rule.delete();
            }
        }

        // Delete DNAT rules for this IP
        for (Rule rule : preNat.getRules()) {
            String destAddr = rule.getNwDstAddress();
            if (destAddr != null && destAddr.equals(floatingIp)) {
                rule.delete();
            }
        }
        // Delete SNAT rules for this IP
        for (Rule rule : postNat.getRules()) {
            String srcAddr = rule.getNwSrcAddress();
            if (srcAddr != null && srcAddr.equals(fixedIp)) {
                rule.delete();
            }
        }

        //we also created a route to go with this rule. That needs to be
        //deleted as well.
        for (Route route : providerRouter.getRoutes(new MultivaluedMapImpl())) {
            String routeDstAddr = route.getDstNetworkAddr();
            if (routeDstAddr != null && routeDstAddr.equals(floatingIp)) {
                route.delete();
            }
        }
    }

    private void addMidonetStaticNAT(RuleChain preFilter, RuleChain preNat, RuleChain postNat, String floatingIp, String fixedIp, RouterPort tenantUplink,
        RouterPort providerDownlink, Router providerRouter, Network network) {

        DtoRule.DtoNatTarget[] preTargets = new DtoRule.DtoNatTarget[] {new DtoRule.DtoNatTarget(fixedIp, fixedIp, 0, 0)};

        // Set inbound filter rule (allow return traffic to this IP)
        //     Implemented as "jump to NAT chain" instead of ACCEPT;
        //     this is to enforce that filter / firewall rules are evaluated
        //     before NAT rules.
        preFilter.addRule().type(DtoRule.Jump).jumpChainId(preNat.getId()).nwDstAddress(floatingIp).nwDstLength(32).matchReturnFlow(true).position(1).create();

        // Allow ICMP replies (ICMP type 0, code 0 is ICMP reply)
        preFilter.addRule()
            .type(DtoRule.Jump)
            .jumpChainId(preNat.getId())
            .nwDstAddress(floatingIp)
            .nwDstLength(32)
            .nwProto(SimpleFirewallRule.stringToProtocolNumber("icmp"))
            .tpSrc(new DtoRange<Integer>(0, 0))
            .tpDst(new DtoRange<Integer>(0, 0))
            .position(2)
            .create();

        // We only want to set the default DROP rule for static NAT if
        // Firewall is handled by the Midonet plugin.
        // Set inbound filter rule (drop all traffic to this IP)
        if (canHandle(network, Service.Firewall)) {
            preFilter.addRule().type(DtoRule.Drop).nwDstAddress(floatingIp).nwDstLength(32).position(3).create();
        }

        // Set inbound (DNAT) rule
        preNat.addRule()
            .type(DtoRule.DNAT)
            .flowAction(DtoRule.Accept)
            .nwDstAddress(floatingIp)
            .nwDstLength(32)
            .inPorts(new UUID[] {tenantUplink.getId()})
            .natTargets(preTargets)
            .position(1)
            .create();

        DtoRule.DtoNatTarget[] postTargets = new DtoRule.DtoNatTarget[] {new DtoRule.DtoNatTarget(floatingIp, floatingIp, 0, 0)};

        // Set outbound (SNAT) rule
        //    Match forward flow so that return traffic will be recognized
        postNat.addRule()
            .type(DtoRule.SNAT)
            .flowAction(DtoRule.Accept)
            .matchForwardFlow(true)
            .nwSrcAddress(fixedIp)
            .nwSrcLength(32)
            .outPorts(new UUID[] {tenantUplink.getId()})
            .natTargets(postTargets)
            .position(1)
            .create();

        // Set outbound (SNAT) rule
        //    Match return flow to also allow out traffic which was marked as forward flow on way in
        postNat.addRule()
            .type(DtoRule.SNAT)
            .flowAction(DtoRule.Accept)
            .matchReturnFlow(true)
            .nwSrcAddress(fixedIp)
            .nwSrcLength(32)
            .outPorts(new UUID[] {tenantUplink.getId()})
            .natTargets(postTargets)
            .position(2)
            .create();

        // Set routes for traffic to the SNAT IP to come back from provider router
        providerRouter.addRoute()
            .type("Normal")
            .weight(100)
            .srcNetworkAddr("0.0.0.0")
            .srcNetworkLength(0)
            .dstNetworkAddr(floatingIp)
            .dstNetworkLength(32)
            .nextHopPort(providerDownlink.getId())
            .create();
    }

    /**
     * From interface StaticNatServiceProvider
     */
    @Override
    public boolean applyStaticNats(Network network, List<? extends StaticNat> rules) throws ResourceUnavailableException {
        s_logger.debug("applyStaticNats called with network: " + network.toString());
        if (!midoInNetwork(network)) {
            return false;
        }
        if (!canHandle(network, Service.StaticNat)) {
            return false;
        }

        boolean resources = false;
        Router tenantRouter = null;
        Router providerRouter = null;

        RouterPort[] ports = null;

        RouterPort tenantUplink = null;
        RouterPort providerDownlink = null;

        RuleChain preFilter = null;
        RuleChain preNat = null;
        RuleChain post = null;

        String accountIdStr = getAccountUuid(network);
        String networkUUIDStr = String.valueOf(network.getId());

        for (StaticNat rule : rules) {
            IpAddress sourceIp = _networkModel.getIp(rule.getSourceIpAddressId());
            String sourceIpAddr = sourceIp.getAddress().addr();

            if (resources == false) {
                tenantRouter = getOrCreateGuestNetworkRouter(network);
                providerRouter = api.getRouter(_providerRouterId);

                ports = getOrCreateProviderRouterPorts(tenantRouter, providerRouter);

                tenantUplink = ports[0];
                providerDownlink = ports[1];

                boolean isVpc = getIsVpc(network);
                long id = getRouterId(network, isVpc);
                String routerName = getRouterName(isVpc, id);

                preFilter = getChain(accountIdStr, routerName, RuleChainCode.TR_PREFILTER);
                preNat = getChain(accountIdStr, routerName, RuleChainCode.TR_PRENAT);
                post = api.getChain(tenantRouter.getOutboundFilterId());
                resources = true;
            }

            if (rule.isForRevoke()) {
                removeMidonetStaticNAT(preFilter, preNat, post, sourceIpAddr, rule.getDestIpAddress(), providerRouter);
            } else {
                addMidonetStaticNAT(preFilter, preNat, post, sourceIpAddr, rule.getDestIpAddress(), tenantUplink, providerDownlink, providerRouter, network);
            }
        }

        return true;
    }

    @Override
    public boolean applyFWRules(Network config, List<? extends FirewallRule> rulesToApply) throws ResourceUnavailableException {
        if (!midoInNetwork(config)) {
            return false;
        }
        if (canHandle(config, Service.Firewall)) {
            String accountIdStr = getAccountUuid(config);
            String networkUUIDStr = String.valueOf(config.getId());
            RuleChain preFilter = getChain(accountIdStr, networkUUIDStr, RuleChainCode.TR_PREFILTER);
            RuleChain preNat = getChain(accountIdStr, networkUUIDStr, RuleChainCode.TR_PRENAT);

            // Create a map of Rule description -> Rule for quicker lookups
            Map<String, Rule> existingRules = new HashMap<String, Rule>();

            for (Rule existingRule : preFilter.getRules()) {
                // The "whitelist" rules we're interested in are the Jump rules where src address is specified
                if (existingRule.getType().equals(DtoRule.Jump) && existingRule.getNwSrcAddress() != null) {
                    String ruleString = new SimpleFirewallRule(existingRule).toStringArray()[0];
                    existingRules.put(ruleString, existingRule);
                }
            }

            for (FirewallRule rule : rulesToApply) {
                if (rule.getState() == FirewallRule.State.Revoke || rule.getState() == FirewallRule.State.Add) {
                    IpAddress dstIp = _networkModel.getIp(rule.getSourceIpAddressId());
                    FirewallRuleTO ruleTO = new FirewallRuleTO(rule, null, dstIp.getAddress().addr());

                    // Convert to string representation
                    SimpleFirewallRule fwRule = new SimpleFirewallRule(ruleTO);
                    String[] ruleStrings = fwRule.toStringArray();

                    if (rule.getState() == FirewallRule.State.Revoke) {
                        // Lookup in existingRules, delete if present
                        for (String revokeRuleString : ruleStrings) {
                            Rule foundRule = existingRules.get(revokeRuleString);
                            if (foundRule != null) {
                                foundRule.delete();
                            }
                        }
                    } else if (rule.getState() == FirewallRule.State.Add) {
                        // Lookup in existingRules, add if not present
                        for (int i = 0; i < ruleStrings.length; i++) {
                            String ruleString = ruleStrings[i];
                            Rule foundRule = existingRules.get(ruleString);
                            if (foundRule == null) {
                                // Get the cidr for the related entry in the Source Cidrs list
                                String relatedCidr = fwRule.sourceCidrs.get(i);
                                Pair<String, Integer> cidrParts = NetUtils.getCidr(relatedCidr);

                                // Create rule with correct proto, cidr, ACCEPT, dst IP
                                Rule toApply =
                                    preFilter.addRule()
                                        .type(DtoRule.Jump)
                                        .jumpChainId(preNat.getId())
                                        .position(1)
                                        .nwSrcAddress(cidrParts.first())
                                        .nwSrcLength(cidrParts.second())
                                        .nwDstAddress(ruleTO.getSrcIp())
                                        .nwDstLength(32)
                                        .nwProto(SimpleFirewallRule.stringToProtocolNumber(rule.getProtocol()));

                                if (rule.getProtocol().equals("icmp")) {
                                    // ICMP rules - reuse port fields
                                    // (-1, -1) means "allow all ICMP", so we don't set tpSrc / tpDst
                                    if (fwRule.icmpType != -1 | fwRule.icmpCode != -1) {
                                        toApply.tpSrc(new DtoRange(fwRule.icmpType, fwRule.icmpType)).tpDst(new DtoRange(fwRule.icmpCode, fwRule.icmpCode));
                                    }
                                } else {
                                    toApply.tpDst(new DtoRange(fwRule.dstPortStart, fwRule.dstPortEnd));
                                }

                                toApply.create();
                            }
                        }
                    }
                }
            }
            return true;
        } else {
            return true;
        }
    }

    @Override
    public Provider getProvider() {
        return MidoNet;
    }

    @Override
    public boolean implement(Network network, NetworkOffering offering, DeployDestination dest, ReservationContext context) throws ConcurrentOperationException,
        ResourceUnavailableException, InsufficientCapacityException {
        s_logger.debug("implement called with network: " + network.toString());
        if (!midoInNetwork(network)) {
            return false;
        }

        if (network.getTrafficType() == Networks.TrafficType.Guest) {
            // Create the Midonet bridge for this network
            Bridge netBridge = getOrCreateNetworkBridge(network);
            Router tenantRouter = getOrCreateGuestNetworkRouter(network);

            // connect router and bridge
            ensureBridgeConnectedToRouter(network, netBridge, tenantRouter);
        }

        return true;
    }

    @Override
    public boolean prepare(Network network, NicProfile nic, VirtualMachineProfile vm, DeployDestination dest, ReservationContext context)
        throws ConcurrentOperationException, ResourceUnavailableException, InsufficientCapacityException {
        s_logger.debug("prepare called with network: " + network.toString() + " nic: " + nic.toString() + " vm: " + vm.toString());
        if (!midoInNetwork(network)) {
            return false;
        }

        if (nic.getTrafficType() == Networks.TrafficType.Guest && !canHandle(network, Service.StaticNat)) {
            return false;
        }

        if (nic.getTrafficType() == Networks.TrafficType.Guest || nic.getTrafficType() == Networks.TrafficType.Public &&
            nic.getBroadcastType() == Networks.BroadcastDomainType.Mido) {
            Bridge netBridge = getOrCreateNetworkBridge(network);
            if (nic.getTrafficType() == Networks.TrafficType.Public && vm.getVirtualMachine().getType() != VirtualMachine.Type.DomainRouter) {
                // Get provider router
                Router providerRouter = api.getRouter(_providerRouterId);

                Port[] ports = getOrCreatePublicBridgePorts(nic, netBridge, providerRouter);

                RouterPort providerDownlink = (RouterPort)ports[1];

                // Set route from router to bridge for this particular IP. Prepare
                // is called in both starting a new VM and restarting a VM, so the
                // NIC may
                boolean routeExists = false;
                for (Route route : providerRouter.getRoutes(new MultivaluedMapImpl())) {
                    String ip4 = route.getDstNetworkAddr();
                    if (ip4 != null && ip4.equals(nic.getIPv4Address())) {
                        routeExists = true;
                        break;
                    }
                }

                if (!routeExists) {
                    providerRouter.addRoute()
                        .type("Normal")
                        .weight(100)
                        .srcNetworkAddr("0.0.0.0")
                        .srcNetworkLength(0)
                        .dstNetworkAddr(nic.getIPv4Address())
                        .dstNetworkLength(32)
                        .nextHopPort(providerDownlink.getId())
                        .nextHopGateway(null)
                        .create();
                }
            }

            // Add port on bridge
            BridgePort newPort = netBridge.addExteriorPort().create(); // returns wrapper resource of port

            // Set MidoNet port VIF ID to UUID of nic
            UUID nicUUID = getNicUUID(nic);
            newPort.vifId(nicUUID).update();
        }

        return true;
    }

    @Override
    public boolean release(Network network, NicProfile nic, VirtualMachineProfile vm, ReservationContext context) throws ConcurrentOperationException,
        ResourceUnavailableException {
        s_logger.debug("release called with network: " + network.toString() + " nic: " + nic.toString() + " vm: " + vm.toString());
        if (!midoInNetwork(network)) {
            return false;
        }

        UUID nicUUID = getNicUUID(nic);
        if (nic.getTrafficType() == Networks.TrafficType.Guest ||
            (nic.getTrafficType() == Networks.TrafficType.Public && nic.getBroadcastType() == Networks.BroadcastDomainType.Mido)) {
            // Seems like a good place to remove the port in midonet
            Bridge netBridge = getOrCreateNetworkBridge(network);

            Router providerRouter = api.getRouter(_providerRouterId);

            //remove the routes associated with this IP address
            for (Route route : providerRouter.getRoutes(new MultivaluedMapImpl())) {
                String routeDstAddr = route.getDstNetworkAddr();
                if (routeDstAddr != null && routeDstAddr.equals(nic.getIPv4Address())) {
                    route.delete();
                }
            }

            for (BridgePort p : netBridge.getPorts()) {
                UUID vifID = p.getVifId();
                if (vifID != null && vifID.equals(nicUUID)) {
                    // This is the MidoNet port which corresponds to the NIC we are releasing

                    // Set VIF ID to null
                    p.vifId(null).update();

                    // Delete port
                    p.delete();
                }
            }
        }

        return true;
    }

    @Override
    public boolean shutdown(Network network, ReservationContext context, boolean cleanup) throws ConcurrentOperationException, ResourceUnavailableException {
        s_logger.debug("shutdown called with network: " + network.toString());
        if (!midoInNetwork(network)) {
            return false;
        }

        // Find Mido API server, remove ports from this network's bridge, remove bridge itself
        deleteNetworkBridges(network);
        if (network.getVpcId() == null) {
            deleteGuestNetworkRouters(network);
        }

        return true;
    }

    @Override
    public boolean destroy(Network network, ReservationContext context) throws ConcurrentOperationException, ResourceUnavailableException {
        s_logger.debug("destroy called with network: " + network.toString());
        if (!midoInNetwork(network)) {
            return false;
        }

        deleteNetworkBridges(network);
        // if This is part of a VPC, then we do not want to delete the router.
        // we only delete the router when the VPC is destroyed
        if (network.getVpcId() == null) {
            deleteGuestNetworkRouters(network);
        }

        return true;
    }

    @Override
    public boolean isReady(PhysicalNetworkServiceProvider provider) {
        // We are always ready.
        return true;
    }

    @Override
    public boolean shutdownProviderInstances(PhysicalNetworkServiceProvider provider, ReservationContext context) throws ConcurrentOperationException,
        ResourceUnavailableException {
        // Nothing to do here because the cleanup of the networks themselves clean up the resources.
        return true;
    }

    @Override
    public boolean canEnableIndividualServices() {
        // We can enable individual services, though this is still subject to
        // "VerifyServicesCombination
        return true;
    }

    @Override
    public boolean verifyServicesCombination(Set<Service> services) {
        if (services.contains(Service.Vpn) || services.contains(Service.Dns) || services.contains(Service.Lb) || services.contains(Service.UserData) ||
            services.contains(Service.SecurityGroup) || services.contains(Service.NetworkACL)) {
            // We don't implement any of these services, and we don't
            // want anyone else to do it for us. So if these services
            // exist, we can't handle it.
            return false;
        }
        return true;
    }

    @Override
    public boolean applyPFRules(Network network, List<PortForwardingRule> rules) throws ResourceUnavailableException {
        s_logger.debug("applyPFRules called with network " + network.toString());
        if (!midoInNetwork(network)) {
            return false;
        }
        if (!canHandle(network, Service.PortForwarding)) {
            return false;
        }

        String accountIdStr = getAccountUuid(network);
        String networkUUIDStr = String.valueOf(network.getId());
        RuleChain preNat = getChain(accountIdStr, networkUUIDStr, RuleChainCode.TR_PRENAT);
        RuleChain postNat = getChain(accountIdStr, networkUUIDStr, RuleChainCode.TR_POST);
        RuleChain preFilter = getChain(accountIdStr, networkUUIDStr, RuleChainCode.TR_PREFILTER);
        Router providerRouter = api.getRouter(_providerRouterId);
        Router tenantRouter = getOrCreateGuestNetworkRouter(network);
        RouterPort[] ports = getOrCreateProviderRouterPorts(tenantRouter, providerRouter);
        RouterPort providerDownlink = ports[1];

        // Rules in the preNat table
        Map<String, Rule> existingPreNatRules = new HashMap<String, Rule>();
        for (Rule existingRule : preNat.getRules()) {
            // The "port forwarding" rules we're interested in are dnat rules where src / dst ports are specified
            if (existingRule.getType().equals(DtoRule.DNAT) && existingRule.getTpDst() != null) {
                String ruleString = new SimpleFirewallRule(existingRule).toStringArray()[0];
                existingPreNatRules.put(ruleString, existingRule);
            }
        }

        /*
         * Counts of rules associated with an IP address. Use this to check
         * how many rules we have of a given IP address. When it reaches 0,
         * we can delete the route associated with it.
         */
        Map<String, Integer> ipRuleCounts = new HashMap<String, Integer>();
        for (Rule rule : preNat.getRules()) {
            String ip = rule.getNwDstAddress();
            if (ip != null && rule.getNwDstLength() == 32) {
                if (ipRuleCounts.containsKey(ip)) {
                    ipRuleCounts.put(ip, new Integer(ipRuleCounts.get(ip).intValue() + 1));
                } else {
                    ipRuleCounts.put(ip, new Integer(1));
                }
            }
        }

        /*
         * Routes associated with IP. When we delete all the rules associated
         * with a given IP, we can delete the route associated with it.
         */
        Map<String, Route> routes = new HashMap<String, Route>();
        for (Route route : providerRouter.getRoutes(new MultivaluedMapImpl())) {
            String ip = route.getDstNetworkAddr();
            if (ip != null && route.getDstNetworkLength() == 32) {
                routes.put(ip, route);
            }
        }

        for (PortForwardingRule rule : rules) {
            IpAddress dstIp = _networkModel.getIp(rule.getSourceIpAddressId());
            PortForwardingRuleTO ruleTO = new PortForwardingRuleTO(rule, null, dstIp.getAddress().addr());
            SimpleFirewallRule fwRule = new SimpleFirewallRule(ruleTO);
            String[] ruleStrings = fwRule.toStringArray();

            if (rule.getState() == FirewallRule.State.Revoke) {
                /*
                 * Lookup in existingRules, delete if present
                 * We need to delete from both the preNat table and the
                 * postNat table.
                 */
                for (String revokeRuleString : ruleStrings) {
                    Rule foundPreNatRule = existingPreNatRules.get(revokeRuleString);
                    if (foundPreNatRule != null) {
                        String ip = foundPreNatRule.getNwDstAddress();
                        // is this the last rule associated with this IP?
                        Integer cnt = ipRuleCounts.get(ip);
                        if (cnt != null) {
                            if (cnt == 1) {
                                ipRuleCounts.remove(ip);
                                // no more rules for this IP. delete the route.
                                Route route = routes.remove(ip);
                                route.delete();
                            } else {
                                ipRuleCounts.put(ip, new Integer(ipRuleCounts.get(ip).intValue() - 1));
                            }
                        }
                        foundPreNatRule.delete();
                    }
                }
            } else if (rule.getState() == FirewallRule.State.Add) {
                for (int i = 0; i < ruleStrings.length; i++) {
                    String ruleString = ruleStrings[i];
                    Rule foundRule = existingPreNatRules.get(ruleString);
                    if (foundRule == null) {

                        String vmIp = ruleTO.getDstIp();
                        String publicIp = dstIp.getAddress().addr();
                        int privPortStart = ruleTO.getDstPortRange()[0];
                        int privPortEnd = ruleTO.getDstPortRange()[1];
                        int pubPortStart = ruleTO.getSrcPortRange()[0];
                        int pubPortEnd = ruleTO.getSrcPortRange()[1];

                        DtoRule.DtoNatTarget[] preTargets = new DtoRule.DtoNatTarget[] {new DtoRule.DtoNatTarget(vmIp, vmIp, privPortStart, privPortEnd)};

                        Rule preNatRule =
                            preNat.addRule()
                                .type(DtoRule.DNAT)
                                .flowAction(DtoRule.Accept)
                                .nwDstAddress(publicIp)
                                .nwDstLength(32)
                                .tpDst(new DtoRange(pubPortStart, pubPortEnd))
                                .natTargets(preTargets)
                                .nwProto(SimpleFirewallRule.stringToProtocolNumber(rule.getProtocol()))
                                .position(1);

                        Integer cnt = ipRuleCounts.get(publicIp);
                        if (cnt != null) {
                            ipRuleCounts.put(publicIp, new Integer(cnt.intValue() + 1));
                        } else {
                            ipRuleCounts.put(publicIp, new Integer(1));
                        }
                        String preNatRuleStr = new SimpleFirewallRule(preNatRule).toStringArray()[0];
                        existingPreNatRules.put(preNatRuleStr, preNatRule);
                        preNatRule.create();

                        if (routes.get(publicIp) == null) {
                            Route route =
                                providerRouter.addRoute()
                                    .type("Normal")
                                    .weight(100)
                                    .srcNetworkAddr("0.0.0.0")
                                    .srcNetworkLength(0)
                                    .dstNetworkAddr(publicIp)
                                    .dstNetworkLength(32)
                                    .nextHopPort(providerDownlink.getId());
                            route.create();
                            routes.put(publicIp, route);
                        }

                        // If Firewall is in our service offering, set up the
                        // default firewall rule
                        if (canHandle(network, Service.Firewall)) {
                            boolean defaultBlock = false;
                            for (Rule filterRule : preFilter.getRules()) {
                                String pfDstIp = filterRule.getNwDstAddress();
                                if (pfDstIp != null && filterRule.getNwDstAddress().equals(publicIp)) {
                                    defaultBlock = true;
                                    break;
                                }
                            }
                            if (!defaultBlock) {
                                preFilter.addRule().type(DtoRule.Drop).nwDstAddress(publicIp).nwDstLength(32).create();
                            }
                        }
                    }
                }
            }
        }
        return true;
    }

    private static Map<Service, Map<Capability, String>> setCapabilities() {
        Map<Service, Map<Capability, String>> capabilities = new HashMap<Service, Map<Capability, String>>();

        // L2 Support : SDN provisioning
        capabilities.put(Service.Connectivity, null);

        // L3 Support : Generic?
        capabilities.put(Service.Gateway, null);

        // L3 Support : DHCP
        Map<Capability, String> dhcpCapabilities = new HashMap<Capability, String>();
        capabilities.put(Service.Dhcp, dhcpCapabilities);

        // L3 Support : SourceNat
        Map<Capability, String> sourceNatCapabilities = new HashMap<Capability, String>();
        //sourceNatCapabilities.putAll(capabilities.get(Service.SourceNat));
        sourceNatCapabilities.put(Capability.SupportedSourceNatTypes, "peraccount");
        //sourceNatCapabilities.putAll(capabilities.get(Service.SourceNat));
        sourceNatCapabilities.put(Capability.RedundantRouter, "false");
        capabilities.put(Service.SourceNat, sourceNatCapabilities);

        // L3 Support : Port Forwarding
        capabilities.put(Service.PortForwarding, null);

        // L3 support : StaticNat
        capabilities.put(Service.StaticNat, null);

        // Set capabilities for Firewall service
        Map<Capability, String> firewallCapabilities = new HashMap<Capability, String>();
        firewallCapabilities.put(Capability.TrafficStatistics, "per public ip");
        firewallCapabilities.put(Capability.SupportedProtocols, "tcp,udp,icmp");
        firewallCapabilities.put(Capability.SupportedTrafficDirection, "ingress");
        firewallCapabilities.put(Capability.MultipleIps, "true");
        capabilities.put(Service.Firewall, firewallCapabilities);

        return capabilities;
    }

    private String getChainName(String routerName, RuleChainCode chainCode) {
        return getChainName("", routerName, chainCode);
    }

    private String getChainName(String networkId, String routerName, RuleChainCode chainCode) {

        String chain = "";

        switch (chainCode) {
            case TR_PRE:
                chain = "pre-routing";
                break;
            case TR_PREFILTER:
                chain = "pre-filter";
                break;
            case TR_PRENAT:
                chain = "pre-nat";
                break;
            case TR_POST:
                chain = "post-routing";
                break;
            case ACL_INGRESS:
                chain = "ACL-ingress-" + networkId;
                break;
            case ACL_EGRESS:
                chain = "ACL-egress-" + networkId;
                break;
        }

        return routerName + "-tenantrouter-" + chain;
    }

    protected RuleChain getChain(String accountUuid, String routerName, RuleChainCode chainCode) {
        return getChain("", accountUuid, routerName, chainCode);
    }

    protected RuleChain getChain(String networkId, String accountUuid, String routerName, RuleChainCode chainCode) {
        String chainName = getChainName(networkId, routerName, chainCode);

        MultivaluedMap findChain = new MultivaluedMapImpl();
        findChain.add("tenant_id", accountUuid);

        ResourceCollection<RuleChain> ruleChains = api.getChains(findChain);

        for (RuleChain chain : ruleChains) {
            if (chain.getName().equals(chainName)) {
                return chain;
            }
        }

        return null;
    }

    protected RouterPort[] getOrCreateProviderRouterPorts(Router tenantRouter, Router providerRouter) {
        RouterPort[] ports = new RouterPort[2];

        RouterPort tenantUplink = null;
        RouterPort providerDownlink = null;

        // Check if the ports and connection already exist
        for (Port peerPort : tenantRouter.getPeerPorts((new MultivaluedMapImpl()))) {
            if (peerPort != null && peerPort instanceof RouterPort) {
                RouterPort checkPort = (RouterPort)peerPort;
                if (checkPort.getDeviceId().compareTo(providerRouter.getId()) == 0) {
                    providerDownlink = checkPort;
                    tenantUplink = (RouterPort)api.getPort(checkPort.getPeerId());
                    break;
                }
            }
        }

        // Create the ports and connection if they don't exist
        if (providerDownlink == null) {
            // Add interior port on router side, with network details
            providerDownlink = providerRouter.addInteriorRouterPort().networkAddress("169.254.255.0").networkLength(32).portAddress("169.254.255.1").create();
            tenantUplink = tenantRouter.addInteriorRouterPort().networkAddress("169.254.255.0").networkLength(32).portAddress("169.254.255.2").create();

            // Link them up
            providerDownlink.link(tenantUplink.getId()).update();
        }

        ports[0] = tenantUplink;
        ports[1] = providerDownlink;

        return ports;
    }

    private Port[] getOrCreatePublicBridgePorts(NicProfile nic, Bridge publicBridge, Router providerRouter) {
        Port[] ports = new Port[2];

        BridgePort bridgeUplink = null;
        RouterPort providerDownlink = null;

        // Check if the ports and connection already exist
        for (Port peerPort : publicBridge.getPeerPorts()) {
            if (peerPort != null && peerPort instanceof RouterPort) {
                RouterPort checkPort = (RouterPort)peerPort;
                // Check it's a port on the providerRouter with the right gateway address
                if (checkPort.getDeviceId().compareTo(providerRouter.getId()) == 0 && checkPort.getPortAddress().equals(nic.getIPv4Gateway())) {
                    providerDownlink = checkPort;
                    bridgeUplink = (BridgePort)api.getPort(checkPort.getPeerId());
                    break;
                }
            }
        }

        // Create the ports and connection if they don't exist
        if (providerDownlink == null) {
            String cidr = NetUtils.ipAndNetMaskToCidr(nic.getIPv4Gateway(), nic.getIPv4Netmask());
            String cidrSubnet = NetUtils.getCidrSubNet(cidr);
            int cidrSize = (int)NetUtils.getCidrSize(NetUtils.cidr2Netmask(cidr));
            String gateway = nic.getIPv4Gateway();

            // Add interior port on router side, with network details
            providerDownlink = providerRouter.addInteriorRouterPort().networkAddress(cidrSubnet).networkLength(cidrSize).portAddress(gateway).create();
            bridgeUplink = publicBridge.addInteriorPort().create();

            // Link them up
            providerDownlink.link(bridgeUplink.getId()).update();

        }

        ports[0] = bridgeUplink;
        ports[1] = providerDownlink;

        return ports;
    }

    private void ensureBridgeConnectedToRouter(Network network, Bridge netBridge, Router netRouter) {
        if (getBridgeToRouterPort(network, netBridge, netRouter) == null) {
            connectBridgeToRouter(network, netBridge, netRouter);
        }
    }

    private BridgePort getBridgeToRouterPort(Network network, Bridge netBridge, Router netRouter) {
        for (Port p : netBridge.getPeerPorts()) {
            if (p.getClass().equals(BridgePort.class)) {
                BridgePort bp = (BridgePort)p;
                if (bp.getPeerId().compareTo(netRouter.getId()) == 0) {
                    return bp;
                }
            }
        }
        return null;
    }

    /*
     * resetEgressACLFilter sets the Egress ACL Filter back to its initial
     * state - drop everything. This needs to be called when all Egress
     * ACL rules are deleted, so we can start allowing all Egress traffic
     * again
     */
    protected void resetEgressACLFilter(Network network) {
        boolean isVpc = getIsVpc(network);
        long id = getRouterId(network, isVpc);
        String routerName = getRouterName(isVpc, id);

        RuleChain egressChain = getChain(String.valueOf(network.getId()), getAccountUuid(network), routerName, RuleChainCode.ACL_EGRESS);

        // Clear all the rules out
        for (Rule rule : egressChain.getRules()) {
            rule.delete();
        }

        // Add a matchForwardFlow rule so that we can accept all return traffic
        egressChain.addRule().type(DtoRule.Accept).matchForwardFlow(true).position(1).create();
    }

    protected RuleChain getOrInitEgressACLFilter(Network network) {
        boolean isVpc = getIsVpc(network);
        long id = getRouterId(network, isVpc);
        String routerName = getRouterName(isVpc, id);

        RuleChain egressChain = getChain(String.valueOf(network.getId()), getAccountUuid(network), routerName, RuleChainCode.ACL_EGRESS);

        // Rules set by the user will have a protocol, so we count the ACL
        // rules by counting how much have the nwProto field set.
        int totalRules = 0;
        for (Rule rule : egressChain.getRules()) {
            if (rule.getNwProto() != 0) {
                totalRules++;
            }
        }

        if (totalRules > 0) {
            // There are already rules present, no need to init.
            return egressChain;
        } else {
            // We need to delete any placeholder rules
            for (Rule rule : egressChain.getRules()) {
                rule.delete();
            }
        }

        int pos = 1;
        // If it is ARP, accept it
        egressChain.addRule().type(DtoRule.Accept).dlType(0x0806).position(pos++).create();

        // If it is ICMP to the router, accept that
        egressChain.addRule()
            .type(DtoRule.Accept)
            .nwProto(SimpleFirewallRule.stringToProtocolNumber("icmp"))
            .nwDstAddress(network.getGateway())
            .nwDstLength(32)
            .position(pos++)
            .create();

        // Everything else gets dropped
        egressChain.addRule().type(DtoRule.Drop).position(pos).create();

        return egressChain;
    }

    private void connectBridgeToRouter(Network network, Bridge netBridge, Router netRouter) {

        boolean isVpc = getIsVpc(network);
        long id = getRouterId(network, isVpc);
        String routerName = getRouterName(isVpc, id);
        String accountIdStr = getAccountUuid(network);

        // Add interior port on bridge side
        BridgePort bridgePort = netBridge.addInteriorPort().create();

        // Add interior port on router side, with network details
        RouterPort routerPort = netRouter.addInteriorRouterPort();
        String cidr = network.getCidr();
        String cidrSubnet = NetUtils.getCidrSubNet(cidr);
        int cidrSize = (int)NetUtils.getCidrSize(NetUtils.cidr2Netmask(cidr));

        routerPort.networkAddress(cidrSubnet);
        routerPort.networkLength(cidrSize);
        routerPort.portAddress(network.getGateway());

        // If this is a VPC, then we will be using NetworkACLs, which is
        // implemented via chains on the router port to that network.
        if (getIsVpc(network)) {
            // Create ACL filter chain for traffic coming INTO the network
            // (outbound from the port
            int pos = 1;

            RuleChain inc = api.addChain().name(getChainName(String.valueOf(network.getId()), routerName, RuleChainCode.ACL_INGRESS)).tenantId(accountIdStr).create();

            // If it is ARP, accept it
            inc.addRule().type(DtoRule.Accept).dlType(0x0806).position(pos++).create();

            // If it is ICMP to the router, accept that
            inc.addRule()
                .type(DtoRule.Accept)
                .nwProto(SimpleFirewallRule.stringToProtocolNumber("icmp"))
                .nwDstAddress(network.getGateway())
                .nwDstLength(32)
                .position(pos++)
                .create();

            // If it is connection tracked, accept that as well
            inc.addRule().type(DtoRule.Accept).matchReturnFlow(true).position(pos++).create();

            inc.addRule().type(DtoRule.Drop).position(pos).create();

            //
            RuleChain out = api.addChain().name(getChainName(String.valueOf(network.getId()), routerName, RuleChainCode.ACL_EGRESS)).tenantId(accountIdStr).create();

            // Creating the first default rule here that does nothing
            // but start connection tracking.
            out.addRule().type(DtoRule.Accept).matchForwardFlow(true).position(1).create();

            routerPort.outboundFilterId(inc.getId());
            routerPort.inboundFilterId(out.getId());
        }

        routerPort.create();

        // Link them up
        bridgePort.link(routerPort.getId()).update();

        // Set up default route from router to subnet
        netRouter.addRoute()
            .type("Normal")
            .weight(100)
            .srcNetworkAddr("0.0.0.0")
            .srcNetworkLength(0)
            .dstNetworkAddr(cidrSubnet)
            .dstNetworkLength(cidrSize)
            .nextHopPort(routerPort.getId())
            .nextHopGateway(null)
            .create();
    }

    private Bridge getOrCreateNetworkBridge(Network network) {
        // Find the single bridge for this network, create if doesn't exist
        return getOrCreateNetworkBridge(network.getId(), getAccountUuid(network));
    }

    private Bridge getOrCreateNetworkBridge(long networkID, String accountUuid) {
        Bridge netBridge = getNetworkBridge(networkID, accountUuid);
        if (netBridge == null) {

            String networkUUIDStr = String.valueOf(networkID);

            s_logger.debug("Attempting to create guest network bridge");
            try {
                netBridge = api.addBridge().tenantId(accountUuid).name(networkUUIDStr).create();
            } catch (HttpInternalServerError ex) {
                s_logger.warn("Bridge creation failed, retrying bridge get in case it now exists.", ex);
                netBridge = getNetworkBridge(networkID, accountUuid);
            }
        }
        return netBridge;
    }

    private Bridge getNetworkBridge(long networkID, String accountUuid) {

        MultivaluedMap qNetBridge = new MultivaluedMapImpl();
        String networkUUIDStr = String.valueOf(networkID);
        qNetBridge.add("tenant_id", accountUuid);

        for (Bridge b : this.api.getBridges(qNetBridge)) {
            if (b.getName().equals(networkUUIDStr)) {
                return b;
            }
        }

        return null;
    }

    protected boolean getIsVpc(Network network) {
        return (network.getVpcId() != null);
    }

    protected long getRouterId(Network network, boolean isVpc) {
        if (isVpc) {
            return network.getVpcId();
        } else {
            return network.getId();
        }
    }

    private Router getOrCreateGuestNetworkRouter(Network network) {
        // Find the single bridge for this (isolated) guest network, create if doesn't exist
        boolean isVpc = getIsVpc(network);
        long id = getRouterId(network, isVpc);

        return getOrCreateGuestNetworkRouter(id, getAccountUuid(network), isVpc);

    }

    protected String getRouterName(boolean isVpc, long id) {
        if (isVpc) {
            return "VPC" + String.valueOf(id);
        } else {
            return String.valueOf(id);
        }
    }

    protected Router createRouter(long id, String accountUuid, boolean isVpc) {

        String routerName = getRouterName(isVpc, id);

        //Set up rule chains
        RuleChain pre = api.addChain().name(getChainName(routerName, RuleChainCode.TR_PRE)).tenantId(accountUuid).create();
        RuleChain post = api.addChain().name(getChainName(routerName, RuleChainCode.TR_POST)).tenantId(accountUuid).create();

        // Set up NAT and filter chains for pre-routing
        RuleChain preFilter = api.addChain().name(getChainName(routerName, RuleChainCode.TR_PREFILTER)).tenantId(accountUuid).create();
        RuleChain preNat = api.addChain().name(getChainName(routerName, RuleChainCode.TR_PRENAT)).tenantId(accountUuid).create();

        // Hook the chains in - first jump to Filter chain, then jump to Nat chain
        pre.addRule().type(DtoRule.Jump).jumpChainId(preFilter.getId()).position(1).create();
        pre.addRule().type(DtoRule.Jump).jumpChainId(preNat.getId()).position(2).create();

        return api.addRouter().tenantId(accountUuid).name(routerName).inboundFilterId(pre.getId()).outboundFilterId(post.getId()).create();
    }

    private Router getOrCreateGuestNetworkRouter(long id, String accountUuid, boolean isVpc) {
        Router tenantRouter = getGuestNetworkRouter(id, accountUuid, isVpc);
        if (tenantRouter == null) {
            tenantRouter = createRouter(id, accountUuid, isVpc);
        }
        return tenantRouter;
    }

    private Router getGuestNetworkRouter(long id, String accountUuid, boolean isVpc) {

        MultivaluedMap qNetRouter = new MultivaluedMapImpl();
        String routerName = getRouterName(isVpc, id);

        qNetRouter.add("tenant_id", accountUuid);

        for (Router router : api.getRouters(qNetRouter)) {
            if (router.getName().equals(routerName)) {
                return router;
            }
        }

        return null;
    }

    private UUID getNicUUID(NicProfile nic) {
        NicVO nicvo = _nicDao.findById(nic.getId());
        return UUID.fromString(nicvo.getUuid());
    }

    private void cleanBridge(Bridge br) {

        for (Port peerPort : br.getPeerPorts()) {
            if (peerPort != null && peerPort instanceof RouterPort) {
                RouterPort checkPort = (RouterPort)peerPort;
                if (checkPort.getType().equals("ExteriorRouter")) {
                    checkPort.vifId(null).update();
                } else if (checkPort.getType().equals("InteriorRouter")) {
                    checkPort.unlink();
                }
                checkPort.delete();
            }
        }

        for (BridgePort p : br.getPorts()) {

            if (p.getType().equals("ExteriorBridge")) {
                // Set VIF ID to null
                p.vifId(null).update();
            }

            if (p.getType().equals("InteriorBridge")) {
                p.unlink();
            }

            // Delete port
            p.delete();
        }
    }

    private void deleteNetworkBridges(Network network) {
        String accountUuid = getAccountUuid(network);
        long networkID = network.getId();

        Bridge netBridge = getNetworkBridge(networkID, accountUuid);
        if (netBridge != null) {

            cleanBridge(netBridge);

            // Delete DHCP subnets
            for (Object dhcpSubnet : netBridge.getDhcpSubnets()) {
                DhcpSubnet sub = (DhcpSubnet)dhcpSubnet;
                sub.delete();
            }

            netBridge.delete();
        }
    }

    private void deleteGuestNetworkRouters(Network network) {
        String accountUuid = getAccountUuid(network);
        boolean isVpc = getIsVpc(network);
        long id = getRouterId(network, isVpc);

        Router tenantRouter = getGuestNetworkRouter(id, accountUuid, isVpc);

        // Delete any peer ports corresponding to this router
        for (Port peerPort : tenantRouter.getPeerPorts((new MultivaluedMapImpl()))) {
            if (peerPort != null && peerPort instanceof RouterPort) {
                RouterPort checkPort = (RouterPort)peerPort;
                if (checkPort.getType().equals("ExteriorRouter")) {
                    checkPort.vifId(null).update();
                } else if (checkPort.getType().equals("InteriorRouter")) {
                    checkPort.unlink();
                }
                checkPort.delete();
            } else if (peerPort != null && peerPort instanceof BridgePort) {
                BridgePort checkPort = (BridgePort)peerPort;
                if (checkPort.getType().equals("ExteriorBridge")) {
                    checkPort.vifId(null).update();
                } else if (checkPort.getType().equals("InteriorBridge")) {
                    checkPort.unlink();
                }
                checkPort.delete();
            }
        }

        if (tenantRouter != null) {
            // Remove all peer ports if any exist
            for (RouterPort p : tenantRouter.getPorts(new MultivaluedMapImpl())) {
                if (p.getType().equals("ExteriorRouter")) {
                    // Set VIF ID to null
                    p.vifId(null).update();
                    // the port might have some chains associated with it
                }

                if (p.getType().equals("InteriorRouter")) {
                    p.unlink();
                }

                // Delete port
                p.delete();
            }

            // Remove inbound and outbound filter chains
            String accountIdStr = String.valueOf(accountUuid);
            String routerName = getRouterName(isVpc, id);

            RuleChain pre = api.getChain(tenantRouter.getInboundFilterId());
            RuleChain preFilter = getChain(accountIdStr, routerName, RuleChainCode.TR_PREFILTER);
            RuleChain preNat = getChain(accountIdStr, routerName, RuleChainCode.TR_PRENAT);
            RuleChain post = api.getChain(tenantRouter.getOutboundFilterId());

            pre.delete();
            preFilter.delete();
            preNat.delete();
            post.delete();

            // Remove routes
            for (Route r : tenantRouter.getRoutes(new MultivaluedMapImpl())) {
                r.delete();
            }

            tenantRouter.delete();
        }
    }

    @Override
    public List<Class<?>> getCommands() {
        // MidoNet does not implement any commands, so we return an empty list.
        return new ArrayList<Class<?>>();
    }
}
