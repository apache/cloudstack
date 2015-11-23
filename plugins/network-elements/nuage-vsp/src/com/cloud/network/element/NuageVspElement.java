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
import com.cloud.network.vpc.dao.VpcDao;
import com.cloud.offering.NetworkOffering;
import com.cloud.offerings.NetworkOfferingVO;
import com.cloud.offerings.dao.NetworkOfferingDao;
import com.cloud.offerings.dao.NetworkOfferingServiceMapDao;
import com.cloud.resource.ResourceManager;
import com.cloud.resource.ResourceStateAdapter;
import com.cloud.resource.ServerResource;
import com.cloud.resource.UnableDeleteHostException;
import com.cloud.util.NuageVspUtil;
import com.cloud.utils.component.AdapterBase;
import com.cloud.utils.exception.CloudRuntimeException;
import com.cloud.vm.NicProfile;
import com.cloud.vm.NicVO;
import com.cloud.vm.ReservationContext;
import com.cloud.vm.VirtualMachineProfile;
import com.cloud.vm.dao.NicDao;
import com.google.common.base.Function;
import com.google.common.collect.Lists;
import org.apache.cloudstack.api.InternalIdentity;
import org.apache.cloudstack.framework.config.dao.ConfigurationDao;
import org.apache.cloudstack.network.ExternalNetworkDeviceManager;
import org.apache.log4j.Logger;

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
    protected DomainDao _domainDao;
    @Inject
    protected DataCenterDao _dcDao;
    @Inject
    IPAddressDao _ipAddressDao;
    @Inject
    VlanDao _vlanDao;
    @Inject
    NicDao _nicDao;
    @Inject
    VpcDao _vpcDao;
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

        boolean egressDefaultPolicy = offering.getEgressDefaultPolicy();
        Domain networkDomain = _domainDao.findById(network.getDomainId());
        boolean isFirewallServiceSupported = _networkModel.areServicesSupportedByNetworkOffering(offering.getId(), Service.Firewall);
        List<String> dnsServers = _nuageVspManager.getDnsDetails(network);

        boolean isL2Network = false, isL3Network = false, isShared = false;
        String subnetUuid = network.getUuid();
        if (offering.getGuestType() == Network.GuestType.Shared) {
            isShared = true;
            subnetUuid = networkDomain.getUuid();
        } else if (_ntwkOfferingSrvcDao.areServicesSupportedByNetworkOffering(offering.getId(), Service.SourceNat)
                || _ntwkOfferingSrvcDao.areServicesSupportedByNetworkOffering(offering.getId(), Service.StaticNat)
                || _ntwkOfferingSrvcDao.areServicesSupportedByNetworkOffering(offering.getId(), Service.Connectivity)) {
            isL3Network = true;
        } else {
            isL2Network = true;
        }

        String preConfiguredDomainTemplateName = NuageVspUtil.getPreConfiguredDomainTemplateName(_configDao, network, offering);
        List<Map<String, Object>> ingressFirewallRules = getFirewallRulesToApply(network.getId(), FirewallRule.TrafficType.Ingress, egressDefaultPolicy);
        List<Map<String, Object>> egressFirewallRules = getFirewallRulesToApply(network.getId(), FirewallRule.TrafficType.Egress, egressDefaultPolicy);

        List<IPAddressVO> ips = _ipAddressDao.listStaticNatPublicIps(network.getId());
        List<String> acsFipUuid = new ArrayList<String>();
        for (IPAddressVO ip : ips) {
            acsFipUuid.add(ip.getUuid());
        }

        HostVO nuageVspHost = getNuageVspHost(network.getPhysicalNetworkId());
        ImplementVspCommand.Builder cmdBuilder = new ImplementVspCommand.Builder()
                .networkId(network.getId())
                .networkDomainUuid(networkDomain.getUuid())
                .networkUuid(network.getUuid())
                .networkName(network.getName())
                .vpcOrSubnetUuid(subnetUuid)
                .isL2Network(isL2Network)
                .isL3Network(isL3Network)
                .isVpc(false)
                .isShared(isShared)
                .domainTemplateName(preConfiguredDomainTemplateName)
                .isFirewallServiceSupported(isFirewallServiceSupported)
                .dnsServers(dnsServers)
                .ingressFirewallRules(ingressFirewallRules)
                .egressFirewallRules(egressFirewallRules)
                .acsFipUuid(acsFipUuid)
                .egressDefaultPolicy(egressDefaultPolicy);
        Answer answer = _agentMgr.easySend(nuageVspHost.getId(), cmdBuilder.build());
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
            applyACLRules(network, rules, true, null, false);
        }
        return true;
    }

    private List<Map<String, Object>> getFirewallRulesToApply(long networkId, FirewallRule.TrafficType trafficType, final boolean egressDefaultPolicy) {
        List<FirewallRuleVO> firewallRulesToApply = _firewallRulesDao.listByNetworkPurposeTrafficType(networkId, FirewallRule.Purpose.Firewall, trafficType);
        for (FirewallRuleVO rule : firewallRulesToApply) {
            // load cidrs if any
            rule.setSourceCidrList(_firewallRulesCidrsDao.getSourceCidrs(rule.getId()));
        }
        return Lists.transform(firewallRulesToApply, new Function<FirewallRuleVO, Map<String, Object>>() {
            @Override
            public Map<String, Object> apply(FirewallRuleVO firewallRuleVO) {
                return getACLRuleDetails(firewallRuleVO, egressDefaultPolicy);
            }
        });
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
        //Check if the network is associated to a VPC
        Long vpcId = config.getVpcId();
        String vpcOrSubnetUuid = null;
        if (vpcId != null) {
            Vpc vpcObj = _vpcDao.findById(vpcId);
            vpcOrSubnetUuid = vpcObj.getUuid();
        } else {
            vpcOrSubnetUuid = config.getUuid();
        }
        Domain networkDomain = _domainDao.findById(config.getDomainId());

        long networkOfferingId = _ntwkOfferingDao.findById(config.getNetworkOfferingId()).getId();
        boolean isL3Network = isL3Network(networkOfferingId);

        List<Map<String, Object>> sourceNatDetails = new ArrayList<Map<String, Object>>();
        for (StaticNat staticNat : rules) {
            Map<String, Object> sourceNatDetail = new HashMap<String, Object>();
            IPAddressVO sourceNatIp = _ipAddressDao.findById(staticNat.getSourceIpAddressId());
            VlanVO sourceNatVan = _vlanDao.findById(sourceNatIp.getVlanId());
            NicVO nicVO = _nicDao.findByIp4AddressAndNetworkId(staticNat.getDestIpAddress(), staticNat.getNetworkId());
            //Just get all the information about the sourceNat which will be used by NuageVsp
            //client to process the request
            sourceNatDetail.put("sourceNatIpUuid", sourceNatIp.getUuid());
            sourceNatDetail.put("sourceNatIpAddress", sourceNatIp.getAddress().addr());
            sourceNatDetail.put("nicUuid", nicVO == null ? null : nicVO.getUuid());
            sourceNatDetail.put("nicMacAddress", nicVO == null ? null : nicVO.getMacAddress());
            sourceNatDetail.put("isRevoke", staticNat.isForRevoke());
            sourceNatDetail.put("sourceNatVlanUuid", sourceNatVan.getUuid());
            sourceNatDetail.put("sourceNatVlanGateway", sourceNatVan.getVlanGateway());
            sourceNatDetail.put("sourceNatVlanNetmask", sourceNatVan.getVlanNetmask());
            sourceNatDetails.add(sourceNatDetail);
        }

        HostVO nuageVspHost = getNuageVspHost(config.getPhysicalNetworkId());
        ApplyStaticNatVspCommand.Builder cmdBuilder = new ApplyStaticNatVspCommand.Builder()
                .networkDomainUuid(networkDomain.getUuid())
                .networkUuid(config.getUuid())
                .vpcOrSubnetUuid(vpcOrSubnetUuid)
                .isL3Network(isL3Network)
                .isVpc(vpcId != null)
                .staticNatDetails(sourceNatDetails);
        Answer answer = _agentMgr.easySend(nuageVspHost.getId(), cmdBuilder.build());
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
        return applyACLRules(network, rules, false, rules.iterator().next().getTrafficType().equals(FirewallRule.TrafficType.Ingress), false);
    }

    protected boolean applyACLRules(Network network, List<? extends InternalIdentity> rules, boolean isNetworkAcl, Boolean isAcsIngressAcl, boolean networkReset)
            throws ResourceUnavailableException {
        Domain networksDomain = _domainDao.findById(network.getDomainId());
        NetworkOfferingVO networkOfferingVO = _ntwkOfferingDao.findById(network.getNetworkOfferingId());
        Long vpcId = network.getVpcId();
        String vpcOrSubnetUuid = null;
        if (vpcId != null) {
            Vpc vpcObj = _vpcDao.findById(vpcId);
            vpcOrSubnetUuid = vpcObj.getUuid();
        } else {
            vpcOrSubnetUuid = network.getUuid();
        }
        boolean egressDefaultPolicy = networkOfferingVO.getEgressDefaultPolicy();
        List<Map<String, Object>> aclRules = new ArrayList<Map<String, Object>>();
        for (InternalIdentity acl : rules) {
            aclRules.add(getACLRuleDetails(acl, egressDefaultPolicy));
        }

        boolean isL3Network = isL3Network(network.getNetworkOfferingId());
        HostVO nuageVspHost = getNuageVspHost(network.getPhysicalNetworkId());
        String preConfiguredDomainTemplateName = NuageVspUtil.getPreConfiguredDomainTemplateName(_configDao, network, networkOfferingVO);
        ApplyAclRuleVspCommand.Builder cmdBuilder = new ApplyAclRuleVspCommand.Builder()
                .networkAcl(isNetworkAcl)
                .networkUuid(network.getUuid())
                .networkDomainUuid(networksDomain.getUuid())
                .vpcOrSubnetUuid(vpcOrSubnetUuid)
                .networkName(network.getName())
                .isL2Network(!isL3Network)
                .aclRules(aclRules)
                .networkId(network.getId())
                .egressDefaultPolicy(networkOfferingVO.getEgressDefaultPolicy())
                .acsIngressAcl(isAcsIngressAcl)
                .networkReset(networkReset)
                .domainTemplateName(preConfiguredDomainTemplateName);
        Answer answer = _agentMgr.easySend(nuageVspHost.getId(), cmdBuilder.build());
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
            applyACLRules(config, rules, true, null, rules.isEmpty());
        }
        return true;
    }

    @Override
    public boolean implementVpc(Vpc vpc, DeployDestination dest, ReservationContext context) throws ConcurrentOperationException, ResourceUnavailableException, InsufficientCapacityException {
        return true;
    }

    @Override
    public boolean shutdownVpc(Vpc vpc, ReservationContext context) throws ConcurrentOperationException, ResourceUnavailableException {
        if (vpc.getState().equals(Vpc.State.Inactive)) {
            Domain vpcDomain = _domainDao.findById(vpc.getDomainId());
            HostVO nuageVspHost = getNuageVspHost(getPhysicalNetworkId(vpc.getZoneId()));
            String preConfiguredDomainTemplateName = _configDao.getValue(NuageVspManager.NuageVspVpcDomainTemplateName.key());
            ShutDownVpcVspCommand.Builder cmdBuilder = new ShutDownVpcVspCommand.Builder()
                    .domainUuid(vpcDomain.getUuid())
                    .vpcUuid(vpc.getUuid())
                    .domainTemplateName(preConfiguredDomainTemplateName);
            Answer answer =  _agentMgr.easySend(nuageVspHost.getId(), cmdBuilder.build());
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

    protected boolean isL3Network(Long offeringId) {
        return _ntwkOfferingSrvcDao.areServicesSupportedByNetworkOffering(offeringId, Service.SourceNat)
                || _ntwkOfferingSrvcDao.areServicesSupportedByNetworkOffering(offeringId, Service.StaticNat);
    }

    private Map<String, Object> getACLRuleDetails(Object aclRule, boolean egressDefaultPolicy) {
        Map<String, Object> aclDetails = new HashMap<String, Object>();
        if (aclRule instanceof FirewallRule) {
            FirewallRule firewallRule = (FirewallRule)aclRule;
            aclDetails.put("sourceCidrList", firewallRule.getSourceCidrList());
            aclDetails.put("uuid", firewallRule.getUuid());
            aclDetails.put("protocol", firewallRule.getProtocol());
            aclDetails.put("startPort", firewallRule.getSourcePortStart());
            aclDetails.put("endPort", firewallRule.getSourcePortEnd());
            aclDetails.put("state", firewallRule.getState().name());
            aclDetails.put("trafficType", firewallRule.getTrafficType().name());
            if (firewallRule.getSourceIpAddressId() != null) {
                //add the source IP
                IPAddressVO ipaddress = _ipAddressDao.findById(((FirewallRule)aclRule).getSourceIpAddressId());
                aclDetails.put("sourceIpAddress", ipaddress != null ? ipaddress.getVmIp() + "/32" : null);
            }
            if (firewallRule.getTrafficType().equals(FirewallRule.TrafficType.Egress) && egressDefaultPolicy) {
                aclDetails.put("action", "Deny");
            } else {
                aclDetails.put("action", "Allow");
            }
            aclDetails.put("priority", -1);
            aclDetails.put("type", "Firewall");
        } else {
            NetworkACLItem networkAcl = (NetworkACLItem)aclRule;
            aclDetails.put("sourceCidrList", networkAcl.getSourceCidrList());
            aclDetails.put("uuid", networkAcl.getUuid());
            aclDetails.put("protocol", networkAcl.getProtocol());
            aclDetails.put("startPort", networkAcl.getSourcePortStart());
            aclDetails.put("endPort", networkAcl.getSourcePortEnd());
            aclDetails.put("state", networkAcl.getState().name());
            aclDetails.put("trafficType", networkAcl.getTrafficType().name());
            //Set sourceIP to null as it is not applicable
            aclDetails.put("sourceIpAddress", null);
            aclDetails.put("action", networkAcl.getAction().name());
            aclDetails.put("priority", networkAcl.getNumber());
            aclDetails.put("type", "NetworkACL");
        }
        return aclDetails;
    }
}