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

import javax.inject.Inject;

import org.apache.cloudstack.engine.subsystem.api.storage.*;
import org.apache.cloudstack.engine.subsystem.api.storage.ObjectInDataStoreStateMachine.Event;
import org.apache.cloudstack.engine.subsystem.api.storage.ObjectInDataStoreStateMachine.State;
import org.apache.cloudstack.framework.config.dao.ConfigurationDao;
import org.apache.cloudstack.storage.command.CreateObjectAnswer;
import org.apache.cloudstack.storage.datastore.db.SnapshotDataStoreDao;
import org.apache.cloudstack.storage.datastore.db.SnapshotDataStoreVO;
import org.apache.cloudstack.storage.to.SnapshotObjectTO;

import org.apache.log4j.Logger;
import org.springframework.stereotype.Component;

import com.cloud.exception.InvalidParameterValueException;
import com.cloud.storage.DataStoreRole;
import com.cloud.storage.Snapshot;
import com.cloud.storage.SnapshotVO;
import com.cloud.storage.dao.SnapshotDao;
import com.cloud.storage.snapshot.SnapshotManager;
import com.cloud.utils.NumbersUtil;
import com.cloud.utils.exception.CloudRuntimeException;
import com.cloud.utils.fsm.NoTransitionException;

@Component
public class XenserverSnapshotStrategy extends SnapshotStrategyBase {
    private static final Logger s_logger = Logger.getLogger(XenserverSnapshotStrategy.class);

    @Inject
    SnapshotManager snapshotMgr;
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
    SnapshotDataFactory snapshotDataFactory;

    @Override
    public SnapshotInfo backupSnapshot(SnapshotInfo snapshot) {
        SnapshotInfo parentSnapshot = snapshot.getParent();

        if (parentSnapshot != null && snapshot.getPath().equalsIgnoreCase(parentSnapshot.getPath())) {
            s_logger.debug("backup an empty snapshot");
            // don't need to backup this snapshot
            SnapshotDataStoreVO parentSnapshotOnBackupStore = this.snapshotStoreDao.findBySnapshot(
                    parentSnapshot.getId(), DataStoreRole.Image);
            if (parentSnapshotOnBackupStore != null && parentSnapshotOnBackupStore.getState() == State.Ready) {
                DataStore store = dataStoreMgr.getDataStore(parentSnapshotOnBackupStore.getDataStoreId(),
                        parentSnapshotOnBackupStore.getRole());

                SnapshotInfo snapshotOnImageStore = (SnapshotInfo) store.create(snapshot);
                snapshotOnImageStore.processEvent(Event.CreateOnlyRequested);

                SnapshotObjectTO snapTO = new SnapshotObjectTO();
                snapTO.setPath(parentSnapshotOnBackupStore.getInstallPath());

                CreateObjectAnswer createSnapshotAnswer = new CreateObjectAnswer(snapTO);

                snapshotOnImageStore.processEvent(Event.OperationSuccessed, createSnapshotAnswer);
                SnapshotObject snapObj = (SnapshotObject) snapshot;
                try {
                    snapObj.processEvent(Snapshot.Event.OperationNotPerformed);
                } catch (NoTransitionException e) {
                    s_logger.debug("Failed to change state: " + snapshot.getId() + ": " + e.toString());
                    throw new CloudRuntimeException(e.toString());
                }
                return this.snapshotDataFactory.getSnapshot(snapObj.getId(), store);
            } else {
                s_logger.debug("parent snapshot hasn't been backed up yet");
            }
        }

        // determine full snapshot backup or not

        boolean fullBackup = false;

        if (parentSnapshot != null) {
            int _deltaSnapshotMax = NumbersUtil.parseInt(configDao.getValue("snapshot.delta.max"),
                    SnapshotManager.DELTAMAX);
            int deltaSnap = _deltaSnapshotMax;

            int i;
            SnapshotDataStoreVO parentSnapshotOnBackupStore = null;
            for (i = 1; i < deltaSnap; i++) {
                parentSnapshotOnBackupStore = this.snapshotStoreDao.findBySnapshot(parentSnapshot.getId(),
                        DataStoreRole.Image);
                if (parentSnapshotOnBackupStore == null) {
                    break;
                }
                Long prevBackupId = parentSnapshotOnBackupStore.getParentSnapshotId();

                if (prevBackupId == 0) {
                    break;
                }

                parentSnapshotOnBackupStore = this.snapshotStoreDao.findBySnapshot(prevBackupId, DataStoreRole.Image);
            }
            if (i >= deltaSnap) {
                fullBackup = true;
            }
        }

        snapshot.addPayload(fullBackup);
        return this.snapshotSvr.backupSnapshot(snapshot);
    }

    protected boolean deleteSnapshotChain(SnapshotInfo snapshot) {
        s_logger.debug("delete snapshot chain for snapshot: " + snapshot.getId());
        boolean result = false;
        boolean resultIsSet = false;   //need to track, the snapshot itself is deleted or not.
        try {
            while (snapshot != null && (snapshot.getState() == Snapshot.State.Destroying || snapshot.getState()
                    == Snapshot.State.Destroyed || snapshot.getState() == Snapshot.State.Error)) {
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
                    boolean r = this.snapshotSvr.deleteSnapshot(snapshot);
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

        if (snapshotVO.getState() == Snapshot.State.CreatedOnPrimary) {
            s_logger.debug("delete snapshot on primary storage:");
            snapshotVO.setState(Snapshot.State.Destroyed);
            snapshotDao.update(snapshotId, snapshotVO);
            return true;
        }

        if (!Snapshot.State.BackedUp.equals(snapshotVO.getState())) {
            throw new InvalidParameterValueException("Can't delete snapshotshot " + snapshotId
                    + " due to it is not in BackedUp Status");
        }

        // first mark the snapshot as destroyed, so that ui can't see it, but we
        // may not destroy the snapshot on the storage, as other snapshots may
        // depend on it.
        SnapshotInfo snapshotOnImage = this.snapshotDataFactory.getSnapshot(snapshotId, DataStoreRole.Image);
        if (snapshotOnImage == null) {
            s_logger.debug("Can't find snapshot on backup storage, delete it in db");
            snapshotDao.remove(snapshotId);
            return true;
        }

        SnapshotObject obj = (SnapshotObject) snapshotOnImage;
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
    public SnapshotInfo takeSnapshot(SnapshotInfo snapshot) {
        SnapshotResult result =  snapshotSvr.takeSnapshot(snapshot);
        if (result.isFailed()) {
            s_logger.debug("Failed to take snapshot: " + result.getResult());
            throw new CloudRuntimeException(result.getResult());
        }
        snapshot = result.getSnashot();
        DataStore primaryStore = snapshot.getDataStore();

        SnapshotInfo backupedSnapshot = this.backupSnapshot(snapshot);
        try {
            SnapshotInfo parent = snapshot.getParent();
            if (backupedSnapshot != null && parent != null) {
                Long parentSnapshotId = parent.getId();
                while (parentSnapshotId != null && parentSnapshotId != 0L) {
                    SnapshotDataStoreVO snapshotDataStoreVO = snapshotStoreDao.findByStoreSnapshot(primaryStore.getRole(),primaryStore.getId(), parentSnapshotId);
                    if (snapshotDataStoreVO != null) {
                        parentSnapshotId = snapshotDataStoreVO.getParentSnapshotId();
                        snapshotStoreDao.remove(snapshotDataStoreVO.getId());
                    } else {
                        parentSnapshotId = null;
                    }
                }
                SnapshotDataStoreVO snapshotDataStoreVO = snapshotStoreDao.findByStoreSnapshot(primaryStore.getRole(), primaryStore.getId(),
                        snapshot.getId());
                if (snapshotDataStoreVO != null) {
                    snapshotDataStoreVO.setParentSnapshotId(0L);
                    snapshotStoreDao.update(snapshotDataStoreVO.getId(), snapshotDataStoreVO);
                }
            }
        } catch (Exception e) {
            s_logger.debug("Failed to clean up snapshots on primary storage", e);
        }
        return backupedSnapshot;
    }

    @Override
    public boolean canHandle(Snapshot snapshot) {
        return true;
    }
}
