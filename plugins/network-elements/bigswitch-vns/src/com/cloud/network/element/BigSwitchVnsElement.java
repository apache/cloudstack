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

import javax.ejb.Local;
import javax.inject.Inject;
import javax.naming.ConfigurationException;

import org.apache.log4j.Logger;
import org.springframework.stereotype.Component;
import org.apache.cloudstack.network.ExternalNetworkDeviceManager.NetworkDevice;

import com.cloud.agent.AgentManager;
import com.cloud.agent.api.CreateVnsPortAnswer;
import com.cloud.agent.api.CreateVnsPortCommand;
import com.cloud.agent.api.DeleteVnsPortAnswer;
import com.cloud.agent.api.DeleteVnsPortCommand;
import com.cloud.agent.api.StartupBigSwitchVnsCommand;
import com.cloud.agent.api.StartupCommand;
import com.cloud.api.ApiDBUtils;
import com.cloud.api.commands.AddBigSwitchVnsDeviceCmd;
import com.cloud.api.commands.DeleteBigSwitchVnsDeviceCmd;
import com.cloud.api.commands.ListBigSwitchVnsDevicesCmd;
import com.cloud.api.commands.VnsConstants;
import com.cloud.api.response.BigSwitchVnsDeviceResponse;
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
import com.cloud.network.BigSwitchVnsDeviceVO;
import com.cloud.network.Network;
import com.cloud.network.Network.Capability;
import com.cloud.network.Network.Provider;
import com.cloud.network.Network.Service;
import com.cloud.network.NetworkModel;
import com.cloud.network.Networks;
import com.cloud.network.Networks.BroadcastDomainType;
import com.cloud.network.PhysicalNetwork;
import com.cloud.network.PhysicalNetworkServiceProvider;
import com.cloud.network.dao.BigSwitchVnsDao;
import com.cloud.network.dao.NetworkDao;
import com.cloud.network.dao.NetworkServiceMapDao;
import com.cloud.network.dao.NetworkVO;
import com.cloud.network.dao.PhysicalNetworkDao;
import com.cloud.network.dao.PhysicalNetworkServiceProviderDao;
import com.cloud.network.dao.PhysicalNetworkServiceProviderVO;
import com.cloud.network.dao.PhysicalNetworkVO;
import com.cloud.network.resource.BigSwitchVnsResource;
import com.cloud.offering.NetworkOffering;
import com.cloud.resource.ResourceManager;
import com.cloud.resource.ResourceStateAdapter;
import com.cloud.resource.ServerResource;
import com.cloud.resource.UnableDeleteHostException;
import com.cloud.utils.component.AdapterBase;
import com.cloud.utils.db.DB;
import com.cloud.utils.db.Transaction;
import com.cloud.utils.db.TransactionCallback;
import com.cloud.utils.db.TransactionCallbackNoReturn;
import com.cloud.utils.db.TransactionStatus;
import com.cloud.utils.exception.CloudRuntimeException;
import com.cloud.vm.NicProfile;
import com.cloud.vm.ReservationContext;
import com.cloud.vm.VirtualMachineProfile;

@Component
@Local(value = {NetworkElement.class, ConnectivityProvider.class})
public class BigSwitchVnsElement extends AdapterBase implements
        BigSwitchVnsElementService, ConnectivityProvider, ResourceStateAdapter {
    private static final Logger s_logger = Logger.getLogger(BigSwitchVnsElement.class);

    private static final Map<Service, Map<Capability, String>> capabilities = setCapabilities();

    @Inject
    ResourceManager _resourceMgr;
    @Inject
    PhysicalNetworkDao _physicalNetworkDao;
    @Inject
    PhysicalNetworkServiceProviderDao _physicalNetworkServiceProviderDao;
    @Inject
    BigSwitchVnsDao _bigswitchVnsDao;
    @Inject
    HostDetailsDao _hostDetailsDao;
    @Inject
    HostDao _hostDao;
    @Inject
    AgentManager _agentMgr;
    @Inject
    NetworkDao _networkDao;
    @Inject
    NetworkModel _networkModel;
    @Inject
    ConfigurationManager _configMgr;
    @Inject
    NetworkServiceMapDao _ntwkSrvcDao;
    @Inject
    VlanDao _vlanDao;

    @Override
    public Map<Service, Map<Capability, String>> getCapabilities() {
        return capabilities;
    }

    @Override
    public Provider getProvider() {
        return VnsConstants.BigSwitchVns;
    }

    private boolean canHandle(Network network, Service service) {
        s_logger.debug("Checking if BigSwitchVnsElement can handle service "
                + service.getName() + " on network " + network.getDisplayText());
        if (network.getBroadcastDomainType() != BroadcastDomainType.Lswitch) {
            return false;
        }

        if (!_networkModel.isProviderForNetwork(getProvider(),
                network.getId())) {
            s_logger.debug("BigSwitchVnsElement is not a provider for network "
                    + network.getDisplayText());
            return false;
        }

        if (!_ntwkSrvcDao.canProviderSupportServiceInNetwork(network.getId(),
                service, VnsConstants.BigSwitchVns)) {
            s_logger.debug("BigSwitchVnsElement can't provide the "
                    + service.getName() + " service on network "
                    + network.getDisplayText());
            return false;
        }

        return true;
    }

    @Override
    public boolean configure(String name, Map<String, Object> params)
            throws ConfigurationException {
        super.configure(name, params);
        _resourceMgr.registerResourceStateAdapter(this.getClass()
                .getSimpleName(), this);
        return true;
    }

    @Override
    public boolean implement(Network network, NetworkOffering offering,
            DeployDestination dest, ReservationContext context)
            throws ConcurrentOperationException, ResourceUnavailableException,
            InsufficientCapacityException {
        s_logger.debug("entering BigSwitchVnsElement implement function for network "
                + network.getDisplayText()
                + " (state "
                + network.getState()
                + ")");

        return true;
    }

    @Override
    public boolean prepare(Network network, NicProfile nic,
            VirtualMachineProfile vm,
            DeployDestination dest, ReservationContext context)
            throws ConcurrentOperationException, ResourceUnavailableException,
            InsufficientCapacityException {

        if (!canHandle(network, Service.Connectivity)) {
            return false;
        }

        if (network.getBroadcastUri() == null) {
            s_logger.error("Nic has no broadcast Uri with the LSwitch Uuid");
            return false;
        }

        String mac = nic.getMacAddress();
        String tenantId = context.getDomain().getName();

        List<BigSwitchVnsDeviceVO> devices = _bigswitchVnsDao
                .listByPhysicalNetwork(network.getPhysicalNetworkId());
        if (devices.isEmpty()) {
            s_logger.error("No BigSwitch Controller on physical network "
                    + network.getPhysicalNetworkId());
            return false;
        }
        BigSwitchVnsDeviceVO bigswitchVnsDevice = devices.get(0);
        HostVO bigswitchVnsHost = _hostDao.findById(bigswitchVnsDevice.getHostId());

        CreateVnsPortCommand cmd = new CreateVnsPortCommand(
                BroadcastDomainType.getValue(network.getBroadcastUri()),
                vm.getUuid(),
                tenantId,
                nic.getName(),
                mac);
        CreateVnsPortAnswer answer = (CreateVnsPortAnswer)_agentMgr
                .easySend(bigswitchVnsHost.getId(), cmd);

        if (answer == null || !answer.getResult()) {
            s_logger.error("CreatePortCommand failed");
            return false;
        }

        return true;
    }

    @Override
    public boolean release(Network network, NicProfile nic,
            VirtualMachineProfile vm,
            ReservationContext context) throws ConcurrentOperationException,
            ResourceUnavailableException {

        if (!canHandle(network, Service.Connectivity)) {
            return false;
        }

        if (network.getBroadcastUri() == null) {
            s_logger.error("Nic has no broadcast Uri with the LSwitch Uuid");
            return false;
        }

        String tenantId = context.getDomain().getName();

        List<BigSwitchVnsDeviceVO> devices = _bigswitchVnsDao
                .listByPhysicalNetwork(network.getPhysicalNetworkId());
        if (devices.isEmpty()) {
            s_logger.error("No BigSwitch Controller on physical network "
                    + network.getPhysicalNetworkId());
            return false;
        }
        BigSwitchVnsDeviceVO bigswitchVnsDevice = devices.get(0);
        HostVO bigswitchVnsHost = _hostDao.findById(bigswitchVnsDevice.getHostId());

        DeleteVnsPortCommand cmd = new DeleteVnsPortCommand(
                BroadcastDomainType.getValue(network.getBroadcastUri()),
                vm.getUuid(),
                tenantId);
        DeleteVnsPortAnswer answer = (DeleteVnsPortAnswer)_agentMgr
                .easySend(bigswitchVnsHost.getId(), cmd);

        if (answer == null || !answer.getResult()) {
            s_logger.error("DeletePortCommand failed");
            return false;
        }

        return true;
    }

    @Override
    public boolean shutdown(Network network, ReservationContext context,
            boolean cleanup) throws ConcurrentOperationException,
            ResourceUnavailableException {
        if (!canHandle(network, Service.Connectivity)) {
            return false;
        }

        return true;
    }

    @Override
    public boolean destroy(Network network, ReservationContext context)
            throws ConcurrentOperationException, ResourceUnavailableException {
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
    public boolean shutdownProviderInstances(
            PhysicalNetworkServiceProvider provider, ReservationContext context)
            throws ConcurrentOperationException, ResourceUnavailableException {
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
            s_logger.warn("Unable to provide services without Connectivity service enabled for this element");
            return false;
        }
        return true;
    }

    private static Map<Service, Map<Capability, String>> setCapabilities() {
        Map<Service, Map<Capability, String>> capabilities = new HashMap<Service, Map<Capability, String>>();

        // L2 Support : SDN provisioning
        capabilities.put(Service.Connectivity, null);

        return capabilities;
    }

    @Override
    @DB
    public BigSwitchVnsDeviceVO addBigSwitchVnsDevice(AddBigSwitchVnsDeviceCmd cmd) {
        ServerResource resource = new BigSwitchVnsResource();
        final String deviceName = VnsConstants.BigSwitchVns.getName();
        NetworkDevice networkDevice = NetworkDevice
                .getNetworkDevice(deviceName);
        final Long physicalNetworkId = cmd.getPhysicalNetworkId();

        PhysicalNetworkVO physicalNetwork = _physicalNetworkDao
                .findById(physicalNetworkId);
        if (physicalNetwork == null) {
            throw new InvalidParameterValueException(
                    "Could not find phyical network with ID: "
                            + physicalNetworkId);
        }
        long zoneId = physicalNetwork.getDataCenterId();

        final PhysicalNetworkServiceProviderVO ntwkSvcProvider = _physicalNetworkServiceProviderDao
                .findByServiceProvider(physicalNetwork.getId(),
                        networkDevice.getNetworkServiceProvder());
        if (ntwkSvcProvider == null) {
            throw new CloudRuntimeException("Network Service Provider: "
                    + networkDevice.getNetworkServiceProvder()
                    + " is not enabled in the physical network: "
                    + physicalNetworkId + "to add this device");
        } else if (ntwkSvcProvider.getState() == PhysicalNetworkServiceProvider.State.Shutdown) {
            throw new CloudRuntimeException("Network Service Provider: "
                    + ntwkSvcProvider.getProviderName()
                    + " is in shutdown state in the physical network: "
                    + physicalNetworkId + "to add this device");
        }

        if (_bigswitchVnsDao.listByPhysicalNetwork(physicalNetworkId).size() != 0) {
            throw new CloudRuntimeException(
                    "A BigSwitch controller device is already configured on this physical network");
        }

        Map<String, String> params = new HashMap<String, String>();
        params.put("guid", UUID.randomUUID().toString());
        params.put("zoneId", String.valueOf(physicalNetwork.getDataCenterId()));
        params.put("physicalNetworkId", String.valueOf(physicalNetwork.getId()));
        params.put("name", "BigSwitch Controller - " + cmd.getHost());
        params.put("ip", cmd.getHost());
        // FIXME What to do with multiple isolation types
        params.put("transportzoneisotype",
                physicalNetwork.getIsolationMethods().get(0).toLowerCase());

        Map<String, Object> hostdetails = new HashMap<String, Object>();
        hostdetails.putAll(params);

        try {
            resource.configure(cmd.getHost(), hostdetails);

            final Host host = _resourceMgr.addHost(zoneId, resource,
                    Host.Type.L2Networking, params);
            if (host != null) {
                return Transaction.execute(new TransactionCallback<BigSwitchVnsDeviceVO>() {
                    @Override
                    public BigSwitchVnsDeviceVO doInTransaction(TransactionStatus status) {
                        BigSwitchVnsDeviceVO bigswitchVnsDevice = new BigSwitchVnsDeviceVO(host.getId(),
                                physicalNetworkId, ntwkSvcProvider.getProviderName(),
                                deviceName);
                        _bigswitchVnsDao.persist(bigswitchVnsDevice);
        
                        DetailVO detail = new DetailVO(host.getId(),
                                "bigswitchvnsdeviceid",
                                String.valueOf(bigswitchVnsDevice.getId()));
                        _hostDetailsDao.persist(detail);
                        
                        return bigswitchVnsDevice;
                    }
                });
            } else {
                throw new CloudRuntimeException(
                        "Failed to add BigSwitch Vns Device due to internal error.");
            }
        } catch (ConfigurationException e) {
            throw new CloudRuntimeException(e.getMessage());
        }
    }

    @Override
    public BigSwitchVnsDeviceResponse createBigSwitchVnsDeviceResponse(
            BigSwitchVnsDeviceVO bigswitchVnsDeviceVO) {
        HostVO bigswitchVnsHost = _hostDao.findById(bigswitchVnsDeviceVO.getHostId());
        _hostDao.loadDetails(bigswitchVnsHost);

        BigSwitchVnsDeviceResponse response = new BigSwitchVnsDeviceResponse();
        response.setDeviceName(bigswitchVnsDeviceVO.getDeviceName());
        PhysicalNetwork pnw = ApiDBUtils.findPhysicalNetworkById(bigswitchVnsDeviceVO.getPhysicalNetworkId());
        if (pnw != null) {
            response.setPhysicalNetworkId(pnw.getUuid());
        }
        response.setId(bigswitchVnsDeviceVO.getUuid());
        response.setProviderName(bigswitchVnsDeviceVO.getProviderName());
        response.setHostName(bigswitchVnsHost.getDetail("ip"));
        response.setObjectName("bigswitchvnsdevice");
        return response;
    }

    @Override
    public boolean deleteBigSwitchVnsDevice(DeleteBigSwitchVnsDeviceCmd cmd) {
        Long bigswitchVnsDeviceId = cmd.getBigSwitchVnsDeviceId();
        BigSwitchVnsDeviceVO bigswitchVnsDevice = _bigswitchVnsDao
                .findById(bigswitchVnsDeviceId);
        if (bigswitchVnsDevice == null) {
            throw new InvalidParameterValueException(
                    "Could not find a BigSwitch Controller with id " + bigswitchVnsDevice);
        }

        // Find the physical network we work for
        Long physicalNetworkId = bigswitchVnsDevice.getPhysicalNetworkId();
        PhysicalNetworkVO physicalNetwork = _physicalNetworkDao
                .findById(physicalNetworkId);
        if (physicalNetwork != null) {
            List<NetworkVO> networkList = _networkDao
                    .listByPhysicalNetwork(physicalNetworkId);

            // Networks with broadcast type lswitch are ours
            for (NetworkVO network : networkList) {
                if (network.getBroadcastDomainType() == Networks.BroadcastDomainType.Lswitch) {
                    if ((network.getState() != Network.State.Shutdown)
                            && (network.getState() != Network.State.Destroy)) {
                        throw new CloudRuntimeException(
                                "This BigSwitch Controller device can not be deleted as there are one or more " +
                                        "logical networks provisioned by cloudstack.");
                    }
                }
            }
        }

        HostVO bigswitchHost = _hostDao.findById(bigswitchVnsDevice.getHostId());
        Long hostId = bigswitchHost.getId();

        //bigswitchHost.setResourceState(ResourceState.Maintenance);
        //_hostDao.update(hostId, bigswitchHost);
        _hostDao.remove(hostId);
        _resourceMgr.deleteHost(hostId, false, false);

        _bigswitchVnsDao.remove(bigswitchVnsDeviceId);
        return true;
    }

    @Override
    public List<BigSwitchVnsDeviceVO> listBigSwitchVnsDevices(
            ListBigSwitchVnsDevicesCmd cmd) {
        Long physicalNetworkId = cmd.getPhysicalNetworkId();
        Long bigswitchVnsDeviceId = cmd.getBigSwitchVnsDeviceId();
        List<BigSwitchVnsDeviceVO> responseList = new ArrayList<BigSwitchVnsDeviceVO>();

        if (physicalNetworkId == null && bigswitchVnsDeviceId == null) {
            throw new InvalidParameterValueException(
                    "Either physical network Id or bigswitch device Id must be specified");
        }

        if (bigswitchVnsDeviceId != null) {
            BigSwitchVnsDeviceVO bigswitchVnsDevice = _bigswitchVnsDao
                    .findById(bigswitchVnsDeviceId);
            if (bigswitchVnsDevice == null) {
                throw new InvalidParameterValueException(
                        "Could not find BigSwitch controller with id: "
                                + bigswitchVnsDevice);
            }
            responseList.add(bigswitchVnsDevice);
        } else {
            PhysicalNetworkVO physicalNetwork = _physicalNetworkDao
                    .findById(physicalNetworkId);
            if (physicalNetwork == null) {
                throw new InvalidParameterValueException(
                        "Could not find a physical network with id: "
                                + physicalNetworkId);
            }
            responseList = _bigswitchVnsDao
                    .listByPhysicalNetwork(physicalNetworkId);
        }

        return responseList;
    }

    @Override
    public HostVO createHostVOForConnectedAgent(HostVO host,
            StartupCommand[] cmd) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public HostVO createHostVOForDirectConnectAgent(HostVO host,
            StartupCommand[] startup, ServerResource resource,
            Map<String, String> details, List<String> hostTags) {
        if (!(startup[0] instanceof StartupBigSwitchVnsCommand)) {
            return null;
        }
        host.setType(Host.Type.L2Networking);
        return host;
    }

    @Override
    public DeleteHostAnswer deleteHost(HostVO host, boolean isForced,
            boolean isForceDeleteStorage) throws UnableDeleteHostException {
        if (!(host.getType() == Host.Type.L2Networking)) {
            return null;
        }
        return new DeleteHostAnswer(true);
    }

    @Override
    public List<Class<?>> getCommands() {
        List<Class<?>> cmdList = new ArrayList<Class<?>>();
        cmdList.add(AddBigSwitchVnsDeviceCmd.class);
        cmdList.add(DeleteBigSwitchVnsDeviceCmd.class);
        cmdList.add(ListBigSwitchVnsDevicesCmd.class);
        return cmdList;
    }

}
