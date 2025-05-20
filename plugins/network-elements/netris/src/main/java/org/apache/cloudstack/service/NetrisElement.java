// Licensed to the Apache Software Foundation (ASF) under one
// or more contributor license agreements.  See the NOTICE file
// distributed with this work for additional information
// regarding copyright ownership.  The ASF licenses this file
// to you under the Apache License, Version 2.0 (the
// "License"); you may not use this file except in compliance
// with the License.  You may obtain a copy of the License at
//
//   http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing,
// software distributed under the License is distributed on an
// "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
// KIND, either express or implied.  See the License for the
// specific language governing permissions and limitations
// under the License.
package org.apache.cloudstack.service;

import com.amazonaws.util.CollectionUtils;
import com.cloud.agent.AgentManager;
import com.cloud.agent.Listener;
import com.cloud.agent.api.AgentControlAnswer;
import com.cloud.agent.api.AgentControlCommand;
import com.cloud.agent.api.Answer;
import com.cloud.agent.api.Command;
import com.cloud.agent.api.StartupCommand;
import com.cloud.agent.api.to.LoadBalancerTO;
import com.cloud.api.ApiDBUtils;
import com.cloud.dc.DataCenterVO;
import com.cloud.dc.dao.DataCenterDao;
import com.cloud.deploy.DeployDestination;
import com.cloud.domain.DomainVO;
import com.cloud.domain.dao.DomainDao;
import com.cloud.exception.ConcurrentOperationException;
import com.cloud.exception.ConnectionException;
import com.cloud.exception.InsufficientCapacityException;
import com.cloud.exception.ResourceUnavailableException;
import com.cloud.host.Host;
import com.cloud.host.HostVO;
import com.cloud.host.Status;
import com.cloud.network.IpAddress;
import com.cloud.network.Network;
import com.cloud.network.NetworkModel;
import com.cloud.network.PhysicalNetworkServiceProvider;
import com.cloud.network.PublicIpAddress;
import com.cloud.network.SDNProviderNetworkRule;
import com.cloud.network.SDNProviderOpObject;
import com.cloud.network.as.AutoScaleCounter;
import com.cloud.network.dao.IPAddressDao;
import com.cloud.network.dao.IPAddressVO;
import com.cloud.network.dao.LoadBalancerVMMapDao;
import com.cloud.network.dao.LoadBalancerVMMapVO;
import com.cloud.network.dao.NetworkDao;
import com.cloud.network.dao.NetworkVO;
import com.cloud.network.element.DhcpServiceProvider;
import com.cloud.network.element.DnsServiceProvider;
import com.cloud.network.element.IpDeployer;
import com.cloud.network.element.LoadBalancingServiceProvider;
import com.cloud.network.element.NetworkACLServiceProvider;
import com.cloud.network.element.PortForwardingServiceProvider;
import com.cloud.network.element.StaticNatServiceProvider;
import com.cloud.network.element.VirtualRouterElement;
import com.cloud.network.element.VpcProvider;
import com.cloud.network.lb.LoadBalancingRule;
import com.cloud.network.netris.NetrisLbBackend;
import com.cloud.network.netris.NetrisService;
import com.cloud.network.rules.FirewallRule;
import com.cloud.network.rules.LoadBalancerContainer;
import com.cloud.network.rules.PortForwardingRule;
import com.cloud.network.rules.StaticNat;
import com.cloud.network.vpc.NetworkACLItem;
import com.cloud.network.vpc.PrivateGateway;
import com.cloud.network.vpc.StaticRoute;
import com.cloud.network.vpc.StaticRouteProfile;
import com.cloud.network.vpc.Vpc;
import com.cloud.network.vpc.VpcVO;
import com.cloud.network.vpc.dao.VpcDao;
import com.cloud.offering.NetworkOffering;
import com.cloud.resource.ResourceManager;
import com.cloud.resource.ResourceStateAdapter;
import com.cloud.resource.ServerResource;
import com.cloud.resource.UnableDeleteHostException;
import com.cloud.user.Account;
import com.cloud.user.AccountManager;
import com.cloud.uservm.UserVm;
import com.cloud.utils.Pair;
import com.cloud.utils.component.AdapterBase;
import com.cloud.utils.db.Transaction;
import com.cloud.utils.db.TransactionCallback;
import com.cloud.utils.exception.CloudRuntimeException;
import com.cloud.vm.NicProfile;
import com.cloud.vm.ReservationContext;
import com.cloud.vm.VMInstanceVO;
import com.cloud.vm.VirtualMachineProfile;
import com.cloud.vm.dao.VMInstanceDao;
import com.google.gson.Gson;
import org.apache.cloudstack.StartupNetrisCommand;
import org.apache.cloudstack.api.ApiConstants;
import com.cloud.network.netris.NetrisNetworkRule;
import org.apache.cloudstack.resourcedetail.FirewallRuleDetailVO;
import org.apache.cloudstack.resourcedetail.dao.FirewallRuleDetailsDao;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.stereotype.Component;

import javax.inject.Inject;
import javax.naming.ConfigurationException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

@Component
public class NetrisElement extends AdapterBase implements DhcpServiceProvider, DnsServiceProvider, VpcProvider,
        StaticNatServiceProvider, IpDeployer, PortForwardingServiceProvider, NetworkACLServiceProvider,
        LoadBalancingServiceProvider, ResourceStateAdapter, Listener {

    @Inject
    NetworkModel networkModel;
    @Inject
    AgentManager agentManager;
    @Inject
    ResourceManager resourceManager;
    @Inject
    private NetrisService netrisService;
    @Inject
    private AccountManager accountManager;
    @Inject
    private DataCenterDao dataCenterDao;
    @Inject
    private NetworkDao networkDao;
    @Inject
    private DomainDao domainDao;
    @Inject
    private VpcDao vpcDao;
    @Inject
    private FirewallRuleDetailsDao firewallRuleDetailsDao;
    @Inject
    private IPAddressDao ipAddressDao;
    @Inject
    private VMInstanceDao vmInstanceDao;
    @Inject
    LoadBalancerVMMapDao lbVmMapDao;

    protected Logger logger = LogManager.getLogger(getClass());

    private final Map<Network.Service, Map<Network.Capability, String>> capabilities = initCapabilities();

    protected static List<AutoScaleCounter> getNetrisAutoScaleCounters() {
        AutoScaleCounter counter;
        final List<AutoScaleCounter> counterList = new ArrayList<>();
        counter = new AutoScaleCounter(AutoScaleCounter.AutoScaleCounterType.Cpu);
        counterList.add(counter);
        counter = new AutoScaleCounter(AutoScaleCounter.AutoScaleCounterType.Memory);
        counterList.add(counter);
        return counterList;
    }

    protected static Map<Network.Service, Map<Network.Capability, String>> initCapabilities() {
        Map<Network.Service, Map<Network.Capability, String>> capabilities = new HashMap<>();

        Map<Network.Capability, String> dhcpCapabilities = Map.of(Network.Capability.DhcpAccrossMultipleSubnets, "true");
        capabilities.put(Network.Service.Dhcp, dhcpCapabilities);

        Map<Network.Capability, String> dnsCapabilities = new HashMap<>();
        dnsCapabilities.put(Network.Capability.AllowDnsSuffixModification, "true");
        capabilities.put(Network.Service.Dns, dnsCapabilities);

        capabilities.put(Network.Service.StaticNat, null);

        // Set capabilities for LB service
        Map<Network.Capability, String> lbCapabilities = new HashMap<Network.Capability, String>();
        lbCapabilities.put(Network.Capability.SupportedLBAlgorithms, "roundrobin,leastconn");
        lbCapabilities.put(Network.Capability.SupportedLBIsolation, "dedicated");
        lbCapabilities.put(Network.Capability.SupportedProtocols, "tcp, udp");
        lbCapabilities.put(Network.Capability.SupportedStickinessMethods, VirtualRouterElement.getHAProxyStickinessCapability());
        lbCapabilities.put(Network.Capability.LbSchemes, String.join(",", LoadBalancerContainer.Scheme.Internal.name(), LoadBalancerContainer.Scheme.Public.name()));
        final Gson gson = new Gson();
        final String autoScaleCounterList = gson.toJson(getNetrisAutoScaleCounters());
        lbCapabilities.put(Network.Capability.AutoScaleCounters, autoScaleCounterList);
        lbCapabilities.put(Network.Capability.VmAutoScaling, "true");

        capabilities.put(Network.Service.Lb, lbCapabilities);
        capabilities.put(Network.Service.PortForwarding, null);
        capabilities.put(Network.Service.NetworkACL, null);

        Map<Network.Capability, String> firewallCapabilities = new HashMap<>();
        firewallCapabilities.put(Network.Capability.SupportedProtocols, "tcp,udp,icmp");
        firewallCapabilities.put(Network.Capability.SupportedEgressProtocols, "tcp,udp,icmp,all");
        firewallCapabilities.put(Network.Capability.MultipleIps, "true");
        firewallCapabilities.put(Network.Capability.TrafficStatistics, "per public ip");
        firewallCapabilities.put(Network.Capability.SupportedTrafficDirection, "ingress, egress");
        capabilities.put(Network.Service.Firewall, firewallCapabilities);

        Map<Network.Capability, String> sourceNatCapabilities = new HashMap<>();
        sourceNatCapabilities.put(Network.Capability.RedundantRouter, "true");
        sourceNatCapabilities.put(Network.Capability.SupportedSourceNatTypes, "peraccount");
        capabilities.put(Network.Service.Gateway, null);
        capabilities.put(Network.Service.SourceNat, sourceNatCapabilities);
        return capabilities;
    }

    @Override
    public boolean processAnswers(long agentId, long seq, Answer[] answers) {
        return false;
    }

    @Override
    public boolean processCommands(long agentId, long seq, Command[] commands) {
        return false;
    }

    @Override
    public AgentControlAnswer processControlCommand(long agentId, AgentControlCommand cmd) {
        return null;
    }

    @Override
    public void processHostAdded(long hostId) {
        // Do nothing
    }

    @Override
    public void processConnect(Host host, StartupCommand cmd, boolean forRebalance) throws ConnectionException {
        // Do nothing
    }

    @Override
    public boolean processDisconnect(long agentId, Status state) {
        return false;
    }

    @Override
    public void processHostAboutToBeRemoved(long hostId) {
        // Do nothing
    }

    @Override
    public void processHostRemoved(long hostId, long clusterId) {
        // Do nothing
    }

    @Override
    public boolean isRecurring() {
        return false;
    }

    @Override
    public int getTimeout() {
        return 0;
    }

    @Override
    public boolean processTimeout(long agentId, long seq) {
        return false;
    }

    protected boolean canHandle(Network network, Network.Service service) {
        logger.debug("Checking if Netris Element can handle service " + service.getName() + " on network "
                + network.getDisplayText());

        if (!networkModel.isProviderForNetwork(getProvider(), network.getId())) {
            logger.debug("Netris Element is not a provider for network " + network.getDisplayText());
            return false;
        }

        return true;
    }

    @Override
    public boolean addDhcpEntry(Network network, NicProfile nic, VirtualMachineProfile vm, DeployDestination dest, ReservationContext context) throws ConcurrentOperationException, InsufficientCapacityException, ResourceUnavailableException {
        return true;
    }

    @Override
    public boolean configDhcpSupportForSubnet(Network network, NicProfile nic, VirtualMachineProfile vm, DeployDestination dest, ReservationContext context) throws ConcurrentOperationException, InsufficientCapacityException, ResourceUnavailableException {
        return true;
    }

    @Override
    public boolean removeDhcpSupportForSubnet(Network network) throws ResourceUnavailableException {
        return true;
    }

    @Override
    public boolean setExtraDhcpOptions(Network network, long nicId, Map<Integer, String> dhcpOptions) {
        return true;
    }

    @Override
    public boolean removeDhcpEntry(Network network, NicProfile nic, VirtualMachineProfile vmProfile) throws ResourceUnavailableException {
        return true;
    }

    @Override
    public boolean addDnsEntry(Network network, NicProfile nic, VirtualMachineProfile vm, DeployDestination dest, ReservationContext context) throws ConcurrentOperationException, InsufficientCapacityException, ResourceUnavailableException {
        return true;
    }

    @Override
    public boolean configDnsSupportForSubnet(Network network, NicProfile nic, VirtualMachineProfile vm, DeployDestination dest, ReservationContext context) throws ConcurrentOperationException, InsufficientCapacityException, ResourceUnavailableException {
        return true;
    }

    @Override
    public boolean removeDnsSupportForSubnet(Network network) throws ResourceUnavailableException {
        return true;
    }

    @Override
    public Map<Network.Service, Map<Network.Capability, String>> getCapabilities() {
        return capabilities;
    }

    @Override
    public boolean applyIps(Network network, List<? extends PublicIpAddress> ipAddress, Set<Network.Service> services) throws ResourceUnavailableException {
        return true;
    }

    @Override
    public Network.Provider getProvider() {
        return Network.Provider.Netris;
    }

    @Override
    public boolean configure(String name, Map<String, Object> params) throws ConfigurationException {
        agentManager.registerForHostEvents(this, true, true, true);
        resourceManager.registerResourceStateAdapter(this.getClass().getSimpleName(), this);
        return true;
    }

    @Override
    public boolean implement(Network network, NetworkOffering offering, DeployDestination dest, ReservationContext context) throws ConcurrentOperationException, ResourceUnavailableException, InsufficientCapacityException {
        return true;
    }

    @Override
    public boolean prepare(Network network, NicProfile nic, VirtualMachineProfile vm, DeployDestination dest, ReservationContext context) throws ConcurrentOperationException, ResourceUnavailableException, InsufficientCapacityException {
        return false;
    }

    @Override
    public boolean release(Network network, NicProfile nic, VirtualMachineProfile vm, ReservationContext context) throws ConcurrentOperationException, ResourceUnavailableException {
        return false;
    }

    @Override
    public boolean releaseIp(IpAddress ipAddress) {
        return netrisService.releaseNatIp(ipAddress.getDataCenterId(), ipAddress.getAddress().addr());
    }

    @Override
    public boolean shutdown(Network network, ReservationContext context, boolean cleanup) throws ConcurrentOperationException, ResourceUnavailableException {
        return canHandle(network, Network.Service.Connectivity);
    }

    @Override
    public boolean destroy(Network network, ReservationContext context) throws ConcurrentOperationException, ResourceUnavailableException {
        Account account = accountManager.getAccount(network.getAccountId());
        NetworkVO networkVO = networkDao.findById(network.getId());
        DataCenterVO zone = dataCenterDao.findById(network.getDataCenterId());
        DomainVO domain = domainDao.findById(account.getDomainId());
        if (Objects.isNull(zone)) {
            String msg = String.format("Cannot find zone with ID %s", network.getDataCenterId());
            logger.error(msg);
            throw new CloudRuntimeException(msg);
        }
        String vpcName = null;
        Long vpcId = network.getVpcId();
        if (Objects.nonNull(vpcId)) {
            VpcVO vpc = vpcDao.findById(vpcId);
            if (Objects.nonNull(vpc)) {
                vpcName = vpc.getName();
            }
        }
        netrisService.deleteVnetResource(zone.getId(), account.getId(), domain.getId(), vpcName, vpcId, networkVO.getName(), network.getId(), network.getCidr());
        return true;
    }

    @Override
    public boolean isReady(PhysicalNetworkServiceProvider provider) {
        return true;
    }

    @Override
    public boolean shutdownProviderInstances(PhysicalNetworkServiceProvider provider, ReservationContext context) throws ConcurrentOperationException, ResourceUnavailableException {
        return false;
    }

    @Override
    public boolean canEnableIndividualServices() {
        return true;
    }

    @Override
    public boolean verifyServicesCombination(Set<Network.Service> services) {
        return true;
    }

    @Override
    public HostVO createHostVOForConnectedAgent(HostVO host, StartupCommand[] cmd) {
        return null;
    }

    @Override
    public HostVO createHostVOForDirectConnectAgent(HostVO host, StartupCommand[] startup, ServerResource resource, Map<String, String> details, List<String> hostTags) {
        if (!(startup[0] instanceof StartupNetrisCommand)) {
            return null;
        }
        host.setType(Host.Type.L2Networking);
        return host;
    }

    @Override
    public DeleteHostAnswer deleteHost(HostVO host, boolean isForced, boolean isForceDeleteStorage) throws UnableDeleteHostException {
        return null;
    }

    @Override
    public boolean implementVpc(Vpc vpc, DeployDestination dest, ReservationContext context) throws ConcurrentOperationException, ResourceUnavailableException, InsufficientCapacityException {
        return true;
    }

    @Override
    public boolean shutdownVpc(Vpc vpc, ReservationContext context) throws ConcurrentOperationException, ResourceUnavailableException {
        long zoneId = vpc.getZoneId();
        long accountId = vpc.getAccountId();
        long domainId = vpc.getDomainId();
        return netrisService.deleteVpcResource(zoneId, accountId, domainId, vpc);
    }

    @Override
    public boolean createPrivateGateway(PrivateGateway gateway) throws ConcurrentOperationException, ResourceUnavailableException {
        return true;
    }

    @Override
    public boolean deletePrivateGateway(PrivateGateway privateGateway) throws ConcurrentOperationException, ResourceUnavailableException {
        return true;
    }

    @Override
    public boolean applyStaticRoutes(Vpc vpc, List<StaticRouteProfile> routes) throws ResourceUnavailableException {
        List<StaticRoute> existingStaticRoutes = netrisService.listStaticRoutes(vpc.getZoneId(), vpc.getAccountId(), vpc.getDomainId(), vpc.getName(), vpc.getId(), true, null, null, null);
        List<String> staticRouteCidrs = new ArrayList<>();
        for(StaticRouteProfile staticRoute : routes) {
            if (StaticRoute.State.Add == staticRoute.getState()) {
                netrisService.addOrUpdateStaticRoute(vpc.getZoneId(), vpc.getAccountId(), vpc.getDomainId(), vpc.getName(), vpc.getId(), true, staticRoute.getCidr(), staticRoute.getGateway(), staticRoute.getId(), false);
            } else if (StaticRoute.State.Revoke == staticRoute.getState()) {
                netrisService.deleteStaticRoute(vpc.getZoneId(), vpc.getAccountId(), vpc.getDomainId(), vpc.getName(), vpc.getId(), true, staticRoute.getCidr(), staticRoute.getGateway(), staticRoute.getId());
            } else if (StaticRoute.State.Update == staticRoute.getState()) {
                netrisService.addOrUpdateStaticRoute(vpc.getZoneId(), vpc.getAccountId(), vpc.getDomainId(), vpc.getName(), vpc.getId(), true, staticRoute.getCidr(), staticRoute.getGateway(), staticRoute.getId(), true);
            }
            staticRouteCidrs.add(staticRoute.getCidr());
        }
        for (StaticRoute staticRoute : existingStaticRoutes) {
            if (!staticRouteCidrs.contains(staticRoute.getCidr())) {
                logger.info("Revoking static route with cidr {} which are not used by VPC {}", staticRoute.getCidr(), vpc);
                netrisService.deleteStaticRoute(vpc.getZoneId(), vpc.getAccountId(), vpc.getDomainId(), vpc.getName(), vpc.getId(), true, staticRoute.getCidr(), null, staticRoute.getId());
            }
        }
        return true;
    }

    @Override
    public boolean applyACLItemsToPrivateGw(PrivateGateway gateway, List<? extends NetworkACLItem> rules) throws ResourceUnavailableException {
        return true;
    }

    @Override
    public boolean updateVpcSourceNatIp(Vpc vpc, IpAddress address) {
        return netrisService.updateVpcSourceNatIp(vpc, address);
    }

    @Override
    public boolean updateVpc(Vpc vpc, String previousVpcName) {
        return netrisService.updateVpcResource(vpc.getZoneId(), vpc.getAccountId(), vpc.getDomainId(), vpc.getId(), vpc.getName(), previousVpcName);
    }

    @Override
    public boolean applyNetworkACLs(Network network, List<? extends NetworkACLItem> rules) throws ResourceUnavailableException {
        if (!canHandle(network, Network.Service.NetworkACL)) {
            return false;
        }

        List<NetrisNetworkRule> nsxDelNetworkRules = new ArrayList<>();
        boolean success = true;
        for (NetworkACLItem rule : rules) {
            String privatePort = PortForwardingServiceProvider.getPrivatePortRangeForACLRule(rule);
            NetrisNetworkRule networkRule = getNetrisNetworkRuleForAcl(rule, privatePort);
            if (Arrays.asList(NetworkACLItem.State.Active, NetworkACLItem.State.Add).contains(rule.getState())) {
                success = success && netrisService.addFirewallRules(network, List.of(networkRule));
            } else if (NetworkACLItem.State.Revoke == rule.getState()) {
                nsxDelNetworkRules.add(networkRule);
            }
        }

        if (!nsxDelNetworkRules.isEmpty()) {
            success = netrisService.deleteFirewallRules(network, nsxDelNetworkRules);
            if (!success) {
                logger.warn("Not all firewall rules were successfully deleted");
            }
        }
        return success;
    }

    private NetrisNetworkRule getNetrisNetworkRuleForAcl(NetworkACLItem rule, String privatePort) {
        SDNProviderNetworkRule baseNetworkRule = new SDNProviderNetworkRule.Builder()
                .setRuleId(rule.getId())
                .setSourceCidrList(!CollectionUtils.isNullOrEmpty(rule.getSourceCidrList())  ? transformCidrListValues(rule.getSourceCidrList()) : List.of("ANY"))
                .setTrafficType(rule.getTrafficType().toString())
                .setProtocol(rule.getProtocol().toUpperCase())
                .setPublicPort(String.valueOf(rule.getSourcePortStart()))
                .setPrivatePort(String.valueOf(privatePort))
                .setIcmpCode(rule.getIcmpCode())
                .setIcmpType(rule.getIcmpType())
                .setService(Network.Service.NetworkACL)
                .build();
        return new NetrisNetworkRule.Builder()
                .baseRule(baseNetworkRule)
                .aclAction(transformActionValue(rule.getAction()))
                        .reason(rule.getReason())
                .build();
    }

    protected List<String> transformCidrListValues(List<String> sourceCidrList) {
        List<String> list = new ArrayList<>();
        if (org.apache.commons.collections.CollectionUtils.isNotEmpty(sourceCidrList)) {
            for (String cidr : sourceCidrList) {
                if (cidr.equals("0.0.0.0/0")) {
                    list.add("ANY");
                } else {
                    list.add(cidr);
                }
            }
        }
        return list;
    }

    protected NetrisNetworkRule.NetrisRuleAction transformActionValue(NetworkACLItem.Action action) {
        if (action == NetworkACLItem.Action.Allow) {
            return NetrisNetworkRule.NetrisRuleAction.PERMIT;
        } else if (action == NetworkACLItem.Action.Deny) {
            return NetrisNetworkRule.NetrisRuleAction.DENY;
        }
        String err = String.format("Unsupported action %s", action.toString());
        logger.error(err);
        throw new CloudRuntimeException(err);
    }

    @Override
    public boolean reorderAclRules(Vpc vpc, List<? extends Network> networks, List<? extends NetworkACLItem> networkACLItems) {
        List<NetrisNetworkRule> aclRulesList = new ArrayList<>();
        for (NetworkACLItem rule : networkACLItems) {
            String privatePort = PortForwardingServiceProvider.getPrivatePortRangeForACLRule(rule);
            aclRulesList.add(getNetrisNetworkRuleForAcl(rule, privatePort));
        }
        for (Network network: networks) {
            netrisService.deleteFirewallRules(network, aclRulesList);
        }
        boolean success = true;
        for (Network network : networks) {
            for (NetrisNetworkRule aclRule : aclRulesList) {
                success = success && netrisService.addFirewallRules(network, List.of(aclRule));
            }
        }
        return success;
    }

    @Override
    public IpDeployer getIpDeployer(Network network) {
        return this;
    }

    @Override
    public boolean applyPFRules(Network network, List<PortForwardingRule> rules) throws ResourceUnavailableException {
        if (!canHandle(network, Network.Service.PortForwarding)) {
            return false;
        }
        return applyPFRulesInternal(network, rules);
    }

    private boolean addOrRemovePFRuleOnNetris(UserVm vm, PortForwardingRule rule, NetrisNetworkRule networkRule, SDNProviderOpObject netrisObject, boolean create) {
        logger.debug("{} port forwarding rule on Netris for VM {} to ports {} - {}",
                create ? "Creating" : "Deleting", vm.getUuid(), rule.getDestinationPortStart(), rule.getDestinationPortEnd());
        Long vpcId = netrisObject.getVpcVO() != null ? netrisObject.getVpcVO().getId() : null;
        String vpcName = netrisObject.getVpcVO() != null ? netrisObject.getVpcVO().getName() : null;
        Long networkId = netrisObject.getNetworkVO() != null ? netrisObject.getNetworkVO().getId() : null;
        String networkName = netrisObject.getNetworkVO() != null ? netrisObject.getNetworkVO().getName() : null;
        String vpcCidr = netrisObject.getVpcVO() != null ? netrisObject.getVpcVO().getCidr() : null;
        SDNProviderNetworkRule baseNetRule = networkRule.getBaseRule();
        return create ?
                netrisService.createPortForwardingRule(baseNetRule.getZoneId(), baseNetRule.getAccountId(), baseNetRule.getDomainId(),
                        vpcName, vpcId, networkName, networkId, netrisObject.isVpcResource(), vpcCidr, baseNetRule) :
                netrisService.deletePortForwardingRule(baseNetRule.getZoneId(), baseNetRule.getAccountId(), baseNetRule.getDomainId(),
                        vpcName, vpcId, networkName, networkId, netrisObject.isVpcResource(), vpcCidr, baseNetRule);
    }

    private boolean applyPFRulesInternal(Network network, List<PortForwardingRule> rules) {
        return Transaction.execute((TransactionCallback<Boolean>) status -> {
            boolean result = true;
            for (PortForwardingRule rule : rules) {
                IPAddressVO publicIp = ApiDBUtils.findIpAddressById(rule.getSourceIpAddressId());
                UserVm vm = ApiDBUtils.findUserVmById(rule.getVirtualMachineId());
                if (vm == null && rule.getState() != FirewallRule.State.Revoke) {
                    continue;
                }
                SDNProviderOpObject netrisObject = getNetrisOpObject(network);
                String publicPort = PortForwardingServiceProvider.getPublicPortRange(rule);
                String privatePort = PortForwardingServiceProvider.getPrivatePFPortRange(rule);
                FirewallRuleDetailVO ruleDetail = firewallRuleDetailsDao.findDetail(rule.getId(), ApiConstants.NETRIS_DETAIL_KEY);

                SDNProviderNetworkRule baseNetworkRule = new SDNProviderNetworkRule.Builder()
                        .setDomainId(netrisObject.getDomainId())
                .setAccountId(netrisObject.getAccountId())
                .setZoneId(netrisObject.getZoneId())
                .setNetworkResourceId(netrisObject.getNetworkResourceId())
                .setNetworkResourceName(netrisObject.getNetworkResourceName())
                .setVpcResource(netrisObject.isVpcResource())
                .setVmId(Objects.nonNull(vm) ? vm.getId() : 0)
                .setVmIp(Objects.nonNull(vm) ? vm.getPrivateIpAddress() : null)
                .setPublicIp(publicIp.getAddress().addr())
                .setPrivatePort(privatePort)
                .setPublicPort(publicPort)
                .setRuleId(rule.getId())
                .setProtocol(rule.getProtocol().toUpperCase(Locale.ROOT))
                        .build();

                NetrisNetworkRule networkRule = new NetrisNetworkRule.Builder().baseRule(baseNetworkRule).build();

                if (Arrays.asList(FirewallRule.State.Add, FirewallRule.State.Active).contains(rule.getState())) {
                    boolean pfRuleResult = addOrRemovePFRuleOnNetris(vm, rule, networkRule, netrisObject, true);
                    if (pfRuleResult) {
                        logger.debug("Port forwarding rule {} created on Netris, adding detail on firewall rules details", rule.getId());
                        if (ruleDetail == null && FirewallRule.State.Add == rule.getState()) {
                            logger.debug("Adding new firewall detail for rule {}", rule.getId());
                            firewallRuleDetailsDao.addDetail(rule.getId(), ApiConstants.NETRIS_DETAIL_KEY, "true", false);
                        } else if (ruleDetail != null) {
                            logger.debug("Updating firewall detail for rule {}", rule.getId());
                            ruleDetail.setValue("true");
                            firewallRuleDetailsDao.update(ruleDetail.getId(), ruleDetail);
                        }
                    }
                    result &= pfRuleResult;
                } else if (rule.getState() == FirewallRule.State.Revoke) {
                    boolean pfRuleResult = addOrRemovePFRuleOnNetris(vm, rule, networkRule, netrisObject, false);
                    if (pfRuleResult && ruleDetail != null) {
                        logger.debug("Updating firewall rule detail {} for rule {}, set to false", ruleDetail.getId(), rule.getId());
                        ruleDetail.setValue("false");
                        firewallRuleDetailsDao.update(ruleDetail.getId(), ruleDetail);
                    }
                    result &= pfRuleResult;
                }
            }
            return result;
        });
    }

    private SDNProviderOpObject getNetrisOpObject(Network network) {
        Pair<VpcVO, NetworkVO> vpcOrNetwork = getVpcOrNetwork(network.getVpcId(), network.getId());
        VpcVO vpc = vpcOrNetwork.first();
        NetworkVO networkVO = vpcOrNetwork.second();
        long domainId = getResourceId("domain", vpc, networkVO);
        long accountId = getResourceId("account", vpc, networkVO);
        long zoneId = getResourceId("zone", vpc, networkVO);

        return new SDNProviderOpObject.Builder()
                .vpcVO(vpc)
                .networkVO(networkVO)
                .domainId(domainId)
                .accountId(accountId)
                .zoneId(zoneId)
                .build();
    }

    public boolean applyStaticNats(Network config, List<? extends StaticNat> rules) throws ResourceUnavailableException {
        for(StaticNat staticNat : rules) {
            long sourceIpAddressId = staticNat.getSourceIpAddressId();
            IPAddressVO ipAddressVO = ipAddressDao.findByIdIncludingRemoved(sourceIpAddressId);
            VMInstanceVO vm = vmInstanceDao.findByIdIncludingRemoved(ipAddressVO.getAssociatedWithVmId());
            // floating ip is released when nic was deleted
            if (vm == null || networkModel.getNicInNetworkIncludingRemoved(vm.getId(), config.getId()) == null) {
                continue;
            }
            long vmId = vm.getId();
            Pair<VpcVO, NetworkVO> vpcOrNetwork = getVpcOrNetwork(config.getVpcId(), config.getId());
            VpcVO vpc = vpcOrNetwork.first();
            NetworkVO network = vpcOrNetwork.second();
            Long networkResourceId = Objects.nonNull(vpc) ? vpc.getId() : network.getId();
            String networkResourceName = Objects.nonNull(vpc) ? vpc.getName() : network.getName();
            boolean isVpcResource = Objects.nonNull(vpc);
            if (!staticNat.isForRevoke()) {
                return netrisService.createStaticNatRule(config.getDataCenterId(), config.getAccountId(), config.getDomainId(),
                       networkResourceName, networkResourceId, isVpcResource, vpc.getCidr(),
                        ipAddressVO.getAddress().addr(), staticNat.getDestIpAddress(), vmId);
            } else {
                return netrisService.deleteStaticNatRule(config.getDataCenterId(), config.getAccountId(), config.getDomainId(),
                        networkResourceName, networkResourceId, isVpcResource, ipAddressVO.getAddress().addr(), vmId);
            }
        }
        return false;
    }

    public Pair<VpcVO, NetworkVO> getVpcOrNetwork(Long vpcId, long networkId) {
        VpcVO vpc = null;
        NetworkVO network = null;
        if (Objects.nonNull(vpcId)) {
            vpc = vpcDao.findById(vpcId);
            if (Objects.isNull(vpc)) {
                throw new CloudRuntimeException(String.format("Failed to find VPC with id: %s", vpcId));
            }
        } else {
            network = networkDao.findById(networkId);
            if (Objects.isNull(network)) {
                throw new CloudRuntimeException(String.format("Failed to find network with id: %s", networkId));
            }
        }
        return new Pair<>(vpc, network);
    }

    private long getResourceId(String resource, VpcVO vpc, NetworkVO network) {
        switch (resource) {
            case "domain":
                return Objects.nonNull(vpc) ? vpc.getDomainId() : network.getDomainId();
            case "account":
                return Objects.nonNull(vpc) ? vpc.getAccountId() : network.getAccountId();
            case "zone":
                return Objects.nonNull(vpc) ? vpc.getZoneId() : network.getDataCenterId();
            default:
                return 0;
        }
    }

    private SDNProviderOpObject getNetrisObject(Network network) {
        Pair<VpcVO, NetworkVO> vpcOrNetwork = getVpcOrNetwork(network.getVpcId(), network.getId());
        VpcVO vpc = vpcOrNetwork.first();
        NetworkVO networkVO = vpcOrNetwork.second();
        long domainId = getResourceId("domain", vpc, networkVO);
        long accountId = getResourceId("account", vpc, networkVO);
        long zoneId = getResourceId("zone", vpc, networkVO);

        return new SDNProviderOpObject.Builder()
                .vpcVO(vpc)
                .networkVO(networkVO)
                .domainId(domainId)
                .accountId(accountId)
                .zoneId(zoneId)
                .build();
    }

    @Override
    public boolean applyLBRules(Network network, List<LoadBalancingRule> rules) throws ResourceUnavailableException {
        boolean result = true;
        for (LoadBalancingRule loadBalancingRule : rules) {
            IPAddressVO publicIp = ipAddressDao.findByIpAndDcId(network.getDataCenterId(),
                    loadBalancingRule.getSourceIp().addr());

            List<NetrisLbBackend> lbBackends = getLoadBalancerBackends(loadBalancingRule);
            SDNProviderOpObject netrisObject = getNetrisObject(network);
            SDNProviderNetworkRule baseNetworkRule = new SDNProviderNetworkRule.Builder()
                    .setDomainId(netrisObject.getDomainId())
                    .setAccountId(netrisObject.getAccountId())
                    .setZoneId(netrisObject.getZoneId())
                    .setNetworkResourceId(netrisObject.getNetworkResourceId())
                    .setNetworkResourceName(netrisObject.getNetworkResourceName())
                    .setVpcResource(netrisObject.isVpcResource())
                    .setPublicIp(LoadBalancerContainer.Scheme.Public == loadBalancingRule.getScheme() ?
                            publicIp.getAddress().addr() : loadBalancingRule.getSourceIp().addr())
                    .setPrivatePort(String.valueOf(loadBalancingRule.getDefaultPortStart()))
                    .setPublicPort(String.valueOf(loadBalancingRule.getSourcePortStart()))
                    .setRuleId(loadBalancingRule.getId())
                    .setProtocol(loadBalancingRule.getProtocol().toUpperCase(Locale.ROOT))
                    .setAlgorithm(loadBalancingRule.getAlgorithm())
                    .build();

            NetrisNetworkRule networkRule = new NetrisNetworkRule.Builder()
                    .baseRule(baseNetworkRule)
                    .lbBackends(lbBackends)
                    .lbCidrList(loadBalancingRule.getCidrList())
                    .lbRuleName(loadBalancingRule.getName())
                    .build();
            if (Arrays.asList(FirewallRule.State.Add, FirewallRule.State.Active).contains(loadBalancingRule.getState())) {
                result &= netrisService.createOrUpdateLbRule(networkRule);
            } else if (loadBalancingRule.getState() == FirewallRule.State.Revoke) {
                result &= netrisService.deleteLbRule(networkRule);
            }
        }
        return result;
    }

    private List<NetrisLbBackend> getLoadBalancerBackends(LoadBalancingRule lbRule) {
        List<LoadBalancerVMMapVO> lbVms = lbVmMapDao.listByLoadBalancerId(lbRule.getId(), false);
        List<NetrisLbBackend> lbMembers = new ArrayList<>();

        for (LoadBalancerVMMapVO lbVm : lbVms) {
            NetrisLbBackend member = new NetrisLbBackend(lbVm.getInstanceId(), lbVm.getInstanceIp(), lbRule.getDefaultPortStart());
            lbMembers.add(member);
        }
        return lbMembers;
    }

    @Override
    public boolean validateLBRule(Network network, LoadBalancingRule rule) {
        return true;
    }

    @Override
    public List<LoadBalancerTO> updateHealthChecks(Network network, List<LoadBalancingRule> lbrules) {
        return List.of();
    }

    @Override
    public boolean handlesOnlyRulesInTransitionState() {
        return false;
    }
}
