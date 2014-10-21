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

import com.cloud.dc.ClusterVO;
import com.cloud.dc.dao.ClusterDao;
import com.cloud.exception.InvalidParameterValueException;
import com.cloud.host.HostVO;
import com.cloud.host.dao.HostDao;
import com.cloud.hypervisor.Hypervisor.HypervisorType;
import com.cloud.storage.DataStoreRole;
import com.cloud.storage.Snapshot;
import com.cloud.storage.SnapshotVO;
import com.cloud.storage.Volume;
import com.cloud.storage.Volume.Type;
import com.cloud.storage.VolumeVO;
import com.cloud.storage.dao.SnapshotDao;
import com.cloud.storage.dao.VolumeDao;
import com.cloud.uservm.UserVm;
import com.cloud.utils.db.DB;
import com.cloud.utils.db.EntityManager;
import com.cloud.utils.exception.CloudRuntimeException;
import com.cloud.utils.fsm.NoTransitionException;
import com.cloud.vm.VirtualMachine.State;

@Component
public class StorageSystemSnapshotStrategy extends SnapshotStrategyBase {
    private static final Logger s_logger = Logger.getLogger(StorageSystemSnapshotStrategy.class);

    @Inject private ClusterDao _clusterDao;
    @Inject private DataStoreManager _dataStoreMgr;
    @Inject private EntityManager _entityMgr;
    @Inject private HostDao _hostDao;
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
        // verify the following:
        //  if root disk, the VM cannot be running (don't allow this at all for ESX)
        //  if data disk, the disk cannot be in the attached state

        SnapshotInfo snapshotInfo = _snapshotDataFactory.getSnapshot(snapshotId, DataStoreRole.Primary);
        VolumeInfo volumeInfo = snapshotInfo.getBaseVolume();
        VolumeVO volume = _volumeDao.findById(volumeInfo.getId());

        if (volume.getVolumeType() == Type.DATADISK) {
            if (volume.getAttached() != null) {
                throw new CloudRuntimeException("A data disk must be in the detached state in order to perform a revert.");
            }
        }
        else if (volume.getVolumeType() == Type.ROOT) {
            Long instanceId = volume.getInstanceId();
            UserVm vm = _entityMgr.findById(UserVm.class, instanceId);

            Long hostId = vm.getHostId();
            HostVO hostVO = _hostDao.findById(hostId);
            Long clusterId = hostVO.getClusterId();
            ClusterVO clusterVO = _clusterDao.findById(clusterId);

            if (clusterVO.getHypervisorType() != HypervisorType.XenServer && clusterVO.getHypervisorType() !=  HypervisorType.KVM) {
                throw new CloudRuntimeException("Unsupported hypervisor type for root disk revert. Create a template from this disk and use it instead.");
            }

            if (vm.getState() != State.Stopped) {
                throw new CloudRuntimeException("A root disk cannot be reverted unless the VM it's attached to is in the stopped state.");
            }
        }
        else {
            throw new CloudRuntimeException("Unsupported volume type");
        }

        SnapshotVO snapshotVO = _snapshotDao.acquireInLockTable(snapshotId);

        if (snapshotVO == null) {
            throw new CloudRuntimeException("Failed to acquire lock on the following snapshot: " + snapshotId);
        }

        try {
            volumeInfo.stateTransit(Volume.Event.RevertRequested);

            boolean result = false;

            try {
                result = snapshotSvr.revertSnapshot(snapshotId);
            }
            finally {
                if (result) {
                    volumeInfo.stateTransit(Volume.Event.OperationSucceeded);
                }
                else {
                    String msg = "Failed to revert the volume to a snapshot";

                    s_logger.debug(msg);

                    volumeInfo.stateTransit(Volume.Event.OperationFailed);

                    throw new CloudRuntimeException("Failed to revert the volume to a snapshot");
                }
            }
        }
        finally {
            if (snapshotVO != null) {
                _snapshotDao.releaseFromLockTable(snapshotId);
            }
        }

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
        VolumeVO volumeVO = _volumeDao.findById(volumeId);

        long storagePoolId = volumeVO.getPoolId();
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
