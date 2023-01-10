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

import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import javax.inject.Inject;

import com.cloud.storage.VolumeDetailVO;
import org.apache.cloudstack.utils.reflectiontostringbuilderutils.ReflectionToStringBuilderUtils;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.BooleanUtils;
import org.apache.cloudstack.engine.subsystem.api.storage.DataStore;
import org.apache.cloudstack.engine.subsystem.api.storage.DataStoreManager;
import org.apache.cloudstack.engine.subsystem.api.storage.ObjectInDataStoreStateMachine.Event;
import org.apache.cloudstack.engine.subsystem.api.storage.ObjectInDataStoreStateMachine.State;
import org.apache.cloudstack.engine.subsystem.api.storage.SnapshotDataFactory;
import org.apache.cloudstack.engine.subsystem.api.storage.SnapshotInfo;
import org.apache.cloudstack.engine.subsystem.api.storage.SnapshotResult;
import org.apache.cloudstack.engine.subsystem.api.storage.SnapshotService;
import org.apache.cloudstack.engine.subsystem.api.storage.StrategyPriority;
import org.apache.cloudstack.engine.subsystem.api.storage.VolumeInfo;
import org.apache.cloudstack.framework.config.dao.ConfigurationDao;
import org.apache.cloudstack.framework.jobs.AsyncJob;
import org.apache.cloudstack.storage.command.CreateObjectAnswer;
import org.apache.cloudstack.storage.datastore.db.SnapshotDataStoreDao;
import org.apache.cloudstack.storage.datastore.db.SnapshotDataStoreVO;
import org.apache.cloudstack.storage.datastore.PrimaryDataStoreImpl;
import org.apache.cloudstack.storage.to.SnapshotObjectTO;
import org.apache.cloudstack.utils.identity.ManagementServerNode;

import com.cloud.agent.api.to.DataTO;
import com.cloud.event.EventTypes;
import com.cloud.event.UsageEventUtils;
import com.cloud.exception.InvalidParameterValueException;
import com.cloud.hypervisor.Hypervisor;
import com.cloud.hypervisor.Hypervisor.HypervisorType;
import com.cloud.storage.CreateSnapshotPayload;
import com.cloud.storage.DataStoreRole;
import com.cloud.storage.Snapshot;
import com.cloud.storage.SnapshotVO;
import com.cloud.storage.StoragePool;
import com.cloud.storage.StoragePoolStatus;
import com.cloud.storage.Volume;
import com.cloud.storage.VolumeVO;
import com.cloud.storage.dao.SnapshotDao;
import com.cloud.storage.dao.SnapshotDetailsDao;
import com.cloud.storage.dao.VolumeDao;
import com.cloud.storage.dao.VolumeDetailsDao;
import com.cloud.utils.db.Transaction;
import com.cloud.utils.db.TransactionCallbackNoReturn;
import com.cloud.utils.db.TransactionStatus;
import com.cloud.storage.snapshot.SnapshotManager;
import com.cloud.storage.Storage.ImageFormat;
import com.cloud.storage.Storage.StoragePoolType;
import com.cloud.utils.NumbersUtil;
import com.cloud.utils.db.DB;
import com.cloud.utils.exception.CloudRuntimeException;
import com.cloud.utils.fsm.NoTransitionException;

public class DefaultSnapshotStrategy extends SnapshotStrategyBase {
    private static final String SECONDARY_STORAGE_SNAPSHOT_ENTRY_IDENTIFIER = "secondary storage";
    private static final String PRIMARY_STORAGE_SNAPSHOT_ENTRY_IDENTIFIER = "primary storage";


    @Inject
    SnapshotService snapshotSvr;
    @Inject
    DataStoreManager dataStoreMgr;
    @Inject
    SnapshotDataStoreDao snapshotStoreDao;
    @Inject
    ConfigurationDao configDao;
    @Inject
    SnapshotDao snapshotDao;
    @Inject
    VolumeDao volumeDao;
    @Inject
    SnapshotDataFactory snapshotDataFactory;
    @Inject
    private SnapshotDetailsDao _snapshotDetailsDao;
    @Inject
    VolumeDetailsDao _volumeDetailsDaoImpl;

    @Override
    public SnapshotInfo backupSnapshot(SnapshotInfo snapshot) {
        SnapshotInfo parentSnapshot = snapshot.getParent();

        if (parentSnapshot != null && snapshot.getPath().equalsIgnoreCase(parentSnapshot.getPath())) {
            // don't need to backup this snapshot
            SnapshotDataStoreVO parentSnapshotOnBackupStore = snapshotStoreDao.findBySnapshot(parentSnapshot.getId(), DataStoreRole.Image);
            if (parentSnapshotOnBackupStore != null && parentSnapshotOnBackupStore.getState() == State.Ready) {
                DataStore store = dataStoreMgr.getDataStore(parentSnapshotOnBackupStore.getDataStoreId(), parentSnapshotOnBackupStore.getRole());

                SnapshotInfo snapshotOnImageStore = (SnapshotInfo)store.create(snapshot);
                snapshotOnImageStore.processEvent(Event.CreateOnlyRequested);

                SnapshotObjectTO snapTO = new SnapshotObjectTO();
                snapTO.setPath(parentSnapshotOnBackupStore.getInstallPath());

                CreateObjectAnswer createSnapshotAnswer = new CreateObjectAnswer(snapTO);

                snapshotOnImageStore.processEvent(Event.OperationSuccessed, createSnapshotAnswer);
                SnapshotObject snapObj = castSnapshotInfoToSnapshotObject(snapshot);
                try {
                    snapObj.processEvent(Snapshot.Event.OperationNotPerformed);
                } catch (NoTransitionException e) {
                    logger.debug("Failed to change state: " + snapshot.getId() + ": " + e.toString());
                    throw new CloudRuntimeException(e.toString());
                }
                return snapshotDataFactory.getSnapshot(snapObj.getId(), store);
            } else {
                logger.debug("parent snapshot hasn't been backed up yet");
            }
        }

        // determine full snapshot backup or not

        boolean fullBackup = true;
        SnapshotDataStoreVO parentSnapshotOnBackupStore = snapshotStoreDao.findLatestSnapshotForVolume(snapshot.getVolumeId(), DataStoreRole.Image);
        SnapshotDataStoreVO parentSnapshotOnPrimaryStore = snapshotStoreDao.findLatestSnapshotForVolume(snapshot.getVolumeId(), DataStoreRole.Primary);
        HypervisorType hypervisorType = snapshot.getBaseVolume().getHypervisorType();
        if (parentSnapshotOnPrimaryStore != null && parentSnapshotOnBackupStore != null && hypervisorType == Hypervisor.HypervisorType.XenServer) { // CS does incremental backup only for XenServer

            // In case of volume migration from one pool to other pool, CS should take full snapshot to avoid any issues with delta chain,
            // to check if this is a migrated volume, compare the current pool id of volume and store_id of oldest snapshot on primary for this volume.
            // Why oldest? Because at this point CS has two snapshot on primary entries for same volume, one with old pool_id and other one with
            // current pool id. So, verify and if volume found to be migrated, delete snapshot entry with previous pool store_id.
            SnapshotDataStoreVO oldestSnapshotOnPrimary = snapshotStoreDao.findOldestSnapshotForVolume(snapshot.getVolumeId(), DataStoreRole.Primary);
            VolumeVO volume = volumeDao.findById(snapshot.getVolumeId());
            if (oldestSnapshotOnPrimary != null) {
                if (oldestSnapshotOnPrimary.getDataStoreId() == volume.getPoolId() && oldestSnapshotOnPrimary.getId() != parentSnapshotOnPrimaryStore.getId()) {
                    int _deltaSnapshotMax = NumbersUtil.parseInt(configDao.getValue("snapshot.delta.max"),
                            SnapshotManager.DELTAMAX);
                    int deltaSnap = _deltaSnapshotMax;
                    int i;

                    for (i = 1; i < deltaSnap; i++) {
                        Long prevBackupId = parentSnapshotOnBackupStore.getParentSnapshotId();
                        if (prevBackupId == 0) {
                            break;
                        }
                        parentSnapshotOnBackupStore = snapshotStoreDao.findBySnapshot(prevBackupId, DataStoreRole.Image);
                        if (parentSnapshotOnBackupStore == null) {
                            break;
                        }
                    }

                    if (i >= deltaSnap) {
                        fullBackup = true;
                    } else {
                        fullBackup = false;
                    }
                } else if (oldestSnapshotOnPrimary.getId() != parentSnapshotOnPrimaryStore.getId()){
                    // if there is an snapshot entry for previousPool(primary storage) of migrated volume, delete it becasue CS created one more snapshot entry for current pool
                    snapshotStoreDao.remove(oldestSnapshotOnPrimary.getId());
                }
            }
        }

        snapshot.setFullBackup(fullBackup);
        return snapshotSvr.backupSnapshot(snapshot);
    }

    private final List<Snapshot.State> snapshotStatesAbleToDeleteSnapshot = Arrays.asList(Snapshot.State.Destroying, Snapshot.State.Destroyed, Snapshot.State.Error);

    protected boolean deleteSnapshotChain(SnapshotInfo snapshot, String storage) {
        DataTO snapshotTo = snapshot.getTO();
        logger.debug(String.format("Deleting %s chain of snapshots.", snapshotTo));

        boolean result = false;
        boolean resultIsSet = false;
        try {
            while (snapshot != null && snapshotStatesAbleToDeleteSnapshot.contains(snapshot.getState())) {
                SnapshotInfo child = snapshot.getChild();

                if (child != null) {
                    logger.debug(String.format("Snapshot [%s] has child [%s], not deleting it on the storage [%s]", snapshotTo, child.getTO(), storage));
                    break;
                }

                logger.debug(String.format("Snapshot [%s] does not have children; therefore, we will delete it and its parents.", snapshotTo));

                SnapshotInfo parent = snapshot.getParent();
                boolean deleted = false;
                if (parent != null) {
                    if (parent.getPath() != null && parent.getPath().equalsIgnoreCase(snapshot.getPath())) {
                        //NOTE: if both snapshots share the same path, it's for xenserver's empty delta snapshot. We can't delete the snapshot on the backend, as parent snapshot still reference to it
                        //Instead, mark it as destroyed in the db.
                        logger.debug(String.format("Snapshot [%s] is an empty delta snapshot; therefore, we will only mark it as destroyed in the database.", snapshotTo));
                        snapshot.processEvent(Event.DestroyRequested);
                        snapshot.processEvent(Event.OperationSuccessed);
                        deleted = true;
                        if (!resultIsSet) {
                            result = true;
                            resultIsSet = true;
                        }
                    }
                }

                if (!deleted) {
                    try {
                        boolean r = snapshotSvr.deleteSnapshot(snapshot);
                        if (r) {
                            List<SnapshotInfo> cacheSnaps = snapshotDataFactory.listSnapshotOnCache(snapshot.getId());
                            for (SnapshotInfo cacheSnap : cacheSnaps) {
                                logger.debug(String.format("Deleting snapshot %s from image cache [%s].", snapshotTo, cacheSnap.getDataStore().getName()));
                                cacheSnap.delete();
                            }
                        }

                        if (!resultIsSet) {
                            result = r;
                            resultIsSet = true;
                        }
                    } catch (Exception e) {
                        logger.error(String.format("Failed to delete snapshot [%s] on storage [%s] due to [%s].", snapshotTo, storage, e.getMessage()), e);
                    }
                }

                snapshot = parent;
            }
        } catch (Exception e) {
            logger.error(String.format("Failed to delete snapshot [%s] on storage [%s] due to [%s].", snapshotTo, storage, e.getMessage()), e);
        }
        return result;
    }

    @Override
    public boolean deleteSnapshot(Long snapshotId) {
        SnapshotVO snapshotVO = snapshotDao.findById(snapshotId);

        if (snapshotVO.getState() == Snapshot.State.Allocated) {
            snapshotDao.remove(snapshotId);
            return true;
        }

        if (snapshotVO.getState() == Snapshot.State.Destroyed) {
            return true;
        }

        if (Snapshot.State.Error.equals(snapshotVO.getState())) {
            List<SnapshotDataStoreVO> storeRefs = snapshotStoreDao.findBySnapshotId(snapshotId);
            for (SnapshotDataStoreVO ref : storeRefs) {
                snapshotStoreDao.expunge(ref.getId());
            }
            snapshotDao.remove(snapshotId);
            return true;
        }

        if (snapshotVO.getState() == Snapshot.State.CreatedOnPrimary) {
            snapshotVO.setState(Snapshot.State.Destroyed);
            snapshotDao.update(snapshotId, snapshotVO);
            return true;
        }

        if (!Snapshot.State.BackedUp.equals(snapshotVO.getState()) &&
                !Snapshot.State.Destroying.equals(snapshotVO.getState())) {
            throw new InvalidParameterValueException("Can't delete snapshotshot " + snapshotId + " due to it is in " + snapshotVO.getState() + " Status");
        }

        return destroySnapshotEntriesAndFiles(snapshotVO);
    }

    /**
     * Destroys the snapshot entries and files on both primary and secondary storage (if it exists).
     * @return true if destroy successfully, else false.
     */
    protected boolean destroySnapshotEntriesAndFiles(SnapshotVO snapshotVo) {
        if (!deleteSnapshotInfos(snapshotVo)) {
            return false;
        }

        updateSnapshotToDestroyed(snapshotVo);

        return true;
    }

    /**
     * Updates the snapshot to {@link Snapshot.State#Destroyed}.
     */
    protected void updateSnapshotToDestroyed(SnapshotVO snapshotVo) {
        snapshotVo.setState(Snapshot.State.Destroyed);
        snapshotDao.update(snapshotVo.getId(), snapshotVo);
    }

    protected boolean deleteSnapshotInfos(SnapshotVO snapshotVo) {
        Map<String, SnapshotInfo> snapshotInfos = retrieveSnapshotEntries(snapshotVo.getId());

        boolean result = false;
        for (var infoEntry : snapshotInfos.entrySet()) {
            if (BooleanUtils.toBooleanDefaultIfNull(deleteSnapshotInfo(infoEntry.getValue(), infoEntry.getKey(), snapshotVo), false)) {
                result = true;
            }
        }

        return result;
    }

    /**
     * Destroys the snapshot entry and file.
     * @return true if destroy successfully, else false.
     */
    protected Boolean deleteSnapshotInfo(SnapshotInfo snapshotInfo, String storage, SnapshotVO snapshotVo) {
        if (snapshotInfo == null) {
            logger.debug(String.format("Could not find %s entry on %s. Skipping deletion on %s.", snapshotVo, storage, storage));
            return SECONDARY_STORAGE_SNAPSHOT_ENTRY_IDENTIFIER.equals(storage) ? null : true;
        }

        DataStore dataStore = snapshotInfo.getDataStore();
        String storageToString = String.format("%s {uuid: \"%s\", name: \"%s\"}", storage, dataStore.getUuid(), dataStore.getName());

        try {
            SnapshotObject snapshotObject = castSnapshotInfoToSnapshotObject(snapshotInfo);
            snapshotObject.processEvent(Snapshot.Event.DestroyRequested);

            if (SECONDARY_STORAGE_SNAPSHOT_ENTRY_IDENTIFIER.equals(storage)) {

                verifyIfTheSnapshotIsBeingUsedByAnyVolume(snapshotObject);

                if (deleteSnapshotChain(snapshotInfo, storageToString)) {
                    logger.debug(String.format("%s was deleted on %s. We will mark the snapshot as destroyed.", snapshotVo, storageToString));
                } else {
                    logger.debug(String.format("%s was not deleted on %s; however, we will mark the snapshot as destroyed for future garbage collecting.", snapshotVo,
                        storageToString));
                }

                snapshotObject.processEvent(Snapshot.Event.OperationSucceeded);
                return true;
            } else if (deleteSnapshotInPrimaryStorage(snapshotInfo, snapshotVo, storageToString, snapshotObject)) {
                return true;
            }

            logger.debug(String.format("Failed to delete %s on %s.", snapshotVo, storageToString));
            snapshotObject.processEvent(Snapshot.Event.OperationFailed);
        } catch (NoTransitionException ex) {
            logger.warn(String.format("Failed to delete %s on %s due to %s.", snapshotVo, storageToString, ex.getMessage()), ex);
        }

        return false;
    }

    protected boolean deleteSnapshotInPrimaryStorage(SnapshotInfo snapshotInfo, SnapshotVO snapshotVo, String storageToString, SnapshotObject snapshotObject) throws NoTransitionException {
        try {
            if (snapshotSvr.deleteSnapshot(snapshotInfo)) {
                snapshotObject.processEvent(Snapshot.Event.OperationSucceeded);
                logger.debug(String.format("%s was deleted on %s. We will mark the snapshot as destroyed.", snapshotVo, storageToString));
                return true;
            }
        } catch (CloudRuntimeException ex) {
            logger.warn(String.format("Unable do delete snapshot %s on %s due to [%s]. The reference will be marked as 'Destroying' for future garbage collecting.",
                    snapshotVo, storageToString, ex.getMessage()), ex);
        }
        return false;
    }

    protected void verifyIfTheSnapshotIsBeingUsedByAnyVolume(SnapshotObject snapshotObject) throws NoTransitionException {
        List<VolumeDetailVO> volumesFromSnapshot = _volumeDetailsDaoImpl.findDetails("SNAPSHOT_ID", String.valueOf(snapshotObject.getSnapshotId()), null);
        if (CollectionUtils.isEmpty(volumesFromSnapshot)) {
            return;
        }

        snapshotObject.processEvent(Snapshot.Event.OperationFailed);
        throw new CloudRuntimeException(String.format("Unable to delete snapshot [%s] because it is being used by the following volumes: %s.",
            ReflectionToStringBuilderUtils.reflectOnlySelectedFields(snapshotObject.getSnapshotVO(), "id", "uuid", "volumeId", "name"),
            ReflectionToStringBuilderUtils.reflectOnlySelectedFields(volumesFromSnapshot, "resourceId")));
    }

    /**
     * Cast SnapshotInfo to SnapshotObject.
     * @return SnapshotInfo cast to SnapshotObject.
     */
    protected SnapshotObject castSnapshotInfoToSnapshotObject(SnapshotInfo snapshotInfo) {
        return (SnapshotObject) snapshotInfo;
    }

    /**
     * Retrieves the snapshot infos on primary and secondary storage.
     * @param snapshotId The snapshot to retrieve the infos.
     * @return A map of snapshot infos.
     */
    protected Map<String, SnapshotInfo> retrieveSnapshotEntries(long snapshotId) {
        Map<String, SnapshotInfo> snapshotInfos = new LinkedHashMap<>();
        snapshotInfos.put(SECONDARY_STORAGE_SNAPSHOT_ENTRY_IDENTIFIER, snapshotDataFactory.getSnapshot(snapshotId, DataStoreRole.Image, false));
        snapshotInfos.put(PRIMARY_STORAGE_SNAPSHOT_ENTRY_IDENTIFIER, snapshotDataFactory.getSnapshot(snapshotId, DataStoreRole.Primary, false));
        return snapshotInfos;
    }

    @Override
    public boolean revertSnapshot(SnapshotInfo snapshot) {
        if (canHandle(snapshot, SnapshotOperation.REVERT) == StrategyPriority.CANT_HANDLE) {
            throw new CloudRuntimeException("Reverting not supported. Create a template or volume based on the snapshot instead.");
        }

        SnapshotVO snapshotVO = snapshotDao.acquireInLockTable(snapshot.getId());

        if (snapshotVO == null) {
            throw new CloudRuntimeException("Failed to get lock on snapshot:" + snapshot.getId());
        }

        try {
            VolumeInfo volumeInfo = snapshot.getBaseVolume();
            StoragePool store = (StoragePool)volumeInfo.getDataStore();

            if (store != null && store.getStatus() != StoragePoolStatus.Up) {
                snapshot.processEvent(Event.OperationFailed);

                throw new CloudRuntimeException("store is not in up state");
            }

            volumeInfo.stateTransit(Volume.Event.RevertSnapshotRequested);

            boolean result = false;

            try {
                result =  snapshotSvr.revertSnapshot(snapshot);

                if (!result) {
                    logger.debug("Failed to revert snapshot: " + snapshot.getId());

                    throw new CloudRuntimeException("Failed to revert snapshot: " + snapshot.getId());
                }
            } finally {
                if (result) {
                    volumeInfo.stateTransit(Volume.Event.OperationSucceeded);
                } else {
                    volumeInfo.stateTransit(Volume.Event.OperationFailed);
                }
            }

            return result;
        } finally {
            if (snapshotVO != null) {
                snapshotDao.releaseFromLockTable(snapshot.getId());
            }
        }
    }

    @Override
    @DB
    public SnapshotInfo takeSnapshot(SnapshotInfo snapshot) {
        SnapshotInfo snapshotOnPrimary = null;
        Object payload = snapshot.getPayload();
        CreateSnapshotPayload createSnapshotPayload = null;
        if (payload != null) {
            createSnapshotPayload = (CreateSnapshotPayload)payload;
            if (createSnapshotPayload.getQuiescevm()) {
                throw new InvalidParameterValueException("can't handle quiescevm equal true for volume snapshot");
            }
        }

        SnapshotVO snapshotVO = snapshotDao.acquireInLockTable(snapshot.getId());
        if (snapshotVO == null) {
            throw new CloudRuntimeException("Failed to get lock on snapshot:" + snapshot.getId());
        }

        try {
            VolumeInfo volumeInfo = snapshot.getBaseVolume();
            volumeInfo.stateTransit(Volume.Event.SnapshotRequested);
            SnapshotResult result = null;
            try {
                result = snapshotSvr.takeSnapshot(snapshot);
                if (result.isFailed()) {
                    logger.debug("Failed to take snapshot: " + result.getResult());
                    throw new CloudRuntimeException(result.getResult());
                }
            } finally {
                if (result != null && result.isSuccess()) {
                    volumeInfo.stateTransit(Volume.Event.OperationSucceeded);
                } else {
                    volumeInfo.stateTransit(Volume.Event.OperationFailed);
                }
            }
            snapshotOnPrimary = result.getSnapshot();
            snapshotOnPrimary.addPayload(snapshot.getPayload());

            /*The Management Server ID is stored in snapshot_details table with the snapshot id and (name, value): (MS_ID, <ms_id>), to know which snapshots have not been completed in case of some failure situation like
             *  Mgmt server down etc. and by fetching the entries on restart the cleaning up of failed snapshots is done*/
            _snapshotDetailsDao.addDetail(castSnapshotInfoToSnapshotObject(snapshotOnPrimary).getId(), AsyncJob.Constants.MS_ID, Long.toString(ManagementServerNode.getManagementServerId()), false);
            return snapshotOnPrimary;
        } finally {
            if (snapshotVO != null) {
                snapshotDao.releaseFromLockTable(snapshot.getId());
            }
        }
    }

    @Override
    public void postSnapshotCreation(SnapshotInfo snapshotOnPrimary) {
        Transaction.execute(new TransactionCallbackNoReturn() {
            @Override
            public void doInTransactionWithoutResult(TransactionStatus status) {
                _snapshotDetailsDao.removeDetail(castSnapshotInfoToSnapshotObject(snapshotOnPrimary).getId(), AsyncJob.Constants.MS_ID);
                DataStore primaryStore = snapshotOnPrimary.getDataStore();
                try {
                    SnapshotInfo parent = snapshotOnPrimary.getParent();
                    if (parent != null && primaryStore instanceof PrimaryDataStoreImpl) {
                        if (((PrimaryDataStoreImpl)primaryStore).getPoolType() != StoragePoolType.RBD) {
                            Long parentSnapshotId = parent.getId();
                            while (parentSnapshotId != null && parentSnapshotId != 0L) {
                                SnapshotDataStoreVO snapshotDataStoreVO = snapshotStoreDao.findByStoreSnapshot(primaryStore.getRole(), primaryStore.getId(), parentSnapshotId);
                                if (snapshotDataStoreVO != null) {
                                    parentSnapshotId = snapshotDataStoreVO.getParentSnapshotId();
                                    UsageEventUtils.publishUsageEvent(EventTypes.EVENT_SNAPSHOT_OFF_PRIMARY, parent.getAccountId(), parent.getDataCenterId(), parent.getId(),
                                            parent.getName(), null, null, 0L, 0L, parent.getClass().getName(), parent.getUuid());
                                    snapshotStoreDao.remove(snapshotDataStoreVO.getId());
                                } else {
                                    parentSnapshotId = null;
                                }
                            }
                        }
                        SnapshotDataStoreVO snapshotDataStoreVO = snapshotStoreDao.findByStoreSnapshot(primaryStore.getRole(), primaryStore.getId(), snapshotOnPrimary.getId());
                        if (snapshotDataStoreVO != null) {
                            snapshotDataStoreVO.setParentSnapshotId(0L);
                            snapshotStoreDao.update(snapshotDataStoreVO.getId(), snapshotDataStoreVO);
                        }
                    }
                } catch (Exception e) {
                    logger.debug("Failed to clean up snapshots on primary storage", e);
                }
            }
        });
    }

    @Override
    public StrategyPriority canHandle(Snapshot snapshot, SnapshotOperation op) {
        if (SnapshotOperation.REVERT.equals(op)) {
            long volumeId = snapshot.getVolumeId();
            VolumeVO volumeVO = volumeDao.findById(volumeId);

            if (volumeVO != null && ImageFormat.QCOW2.equals(volumeVO.getFormat())) {
                return StrategyPriority.DEFAULT;
            }

            return StrategyPriority.CANT_HANDLE;
        }

        return StrategyPriority.DEFAULT;
    }

}
