//
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
//

package com.cloud.network.element;

import com.cloud.agent.AgentManager;
import com.cloud.agent.api.Answer;
import com.cloud.agent.api.StartupCommand;
import com.cloud.agent.api.StartupVspCommand;
import com.cloud.agent.api.element.ApplyAclRuleVspCommand;
import com.cloud.agent.api.element.ApplyStaticNatVspCommand;
import com.cloud.agent.api.element.ImplementVspCommand;
import com.cloud.agent.api.element.ShutDownVpcVspCommand;
import com.cloud.dc.VlanVO;
import com.cloud.dc.dao.DataCenterDao;
import com.cloud.dc.dao.VlanDao;
import com.cloud.deploy.DeployDestination;
import com.cloud.domain.Domain;
import com.cloud.domain.dao.DomainDao;
import com.cloud.exception.ConcurrentOperationException;
import com.cloud.exception.InsufficientCapacityException;
import com.cloud.exception.ResourceUnavailableException;
import com.cloud.host.Host;
import com.cloud.host.HostVO;
import com.cloud.host.dao.HostDao;
import com.cloud.network.Network;
import com.cloud.network.Network.Capability;
import com.cloud.network.Network.Provider;
import com.cloud.network.Network.Service;
import com.cloud.network.NetworkModel;
import com.cloud.network.Networks;
import com.cloud.network.NuageVspDeviceVO;
import com.cloud.network.PhysicalNetwork;
import com.cloud.network.PhysicalNetworkServiceProvider;
import com.cloud.network.PublicIpAddress;
import com.cloud.network.dao.FirewallRulesCidrsDao;
import com.cloud.network.dao.FirewallRulesDao;
import com.cloud.network.dao.IPAddressDao;
import com.cloud.network.dao.IPAddressVO;
import com.cloud.network.dao.NetworkDao;
import com.cloud.network.dao.NetworkServiceMapDao;
import com.cloud.network.dao.NuageVspDao;
import com.cloud.network.dao.PhysicalNetworkDao;
import com.cloud.network.dao.PhysicalNetworkVO;
import com.cloud.network.manager.NuageVspManager;
import com.cloud.network.manager.NuageVspManagerImpl;
import com.cloud.network.rules.FirewallRule;
import com.cloud.network.rules.FirewallRule.FirewallRuleType;
import com.cloud.network.rules.FirewallRuleVO;
import com.cloud.network.rules.StaticNat;
import com.cloud.network.vpc.NetworkACLItem;
import com.cloud.network.vpc.NetworkACLItemDao;
import com.cloud.network.vpc.NetworkACLItemVO;
import com.cloud.network.vpc.PrivateGateway;
import com.cloud.network.vpc.StaticRouteProfile;
import com.cloud.network.vpc.Vpc;
import com.cloud.network.vpc.VpcOfferingServiceMapVO;
import com.cloud.network.vpc.dao.VpcDao;
import com.cloud.network.vpc.dao.VpcOfferingServiceMapDao;
import com.cloud.offering.NetworkOffering;
import com.cloud.offerings.dao.NetworkOfferingDao;
import com.cloud.offerings.dao.NetworkOfferingServiceMapDao;
import com.cloud.resource.ResourceManager;
import com.cloud.resource.ResourceStateAdapter;
import com.cloud.resource.ServerResource;
import com.cloud.resource.UnableDeleteHostException;
import com.cloud.utils.component.AdapterBase;
import com.cloud.utils.exception.CloudRuntimeException;
import com.cloud.vm.DomainRouterVO;
import com.cloud.vm.NicProfile;
import com.cloud.vm.NicVO;
import com.cloud.vm.ReservationContext;
import com.cloud.vm.VirtualMachineProfile;
import com.cloud.vm.dao.DomainRouterDao;
import com.cloud.vm.dao.NicDao;
import com.google.common.base.Function;
import com.google.common.collect.Lists;
import com.cloud.util.NuageVspEntityBuilder;
import net.nuage.vsp.acs.client.api.model.VspAclRule;
import net.nuage.vsp.acs.client.api.model.VspNetwork;
import net.nuage.vsp.acs.client.api.model.VspStaticNat;
import org.apache.cloudstack.api.InternalIdentity;
import org.apache.cloudstack.framework.config.dao.ConfigurationDao;
import org.apache.cloudstack.network.ExternalNetworkDeviceManager;
import org.apache.cloudstack.resourcedetail.VpcDetailVO;
import org.apache.cloudstack.resourcedetail.dao.VpcDetailsDao;
import org.apache.commons.collections.CollectionUtils;
import org.apache.log4j.Logger;

import javax.annotation.Nullable;
import javax.inject.Inject;
import javax.naming.ConfigurationException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class NuageVspElement extends AdapterBase implements ConnectivityProvider, IpDeployer, SourceNatServiceProvider, StaticNatServiceProvider, FirewallServiceProvider,
        DhcpServiceProvider, ResourceStateAdapter, VpcProvider, NetworkACLServiceProvider {

    private static final Logger s_logger = Logger.getLogger(NuageVspElement.class);

    private static final Map<Service, Map<Capability, String>> capabilities = setCapabilities();

    public static final ExternalNetworkDeviceManager.NetworkDevice NuageVspDevice = new ExternalNetworkDeviceManager.NetworkDevice("NuageVsp", Provider.NuageVsp.getName());

    @Inject
    ResourceManager _resourceMgr;
    @Inject
    HostDao _hostDao;
    @Inject
    NetworkModel _networkModel;
    @Inject
    NetworkServiceMapDao _ntwkSrvcDao;
    @Inject
    NuageVspDao _nuageVspDao;
    @Inject
    NetworkDao _networkDao;
    @Inject
    DomainDao _domainDao;
    @Inject
    DataCenterDao _dcDao;
    @Inject
    IPAddressDao _ipAddressDao;
    @Inject
    VlanDao _vlanDao;
    @Inject
    NicDao _nicDao;
    @Inject
    VpcDao _vpcDao;
    @Inject
    VpcOfferingServiceMapDao _vpcOfferingSrvcDao;
    @Inject
    NetworkOfferingServiceMapDao _ntwkOfferingSrvcDao;
    @Inject
    AgentManager _agentMgr;
    @Inject
    NetworkOfferingDao _ntwkOfferingDao;
    @Inject
    ConfigurationDao _configDao;
    @Inject
    NuageVspManager _nuageVspManager;
    @Inject
    FirewallRulesDao _firewallRulesDao;
    @Inject
    FirewallRulesCidrsDao _firewallRulesCidrsDao;
    @Inject
    PhysicalNetworkDao _physicalNetworkDao;
    @Inject
    NetworkACLItemDao _networkACLItemDao;
    @Inject
    NuageVspEntityBuilder _nuageVspEntityBuilder;
    @Inject
    VpcDetailsDao _vpcDetailsDao;
    @Inject
    DomainRouterDao _routerDao;

    @Override
    public boolean applyIps(Network network, List<? extends PublicIpAddress> ipAddress, Set<Service> service) throws ResourceUnavailableException {
        return false;
    }

    @Override
    public Map<Service, Map<Capability, String>> getCapabilities() {
        return capabilities;
    }

    private static Map<Service, Map<Capability, String>> setCapabilities() {
        Map<Service, Map<Capability, String>> capabilities = new HashMap<Service, Map<Capability, String>>();

        // L2 Support : SDN provisioning
        capabilities.put(Service.Connectivity, null);

        // L3 Support : Generic
        capabilities.put(Service.Gateway, null);

        // Security Group
        capabilities.put(Service.SecurityGroup, null);

        // L3 Support : SourceNat
        Map<Capability, String> sourceNatCapabilities = new HashMap<Capability, String>();
        sourceNatCapabilities.put(Capability.SupportedSourceNatTypes, "perzone");
        sourceNatCapabilities.put(Capability.RedundantRouter, "false");
        capabilities.put(Service.SourceNat, sourceNatCapabilities);

        // L3 support : StaticNat
        capabilities.put(Service.StaticNat, null);

        // Set capabilities for Firewall service
        Map<Capability, String> firewallCapabilities = new HashMap<Capability, String>();
        firewallCapabilities.put(Capability.TrafficStatistics, "per public ip");
        firewallCapabilities.put(Capability.SupportedProtocols, "tcp,udp,icmp");
        firewallCapabilities.put(Capability.SupportedEgressProtocols, "tcp,udp,icmp, all");
        firewallCapabilities.put(Capability.SupportedTrafficDirection, "ingress, egress");
        firewallCapabilities.put(Capability.MultipleIps, "true");
        capabilities.put(Service.Firewall, firewallCapabilities);

        // L3 Support : DHCP
        Map<Capability, String> dhcpCapabilities = new HashMap<Capability, String>();
        capabilities.put(Service.Dhcp, dhcpCapabilities);

        //add network ACL capability
        Map<Network.Capability, String> networkACLCapabilities = new HashMap<Network.Capability, String>();
        networkACLCapabilities.put(Network.Capability.SupportedProtocols, "tcp,udp,icmp");
        capabilities.put(Network.Service.NetworkACL, networkACLCapabilities);

        return capabilities;
    }

    @Override
    public Provider getProvider() {
        return Provider.NuageVsp;
    }

    @Override
    public boolean configure(String name, Map<String, Object> params) throws ConfigurationException {
        super.configure(name, params);
        _resourceMgr.registerResourceStateAdapter(name, this);
        return true;
    }

    @Override
    public boolean implement(Network network, NetworkOffering offering, DeployDestination dest, ReservationContext context) throws ConcurrentOperationException,
            ResourceUnavailableException, InsufficientCapacityException {
        if (s_logger.isDebugEnabled()) {
            s_logger.debug("Entering NuageElement implement function for network " + network.getDisplayText() + " (state " + network.getState() + ")");
        }

        if (network.getVpcId() != null) {
            return applyACLRulesForVpc(network, offering);
        }

        if (!canHandle(network, Service.Connectivity)) {
            return false;
        }

        if (network.getBroadcastUri() == null) {
            s_logger.error("Nic has no broadcast Uri with the virtual router IP");
            return false;
        }


        VspNetwork vspNetwork = _nuageVspEntityBuilder.buildVspNetwork(network, false);
        List<String> dnsServers = _nuageVspManager.getDnsDetails(network);
        List<VspAclRule> ingressFirewallRules = getFirewallRulesToApply(network, FirewallRule.TrafficType.Ingress);
        List<VspAclRule> egressFirewallRules = getFirewallRulesToApply(network, FirewallRule.TrafficType.Egress);

        List<IPAddressVO> ips = _ipAddressDao.listStaticNatPublicIps(network.getId());
        List<String> floatingIpUuids = new ArrayList<String>();
        for (IPAddressVO ip : ips) {
            floatingIpUuids.add(ip.getUuid());
        }

        HostVO nuageVspHost = getNuageVspHost(network.getPhysicalNetworkId());
        ImplementVspCommand cmd = new ImplementVspCommand(vspNetwork, dnsServers, ingressFirewallRules, egressFirewallRules, floatingIpUuids);
        Answer answer = _agentMgr.easySend(nuageVspHost.getId(), cmd);
        if (answer == null || !answer.getResult()) {
            s_logger.error("ImplementVspCommand for network " + network.getUuid() + " failed on Nuage VSD " + nuageVspHost.getDetail("hostname"));
            if ((null != answer) && (null != answer.getDetails())) {
                throw new ResourceUnavailableException(answer.getDetails(), Network.class, network.getId());
            }
        }

        return true;
    }

    private boolean applyACLRulesForVpc(Network network, NetworkOffering offering) throws ResourceUnavailableException {
        List<NetworkACLItemVO> rules = _networkACLItemDao.listByACL(network.getNetworkACLId());
        if (_networkModel.areServicesSupportedByNetworkOffering(offering.getId(), Network.Service.NetworkACL)) {
            applyACLRules(network, rules, true, false);
        }
        return true;
    }

    private List<VspAclRule> getFirewallRulesToApply(final Network network, FirewallRule.TrafficType trafficType) {
        List<FirewallRuleVO> firewallRulesToApply = _firewallRulesDao.listByNetworkPurposeTrafficType(network.getId(), FirewallRule.Purpose.Firewall, trafficType);
        List<VspAclRule> vspAclRulesToApply = Lists.newArrayListWithExpectedSize(firewallRulesToApply.size());

        for (FirewallRuleVO rule : firewallRulesToApply) {
            rule.setSourceCidrList(_firewallRulesCidrsDao.getSourceCidrs(rule.getId()));
            VspAclRule vspAclRule = _nuageVspEntityBuilder.buildVspAclRule(rule, network);
            vspAclRulesToApply.add(vspAclRule);
        }
        return vspAclRulesToApply;
    }

    @Override
    public boolean prepare(Network network, NicProfile nic, VirtualMachineProfile vm, DeployDestination dest, ReservationContext context) throws ConcurrentOperationException,
            ResourceUnavailableException, InsufficientCapacityException {
        if (!canHandle(network, Service.Connectivity)) {
            return false;
        }

        if (network.getBroadcastUri() == null) {
            s_logger.error("Nic has no broadcast Uri with the virtual router IP");
            return false;
        }

        return true;
    }

    @Override
    public boolean release(Network network, NicProfile nic, VirtualMachineProfile vm, ReservationContext context) throws ConcurrentOperationException, ResourceUnavailableException {
        if (!canHandle(network, Service.Connectivity)) {
            return false;
        }

        if (network.getBroadcastUri() == null) {
            s_logger.error("Nic has no broadcast Uri with the virtual router IP");
            return false;
        }

        return true;
    }

    @Override
    public boolean shutdown(Network network, ReservationContext context, boolean cleanup) throws ConcurrentOperationException, ResourceUnavailableException {
        if (!canHandle(network, Service.Connectivity)) {
            return false;
        }

        return true;
    }

    @Override
    public boolean isReady(PhysicalNetworkServiceProvider provider) {
        return true;
    }

    @Override
    public boolean shutdownProviderInstances(PhysicalNetworkServiceProvider provider, ReservationContext context) throws ConcurrentOperationException, ResourceUnavailableException {
        return true;
    }

    @Override
    public boolean canEnableIndividualServices() {
        return true;
    }

    @Override
    public boolean destroy(Network network, ReservationContext context) throws ConcurrentOperationException, ResourceUnavailableException {
        if (!canHandle(network, Service.Connectivity)) {
            return false;
        }

        return true;
    }

    @Override
    public boolean verifyServicesCombination(Set<Service> services) {
        // This element can only function in a NuageVsp based
        // SDN network, so Connectivity needs to be present here
        if (!services.contains(Service.Connectivity)) {
            s_logger.warn("Unable to support services combination without Connectivity service provided by Nuage VSP.");
            return false;
        }

        if ((services.contains(Service.StaticNat)) && (!services.contains(Service.SourceNat))) {
            s_logger.warn("Unable to provide StaticNat without the SourceNat service.");
            return false;
        }

        if (services.contains(Service.Vpn) || services.contains(Service.Dns) || services.contains(Service.Lb) || services.contains(Service.PortForwarding)
                || services.contains(Service.SecurityGroup)) {
            // NuageVsp doesn't implement any of these services, and we don't
            // want anyone else to do it for us. So if these services
            // exist, we can't handle it.
            s_logger.warn("Unable to support services combination. The services list contains service(s) not supported by Nuage VSP.");
            return false;
        }

        return true;
    }

    protected boolean canHandle(Network network, Service service) {

        if (network.getBroadcastDomainType() != Networks.BroadcastDomainType.Vsp) {
            return false;
        }

        if (!_networkModel.isProviderForNetwork(getProvider(), network.getId())) {
            if (s_logger.isDebugEnabled()) {
                s_logger.debug("NuageVsp is not a provider for network " + network.getDisplayText());
            }
            return false;
        }

        if (service != null) {
            if (!_ntwkSrvcDao.canProviderSupportServiceInNetwork(network.getId(), service, getProvider())) {
                if (s_logger.isDebugEnabled()) {
                    s_logger.debug("NuageVsp can't provide the " + service.getName() + " service on network " + network.getDisplayText());
                }
                return false;
            }
        }

        if (network.getVpcId() != null) {
            NetworkOffering networkOffering = _ntwkOfferingDao.findById(network.getNetworkOfferingId());
            if (!networkOffering.getIsPersistent()) {
                if (s_logger.isDebugEnabled()) {
                    s_logger.debug("NuageVsp can't handle VPC tiers which use a network offering which are not persistent");
                }
                return false;
            }
        }

        return true;
    }

    @Override
    public boolean addDhcpEntry(Network network, NicProfile nic, VirtualMachineProfile vm, DeployDestination dest, ReservationContext context) throws ConcurrentOperationException,
            InsufficientCapacityException, ResourceUnavailableException {
        return true;
    }

    @Override
    public boolean configDhcpSupportForSubnet(Network network, NicProfile nic, VirtualMachineProfile vm, DeployDestination dest, ReservationContext context)
            throws ConcurrentOperationException, InsufficientCapacityException, ResourceUnavailableException {
        return true;
    }

    @Override
    public boolean removeDhcpSupportForSubnet(Network network) throws ResourceUnavailableException {
        return true;
    }

    @Override
    public boolean applyStaticNats(Network config, List<? extends StaticNat> rules) throws ResourceUnavailableException {
        List<VspStaticNat> vspStaticNatDetails = new ArrayList<VspStaticNat>();
        for (StaticNat staticNat : rules) {
            IPAddressVO sourceNatIp = _ipAddressDao.findById(staticNat.getSourceIpAddressId());
            VlanVO sourceNatVlan = _vlanDao.findById(sourceNatIp.getVlanId());
            NicVO nicVO = _nicDao.findByIp4AddressAndNetworkId(staticNat.getDestIpAddress(), staticNat.getNetworkId());
            VspStaticNat vspStaticNat = _nuageVspEntityBuilder.buildVspStaticNat(staticNat.isForRevoke(), sourceNatIp, sourceNatVlan, nicVO);
            vspStaticNatDetails.add(vspStaticNat);
        }

        VspNetwork vspNetwork = _nuageVspEntityBuilder.buildVspNetwork(config, false);
        HostVO nuageVspHost = getNuageVspHost(config.getPhysicalNetworkId());
        ApplyStaticNatVspCommand cmd = new ApplyStaticNatVspCommand(vspNetwork, vspStaticNatDetails);
        Answer answer = _agentMgr.easySend(nuageVspHost.getId(), cmd);
        if (answer == null || !answer.getResult()) {
            s_logger.error("ApplyStaticNatNuageVspCommand for network " + config.getUuid() + " failed on Nuage VSD " + nuageVspHost.getDetail("hostname"));
            if ((null != answer) && (null != answer.getDetails())) {
                throw new ResourceUnavailableException(answer.getDetails(), Network.class, config.getId());
            }
        }

        return true;
    }

    @Override
    public IpDeployer getIpDeployer(Network network) {
        return this;
    }

    @Override
    public boolean applyFWRules(Network network, List<? extends FirewallRule> rules) throws ResourceUnavailableException {
        if (rules == null || rules.isEmpty()) {
            return true;
        }

        if (rules.size() == 1 && rules.iterator().next().getType().equals(FirewallRuleType.System)) {
            if (s_logger.isDebugEnabled()) {
                s_logger.debug("Default ACL added by CS as system is ignored for network " + network.getName() + " with rule " + rules);
            }
            return true;
        }

        s_logger.info("Applying " + rules.size() + " Firewall Rules for network " + network.getName());
        return applyACLRules(network, rules, false, false);
    }

    protected boolean applyACLRules(final Network network, List<? extends InternalIdentity> rules, boolean isNetworkAcl, boolean networkReset)
            throws ResourceUnavailableException {
        VspNetwork vspNetwork = _nuageVspEntityBuilder.buildVspNetwork(network, false);
        List<VspAclRule> vspAclRules = Lists.transform(rules, new Function<InternalIdentity, VspAclRule>() {
            @Nullable
            @Override
            public VspAclRule apply(@Nullable InternalIdentity input) {
                if (input instanceof FirewallRule) {
                    return _nuageVspEntityBuilder.buildVspAclRule((FirewallRule) input, network);
                }
                return _nuageVspEntityBuilder.buildVspAclRule((NetworkACLItem) input);
            }
        });

        HostVO nuageVspHost = getNuageVspHost(network.getPhysicalNetworkId());
        VspAclRule.ACLType vspAclType = isNetworkAcl ? VspAclRule.ACLType.NetworkACL : VspAclRule.ACLType.Firewall;
        ApplyAclRuleVspCommand cmd = new ApplyAclRuleVspCommand(vspAclType, vspNetwork, vspAclRules, networkReset);
        Answer answer = _agentMgr.easySend(nuageVspHost.getId(), cmd);
        if (answer == null || !answer.getResult()) {
            s_logger.error("ApplyAclRuleNuageVspCommand for network " + network.getUuid() + " failed on Nuage VSD " + nuageVspHost.getDetail("hostname"));
            if ((null != answer) && (null != answer.getDetails())) {
                throw new ResourceUnavailableException(answer.getDetails(), Network.class, network.getId());
            }
        }
        return true;
    }

    @Override
    public boolean applyNetworkACLs(Network config, List<? extends NetworkACLItem> rules) throws ResourceUnavailableException {
        if (rules == null || rules.isEmpty()) {
            if (s_logger.isDebugEnabled()) {
                s_logger.debug("No rules to apply. So, delete all the existing ACL in VSP from Subnet with uuid " + config.getUuid());
            }
        } else {
            if (s_logger.isDebugEnabled()) {
                s_logger.debug("New rules has to applied. So, delete all the existing ACL in VSP from Subnet with uuid " + config.getUuid());
            }
        }
        if (rules != null) {
            s_logger.info("Applying " + rules.size() + " Network ACLs for network " + config.getName());
            applyACLRules(config, rules, true, rules.isEmpty());
        }
        return true;
    }

    @Override
    public boolean implementVpc(Vpc vpc, DeployDestination dest, ReservationContext context) throws ConcurrentOperationException, ResourceUnavailableException, InsufficientCapacityException {
        List<VpcOfferingServiceMapVO> vpcOfferingServices = _vpcOfferingSrvcDao.listByVpcOffId(vpc.getVpcOfferingId());
        Map<Network.Service, Set<Network.Provider>> supportedVpcServices = NuageVspManagerImpl.NUAGE_VSP_VPC_SERVICE_MAP;
        for (VpcOfferingServiceMapVO vpcOfferingService : vpcOfferingServices) {
            Network.Service service = Network.Service.getService(vpcOfferingService.getService());
            if (!supportedVpcServices.containsKey(service)) {
                s_logger.warn(String.format("NuageVsp doesn't support service %s for VPCs", service.getName()));
                return false;
            }

            Network.Provider provider = Network.Provider.getProvider(vpcOfferingService.getProvider());
            if (!supportedVpcServices.get(service).contains(provider)) {
                s_logger.warn(String.format("NuageVsp doesn't support provider %s for service %s for VPCs", provider.getName(), service.getName()));
                return false;
            }
        }

        return true;
    }

    @Override
    public boolean shutdownVpc(Vpc vpc, ReservationContext context) throws ConcurrentOperationException, ResourceUnavailableException {
        if (vpc.getState().equals(Vpc.State.Inactive)) {
            List<DomainRouterVO> routers = _routerDao.listByVpcId(vpc.getId());
            if (CollectionUtils.isEmpty(routers)) {
                routers = _routerDao.listIncludingRemovedByVpcId(vpc.getId());
            }

            List<String> domainRouterUuids = Lists.transform(routers, new Function<DomainRouterVO, String>() {
                @Nullable
                @Override
                public String apply(@Nullable DomainRouterVO input) {
                    return input != null ? input.getUuid() : null;
                }
            });

            Domain vpcDomain = _domainDao.findById(vpc.getDomainId());
            HostVO nuageVspHost = getNuageVspHost(getPhysicalNetworkId(vpc.getZoneId()));

            String preConfiguredDomainTemplateName;
            VpcDetailVO domainTemplateNameDetail = _vpcDetailsDao.findDetail(vpc.getId(), NuageVspManager.nuageDomainTemplateDetailName);
            if (domainTemplateNameDetail != null) {
                preConfiguredDomainTemplateName = domainTemplateNameDetail.getValue();
            } else {
                preConfiguredDomainTemplateName = _configDao.getValue(NuageVspManager.NuageVspVpcDomainTemplateName.key());
            }

            ShutDownVpcVspCommand cmd = new ShutDownVpcVspCommand(vpcDomain.getUuid(), vpc.getUuid(), preConfiguredDomainTemplateName, domainRouterUuids);
            Answer answer =  _agentMgr.easySend(nuageVspHost.getId(), cmd);
            if (answer == null || !answer.getResult()) {
                s_logger.error("ShutDownVpcVspCommand for VPC " + vpc.getUuid() + " failed on Nuage VSD " + nuageVspHost.getDetail("hostname"));
                if ((null != answer) && (null != answer.getDetails())) {
                    throw new ResourceUnavailableException(answer.getDetails(), Vpc.class, vpc.getId());
                }
            }
        }
        return true;
    }

    private Long getPhysicalNetworkId(Long zoneId) {
        Long guestPhysicalNetworkId = 0L;
        List<PhysicalNetworkVO> physicalNetworkList = _physicalNetworkDao.listByZone(zoneId);
        for (PhysicalNetworkVO phyNtwk : physicalNetworkList) {
            if (phyNtwk.getIsolationMethods().contains(PhysicalNetwork.IsolationMethod.VSP.name())) {
                guestPhysicalNetworkId = phyNtwk.getId();
                break;
            }
        }
        return guestPhysicalNetworkId;
    }

    @Override
    public boolean createPrivateGateway(PrivateGateway gateway) throws ConcurrentOperationException, ResourceUnavailableException {
        return false;
    }

    @Override
    public boolean deletePrivateGateway(PrivateGateway privateGateway) throws ConcurrentOperationException, ResourceUnavailableException {
        return false;
    }

    @Override
    public boolean applyStaticRoutes(Vpc vpc, List<StaticRouteProfile> routes) throws ResourceUnavailableException {
        return true;
    }

    @Override
    public boolean applyACLItemsToPrivateGw(PrivateGateway gateway, List<? extends NetworkACLItem> rules) throws ResourceUnavailableException {
        return false;
    }

    @Override
    public HostVO createHostVOForConnectedAgent(HostVO host, StartupCommand[] cmd) {
        return null;
    }

    @Override
    public HostVO createHostVOForDirectConnectAgent(HostVO host, StartupCommand[] startup, ServerResource resource, Map<String, String> details, List<String> hostTags) {
        if (!(startup[0] instanceof StartupVspCommand)) {
            return null;
        }
        host.setType(Host.Type.L2Networking);
        return host;
    }

    @Override
    public DeleteHostAnswer deleteHost(HostVO host, boolean isForced, boolean isForceDeleteStorage) throws UnableDeleteHostException {
        if (!(host.getType() == Host.Type.L2Networking)) {
            return null;
        }
        return new DeleteHostAnswer(true);
    }

    private HostVO getNuageVspHost(Long physicalNetworkId) {
        HostVO nuageVspHost;
        List<NuageVspDeviceVO> nuageVspDevices = _nuageVspDao.listByPhysicalNetwork(physicalNetworkId);
        if (nuageVspDevices != null && (!nuageVspDevices.isEmpty())) {
            NuageVspDeviceVO config = nuageVspDevices.iterator().next();
            nuageVspHost = _hostDao.findById(config.getHostId());
            _hostDao.loadDetails(nuageVspHost);
        } else {
            throw new CloudRuntimeException("There is no Nuage VSP device configured on physical network " + physicalNetworkId);
        }
        return nuageVspHost;
    }
}