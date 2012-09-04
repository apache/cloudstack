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
/** Licensed to the Apache Software Foundation (ASF) under one
 *  or more contributor license agreements.  See the NOTICE file
 *  distributed with this work for additional information
 *  regarding copyright ownership.  The ASF licenses this file
 *  to you under the Apache License, Version 2.0 (the
 *  "License"); you may not use this file except in compliance
 *  with the License.  You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing,
 *  software distributed under the License is distributed on an
 *  "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 *  KIND, either express or implied.  See the License for the
 *  specific language governing permissions and limitations
 *  under the License.
 */
package com.cloud.network.element;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import javax.ejb.Local;
import javax.naming.ConfigurationException;

import org.apache.log4j.Logger;

import com.cloud.agent.AgentManager;
import com.cloud.agent.api.CreateLogicalSwitchPortAnswer;
import com.cloud.agent.api.CreateLogicalSwitchPortCommand;
import com.cloud.agent.api.DeleteLogicalSwitchPortAnswer;
import com.cloud.agent.api.DeleteLogicalSwitchPortCommand;
import com.cloud.agent.api.FindLogicalSwitchPortAnswer;
import com.cloud.agent.api.FindLogicalSwitchPortCommand;
import com.cloud.agent.api.StartupCommand;
import com.cloud.agent.api.StartupNiciraNvpCommand;
import com.cloud.agent.api.UpdateLogicalSwitchPortAnswer;
import com.cloud.agent.api.UpdateLogicalSwitchPortCommand;
import com.cloud.api.commands.AddNiciraNvpDeviceCmd;
import com.cloud.api.commands.DeleteNiciraNvpDeviceCmd;
import com.cloud.api.commands.ListNiciraNvpDeviceNetworksCmd;
import com.cloud.api.commands.ListNiciraNvpDevicesCmd;
import com.cloud.api.response.NiciraNvpDeviceResponse;
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
import com.cloud.network.Network;
import com.cloud.network.ExternalNetworkDeviceManager.NetworkDevice;
import com.cloud.network.Network.Capability;
import com.cloud.network.Network.Provider;
import com.cloud.network.Network.Service;
import com.cloud.network.NetworkVO;
import com.cloud.network.Networks;
import com.cloud.network.Networks.BroadcastDomainType;
import com.cloud.network.NiciraNvpDeviceVO;
import com.cloud.network.NiciraNvpNicMappingVO;
import com.cloud.network.PhysicalNetworkServiceProvider;
import com.cloud.network.PhysicalNetworkVO;
import com.cloud.network.dao.NetworkDao;
import com.cloud.network.dao.NiciraNvpDao;
import com.cloud.network.dao.NiciraNvpNicMappingDao;
import com.cloud.network.dao.PhysicalNetworkDao;
import com.cloud.network.dao.PhysicalNetworkServiceProviderDao;
import com.cloud.network.dao.PhysicalNetworkServiceProviderVO;
import com.cloud.network.guru.NiciraNvpGuestNetworkGuru;
import com.cloud.network.resource.NiciraNvpResource;
import com.cloud.offering.NetworkOffering;
import com.cloud.resource.ResourceManager;
import com.cloud.resource.ResourceState;
import com.cloud.resource.ResourceStateAdapter;
import com.cloud.resource.ServerResource;
import com.cloud.resource.UnableDeleteHostException;
import com.cloud.utils.component.AdapterBase;
import com.cloud.utils.component.Inject;
import com.cloud.utils.db.DB;
import com.cloud.utils.db.Transaction;
import com.cloud.utils.exception.CloudRuntimeException;
import com.cloud.vm.NicProfile;
import com.cloud.vm.NicVO;
import com.cloud.vm.ReservationContext;
import com.cloud.vm.VirtualMachine;
import com.cloud.vm.VirtualMachineProfile;
import com.cloud.vm.dao.NicDao;

@Local(value = NetworkElement.class)
public class NiciraNvpElement extends AdapterBase implements ConnectivityProvider, NiciraNvpElementService, ResourceStateAdapter {
    private static final Logger s_logger = Logger.getLogger(NiciraNvpElement.class);
    
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
    NiciraNvpDao _niciraNvpDao;
    @Inject 
    HostDetailsDao _hostDetailsDao;
    @Inject
    HostDao _hostDao;  
    @Inject
    AgentManager _agentMgr;
    @Inject
    NiciraNvpNicMappingDao _niciraNvpNicMappingDao;
    @Inject
    NetworkDao _networkDao;
    
    @Override
    public Map<Service, Map<Capability, String>> getCapabilities() {
        return capabilities;
    }

    @Override
    public Provider getProvider() {
        return Provider.NiciraNvp;
    }
    
    private boolean canHandle(Network network) {
        if (network.getBroadcastDomainType() != BroadcastDomainType.Lswitch) {
            return false;
        }
        
        return true;        
    }
    
    @Override
    public boolean configure(String name, Map<String, Object> params)
            throws ConfigurationException {
        super.configure(name, params);
        _resourceMgr.registerResourceStateAdapter(this.getClass().getSimpleName(), this);
        return true;
    }

    @Override
    public boolean implement(Network network, NetworkOffering offering,
            DeployDestination dest, ReservationContext context)
            throws ConcurrentOperationException, ResourceUnavailableException,
            InsufficientCapacityException {
        
        if (!canHandle(network)) {
            return false;
        }
        
        return true;
    }

    @Override
    public boolean prepare(Network network, NicProfile nic,
            VirtualMachineProfile<? extends VirtualMachine> vm,
            DeployDestination dest, ReservationContext context)
            throws ConcurrentOperationException, ResourceUnavailableException,
            InsufficientCapacityException {
        
        if (!canHandle(network)) {
            return false;
        }

        if (network.getBroadcastUri() == null) {
            s_logger.error("Nic has no broadcast Uri with the LSwitch Uuid");
            return false;
        }

        NicVO nicVO = _nicDao.findById(nic.getId());

        List<NiciraNvpDeviceVO> devices = _niciraNvpDao.listByPhysicalNetwork(network.getPhysicalNetworkId());
        if (devices.isEmpty()) {
            s_logger.error("No NiciraNvp Controller on physical network " + network.getPhysicalNetworkId());
            return false;
        }
        NiciraNvpDeviceVO niciraNvpDevice = devices.get(0);
        HostVO niciraNvpHost = _hostDao.findById(niciraNvpDevice.getHostId());

        NiciraNvpNicMappingVO existingNicMap = _niciraNvpNicMappingDao.findByNicUuid(nicVO.getUuid());
        if (existingNicMap != null) {
            FindLogicalSwitchPortCommand findCmd = new FindLogicalSwitchPortCommand(existingNicMap.getLogicalSwitchUuid(), 
            		existingNicMap.getLogicalSwitchPortUuid());
            FindLogicalSwitchPortAnswer answer = (FindLogicalSwitchPortAnswer) _agentMgr.easySend(niciraNvpHost.getId(), findCmd);
            
            if (answer.getResult()) {
	            s_logger.warn("Existing Logical Switchport found for nic " + nic.getName() + " with uuid " + existingNicMap.getLogicalSwitchPortUuid());
	            UpdateLogicalSwitchPortCommand cmd = new UpdateLogicalSwitchPortCommand(existingNicMap.getLogicalSwitchPortUuid(), 
	            		network.getBroadcastUri().getSchemeSpecificPart(), nicVO.getUuid(), 
	                    context.getDomain().getName() + "-" + context.getAccount().getAccountName(), nic.getName());
	            _agentMgr.easySend(niciraNvpHost.getId(), cmd);
	            return true;
            }
            else {
	            s_logger.error("Stale entry found for nic " + nic.getName() + " with logical switchport uuid " + existingNicMap.getLogicalSwitchPortUuid());
	            _niciraNvpNicMappingDao.remove(existingNicMap.getId());
            }
        }
        
        CreateLogicalSwitchPortCommand cmd = new CreateLogicalSwitchPortCommand(network.getBroadcastUri().getSchemeSpecificPart(), nicVO.getUuid(), 
                context.getDomain().getName() + "-" + context.getAccount().getAccountName(), nic.getName());
        CreateLogicalSwitchPortAnswer answer = (CreateLogicalSwitchPortAnswer) _agentMgr.easySend(niciraNvpHost.getId(), cmd);
        
        if (answer == null || !answer.getResult()) {
            s_logger.error ("CreateLogicalSwitchPortCommand failed");
            return false;
        }
        
        NiciraNvpNicMappingVO nicMap = new NiciraNvpNicMappingVO(network.getBroadcastUri().getSchemeSpecificPart(), answer.getLogicalSwitchPortUuid(), nicVO.getUuid());
        _niciraNvpNicMappingDao.persist(nicMap);

        return true;
    }

    @Override
    public boolean release(Network network, NicProfile nic,
            VirtualMachineProfile<? extends VirtualMachine> vm,
            ReservationContext context) throws ConcurrentOperationException,
            ResourceUnavailableException {

        if (!canHandle(network)) {
            return false;
        }

        if (network.getBroadcastUri() == null) {
            s_logger.error("Nic has no broadcast Uri with the LSwitch Uuid");
            return false;
        }
        
        NicVO nicVO = _nicDao.findById(nic.getId());

        List<NiciraNvpDeviceVO> devices = _niciraNvpDao.listByPhysicalNetwork(network.getPhysicalNetworkId());
        if (devices.isEmpty()) {
            s_logger.error("No NiciraNvp Controller on physical network " + network.getPhysicalNetworkId());
            return false;
        }
        NiciraNvpDeviceVO niciraNvpDevice = devices.get(0);
        HostVO niciraNvpHost = _hostDao.findById(niciraNvpDevice.getHostId());
        
        NiciraNvpNicMappingVO nicMap = _niciraNvpNicMappingDao.findByNicUuid(nicVO.getUuid());
        if (nicMap == null) {
            s_logger.error("No mapping for nic " + nic.getName());
            return false;
        }
                
        DeleteLogicalSwitchPortCommand cmd = new DeleteLogicalSwitchPortCommand(nicMap.getLogicalSwitchUuid(), nicMap.getLogicalSwitchPortUuid());
        DeleteLogicalSwitchPortAnswer answer = (DeleteLogicalSwitchPortAnswer) _agentMgr.easySend(niciraNvpHost.getId(), cmd);
        
        if (answer == null || !answer.getResult()) {
            s_logger.error ("DeleteLogicalSwitchPortCommand failed");
            return false;
        }
        
        _niciraNvpNicMappingDao.remove(nicMap.getId());
        
        return true;
    }

    @Override
    public boolean shutdown(Network network, ReservationContext context,
            boolean cleanup) throws ConcurrentOperationException,
            ResourceUnavailableException {
        if (!canHandle(network)) {
            return false;
        }

        return true;
    }

    @Override
    public boolean destroy(Network network)
            throws ConcurrentOperationException, ResourceUnavailableException {
        if (!canHandle(network)) {
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
        return false;
    }

    @Override
    public boolean verifyServicesCombination(Set<Service> services) {
        return true;
    }

    private static Map<Service, Map<Capability, String>> setCapabilities() {
        Map<Service, Map<Capability, String>> capabilities = new HashMap<Service, Map<Capability, String>>();

        capabilities.put(Service.Connectivity, null);
        return capabilities;
    }

    @Override
    public String getPropertiesFile() {
        return "nicira-nvp_commands.properties";
    }

    @Override
    @DB
    public NiciraNvpDeviceVO addNiciraNvpDevice(AddNiciraNvpDeviceCmd cmd) {
        ServerResource resource = new NiciraNvpResource();
        String deviceName = Network.Provider.NiciraNvp.getName();
        NetworkDevice networkDevice = NetworkDevice.getNetworkDevice(deviceName);
        Long physicalNetworkId = cmd.getPhysicalNetworkId();
        NiciraNvpDeviceVO niciraNvpDevice = null;
        
        PhysicalNetworkVO physicalNetwork = _physicalNetworkDao.findById(physicalNetworkId);
        if (physicalNetwork == null) {
            throw new InvalidParameterValueException("Could not find phyical network with ID: " + physicalNetworkId);
        }
        long zoneId = physicalNetwork.getDataCenterId();

        PhysicalNetworkServiceProviderVO ntwkSvcProvider = _physicalNetworkServiceProviderDao.findByServiceProvider(physicalNetwork.getId(), networkDevice.getNetworkServiceProvder());
        if (ntwkSvcProvider == null) {
            throw new CloudRuntimeException("Network Service Provider: " + networkDevice.getNetworkServiceProvder() +
                    " is not enabled in the physical network: " + physicalNetworkId + "to add this device");
        } else if (ntwkSvcProvider.getState() == PhysicalNetworkServiceProvider.State.Shutdown) {
            throw new CloudRuntimeException("Network Service Provider: " + ntwkSvcProvider.getProviderName() +
                    " is in shutdown state in the physical network: " + physicalNetworkId + "to add this device");
        }
        
        if (_niciraNvpDao.listByPhysicalNetwork(physicalNetworkId).size() != 0) {
            throw new CloudRuntimeException("A NiciraNvp device is already configured on this physical network");
        }
        
        Map<String, String> params = new HashMap<String,String>();
        params.put("guid", UUID.randomUUID().toString());
        params.put("zoneId", String.valueOf(physicalNetwork.getDataCenterId()));
        params.put("physicalNetworkId", String.valueOf(physicalNetwork.getId()));
        params.put("name", "Nicira Controller - " + cmd.getHost());
        params.put("ip", cmd.getHost());
        params.put("adminuser", cmd.getUsername());
        params.put("adminpass", cmd.getPassword());
        params.put("transportzoneuuid", cmd.getTransportzoneUuid());
        params.put("transportzoneisotype", physicalNetwork.getIsolationMethods().get(0).toLowerCase()); // FIXME What to do with multiple isolation types

        Map<String, Object> hostdetails = new HashMap<String,Object>();
        hostdetails.putAll(params);
        
        
        Transaction txn = Transaction.currentTxn();
        try {
            resource.configure(cmd.getHost(), hostdetails);
            
            Host host = _resourceMgr.addHost(zoneId, resource, Host.Type.L2Networking, params);
            if (host != null) {
                txn.start();
                
                niciraNvpDevice = new NiciraNvpDeviceVO(host.getId(), physicalNetworkId, ntwkSvcProvider.getProviderName(), deviceName);
                _niciraNvpDao.persist(niciraNvpDevice);
                
                DetailVO detail = new DetailVO(host.getId(), "niciranvpdeviceid", String.valueOf(niciraNvpDevice.getId()));
                _hostDetailsDao.persist(detail);

                txn.commit();
                return niciraNvpDevice;
            } else {
                throw new CloudRuntimeException("Failed to add Nicira Nvp Device due to internal error.");
            }            
        } catch (ConfigurationException e) {
            txn.rollback();
            throw new CloudRuntimeException(e.getMessage());
        }
    }

    @Override
    public NiciraNvpDeviceResponse createNiciraNvpDeviceResponse(
            NiciraNvpDeviceVO niciraNvpDeviceVO) {
        NiciraNvpDeviceResponse response = new NiciraNvpDeviceResponse();
        response.setDeviceName(niciraNvpDeviceVO.getDeviceName());
        response.setPhysicalNetworkId(niciraNvpDeviceVO.getPhysicalNetworkId());
        response.setId(niciraNvpDeviceVO.getId());
        response.setProviderName(niciraNvpDeviceVO.getProviderName());
        return response;
    }
    
    @Override
    public boolean deleteNiciraNvpDevice(DeleteNiciraNvpDeviceCmd cmd) {
        Long niciraDeviceId = cmd.getNiciraNvpDeviceId();
        NiciraNvpDeviceVO niciraNvpDevice = _niciraNvpDao.findById(niciraDeviceId);
        if (niciraNvpDevice == null) {
            throw new InvalidParameterValueException("Could not find a nicira device with id " + niciraDeviceId);
        }        
        
        // Find the physical network we work for
        Long physicalNetworkId = niciraNvpDevice.getPhysicalNetworkId();
        PhysicalNetworkVO physicalNetwork = _physicalNetworkDao.findById(physicalNetworkId);
        if (physicalNetwork != null) {
            // Lets see if there are networks that use us
            // Find the nicira networks on this physical network
            List<NetworkVO> networkList = _networkDao.listByPhysicalNetwork(physicalNetworkId);
            
            // Networks with broadcast type lswitch are ours
            for (NetworkVO network : networkList) {
                if (network.getBroadcastDomainType() == Networks.BroadcastDomainType.Lswitch) {
                    if ((network.getState() != Network.State.Shutdown) && (network.getState() != Network.State.Destroy)) {
                        throw new CloudRuntimeException("This Nicira Nvp device can not be deleted as there are one or more logical networks provisioned by cloudstack.");
                    }
                }
            }
        }
        
        HostVO niciraHost = _hostDao.findById(niciraNvpDevice.getHostId());
        Long hostId = niciraHost.getId();
        
        niciraHost.setResourceState(ResourceState.Maintenance);
        _hostDao.update(hostId, niciraHost);
        _resourceMgr.deleteHost(hostId, false, false);
    
        _niciraNvpDao.remove(niciraDeviceId);
        
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
            NiciraNvpDeviceVO niciraNvpDevice = _niciraNvpDao.findById(niciraNvpDeviceId);
            if (niciraNvpDevice == null) {
                throw new InvalidParameterValueException("Could not find Nicira Nvp device with id: " + niciraNvpDevice);
            }
            responseList.add(niciraNvpDevice);
        }
        else {
            PhysicalNetworkVO physicalNetwork = _physicalNetworkDao.findById(physicalNetworkId);
            if (physicalNetwork == null) {
                throw new InvalidParameterValueException("Could not find a physical network with id: " + physicalNetworkId);
            }
            responseList = _niciraNvpDao.listByPhysicalNetwork(physicalNetworkId);
        }
        
        return responseList;
    }
    
    @Override    
    public List<? extends Network> listNiciraNvpDeviceNetworks(ListNiciraNvpDeviceNetworksCmd cmd) {
        Long niciraDeviceId = cmd.getNiciraNvpDeviceId();
        NiciraNvpDeviceVO niciraNvpDevice = _niciraNvpDao.findById(niciraDeviceId);
        if (niciraNvpDevice == null) {
            throw new InvalidParameterValueException("Could not find a nicira device with id " + niciraDeviceId);
        }        
        
        // Find the physical network we work for
        Long physicalNetworkId = niciraNvpDevice.getPhysicalNetworkId();
        PhysicalNetworkVO physicalNetwork = _physicalNetworkDao.findById(physicalNetworkId);
        if (physicalNetwork == null) {
            // No such physical network, so no provisioned networks
            return Collections.emptyList();
        }
        
        // Find the nicira networks on this physical network
        List<NetworkVO> networkList = _networkDao.listByPhysicalNetwork(physicalNetworkId);
        
        // Networks with broadcast type lswitch are ours
        List<NetworkVO> responseList  = new ArrayList<NetworkVO>();
        for (NetworkVO network : networkList) {
            if (network.getBroadcastDomainType() == Networks.BroadcastDomainType.Lswitch) {
                responseList.add(network);
            }
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
        if (!(startup[0] instanceof StartupNiciraNvpCommand)) {
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

}
