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

import java.util.Map;

import javax.inject.Inject;

import org.apache.log4j.Logger;
import org.springframework.stereotype.Component;
import org.apache.cloudstack.engine.subsystem.api.storage.DataStore;
import org.apache.cloudstack.engine.subsystem.api.storage.DataStoreCapabilities;
import org.apache.cloudstack.engine.subsystem.api.storage.DataStoreManager;
import org.apache.cloudstack.engine.subsystem.api.storage.SnapshotDataFactory;
import org.apache.cloudstack.engine.subsystem.api.storage.SnapshotInfo;
import org.apache.cloudstack.engine.subsystem.api.storage.SnapshotResult;
import org.apache.cloudstack.engine.subsystem.api.storage.StrategyPriority;
import org.apache.cloudstack.engine.subsystem.api.storage.VolumeInfo;
import org.apache.cloudstack.storage.datastore.db.PrimaryDataStoreDao;
import org.apache.cloudstack.storage.datastore.db.SnapshotDataStoreDao;

import com.cloud.exception.InvalidParameterValueException;
import com.cloud.storage.DataStoreRole;
import com.cloud.storage.Snapshot;
import com.cloud.storage.SnapshotVO;
import com.cloud.storage.Volume;
import com.cloud.storage.VolumeVO;
import com.cloud.storage.dao.SnapshotDao;
import com.cloud.storage.dao.VolumeDao;
import com.cloud.utils.db.DB;
import com.cloud.utils.exception.CloudRuntimeException;
import com.cloud.utils.fsm.NoTransitionException;

@Component
public class StorageSystemSnapshotStrategy extends SnapshotStrategyBase {
    private static final Logger s_logger = Logger.getLogger(StorageSystemSnapshotStrategy.class);

    @Inject private DataStoreManager _dataStoreMgr;
    @Inject private SnapshotDao _snapshotDao;
    @Inject private SnapshotDataFactory _snapshotDataFactory;
    @Inject private SnapshotDataStoreDao _snapshotStoreDao;
    @Inject private PrimaryDataStoreDao _storagePoolDao;
    @Inject private VolumeDao _volumeDao;

    @Override
    public SnapshotInfo backupSnapshot(SnapshotInfo snapshotInfo) {
        return snapshotInfo;
    }

    @Override
    public boolean deleteSnapshot(Long snapshotId) {
        SnapshotVO snapshotVO = _snapshotDao.findById(snapshotId);

        if (Snapshot.State.Destroyed.equals(snapshotVO.getState())) {
            return true;
        }

        if (Snapshot.State.Error.equals(snapshotVO.getState())) {
            _snapshotDao.remove(snapshotId);

            return true;
        }

        if (!Snapshot.State.BackedUp.equals(snapshotVO.getState())) {
            throw new InvalidParameterValueException("Unable to delete snapshotshot " + snapshotId + " because it is in the following state: " + snapshotVO.getState());
        }

        SnapshotObject snapshotObj = (SnapshotObject)_snapshotDataFactory.getSnapshot(snapshotId, DataStoreRole.Primary);

        if (snapshotObj == null) {
            s_logger.debug("Can't find snapshot; deleting it in DB");

            _snapshotDao.remove(snapshotId);

            return true;
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
    public boolean revertSnapshot(Long snapshotId) {
        return true;
    }

    @Override
    @DB
    public SnapshotInfo takeSnapshot(SnapshotInfo snapshotInfo) {
        SnapshotVO snapshotVO = _snapshotDao.acquireInLockTable(snapshotInfo.getId());

        if (snapshotVO == null) {
            throw new CloudRuntimeException("Failed to acquire lock on the following snapshot: " + snapshotInfo.getId());
        }

        try {
            VolumeInfo volumeInfo = snapshotInfo.getBaseVolume();

            volumeInfo.stateTransit(Volume.Event.SnapshotRequested);

            SnapshotResult result = null;

            try {
                result = snapshotSvr.takeSnapshot(snapshotInfo);

                if (result.isFailed()) {
                    s_logger.debug("Failed to take the following snapshot: " + result.getResult());

                    throw new CloudRuntimeException(result.getResult());
                }

                markAsBackedUp((SnapshotObject)result.getSnashot());
            }
            finally {
                if (result != null && result.isSuccess()) {
                    volumeInfo.stateTransit(Volume.Event.OperationSucceeded);
                }
                else {
                    volumeInfo.stateTransit(Volume.Event.OperationFailed);
                }
            }
        }
        finally {
            if (snapshotVO != null) {
                _snapshotDao.releaseFromLockTable(snapshotInfo.getId());
            }
        }

        return snapshotInfo;
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
        long volumeId = snapshot.getVolumeId();
        VolumeVO volume = _volumeDao.findById(volumeId);

        long storagePoolId = volume.getPoolId();
        DataStore dataStore = _dataStoreMgr.getDataStore(storagePoolId, DataStoreRole.Primary);

        Map<String, String> mapCapabilities = dataStore.getDriver().getCapabilities();

        String value = mapCapabilities.get(DataStoreCapabilities.STORAGE_SYSTEM_SNAPSHOT.toString());
        Boolean supportsStorageSystemSnapshots = new Boolean(value);

        if (supportsStorageSystemSnapshots) {
            return StrategyPriority.HIGHEST;
        }

        return StrategyPriority.CANT_HANDLE;
    }
}
