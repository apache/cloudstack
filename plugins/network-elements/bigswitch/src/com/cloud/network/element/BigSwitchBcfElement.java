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
import java.util.UUID;

import javax.ejb.Local;
import javax.inject.Inject;
import javax.naming.ConfigurationException;

import org.apache.log4j.Logger;
import org.springframework.stereotype.Component;
import org.apache.cloudstack.network.ExternalNetworkDeviceManager.NetworkDevice;
import org.apache.commons.net.util.SubnetUtils;

import com.cloud.agent.AgentManager;
import com.cloud.agent.api.BcfAnswer;
import com.cloud.agent.api.UpdateBcfRouterCommand;
import com.cloud.agent.api.CreateBcfAttachmentCommand;
import com.cloud.agent.api.CreateBcfStaticNatCommand;
import com.cloud.agent.api.DeleteBcfAttachmentCommand;
import com.cloud.agent.api.DeleteBcfStaticNatCommand;
import com.cloud.agent.api.StartupBigSwitchBcfCommand;
import com.cloud.agent.api.StartupCommand;
import com.cloud.api.ApiDBUtils;
import com.cloud.api.commands.AddBigSwitchBcfDeviceCmd;
import com.cloud.api.commands.DeleteBigSwitchBcfDeviceCmd;
import com.cloud.api.commands.ListBigSwitchBcfDevicesCmd;
import com.cloud.api.commands.BcfConstants;
import com.cloud.api.response.BigSwitchBcfDeviceResponse;
import com.cloud.configuration.ConfigurationManager;
import com.cloud.dc.DataCenterVO;
import com.cloud.dc.dao.DataCenterDao;
import com.cloud.dc.dao.VlanDao;
import com.cloud.deploy.DeployDestination;
import com.cloud.exception.ConcurrentOperationException;
import com.cloud.exception.InsufficientCapacityException;
import com.cloud.exception.InvalidParameterValueException;
import com.cloud.exception.ResourceUnavailableException;
import com.cloud.host.DetailVO;
import com.cloud.host.Host;
import com.cloud.host.HostVO;
import com.cloud.host.dao.HostDao;
import com.cloud.host.dao.HostDetailsDao;
import com.cloud.hypervisor.Hypervisor.HypervisorType;
import com.cloud.network.BigSwitchBcfDeviceVO;
import com.cloud.network.IpAddressManager;
import com.cloud.network.Network;
import com.cloud.network.Network.Capability;
import com.cloud.network.Network.Provider;
import com.cloud.network.Network.Service;
import com.cloud.network.NetworkModel;
import com.cloud.network.Networks.BroadcastDomainType;
import com.cloud.network.PhysicalNetwork;
import com.cloud.network.PhysicalNetworkServiceProvider;
import com.cloud.network.PublicIpAddress;
import com.cloud.network.bigswitch.AclData;
import com.cloud.network.bigswitch.BigSwitchBcfApi;
import com.cloud.network.bigswitch.BigSwitchBcfUtils;
import com.cloud.network.bigswitch.TopologyData;
import com.cloud.network.dao.BigSwitchBcfDao;
import com.cloud.network.dao.FirewallRulesCidrsDao;
import com.cloud.network.dao.FirewallRulesDao;
import com.cloud.network.dao.IPAddressDao;
import com.cloud.network.dao.NetworkDao;
import com.cloud.network.dao.NetworkServiceMapDao;
import com.cloud.network.dao.PhysicalNetworkDao;
import com.cloud.network.dao.PhysicalNetworkServiceProviderDao;
import com.cloud.network.dao.PhysicalNetworkServiceProviderVO;
import com.cloud.network.dao.PhysicalNetworkVO;
import com.cloud.network.resource.BigSwitchBcfResource;
import com.cloud.network.rules.FirewallRule;
import com.cloud.network.rules.StaticNat;
import com.cloud.network.vpc.NetworkACLItem;
import com.cloud.network.vpc.NetworkACLItemCidrsDao;
import com.cloud.network.vpc.NetworkACLItemDao;
import com.cloud.network.vpc.Vpc;
import com.cloud.network.vpc.dao.VpcDao;
import com.cloud.offering.NetworkOffering;
import com.cloud.resource.ResourceManager;
import com.cloud.resource.ResourceState;
import com.cloud.resource.ResourceStateAdapter;
import com.cloud.resource.ServerResource;
import com.cloud.resource.UnableDeleteHostException;
import com.cloud.user.dao.AccountDao;
import com.cloud.utils.component.AdapterBase;
import com.cloud.utils.db.DB;
import com.cloud.utils.db.Transaction;
import com.cloud.utils.db.TransactionCallback;
import com.cloud.utils.db.TransactionStatus;
import com.cloud.utils.exception.CloudRuntimeException;
import com.cloud.vm.NicProfile;
import com.cloud.vm.ReservationContext;
import com.cloud.vm.VirtualMachineProfile;
import com.cloud.vm.dao.DomainRouterDao;
import com.cloud.vm.dao.NicDao;
import com.cloud.vm.dao.VMInstanceDao;

/**
 * BigSwitchBcfElement is responsible for creating and plugging a nic into a BCF Segment network.
 * When a VM is created and needs to be plugged into a BCF Segment network, BigSwitchBcfElement is
 * called by NetworkOrchestrator to create a "port" and an "attachment" for each nic, and
 * register them with the controller to be plugged into the corresponding network. It also
 * removes them when the VM is destroyed.
 */
@Component
@Local(value = {NetworkElement.class, ConnectivityProvider.class, IpDeployer.class,
        SourceNatServiceProvider.class, StaticNatServiceProvider.class,
        NetworkACLServiceProvider.class, FirewallServiceProvider.class})
public class BigSwitchBcfElement extends AdapterBase implements BigSwitchBcfElementService,
ConnectivityProvider, IpDeployer, SourceNatServiceProvider, StaticNatServiceProvider,
NetworkACLServiceProvider, FirewallServiceProvider, ResourceStateAdapter {
    private static final Logger s_logger = Logger.getLogger(BigSwitchBcfElement.class);

    private static final Map<Service, Map<Capability, String>> capabilities = setCapabilities();

    @Inject
    ResourceManager _resourceMgr;
    @Inject
    PhysicalNetworkDao _physicalNetworkDao;
    @Inject
    PhysicalNetworkServiceProviderDao _physicalNetworkServiceProviderDao;
    @Inject
    BigSwitchBcfDao _bigswitchBcfDao;
    @Inject
    HostDetailsDao _hostDetailsDao;
    @Inject
    HostDao _hostDao;
    @Inject
    AgentManager _agentMgr;
    @Inject
    NetworkDao _networkDao;
    @Inject
    DomainRouterDao _routerDao;
    @Inject
    NicDao _nicDao;
    @Inject
    VMInstanceDao _vmDao;
    @Inject
    AccountDao _accountDao;
    @Inject
    VpcDao _vpcDao;
    @Inject
    NetworkModel _networkModel;
    @Inject
    ConfigurationManager _configMgr;
    @Inject
    NetworkServiceMapDao _ntwkSrvcDao;
    @Inject
    DataCenterDao _zoneDao;
    @Inject
    IPAddressDao _ipAddressDao;
    @Inject
    IpAddressManager _ipAddrMgr;
    @Inject
    VlanDao _vlanDao;
    @Inject
    FirewallRulesDao _fwRulesDao;
    @Inject
    FirewallRulesCidrsDao _fwCidrsDao;
    @Inject
    NetworkACLItemDao _aclItemDao;
    @Inject
    NetworkACLItemCidrsDao _aclItemCidrsDao;

    private BigSwitchBcfUtils _bcfUtils = null;

    @Override
    public Map<Service, Map<Capability, String>> getCapabilities() {
        return capabilities;
    }

    @Override
    public Provider getProvider() {
        return Provider.BigSwitchBcf;
    }

    private boolean canHandle(Network network, Service service) {
        s_logger.debug("Checking if BigSwitchBcfElement can handle service " + service.getName() + " on network " + network.getDisplayText());
        if (network.getBroadcastDomainType() != BroadcastDomainType.Vlan) {
            return false;
        }

        if (!_networkModel.isProviderForNetwork(getProvider(), network.getId())) {
            s_logger.debug("BigSwitchBcfElement is not a provider for network " + network.getDisplayText());
            return false;
        }

        if (!_ntwkSrvcDao.canProviderSupportServiceInNetwork(network.getId(), service, BcfConstants.BIG_SWITCH_BCF)) {
            s_logger.debug("BigSwitchBcfElement can't provide the " + service.getName() + " service on network " + network.getDisplayText());
            return false;
        }

        return true;
    }

    @Override
    public boolean configure(String name, Map<String, Object> params) throws ConfigurationException {
        super.configure(name, params);
        _resourceMgr.registerResourceStateAdapter(this.getClass().getSimpleName(), this);
        return true;
    }

    @Override
    public boolean implement(Network network, NetworkOffering offering, DeployDestination dest, ReservationContext context) throws ConcurrentOperationException,
        ResourceUnavailableException, InsufficientCapacityException {
        updateBcfRouter(network);
        return true;
    }

    @Override
    public boolean prepare(Network network, NicProfile nic, VirtualMachineProfile vm, DeployDestination dest, ReservationContext context)
        throws ConcurrentOperationException, ResourceUnavailableException, InsufficientCapacityException {

        if (!canHandle(network, Service.Connectivity)) {
            return false;
        }

        bcfUtilsInit();

        // get arguments for CreateBcfAttachmentCommand
        // determine whether this is VPC network or stand-alone network
        Vpc vpc = null;
        if(network.getVpcId()!=null){
            vpc = _vpcDao.acquireInLockTable(network.getVpcId());
        }

        String tenantId;
        String tenantName;
        if (vpc != null) {
            tenantId = vpc.getUuid();
            tenantName = vpc.getName();
            _vpcDao.releaseFromLockTable(vpc.getId());
        } else {
            // use account in CS as tenant in BSN
            // use network id/name as tenant id/name for non-VPC networks
            tenantId = network.getUuid();
            tenantName = network.getName();
        }

        String networkId = network.getUuid();
        String hostname = dest.getHost().getName();
        String nicId = nic.getUuid();
        Integer vlan = Integer.valueOf(BroadcastDomainType.getValue(nic.getIsolationUri()));
        String ipv4 = nic.getIPv4Address();
        String mac = nic.getMacAddress();
        long zoneId = network.getDataCenterId();
        String vmwareVswitchLabel = _networkModel.getDefaultGuestTrafficLabel(zoneId, HypervisorType.VMware);
        String[] labelArray = null;
        String vswitchName = null;
        if(vmwareVswitchLabel!=null){
            labelArray=vmwareVswitchLabel.split(",");
            vswitchName = labelArray[0];
        }

        // hypervisor type:
        //   kvm: ivs port name
        //   vmware: specific portgroup naming convention
        String pgName = "";
        if (dest.getHost().getHypervisorType() == HypervisorType.KVM){
            pgName = hostname;
        } else if (dest.getHost().getHypervisorType() == HypervisorType.VMware){
            pgName = hostname + "-" + vswitchName;
        }

        CreateBcfAttachmentCommand cmd = new CreateBcfAttachmentCommand(tenantId,
                tenantName, networkId, pgName, nicId, vlan, ipv4, mac);

        _bcfUtils.sendBcfCommandWithNetworkSyncCheck(cmd, network);

        return true;
    }

    @Override
    public boolean release(Network network, NicProfile nic, VirtualMachineProfile vm, ReservationContext context) throws ConcurrentOperationException,
        ResourceUnavailableException {

        if (!canHandle(network, Service.Connectivity)) {
            return false;
        }

        if (network.getBroadcastUri() == null) {
            s_logger.error("Nic has no broadcast Uri");
            return false;
        }

        bcfUtilsInit();

        String networkId = network.getUuid();
        String nicId = nic.getUuid();

        String tenantId;
        if(network.getVpcId()!=null) {
            tenantId = network.getNetworkDomain();
        } else {
            tenantId = networkId;
        }

        DeleteBcfAttachmentCommand cmd = new DeleteBcfAttachmentCommand(tenantId, networkId, nicId);

        _bcfUtils.sendBcfCommandWithNetworkSyncCheck(cmd, network);
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
    public boolean destroy(Network network, ReservationContext context) throws ConcurrentOperationException, ResourceUnavailableException {
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
        if (!services.contains(Service.Connectivity)) {
            s_logger.warn("Unable to provide services without Connectivity service enabled for this element");
            return false;
        }
        return true;
    }

    private static Map<Service, Map<Capability, String>> setCapabilities() {
        Map<Service, Map<Capability, String>> capabilities = new HashMap<Service, Map<Capability, String>>();

        // L2 Support
        capabilities.put(Service.Connectivity, null);

        // L3 Support
        capabilities.put(Service.Gateway, null);

        // L3 Support : SourceNat
        Map<Capability, String> sourceNatCapabilities = new HashMap<Capability, String>();
        sourceNatCapabilities.put(Capability.SupportedSourceNatTypes, "peraccount");
        sourceNatCapabilities.put(Capability.RedundantRouter, "false");
        capabilities.put(Service.SourceNat, sourceNatCapabilities);

        // L3 support : StaticNat
        capabilities.put(Service.StaticNat, null);

        //add network ACL capability
        Map<Network.Capability, String> networkACLCapabilities = new HashMap<Network.Capability, String>();
        networkACLCapabilities.put(Network.Capability.SupportedProtocols, "tcp,udp,icmp");
        capabilities.put(Network.Service.NetworkACL, networkACLCapabilities);

        // Set capabilities for Firewall service
        Map<Capability, String> firewallCapabilities = new HashMap<Capability, String>();
        firewallCapabilities.put(Capability.TrafficStatistics, "per public ip");
        firewallCapabilities.put(Capability.SupportedProtocols, "tcp,udp,icmp");
        firewallCapabilities.put(Capability.SupportedEgressProtocols, "tcp,udp,icmp, all");
        firewallCapabilities.put(Capability.SupportedTrafficDirection, "ingress, egress");
        firewallCapabilities.put(Capability.MultipleIps, "true");
        capabilities.put(Service.Firewall, firewallCapabilities);

        return capabilities;
    }

    @Override
    @DB
    public BigSwitchBcfDeviceVO addBigSwitchBcfDevice(AddBigSwitchBcfDeviceCmd cmd) {
        BigSwitchBcfDeviceVO newBcfDevice;

        bcfUtilsInit();

        ServerResource resource = new BigSwitchBcfResource();

        final String deviceName = BcfConstants.BIG_SWITCH_BCF.getName();
        NetworkDevice networkDevice = NetworkDevice.getNetworkDevice(deviceName);
        final Long physicalNetworkId = cmd.getPhysicalNetworkId();
        final String hostname = cmd.getHost();
        final String username = cmd.getUsername();
        final String password = cmd.getPassword();
        final Boolean nat = cmd.getNat();

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
        ntwkSvcProvider.setFirewallServiceProvided(true);
        ntwkSvcProvider.setGatewayServiceProvided(true);
        ntwkSvcProvider.setNetworkAclServiceProvided(true);
        ntwkSvcProvider.setSourcenatServiceProvided(true);
        ntwkSvcProvider.setStaticnatServiceProvided(true);

        if (_bigswitchBcfDao.listByPhysicalNetwork(physicalNetworkId).size() > 1) {
            throw new CloudRuntimeException("At most two BCF controllers can be configured");
        }

        DataCenterVO zone = _zoneDao.findById(physicalNetwork.getDataCenterId());
        String zoneName;
        if(zone!= null){
            zoneName = zone.getName();
        } else {
            zoneName = String.valueOf(zoneId);
        }

        Boolean natNow =  _bcfUtils.isNatEnabled();
        if (!nat && natNow){
            throw new CloudRuntimeException("NAT is enabled in existing controller. Enable NAT for new controller or remove existing controller first.");
        } else if (nat && !natNow){
            throw new CloudRuntimeException("NAT is disabled in existing controller. Disable NAT for new controller or remove existing controller first.");
        }

        Map<String, String> params = new HashMap<String, String>();
        params.put("guid", UUID.randomUUID().toString());
        params.put("zoneId", zoneName);
        params.put("physicalNetworkId", String.valueOf(physicalNetwork.getId()));
        params.put("name", "BigSwitch Controller - " + cmd.getHost());
        params.put("hostname", cmd.getHost());
        params.put("username", username);
        params.put("password", password);
        params.put("nat", nat.toString());

        // FIXME What to do with multiple isolation types
        params.put("transportzoneisotype", physicalNetwork.getIsolationMethods().get(0).toLowerCase());
        Map<String, Object> hostdetails = new HashMap<String, Object>();
        hostdetails.putAll(params);

        try {
            resource.configure(cmd.getHost(), hostdetails);

            // store current topology in bcf resource
            TopologyData topo = _bcfUtils.getTopology(physicalNetwork.getId());
            ((BigSwitchBcfResource) resource).setTopology(topo);

            final Host host = _resourceMgr.addHost(zoneId, resource, Host.Type.L2Networking, params);
            if (host != null) {
                newBcfDevice = Transaction.execute(new TransactionCallback<BigSwitchBcfDeviceVO>() {
                    @Override
                    public BigSwitchBcfDeviceVO doInTransaction(TransactionStatus status) {
                        BigSwitchBcfDeviceVO bigswitchBcfDevice =
                            new BigSwitchBcfDeviceVO(host.getId(), physicalNetworkId, ntwkSvcProvider.getProviderName(),
                                    deviceName, hostname, username, password, nat, BigSwitchBcfApi.HASH_IGNORE);
                        _bigswitchBcfDao.persist(bigswitchBcfDevice);

                        DetailVO detail = new DetailVO(host.getId(), "bigswitchbcfdeviceid", String.valueOf(bigswitchBcfDevice.getId()));
                        _hostDetailsDao.persist(detail);

                        return bigswitchBcfDevice;
                    }
                });
            } else {
                throw new CloudRuntimeException("Failed to add BigSwitch BCF Controller Device due to internal error.");
            }
        } catch (ConfigurationException e) {
            throw new CloudRuntimeException(e.getMessage());
        }

        // initial topology sync to newly added BCF controller
        HostVO bigswitchBcfHost = _hostDao.findById(newBcfDevice.getHostId());
        _bcfUtils.syncTopologyToBcfHost(bigswitchBcfHost, nat);

        return newBcfDevice;
    }

    @Override
    public BigSwitchBcfDeviceResponse createBigSwitchBcfDeviceResponse(BigSwitchBcfDeviceVO bigswitchBcfDeviceVO) {
        HostVO bigswitchBcfHost = _hostDao.findById(bigswitchBcfDeviceVO.getHostId());
        _hostDao.loadDetails(bigswitchBcfHost);

        BigSwitchBcfDeviceResponse response = new BigSwitchBcfDeviceResponse();
        response.setDeviceName(bigswitchBcfDeviceVO.getDeviceName());
        PhysicalNetwork pnw = ApiDBUtils.findPhysicalNetworkById(bigswitchBcfDeviceVO.getPhysicalNetworkId());
        if (pnw != null) {
            response.setPhysicalNetworkId(pnw.getUuid());
        }
        response.setId(bigswitchBcfDeviceVO.getUuid());
        response.setProviderName(bigswitchBcfDeviceVO.getProviderName());
        response.setHostName(bigswitchBcfHost.getDetail("hostname"));
        response.setObjectName("bigswitchbcfdevice");
        return response;
    }

    @Override
    public boolean deleteBigSwitchBcfDevice(DeleteBigSwitchBcfDeviceCmd cmd) {
        Long bigswitchBcfDeviceId = cmd.getBigSwitchBcfDeviceId();
        BigSwitchBcfDeviceVO bigswitchBcfDevice = _bigswitchBcfDao.findById(bigswitchBcfDeviceId);
        if (bigswitchBcfDevice == null) {
            throw new InvalidParameterValueException("Could not find a BigSwitch Controller with id " + bigswitchBcfDevice);
        }

        HostVO bigswitchHost = _hostDao.findById(bigswitchBcfDevice.getHostId());
        Long hostId = bigswitchHost.getId();

        bigswitchHost.setResourceState(ResourceState.Maintenance);
        _hostDao.update(hostId, bigswitchHost);
        _resourceMgr.deleteHost(hostId, false, false);

        _bigswitchBcfDao.remove(bigswitchBcfDeviceId);
        return true;
    }

    @Override
    public List<BigSwitchBcfDeviceVO> listBigSwitchBcfDevices(ListBigSwitchBcfDevicesCmd cmd) {
        Long physicalNetworkId = cmd.getPhysicalNetworkId();
        Long bigswitchBcfDeviceId = cmd.getBigSwitchBcfDeviceId();
        List<BigSwitchBcfDeviceVO> responseList = new ArrayList<BigSwitchBcfDeviceVO>();

        if (physicalNetworkId == null && bigswitchBcfDeviceId == null) {
            throw new InvalidParameterValueException("Either physical network Id or bigswitch device Id must be specified");
        }

        if (bigswitchBcfDeviceId != null) {
            BigSwitchBcfDeviceVO bigswitchBcfDevice = _bigswitchBcfDao.findById(bigswitchBcfDeviceId);
            if (bigswitchBcfDevice == null) {
                throw new InvalidParameterValueException("Could not find BigSwitch controller with id: " + bigswitchBcfDevice);
            }
            responseList.add(bigswitchBcfDevice);
        } else {
            PhysicalNetworkVO physicalNetwork = _physicalNetworkDao.findById(physicalNetworkId);
            if (physicalNetwork == null) {
                throw new InvalidParameterValueException("Could not find a physical network with id: " + physicalNetworkId);
            }
            responseList = _bigswitchBcfDao.listByPhysicalNetwork(physicalNetworkId);
        }

        return responseList;
    }

    @Override
    public HostVO createHostVOForConnectedAgent(HostVO host, StartupCommand[] cmd) {
        return null;
    }

    @Override
    public HostVO createHostVOForDirectConnectAgent(HostVO host, StartupCommand[] startup, ServerResource resource, Map<String, String> details, List<String> hostTags) {
        if (!(startup[0] instanceof StartupBigSwitchBcfCommand)) {
            return null;
        }

        BigSwitchBcfResource bcfResource = (BigSwitchBcfResource) resource;
        bcfUtilsInit();

        if(_bcfUtils.getTopology()!=null){
            bcfResource.setTopology(_bcfUtils.getTopology());
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

    @Override
    public List<Class<?>> getCommands() {
        List<Class<?>> cmdList = new ArrayList<Class<?>>();
        cmdList.add(AddBigSwitchBcfDeviceCmd.class);
        cmdList.add(DeleteBigSwitchBcfDeviceCmd.class);
        cmdList.add(ListBigSwitchBcfDevicesCmd.class);
        return cmdList;
    }

    @Override
    public IpDeployer getIpDeployer(Network network) {
        return this;
    }

    @Override
    public boolean applyStaticNats(Network network,
            List<? extends StaticNat> rules)
            throws ResourceUnavailableException {
        bcfUtilsInit();

        _bcfUtils.listACLbyNetwork(network);

        Vpc vpc = null;
        if(network.getVpcId()!=null){
            vpc = _vpcDao.acquireInLockTable(network.getVpcId());
        }

        String tenantId;
        if (vpc != null) {
            tenantId = vpc.getUuid();
            _vpcDao.releaseFromLockTable(vpc.getId());
        } else {
            // use account in CS as tenant in BSN
            // use network uuid as tenantId for non-VPC networks
            tenantId = network.getUuid();
        }

        for (StaticNat rule: rules){
            String srcIp = _ipAddressDao.findById(rule.getSourceIpAddressId()).getAddress().addr();
            String dstIp = rule.getDestIpAddress();
            String mac = rule.getSourceMacAddress();
            if(!rule.isForRevoke()) {
                s_logger.debug("BCF enables static NAT for public IP: " + srcIp + " private IP " + dstIp
                        + " mac " + mac);
                CreateBcfStaticNatCommand cmd = new CreateBcfStaticNatCommand(
                        tenantId, network.getUuid(), dstIp, srcIp, mac);

                _bcfUtils.sendBcfCommandWithNetworkSyncCheck(cmd, network);
            } else {
                s_logger.debug("BCF removes static NAT for public IP: " + srcIp + " private IP " + dstIp
                        + " mac " + mac);
                DeleteBcfStaticNatCommand cmd = new DeleteBcfStaticNatCommand(tenantId, srcIp);

                _bcfUtils.sendBcfCommandWithNetworkSyncCheck(cmd, network);
            }
        }
        return true;
    }

    @Override
    public boolean applyIps(Network network,
            List<? extends PublicIpAddress> ipAddress, Set<Service> services)
            throws ResourceUnavailableException {
        return false;
    }

    @Override
    public boolean applyNetworkACLs(Network network,
            List<? extends NetworkACLItem> rules)
            throws ResourceUnavailableException {
        SubnetUtils utils;
        String cidr = null;
        List<String> cidrList;
        for(NetworkACLItem r: rules){
            if(r.getState()==NetworkACLItem.State.Revoke){
                continue;
            }
            cidrList = r.getSourceCidrList();
            if(cidrList != null){
                if(cidrList.size()>1 || !r.getSourcePortEnd().equals(r.getSourcePortStart())){
                    throw new ResourceUnavailableException("One CIDR and one port only please.",
                            Network.class, network.getId());
                } else {
                    cidr = cidrList.get(0);
                }
            }
            if (cidr == null || cidr.equalsIgnoreCase("0.0.0.0/0")) {
                cidr = "";
            } else {
                utils = new SubnetUtils(cidr);
                if(!utils.getInfo().getNetworkAddress().equals(utils.getInfo().getAddress())){
                    throw new ResourceUnavailableException("Invalid CIDR in Network ACL rule.",
                            Network.class, network.getId());
                }
            }
        }
        updateBcfRouter(network);
        return true;
    }

    @Override
    public boolean applyFWRules(Network network,
            List<? extends FirewallRule> rules)
            throws ResourceUnavailableException {
        SubnetUtils utils;
        String cidr = null;
        List<String> cidrList;
        for(FirewallRule r: rules){
            if(r.getState()==FirewallRule.State.Revoke){
                continue;
            }
            cidrList = r.getSourceCidrList();
            if(cidrList != null){
                if(cidrList.size()>1 || !r.getSourcePortEnd().equals(r.getSourcePortStart())){
                    throw new ResourceUnavailableException("One CIDR and one port only please.",
                            Network.class, network.getId());
                } else {
                    cidr = cidrList.get(0);
                }
            }
            if (cidr == null || cidr.equalsIgnoreCase("0.0.0.0/0")) {
                cidr = "";
            } else {
                utils = new SubnetUtils(cidr);
                if(!utils.getInfo().getNetworkAddress().equals(utils.getInfo().getAddress())){
                    throw new ResourceUnavailableException("Invalid CIDR in Firewall rule.",
                            Network.class, network.getId());
                }
            }
        }
        updateBcfRouter(network);
        return true;
    }

    private void updateBcfRouter(Network network) throws IllegalArgumentException{
        bcfUtilsInit();

        Vpc vpc = null;
        if(network.getVpcId()!=null){
            vpc = _vpcDao.acquireInLockTable(network.getVpcId());
        }

        String tenantId;
        if (vpc != null) {
            tenantId = vpc.getUuid();
            _vpcDao.releaseFromLockTable(vpc.getId());
        } else {
            tenantId = network.getUuid();
        }

        UpdateBcfRouterCommand cmd = new UpdateBcfRouterCommand(tenantId);

        List<AclData> aclList = _bcfUtils.listACLbyNetwork(network);
        for(AclData acl: aclList){
            cmd.addAcl(acl);
        }

        if(vpc != null){
            cmd.setPublicIp(_bcfUtils.getPublicIpByVpc(vpc));
        } else {
            cmd.setPublicIp(_bcfUtils.getPublicIpByNetwork(network));
        }

        BcfAnswer answer = _bcfUtils.sendBcfCommandWithNetworkSyncCheck(cmd, network);
        if(answer != null && !answer.getResult()){
            throw new IllegalArgumentException("Illegal router update arguments");
        }
    }

    private void bcfUtilsInit(){
        if (_bcfUtils == null) {
            _bcfUtils = new BigSwitchBcfUtils(_networkDao, _nicDao,
                    _vmDao, _hostDao, _vpcDao, _bigswitchBcfDao,
                    _agentMgr, _vlanDao, _ipAddressDao, _fwRulesDao,
                    _fwCidrsDao, _aclItemDao, _aclItemCidrsDao, _networkModel);
        }
    }
}
