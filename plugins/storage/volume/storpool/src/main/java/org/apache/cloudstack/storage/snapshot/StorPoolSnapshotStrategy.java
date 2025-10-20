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

import com.cloud.api.query.dao.SnapshotJoinDao;
import com.cloud.api.query.vo.SnapshotJoinVO;
import com.cloud.dc.dao.ClusterDao;
import com.cloud.exception.InvalidParameterValueException;
import com.cloud.hypervisor.kvm.storage.StorPoolStorageAdaptor;
import com.cloud.storage.DataStoreRole;
import com.cloud.storage.Snapshot;
import com.cloud.storage.SnapshotVO;
import com.cloud.storage.Storage;
import com.cloud.storage.dao.SnapshotDao;
import com.cloud.storage.dao.SnapshotDetailsDao;
import com.cloud.storage.dao.SnapshotDetailsVO;
import com.cloud.storage.dao.SnapshotZoneDao;
import com.cloud.storage.dao.VolumeDao;
import com.cloud.utils.exception.CloudRuntimeException;
import com.cloud.utils.fsm.NoTransitionException;

import java.util.HashMap;
import java.util.Map;
import org.apache.cloudstack.engine.subsystem.api.storage.CreateCmdResult;
import org.apache.cloudstack.engine.subsystem.api.storage.DataObject;
import org.apache.cloudstack.engine.subsystem.api.storage.DataStore;
import org.apache.cloudstack.engine.subsystem.api.storage.DataStoreManager;
import org.apache.cloudstack.engine.subsystem.api.storage.ObjectInDataStoreStateMachine;
import org.apache.cloudstack.engine.subsystem.api.storage.ObjectInDataStoreStateMachine.Event;
import org.apache.cloudstack.engine.subsystem.api.storage.ObjectInDataStoreStateMachine.State;
import org.apache.cloudstack.engine.subsystem.api.storage.SnapshotDataFactory;
import org.apache.cloudstack.engine.subsystem.api.storage.SnapshotInfo;
import org.apache.cloudstack.engine.subsystem.api.storage.SnapshotService;
import org.apache.cloudstack.engine.subsystem.api.storage.SnapshotStrategy;
import org.apache.cloudstack.engine.subsystem.api.storage.StrategyPriority;
import org.apache.cloudstack.framework.async.AsyncCompletionCallback;
import org.apache.cloudstack.storage.datastore.db.PrimaryDataStoreDao;
import org.apache.cloudstack.storage.datastore.db.SnapshotDataStoreDao;
import org.apache.cloudstack.storage.datastore.db.SnapshotDataStoreVO;
import org.apache.cloudstack.storage.datastore.db.StoragePoolDetailVO;
import org.apache.cloudstack.storage.datastore.db.StoragePoolDetailsDao;
import org.apache.cloudstack.storage.datastore.db.StoragePoolVO;
import org.apache.cloudstack.storage.datastore.util.StorPoolHelper;
import org.apache.cloudstack.storage.datastore.util.StorPoolUtil;
import org.apache.cloudstack.storage.datastore.util.StorPoolUtil.SpApiResponse;
import org.apache.cloudstack.storage.datastore.util.StorPoolUtil.SpConnectionDesc;
import org.apache.cloudstack.storage.to.SnapshotObjectTO;

import org.apache.commons.collections.CollectionUtils;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import org.jetbrains.annotations.NotNull;
import org.springframework.stereotype.Component;

import javax.inject.Inject;
import java.util.ArrayList;
import java.util.List;


@Component
public class StorPoolSnapshotStrategy implements SnapshotStrategy {
    protected Logger logger = LogManager.getLogger(getClass());

    @Inject
    private SnapshotDao _snapshotDao;
    @Inject
    private PrimaryDataStoreDao _primaryDataStoreDao;
    @Inject
    private VolumeDao _volumeDao;
    @Inject
    private SnapshotDataStoreDao _snapshotStoreDao;
    @Inject
    private SnapshotDetailsDao _snapshotDetailsDao;
    @Inject
    private SnapshotService snapshotSvr;
    @Inject
    private SnapshotDataFactory snapshotDataFactory;
    @Inject
    private StoragePoolDetailsDao storagePoolDetailsDao;
    @Inject
    DataStoreManager dataStoreMgr;
    @Inject
    SnapshotZoneDao snapshotZoneDao;
    @Inject
    SnapshotJoinDao snapshotJoinDao;
    @Inject
    private ClusterDao clusterDao;

    @Override
    public SnapshotInfo backupSnapshot(SnapshotInfo snapshotInfo) {
        SnapshotObject snapshotObj = (SnapshotObject) snapshotInfo;
        try {
            snapshotObj.processEvent(Snapshot.Event.BackupToSecondary);
            snapshotObj.processEvent(Snapshot.Event.OperationSucceeded);
        } catch (NoTransitionException ex) {
            logger.debug("Failed to change state: " + ex.toString());
            try {
                snapshotObj.processEvent(Snapshot.Event.OperationFailed);
            } catch (NoTransitionException ex2) {
                logger.debug("Failed to change state: " + ex2.toString());
            }
        }
        return snapshotInfo;
    }

    @Override
    public boolean deleteSnapshot(Long snapshotId, Long zoneId) {

        final SnapshotVO snapshotVO = _snapshotDao.findById(snapshotId);
        String name = StorPoolHelper.getSnapshotName(snapshotId, snapshotVO.getUuid(), _snapshotStoreDao, _snapshotDetailsDao);
        boolean res = false;
        // clean-up snapshot from Storpool storage pools
        List<SnapshotDataStoreVO> snapshotDataStoreVOS;
        List<SnapshotJoinVO> snapshotJoinVOList = snapshotJoinDao.listBySnapshotIdAndZoneId(zoneId, snapshotId);
        try {
            for (SnapshotJoinVO snapshot: snapshotJoinVOList) {
                if (State.Destroyed.equals(snapshot.getStatus())) {
                    continue;
                }
                if (snapshot.getStoreRole().isImageStore()) {
                    continue;
                }
                StoragePoolVO storage = _primaryDataStoreDao.findById(snapshot.getStoreId());
                if (zoneId != null) {
                    if (!zoneId.equals(snapshot.getDataCenterId())) {
                        continue;
                    }
                    res = deleteSnapshot(snapshotId, zoneId, snapshotVO, name, storage);
                    break;
                }
                res = deleteSnapshot(snapshotId, zoneId, snapshotVO, name, storage);
            }
        } catch (Exception e) {
            String errMsg = String.format("Cannot delete snapshot due to %s", e.getMessage());
            throw new CloudRuntimeException(errMsg);
        }
        snapshotDataStoreVOS = _snapshotStoreDao.listSnapshotsBySnapshotId(snapshotId);
        boolean areAllSnapshotsDestroyed = snapshotDataStoreVOS.stream().allMatch(v -> v.getState().equals(State.Destroyed) || v.getState().equals(State.Destroying));
        if (areAllSnapshotsDestroyed) {
            updateSnapshotToDestroyed(snapshotVO);
            return true;
        }
        return res;
    }

    private boolean deleteSnapshot(Long snapshotId, Long zoneId, SnapshotVO snapshotVO, String name, StoragePoolVO storage) {

        boolean res = false;
        SpConnectionDesc conn = StorPoolUtil.getSpConnection(storage.getUuid(), storage.getId(), storagePoolDetailsDao, _primaryDataStoreDao);
        SpApiResponse resp = StorPoolUtil.snapshotDelete(name, conn);
        List<SnapshotInfo> snapshotInfos = snapshotDataFactory.getSnapshots(snapshotId, zoneId);
        processResult(snapshotInfos, ObjectInDataStoreStateMachine.Event.DestroyRequested);
        if (resp.getError() != null) {
            if (resp.getError().getDescr().contains("still exported")) {
                processResult(snapshotInfos, Event.OperationFailed);
                throw new CloudRuntimeException(String.format("The snapshot [%s] was exported to another cluster. [%s]", name, resp.getError()));
            }
            final String err = String.format("Failed to clean-up Storpool snapshot %s. Error: %s", name, resp.getError());
            StorPoolUtil.spLog(err);
            if (resp.getError().getName().equals("objectDoesNotExist")) {
                return true;
            }
        } else {
            res = deleteSnapshotFromDbIfNeeded(snapshotVO, zoneId);
            StorPoolUtil.spLog("StorpoolSnapshotStrategy.deleteSnapshot: executed successfully=%s, snapshot uuid=%s, name=%s", res, snapshotVO.getUuid(), name);
        }
        if (res) {
            processResult(snapshotInfos, Event.OperationSuccessed);
            cleanUpDestroyedRecords(snapshotId);
        } else {
            processResult(snapshotInfos, Event.OperationFailed);
        }
        return res;
    }

    private void cleanUpDestroyedRecords(Long snapshotId) {
        List<SnapshotDataStoreVO> snapshots = _snapshotStoreDao.listBySnapshotId(snapshotId);
        for (SnapshotDataStoreVO snapshot : snapshots) {
            if (snapshot.getInstallPath().contains("/dev/storpool-byid") && State.Destroyed.equals(snapshot.getState())) {
                _snapshotStoreDao.remove(snapshot.getId());
            }
        }
    }

    private void processResult(List<SnapshotInfo> snapshotInfos, ObjectInDataStoreStateMachine.Event event) {
        for (SnapshotInfo snapshot : snapshotInfos) {
            SnapshotObject snapshotObject = (SnapshotObject) snapshot;
            if (DataStoreRole.Primary.equals(snapshotObject.getDataStore().getRole())) {
                snapshotObject.processEvent(event);
            }
        }
    }

    @Override
    public StrategyPriority canHandle(Snapshot snapshot, Long zoneId, SnapshotOperation op) {
        logger.debug("StorpoolSnapshotStrategy.canHandle: snapshot {}, op={}", snapshot, op);

        if (op != SnapshotOperation.DELETE && op != SnapshotOperation.COPY) {
            return StrategyPriority.CANT_HANDLE;
        }
        List<StoragePoolVO> pools = _primaryDataStoreDao.findPoolsByStorageType(Storage.StoragePoolType.StorPool);
        if (CollectionUtils.isEmpty(pools)) {
            return StrategyPriority.CANT_HANDLE;
        }
        List<SnapshotJoinVO> snapshots = snapshotJoinDao.listBySnapshotIdAndZoneId(zoneId, snapshot.getId());
        boolean snapshotNotOnStorPool = snapshots.stream().filter(s -> DataStoreRole.Primary.equals(s.getStoreRole())).count() == 0;

        if (snapshotNotOnStorPool) {
            for (SnapshotJoinVO snapshotOnStore : snapshots) {
                SnapshotDataStoreVO snap = _snapshotStoreDao.findOneBySnapshotAndDatastoreRole(snapshot.getId(), DataStoreRole.Image);
                if (snap != null && snap.getInstallPath() != null && snap.getInstallPath().startsWith(StorPoolUtil.SP_DEV_PATH)) {
                    return StrategyPriority.HIGHEST;
                }
            }
            return StrategyPriority.CANT_HANDLE;
        }
        for (StoragePoolVO pool : pools) {
            SnapshotDataStoreVO snapshotOnPrimary = _snapshotStoreDao.findByStoreSnapshot(DataStoreRole.Primary, pool.getId(), snapshot.getId());
            if (snapshotOnPrimary != null && (snapshotOnPrimary.getState().equals(State.Ready) || snapshotOnPrimary.getState().equals(State.Created))) {
                return StrategyPriority.HIGHEST;
            }
        }

        return StrategyPriority.CANT_HANDLE;
    }

    private boolean deleteSnapshotChain(SnapshotInfo snapshot) {
        logger.debug("delete snapshot chain for snapshot: {}", snapshot);
        final SnapshotInfo snapOnImage = snapshot;
        boolean result = false;
        boolean resultIsSet = false;
        try {
            while (snapshot != null &&
                (snapshot.getState() == Snapshot.State.Destroying || snapshot.getState() == Snapshot.State.Destroyed || snapshot.getState() == Snapshot.State.Error || snapshot.getState() == Snapshot.State.BackedUp)) {
                SnapshotInfo child = snapshot.getChild();

                if (child != null) {
                    logger.debug("the snapshot has child, can't delete it on the storage");
                    break;
                }
                logger.debug("Snapshot: {} doesn't have children, so it's ok to delete it and its parents", snapshot);
                SnapshotInfo parent = snapshot.getParent();
                boolean deleted = false;
                if (parent != null) {
                    if (parent.getPath() != null && parent.getPath().equalsIgnoreCase(snapshot.getPath())) {
                        logger.debug("for empty delta snapshot, only mark it as destroyed in db");
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
                    if (StorPoolStorageAdaptor.getVolumeNameFromPath(snapOnImage.getPath(), true) == null) {
                        try {
                            boolean r = snapshotSvr.deleteSnapshot(snapshot);
                            if (r) {
                                List<SnapshotInfo> cacheSnaps = snapshotDataFactory.listSnapshotOnCache(snapshot.getId());
                                for (SnapshotInfo cacheSnap : cacheSnaps) {
                                    logger.debug("Delete snapshot {} from image cache store: {}", snapshot, cacheSnap.getDataStore());
                                    cacheSnap.delete();
                                }
                            }
                            if (!resultIsSet) {
                                result = r;
                                resultIsSet = true;
                            }
                        } catch (Exception e) {
                            logger.debug("Failed to delete snapshot on storage. ", e);
                        }
                    }
                } else {
                    result = true;
                }
                snapshot = parent;
            }
        } catch (Exception e) {
            logger.debug("delete snapshot failed: ", e);
        }
        return result;
    }

    protected boolean areLastSnapshotRef(long snapshotId) {
        List<SnapshotDataStoreVO> snapshotStoreRefs = _snapshotStoreDao.findBySnapshotId(snapshotId);
        if (CollectionUtils.isEmpty(snapshotStoreRefs) || snapshotStoreRefs.size() == 1) {
            return true;
        }
        return snapshotStoreRefs.size() == 2 && DataStoreRole.Primary.equals(snapshotStoreRefs.get(1).getRole());
    }

    protected boolean deleteSnapshotOnImageAndPrimary(long snapshotId, DataStore store) {
        SnapshotInfo snapshotOnImage = snapshotDataFactory.getSnapshot(snapshotId, store);
        SnapshotObject obj = (SnapshotObject)snapshotOnImage;

        boolean result = false;
        try {
            result = deleteSnapshotChain(snapshotOnImage);
            _snapshotStoreDao.updateDisplayForSnapshotStoreRole(snapshotId, store.getId(), store.getRole(), false);
        } catch (Exception e) {
            logger.debug("Failed to delete snapshot: ", e);
            return false;
        }
        return result;
    }

    private boolean deleteSnapshotFromDbIfNeeded(SnapshotVO snapshotVO, Long zoneId) {
        final long snapshotId = snapshotVO.getId();
        SnapshotDetailsVO snapshotDetails = _snapshotDetailsDao.findDetail(snapshotId, snapshotVO.getUuid());
        if (snapshotDetails != null) {
            _snapshotDetailsDao.remove(snapshotId);
        }

        if (zoneId != null && List.of(Snapshot.State.Allocated, Snapshot.State.CreatedOnPrimary).contains(snapshotVO.getState())) {
            throw new InvalidParameterValueException(String.format("Snapshot in %s can not be deleted for a zone", snapshotVO.getState()));
        }

        if (snapshotVO.getState() == Snapshot.State.Allocated) {
            _snapshotDao.remove(snapshotId);
            return true;
        }

        if (snapshotVO.getState() == Snapshot.State.Destroyed) {
            return true;
        }

        if (Snapshot.State.Error.equals(snapshotVO.getState())) {
            List<SnapshotDataStoreVO> storeRefs = _snapshotStoreDao.findBySnapshotId(snapshotId);
            List<Long> deletedRefs = new ArrayList<>();
            for (SnapshotDataStoreVO ref : storeRefs) {
                boolean refZoneIdMatch = false;
                if (zoneId != null) {
                    Long refZoneId = dataStoreMgr.getStoreZoneId(ref.getDataStoreId(), ref.getRole());
                    refZoneIdMatch = zoneId.equals(refZoneId);
                }
                if (zoneId == null || refZoneIdMatch) {
                    _snapshotStoreDao.expunge(ref.getId());
                    deletedRefs.add(ref.getId());
                }
            }
            if (deletedRefs.size() == storeRefs.size()) {
                _snapshotDao.remove(snapshotId);
            }
            return true;
        }

        if (!Snapshot.State.BackedUp.equals(snapshotVO.getState()) && !Snapshot.State.Error.equals(snapshotVO.getState()) &&
                !Snapshot.State.Destroying.equals(snapshotVO.getState())) {
            throw new InvalidParameterValueException(String.format("Can't delete snapshot %s due to it is in %s Status", snapshotVO, snapshotVO.getState()));
        }
        List<SnapshotDataStoreVO> storeRefs = _snapshotStoreDao.listBySnapshotAndDataStoreRole(snapshotId, DataStoreRole.Image);
        if (zoneId != null) {
            storeRefs.removeIf(ref -> !zoneId.equals(dataStoreMgr.getStoreZoneId(ref.getDataStoreId(), ref.getRole())));
        } else {
            storeRefs.removeIf(ref -> !ref.getState().equals(State.Ready));
        }
        for (SnapshotDataStoreVO ref : storeRefs) {
            if (!deleteSnapshotOnImageAndPrimary(snapshotId, dataStoreMgr.getDataStore(ref.getDataStoreId(), ref.getRole()))) {
                return false;
            }
        }
        if (zoneId != null) {
            snapshotZoneDao.removeSnapshotFromZone(snapshotVO.getId(), zoneId);
        } else {
            snapshotZoneDao.removeSnapshotFromZones(snapshotVO.getId());
        }
        if (CollectionUtils.isNotEmpty(retrieveSnapshotEntries(snapshotId, null))) {
            return true;
        }
        return true;
    }

    private List<SnapshotInfo> retrieveSnapshotEntries(long snapshotId, Long zoneId) {
        return snapshotDataFactory.getSnapshots(snapshotId, zoneId);
    }

    private void updateSnapshotToDestroyed(SnapshotVO snapshotVo) {
        snapshotVo.setState(Snapshot.State.Destroyed);
        _snapshotDao.update(snapshotVo.getId(), snapshotVo);
    }

    @Override
    public SnapshotInfo takeSnapshot(SnapshotInfo snapshot) {
        return null;
    }

    @Override
    public boolean revertSnapshot(SnapshotInfo snapshot) {
        return false;
    }

    @Override
    public void postSnapshotCreation(SnapshotInfo snapshot) {
    }

    @Override
    public void copySnapshot(DataObject snapshot, DataObject snapshotDest, AsyncCompletionCallback<CreateCmdResult> callback) {
        // export snapshot on remote
        StoragePoolVO storagePoolVO = _primaryDataStoreDao.findById(snapshotDest.getDataStore().getId());
        String location = StorPoolConfigurationManager.StorPoolClusterLocation.valueIn(snapshotDest.getDataStore().getId());
        StorPoolUtil.spLog("StorpoolSnapshotStrategy.copySnapshot: snapshot %s to pool=%s", snapshot.getUuid(), storagePoolVO.getName());
        SnapshotInfo srcSnapshot = (SnapshotInfo) snapshot;
        SnapshotInfo destSnapshot = (SnapshotInfo) snapshotDest;
        String err = null;
        String snapshotName = StorPoolStorageAdaptor.getVolumeNameFromPath(srcSnapshot.getPath(), false);
        if (location != null) {
            SpApiResponse resp = exportSnapshot(snapshot, location, snapshotName);
            if (resp.getError() != null) {
                err = String.format("Failed to export snapshot [{}] from [{}] due to [{}]", snapshotName, location, resp.getError());
                StorPoolUtil.spLog(err);
                completeCallback(callback, destSnapshot.getPath(), err);
                return;
            }
            keepExportedSnapshot(snapshot, location, snapshotName);

            SpConnectionDesc connectionRemote = StorPoolUtil.getSpConnection(storagePoolVO.getUuid(),
                    storagePoolVO.getId(), storagePoolDetailsDao, _primaryDataStoreDao);
            SpApiResponse respFromRemote = copySnapshotFromRemote(snapshot, storagePoolVO, snapshotName, connectionRemote);

            if (respFromRemote.getError() != null) {
                err = String.format("Failed to copy snapshot [{}] to [{}] due to [{}]", snapshotName, location, respFromRemote.getError());
                StorPoolUtil.spLog(err);
                completeCallback(callback, destSnapshot.getPath(), err);
                return;
            }
            StorPoolUtil.spLog("The snapshot [%s] was copied from remote", snapshotName);

            respFromRemote = StorPoolUtil.snapshotReconcile("~" + snapshotName, connectionRemote);
            if (respFromRemote.getError() != null) {
                err = String.format("Failed to reconcile snapshot [{}] from [{}] due to [{}]", snapshotName, location, respFromRemote.getError());
                StorPoolUtil.spLog(err);
                completeCallback(callback, destSnapshot.getPath(), err);
                return;
            }
            updateSnapshotPath(snapshotDest, srcSnapshot, destSnapshot);
        } else {
            completeCallback(callback, destSnapshot.getPath(), "The snapshot is not in the right location");
        }
        SnapshotObjectTO snap = (SnapshotObjectTO) snapshotDest.getTO();
        snap.setPath(srcSnapshot.getPath());
        completeCallback(callback, destSnapshot.getPath(), err);
    }

    private void completeCallback(AsyncCompletionCallback<CreateCmdResult> callback, String snapshotPath, String err) {
        CreateCmdResult res = new CreateCmdResult(snapshotPath, null);
        res.setResult(err);
        callback.complete(res);
    }

    private void updateSnapshotPath(DataObject snapshotDest, SnapshotInfo srcSnapshot, SnapshotInfo destSnapshot) {

        SnapshotDataStoreVO snapshotStore = _snapshotStoreDao.findByStoreSnapshot(DataStoreRole.Primary, snapshotDest.getDataStore().getId(), destSnapshot.getSnapshotId());
        snapshotStore.setInstallPath(srcSnapshot.getPath());
        _snapshotStoreDao.update(snapshotStore.getId(), snapshotStore);
    }

    @NotNull
    private SpApiResponse copySnapshotFromRemote(DataObject snapshot, StoragePoolVO storagePoolVO, String snapshotName, SpConnectionDesc connectionRemote) {

        String localLocation = StorPoolConfigurationManager.StorPoolClusterLocation
                .valueIn(snapshot.getDataStore().getId());
        StoragePoolDetailVO template = storagePoolDetailsDao.findDetail(storagePoolVO.getId(),
                StorPoolUtil.SP_TEMPLATE);
        Map<String, String> tags = addStorPoolTags(snapshot);
        SpApiResponse respFromRemote = StorPoolUtil.snapshotFromRemote(snapshotName, localLocation,
                template.getValue(), tags, connectionRemote);
        return respFromRemote;
    }

    @NotNull
    private static Map<String, String> addStorPoolTags(DataObject snapshot) {
        Map<String, String> tags = new HashMap<>();
        tags.put("cs", "snapshot");
        tags.put("uuid", snapshot.getUuid());
        return tags;
    }

    private void keepExportedSnapshot(DataObject snapshot, String location, String snapshotName) {

        String detail = "~" + snapshotName + ";" + location;
        SnapshotDetailsVO snapshotForRecovery = new SnapshotDetailsVO(snapshot.getId(), StorPoolUtil.SP_RECOVERED_SNAPSHOT, detail, true);
        _snapshotDetailsDao.persist(snapshotForRecovery);
    }

    @NotNull
    private SpApiResponse exportSnapshot(DataObject snapshot, String location, String snapshotName) {

        SpConnectionDesc connectionLocal = StorPoolUtil.getSpConnection(snapshot.getDataStore().getUuid(),
                snapshot.getDataStore().getId(), storagePoolDetailsDao, _primaryDataStoreDao);
        Long clusterId = StorPoolHelper.findClusterIdByGlobalId(StorPoolUtil.getSnapshotClusterId("~" + snapshotName, connectionLocal), clusterDao);
        connectionLocal = StorPoolHelper.getSpConnectionDesc(connectionLocal, clusterId);
        SpApiResponse resp = StorPoolUtil.snapshotExport("~" + snapshotName, location, connectionLocal);
        return resp;
    }
}
