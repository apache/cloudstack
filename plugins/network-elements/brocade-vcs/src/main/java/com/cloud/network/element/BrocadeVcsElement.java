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
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import javax.inject.Inject;
import javax.naming.ConfigurationException;

import org.apache.cloudstack.engine.orchestration.service.NetworkOrchestrationService;
import org.apache.cloudstack.network.ExternalNetworkDeviceManager.NetworkDevice;
import org.springframework.stereotype.Component;

import com.cloud.agent.AgentManager;
import com.cloud.agent.api.StartupBrocadeVcsCommand;
import com.cloud.agent.api.StartupCommand;
import com.cloud.api.ApiDBUtils;
import com.cloud.api.commands.AddBrocadeVcsDeviceCmd;
import com.cloud.api.commands.DeleteBrocadeVcsDeviceCmd;
import com.cloud.api.commands.ListBrocadeVcsDeviceNetworksCmd;
import com.cloud.api.commands.ListBrocadeVcsDevicesCmd;
import com.cloud.api.response.BrocadeVcsDeviceResponse;
import com.cloud.configuration.ConfigurationManager;
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
import com.cloud.network.BrocadeVcsDeviceVO;
import com.cloud.network.IpAddressManager;
import com.cloud.network.Network;
import com.cloud.network.Networks;
import com.cloud.network.PhysicalNetwork;
import com.cloud.network.Network.Capability;
import com.cloud.network.Network.Provider;
import com.cloud.network.Network.Service;
import com.cloud.network.NetworkModel;
import com.cloud.network.Networks.BroadcastDomainType;
import com.cloud.network.PhysicalNetworkServiceProvider;
import com.cloud.network.dao.BrocadeVcsDao;
import com.cloud.network.dao.NetworkDao;
import com.cloud.network.dao.NetworkServiceMapDao;
import com.cloud.network.dao.NetworkVO;
import com.cloud.network.dao.PhysicalNetworkDao;
import com.cloud.network.dao.PhysicalNetworkServiceProviderDao;
import com.cloud.network.dao.PhysicalNetworkServiceProviderVO;
import com.cloud.network.dao.PhysicalNetworkVO;
import com.cloud.network.resource.BrocadeVcsResource;
import com.cloud.offering.NetworkOffering;
import com.cloud.resource.ResourceManager;
import com.cloud.resource.ResourceState;
import com.cloud.resource.ResourceStateAdapter;
import com.cloud.resource.ServerResource;
import com.cloud.resource.UnableDeleteHostException;
import com.cloud.utils.component.AdapterBase;
import com.cloud.utils.db.DB;
import com.cloud.utils.db.Transaction;
import com.cloud.utils.db.TransactionCallback;
import com.cloud.utils.db.TransactionStatus;
import com.cloud.utils.exception.CloudRuntimeException;
import com.cloud.vm.NicProfile;
import com.cloud.vm.ReservationContext;
import com.cloud.vm.VirtualMachineProfile;
import com.cloud.vm.dao.NicDao;

@Component
public class BrocadeVcsElement extends AdapterBase implements NetworkElement, ResourceStateAdapter, BrocadeVcsElementService {

    private static final Map<Service, Map<Capability, String>> capabilities = setCapabilities();

    @Inject
    NicDao _nicDao;
    @Inject
    ResourceManager _resourceMgr;
    @Inject
    PhysicalNetworkDao _physicalNetworkDao;
    @Inject
    PhysicalNetworkServiceProviderDao _physicalNetworkServiceProviderDao;
    @Inject
    protected BrocadeVcsDao _brocadeVcsDao;
    @Inject
    HostDetailsDao _hostDetailsDao;
    @Inject
    HostDao _hostDao;
    @Inject
    AgentManager _agentMgr;
    @Inject
    NetworkDao _networkDao;
    @Inject
    NetworkOrchestrationService _networkManager;
    @Inject
    NetworkModel _networkModel;
    @Inject
    ConfigurationManager _configMgr;
    @Inject
    NetworkServiceMapDao _ntwkSrvcDao;
    @Inject
    VlanDao _vlanDao;
    @Inject
    IpAddressManager _ipAddrMgr;

    @Override
    public Map<Service, Map<Capability, String>> getCapabilities() {
        return capabilities;
    }

    @Override
    public Provider getProvider() {
        return Network.Provider.BrocadeVcs;
    }

    protected boolean canHandle(Network network, Service service) {
        logger.debug("Checking if BrocadeVcsElement can handle service " + service.getName() + " on network " + network.getDisplayText());
        if (network.getBroadcastDomainType() != BroadcastDomainType.Vcs) {
            return false;
        }

        if (!_networkModel.isProviderForNetwork(getProvider(), network.getId())) {
            logger.debug("BrocadeVcsElement is not a provider for network " + network.getDisplayText());
            return false;
        }

        if (!_ntwkSrvcDao.canProviderSupportServiceInNetwork(network.getId(), service, Network.Provider.BrocadeVcs)) {
            logger.debug("BrocadeVcsElement can't provide the " + service.getName() + " service on network " + network.getDisplayText());
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
        logger.debug("entering BrocadeVcsElement implement function for network " + network.getDisplayText() + " (state " + network.getState() + ")");

        if (!canHandle(network, Service.Connectivity)) {
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

        return true;
    }

    @Override
    public boolean release(Network network, NicProfile nic, VirtualMachineProfile vm, ReservationContext context) throws ConcurrentOperationException, ResourceUnavailableException {

        if (!canHandle(network, Service.Connectivity)) {
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
    public boolean shutdownProviderInstances(PhysicalNetworkServiceProvider provider, ReservationContext context) throws ConcurrentOperationException, ResourceUnavailableException {
        // Nothing to do here.
        return true;
    }

    @Override
    public boolean canEnableIndividualServices() {
        return true;
    }

    @Override
    public boolean verifyServicesCombination(Set<Service> services) {

        if (!services.contains(Service.Connectivity)) {
            logger.warn("Unable to provide services without Connectivity service enabled for this element");
            return false;
        }
        return true;
    }

    private static Map<Service, Map<Capability, String>> setCapabilities() {
        Map<Service, Map<Capability, String>> capabilities = new HashMap<Service, Map<Capability, String>>();

        // L2 Support
        capabilities.put(Service.Connectivity, null);

        return capabilities;
    }

    @Override
    public List<Class<?>> getCommands() {
        List<Class<?>> cmdList = new ArrayList<Class<?>>();
        cmdList.add(AddBrocadeVcsDeviceCmd.class);
        cmdList.add(DeleteBrocadeVcsDeviceCmd.class);
        cmdList.add(ListBrocadeVcsDeviceNetworksCmd.class);
        cmdList.add(ListBrocadeVcsDevicesCmd.class);
        return cmdList;
    }

    @Override
    @DB
    public BrocadeVcsDeviceVO addBrocadeVcsDevice(AddBrocadeVcsDeviceCmd cmd) {
        ServerResource resource = new BrocadeVcsResource();
        final String deviceName = Network.Provider.BrocadeVcs.getName();
        NetworkDevice networkDevice = NetworkDevice.getNetworkDevice(deviceName);
        if (networkDevice == null) {
            throw new CloudRuntimeException("No network device found for " + deviceName);
        }
        final Long physicalNetworkId = cmd.getPhysicalNetworkId();
        PhysicalNetworkVO physicalNetwork = _physicalNetworkDao.findById(physicalNetworkId);
        if (physicalNetwork == null) {
            throw new InvalidParameterValueException("Could not find phyical network with ID: " + physicalNetworkId);
        }
        long zoneId = physicalNetwork.getDataCenterId();

        final PhysicalNetworkServiceProviderVO ntwkSvcProvider = _physicalNetworkServiceProviderDao.findByServiceProvider(physicalNetwork.getId(),
                networkDevice.getNetworkServiceProvder());
        if (ntwkSvcProvider == null) {
            throw new CloudRuntimeException("Network Service Provider: " + networkDevice.getNetworkServiceProvder() + " is not enabled in the physical network: "
                    + physicalNetworkId + "to add this device");
        } else if (ntwkSvcProvider.getState() == PhysicalNetworkServiceProvider.State.Shutdown) {
            throw new CloudRuntimeException("Network Service Provider: " + ntwkSvcProvider.getProviderName() + " is in shutdown state in the physical network: "
                    + physicalNetworkId + "to add this device");
        }

        Map<String, String> params = new HashMap<String, String>();
        params.put("guid", UUID.randomUUID().toString());
        params.put("zoneId", String.valueOf(physicalNetwork.getDataCenterId()));
        params.put("physicalNetworkId", String.valueOf(physicalNetwork.getId()));
        params.put("name", "Brocade VCS - " + cmd.getHost());
        params.put("ip", cmd.getHost());
        params.put("adminuser", cmd.getUsername());
        params.put("adminpass", cmd.getPassword());

        Map<String, Object> hostdetails = new HashMap<String, Object>();
        hostdetails.putAll(params);

        try {
            resource.configure(cmd.getHost(), hostdetails);

            final Host host = _resourceMgr.addHost(zoneId, resource, Host.Type.L2Networking, params);
            if (host != null) {
                return Transaction.execute(new TransactionCallback<BrocadeVcsDeviceVO>() {
                    @Override
                    public BrocadeVcsDeviceVO doInTransaction(TransactionStatus status) {
                        BrocadeVcsDeviceVO brocadeVcsDevice = new BrocadeVcsDeviceVO(host.getId(), physicalNetworkId, ntwkSvcProvider.getProviderName(), deviceName);
                        _brocadeVcsDao.persist(brocadeVcsDevice);

                        DetailVO detail = new DetailVO(host.getId(), "brocadevcsdeviceid", String.valueOf(brocadeVcsDevice.getId()));
                        _hostDetailsDao.persist(detail);

                        return brocadeVcsDevice;
                    }
                });
            } else {
                throw new CloudRuntimeException("Failed to add Brocade VCS Switch due to internal error.");
            }
        } catch (ConfigurationException e) {
            throw new CloudRuntimeException(e.getMessage());
        }

    }

    @Override
    public BrocadeVcsDeviceResponse createBrocadeVcsDeviceResponse(BrocadeVcsDeviceVO brocadeVcsDeviceVO) {
        HostVO brocadeVcsHost = _hostDao.findById(brocadeVcsDeviceVO.getHostId());
        _hostDao.loadDetails(brocadeVcsHost);

        BrocadeVcsDeviceResponse response = new BrocadeVcsDeviceResponse();
        response.setDeviceName(brocadeVcsDeviceVO.getDeviceName());
        PhysicalNetwork pnw = ApiDBUtils.findPhysicalNetworkById(brocadeVcsDeviceVO.getPhysicalNetworkId());
        if (pnw != null) {
            response.setPhysicalNetworkId(pnw.getUuid());
        }
        response.setId(brocadeVcsDeviceVO.getUuid());
        response.setProviderName(brocadeVcsDeviceVO.getProviderName());
        response.setHostName(brocadeVcsHost.getDetail("ip"));
        response.setObjectName("brocadevcsdevice");
        return response;
    }

    @Override
    public boolean deleteBrocadeVcsDevice(DeleteBrocadeVcsDeviceCmd cmd) {
        Long brocadeDeviceId = cmd.getBrocadeVcsDeviceId();
        BrocadeVcsDeviceVO brocadeVcsDevice = _brocadeVcsDao.findById(brocadeDeviceId);
        if (brocadeVcsDevice == null) {
            throw new InvalidParameterValueException("Could not find a brocade vcs switch with id " + brocadeVcsDevice);
        }

        // Find the physical network we work for
        Long physicalNetworkId = brocadeVcsDevice.getPhysicalNetworkId();
        PhysicalNetworkVO physicalNetwork = _physicalNetworkDao.findById(physicalNetworkId);
        if (physicalNetwork != null) {
            // Lets see if there are networks that use us
            // Find the brocade networks on this physical network
            List<NetworkVO> networkList = _networkDao.listByPhysicalNetwork(physicalNetworkId);
            if (networkList != null) {
                // Networks with broadcast type vcs are ours
                for (NetworkVO network : networkList) {
                    if (network.getBroadcastDomainType() == Networks.BroadcastDomainType.Vcs) {
                        if ((network.getState() != Network.State.Shutdown) && (network.getState() != Network.State.Destroy)) {
                            throw new CloudRuntimeException("This Brocade VCS Switch can not be deleted as there are one or more logical networks provisioned by cloudstack.");
                        }
                    }
                }
            }
        }

        HostVO brocadeHost = _hostDao.findById(brocadeVcsDevice.getHostId());
        Long hostId = brocadeHost.getId();

        brocadeHost.setResourceState(ResourceState.Maintenance);
        _hostDao.update(hostId, brocadeHost);
        _resourceMgr.deleteHost(hostId, false, false);

        _brocadeVcsDao.remove(brocadeDeviceId);
        return true;
    }

    @Override
    public List<BrocadeVcsDeviceVO> listBrocadeVcsDevices(ListBrocadeVcsDevicesCmd cmd) {
        Long physicalNetworkId = cmd.getPhysicalNetworkId();
        Long brocadeVcsDeviceId = cmd.getBrocadeVcsDeviceId();
        List<BrocadeVcsDeviceVO> responseList = new ArrayList<BrocadeVcsDeviceVO>();

        if (physicalNetworkId == null && brocadeVcsDeviceId == null) {
            throw new InvalidParameterValueException("Either physical network Id or brocade vcs switch Id must be specified");
        }

        if (brocadeVcsDeviceId != null) {
            BrocadeVcsDeviceVO brocadeVcsDevice = _brocadeVcsDao.findById(brocadeVcsDeviceId);
            if (brocadeVcsDevice == null) {
                throw new InvalidParameterValueException("Could not find Brocade VCS Switch with id: " + brocadeVcsDeviceId);
            }
            responseList.add(brocadeVcsDevice);
        } else {
            PhysicalNetworkVO physicalNetwork = _physicalNetworkDao.findById(physicalNetworkId);
            if (physicalNetwork == null) {
                throw new InvalidParameterValueException("Could not find a physical network with id: " + physicalNetworkId);
            }
            responseList = _brocadeVcsDao.listByPhysicalNetwork(physicalNetworkId);
        }

        return responseList;
    }

    @Override
    public List<? extends Network> listBrocadeVcsDeviceNetworks(ListBrocadeVcsDeviceNetworksCmd cmd) {
        Long brocadeDeviceId = cmd.getBrocadeVcsDeviceId();
        BrocadeVcsDeviceVO brocadeVcsDevice = _brocadeVcsDao.findById(brocadeDeviceId);
        if (brocadeVcsDevice == null) {
            throw new InvalidParameterValueException("Could not find a Brocade VCS Switch with id " + brocadeDeviceId);
        }

        // Find the physical network we work for
        Long physicalNetworkId = brocadeVcsDevice.getPhysicalNetworkId();
        PhysicalNetworkVO physicalNetwork = _physicalNetworkDao.findById(physicalNetworkId);
        if (physicalNetwork == null) {
            // No such physical network, so no provisioned networks
            return Collections.emptyList();
        }

        // Find the brocade networks on this physical network
        List<NetworkVO> networkList = _networkDao.listByPhysicalNetwork(physicalNetworkId);
        if (networkList == null) {
            return Collections.emptyList();
        }

        // Networks with broadcast type vcs are ours
        List<NetworkVO> responseList = new ArrayList<NetworkVO>();
        for (NetworkVO network : networkList) {
            if (network.getBroadcastDomainType() == Networks.BroadcastDomainType.Vcs) {
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
        if (!(startup[0] instanceof StartupBrocadeVcsCommand)) {
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

}
