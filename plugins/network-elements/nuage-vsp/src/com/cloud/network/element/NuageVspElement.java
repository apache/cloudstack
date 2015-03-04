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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.ejb.Local;
import javax.inject.Inject;
import javax.naming.ConfigurationException;

import org.apache.cloudstack.api.InternalIdentity;
import org.apache.cloudstack.network.ExternalNetworkDeviceManager;
import org.apache.log4j.Logger;

import com.cloud.agent.AgentManager;
import com.cloud.agent.api.StartupCommand;
import com.cloud.agent.api.StartupVspCommand;
import com.cloud.agent.api.element.ApplyAclRuleVspAnswer;
import com.cloud.agent.api.element.ApplyAclRuleVspCommand;
import com.cloud.agent.api.element.ApplyStaticNatVspAnswer;
import com.cloud.agent.api.element.ApplyStaticNatVspCommand;
import com.cloud.dc.VlanVO;
import com.cloud.dc.dao.DataCenterDao;
import com.cloud.dc.dao.VlanDao;
import com.cloud.deploy.DeployDestination;
import com.cloud.domain.Domain;
import com.cloud.domain.dao.DomainDao;
import com.cloud.exception.CloudException;
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
import com.cloud.network.PhysicalNetworkServiceProvider;
import com.cloud.network.PublicIpAddress;
import com.cloud.network.dao.IPAddressDao;
import com.cloud.network.dao.IPAddressVO;
import com.cloud.network.dao.NetworkDao;
import com.cloud.network.dao.NetworkServiceMapDao;
import com.cloud.network.dao.NuageVspDao;
import com.cloud.network.rules.FirewallRule;
import com.cloud.network.rules.FirewallRule.FirewallRuleType;
import com.cloud.network.rules.StaticNat;
import com.cloud.network.vpc.NetworkACLItem;
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
import com.cloud.utils.component.AdapterBase;
import com.cloud.vm.NicProfile;
import com.cloud.vm.NicVO;
import com.cloud.vm.ReservationContext;
import com.cloud.vm.VirtualMachineProfile;
import com.cloud.vm.dao.NicDao;

@Local(value = {NetworkElement.class, ConnectivityProvider.class, IpDeployer.class, SourceNatServiceProvider.class, StaticNatServiceProvider.class, FirewallServiceProvider.class,
        DhcpServiceProvider.class, NetworkACLServiceProvider.class})
public class NuageVspElement extends AdapterBase implements ConnectivityProvider, IpDeployer, SourceNatServiceProvider, StaticNatServiceProvider, FirewallServiceProvider,
        DhcpServiceProvider, NetworkACLServiceProvider, ResourceStateAdapter {

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

        // L3 Support : SourceNat
        Map<Capability, String> sourceNatCapabilities = new HashMap<Capability, String>();
        sourceNatCapabilities.put(Capability.SupportedSourceNatTypes, "peraccount");
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
        s_logger.debug("Entering NuageElement implement function for network " + network.getDisplayText() + " (state " + network.getState() + ")");

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
        return false;
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
            s_logger.debug("NuageElement is not a provider for network " + network.getDisplayText());
            return false;
        }

        if (service != null) {
            if (!_ntwkSrvcDao.canProviderSupportServiceInNetwork(network.getId(), service, getProvider())) {
                s_logger.debug("NuageElement can't provide the " + service.getName() + " service on network " + network.getDisplayText());
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
            sourceNatDetail.put("nicMacAddress", nicVO == null ? null : nicVO.getMacAddress());
            sourceNatDetail.put("isRevoke", staticNat.isForRevoke());
            sourceNatDetail.put("sourceNatVlanUuid", sourceNatVan.getUuid());
            sourceNatDetail.put("sourceNatVlanGateway", sourceNatVan.getVlanGateway());
            sourceNatDetail.put("sourceNatVlanNetmask", sourceNatVan.getVlanNetmask());
            sourceNatDetails.add(sourceNatDetail);
        }
        try {
            try {
                HostVO nuageVspHost = getNuageVspHost(config.getPhysicalNetworkId());
                ApplyStaticNatVspCommand cmd = new ApplyStaticNatVspCommand(networkDomain.getUuid(), vpcOrSubnetUuid, isL3Network, sourceNatDetails);
                ApplyStaticNatVspAnswer answer = (ApplyStaticNatVspAnswer)_agentMgr.easySend(nuageVspHost.getId(), cmd);
                if (answer == null || !answer.getResult()) {
                    s_logger.error("ApplyStaticNatNuageVspCommand for network " + config.getUuid() + " failed");
                    if ((null != answer) && (null != answer.getDetails())) {
                        throw new ResourceUnavailableException(answer.getDetails(), Network.class, config.getId());
                    }
                }
            } catch (Exception e) {
                s_logger.warn("Failed to apply static Nat in Vsp " + e.getMessage());
            }
        } catch (Exception e) {
            throw new ResourceUnavailableException("Failed to apply Static NAT in VSP", Network.class, config.getId(), e);
        }

        return true;
    }

    @Override
    public IpDeployer getIpDeployer(Network network) {
        return this;
    }

    @Override
    public boolean applyFWRules(Network network, List<? extends FirewallRule> rules) throws ResourceUnavailableException {
        s_logger.debug("Handling applyFWRules for network " + network.getName() + " with " + rules.size() + " FWRules");
        if (rules != null && rules.size() == 1 && rules.iterator().next().getType().equals(FirewallRuleType.System)) {
            s_logger.debug("Default ACL added by CS as system is ignored for network " + network.getName() + " with rule " + rules);
            return true;
        }
        return applyACLRules(network, rules, false);
    }

    @Override
    public boolean applyNetworkACLs(Network network, List<? extends NetworkACLItem> rules) throws ResourceUnavailableException {
        if (rules == null || rules.isEmpty()) {
            s_logger.debug("No rules to apply. So, delete all the existing ACL in VSP from Subnet with uuid " + network.getUuid());
        } else {
            s_logger.debug("New rules has to applied. So, delete all the existing ACL in VSP from Subnet with uuid " + network.getUuid());
        }
        if (rules != null) {
            s_logger.debug("Handling applyNetworkACLs for network " + network.getName() + " with " + rules.size() + " Network ACLs");
            applyACLRules(network, rules, true);
        }
        return true;
    }

    protected boolean applyACLRules(Network network, List<? extends InternalIdentity> rules, boolean isVpc) throws ResourceUnavailableException {
        Domain networksDomain = _domainDao.findById(network.getDomainId());
        NetworkOfferingVO networkOferringVO = _ntwkOfferingDao.findById(network.getNetworkOfferingId());
        try {
            Long vpcId = network.getVpcId();
            String vpcOrSubnetUuid = null;
            if (vpcId != null) {
                Vpc vpcObj = _vpcDao.findById(vpcId);
                vpcOrSubnetUuid = vpcObj.getUuid();
            } else {
                vpcOrSubnetUuid = network.getUuid();
            }
            boolean egressDefaultPolicy = networkOferringVO.getEgressDefaultPolicy();
            List<Map<String, Object>> aclRules = new ArrayList<Map<String, Object>>();
            for (InternalIdentity acl : rules) {
                aclRules.add(getACLRuleDetails(acl, egressDefaultPolicy));
            }

            HostVO nuageVspHost = getNuageVspHost(network.getPhysicalNetworkId());
            ApplyAclRuleVspCommand cmd = new ApplyAclRuleVspCommand(network.getUuid(), networksDomain.getUuid(), vpcOrSubnetUuid, isL3Network(networkOferringVO.getId()), aclRules,
                    isVpc, network.getId());
            ApplyAclRuleVspAnswer answer = (ApplyAclRuleVspAnswer)_agentMgr.easySend(nuageVspHost.getId(), cmd);
            if (answer == null || !answer.getResult()) {
                s_logger.error("ApplyAclRuleNuageVspCommand for network " + network.getUuid() + " failed");
                if ((null != answer) && (null != answer.getDetails())) {
                    throw new ResourceUnavailableException(answer.getDetails(), Network.class, network.getId());
                }
            }

        } catch (Exception e1) {
            throw new ResourceUnavailableException(e1.getMessage(), Network.class, network.getId());
        }

        return true;
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

    protected HostVO getNuageVspHost(Long physicalNetworkId) throws CloudException {
        HostVO nuageVspHost;
        List<NuageVspDeviceVO> nuageVspDevices = _nuageVspDao.listByPhysicalNetwork(physicalNetworkId);
        if (nuageVspDevices != null && (!nuageVspDevices.isEmpty())) {
            NuageVspDeviceVO config = nuageVspDevices.iterator().next();
            nuageVspHost = _hostDao.findById(config.getHostId());
            _hostDao.loadDetails(nuageVspHost);
        } else {
            throw new CloudException("Nuage VSD is not configured on physical network " + physicalNetworkId);
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