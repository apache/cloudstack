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

import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Collections;
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

import org.apache.cloudstack.engine.orchestration.service.NetworkOrchestrationService;
import org.apache.cloudstack.network.ExternalNetworkDeviceManager.NetworkDevice;

import com.cloud.agent.AgentManager;
import com.cloud.agent.api.ConfigurePortForwardingRulesOnLogicalRouterAnswer;
import com.cloud.agent.api.ConfigurePortForwardingRulesOnLogicalRouterCommand;
import com.cloud.agent.api.ConfigurePublicIpsOnLogicalRouterAnswer;
import com.cloud.agent.api.ConfigurePublicIpsOnLogicalRouterCommand;
import com.cloud.agent.api.ConfigureStaticNatRulesOnLogicalRouterAnswer;
import com.cloud.agent.api.ConfigureStaticNatRulesOnLogicalRouterCommand;
import com.cloud.agent.api.CreateLogicalRouterAnswer;
import com.cloud.agent.api.CreateLogicalRouterCommand;
import com.cloud.agent.api.CreateLogicalSwitchPortAnswer;
import com.cloud.agent.api.CreateLogicalSwitchPortCommand;
import com.cloud.agent.api.DeleteLogicalRouterAnswer;
import com.cloud.agent.api.DeleteLogicalRouterCommand;
import com.cloud.agent.api.DeleteLogicalSwitchPortAnswer;
import com.cloud.agent.api.DeleteLogicalSwitchPortCommand;
import com.cloud.agent.api.FindLogicalSwitchPortAnswer;
import com.cloud.agent.api.FindLogicalSwitchPortCommand;
import com.cloud.agent.api.StartupCommand;
import com.cloud.agent.api.StartupNiciraNvpCommand;
import com.cloud.agent.api.UpdateLogicalSwitchPortCommand;
import com.cloud.agent.api.to.PortForwardingRuleTO;
import com.cloud.agent.api.to.StaticNatRuleTO;
import com.cloud.api.ApiDBUtils;
import com.cloud.api.commands.AddNiciraNvpDeviceCmd;
import com.cloud.api.commands.DeleteNiciraNvpDeviceCmd;
import com.cloud.api.commands.ListNiciraNvpDeviceNetworksCmd;
import com.cloud.api.commands.ListNiciraNvpDevicesCmd;
import com.cloud.api.response.NiciraNvpDeviceResponse;
import com.cloud.configuration.ConfigurationManager;
import com.cloud.dc.Vlan;
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
import com.cloud.network.IpAddress;
import com.cloud.network.IpAddressManager;
import com.cloud.network.Network;
import com.cloud.network.Network.Capability;
import com.cloud.network.Network.Provider;
import com.cloud.network.Network.Service;
import com.cloud.network.NetworkModel;
import com.cloud.network.Networks;
import com.cloud.network.Networks.BroadcastDomainType;
import com.cloud.network.NiciraNvpDeviceVO;
import com.cloud.network.NiciraNvpNicMappingVO;
import com.cloud.network.NiciraNvpRouterMappingVO;
import com.cloud.network.PhysicalNetwork;
import com.cloud.network.PhysicalNetworkServiceProvider;
import com.cloud.network.PublicIpAddress;
import com.cloud.network.addr.PublicIp;
import com.cloud.network.dao.NetworkDao;
import com.cloud.network.dao.NetworkServiceMapDao;
import com.cloud.network.dao.NetworkVO;
import com.cloud.network.dao.NiciraNvpDao;
import com.cloud.network.dao.NiciraNvpNicMappingDao;
import com.cloud.network.dao.NiciraNvpRouterMappingDao;
import com.cloud.network.dao.PhysicalNetworkDao;
import com.cloud.network.dao.PhysicalNetworkServiceProviderDao;
import com.cloud.network.dao.PhysicalNetworkServiceProviderVO;
import com.cloud.network.dao.PhysicalNetworkVO;
import com.cloud.network.resource.NiciraNvpResource;
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
import com.cloud.utils.db.DB;
import com.cloud.utils.db.Transaction;
import com.cloud.utils.db.TransactionCallback;
import com.cloud.utils.db.TransactionStatus;
import com.cloud.utils.exception.CloudRuntimeException;
import com.cloud.utils.net.NetUtils;
import com.cloud.vm.NicProfile;
import com.cloud.vm.NicVO;
import com.cloud.vm.ReservationContext;
import com.cloud.vm.VirtualMachineProfile;
import com.cloud.vm.dao.NicDao;

@Component
@Local(value = {NetworkElement.class, ConnectivityProvider.class, SourceNatServiceProvider.class, StaticNatServiceProvider.class, PortForwardingServiceProvider.class,
        IpDeployer.class})
public class NiciraNvpElement extends AdapterBase implements ConnectivityProvider, SourceNatServiceProvider, PortForwardingServiceProvider, StaticNatServiceProvider,
NiciraNvpElementService, ResourceStateAdapter, IpDeployer {

    private static final int MAX_PORT = 65535;
    private static final int MIN_PORT = 0;

    private static final Logger s_logger = Logger.getLogger(NiciraNvpElement.class);

    private static final Map<Service, Map<Capability, String>> capabilities = setCapabilities();

    @Inject
    protected NicDao nicDao;
    @Inject
    protected ResourceManager resourceMgr;
    @Inject
    protected PhysicalNetworkDao physicalNetworkDao;
    @Inject
    protected PhysicalNetworkServiceProviderDao physicalNetworkServiceProviderDao;
    @Inject
    protected NiciraNvpDao niciraNvpDao;
    @Inject
    protected HostDetailsDao hostDetailsDao;
    @Inject
    protected HostDao hostDao;
    @Inject
    protected AgentManager agentMgr;
    @Inject
    protected NiciraNvpNicMappingDao niciraNvpNicMappingDao;
    @Inject
    protected NiciraNvpRouterMappingDao niciraNvpRouterMappingDao;
    @Inject
    protected NetworkDao networkDao;
    @Inject
    protected NetworkOrchestrationService networkManager;
    @Inject
    protected NetworkModel networkModel;
    @Inject
    protected ConfigurationManager configMgr;
    @Inject
    protected NetworkServiceMapDao ntwkSrvcDao;
    @Inject
    protected VlanDao vlanDao;
    @Inject
    protected IpAddressManager ipAddrMgr;

    @Override
    public Map<Service, Map<Capability, String>> getCapabilities() {
        return capabilities;
    }

    @Override
    public Provider getProvider() {
        return Provider.NiciraNvp;
    }

    protected boolean canHandle(Network network, Service service) {
        s_logger.debug("Checking if NiciraNvpElement can handle service " + service.getName() + " on network " + network.getDisplayText());
        if (network.getBroadcastDomainType() != BroadcastDomainType.Lswitch) {
            return false;
        }

        if (!networkModel.isProviderForNetwork(getProvider(), network.getId())) {
            s_logger.debug("NiciraNvpElement is not a provider for network " + network.getDisplayText());
            return false;
        }

        if (!ntwkSrvcDao.canProviderSupportServiceInNetwork(network.getId(), service, Network.Provider.NiciraNvp)) {
            s_logger.debug("NiciraNvpElement can't provide the " + service.getName() + " service on network " + network.getDisplayText());
            return false;
        }

        return true;
    }

    @Override
    public boolean configure(String name, Map<String, Object> params) throws ConfigurationException {
        super.configure(name, params);
        resourceMgr.registerResourceStateAdapter(name, this);
        return true;
    }

    @Override
    public boolean implement(Network network, NetworkOffering offering, DeployDestination dest, ReservationContext context) throws ConcurrentOperationException,
    ResourceUnavailableException, InsufficientCapacityException {
        s_logger.debug("entering NiciraNvpElement implement function for network " + network.getDisplayText() + " (state " + network.getState() + ")");

        if (!canHandle(network, Service.Connectivity)) {
            return false;
        }

        if (network.getBroadcastUri() == null) {
            s_logger.error("Nic has no broadcast Uri with the LSwitch Uuid");
            return false;
        }

        List<NiciraNvpDeviceVO> devices = niciraNvpDao.listByPhysicalNetwork(network.getPhysicalNetworkId());
        if (devices.isEmpty()) {
            s_logger.error("No NiciraNvp Controller on physical network " + network.getPhysicalNetworkId());
            return false;
        }
        NiciraNvpDeviceVO niciraNvpDevice = devices.get(0);
        HostVO niciraNvpHost = hostDao.findById(niciraNvpDevice.getHostId());
        hostDao.loadDetails(niciraNvpHost);

        Account owner = context.getAccount();

        /*
         * TODO Shouldn't we lock the network as we might need to do
         * multiple operations that should be done only once.
         */

        // Implement SourceNat immediately as we have al the info already
        if (networkModel.isProviderSupportServiceInNetwork(network.getId(), Service.SourceNat, Provider.NiciraNvp)) {
            s_logger.debug("Apparently we are supposed to provide SourceNat on this network");

            PublicIp sourceNatIp = ipAddrMgr.assignSourceNatIpAddressToGuestNetwork(owner, network);
            String publicCidr = sourceNatIp.getAddress().addr() + "/" + NetUtils.getCidrSize(sourceNatIp.getVlanNetmask());
            String internalCidr = network.getGateway() + "/" + network.getCidr().split("/")[1];
            // assuming a vlan:
            String vtag = sourceNatIp.getVlanTag();
            BroadcastDomainType tiep = null;
            try {
                tiep = BroadcastDomainType.getTypeOf(vtag);
            } catch (URISyntaxException use) {
                throw new CloudRuntimeException("vlantag for sourceNatIp is not valid: " + vtag, use);
            }
            if (tiep == BroadcastDomainType.Vlan) {
                vtag = BroadcastDomainType.Vlan.getValueFrom(BroadcastDomainType.fromString(vtag));
            } else if (!(tiep == BroadcastDomainType.UnDecided || tiep == BroadcastDomainType.Native)) {
                throw new CloudRuntimeException("only vlans are supported for sourceNatIp, at this moment: " + vtag);
            }
            long vlanid = (Vlan.UNTAGGED.equals(vtag)) ? 0 : Long.parseLong(vtag);

            CreateLogicalRouterCommand cmd =
                    new CreateLogicalRouterCommand(niciraNvpHost.getDetail("l3gatewayserviceuuid"), vlanid, BroadcastDomainType.getValue(network.getBroadcastUri()),
                            "router-" + network.getDisplayText(), publicCidr, sourceNatIp.getGateway(), internalCidr, context.getDomain().getName() + "-" +
                                    context.getAccount().getAccountName());
            CreateLogicalRouterAnswer answer = (CreateLogicalRouterAnswer)agentMgr.easySend(niciraNvpHost.getId(), cmd);
            if (answer.getResult() == false) {
                s_logger.error("Failed to create Logical Router for network " + network.getDisplayText());
                return false;
            }

            // Store the uuid so we can easily find it during cleanup
            NiciraNvpRouterMappingVO routermapping = new NiciraNvpRouterMappingVO(answer.getLogicalRouterUuid(), network.getId());
            niciraNvpRouterMappingDao.persist(routermapping);
        }

        return true;
    }

    @Override
    public boolean prepare(Network network, NicProfile nic, VirtualMachineProfile vm, DeployDestination dest, ReservationContext context)
            throws ConcurrentOperationException, ResourceUnavailableException, InsufficientCapacityException {

        if (!canHandle(network, Service.Connectivity)) {
            return false;
        }

        if (network.getBroadcastUri() == null) {
            s_logger.error("Nic has no broadcast Uri with the LSwitch Uuid");
            return false;
        }

        NicVO nicVO = nicDao.findById(nic.getId());

        List<NiciraNvpDeviceVO> devices = niciraNvpDao.listByPhysicalNetwork(network.getPhysicalNetworkId());
        if (devices.isEmpty()) {
            s_logger.error("No NiciraNvp Controller on physical network " + network.getPhysicalNetworkId());
            return false;
        }
        NiciraNvpDeviceVO niciraNvpDevice = devices.get(0);
        HostVO niciraNvpHost = hostDao.findById(niciraNvpDevice.getHostId());

        NiciraNvpNicMappingVO existingNicMap = niciraNvpNicMappingDao.findByNicUuid(nicVO.getUuid());
        if (existingNicMap != null) {
            FindLogicalSwitchPortCommand findCmd = new FindLogicalSwitchPortCommand(existingNicMap.getLogicalSwitchUuid(), existingNicMap.getLogicalSwitchPortUuid());
            FindLogicalSwitchPortAnswer answer = (FindLogicalSwitchPortAnswer)agentMgr.easySend(niciraNvpHost.getId(), findCmd);

            if (answer.getResult()) {
                s_logger.warn("Existing Logical Switchport found for nic " + nic.getName() + " with uuid " + existingNicMap.getLogicalSwitchPortUuid());
                UpdateLogicalSwitchPortCommand cmd =
                        new UpdateLogicalSwitchPortCommand(existingNicMap.getLogicalSwitchPortUuid(), BroadcastDomainType.getValue(network.getBroadcastUri()),
                                nicVO.getUuid(), context.getDomain().getName() + "-" + context.getAccount().getAccountName(), nic.getName());
                agentMgr.easySend(niciraNvpHost.getId(), cmd);
                return true;
            } else {
                s_logger.error("Stale entry found for nic " + nic.getName() + " with logical switchport uuid " + existingNicMap.getLogicalSwitchPortUuid());
                niciraNvpNicMappingDao.remove(existingNicMap.getId());
            }
        }

        CreateLogicalSwitchPortCommand cmd =
                new CreateLogicalSwitchPortCommand(BroadcastDomainType.getValue(network.getBroadcastUri()), nicVO.getUuid(), context.getDomain().getName() + "-" +
                        context.getAccount().getAccountName(), nic.getName());
        CreateLogicalSwitchPortAnswer answer = (CreateLogicalSwitchPortAnswer)agentMgr.easySend(niciraNvpHost.getId(), cmd);

        if (answer == null || !answer.getResult()) {
            s_logger.error("CreateLogicalSwitchPortCommand failed");
            return false;
        }

        NiciraNvpNicMappingVO nicMap =
                new NiciraNvpNicMappingVO(BroadcastDomainType.getValue(network.getBroadcastUri()), answer.getLogicalSwitchPortUuid(), nicVO.getUuid());
        niciraNvpNicMappingDao.persist(nicMap);

        return true;
    }

    @Override
    public boolean release(Network network, NicProfile nic, VirtualMachineProfile vm, ReservationContext context) throws ConcurrentOperationException,
    ResourceUnavailableException {

        if (!canHandle(network, Service.Connectivity)) {
            return false;
        }

        if (network.getBroadcastUri() == null) {
            s_logger.error("Nic has no broadcast Uri with the LSwitch Uuid");
            return false;
        }

        NicVO nicVO = nicDao.findById(nic.getId());

        List<NiciraNvpDeviceVO> devices = niciraNvpDao.listByPhysicalNetwork(network.getPhysicalNetworkId());
        if (devices.isEmpty()) {
            s_logger.error("No NiciraNvp Controller on physical network " + network.getPhysicalNetworkId());
            return false;
        }
        NiciraNvpDeviceVO niciraNvpDevice = devices.get(0);
        HostVO niciraNvpHost = hostDao.findById(niciraNvpDevice.getHostId());

        NiciraNvpNicMappingVO nicMap = niciraNvpNicMappingDao.findByNicUuid(nicVO.getUuid());
        if (nicMap == null) {
            s_logger.error("No mapping for nic " + nic.getName());
            return false;
        }

        DeleteLogicalSwitchPortCommand cmd = new DeleteLogicalSwitchPortCommand(nicMap.getLogicalSwitchUuid(), nicMap.getLogicalSwitchPortUuid());
        DeleteLogicalSwitchPortAnswer answer = (DeleteLogicalSwitchPortAnswer)agentMgr.easySend(niciraNvpHost.getId(), cmd);

        if (answer == null || !answer.getResult()) {
            s_logger.error("DeleteLogicalSwitchPortCommand failed");
            return false;
        }

        niciraNvpNicMappingDao.remove(nicMap.getId());

        return true;
    }

    @Override
    public boolean shutdown(Network network, ReservationContext context, boolean cleanup) throws ConcurrentOperationException, ResourceUnavailableException {
        if (!canHandle(network, Service.Connectivity)) {
            return false;
        }

        List<NiciraNvpDeviceVO> devices = niciraNvpDao.listByPhysicalNetwork(network.getPhysicalNetworkId());
        if (devices.isEmpty()) {
            s_logger.error("No NiciraNvp Controller on physical network " + network.getPhysicalNetworkId());
            return false;
        }
        NiciraNvpDeviceVO niciraNvpDevice = devices.get(0);
        HostVO niciraNvpHost = hostDao.findById(niciraNvpDevice.getHostId());

        if (networkModel.isProviderSupportServiceInNetwork(network.getId(), Service.SourceNat, Provider.NiciraNvp)) {
            s_logger.debug("Apparently we were providing SourceNat on this network");

            // Deleting the LogicalRouter will also take care of all provisioned
            // nat rules.
            NiciraNvpRouterMappingVO routermapping = niciraNvpRouterMappingDao.findByNetworkId(network.getId());
            if (routermapping == null) {
                s_logger.warn("No logical router uuid found for network " + network.getDisplayText());
                // This might be cause by a failed deployment, so don't make shutdown fail as well.
                return true;
            }

            DeleteLogicalRouterCommand cmd = new DeleteLogicalRouterCommand(routermapping.getLogicalRouterUuid());
            DeleteLogicalRouterAnswer answer = (DeleteLogicalRouterAnswer)agentMgr.easySend(niciraNvpHost.getId(), cmd);
            if (answer.getResult() == false) {
                s_logger.error("Failed to delete LogicalRouter for network " + network.getDisplayText());
                return false;
            }

            niciraNvpRouterMappingDao.remove(routermapping.getId());
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
        // Nothing to do here.
        return true;
    }

    @Override
    public boolean canEnableIndividualServices() {
        return true;
    }

    @Override
    public boolean verifyServicesCombination(Set<Service> services) {
        // This element can only function in a Nicra Nvp based
        // SDN network, so Connectivity needs to be present here
        if (!services.contains(Service.Connectivity)) {
            s_logger.warn("Unable to provide services without Connectivity service enabled for this element");
            return false;
        }
        if ((services.contains(Service.PortForwarding) || services.contains(Service.StaticNat)) && !services.contains(Service.SourceNat)) {
            s_logger.warn("Unable to provide StaticNat and/or PortForwarding without the SourceNat service");
            return false;
        }
        return true;
    }

    private static Map<Service, Map<Capability, String>> setCapabilities() {
        Map<Service, Map<Capability, String>> capabilities = new HashMap<Service, Map<Capability, String>>();

        // L2 Support : SDN provisioning
        capabilities.put(Service.Connectivity, null);

        // L3 Support : Generic?
        capabilities.put(Service.Gateway, null);

        // L3 Support : SourceNat
        Map<Capability, String> sourceNatCapabilities = new HashMap<Capability, String>();
        sourceNatCapabilities.put(Capability.SupportedSourceNatTypes, "peraccount");
        sourceNatCapabilities.put(Capability.RedundantRouter, "false");
        capabilities.put(Service.SourceNat, sourceNatCapabilities);

        // L3 Support : Port Forwarding
        capabilities.put(Service.PortForwarding, null);

        // L3 support : StaticNat
        capabilities.put(Service.StaticNat, null);

        return capabilities;
    }

    @Override
    public List<Class<?>> getCommands() {
        List<Class<?>> cmdList = new ArrayList<Class<?>>();
        cmdList.add(AddNiciraNvpDeviceCmd.class);
        cmdList.add(DeleteNiciraNvpDeviceCmd.class);
        cmdList.add(ListNiciraNvpDeviceNetworksCmd.class);
        cmdList.add(ListNiciraNvpDevicesCmd.class);
        return cmdList;
    }

    @Override
    @DB
    public NiciraNvpDeviceVO addNiciraNvpDevice(AddNiciraNvpDeviceCmd cmd) {
        ServerResource resource = new NiciraNvpResource();
        final String deviceName = Network.Provider.NiciraNvp.getName();
        NetworkDevice networkDevice = NetworkDevice.getNetworkDevice(deviceName);
        if (networkDevice == null) {
            throw new CloudRuntimeException("No network device found for " + deviceName);
        }
        final Long physicalNetworkId = cmd.getPhysicalNetworkId();
        PhysicalNetworkVO physicalNetwork = physicalNetworkDao.findById(physicalNetworkId);
        if (physicalNetwork == null) {
            throw new InvalidParameterValueException("Could not find phyical network with ID: " + physicalNetworkId);
        }
        long zoneId = physicalNetwork.getDataCenterId();

        final PhysicalNetworkServiceProviderVO ntwkSvcProvider =
                physicalNetworkServiceProviderDao.findByServiceProvider(physicalNetwork.getId(), networkDevice.getNetworkServiceProvder());
        if (ntwkSvcProvider == null) {
            throw new CloudRuntimeException("Network Service Provider: " + networkDevice.getNetworkServiceProvder() + " is not enabled in the physical network: " +
                    physicalNetworkId + "to add this device");
        } else if (ntwkSvcProvider.getState() == PhysicalNetworkServiceProvider.State.Shutdown) {
            throw new CloudRuntimeException("Network Service Provider: " + ntwkSvcProvider.getProviderName() + " is in shutdown state in the physical network: " +
                    physicalNetworkId + "to add this device");
        }

        if (niciraNvpDao.listByPhysicalNetwork(physicalNetworkId).size() != 0) {
            throw new CloudRuntimeException("A NiciraNvp device is already configured on this physical network");
        }

        Map<String, String> params = new HashMap<String, String>();
        params.put("guid", UUID.randomUUID().toString());
        params.put("zoneId", String.valueOf(physicalNetwork.getDataCenterId()));
        params.put("physicalNetworkId", String.valueOf(physicalNetwork.getId()));
        params.put("name", "Nicira Controller - " + cmd.getHost());
        params.put("ip", cmd.getHost());
        params.put("adminuser", cmd.getUsername());
        params.put("adminpass", cmd.getPassword());
        params.put("transportzoneuuid", cmd.getTransportzoneUuid());
        // FIXME What to do with multiple isolation types
        params.put("transportzoneisotype", physicalNetwork.getIsolationMethods().get(0).toLowerCase());
        if (cmd.getL3GatewayServiceUuid() != null) {
            params.put("l3gatewayserviceuuid", cmd.getL3GatewayServiceUuid());
        }

        Map<String, Object> hostdetails = new HashMap<String, Object>();
        hostdetails.putAll(params);

        try {
            resource.configure(cmd.getHost(), hostdetails);

            final Host host = resourceMgr.addHost(zoneId, resource, Host.Type.L2Networking, params);
            if (host != null) {
                return Transaction.execute(new TransactionCallback<NiciraNvpDeviceVO>() {
                    @Override
                    public NiciraNvpDeviceVO doInTransaction(TransactionStatus status) {
                        NiciraNvpDeviceVO niciraNvpDevice = new NiciraNvpDeviceVO(host.getId(), physicalNetworkId, ntwkSvcProvider.getProviderName(), deviceName);
                        niciraNvpDao.persist(niciraNvpDevice);

                        DetailVO detail = new DetailVO(host.getId(), "niciranvpdeviceid", String.valueOf(niciraNvpDevice.getId()));
                        hostDetailsDao.persist(detail);

                        return niciraNvpDevice;
                    }
                });
            } else {
                throw new CloudRuntimeException("Failed to add Nicira Nvp Device due to internal error.");
            }
        } catch (ConfigurationException e) {
            throw new CloudRuntimeException(e.getMessage());
        }
    }

    @Override
    public NiciraNvpDeviceResponse createNiciraNvpDeviceResponse(NiciraNvpDeviceVO niciraNvpDeviceVO) {
        HostVO niciraNvpHost = hostDao.findById(niciraNvpDeviceVO.getHostId());
        hostDao.loadDetails(niciraNvpHost);

        NiciraNvpDeviceResponse response = new NiciraNvpDeviceResponse();
        response.setDeviceName(niciraNvpDeviceVO.getDeviceName());
        PhysicalNetwork pnw = ApiDBUtils.findPhysicalNetworkById(niciraNvpDeviceVO.getPhysicalNetworkId());
        if (pnw != null) {
            response.setPhysicalNetworkId(pnw.getUuid());
        }
        response.setId(niciraNvpDeviceVO.getUuid());
        response.setProviderName(niciraNvpDeviceVO.getProviderName());
        response.setHostName(niciraNvpHost.getDetail("ip"));
        response.setTransportZoneUuid(niciraNvpHost.getDetail("transportzoneuuid"));
        response.setL3GatewayServiceUuid(niciraNvpHost.getDetail("l3gatewayserviceuuid"));
        response.setObjectName("niciranvpdevice");
        return response;
    }

    @Override
    public boolean deleteNiciraNvpDevice(DeleteNiciraNvpDeviceCmd cmd) {
        Long niciraDeviceId = cmd.getNiciraNvpDeviceId();
        NiciraNvpDeviceVO niciraNvpDevice = niciraNvpDao.findById(niciraDeviceId);
        if (niciraNvpDevice == null) {
            throw new InvalidParameterValueException("Could not find a nicira device with id " + niciraDeviceId);
        }

        // Find the physical network we work for
        Long physicalNetworkId = niciraNvpDevice.getPhysicalNetworkId();
        PhysicalNetworkVO physicalNetwork = physicalNetworkDao.findById(physicalNetworkId);
        if (physicalNetwork != null) {
            // Lets see if there are networks that use us
            // Find the nicira networks on this physical network
            List<NetworkVO> networkList = networkDao.listByPhysicalNetwork(physicalNetworkId);
            if (networkList != null) {
                // Networks with broadcast type lswitch are ours
                for (NetworkVO network : networkList) {
                    if (network.getBroadcastDomainType() == Networks.BroadcastDomainType.Lswitch) {
                        if ((network.getState() != Network.State.Shutdown) && (network.getState() != Network.State.Destroy)) {
                            throw new CloudRuntimeException("This Nicira Nvp device can not be deleted as there are one or more logical networks provisioned by cloudstack.");
                        }
                    }
                }
            }
        }

        HostVO niciraHost = hostDao.findById(niciraNvpDevice.getHostId());
        Long hostId = niciraHost.getId();

        niciraHost.setResourceState(ResourceState.Maintenance);
        hostDao.update(hostId, niciraHost);
        resourceMgr.deleteHost(hostId, false, false);

        niciraNvpDao.remove(niciraDeviceId);
        return true;
    }

    @Override
    public List<NiciraNvpDeviceVO> listNiciraNvpDevices(ListNiciraNvpDevicesCmd cmd) {
        Long physicalNetworkId = cmd.getPhysicalNetworkId();
        Long niciraNvpDeviceId = cmd.getNiciraNvpDeviceId();
        List<NiciraNvpDeviceVO> responseList = new ArrayList<NiciraNvpDeviceVO>();

        if (physicalNetworkId == null && niciraNvpDeviceId == null) {
            throw new InvalidParameterValueException("Either physical network Id or nicira device Id must be specified");
        }

        if (niciraNvpDeviceId != null) {
            NiciraNvpDeviceVO niciraNvpDevice = niciraNvpDao.findById(niciraNvpDeviceId);
            if (niciraNvpDevice == null) {
                throw new InvalidParameterValueException("Could not find Nicira Nvp device with id: " + niciraNvpDevice);
            }
            responseList.add(niciraNvpDevice);
        } else {
            PhysicalNetworkVO physicalNetwork = physicalNetworkDao.findById(physicalNetworkId);
            if (physicalNetwork == null) {
                throw new InvalidParameterValueException("Could not find a physical network with id: " + physicalNetworkId);
            }
            responseList = niciraNvpDao.listByPhysicalNetwork(physicalNetworkId);
        }

        return responseList;
    }

    @Override
    public List<? extends Network> listNiciraNvpDeviceNetworks(ListNiciraNvpDeviceNetworksCmd cmd) {
        Long niciraDeviceId = cmd.getNiciraNvpDeviceId();
        NiciraNvpDeviceVO niciraNvpDevice = niciraNvpDao.findById(niciraDeviceId);
        if (niciraNvpDevice == null) {
            throw new InvalidParameterValueException("Could not find a nicira device with id " + niciraDeviceId);
        }

        // Find the physical network we work for
        Long physicalNetworkId = niciraNvpDevice.getPhysicalNetworkId();
        PhysicalNetworkVO physicalNetwork = physicalNetworkDao.findById(physicalNetworkId);
        if (physicalNetwork == null) {
            // No such physical network, so no provisioned networks
            return Collections.emptyList();
        }

        // Find the nicira networks on this physical network
        List<NetworkVO> networkList = networkDao.listByPhysicalNetwork(physicalNetworkId);
        if (networkList == null) {
            return Collections.emptyList();
        }

        // Networks with broadcast type lswitch are ours
        List<NetworkVO> responseList = new ArrayList<NetworkVO>();
        for (NetworkVO network : networkList) {
            if (network.getBroadcastDomainType() == Networks.BroadcastDomainType.Lswitch) {
                responseList.add(network);
            }
        }

        return responseList;
    }

    @Override
    public HostVO createHostVOForConnectedAgent(HostVO host, StartupCommand[] cmd) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public HostVO createHostVOForDirectConnectAgent(HostVO host, StartupCommand[] startup, ServerResource resource, Map<String, String> details, List<String> hostTags) {
        if (!(startup[0] instanceof StartupNiciraNvpCommand)) {
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

    /**
     * From interface SourceNatServiceProvider
     */
    @Override
    public IpDeployer getIpDeployer(Network network) {
        return this;
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
        if (services.contains(Service.SourceNat)) {
            // Only if we need to provide SourceNat we need to configure the logical router
            // SourceNat is required for StaticNat and PortForwarding
            List<NiciraNvpDeviceVO> devices = niciraNvpDao.listByPhysicalNetwork(network.getPhysicalNetworkId());
            if (devices.isEmpty()) {
                s_logger.error("No NiciraNvp Controller on physical network " + network.getPhysicalNetworkId());
                return false;
            }
            NiciraNvpDeviceVO niciraNvpDevice = devices.get(0);
            HostVO niciraNvpHost = hostDao.findById(niciraNvpDevice.getHostId());
            hostDao.loadDetails(niciraNvpHost);

            NiciraNvpRouterMappingVO routermapping = niciraNvpRouterMappingDao.findByNetworkId(network.getId());
            if (routermapping == null) {
                s_logger.error("No logical router uuid found for network " + network.getDisplayText());
                return false;
            }

            List<String> cidrs = new ArrayList<String>();
            for (PublicIpAddress ip : ipAddress) {
                if (ip.getState() == IpAddress.State.Releasing) {
                    // If we are releasing we don't need to push this ip to
                    // the Logical Router
                    continue;
                }
                cidrs.add(ip.getAddress().addr() + "/" + NetUtils.getCidrSize(ip.getNetmask()));
            }
            ConfigurePublicIpsOnLogicalRouterCommand cmd =
                    new ConfigurePublicIpsOnLogicalRouterCommand(routermapping.getLogicalRouterUuid(), niciraNvpHost.getDetail("l3gatewayserviceuuid"), cidrs);
            ConfigurePublicIpsOnLogicalRouterAnswer answer = (ConfigurePublicIpsOnLogicalRouterAnswer)agentMgr.easySend(niciraNvpHost.getId(), cmd);
            //FIXME answer can be null if the host is down
            return answer.getResult();
        } else {
            s_logger.debug("No need to provision ip addresses as we are not providing L3 services.");
        }

        return true;
    }

    /**
     * From interface StaticNatServiceProvider
     */
    @Override
    public boolean applyStaticNats(Network network, List<? extends StaticNat> rules) throws ResourceUnavailableException {
        if (!canHandle(network, Service.StaticNat)) {
            return false;
        }

        List<NiciraNvpDeviceVO> devices = niciraNvpDao.listByPhysicalNetwork(network.getPhysicalNetworkId());
        if (devices.isEmpty()) {
            s_logger.error("No NiciraNvp Controller on physical network " + network.getPhysicalNetworkId());
            return false;
        }
        NiciraNvpDeviceVO niciraNvpDevice = devices.get(0);
        HostVO niciraNvpHost = hostDao.findById(niciraNvpDevice.getHostId());

        NiciraNvpRouterMappingVO routermapping = niciraNvpRouterMappingDao.findByNetworkId(network.getId());
        if (routermapping == null) {
            s_logger.error("No logical router uuid found for network " + network.getDisplayText());
            return false;
        }

        List<StaticNatRuleTO> staticNatRules = new ArrayList<StaticNatRuleTO>();
        for (StaticNat rule : rules) {
            IpAddress sourceIp = networkModel.getIp(rule.getSourceIpAddressId());
            // Force the nat rule into the StaticNatRuleTO, no use making a new TO object
            // we only need the source and destination ip. Unfortunately no mention if a rule
            // is new.
            StaticNatRuleTO ruleTO =
                    new StaticNatRuleTO(1, sourceIp.getAddress().addr(), MIN_PORT, MAX_PORT, rule.getDestIpAddress(), MIN_PORT, MAX_PORT, "any", rule.isForRevoke(), false);
            staticNatRules.add(ruleTO);
        }

        ConfigureStaticNatRulesOnLogicalRouterCommand cmd = new ConfigureStaticNatRulesOnLogicalRouterCommand(routermapping.getLogicalRouterUuid(), staticNatRules);
        ConfigureStaticNatRulesOnLogicalRouterAnswer answer = (ConfigureStaticNatRulesOnLogicalRouterAnswer)agentMgr.easySend(niciraNvpHost.getId(), cmd);

        return answer.getResult();
    }

    /**
     * From interface PortForwardingServiceProvider
     */
    @Override
    public boolean applyPFRules(Network network, List<PortForwardingRule> rules) throws ResourceUnavailableException {
        if (!canHandle(network, Service.PortForwarding)) {
            return false;
        }

        List<NiciraNvpDeviceVO> devices = niciraNvpDao.listByPhysicalNetwork(network.getPhysicalNetworkId());
        if (devices.isEmpty()) {
            s_logger.error("No NiciraNvp Controller on physical network " + network.getPhysicalNetworkId());
            return false;
        }
        NiciraNvpDeviceVO niciraNvpDevice = devices.get(0);
        HostVO niciraNvpHost = hostDao.findById(niciraNvpDevice.getHostId());

        NiciraNvpRouterMappingVO routermapping = niciraNvpRouterMappingDao.findByNetworkId(network.getId());
        if (routermapping == null) {
            s_logger.error("No logical router uuid found for network " + network.getDisplayText());
            return false;
        }

        List<PortForwardingRuleTO> portForwardingRules = new ArrayList<PortForwardingRuleTO>();
        for (PortForwardingRule rule : rules) {
            IpAddress sourceIp = networkModel.getIp(rule.getSourceIpAddressId());
            Vlan vlan = vlanDao.findById(sourceIp.getVlanId());
            PortForwardingRuleTO ruleTO = new PortForwardingRuleTO(rule, vlan.getVlanTag(), sourceIp.getAddress().addr());
            portForwardingRules.add(ruleTO);
        }

        ConfigurePortForwardingRulesOnLogicalRouterCommand cmd =
                new ConfigurePortForwardingRulesOnLogicalRouterCommand(routermapping.getLogicalRouterUuid(), portForwardingRules);
        ConfigurePortForwardingRulesOnLogicalRouterAnswer answer = (ConfigurePortForwardingRulesOnLogicalRouterAnswer)agentMgr.easySend(niciraNvpHost.getId(), cmd);

        return answer.getResult();
    }

}
