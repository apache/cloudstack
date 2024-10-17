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

import java.util.ArrayList;
import java.util.List;

import javax.inject.Inject;

import org.apache.cloudstack.engine.subsystem.api.storage.DataStore;
import org.apache.cloudstack.engine.subsystem.api.storage.DataStoreManager;
import org.apache.cloudstack.engine.subsystem.api.storage.ObjectInDataStoreStateMachine.Event;
import org.apache.cloudstack.engine.subsystem.api.storage.ObjectInDataStoreStateMachine.State;
import org.apache.cloudstack.engine.subsystem.api.storage.SnapshotDataFactory;
import org.apache.cloudstack.engine.subsystem.api.storage.SnapshotInfo;
import org.apache.cloudstack.engine.subsystem.api.storage.SnapshotService;
import org.apache.cloudstack.engine.subsystem.api.storage.SnapshotStrategy;
import org.apache.cloudstack.engine.subsystem.api.storage.StrategyPriority;
import org.apache.cloudstack.storage.datastore.db.PrimaryDataStoreDao;
import org.apache.cloudstack.storage.datastore.db.SnapshotDataStoreDao;
import org.apache.cloudstack.storage.datastore.db.SnapshotDataStoreVO;
import org.apache.cloudstack.storage.datastore.db.StoragePoolDetailsDao;
import org.apache.cloudstack.storage.datastore.db.StoragePoolVO;
import org.apache.cloudstack.storage.datastore.util.StorPoolHelper;
import org.apache.cloudstack.storage.datastore.util.StorPoolUtil;
import org.apache.cloudstack.storage.datastore.util.StorPoolUtil.SpApiResponse;
import org.apache.cloudstack.storage.datastore.util.StorPoolUtil.SpConnectionDesc;
import org.apache.commons.collections.CollectionUtils;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.LogManager;
import org.springframework.stereotype.Component;

import com.cloud.exception.InvalidParameterValueException;
import com.cloud.hypervisor.kvm.storage.StorPoolStorageAdaptor;
import com.cloud.storage.DataStoreRole;
import com.cloud.storage.Snapshot;
import com.cloud.storage.SnapshotVO;
import com.cloud.storage.VolumeVO;
import com.cloud.storage.dao.SnapshotDao;
import com.cloud.storage.dao.SnapshotDetailsDao;
import com.cloud.storage.dao.SnapshotDetailsVO;
import com.cloud.storage.dao.SnapshotZoneDao;
import com.cloud.storage.dao.VolumeDao;
import com.cloud.utils.exception.CloudRuntimeException;
import com.cloud.utils.fsm.NoTransitionException;


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
        VolumeVO volume = _volumeDao.findByIdIncludingRemoved(snapshotVO.getVolumeId());
        String name = StorPoolHelper.getSnapshotName(snapshotId, snapshotVO.getUuid(), _snapshotStoreDao, _snapshotDetailsDao);
        boolean res = false;
        // clean-up snapshot from Storpool storage pools
        StoragePoolVO storage = _primaryDataStoreDao.findById(volume.getPoolId());
        if (storage.getStorageProviderName().equals(StorPoolUtil.SP_PROVIDER_NAME)) {
            try {
                SpConnectionDesc conn = StorPoolUtil.getSpConnection(storage.getUuid(), storage.getId(), storagePoolDetailsDao, _primaryDataStoreDao);
                SpApiResponse resp = StorPoolUtil.snapshotDelete(name, conn);
                if (resp.getError() != null) {
                    final String err = String.format("Failed to clean-up Storpool snapshot %s. Error: %s", name, resp.getError());
                    StorPoolUtil.spLog(err);
                    markSnapshotAsDestroyedIfAlreadyRemoved(snapshotId, resp);
                    throw new CloudRuntimeException(err);
                } else {
                    res = deleteSnapshotFromDbIfNeeded(snapshotVO, zoneId);
                    StorPoolUtil.spLog("StorpoolSnapshotStrategy.deleteSnapshot: executed successfully=%s, snapshot uuid=%s, name=%s", res, snapshotVO.getUuid(), name);
                }
            } catch (Exception e) {
                String errMsg = String.format("Cannot delete snapshot due to %s", e.getMessage());
                throw new CloudRuntimeException(errMsg);
            }
        }

        return res;
    }

    private void markSnapshotAsDestroyedIfAlreadyRemoved(Long snapshotId, SpApiResponse resp) {
        if (resp.getError().getName().equals("objectDoesNotExist")) {
            SnapshotDataStoreVO snapshotOnPrimary = _snapshotStoreDao.findBySourceSnapshot(snapshotId, DataStoreRole.Primary);
            if (snapshotOnPrimary != null) {
                snapshotOnPrimary.setState(State.Destroyed);
                _snapshotStoreDao.update(snapshotOnPrimary.getId(), snapshotOnPrimary);
            }
        }
    }

    @Override
    public StrategyPriority canHandle(Snapshot snapshot, Long zoneId, SnapshotOperation op) {
        logger.debug(String.format("StorpoolSnapshotStrategy.canHandle: snapshot=%s, uuid=%s, op=%s", snapshot.getName(), snapshot.getUuid(), op));

        if (op != SnapshotOperation.DELETE) {
            return StrategyPriority.CANT_HANDLE;
        }
        SnapshotDataStoreVO snapshotOnPrimary = _snapshotStoreDao.findOneBySnapshotAndDatastoreRole(snapshot.getId(), DataStoreRole.Primary);
        if (snapshotOnPrimary == null) {
            return StrategyPriority.CANT_HANDLE;
        }
        if (zoneId != null) { // If zoneId is present, then it should be same as the zoneId of primary store
            StoragePoolVO storagePoolVO = _primaryDataStoreDao.findById(snapshotOnPrimary.getDataStoreId());
            if (!zoneId.equals(storagePoolVO.getDataCenterId())) {
                return StrategyPriority.CANT_HANDLE;
            }
        }
        String name = StorPoolHelper.getSnapshotName(snapshot.getId(), snapshot.getUuid(), _snapshotStoreDao, _snapshotDetailsDao);
        if (name != null) {
            StorPoolUtil.spLog("StorpoolSnapshotStrategy.canHandle: globalId=%s", name);

            return StrategyPriority.HIGHEST;
        }
        SnapshotDetailsVO snapshotDetails = _snapshotDetailsDao.findDetail(snapshot.getId(), snapshot.getUuid());
        if (snapshotDetails != null) {
            _snapshotDetailsDao.remove(snapshotDetails.getId());
        }
        return StrategyPriority.CANT_HANDLE;
    }

    private boolean deleteSnapshotChain(SnapshotInfo snapshot) {
        logger.debug("delete snapshot chain for snapshot: " + snapshot.getId());
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
                logger.debug("Snapshot: " + snapshot.getId() + " doesn't have children, so it's ok to delete it and its parents");
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
                                    logger.debug("Delete snapshot " + snapshot.getId() + " from image cache store: " + cacheSnap.getDataStore().getName());
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
        boolean areLastSnapshotRef = areLastSnapshotRef(snapshotId);
        try {
            if (areLastSnapshotRef) {
                obj.processEvent(Snapshot.Event.DestroyRequested);
            }
        } catch (NoTransitionException e) {
            logger.debug("Failed to set the state to destroying: ", e);
            return false;
        }

        try {
            boolean result = deleteSnapshotChain(snapshotOnImage);
            _snapshotStoreDao.updateDisplayForSnapshotStoreRole(snapshotId, store.getId(), store.getRole(), false);
            if (areLastSnapshotRef) {
                obj.processEvent(Snapshot.Event.OperationSucceeded);
            }
            if (result) {
                SnapshotDataStoreVO snapshotOnPrimary = _snapshotStoreDao.findOneBySnapshotAndDatastoreRole(snapshotOnImage.getSnapshotId(), DataStoreRole.Primary);
                if (snapshotOnPrimary != null) {
                    snapshotOnPrimary.setState(State.Destroyed);
                    _snapshotStoreDao.update(snapshotOnPrimary.getId(), snapshotOnPrimary);
                }
            }
        } catch (Exception e) {
            logger.debug("Failed to delete snapshot: ", e);
            try {
                if (areLastSnapshotRef) {
                    obj.processEvent(Snapshot.Event.OperationFailed);
                }
            } catch (NoTransitionException e1) {
                logger.debug("Failed to change snapshot state: " + e.toString());
            }
            return false;
        }
        return true;
    }

    private boolean deleteSnapshotFromDbIfNeeded(SnapshotVO snapshotVO, Long zoneId) {
        final long snapshotId = snapshotVO.getId();
        SnapshotDetailsVO snapshotDetails = _snapshotDetailsDao.findDetail(snapshotId, snapshotVO.getUuid());
        if (snapshotDetails != null) {
            _snapshotDetailsDao.removeDetails(snapshotId);
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

        if (snapshotVO.getState() == Snapshot.State.CreatedOnPrimary) {
            snapshotVO.setState(Snapshot.State.Destroyed);
            _snapshotDao.update(snapshotId, snapshotVO);
            return true;
        }

        if (!Snapshot.State.BackedUp.equals(snapshotVO.getState()) && !Snapshot.State.Error.equals(snapshotVO.getState()) &&
                !Snapshot.State.Destroying.equals(snapshotVO.getState())) {
            throw new InvalidParameterValueException("Can't delete snapshot " + snapshotId + " due to it is in " + snapshotVO.getState() + " Status");
        }
        List<SnapshotDataStoreVO> storeRefs = _snapshotStoreDao.listReadyBySnapshot(snapshotId, DataStoreRole.Image);
        if (zoneId != null) {
            storeRefs.removeIf(ref -> !zoneId.equals(dataStoreMgr.getStoreZoneId(ref.getDataStoreId(), ref.getRole())));
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
        updateSnapshotToDestroyed(snapshotVO);
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
}
