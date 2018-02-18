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

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.inject.Inject;

import org.apache.log4j.Logger;

import org.apache.cloudstack.engine.orchestration.service.VolumeOrchestrationService;
import org.apache.cloudstack.engine.subsystem.api.storage.DataStore;
import org.apache.cloudstack.engine.subsystem.api.storage.DataStoreManager;
import org.apache.cloudstack.engine.subsystem.api.storage.EndPoint;
import org.apache.cloudstack.engine.subsystem.api.storage.EndPointSelector;
import org.apache.cloudstack.storage.to.TemplateObjectTO;
import com.cloud.agent.AgentManager;
import com.cloud.agent.api.Answer;
import com.cloud.agent.api.AttachIsoAnswer;
import com.cloud.agent.api.AttachIsoCommand;
import com.cloud.agent.api.HandleConfigDriveIsoCommand;
import com.cloud.agent.api.to.DiskTO;
import com.cloud.configuration.ConfigurationManager;
import com.cloud.dc.dao.DataCenterDao;
import com.cloud.deploy.DeployDestination;
import com.cloud.exception.ConcurrentOperationException;
import com.cloud.exception.InsufficientCapacityException;
import com.cloud.exception.OperationTimedoutException;
import com.cloud.exception.ResourceUnavailableException;
import com.cloud.exception.UnsupportedServiceException;
import com.cloud.host.Host;
import com.cloud.host.dao.HostDao;
import com.cloud.network.Network;
import com.cloud.network.Network.Capability;
import com.cloud.network.Network.Provider;
import com.cloud.network.Network.Service;
import com.cloud.network.NetworkMigrationResponder;
import com.cloud.network.NetworkModel;
import com.cloud.network.Networks.TrafficType;
import com.cloud.network.PhysicalNetworkServiceProvider;
import com.cloud.network.dao.NetworkDao;
import com.cloud.offering.NetworkOffering;
import com.cloud.service.dao.ServiceOfferingDao;
import com.cloud.storage.Storage;
import com.cloud.storage.Volume;
import com.cloud.storage.dao.GuestOSCategoryDao;
import com.cloud.storage.dao.GuestOSDao;
import com.cloud.utils.component.AdapterBase;
import com.cloud.utils.fsm.StateListener;
import com.cloud.utils.fsm.StateMachine2;
import com.cloud.vm.Nic;
import com.cloud.vm.NicProfile;
import com.cloud.vm.ReservationContext;
import com.cloud.vm.UserVmDetailVO;
import com.cloud.vm.UserVmManager;
import com.cloud.vm.UserVmVO;
import com.cloud.vm.VirtualMachine;
import com.cloud.vm.VirtualMachineManager;
import com.cloud.vm.VirtualMachineProfile;
import com.cloud.vm.dao.DomainRouterDao;
import com.cloud.vm.dao.UserVmDao;
import com.cloud.vm.dao.UserVmDetailsDao;

public class ConfigDriveNetworkElement extends AdapterBase implements NetworkElement, UserDataServiceProvider,
        StateListener<VirtualMachine.State, VirtualMachine.Event, VirtualMachine>, NetworkMigrationResponder {
    private static final Logger s_logger = Logger.getLogger(ConfigDriveNetworkElement.class);

    private static final Map<Service, Map<Capability, String>> capabilities = setCapabilities();

    @Inject
    NetworkDao _networkConfigDao;
    @Inject
    NetworkModel _networkMgr;
    @Inject
    UserVmManager _userVmMgr;
    @Inject
    UserVmDao _userVmDao;
    @Inject
    UserVmDetailsDao _userVmDetailsDao;
    @Inject
    DomainRouterDao _routerDao;
    @Inject
    ConfigurationManager _configMgr;
    @Inject
    DataCenterDao _dcDao;
    @Inject
    AgentManager _agentManager;
    @Inject
    ServiceOfferingDao _serviceOfferingDao;
    @Inject
    NetworkModel _networkModel;
    @Inject
    GuestOSCategoryDao _guestOSCategoryDao;
    @Inject
    GuestOSDao _guestOSDao;
    @Inject
    HostDao _hostDao;
    @Inject
    DataStoreManager _dataStoreMgr;
    @Inject
    EndPointSelector _ep;
    @Inject
    VolumeOrchestrationService _volumeMgr;

    public final static String CONFIGDRIVEFILENAME = "configdrive.iso";
    public final static String CONFIGDRIVEDIR= "ConfigDrive";
    public final static Integer CONFIGDRIVEDISKSEQ= new Integer(4);

    private boolean canHandle(TrafficType trafficType) {
        return trafficType.equals(TrafficType.Guest);
    }

    @Override
    public boolean start() {
        VirtualMachine.State.getStateMachine().registerListener(this);
        return super.start();
    }

    @Override
    public boolean implement(Network network, NetworkOffering offering, DeployDestination dest, ReservationContext context) throws ResourceUnavailableException, ConcurrentOperationException,
            InsufficientCapacityException {
        return canHandle(offering.getTrafficType());
    }

    @Override
    public boolean prepare(Network network, NicProfile nic, VirtualMachineProfile vmProfile, DeployDestination dest, ReservationContext context) throws ConcurrentOperationException,
            InsufficientCapacityException, ResourceUnavailableException {
        return true;
    }

    @Override
    public boolean release(Network network, NicProfile nic, VirtualMachineProfile vm, ReservationContext context) {
        if (!nic.isDefaultNic()) {
            return true;
        }
        // Remove form secondary storage
        DataStore secondaryStore = _dataStoreMgr.getImageStore(network.getDataCenterId());

        String isoFile =  "/" + CONFIGDRIVEDIR + "/" + vm.getInstanceName()+ "/" + CONFIGDRIVEFILENAME;
        HandleConfigDriveIsoCommand deleteCommand = new HandleConfigDriveIsoCommand(vm.getVmData(),
                vm.getConfigDriveLabel(), secondaryStore.getTO(), isoFile, false, false);
        // Delete the ISO on the secondary store
        EndPoint endpoint = _ep.select(secondaryStore);
        if (endpoint == null) {
            s_logger.error(String.format("Secondary store: %s not available", secondaryStore.getName()));
            return false;
        }
        Answer answer = endpoint.sendMessage(deleteCommand);
        return answer.getResult();
    }

    @Override
    public boolean shutdown(Network network, ReservationContext context, boolean cleanup) throws ConcurrentOperationException, ResourceUnavailableException {
        return true; // assume that the agent will remove userdata etc
    }

    @Override
    public boolean destroy(Network config, ReservationContext context) throws ConcurrentOperationException, ResourceUnavailableException {
        return true; // assume that the agent will remove userdata etc
    }

    @Override
    public Provider getProvider() {
        return Provider.ConfigDrive;
    }

    @Override
    public Map<Service, Map<Capability, String>> getCapabilities() {
        return capabilities;
    }

    private static Map<Service, Map<Capability, String>> setCapabilities() {
        Map<Service, Map<Capability, String>> capabilities = new HashMap<>();
        capabilities.put(Service.UserData, null);
        return capabilities;
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
    public boolean addPasswordAndUserdata(Network network, NicProfile nic, VirtualMachineProfile profile, DeployDestination dest, ReservationContext context)
            throws ConcurrentOperationException, InsufficientCapacityException, ResourceUnavailableException {
        UserVmDetailVO vmDetailSshKey = _userVmDetailsDao.findDetail(profile.getId(), "SSH.PublicKey");
        return (canHandle(network.getTrafficType()) && updateConfigDrive(profile,
                (vmDetailSshKey!=null?vmDetailSshKey.getValue():null)))
                && updateConfigDriveIso(network, profile, dest.getHost(), false);
    }

    @Override
    public boolean savePassword(Network network, NicProfile nic, VirtualMachineProfile profile) throws ResourceUnavailableException {
        if (!(canHandle(network.getTrafficType()) && updateConfigDrive(profile, (String) profile.getParameter(VirtualMachineProfile.Param.VmSshPubKey)))) return false;
        return updateConfigDriveIso(network, profile, true);
    }

    @Override
    public boolean saveSSHKey(Network network, NicProfile nic, VirtualMachineProfile vm, String sshPublicKey) throws ResourceUnavailableException {
        if (!(canHandle(network.getTrafficType()) && updateConfigDrive(vm, sshPublicKey))) return false;
        return updateConfigDriveIso(network, vm, true);
    }

    @Override
    public boolean saveUserData(Network network, NicProfile nic, VirtualMachineProfile profile) throws ResourceUnavailableException {
        if (!(canHandle(network.getTrafficType()) && updateConfigDrive(profile, (String) profile.getParameter(VirtualMachineProfile.Param.VmSshPubKey)))) return false;
        return updateConfigDriveIso(network, profile, true);
    }

    @Override
    public boolean verifyServicesCombination(Set<Service> services) {
        return true;
    }

    @Override
    public boolean preStateTransitionEvent(VirtualMachine.State oldState, VirtualMachine.Event event, VirtualMachine.State newState, VirtualMachine vo, boolean status, Object opaque) {
        return true;
    }

    @Override
    public boolean postStateTransitionEvent(StateMachine2.Transition<VirtualMachine.State, VirtualMachine.Event> transition, VirtualMachine vo, boolean status, Object opaque) {
        if (transition.getToState().equals(VirtualMachine.State.Expunging) && transition.getEvent().equals(VirtualMachine.Event.ExpungeOperation)) {
            Nic nic = _networkModel.getDefaultNic(vo.getId());
            try {
                if (nic != null) {
                    final Network network = _networkMgr.getNetwork(nic.getNetworkId());
                    final UserDataServiceProvider userDataUpdateProvider = _networkModel.getUserDataUpdateProvider(network);
                    final Provider provider = userDataUpdateProvider.getProvider();
                    if (provider.equals(Provider.ConfigDrive)) {
                        // Delete config drive ISO on destroy
                        DataStore secondaryStore = _dataStoreMgr.getImageStore(vo.getDataCenterId());
                        String isoFile = "/" + CONFIGDRIVEDIR + "/" + vo.getInstanceName() + "/" + CONFIGDRIVEFILENAME;
                        HandleConfigDriveIsoCommand deleteCommand = new HandleConfigDriveIsoCommand(null,
                                null, secondaryStore.getTO(), isoFile, false, false);
                        EndPoint endpoint = _ep.select(secondaryStore);
                        if (endpoint == null) {
                            s_logger.error(String.format("Secondary store: %s not available", secondaryStore.getName()));
                            return false;
                        }
                        Answer answer = endpoint.sendMessage(deleteCommand);
                        if (!answer.getResult()) {
                            s_logger.error(String.format("Update ISO failed, details: %s", answer.getDetails()));
                            return false;
                        }
                    }
                }
            } catch (UnsupportedServiceException usse) {}
        }
        return true;
    }

    @Override
    public boolean prepareMigration(NicProfile nic, Network network, VirtualMachineProfile vm, DeployDestination dest, ReservationContext context) {
        if (nic.isDefaultNic() && _networkModel.getUserDataUpdateProvider(network).getProvider().equals(Provider.ConfigDrive)) {
            s_logger.trace(String.format("[prepareMigration] for vm: %s", vm.getInstanceName()));
            DataStore secondaryStore = _dataStoreMgr.getImageStore(network.getDataCenterId());
            configureConfigDriveDisk(vm, secondaryStore);
            return false;
        }
        else return  true;
    }

    @Override
    public void rollbackMigration(NicProfile nic, Network network, VirtualMachineProfile vm, ReservationContext src, ReservationContext dst) {

    }

    @Override
    public void commitMigration(NicProfile nic, Network network, VirtualMachineProfile vm, ReservationContext src, ReservationContext dst) {

    }

    private boolean updateConfigDriveIso(Network network, VirtualMachineProfile profile, boolean update) throws ResourceUnavailableException {
        return updateConfigDriveIso(network, profile, null, update);
    }

    private boolean updateConfigDriveIso(Network network, VirtualMachineProfile profile, Host host, boolean update) throws ResourceUnavailableException {
        Integer deviceKey = null;
        Long hostId;
        if (host == null) {
            hostId = (profile.getVirtualMachine().getHostId() == null ? profile.getVirtualMachine().getLastHostId(): profile.getVirtualMachine().getHostId());
        } else {
            hostId = host.getId();
        }

        DataStore secondaryStore = _dataStoreMgr.getImageStore(network.getDataCenterId());
        // Detach the existing ISO file if the machine is running
        if (update && profile.getVirtualMachine().getState().equals(VirtualMachine.State.Running)) {
            s_logger.debug("Detach config drive ISO for  vm " + profile.getInstanceName() + " in host " + _hostDao.findById(hostId));
            deviceKey = detachIso(secondaryStore, profile.getInstanceName(), hostId);
        }

        // Create/Update the iso on the secondary store
        s_logger.debug(String.format("%s config drive ISO for  vm %s in host %s",
                (update?"update":"create"), profile.getInstanceName(), _hostDao.findById(hostId).getName()));
        EndPoint endpoint = _ep.select(secondaryStore);
        if (endpoint == null )
            throw new ResourceUnavailableException(String.format("%s failed, secondary store not available",
                    (update?"Update":"Create")),secondaryStore.getClass(),secondaryStore.getId());
        String isoPath = CONFIGDRIVEDIR + "/" + profile.getInstanceName() + "/"  + CONFIGDRIVEFILENAME;
        HandleConfigDriveIsoCommand configDriveIsoCommand = new HandleConfigDriveIsoCommand(profile.getVmData(),
                profile.getConfigDriveLabel(), secondaryStore.getTO(), isoPath, true, update);
        Answer createIsoAnswer = endpoint.sendMessage(configDriveIsoCommand);
        if (!createIsoAnswer.getResult()) {
            throw new ResourceUnavailableException(String.format("%s ISO failed, details: %s",
                    (update?"Update":"Create"), createIsoAnswer.getDetails()),ConfigDriveNetworkElement.class,0L);
        }
        configureConfigDriveDisk(profile, secondaryStore);

        // Re-attach the ISO if the machine is running
        if (update && profile.getVirtualMachine().getState().equals(VirtualMachine.State.Running)) {
            s_logger.debug("Re-attach config drive ISO for  vm " + profile.getInstanceName() + " in host " + _hostDao.findById(hostId));
            attachIso(secondaryStore, profile.getInstanceName(), hostId, deviceKey);
        }
        return true;

    }

    private void configureConfigDriveDisk(VirtualMachineProfile profile, DataStore secondaryStore) {
        boolean isoAvailable = false;
        String isoPath = CONFIGDRIVEDIR + "/" + profile.getInstanceName() + "/"  + CONFIGDRIVEFILENAME;
        for (DiskTO dataTo : profile.getDisks()) {
            if (dataTo.getPath().equals(isoPath)) {
                isoAvailable = true;
                break;
            }
        }
        if (!isoAvailable) {
            TemplateObjectTO dataTO = new TemplateObjectTO();
            dataTO.setDataStore(secondaryStore.getTO());
            dataTO.setUuid(profile.getUuid());
            dataTO.setPath(isoPath);
            dataTO.setFormat(Storage.ImageFormat.ISO);

            profile.addDisk(new DiskTO(dataTO, CONFIGDRIVEDISKSEQ.longValue(), isoPath, Volume.Type.ISO));
        }
    }

    private boolean updateConfigDrive(VirtualMachineProfile profile, String publicKey) {
        UserVmVO vm = _userVmDao.findById(profile.getId());
        if (vm.getType() != VirtualMachine.Type.User) {
            return false;
        }
        // add/update userdata and/or password info into vm profile
        Nic defaultNic = _networkModel.getDefaultNic(vm.getId());
        if (defaultNic != null) {
            final String serviceOffering = _serviceOfferingDao.findByIdIncludingRemoved(vm.getId(), vm.getServiceOfferingId()).getDisplayText();
            final String zoneName = _dcDao.findById(vm.getDataCenterId()).getName();
            boolean isWindows = _guestOSCategoryDao.findById(_guestOSDao.findById(vm.getGuestOSId()).getCategoryId()).getName().equalsIgnoreCase("Windows");

            List<String[]> vmData = _networkModel.generateVmData(vm.getUserData(), serviceOffering, zoneName, vm.getInstanceName(), vm.getId(),
                    publicKey, (String) profile.getParameter(VirtualMachineProfile.Param.VmPassword), isWindows);
            profile.setVmData(vmData);
            profile.setConfigDriveLabel(VirtualMachineManager.VmConfigDriveLabel.value());
        }
        return true;
    }

    private Integer detachIso (DataStore secondaryStore, String instanceName, Long hostId) throws ResourceUnavailableException {
        String isoPath = CONFIGDRIVEDIR + "/" + instanceName + "/"  + CONFIGDRIVEFILENAME;
        AttachIsoCommand isoCommand = new AttachIsoCommand(instanceName, secondaryStore.getUri() + "/" + isoPath, false, CONFIGDRIVEDISKSEQ, true);
        isoCommand.setStoreUrl(secondaryStore.getUri());
        Answer attachIsoAnswer = null;

        try {
            attachIsoAnswer = _agentManager.send(hostId, isoCommand);
        } catch (OperationTimedoutException e) {
            throw new ResourceUnavailableException("Detach ISO failed: " + e.getMessage(), ConfigDriveNetworkElement.class, 0L);
        }

        if (!attachIsoAnswer.getResult()) {
            throw new ResourceUnavailableException("Detach ISO failed: " + attachIsoAnswer.getDetails(), ConfigDriveNetworkElement.class, 0L);
        }

        if (attachIsoAnswer instanceof  AttachIsoAnswer) {
            return ((AttachIsoAnswer)attachIsoAnswer).getDeviceKey();
        } else {
            return CONFIGDRIVEDISKSEQ;
        }
    }

    private void attachIso (DataStore secondaryStore, String instanceName, Long hostId, Integer deviceKey) throws ResourceUnavailableException {
        String isoPath = CONFIGDRIVEDIR + "/" + instanceName + "/"  + CONFIGDRIVEFILENAME;
        AttachIsoCommand isoCommand = new AttachIsoCommand(instanceName, secondaryStore.getUri() + "/" + isoPath, true);
        isoCommand.setStoreUrl(secondaryStore.getUri());
        isoCommand.setDeviceKey(deviceKey);
        Answer attachIsoAnswer = null;
        try {
            attachIsoAnswer = _agentManager.send(hostId, isoCommand);
        } catch (OperationTimedoutException e) {
            throw new ResourceUnavailableException("Attach ISO failed: " + e.getMessage() ,ConfigDriveNetworkElement.class,0L);
        }
        if (!attachIsoAnswer.getResult()) {
            throw new ResourceUnavailableException("Attach ISO failed: " + attachIsoAnswer.getDetails(),ConfigDriveNetworkElement.class,0L);
        }
    }

}
