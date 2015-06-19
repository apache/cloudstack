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

import java.util.List;

import javax.inject.Inject;

import org.apache.log4j.Logger;
import org.springframework.stereotype.Component;
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
import org.apache.cloudstack.storage.command.CreateObjectAnswer;
import org.apache.cloudstack.storage.datastore.db.SnapshotDataStoreDao;
import org.apache.cloudstack.storage.datastore.db.SnapshotDataStoreVO;
import org.apache.cloudstack.storage.to.SnapshotObjectTO;

import com.cloud.exception.InvalidParameterValueException;
import com.cloud.hypervisor.Hypervisor;
import com.cloud.hypervisor.Hypervisor.HypervisorType;
import com.cloud.storage.CreateSnapshotPayload;
import com.cloud.storage.DataStoreRole;
import com.cloud.storage.Snapshot;
import com.cloud.storage.SnapshotVO;
import com.cloud.storage.Volume;
import com.cloud.storage.VolumeVO;
import com.cloud.storage.dao.SnapshotDao;
import com.cloud.storage.dao.VolumeDao;
import com.cloud.storage.snapshot.SnapshotManager;
import com.cloud.utils.NumbersUtil;
import com.cloud.utils.db.DB;
import com.cloud.utils.exception.CloudRuntimeException;
import com.cloud.utils.fsm.NoTransitionException;

@Component
public class XenserverSnapshotStrategy extends SnapshotStrategyBase {
    private static final Logger s_logger = Logger.getLogger(XenserverSnapshotStrategy.class);

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

    @Override
    public SnapshotInfo backupSnapshot(SnapshotInfo snapshot) {
        SnapshotInfo parentSnapshot = snapshot.getParent();

        if (parentSnapshot != null && snapshot.getPath().equalsIgnoreCase(parentSnapshot.getPath())) {
            s_logger.debug("backup an empty snapshot");
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
                SnapshotObject snapObj = (SnapshotObject)snapshot;
                try {
                    snapObj.processEvent(Snapshot.Event.OperationNotPerformed);
                } catch (NoTransitionException e) {
                    s_logger.debug("Failed to change state: " + snapshot.getId() + ": " + e.toString());
                    throw new CloudRuntimeException(e.toString());
                }
                return snapshotDataFactory.getSnapshot(snapObj.getId(), store);
            } else {
                s_logger.debug("parent snapshot hasn't been backed up yet");
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
                if (oldestSnapshotOnPrimary.getDataStoreId() == volume.getPoolId()) {
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
                } else {
                    // if there is an snapshot entry for previousPool(primary storage) of migrated volume, delete it becasue CS created one more snapshot entry for current pool
                    snapshotStoreDao.remove(oldestSnapshotOnPrimary.getId());
                }
            }
        }

        snapshot.addPayload(fullBackup);
        return snapshotSvr.backupSnapshot(snapshot);
    }

    protected boolean deleteSnapshotChain(SnapshotInfo snapshot) {
        s_logger.debug("delete snapshot chain for snapshot: " + snapshot.getId());
        boolean result = false;
        boolean resultIsSet = false;   //need to track, the snapshot itself is deleted or not.
        try {
            while (snapshot != null &&
                (snapshot.getState() == Snapshot.State.Destroying || snapshot.getState() == Snapshot.State.Destroyed || snapshot.getState() == Snapshot.State.Error)) {
                SnapshotInfo child = snapshot.getChild();

                if (child != null) {
                    s_logger.debug("the snapshot has child, can't delete it on the storage");
                    break;
                }
                s_logger.debug("Snapshot: " + snapshot.getId() + " doesn't have children, so it's ok to delete it and its parents");
                SnapshotInfo parent = snapshot.getParent();
                boolean deleted = false;
                if (parent != null) {
                    if (parent.getPath() != null && parent.getPath().equalsIgnoreCase(snapshot.getPath())) {
                        //NOTE: if both snapshots share the same path, it's for xenserver's empty delta snapshot. We can't delete the snapshot on the backend, as parent snapshot still reference to it
                        //Instead, mark it as destroyed in the db.
                        s_logger.debug("for empty delta snapshot, only mark it as destroyed in db");
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
                    boolean r = snapshotSvr.deleteSnapshot(snapshot);
                    if (r) {
                        // delete snapshot in cache if there is
                        List<SnapshotInfo> cacheSnaps = snapshotDataFactory.listSnapshotOnCache(snapshot.getId());
                        for (SnapshotInfo cacheSnap : cacheSnaps) {
                            s_logger.debug("Delete snapshot " + snapshot.getId() + " from image cache store: " + cacheSnap.getDataStore().getName());
                            cacheSnap.delete();
                        }
                    }
                    if (!resultIsSet) {
                        result = r;
                        resultIsSet = true;
                    }
                }
                snapshot = parent;
            }
        } catch (Exception e) {
            s_logger.debug("delete snapshot failed: ", e);
        }
        return result;
    }

    @Override
    public boolean deleteSnapshot(Long snapshotId) {
        SnapshotVO snapshotVO = snapshotDao.findById(snapshotId);
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
            s_logger.debug("delete snapshot on primary storage:");
            snapshotVO.setState(Snapshot.State.Destroyed);
            snapshotDao.update(snapshotId, snapshotVO);
            return true;
        }

        if (!Snapshot.State.BackedUp.equals(snapshotVO.getState()) && !Snapshot.State.Error.equals(snapshotVO.getState())) {
            throw new InvalidParameterValueException("Can't delete snapshotshot " + snapshotId + " due to it is in " + snapshotVO.getState() + " Status");
        }

        // first mark the snapshot as destroyed, so that ui can't see it, but we
        // may not destroy the snapshot on the storage, as other snapshots may
        // depend on it.
        SnapshotInfo snapshotOnImage = snapshotDataFactory.getSnapshot(snapshotId, DataStoreRole.Image);
        if (snapshotOnImage == null) {
            s_logger.debug("Can't find snapshot on backup storage, delete it in db");
            snapshotDao.remove(snapshotId);
            return true;
        }

        SnapshotObject obj = (SnapshotObject)snapshotOnImage;
        try {
            obj.processEvent(Snapshot.Event.DestroyRequested);
        } catch (NoTransitionException e) {
            s_logger.debug("Failed to set the state to destroying: ", e);
            return false;
        }

        try {
            boolean result = deleteSnapshotChain(snapshotOnImage);
            obj.processEvent(Snapshot.Event.OperationSucceeded);
            if (result) {
                //snapshot is deleted on backup storage, need to delete it on primary storage
                SnapshotDataStoreVO snapshotOnPrimary = snapshotStoreDao.findBySnapshot(snapshotId, DataStoreRole.Primary);
                if (snapshotOnPrimary != null) {
                    snapshotOnPrimary.setState(State.Destroyed);
                    snapshotStoreDao.update(snapshotOnPrimary.getId(), snapshotOnPrimary);
                }
            }
        } catch (Exception e) {
            s_logger.debug("Failed to delete snapshot: ", e);
            try {
                obj.processEvent(Snapshot.Event.OperationFailed);
            } catch (NoTransitionException e1) {
                s_logger.debug("Failed to change snapshot state: " + e.toString());
            }
            return false;
        }

        return true;
    }

    @Override
    public boolean revertSnapshot(Long snapshotId) {
        throw new CloudRuntimeException("revert Snapshot is not supported");
    }

    @Override
    @DB
    public SnapshotInfo takeSnapshot(SnapshotInfo snapshot) {
        Object payload = snapshot.getPayload();
        if (payload != null) {
            CreateSnapshotPayload createSnapshotPayload = (CreateSnapshotPayload)payload;
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
                    s_logger.debug("Failed to take snapshot: " + result.getResult());
                    throw new CloudRuntimeException(result.getResult());
                }
            } finally {
                if (result != null && result.isSuccess()) {
                    volumeInfo.stateTransit(Volume.Event.OperationSucceeded);
                } else {
                    volumeInfo.stateTransit(Volume.Event.OperationFailed);
                }
            }

            snapshot = result.getSnashot();
            DataStore primaryStore = snapshot.getDataStore();

            SnapshotInfo backupedSnapshot = backupSnapshot(snapshot);

            try {
                SnapshotInfo parent = snapshot.getParent();
                if (backupedSnapshot != null && parent != null) {
                    Long parentSnapshotId = parent.getId();
                    while (parentSnapshotId != null && parentSnapshotId != 0L) {
                        SnapshotDataStoreVO snapshotDataStoreVO = snapshotStoreDao.findByStoreSnapshot(primaryStore.getRole(), primaryStore.getId(), parentSnapshotId);
                        if (snapshotDataStoreVO != null) {
                            parentSnapshotId = snapshotDataStoreVO.getParentSnapshotId();
                            snapshotStoreDao.remove(snapshotDataStoreVO.getId());
                        } else {
                            parentSnapshotId = null;
                        }
                    }
                    SnapshotDataStoreVO snapshotDataStoreVO = snapshotStoreDao.findByStoreSnapshot(primaryStore.getRole(), primaryStore.getId(), snapshot.getId());
                    if (snapshotDataStoreVO != null) {
                        snapshotDataStoreVO.setParentSnapshotId(0L);
                        snapshotStoreDao.update(snapshotDataStoreVO.getId(), snapshotDataStoreVO);
                    }
                }
            } catch (Exception e) {
                s_logger.debug("Failed to clean up snapshots on primary storage", e);
            }
            return backupedSnapshot;
        } finally {
            if (snapshotVO != null) {
                snapshotDao.releaseFromLockTable(snapshot.getId());
            }
        }
    }

    @Override
    public StrategyPriority canHandle(Snapshot snapshot, SnapshotOperation op) {
        if (op == SnapshotOperation.REVERT) {
            return StrategyPriority.CANT_HANDLE;
        }

        return StrategyPriority.DEFAULT;
    }
}
