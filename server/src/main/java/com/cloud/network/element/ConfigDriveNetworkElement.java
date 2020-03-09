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

import org.apache.cloudstack.engine.subsystem.api.storage.DataStore;
import org.apache.cloudstack.engine.subsystem.api.storage.DataStoreManager;
import org.apache.cloudstack.engine.subsystem.api.storage.EndPoint;
import org.apache.cloudstack.engine.subsystem.api.storage.EndPointSelector;
import org.apache.cloudstack.storage.configdrive.ConfigDrive;
import org.apache.cloudstack.storage.configdrive.ConfigDriveBuilder;
import org.apache.cloudstack.storage.to.TemplateObjectTO;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.collections.MapUtils;
import org.apache.log4j.Logger;

import com.cloud.agent.AgentManager;
import com.cloud.agent.api.Answer;
import com.cloud.agent.api.HandleConfigDriveIsoCommand;
import com.cloud.agent.api.to.DiskTO;
import com.cloud.configuration.ConfigurationManager;
import com.cloud.dc.dao.DataCenterDao;
import com.cloud.deploy.DeployDestination;
import com.cloud.exception.ConcurrentOperationException;
import com.cloud.exception.InsufficientCapacityException;
import com.cloud.exception.ResourceUnavailableException;
import com.cloud.exception.UnsupportedServiceException;
import com.cloud.host.dao.HostDao;
import com.cloud.network.Network;
import com.cloud.network.Network.Capability;
import com.cloud.network.Network.Provider;
import com.cloud.network.Network.Service;
import com.cloud.network.NetworkMigrationResponder;
import com.cloud.network.NetworkModel;
import com.cloud.network.Networks.TrafficType;
import com.cloud.network.PhysicalNetworkServiceProvider;
import com.cloud.offering.NetworkOffering;
import com.cloud.service.dao.ServiceOfferingDao;
import com.cloud.storage.DataStoreRole;
import com.cloud.storage.Storage;
import com.cloud.storage.StoragePool;
import com.cloud.storage.Volume;
import com.cloud.storage.VolumeVO;
import com.cloud.storage.dao.GuestOSCategoryDao;
import com.cloud.storage.dao.GuestOSDao;
import com.cloud.storage.dao.VolumeDao;
import com.cloud.utils.component.AdapterBase;
import com.cloud.utils.crypt.DBEncryptionUtil;
import com.cloud.utils.exception.CloudRuntimeException;
import com.cloud.utils.fsm.StateListener;
import com.cloud.utils.fsm.StateMachine2;
import com.cloud.vm.Nic;
import com.cloud.vm.NicProfile;
import com.cloud.vm.ReservationContext;
import com.cloud.vm.UserVmDetailVO;
import com.cloud.vm.UserVmVO;
import com.cloud.vm.VirtualMachine;
import com.cloud.vm.VirtualMachineManager;
import com.cloud.vm.VirtualMachineProfile;
import com.cloud.vm.VmDetailConstants;
import com.cloud.vm.dao.UserVmDao;
import com.cloud.vm.dao.UserVmDetailsDao;

public class ConfigDriveNetworkElement extends AdapterBase implements NetworkElement, UserDataServiceProvider,
        StateListener<VirtualMachine.State, VirtualMachine.Event, VirtualMachine>, NetworkMigrationResponder {
    private static final Logger LOG = Logger.getLogger(ConfigDriveNetworkElement.class);

    private static final Map<Service, Map<Capability, String>> capabilities = setCapabilities();

    @Inject
    NetworkModel _networkMgr;
    @Inject
    UserVmDao _userVmDao;
    @Inject
    UserVmDetailsDao _userVmDetailsDao;
    @Inject
    ConfigurationManager _configMgr;
    @Inject
    DataCenterDao _dcDao;
    @Inject
    ServiceOfferingDao _serviceOfferingDao;
    @Inject
    NetworkModel _networkModel;
    @Inject
    GuestOSCategoryDao _guestOSCategoryDao;
    @Inject
    GuestOSDao _guestOSDao;
    @Inject
    VolumeDao _volumeDao;
    @Inject
    HostDao _hostDao;
    @Inject
    AgentManager agentManager;
    @Inject
    DataStoreManager _dataStoreMgr;
    @Inject
    EndPointSelector _ep;

    private final static Integer CONFIGDRIVEDISKSEQ = 4;

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

        try {
            return deleteConfigDriveIso(vm.getVirtualMachine());
        } catch (ResourceUnavailableException e) {
            LOG.error("Failed to delete config drive due to: ", e);
            return false;
        }
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

    private String getSshKey(VirtualMachineProfile profile) {
        final UserVmDetailVO vmDetailSshKey = _userVmDetailsDao.findDetail(profile.getId(), VmDetailConstants.SSH_PUBLIC_KEY);
        return (vmDetailSshKey!=null ? vmDetailSshKey.getValue() : null);
    }

    @Override
    public boolean addPasswordAndUserdata(Network network, NicProfile nic, VirtualMachineProfile profile, DeployDestination dest, ReservationContext context)
            throws ConcurrentOperationException, InsufficientCapacityException, ResourceUnavailableException {
        return (canHandle(network.getTrafficType())
                && configureConfigDriveData(profile, nic))
                && createConfigDriveIso(profile, dest);
    }

    @Override
    public boolean savePassword(final Network network, final NicProfile nic, final VirtualMachineProfile vm) throws ResourceUnavailableException {
        // savePassword is called by resetPasswordForVirtualMachine API which requires VM to be shutdown
        // Upper layers should save password in db, we do not need to update/create config drive iso at this point
        // Config drive will be created with updated password when VM starts in future
        if (vm != null && vm.getVirtualMachine().getState().equals(VirtualMachine.State.Running)) {
            throw new CloudRuntimeException("VM should to stopped to reset password");
        }

        final boolean canHandle = canHandle(network.getTrafficType());

        if (canHandle) {
            storePasswordInVmDetails(vm);
        }

        return canHandle;
    }

    @Override
    public boolean saveSSHKey(final Network network, final NicProfile nic, final VirtualMachineProfile vm, final String sshPublicKey) throws ResourceUnavailableException {
        // saveSSHKey is called by resetSSHKeyForVirtualMachine API which requires VM to be shutdown
        // Upper layers should save ssh public key in db, we do not need to update/create config drive iso at this point
        // Config drive will be created with updated password when VM starts in future
        if (vm != null && vm.getVirtualMachine().getState().equals(VirtualMachine.State.Running)) {
            throw new CloudRuntimeException("VM should to stopped to reset password");
        }

        final boolean canHandle = canHandle(network.getTrafficType());

        if (canHandle) {
            storePasswordInVmDetails(vm);
        }

        return canHandle;
    }

    @Override
    public boolean saveUserData(final Network network, final NicProfile nic, final VirtualMachineProfile vm) throws ResourceUnavailableException {
        // saveUserData is called by updateVirtualMachine API which requires VM to be shutdown
        // Upper layers should save userdata in db, we do not need to update/create config drive iso at this point
        // Config drive will be created with updated password when VM starts in future
        if (vm != null && vm.getVirtualMachine().getState().equals(VirtualMachine.State.Running)) {
            throw new CloudRuntimeException("VM should to stopped to reset password");
        }
        return canHandle(network.getTrafficType());
    }

    /**
     * Store password in vm details so it can be picked up during VM start.
     */
    private void storePasswordInVmDetails(VirtualMachineProfile vm) {
        final String password = (String) vm.getParameter(VirtualMachineProfile.Param.VmPassword);
        final String password_encrypted = DBEncryptionUtil.encrypt(password);
        final UserVmVO userVmVO = _userVmDao.findById(vm.getId());

        _userVmDetailsDao.addDetail(vm.getId(), VmDetailConstants.PASSWORD,  password_encrypted, false);

        userVmVO.setUpdateParameters(true);
        _userVmDao.update(userVmVO.getId(), userVmVO);
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
    public boolean postStateTransitionEvent(StateMachine2.Transition<VirtualMachine.State, VirtualMachine.Event> transition, VirtualMachine vm, boolean status, Object opaque) {
        if (transition.getToState().equals(VirtualMachine.State.Expunging) && transition.getEvent().equals(VirtualMachine.Event.ExpungeOperation)) {
            Nic nic = _networkModel.getDefaultNic(vm.getId());
            if (nic == null) {
                return true;
            }
            try {
                final Network network = _networkMgr.getNetwork(nic.getNetworkId());
                final UserDataServiceProvider userDataUpdateProvider = _networkModel.getUserDataUpdateProvider(network);
                final Provider provider = userDataUpdateProvider.getProvider();
                if (provider.equals(Provider.ConfigDrive)) {
                    try {
                        return deleteConfigDriveIso(vm);
                    } catch (ResourceUnavailableException e) {
                        LOG.error("Failed to delete config drive due to: ", e);
                        return false;
                    }
                }
            } catch (UnsupportedServiceException usse) {}
        }
        return true;
    }

    @Override
    public boolean prepareMigration(NicProfile nic, Network network, VirtualMachineProfile vm, DeployDestination dest, ReservationContext context) {
        if (nic.isDefaultNic() && _networkModel.getUserDataUpdateProvider(network).getProvider().equals(Provider.ConfigDrive)) {
            LOG.trace(String.format("[prepareMigration] for vm: %s", vm.getInstanceName()));
            final DataStore dataStore = findDataStore(vm, dest);

            try {
                addConfigDriveDisk(vm, dataStore);
            } catch (ResourceUnavailableException e) {
                LOG.error("Failed to add config disk drive due to: ", e);
            }
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

    private DataStore findDataStore(VirtualMachineProfile profile, DeployDestination dest) {
        DataStore dataStore = null;
        if (VirtualMachineManager.VmConfigDriveOnPrimaryPool.value()) {
            if(MapUtils.isNotEmpty(dest.getStorageForDisks())) {
                dataStore = getPlannedDataStore(dest, dataStore);
            }
            if (dataStore == null) {
                dataStore = pickExistingRootVolumeFromDataStore(profile, dataStore);
            }
        } else {
            dataStore = _dataStoreMgr.getImageStoreWithFreeCapacity(dest.getDataCenter().getId());
        }
        return dataStore;
    }

    private DataStore getPlannedDataStore(DeployDestination dest, DataStore dataStore) {
        for (final Volume volume : dest.getStorageForDisks().keySet()) {
            if (volume.getVolumeType() == Volume.Type.ROOT) {
                final StoragePool primaryPool = dest.getStorageForDisks().get(volume);
                dataStore = _dataStoreMgr.getDataStore(primaryPool.getId(), DataStoreRole.Primary);
                break;
            }
        }
        return dataStore;
    }

    private DataStore pickExistingRootVolumeFromDataStore(VirtualMachineProfile profile, DataStore dataStore) {
        final List<VolumeVO> volumes = _volumeDao.findByInstanceAndType(profile.getVirtualMachine().getId(), Volume.Type.ROOT);
        if (CollectionUtils.isNotEmpty(volumes)) {
            dataStore = pickDataStoreFromVolumes(volumes);
        }
        return dataStore;
    }

    private DataStore pickDataStoreFromVolumes(List<VolumeVO> volumes) {
        DataStore dataStore = null;
        for (Volume vol : volumes) {
            if (doesVolumeStateCheckout(vol)) {
                dataStore = _dataStoreMgr.getDataStore(vol.getPoolId(), DataStoreRole.Primary);
                if (dataStore != null) {
                    return dataStore;
                }
            }
        }
        return dataStore;
    }

    private boolean doesVolumeStateCheckout(Volume vol) {
        switch (vol.getState()) {
        case Allocated:
        case Creating:
        case Ready:
        case Snapshotting:
        case RevertSnapshotting:
        case Resizing:
        case Copying:
        case Attaching:
            return true;
        case Migrating:
        case Expunging:
        case Expunged:
        case Destroy:
        case Destroying:
        case UploadOp:
        case Uploaded:
        case NotUploaded:
        case UploadInProgress:
        case UploadError:
        case UploadAbandoned:
            return false;
        default:
            throw new IllegalArgumentException("volume has a state that does not compute: " +vol.getState());
        }
    }

    private Long findAgentIdForImageStore(final DataStore dataStore) throws ResourceUnavailableException {
        EndPoint endpoint = _ep.select(dataStore);
        if (endpoint == null) {
            throw new ResourceUnavailableException("Config drive creation failed, secondary store not available",
                    dataStore.getClass(), dataStore.getId());
        }
        return endpoint.getId();
    }

    private Long findAgentId(VirtualMachineProfile profile, DeployDestination dest, DataStore dataStore) throws ResourceUnavailableException {
        Long agentId;
        if (dest.getHost() == null) {
            agentId = (profile.getVirtualMachine().getHostId() == null ? profile.getVirtualMachine().getLastHostId() : profile.getVirtualMachine().getHostId());
        } else {
            agentId = dest.getHost().getId();
        }
        if (!VirtualMachineManager.VmConfigDriveOnPrimaryPool.value()) {
            agentId = findAgentIdForImageStore(dataStore);
        }
        return agentId;
    }

    private boolean createConfigDriveIso(VirtualMachineProfile profile, DeployDestination dest) throws ResourceUnavailableException {
        final DataStore dataStore = findDataStore(profile, dest);
        final Long agentId = findAgentId(profile, dest, dataStore);
        if (agentId == null || dataStore == null) {
            throw new ResourceUnavailableException("Config drive iso creation failed, agent or datastore not available",
                    ConfigDriveNetworkElement.class, 0L);
        }

        LOG.debug("Creating config drive ISO for vm: " + profile.getInstanceName());

        final String isoFileName = ConfigDrive.configIsoFileName(profile.getInstanceName());
        final String isoPath = ConfigDrive.createConfigDrivePath(profile.getInstanceName());
        final String isoData = ConfigDriveBuilder.buildConfigDrive(profile.getVmData(), isoFileName, profile.getConfigDriveLabel());
        final HandleConfigDriveIsoCommand configDriveIsoCommand = new HandleConfigDriveIsoCommand(isoPath, isoData, dataStore.getTO(), true);

        final Answer answer = agentManager.easySend(agentId, configDriveIsoCommand);
        if (!answer.getResult()) {
            throw new ResourceUnavailableException(String.format("Config drive iso creation failed, details: %s",
                    answer.getDetails()), ConfigDriveNetworkElement.class, 0L);
        }
        addConfigDriveDisk(profile, dataStore);
        return true;
    }

    private boolean deleteConfigDriveIso(final VirtualMachine vm) throws ResourceUnavailableException {
        DataStore dataStore = _dataStoreMgr.getImageStoreWithFreeCapacity(vm.getDataCenterId());
        Long agentId = findAgentIdForImageStore(dataStore);

        if (VirtualMachineManager.VmConfigDriveOnPrimaryPool.value()) {
            List<VolumeVO> volumes = _volumeDao.findByInstanceAndType(vm.getId(), Volume.Type.ROOT);
            if (volumes != null && volumes.size() > 0) {
                dataStore = _dataStoreMgr.getDataStore(volumes.get(0).getPoolId(), DataStoreRole.Primary);
            }
            agentId = (vm.getHostId() != null) ? vm.getHostId() : vm.getLastHostId();
        }

        if (agentId == null || dataStore == null) {
            throw new ResourceUnavailableException("Config drive iso creation failed, agent or datastore not available",
                    ConfigDriveNetworkElement.class, 0L);
        }

        LOG.debug("Deleting config drive ISO for vm: " + vm.getInstanceName());

        final String isoPath = ConfigDrive.createConfigDrivePath(vm.getInstanceName());
        final HandleConfigDriveIsoCommand configDriveIsoCommand = new HandleConfigDriveIsoCommand(isoPath, null, dataStore.getTO(), false);

        final Answer answer = agentManager.easySend(agentId, configDriveIsoCommand);
        if (!answer.getResult()) {
            LOG.error("Failed to remove config drive for instance: " + vm.getInstanceName());
            return false;
        }
        return true;
    }

    private void addConfigDriveDisk(final VirtualMachineProfile profile, final DataStore dataStore) throws ResourceUnavailableException {
        boolean isoAvailable = false;
        final String isoPath = ConfigDrive.createConfigDrivePath(profile.getInstanceName());
        for (DiskTO dataTo : profile.getDisks()) {
            if (dataTo.getPath().equals(isoPath)) {
                isoAvailable = true;
                break;
            }
        }
        if (!isoAvailable) {
            TemplateObjectTO dataTO = new TemplateObjectTO();
            if (dataStore == null) {
                throw new ResourceUnavailableException("Config drive disk add failed, datastore not available",
                        ConfigDriveNetworkElement.class, 0L);
            }
            dataTO.setDataStore(dataStore.getTO());
            dataTO.setUuid(profile.getUuid());
            dataTO.setPath(isoPath);
            dataTO.setFormat(Storage.ImageFormat.ISO);

            profile.addDisk(new DiskTO(dataTO, CONFIGDRIVEDISKSEQ.longValue(), isoPath, Volume.Type.ISO));
        } else {
            LOG.warn("Config drive iso already is in VM profile.");
        }
    }

    private boolean configureConfigDriveData(final VirtualMachineProfile profile, final NicProfile nic) {
        final UserVmVO vm = _userVmDao.findById(profile.getId());
        if (vm.getType() != VirtualMachine.Type.User) {
            return false;
        }
        final Nic defaultNic = _networkModel.getDefaultNic(vm.getId());
        if (defaultNic != null) {
            final String sshPublicKey = getSshKey(profile);
            final String serviceOffering = _serviceOfferingDao.findByIdIncludingRemoved(vm.getId(), vm.getServiceOfferingId()).getDisplayText();
            boolean isWindows = _guestOSCategoryDao.findById(_guestOSDao.findById(vm.getGuestOSId()).getCategoryId()).getName().equalsIgnoreCase("Windows");

            final List<String[]> vmData = _networkModel.generateVmData(vm.getUserData(), serviceOffering, vm.getDataCenterId(), vm.getInstanceName(), vm.getHostName(), vm.getId(),
                    vm.getUuid(), nic.getIPv4Address(), sshPublicKey, (String) profile.getParameter(VirtualMachineProfile.Param.VmPassword), isWindows);
            profile.setVmData(vmData);
            profile.setConfigDriveLabel(VirtualMachineManager.VmConfigDriveLabel.value());
        }
        return true;
    }

}
