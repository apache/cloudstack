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
package org.apache.cloudstack.storage.snapshot;

import com.cloud.agent.AgentManager;
import com.cloud.agent.api.to.DiskTO;
import com.cloud.dc.dao.ClusterDao;
import com.cloud.exception.InvalidParameterValueException;
import com.cloud.host.HostVO;
import com.cloud.host.dao.HostDao;
import com.cloud.hypervisor.Hypervisor.HypervisorType;
import com.cloud.org.Cluster;
import com.cloud.org.Grouping.AllocationState;
import com.cloud.resource.ResourceState;
import com.cloud.server.ManagementService;
import com.cloud.storage.CreateSnapshotPayload;
import com.cloud.storage.DataStoreRole;
import com.cloud.storage.Snapshot;
import com.cloud.storage.SnapshotVO;
import com.cloud.storage.Storage.ImageFormat;
import com.cloud.storage.Volume;
import com.cloud.storage.VolumeVO;
import com.cloud.storage.dao.SnapshotDao;
import com.cloud.storage.dao.SnapshotDetailsDao;
import com.cloud.storage.dao.SnapshotDetailsVO;
import com.cloud.storage.dao.VolumeDao;
import com.cloud.utils.db.DB;
import com.cloud.utils.exception.CloudRuntimeException;
import com.cloud.utils.fsm.NoTransitionException;
import com.cloud.vm.VMInstanceVO;
import com.cloud.vm.dao.VMInstanceDao;
import com.google.common.base.Optional;
import com.google.common.base.Preconditions;
import org.apache.cloudstack.engine.subsystem.api.storage.ChapInfo;
import org.apache.cloudstack.engine.subsystem.api.storage.DataStore;
import org.apache.cloudstack.engine.subsystem.api.storage.DataStoreCapabilities;
import org.apache.cloudstack.engine.subsystem.api.storage.DataStoreManager;
import org.apache.cloudstack.engine.subsystem.api.storage.ObjectInDataStoreStateMachine;
import org.apache.cloudstack.engine.subsystem.api.storage.SnapshotDataFactory;
import org.apache.cloudstack.engine.subsystem.api.storage.SnapshotInfo;
import org.apache.cloudstack.engine.subsystem.api.storage.SnapshotResult;
import org.apache.cloudstack.engine.subsystem.api.storage.StrategyPriority;
import org.apache.cloudstack.engine.subsystem.api.storage.VolumeInfo;
import org.apache.cloudstack.engine.subsystem.api.storage.VolumeService;
import org.apache.cloudstack.storage.command.SnapshotAndCopyAnswer;
import org.apache.cloudstack.storage.command.SnapshotAndCopyCommand;
import org.apache.cloudstack.storage.datastore.db.PrimaryDataStoreDao;
import org.apache.cloudstack.storage.datastore.db.StoragePoolVO;
import org.apache.log4j.Logger;
import org.springframework.stereotype.Component;

import javax.inject.Inject;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

@Component
public class StorageSystemSnapshotStrategy extends SnapshotStrategyBase {
    private static final Logger s_logger = Logger.getLogger(StorageSystemSnapshotStrategy.class);

    @Inject private AgentManager agentMgr;
    @Inject private ClusterDao clusterDao;
    @Inject private DataStoreManager dataStoreMgr;
    @Inject private HostDao hostDao;
    @Inject private ManagementService mgr;
    @Inject private PrimaryDataStoreDao storagePoolDao;
    @Inject private SnapshotDao snapshotDao;
    @Inject private SnapshotDataFactory snapshotDataFactory;
    @Inject private SnapshotDetailsDao snapshotDetailsDao;
    @Inject private VMInstanceDao vmInstanceDao;
    @Inject private VolumeDao volumeDao;
    @Inject private VolumeService volService;

    @Override
    public SnapshotInfo backupSnapshot(SnapshotInfo snapshotInfo) {
        Preconditions.checkArgument(snapshotInfo != null, "backupSnapshot expects a valid snapshot");

        if (snapshotInfo.getLocationType() != Snapshot.LocationType.SECONDARY) {
            markAsBackedUp((SnapshotObject)snapshotInfo);

            return snapshotInfo;
        }

        // At this point, the snapshot is either taken as a native
        // snapshot on the storage or exists as a volume on the storage (clone).
        // If archive flag is passed in, we should copy this snapshot to secondary
        // storage and delete it from the primary storage.

        HostVO host = getHost(snapshotInfo.getVolumeId());
        boolean canStorageSystemCreateVolumeFromSnapshot = canStorageSystemCreateVolumeFromSnapshot(snapshotInfo.getBaseVolume().getPoolId());
        boolean computeClusterSupportsResign = clusterDao.getSupportsResigning(host.getClusterId());

        if (!canStorageSystemCreateVolumeFromSnapshot || !computeClusterSupportsResign) {
            String msg = "Cannot archive snapshot: canStorageSystemCreateVolumeFromSnapshot and/or computeClusterSupportsResign were false.";

            s_logger.warn(msg);

            throw new CloudRuntimeException(msg);
        }

        return snapshotSvr.backupSnapshot(snapshotInfo);
    }

    @Override
    public boolean deleteSnapshot(Long snapshotId) {
        SnapshotVO snapshotVO = snapshotDao.findById(snapshotId);

        if (Snapshot.State.Destroyed.equals(snapshotVO.getState())) {
            return true;
        }

        if (Snapshot.State.Error.equals(snapshotVO.getState())) {
            snapshotDao.remove(snapshotId);

            return true;
        }

        if (!Snapshot.State.BackedUp.equals(snapshotVO.getState())) {
            throw new InvalidParameterValueException("Unable to delete snapshotshot " + snapshotId + " because it is in the following state: " + snapshotVO.getState());
        }

        return cleanupSnapshotOnPrimaryStore(snapshotId);
    }

    /**
     * Cleans up a snapshot which was taken on a primary store. This function
     * removes
     *
     * @param snapshotId: ID of snapshot that needs to be removed
     * @return true if snapshot is removed, false otherwise
     */

    private boolean cleanupSnapshotOnPrimaryStore(long snapshotId) {

        SnapshotObject snapshotObj = (SnapshotObject)snapshotDataFactory.getSnapshot(snapshotId, DataStoreRole.Primary);

        if (snapshotObj == null) {
            s_logger.debug("Can't find snapshot; deleting it in DB");

            snapshotDao.remove(snapshotId);

            return true;
        }

        if (ObjectInDataStoreStateMachine.State.Copying.equals(snapshotObj.getStatus())) {
            throw new InvalidParameterValueException("Unable to delete snapshotshot " + snapshotId + " because it is in the copying state.");
        }

        try {
            snapshotObj.processEvent(Snapshot.Event.DestroyRequested);
        }
        catch (NoTransitionException e) {
            s_logger.debug("Failed to set the state to destroying: ", e);

            return false;
        }

        try {
            snapshotSvr.deleteSnapshot(snapshotObj);

            snapshotObj.processEvent(Snapshot.Event.OperationSucceeded);
        }
        catch (Exception e) {
            s_logger.debug("Failed to delete snapshot: ", e);

            try {
                snapshotObj.processEvent(Snapshot.Event.OperationFailed);
            }
            catch (NoTransitionException e1) {
                s_logger.debug("Failed to change snapshot state: " + e.toString());
            }

            return false;
        }
        return true;
    }

    @Override
    public boolean revertSnapshot(SnapshotInfo snapshot) {
        throw new UnsupportedOperationException("Reverting not supported. Create a template or volume based on the snapshot instead.");
    }

    @Override
    @DB
    public SnapshotInfo takeSnapshot(SnapshotInfo snapshotInfo) {
        VolumeInfo volumeInfo = snapshotInfo.getBaseVolume();

        if (volumeInfo.getFormat() != ImageFormat.VHD) {
            throw new CloudRuntimeException("Only the " + ImageFormat.VHD.toString() + " image type is currently supported.");
        }

        SnapshotVO snapshotVO = snapshotDao.acquireInLockTable(snapshotInfo.getId());

        if (snapshotVO == null) {
            throw new CloudRuntimeException("Failed to acquire lock on the following snapshot: " + snapshotInfo.getId());
        }

        SnapshotResult result = null;
        SnapshotInfo snapshotOnPrimary = null;

        try {
            volumeInfo.stateTransit(Volume.Event.SnapshotRequested);

            // only XenServer is currently supported
            HostVO hostVO = getHost(volumeInfo.getId());

            boolean canStorageSystemCreateVolumeFromSnapshot = canStorageSystemCreateVolumeFromSnapshot(volumeInfo.getPoolId());
            boolean computeClusterSupportsResign = clusterDao.getSupportsResigning(hostVO.getClusterId());

            // if canStorageSystemCreateVolumeFromSnapshot && computeClusterSupportsResign, then take a back-end snapshot or create a back-end clone;
            // else, just create a new back-end volume (eventually used to create a new SR on and to copy a VDI to)

            if (canStorageSystemCreateVolumeFromSnapshot && computeClusterSupportsResign) {
                SnapshotDetailsVO snapshotDetail = new SnapshotDetailsVO(snapshotInfo.getId(),
                    "takeSnapshot",
                    Boolean.TRUE.toString(),
                    false);

                snapshotDetailsDao.persist(snapshotDetail);
            }

            result = snapshotSvr.takeSnapshot(snapshotInfo);

            if (result.isFailed()) {
                s_logger.debug("Failed to take a snapshot: " + result.getResult());

                throw new CloudRuntimeException(result.getResult());
            }

            if (!canStorageSystemCreateVolumeFromSnapshot || !computeClusterSupportsResign) {
                performSnapshotAndCopyOnHostSide(volumeInfo, snapshotInfo);
            }

            snapshotOnPrimary = result.getSnapshot();
        }
        finally {
            if (result != null && result.isSuccess()) {
                volumeInfo.stateTransit(Volume.Event.OperationSucceeded);
            } else {
                volumeInfo.stateTransit(Volume.Event.OperationFailed);
            }
        }

        snapshotDao.releaseFromLockTable(snapshotInfo.getId());

        return snapshotOnPrimary;
    }

    @Override
    public void postSnapshotCreation(SnapshotInfo snapshot) {
        updateLocationTypeInDb(snapshot);

        if (snapshot.getLocationType() == Snapshot.LocationType.SECONDARY) {
            // remove the snapshot on primary storage
            try {
                snapshotSvr.deleteSnapshot(snapshot);
            } catch (Exception e) {
                s_logger.warn("Failed to clean up snapshot '" + snapshot.getId() + "' on primary storage: " + e.getMessage());
            }
        }

    }

    private void updateLocationTypeInDb(SnapshotInfo snapshotInfo) {
        Object objPayload = snapshotInfo.getPayload();

        if (objPayload instanceof CreateSnapshotPayload) {
            CreateSnapshotPayload payload = (CreateSnapshotPayload)objPayload;

            SnapshotVO snapshot = snapshotDao.findById(snapshotInfo.getId());

            snapshot.setLocationType(payload.getLocationType());

            snapshotDao.update(snapshotInfo.getId(), snapshot);
        }
    }

    private boolean canStorageSystemCreateVolumeFromSnapshot(long storagePoolId) {
        boolean supportsCloningVolumeFromSnapshot = false;

        DataStore dataStore = dataStoreMgr.getDataStore(storagePoolId, DataStoreRole.Primary);

        Map<String, String> mapCapabilities = dataStore.getDriver().getCapabilities();

        if (mapCapabilities != null) {
            String value = mapCapabilities.get(DataStoreCapabilities.CAN_CREATE_VOLUME_FROM_SNAPSHOT.toString());

            supportsCloningVolumeFromSnapshot = Boolean.valueOf(value);
        }

        return supportsCloningVolumeFromSnapshot;
    }

    private void performSnapshotAndCopyOnHostSide(VolumeInfo volumeInfo, SnapshotInfo snapshotInfo) {
        Map<String, String> sourceDetails = null;

        VolumeVO volumeVO = volumeDao.findById(volumeInfo.getId());

        Long vmInstanceId = volumeVO.getInstanceId();
        VMInstanceVO vmInstanceVO = vmInstanceDao.findById(vmInstanceId);

        Long hostId = null;

        // if the volume to snapshot is associated with a VM
        if (vmInstanceVO != null) {
            hostId = vmInstanceVO.getHostId();

            // if the VM is not associated with a host
            if (hostId == null) {
                hostId = vmInstanceVO.getLastHostId();

                if (hostId == null) {
                    sourceDetails = getSourceDetails(volumeInfo);
                }
            }
        }
        // volume to snapshot is not associated with a VM (could be a data disk in the detached state)
        else {
            sourceDetails = getSourceDetails(volumeInfo);
        }

        HostVO hostVO = null;

        if (hostId != null) {
            hostVO = hostDao.findById(hostId);
        }
        else {
            Optional<HostVO> optHostVO = getHost(volumeInfo.getDataCenterId(), false);

            if (optHostVO.isPresent()) {
                hostVO = optHostVO.get();
            }
        }

        if (hostVO == null) {
            final String errMsg = "Unable to locate an applicable host";

            s_logger.error("performSnapshotAndCopyOnHostSide: " + errMsg);

            throw new CloudRuntimeException(errMsg);
        }

        long storagePoolId = volumeVO.getPoolId();
        StoragePoolVO storagePoolVO = storagePoolDao.findById(storagePoolId);
        DataStore dataStore = dataStoreMgr.getDataStore(storagePoolId, DataStoreRole.Primary);

        Map<String, String> destDetails = getDestDetails(storagePoolVO, snapshotInfo);

        SnapshotAndCopyCommand snapshotAndCopyCommand = new SnapshotAndCopyCommand(volumeInfo.getPath(), sourceDetails, destDetails);

        SnapshotAndCopyAnswer snapshotAndCopyAnswer = null;

        try {
            // if sourceDetails != null, we need to connect the host(s) to the volume
            if (sourceDetails != null) {
                volService.grantAccess(volumeInfo, hostVO, dataStore);
            }

            volService.grantAccess(snapshotInfo, hostVO, dataStore);

            snapshotAndCopyAnswer = (SnapshotAndCopyAnswer)agentMgr.send(hostVO.getId(), snapshotAndCopyCommand);
        }
        catch (Exception ex) {
            throw new CloudRuntimeException(ex.getMessage());
        }
        finally {
            try {
                volService.revokeAccess(snapshotInfo, hostVO, dataStore);

                // if sourceDetails != null, we need to disconnect the host(s) from the volume
                if (sourceDetails != null) {
                    volService.revokeAccess(volumeInfo, hostVO, dataStore);
                }
            }
            catch (Exception ex) {
                s_logger.debug(ex.getMessage(), ex);
            }
        }

        if (snapshotAndCopyAnswer == null || !snapshotAndCopyAnswer.getResult()) {
            final String errMsg;

            if (snapshotAndCopyAnswer != null && snapshotAndCopyAnswer.getDetails() != null && !snapshotAndCopyAnswer.getDetails().isEmpty()) {
                errMsg = snapshotAndCopyAnswer.getDetails();
            }
            else {
                errMsg = "Unable to perform host-side operation";
            }

            throw new CloudRuntimeException(errMsg);
        }

        String path = snapshotAndCopyAnswer.getPath(); // for XenServer, this is the VDI's UUID

        SnapshotDetailsVO snapshotDetail = new SnapshotDetailsVO(snapshotInfo.getId(),
                DiskTO.PATH,
                path,
                false);

        snapshotDetailsDao.persist(snapshotDetail);
    }

    private Map<String, String> getSourceDetails(VolumeInfo volumeInfo) {
        Map<String, String> sourceDetails = new HashMap<>();

        VolumeVO volumeVO = volumeDao.findById(volumeInfo.getId());

        long storagePoolId = volumeVO.getPoolId();
        StoragePoolVO storagePoolVO = storagePoolDao.findById(storagePoolId);

        sourceDetails.put(DiskTO.STORAGE_HOST, storagePoolVO.getHostAddress());
        sourceDetails.put(DiskTO.STORAGE_PORT, String.valueOf(storagePoolVO.getPort()));
        sourceDetails.put(DiskTO.IQN, volumeVO.get_iScsiName());

        ChapInfo chapInfo = volService.getChapInfo(volumeInfo, volumeInfo.getDataStore());

        if (chapInfo != null) {
            sourceDetails.put(DiskTO.CHAP_INITIATOR_USERNAME, chapInfo.getInitiatorUsername());
            sourceDetails.put(DiskTO.CHAP_INITIATOR_SECRET, chapInfo.getInitiatorSecret());
            sourceDetails.put(DiskTO.CHAP_TARGET_USERNAME, chapInfo.getTargetUsername());
            sourceDetails.put(DiskTO.CHAP_TARGET_SECRET, chapInfo.getTargetSecret());
        }

        return sourceDetails;
    }

    private Map<String, String> getDestDetails(StoragePoolVO storagePoolVO, SnapshotInfo snapshotInfo) {
        Map<String, String> destDetails = new HashMap<>();

        destDetails.put(DiskTO.STORAGE_HOST, storagePoolVO.getHostAddress());
        destDetails.put(DiskTO.STORAGE_PORT, String.valueOf(storagePoolVO.getPort()));

        long snapshotId = snapshotInfo.getId();

        destDetails.put(DiskTO.IQN, getProperty(snapshotId, DiskTO.IQN));

        destDetails.put(DiskTO.CHAP_INITIATOR_USERNAME, getProperty(snapshotId, DiskTO.CHAP_INITIATOR_USERNAME));
        destDetails.put(DiskTO.CHAP_INITIATOR_SECRET, getProperty(snapshotId, DiskTO.CHAP_INITIATOR_SECRET));
        destDetails.put(DiskTO.CHAP_TARGET_USERNAME, getProperty(snapshotId, DiskTO.CHAP_TARGET_USERNAME));
        destDetails.put(DiskTO.CHAP_TARGET_SECRET, getProperty(snapshotId, DiskTO.CHAP_TARGET_SECRET));

        return destDetails;
    }

    private String getProperty(long snapshotId, String property) {
        SnapshotDetailsVO snapshotDetails = snapshotDetailsDao.findDetail(snapshotId, property);

        if (snapshotDetails != null) {
            return snapshotDetails.getValue();
        }

        return null;
    }

    private HostVO getHost(long volumeId) {
        VolumeVO volumeVO = volumeDao.findById(volumeId);

        Long vmInstanceId = volumeVO.getInstanceId();
        VMInstanceVO vmInstanceVO = vmInstanceDao.findById(vmInstanceId);

        Long hostId = null;

        // if the volume to snapshot is associated with a VM
        if (vmInstanceVO != null) {
            hostId = vmInstanceVO.getHostId();

            // if the VM is not associated with a host
            if (hostId == null) {
                hostId = vmInstanceVO.getLastHostId();
            }
        }

        return getHost(volumeVO.getDataCenterId(), hostId);
    }

    private HostVO getHost(long zoneId, Long hostId) {
        Optional<HostVO> optHostVO = getHost(zoneId, true);

        if (optHostVO.isPresent()) {
            return optHostVO.get();
        }

        HostVO hostVO = hostDao.findById(hostId);

        if (hostVO != null) {
            return hostVO;
        }

        optHostVO = getHost(zoneId, false);

        if (optHostVO.isPresent()) {
            return optHostVO.get();
        }

        throw new CloudRuntimeException("Unable to locate an applicable host");
    }

    private Optional<HostVO> getHost(long zoneId, boolean computeClusterMustSupportResign) {
        List<? extends Cluster> clusters = mgr.searchForClusters(zoneId, 0L, Long.MAX_VALUE, HypervisorType.XenServer.toString());

        if (clusters == null) {
            clusters = new ArrayList<>();
        }

        Collections.shuffle(clusters, new Random(System.nanoTime()));

        clusters:
        for (Cluster cluster : clusters) {
            if (cluster.getAllocationState() == AllocationState.Enabled) {
                List<HostVO> hosts = hostDao.findByClusterId(cluster.getId());

                if (hosts != null) {
                    Collections.shuffle(hosts, new Random(System.nanoTime()));

                    for (HostVO host : hosts) {
                        if (host.getResourceState() == ResourceState.Enabled) {
                            if (computeClusterMustSupportResign) {
                                if (clusterDao.getSupportsResigning(cluster.getId())) {
                                    return Optional.of(host);
                                }
                                else {
                                    // no other host in the cluster in question should be able to satisfy our requirements here, so move on to the next cluster
                                    continue clusters;
                                }
                            }
                            else {
                                return Optional.of(host);
                            }
                        }
                    }
                }
            }
        }

        return Optional.absent();
    }

    private void markAsBackedUp(SnapshotObject snapshotObj) {
        try {
            snapshotObj.processEvent(Snapshot.Event.BackupToSecondary);
            snapshotObj.processEvent(Snapshot.Event.OperationSucceeded);
        }
        catch (NoTransitionException ex) {
            s_logger.debug("Failed to change state: " + ex.toString());

            try {
                snapshotObj.processEvent(Snapshot.Event.OperationFailed);
            }
            catch (NoTransitionException ex2) {
                s_logger.debug("Failed to change state: " + ex2.toString());
            }
        }
    }

    @Override
    public StrategyPriority canHandle(Snapshot snapshot, SnapshotOperation op) {
        if (SnapshotOperation.REVERT.equals(op)) {
            return StrategyPriority.CANT_HANDLE;
        }

        long volumeId = snapshot.getVolumeId();

        VolumeVO volumeVO = volumeDao.findByIdIncludingRemoved(volumeId);

        long storagePoolId = volumeVO.getPoolId();

        DataStore dataStore = dataStoreMgr.getDataStore(storagePoolId, DataStoreRole.Primary);

        Snapshot.LocationType locationType = snapshot.getLocationType();

        // If the snapshot exists on Secondary Storage, we can't delete it.
        if (SnapshotOperation.DELETE.equals(op) && Snapshot.LocationType.SECONDARY.equals(locationType)) {
            return StrategyPriority.CANT_HANDLE;
        }

        if (dataStore != null) {
            Map<String, String> mapCapabilities = dataStore.getDriver().getCapabilities();

            if (mapCapabilities != null) {
                String value = mapCapabilities.get(DataStoreCapabilities.STORAGE_SYSTEM_SNAPSHOT.toString());
                Boolean supportsStorageSystemSnapshots = Boolean.valueOf(value);

                if (supportsStorageSystemSnapshots) {
                    return StrategyPriority.HIGHEST;
                }
            }
        }

        return StrategyPriority.CANT_HANDLE;
    }

}
