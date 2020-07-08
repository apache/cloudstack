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

package com.cloud.network.element;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import javax.inject.Inject;
import javax.naming.ConfigurationException;
import javax.persistence.EntityExistsException;

import org.apache.cloudstack.context.CallContext;
import org.apache.cloudstack.engine.orchestration.service.NetworkOrchestrationService;
import org.apache.cloudstack.network.ExternalNetworkDeviceManager.NetworkDevice;
import org.apache.log4j.Logger;

import com.cloud.agent.AgentManager;
import com.cloud.agent.api.Answer;
import com.cloud.agent.api.AssociateAsaWithLogicalEdgeFirewallCommand;
import com.cloud.agent.api.CleanupLogicalEdgeFirewallCommand;
import com.cloud.agent.api.ConfigureNexusVsmForAsaCommand;
import com.cloud.agent.api.CreateLogicalEdgeFirewallCommand;
import com.cloud.agent.api.StartupCommand;
import com.cloud.agent.api.StartupExternalFirewallCommand;
import com.cloud.agent.api.routing.NetworkElementCommand;
import com.cloud.agent.api.routing.SetFirewallRulesCommand;
import com.cloud.agent.api.routing.SetPortForwardingRulesCommand;
import com.cloud.agent.api.routing.SetSourceNatCommand;
import com.cloud.agent.api.routing.SetStaticNatRulesCommand;
import com.cloud.agent.api.to.FirewallRuleTO;
import com.cloud.agent.api.to.IpAddressTO;
import com.cloud.agent.api.to.PortForwardingRuleTO;
import com.cloud.agent.api.to.StaticNatRuleTO;
import com.cloud.api.commands.AddCiscoAsa1000vResourceCmd;
import com.cloud.api.commands.AddCiscoVnmcResourceCmd;
import com.cloud.api.commands.DeleteCiscoAsa1000vResourceCmd;
import com.cloud.api.commands.DeleteCiscoVnmcResourceCmd;
import com.cloud.api.commands.ListCiscoAsa1000vResourcesCmd;
import com.cloud.api.commands.ListCiscoVnmcResourcesCmd;
import com.cloud.api.response.CiscoAsa1000vResourceResponse;
import com.cloud.api.response.CiscoVnmcResourceResponse;
import com.cloud.configuration.ConfigurationManager;
import com.cloud.dc.ClusterVO;
import com.cloud.dc.ClusterVSMMapVO;
import com.cloud.dc.DataCenter;
import com.cloud.dc.DataCenter.NetworkType;
import com.cloud.dc.Vlan;
import com.cloud.dc.VlanVO;
import com.cloud.dc.dao.ClusterDao;
import com.cloud.dc.dao.ClusterVSMMapDao;
import com.cloud.dc.dao.VlanDao;
import com.cloud.deploy.DeployDestination;
import com.cloud.exception.ConcurrentOperationException;
import com.cloud.exception.InsufficientAddressCapacityException;
import com.cloud.exception.InsufficientCapacityException;
import com.cloud.exception.InvalidParameterValueException;
import com.cloud.exception.ResourceAllocationException;
import com.cloud.exception.ResourceUnavailableException;
import com.cloud.host.DetailVO;
import com.cloud.host.Host;
import com.cloud.host.HostVO;
import com.cloud.host.dao.HostDao;
import com.cloud.host.dao.HostDetailsDao;
import com.cloud.network.CiscoNexusVSMDeviceVO;
import com.cloud.network.IpAddress;
import com.cloud.network.IpAddressManager;
import com.cloud.network.Network;
import com.cloud.network.Network.Capability;
import com.cloud.network.Network.Provider;
import com.cloud.network.Network.Service;
import com.cloud.network.NetworkModel;
import com.cloud.network.Networks.BroadcastDomainType;
import com.cloud.network.PhysicalNetworkServiceProvider;
import com.cloud.network.PublicIpAddress;
import com.cloud.network.addr.PublicIp;
import com.cloud.network.cisco.CiscoAsa1000vDevice;
import com.cloud.network.cisco.CiscoAsa1000vDeviceVO;
import com.cloud.network.cisco.CiscoVnmcController;
import com.cloud.network.cisco.CiscoVnmcControllerVO;
import com.cloud.network.cisco.NetworkAsa1000vMapVO;
import com.cloud.network.dao.CiscoAsa1000vDao;
import com.cloud.network.dao.CiscoNexusVSMDeviceDao;
import com.cloud.network.dao.CiscoVnmcDao;
import com.cloud.network.dao.IPAddressDao;
import com.cloud.network.dao.IPAddressVO;
import com.cloud.network.dao.NetworkAsa1000vMapDao;
import com.cloud.network.dao.NetworkDao;
import com.cloud.network.dao.PhysicalNetworkDao;
import com.cloud.network.dao.PhysicalNetworkServiceProviderDao;
import com.cloud.network.dao.PhysicalNetworkServiceProviderVO;
import com.cloud.network.dao.PhysicalNetworkVO;
import com.cloud.network.resource.CiscoVnmcResource;
import com.cloud.network.rules.FirewallRule;
import com.cloud.network.rules.FirewallRule.TrafficType;
import com.cloud.network.rules.PortForwardingRule;
import com.cloud.network.rules.StaticNat;
import com.cloud.offering.NetworkOffering;
import com.cloud.resource.ResourceManager;
import com.cloud.resource.ResourceState;
import com.cloud.resource.ResourceStateAdapter;
import com.cloud.resource.ServerResource;
import com.cloud.resource.UnableDeleteHostException;
import com.cloud.user.Account;
import com.cloud.utils.component.AdapterBase;
import com.cloud.utils.db.EntityManager;
import com.cloud.utils.db.Transaction;
import com.cloud.utils.db.TransactionCallback;
import com.cloud.utils.db.TransactionStatus;
import com.cloud.utils.exception.CloudRuntimeException;
import com.cloud.utils.exception.ExceptionUtil;
import com.cloud.utils.net.NetUtils;
import com.cloud.vm.NicProfile;
import com.cloud.vm.ReservationContext;
import com.cloud.vm.VirtualMachine.Type;
import com.cloud.vm.VirtualMachineProfile;

public class CiscoVnmcElement extends AdapterBase implements SourceNatServiceProvider, FirewallServiceProvider, PortForwardingServiceProvider, IpDeployer,
        StaticNatServiceProvider, ResourceStateAdapter, NetworkElement, CiscoVnmcElementService, CiscoAsa1000vService {
    private static final Logger s_logger = Logger.getLogger(CiscoVnmcElement.class);
    private static final Map<Service, Map<Capability, String>> capabilities = setCapabilities();

    @Inject
    EntityManager _entityMgr;
    @Inject
    AgentManager _agentMgr;
    @Inject
    ResourceManager _resourceMgr;
    @Inject
    ConfigurationManager _configMgr;
    @Inject
    NetworkOrchestrationService _networkMgr;
    @Inject
    NetworkModel _networkModel;
    @Inject
    IpAddressManager _ipAddrMgr;

    @Inject
    PhysicalNetworkDao _physicalNetworkDao;
    @Inject
    PhysicalNetworkServiceProviderDao _physicalNetworkServiceProviderDao;
    @Inject
    IPAddressDao _ipAddressDao;
    @Inject
    HostDetailsDao _hostDetailsDao;
    @Inject
    HostDao _hostDao;
    @Inject
    NetworkDao _networkDao;
    @Inject
    ClusterDao _clusterDao;
    @Inject
    VlanDao _vlanDao;
    @Inject
    ClusterVSMMapDao _clusterVsmMapDao;
    @Inject
    CiscoNexusVSMDeviceDao _vsmDeviceDao;
    @Inject
    CiscoVnmcDao _ciscoVnmcDao;
    @Inject
    CiscoAsa1000vDao _ciscoAsa1000vDao;
    @Inject
    NetworkAsa1000vMapDao _networkAsa1000vMapDao;

    protected boolean canHandle(Network network) {
        if (network.getBroadcastDomainType() != BroadcastDomainType.Vlan) {
            return false; //TODO: should handle VxLAN as well
        }

        return true;
    }

    @Override
    public boolean configure(String name, Map<String, Object> params) throws ConfigurationException {
        super.configure(name, params);
        _resourceMgr.registerResourceStateAdapter(this.getClass().getSimpleName(), this);
        return true;
    }

    private static Map<Service, Map<Capability, String>> setCapabilities() {
        Map<Service, Map<Capability, String>> capabilities = new HashMap<Service, Map<Capability, String>>();
        capabilities.put(Service.Gateway, null);

        Map<Capability, String> firewallCapabilities = new HashMap<Capability, String>();
        firewallCapabilities.put(Capability.TrafficStatistics, "per public ip");
        firewallCapabilities.put(Capability.SupportedTrafficDirection, "ingress,egress");
        firewallCapabilities.put(Capability.SupportedProtocols, "tcp,udp,icmp");
        firewallCapabilities.put(Capability.SupportedEgressProtocols, "tcp,udp,icmp");
        firewallCapabilities.put(Capability.MultipleIps, "true");
        capabilities.put(Service.Firewall, firewallCapabilities);

        capabilities.put(Service.StaticNat, null);
        capabilities.put(Service.PortForwarding, null);

        Map<Capability, String> sourceNatCapabilities = new HashMap<Capability, String>();
        sourceNatCapabilities.put(Capability.SupportedSourceNatTypes, "peraccount");
        sourceNatCapabilities.put(Capability.RedundantRouter, "false"); //TODO:
        capabilities.put(Service.SourceNat, sourceNatCapabilities);
        return capabilities;
    }

    @Override
    public Map<Service, Map<Capability, String>> getCapabilities() {
        return capabilities;
    }

    @Override
    public Provider getProvider() {
        return Provider.CiscoVnmc;
    }

    private boolean createLogicalEdgeFirewall(long vlanId, String gateway, String gatewayNetmask, String publicIp, String publicNetmask, List<String> publicGateways,
        long hostId) {
        CreateLogicalEdgeFirewallCommand cmd = new CreateLogicalEdgeFirewallCommand(vlanId, publicIp, gateway, publicNetmask, gatewayNetmask);
        for (String publicGateway : publicGateways) {
            cmd.getPublicGateways().add(publicGateway);
        }
        Answer answer = _agentMgr.easySend(hostId, cmd);
        return answer.getResult();
    }

    private boolean configureNexusVsmForAsa(long vlanId, String gateway, String vsmUsername, String vsmPassword, String vsmIp, String asaInPortProfile, long hostId) {
        ConfigureNexusVsmForAsaCommand cmd = new ConfigureNexusVsmForAsaCommand(vlanId, gateway, vsmUsername, vsmPassword, vsmIp, asaInPortProfile);
        Answer answer = _agentMgr.easySend(hostId, cmd);
        return answer.getResult();
    }

    private boolean configureSourceNat(long vlanId, String guestCidr, PublicIp sourceNatIp, long hostId) {
        boolean add = (sourceNatIp.getState() == IpAddress.State.Releasing ? false : true);
        IpAddressTO ip =
            new IpAddressTO(sourceNatIp.getAccountId(), sourceNatIp.getAddress().addr(), add, false, sourceNatIp.isSourceNat(), sourceNatIp.getVlanTag(),
                sourceNatIp.getGateway(), sourceNatIp.getNetmask(), sourceNatIp.getMacAddress(), null, sourceNatIp.isOneToOneNat());
        boolean addSourceNat = false;
        if (sourceNatIp.isSourceNat()) {
            addSourceNat = add;
        }

        SetSourceNatCommand cmd = new SetSourceNatCommand(ip, addSourceNat);
        cmd.setContextParam(NetworkElementCommand.GUEST_VLAN_TAG, Long.toString(vlanId));
        cmd.setContextParam(NetworkElementCommand.GUEST_NETWORK_CIDR, guestCidr);
        Answer answer = _agentMgr.easySend(hostId, cmd);
        return answer.getResult();
    }

    private boolean associateAsaWithLogicalEdgeFirewall(long vlanId, String asaMgmtIp, long hostId) {
        AssociateAsaWithLogicalEdgeFirewallCommand cmd = new AssociateAsaWithLogicalEdgeFirewallCommand(vlanId, asaMgmtIp);
        Answer answer = _agentMgr.easySend(hostId, cmd);
        return answer.getResult();
    }

    @Override
    public boolean implement(final Network network, final NetworkOffering offering, final DeployDestination dest, final ReservationContext context)
        throws ConcurrentOperationException, ResourceUnavailableException, InsufficientCapacityException {
        final DataCenter zone = _entityMgr.findById(DataCenter.class, network.getDataCenterId());

        if (zone.getNetworkType() == NetworkType.Basic) {
            s_logger.debug("Not handling network implement in zone of type " + NetworkType.Basic);
            return false;
        }

        if (!canHandle(network)) {
            return false;
        }

        final List<CiscoVnmcControllerVO> devices = _ciscoVnmcDao.listByPhysicalNetwork(network.getPhysicalNetworkId());
        if (devices.isEmpty()) {
            s_logger.error("No Cisco Vnmc device on network " + network.getName());
            return false;
        }

        List<CiscoAsa1000vDeviceVO> asaList = _ciscoAsa1000vDao.listByPhysicalNetwork(network.getPhysicalNetworkId());
        if (asaList.isEmpty()) {
            s_logger.debug("No Cisco ASA 1000v device on network " + network.getName());
            return false;
        }

        NetworkAsa1000vMapVO asaForNetwork = _networkAsa1000vMapDao.findByNetworkId(network.getId());
        if (asaForNetwork != null) {
            s_logger.debug("Cisco ASA 1000v device already associated with network " + network.getName());
            return true;
        }

        if (!_networkModel.isProviderSupportServiceInNetwork(network.getId(), Service.SourceNat, Provider.CiscoVnmc)) {
            s_logger.error("SourceNat service is not provided by Cisco Vnmc device on network " + network.getName());
            return false;
        }

        try {
            // ensure that there is an ASA 1000v assigned to this network
            CiscoAsa1000vDevice assignedAsa = assignAsa1000vToNetwork(network);
            if (assignedAsa == null) {
                s_logger.error("Unable to assign ASA 1000v device to network " + network.getName());
                throw new CloudRuntimeException("Unable to assign ASA 1000v device to network " + network.getName());
            }

            ClusterVO asaCluster = _clusterDao.findById(assignedAsa.getClusterId());
            ClusterVSMMapVO clusterVsmMap = _clusterVsmMapDao.findByClusterId(assignedAsa.getClusterId());
            if (clusterVsmMap == null) {
                s_logger.error("Vmware cluster " + asaCluster.getName() + " has no Cisco Nexus VSM device associated with it");
                throw new CloudRuntimeException("Vmware cluster " + asaCluster.getName() + " has no Cisco Nexus VSM device associated with it");
            }

            CiscoNexusVSMDeviceVO vsmDevice = _vsmDeviceDao.findById(clusterVsmMap.getVsmId());
            if (vsmDevice == null) {
                s_logger.error("Unable to load details of Cisco Nexus VSM device associated with cluster " + asaCluster.getName());
                throw new CloudRuntimeException("Unable to load details of Cisco Nexus VSM device associated with cluster " + asaCluster.getName());
            }

            CiscoVnmcControllerVO ciscoVnmcDevice = devices.get(0);
            HostVO ciscoVnmcHost = _hostDao.findById(ciscoVnmcDevice.getHostId());
            _hostDao.loadDetails(ciscoVnmcHost);
            Account owner = context.getAccount();
            PublicIp sourceNatIp = _ipAddrMgr.assignSourceNatIpAddressToGuestNetwork(owner, network);
            long vlanId = Long.parseLong(BroadcastDomainType.getValue(network.getBroadcastUri()));

            List<VlanVO> vlanVOList = _vlanDao.listVlansByPhysicalNetworkId(network.getPhysicalNetworkId());
            List<String> publicGateways = new ArrayList<String>();
            for (VlanVO vlanVO : vlanVOList) {
                publicGateways.add(vlanVO.getVlanGateway());
            }

            // due to VNMC limitation of not allowing source NAT ip as the outside ip of firewall,
            // an additional public ip needs to acquired for assigning as firewall outside ip.
            // In case there are already additional ip addresses available (network restart) use one
            // of them such that it is not the source NAT ip
            IpAddress outsideIp = null;
            List<IPAddressVO> publicIps = _ipAddressDao.listByAssociatedNetwork(network.getId(), null);
            for (IPAddressVO ip : publicIps) {
                if (!ip.isSourceNat()) {
                    outsideIp = ip;
                    break;
                }
            }
            if (outsideIp == null) { // none available, acquire one
                try {
                    Account caller = CallContext.current().getCallingAccount();
                    long callerUserId = CallContext.current().getCallingUserId();
                    outsideIp = _ipAddrMgr.allocateIp(owner, false, caller, callerUserId, zone, true, null);
                } catch (ResourceAllocationException e) {
                    s_logger.error("Unable to allocate additional public Ip address. Exception details " + e);
                    throw new CloudRuntimeException("Unable to allocate additional public Ip address. Exception details " + e);
                }

                try {
                    outsideIp = _ipAddrMgr.associateIPToGuestNetwork(outsideIp.getId(), network.getId(), true);
                } catch (ResourceAllocationException e) {
                    s_logger.error("Unable to assign allocated additional public Ip " + outsideIp.getAddress().addr() + " to network with vlan " + vlanId +
                        ". Exception details " + e);
                    throw new CloudRuntimeException("Unable to assign allocated additional public Ip " + outsideIp.getAddress().addr() + " to network with vlan " +
                        vlanId + ". Exception details " + e);
                }
            }

            // create logical edge firewall in VNMC
            String gatewayNetmask = NetUtils.getCidrNetmask(network.getCidr());
            // due to ASA limitation of allowing single subnet to be assigned to firewall interfaces,
            // all public ip addresses must be from same subnet, this essentially means single public subnet in zone
            if (!createLogicalEdgeFirewall(vlanId, network.getGateway(), gatewayNetmask, outsideIp.getAddress().addr(), sourceNatIp.getNetmask(), publicGateways,
                ciscoVnmcHost.getId())) {
                s_logger.error("Failed to create logical edge firewall in Cisco VNMC device for network " + network.getName());
                throw new CloudRuntimeException("Failed to create logical edge firewall in Cisco VNMC device for network " + network.getName());
            }

            // create stuff in VSM for ASA device
            if (!configureNexusVsmForAsa(vlanId, network.getGateway(), vsmDevice.getUserName(), vsmDevice.getPassword(), vsmDevice.getipaddr(),
                assignedAsa.getInPortProfile(), ciscoVnmcHost.getId())) {
                s_logger.error("Failed to configure Cisco Nexus VSM " + vsmDevice.getipaddr() + " for ASA device for network " + network.getName());
                throw new CloudRuntimeException("Failed to configure Cisco Nexus VSM " + vsmDevice.getipaddr() + " for ASA device for network " + network.getName());
            }

            // configure source NAT
            if (!configureSourceNat(vlanId, network.getCidr(), sourceNatIp, ciscoVnmcHost.getId())) {
                s_logger.error("Failed to configure source NAT in Cisco VNMC device for network " + network.getName());
                throw new CloudRuntimeException("Failed to configure source NAT in Cisco VNMC device for network " + network.getName());
            }

            // associate Asa 1000v instance with logical edge firewall
            if (!associateAsaWithLogicalEdgeFirewall(vlanId, assignedAsa.getManagementIp(), ciscoVnmcHost.getId())) {
                s_logger.error("Failed to associate Cisco ASA 1000v (" + assignedAsa.getManagementIp() + ") with logical edge firewall in VNMC for network " +
                    network.getName());
                throw new CloudRuntimeException("Failed to associate Cisco ASA 1000v (" + assignedAsa.getManagementIp() +
                    ") with logical edge firewall in VNMC for network " + network.getName());
            }
        } catch (CloudRuntimeException e) {
            unassignAsa1000vFromNetwork(network);
            s_logger.error("CiscoVnmcElement failed", e);
            return false;
        } catch (Exception e) {
            unassignAsa1000vFromNetwork(network);
            ExceptionUtil.rethrowRuntime(e);
            ExceptionUtil.rethrow(e, InsufficientAddressCapacityException.class);
            ExceptionUtil.rethrow(e, ResourceUnavailableException.class);
            throw new IllegalStateException(e);
        }

        return true;
    }

    @Override
    public boolean prepare(Network network, NicProfile nic, VirtualMachineProfile vm, DeployDestination dest, ReservationContext context)
        throws ConcurrentOperationException, ResourceUnavailableException, InsufficientCapacityException {
        if (vm.getType() != Type.User) {
            return false;
        }

        // ensure that there is an ASA 1000v assigned to this network
        NetworkAsa1000vMapVO asaForNetwork = _networkAsa1000vMapDao.findByNetworkId(network.getId());
        if (asaForNetwork == null) {
            return false;
        }

        return true;
    }

    @Override
    public boolean release(Network network, NicProfile nic, VirtualMachineProfile vm, ReservationContext context) throws ConcurrentOperationException,
        ResourceUnavailableException {
        return true;
    }

    private boolean cleanupLogicalEdgeFirewall(long vlanId, long hostId) {
        CleanupLogicalEdgeFirewallCommand cmd = new CleanupLogicalEdgeFirewallCommand(vlanId);
        Answer answer = _agentMgr.easySend(hostId, cmd);
        return answer.getResult();
    }

    @Override
    public boolean shutdown(Network network, ReservationContext context, boolean cleanup) throws ConcurrentOperationException, ResourceUnavailableException {

        unassignAsa1000vFromNetwork(network);

        long vlanId = Long.parseLong(BroadcastDomainType.getValue(network.getBroadcastUri()));
        List<CiscoVnmcControllerVO> devices = _ciscoVnmcDao.listByPhysicalNetwork(network.getPhysicalNetworkId());
        if (!devices.isEmpty()) {
            CiscoVnmcControllerVO ciscoVnmcDevice = devices.get(0);
            HostVO ciscoVnmcHost = _hostDao.findById(ciscoVnmcDevice.getHostId());
            cleanupLogicalEdgeFirewall(vlanId, ciscoVnmcHost.getId());
        }

        return true;
    }

    @Override
    public boolean isReady(PhysicalNetworkServiceProvider provider) {
        return true;
    }

    @Override
    public boolean shutdownProviderInstances(PhysicalNetworkServiceProvider provider, ReservationContext context) throws ConcurrentOperationException,
        ResourceUnavailableException {
        return true;
    }

    @Override
    public boolean canEnableIndividualServices() {
        return true;
    }

    @Override
    public boolean verifyServicesCombination(Set<Service> services) {
        if (!services.contains(Service.Firewall)) {
            s_logger.warn("CiscoVnmc must be used as Firewall Service Provider in the network");
            return false;
        }
        return true;
    }

    @Override
    public boolean destroy(Network network, ReservationContext context) throws ConcurrentOperationException, ResourceUnavailableException {
        return true;
    }

    @Override
    public List<Class<?>> getCommands() {
        List<Class<?>> cmdList = new ArrayList<Class<?>>();
        cmdList.add(AddCiscoVnmcResourceCmd.class);
        cmdList.add(DeleteCiscoVnmcResourceCmd.class);
        cmdList.add(ListCiscoVnmcResourcesCmd.class);
        cmdList.add(AddCiscoAsa1000vResourceCmd.class);
        cmdList.add(DeleteCiscoAsa1000vResourceCmd.class);
        cmdList.add(ListCiscoAsa1000vResourcesCmd.class);
        return cmdList;
    }

    @Override
    public CiscoVnmcController addCiscoVnmcResource(AddCiscoVnmcResourceCmd cmd) {
        final String deviceName = Provider.CiscoVnmc.getName();
        NetworkDevice networkDevice = NetworkDevice.getNetworkDevice(deviceName);
        final Long physicalNetworkId = cmd.getPhysicalNetworkId();
        CiscoVnmcController ciscoVnmcResource = null;

        PhysicalNetworkVO physicalNetwork = _physicalNetworkDao.findById(physicalNetworkId);
        if (physicalNetwork == null) {
            throw new InvalidParameterValueException("Could not find phyical network with ID: " + physicalNetworkId);
        }
        long zoneId = physicalNetwork.getDataCenterId();

        final PhysicalNetworkServiceProviderVO ntwkSvcProvider =
            _physicalNetworkServiceProviderDao.findByServiceProvider(physicalNetwork.getId(), networkDevice.getNetworkServiceProvder());
        if (ntwkSvcProvider == null) {
            throw new CloudRuntimeException("Network Service Provider: " + networkDevice.getNetworkServiceProvder() + " is not enabled in the physical network: " +
                physicalNetworkId + "to add this device");
        } else if (ntwkSvcProvider.getState() == PhysicalNetworkServiceProvider.State.Shutdown) {
            throw new CloudRuntimeException("Network Service Provider: " + ntwkSvcProvider.getProviderName() + " is in shutdown state in the physical network: " +
                physicalNetworkId + "to add this device");
        }

        if (_ciscoVnmcDao.listByPhysicalNetwork(physicalNetworkId).size() != 0) {
            throw new CloudRuntimeException("A Cisco Vnmc device is already configured on this physical network");
        }

        Map<String, String> params = new HashMap<String, String>();
        params.put("guid", UUID.randomUUID().toString());
        params.put("zoneId", String.valueOf(physicalNetwork.getDataCenterId()));
        params.put("physicalNetworkId", String.valueOf(physicalNetwork.getId()));
        params.put("name", "Cisco VNMC Controller - " + cmd.getHost());
        params.put("ip", cmd.getHost());
        params.put("username", cmd.getUsername());
        params.put("password", cmd.getPassword());

        Map<String, Object> hostdetails = new HashMap<String, Object>();
        hostdetails.putAll(params);

        ServerResource resource = new CiscoVnmcResource();
        try {
            resource.configure(cmd.getHost(), hostdetails);

            final Host host = _resourceMgr.addHost(zoneId, resource, Host.Type.ExternalFirewall, params);
            if (host != null) {
                return Transaction.execute(new TransactionCallback<CiscoVnmcController>() {
                    @Override
                    public CiscoVnmcController doInTransaction(TransactionStatus status) {
                        CiscoVnmcController ciscoVnmcResource = new CiscoVnmcControllerVO(host.getId(), physicalNetworkId, ntwkSvcProvider.getProviderName(), deviceName);
                        _ciscoVnmcDao.persist((CiscoVnmcControllerVO)ciscoVnmcResource);

                        DetailVO detail = new DetailVO(host.getId(), "deviceid", String.valueOf(ciscoVnmcResource.getId()));
                        _hostDetailsDao.persist(detail);

                        return ciscoVnmcResource;
                    }
                });
            } else {
                throw new CloudRuntimeException("Failed to add Cisco Vnmc device due to internal error.");
            }
        } catch (ConfigurationException e) {
            throw new CloudRuntimeException(e.getMessage());
        }
    }

    @Override
    public CiscoVnmcResourceResponse createCiscoVnmcResourceResponse(CiscoVnmcController ciscoVnmcResourceVO) {
        HostVO ciscoVnmcHost = _hostDao.findById(ciscoVnmcResourceVO.getHostId());

        CiscoVnmcResourceResponse response = new CiscoVnmcResourceResponse();
        response.setId(ciscoVnmcResourceVO.getUuid());
        response.setPhysicalNetworkId(ciscoVnmcResourceVO.getPhysicalNetworkId());
        response.setProviderName(ciscoVnmcResourceVO.getProviderName());
        response.setResourceName(ciscoVnmcHost.getName());

        return response;
    }

    @Override
    public boolean deleteCiscoVnmcResource(DeleteCiscoVnmcResourceCmd cmd) {
        Long vnmcResourceId = cmd.getCiscoVnmcResourceId();
        CiscoVnmcControllerVO vnmcResource = _ciscoVnmcDao.findById(vnmcResourceId);
        if (vnmcResource == null) {
            throw new InvalidParameterValueException("Could not find a Cisco VNMC appliance with id " + vnmcResourceId);
        }

        // Check if there any ASA 1000v appliances
        Long physicalNetworkId = vnmcResource.getPhysicalNetworkId();
        PhysicalNetworkVO physicalNetwork = _physicalNetworkDao.findById(physicalNetworkId);
        if (physicalNetwork != null) {
            List<CiscoAsa1000vDeviceVO> responseList = _ciscoAsa1000vDao.listByPhysicalNetwork(physicalNetworkId);
            if (responseList.size() > 0) {
                throw new CloudRuntimeException("Cisco VNMC appliance with id " + vnmcResourceId + " cannot be deleted as there Cisco ASA 1000v appliances using it");
            }
        }

        HostVO vnmcHost = _hostDao.findById(vnmcResource.getHostId());
        Long hostId = vnmcHost.getId();
        vnmcHost.setResourceState(ResourceState.Maintenance);
        _hostDao.update(hostId, vnmcHost);
        _resourceMgr.deleteHost(hostId, false, false);
        _ciscoVnmcDao.remove(vnmcResourceId);

        return true;
    }

    @Override
    public List<CiscoVnmcControllerVO> listCiscoVnmcResources(ListCiscoVnmcResourcesCmd cmd) {
        Long physicalNetworkId = cmd.getPhysicalNetworkId();
        Long ciscoVnmcResourceId = cmd.getCiscoVnmcResourceId();
        List<CiscoVnmcControllerVO> responseList = new ArrayList<CiscoVnmcControllerVO>();

        if (physicalNetworkId == null && ciscoVnmcResourceId == null) {
            throw new InvalidParameterValueException("Either physical network Id or vnmc device Id must be specified");
        }

        if (ciscoVnmcResourceId != null) {
            CiscoVnmcControllerVO ciscoVnmcResource = _ciscoVnmcDao.findById(ciscoVnmcResourceId);
            if (ciscoVnmcResource == null) {
                throw new InvalidParameterValueException("Could not find Cisco Vnmc device with id: " + ciscoVnmcResource);
            }
            responseList.add(ciscoVnmcResource);
        } else {
            PhysicalNetworkVO physicalNetwork = _physicalNetworkDao.findById(physicalNetworkId);
            if (physicalNetwork == null) {
                throw new InvalidParameterValueException("Could not find a physical network with id: " + physicalNetworkId);
            }
            responseList = _ciscoVnmcDao.listByPhysicalNetwork(physicalNetworkId);
        }

        return responseList;
    }

    @Override
    public IpDeployer getIpDeployer(Network network) {
        return this;
    }

    @Override
    public boolean applyFWRules(Network network, List<? extends FirewallRule> rules) throws ResourceUnavailableException {

        if (!_networkModel.isProviderSupportServiceInNetwork(network.getId(), Service.Firewall, Provider.CiscoVnmc)) {
            s_logger.error("Firewall service is not provided by Cisco Vnmc device on network " + network.getName());
            return false;
        }

        // Find VNMC host for physical network
        List<CiscoVnmcControllerVO> devices = _ciscoVnmcDao.listByPhysicalNetwork(network.getPhysicalNetworkId());
        if (devices.isEmpty()) {
            s_logger.error("No Cisco Vnmc device on network " + network.getName());
            return true;
        }

        // Find if ASA 1000v is associated with network
        NetworkAsa1000vMapVO asaForNetwork = _networkAsa1000vMapDao.findByNetworkId(network.getId());
        if (asaForNetwork == null) {
            s_logger.debug("Cisco ASA 1000v device is not associated with network " + network.getName());
            return true;
        }

        if (network.getState() == Network.State.Allocated) {
            s_logger.debug("External firewall was asked to apply firewall rules for network with ID " + network.getId() +
                "; this network is not implemented. Skipping backend commands.");
            return true;
        }

        CiscoVnmcControllerVO ciscoVnmcDevice = devices.get(0);
        HostVO ciscoVnmcHost = _hostDao.findById(ciscoVnmcDevice.getHostId());

        List<FirewallRuleTO> rulesTO = new ArrayList<FirewallRuleTO>();
        for (FirewallRule rule : rules) {
            String address = "0.0.0.0";
            if (rule.getTrafficType() == TrafficType.Ingress) {
                IpAddress sourceIp = _networkModel.getIp(rule.getSourceIpAddressId());
                address = sourceIp.getAddress().addr();
            }
            FirewallRuleTO ruleTO = new FirewallRuleTO(rule, null, address, rule.getPurpose(), rule.getTrafficType());
            rulesTO.add(ruleTO);
        }

        if (!rulesTO.isEmpty()) {
            SetFirewallRulesCommand cmd = new SetFirewallRulesCommand(rulesTO);
            cmd.setContextParam(NetworkElementCommand.GUEST_VLAN_TAG, BroadcastDomainType.getValue(network.getBroadcastUri()));
            cmd.setContextParam(NetworkElementCommand.GUEST_NETWORK_CIDR, network.getCidr());
            Answer answer = _agentMgr.easySend(ciscoVnmcHost.getId(), cmd);
            if (answer == null || !answer.getResult()) {
                String details = (answer != null) ? answer.getDetails() : "details unavailable";
                String msg = "Unable to apply firewall rules to Cisco ASA 1000v appliance due to: " + details + ".";
                s_logger.error(msg);
                throw new ResourceUnavailableException(msg, DataCenter.class, network.getDataCenterId());
            }
        }

        return true;
    }

    @Override
    public boolean applyPFRules(Network network, List<PortForwardingRule> rules) throws ResourceUnavailableException {

        if (!_networkModel.isProviderSupportServiceInNetwork(network.getId(), Service.PortForwarding, Provider.CiscoVnmc)) {
            s_logger.error("Port forwarding service is not provided by Cisco Vnmc device on network " + network.getName());
            return false;
        }

        // Find VNMC host for physical network
        List<CiscoVnmcControllerVO> devices = _ciscoVnmcDao.listByPhysicalNetwork(network.getPhysicalNetworkId());
        if (devices.isEmpty()) {
            s_logger.error("No Cisco Vnmc device on network " + network.getName());
            return true;
        }

        // Find if ASA 1000v is associated with network
        NetworkAsa1000vMapVO asaForNetwork = _networkAsa1000vMapDao.findByNetworkId(network.getId());
        if (asaForNetwork == null) {
            s_logger.debug("Cisco ASA 1000v device is not associated with network " + network.getName());
            return true;
        }

        if (network.getState() == Network.State.Allocated) {
            s_logger.debug("External firewall was asked to apply port forwarding rules for network with ID " + network.getId() +
                "; this network is not implemented. Skipping backend commands.");
            return true;
        }

        CiscoVnmcControllerVO ciscoVnmcDevice = devices.get(0);
        HostVO ciscoVnmcHost = _hostDao.findById(ciscoVnmcDevice.getHostId());

        List<PortForwardingRuleTO> rulesTO = new ArrayList<PortForwardingRuleTO>();
        for (PortForwardingRule rule : rules) {
            IpAddress sourceIp = _networkModel.getIp(rule.getSourceIpAddressId());
            Vlan vlan = _vlanDao.findById(sourceIp.getVlanId());
            PortForwardingRuleTO ruleTO = new PortForwardingRuleTO(rule, vlan.getVlanTag(), sourceIp.getAddress().addr());
            rulesTO.add(ruleTO);
        }

        if (!rulesTO.isEmpty()) {
            SetPortForwardingRulesCommand cmd = new SetPortForwardingRulesCommand(rulesTO);
            cmd.setContextParam(NetworkElementCommand.GUEST_VLAN_TAG, BroadcastDomainType.getValue(network.getBroadcastUri()));
            cmd.setContextParam(NetworkElementCommand.GUEST_NETWORK_CIDR, network.getCidr());
            Answer answer = _agentMgr.easySend(ciscoVnmcHost.getId(), cmd);
            if (answer == null || !answer.getResult()) {
                String details = (answer != null) ? answer.getDetails() : "details unavailable";
                String msg = "Unable to apply port forwarding rules to Cisco ASA 1000v appliance due to: " + details + ".";
                s_logger.error(msg);
                throw new ResourceUnavailableException(msg, DataCenter.class, network.getDataCenterId());
            }
        }

        return true;
    }

    @Override
    public boolean applyStaticNats(Network network, List<? extends StaticNat> rules) throws ResourceUnavailableException {
        if (!_networkModel.isProviderSupportServiceInNetwork(network.getId(), Service.StaticNat, Provider.CiscoVnmc)) {
            s_logger.error("Static NAT service is not provided by Cisco Vnmc device on network " + network.getName());
            return false;
        }

        // Find VNMC host for physical network
        List<CiscoVnmcControllerVO> devices = _ciscoVnmcDao.listByPhysicalNetwork(network.getPhysicalNetworkId());
        if (devices.isEmpty()) {
            s_logger.error("No Cisco Vnmc device on network " + network.getName());
            return true;
        }

        // Find if ASA 1000v is associated with network
        NetworkAsa1000vMapVO asaForNetwork = _networkAsa1000vMapDao.findByNetworkId(network.getId());
        if (asaForNetwork == null) {
            s_logger.debug("Cisco ASA 1000v device is not associated with network " + network.getName());
            return true;
        }

        if (network.getState() == Network.State.Allocated) {
            s_logger.debug("External firewall was asked to apply static NAT rules for network with ID " + network.getId() +
                "; this network is not implemented. Skipping backend commands.");
            return true;
        }

        CiscoVnmcControllerVO ciscoVnmcDevice = devices.get(0);
        HostVO ciscoVnmcHost = _hostDao.findById(ciscoVnmcDevice.getHostId());

        List<StaticNatRuleTO> rulesTO = new ArrayList<StaticNatRuleTO>();
        for (StaticNat rule : rules) {
            IpAddress sourceIp = _networkModel.getIp(rule.getSourceIpAddressId());
            StaticNatRuleTO ruleTO =
                new StaticNatRuleTO(rule.getSourceIpAddressId(), sourceIp.getAddress().addr(), null, null, rule.getDestIpAddress(), null, null, null, rule.isForRevoke(),
                    false);
            rulesTO.add(ruleTO);
        }

        if (!rulesTO.isEmpty()) {
            SetStaticNatRulesCommand cmd = new SetStaticNatRulesCommand(rulesTO, null);
            cmd.setContextParam(NetworkElementCommand.GUEST_VLAN_TAG, BroadcastDomainType.getValue(network.getBroadcastUri()));
            cmd.setContextParam(NetworkElementCommand.GUEST_NETWORK_CIDR, network.getCidr());
            Answer answer = _agentMgr.easySend(ciscoVnmcHost.getId(), cmd);
            if (answer == null || !answer.getResult()) {
                String details = (answer != null) ? answer.getDetails() : "details unavailable";
                String msg = "Unable to apply static NAT rules to Cisco ASA 1000v appliance due to: " + details + ".";
                s_logger.error(msg);
                throw new ResourceUnavailableException(msg, DataCenter.class, network.getDataCenterId());
            }
        }

        return true;
    }

    @Override
    public boolean applyIps(Network network, List<? extends PublicIpAddress> ipAddress, Set<Service> services) throws ResourceUnavailableException {
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    public HostVO createHostVOForConnectedAgent(HostVO host, StartupCommand[] cmd) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public HostVO createHostVOForDirectConnectAgent(HostVO host, StartupCommand[] startup, ServerResource resource, Map<String, String> details, List<String> hostTags) {
        if (!(startup[0] instanceof StartupExternalFirewallCommand)) {
            return null;
        }
        host.setType(Host.Type.ExternalFirewall);
        return host;
    }

    @Override
    public DeleteHostAnswer deleteHost(HostVO host, boolean isForced, boolean isForceDeleteStorage) throws UnableDeleteHostException {
        if (host.getType() != com.cloud.host.Host.Type.ExternalFirewall) {
            return null;
        }
        return new DeleteHostAnswer(true);
    }

    @Override
    public CiscoAsa1000vDevice addCiscoAsa1000vResource(AddCiscoAsa1000vResourceCmd cmd) {
        Long physicalNetworkId = cmd.getPhysicalNetworkId();
        CiscoAsa1000vDevice ciscoAsa1000vResource = null;

        PhysicalNetworkVO physicalNetwork = _physicalNetworkDao.findById(physicalNetworkId);
        if (physicalNetwork == null) {
            throw new InvalidParameterValueException("Could not find phyical network with ID: " + physicalNetworkId);
        }

        ciscoAsa1000vResource = new CiscoAsa1000vDeviceVO(physicalNetworkId, cmd.getManagementIp().trim(), cmd.getInPortProfile(), cmd.getClusterId());
        try {
            _ciscoAsa1000vDao.persist((CiscoAsa1000vDeviceVO)ciscoAsa1000vResource);
        } catch (EntityExistsException e) {
            throw new InvalidParameterValueException("An ASA 1000v appliance already exists with same configuration");
        }

        return ciscoAsa1000vResource;
    }

    @Override
    public CiscoAsa1000vResourceResponse createCiscoAsa1000vResourceResponse(CiscoAsa1000vDevice ciscoAsa1000vDeviceVO) {
        CiscoAsa1000vResourceResponse response = new CiscoAsa1000vResourceResponse();
        response.setId(ciscoAsa1000vDeviceVO.getUuid());
        response.setManagementIp(ciscoAsa1000vDeviceVO.getManagementIp());
        response.setInPortProfile(ciscoAsa1000vDeviceVO.getInPortProfile());

        NetworkAsa1000vMapVO networkAsaMap = _networkAsa1000vMapDao.findByAsa1000vId(ciscoAsa1000vDeviceVO.getId());
        if (networkAsaMap != null) {
            response.setGuestNetworkId(networkAsaMap.getNetworkId());
        }

        return response;
    }

    @Override
    public boolean deleteCiscoAsa1000vResource(DeleteCiscoAsa1000vResourceCmd cmd) {
        Long asaResourceId = cmd.getCiscoAsa1000vResourceId();
        CiscoAsa1000vDeviceVO asaResource = _ciscoAsa1000vDao.findById(asaResourceId);
        if (asaResource == null) {
            throw new InvalidParameterValueException("Could not find a Cisco ASA 1000v appliance with id " + asaResourceId);
        }

        NetworkAsa1000vMapVO networkAsaMap = _networkAsa1000vMapDao.findByAsa1000vId(asaResource.getId());
        if (networkAsaMap != null) {
            throw new CloudRuntimeException("Cisco ASA 1000v appliance with id " + asaResourceId + " cannot be deleted as it is associated with guest network");
        }

        _ciscoAsa1000vDao.remove(asaResourceId);

        return true;
    }

    @Override
    public List<CiscoAsa1000vDeviceVO> listCiscoAsa1000vResources(ListCiscoAsa1000vResourcesCmd cmd) {
        Long physicalNetworkId = cmd.getPhysicalNetworkId();
        Long ciscoAsa1000vResourceId = cmd.getCiscoAsa1000vResourceId();
        List<CiscoAsa1000vDeviceVO> responseList = new ArrayList<CiscoAsa1000vDeviceVO>();

        if (physicalNetworkId == null && ciscoAsa1000vResourceId == null) {
            throw new InvalidParameterValueException("Either physical network Id or Asa 1000v device Id must be specified");
        }

        if (ciscoAsa1000vResourceId != null) {
            CiscoAsa1000vDeviceVO ciscoAsa1000vResource = _ciscoAsa1000vDao.findById(ciscoAsa1000vResourceId);
            if (ciscoAsa1000vResource == null) {
                throw new InvalidParameterValueException("Could not find Cisco Asa 1000v device with id: " + ciscoAsa1000vResourceId);
            }
            responseList.add(ciscoAsa1000vResource);
        } else {
            PhysicalNetworkVO physicalNetwork = _physicalNetworkDao.findById(physicalNetworkId);
            if (physicalNetwork == null) {
                throw new InvalidParameterValueException("Could not find a physical network with id: " + physicalNetworkId);
            }
            responseList = _ciscoAsa1000vDao.listByPhysicalNetwork(physicalNetworkId);
        }

        return responseList;
    }

    @Override
    public CiscoAsa1000vDevice assignAsa1000vToNetwork(Network network) {
        List<CiscoAsa1000vDeviceVO> asaList = _ciscoAsa1000vDao.listByPhysicalNetwork(network.getPhysicalNetworkId());
        for (CiscoAsa1000vDeviceVO asa : asaList) {
            NetworkAsa1000vMapVO assignedToNetwork = _networkAsa1000vMapDao.findByAsa1000vId(asa.getId());
            if (assignedToNetwork == null) {
                NetworkAsa1000vMapVO networkAsaMap = new NetworkAsa1000vMapVO(network.getId(), asa.getId());
                _networkAsa1000vMapDao.persist(networkAsaMap);
                return asa;
            }
        }
        return null;
    }

    private void unassignAsa1000vFromNetwork(Network network) {
        NetworkAsa1000vMapVO networkAsaMap = _networkAsa1000vMapDao.findByNetworkId(network.getId());
        if (networkAsaMap != null) {
            _networkAsa1000vMapDao.remove(networkAsaMap.getId());
        }
    }
}
