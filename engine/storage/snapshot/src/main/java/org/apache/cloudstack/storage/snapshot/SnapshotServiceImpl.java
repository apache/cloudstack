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
import java.util.concurrent.ExecutionException;

import javax.inject.Inject;

import org.apache.cloudstack.engine.subsystem.api.storage.CopyCommandResult;
import org.apache.cloudstack.engine.subsystem.api.storage.CreateCmdResult;
import org.apache.cloudstack.engine.subsystem.api.storage.DataMotionService;
import org.apache.cloudstack.engine.subsystem.api.storage.DataStore;
import org.apache.cloudstack.engine.subsystem.api.storage.DataStoreManager;
import org.apache.cloudstack.engine.subsystem.api.storage.ObjectInDataStoreStateMachine;
import org.apache.cloudstack.engine.subsystem.api.storage.ObjectInDataStoreStateMachine.Event;
import org.apache.cloudstack.engine.subsystem.api.storage.PrimaryDataStore;
import org.apache.cloudstack.engine.subsystem.api.storage.PrimaryDataStoreDriver;
import org.apache.cloudstack.engine.subsystem.api.storage.SnapshotDataFactory;
import org.apache.cloudstack.engine.subsystem.api.storage.SnapshotInfo;
import org.apache.cloudstack.engine.subsystem.api.storage.SnapshotResult;
import org.apache.cloudstack.engine.subsystem.api.storage.SnapshotService;
import org.apache.cloudstack.engine.subsystem.api.storage.StorageCacheManager;
import org.apache.cloudstack.engine.subsystem.api.storage.VolumeInfo;
import org.apache.cloudstack.framework.async.AsyncCallFuture;
import org.apache.cloudstack.framework.async.AsyncCallbackDispatcher;
import org.apache.cloudstack.framework.async.AsyncCompletionCallback;
import org.apache.cloudstack.framework.async.AsyncRpcContext;
import org.apache.cloudstack.framework.jobs.AsyncJob;
import org.apache.cloudstack.storage.command.CommandResult;
import org.apache.cloudstack.storage.command.CopyCmdAnswer;
import org.apache.cloudstack.storage.datastore.db.SnapshotDataStoreDao;
import org.apache.cloudstack.storage.datastore.db.SnapshotDataStoreVO;
import org.apache.log4j.Logger;

import com.cloud.storage.CreateSnapshotPayload;
import com.cloud.event.EventTypes;
import com.cloud.event.UsageEventUtils;
import com.cloud.storage.DataStoreRole;
import com.cloud.storage.Snapshot;
import com.cloud.storage.SnapshotVO;
import com.cloud.storage.dao.SnapshotDao;
import com.cloud.storage.dao.SnapshotDetailsDao;
import com.cloud.storage.template.TemplateConstants;
import com.cloud.utils.db.Transaction;
import com.cloud.utils.db.TransactionCallbackNoReturn;
import com.cloud.utils.db.TransactionStatus;
import com.cloud.utils.exception.CloudRuntimeException;
import com.cloud.utils.fsm.NoTransitionException;

public class SnapshotServiceImpl implements SnapshotService {
    private static final Logger s_logger = Logger.getLogger(SnapshotServiceImpl.class);
    @Inject
    protected SnapshotDao _snapshotDao;
    @Inject
    protected SnapshotDataStoreDao _snapshotStoreDao;
    @Inject
    SnapshotDataFactory _snapshotFactory;
    @Inject
    DataStoreManager dataStoreMgr;
    @Inject
    DataMotionService motionSrv;
    @Inject
    StorageCacheManager _cacheMgr;
    @Inject
    private SnapshotDetailsDao _snapshotDetailsDao;

    static private class CreateSnapshotContext<T> extends AsyncRpcContext<T> {
        final SnapshotInfo snapshot;
        final AsyncCallFuture<SnapshotResult> future;

        public CreateSnapshotContext(AsyncCompletionCallback<T> callback, VolumeInfo volume, SnapshotInfo snapshot, AsyncCallFuture<SnapshotResult> future) {
            super(callback);
            this.snapshot = snapshot;
            this.future = future;
        }
    }

    static private class DeleteSnapshotContext<T> extends AsyncRpcContext<T> {
        final SnapshotInfo snapshot;
        final AsyncCallFuture<SnapshotResult> future;

        public DeleteSnapshotContext(AsyncCompletionCallback<T> callback, SnapshotInfo snapshot, AsyncCallFuture<SnapshotResult> future) {
            super(callback);
            this.snapshot = snapshot;
            this.future = future;
        }

    }

    static private class CopySnapshotContext<T> extends AsyncRpcContext<T> {
        final SnapshotInfo srcSnapshot;
        final SnapshotInfo destSnapshot;
        final AsyncCallFuture<SnapshotResult> future;

        public CopySnapshotContext(AsyncCompletionCallback<T> callback, SnapshotInfo srcSnapshot, SnapshotInfo destSnapshot, AsyncCallFuture<SnapshotResult> future) {
            super(callback);
            this.srcSnapshot = srcSnapshot;
            this.destSnapshot = destSnapshot;
            this.future = future;
        }

    }

    static private class RevertSnapshotContext<T> extends AsyncRpcContext<T> {
        final SnapshotInfo snapshot;
        final AsyncCallFuture<SnapshotResult> future;

        public RevertSnapshotContext(AsyncCompletionCallback<T> callback, SnapshotInfo snapshot, AsyncCallFuture<SnapshotResult> future) {
            super(callback);
            this.snapshot = snapshot;
            this.future = future;
        }

    }

    protected Void createSnapshotAsyncCallback(AsyncCallbackDispatcher<SnapshotServiceImpl, CreateCmdResult> callback, CreateSnapshotContext<CreateCmdResult> context) {
        CreateCmdResult result = callback.getResult();
        SnapshotObject snapshot = (SnapshotObject)context.snapshot;
        AsyncCallFuture<SnapshotResult> future = context.future;
        SnapshotResult snapResult = new SnapshotResult(snapshot, result.getAnswer());
        if (result.isFailed()) {
            s_logger.debug("create snapshot " + context.snapshot.getName() + " failed: " + result.getResult());
            try {
                snapshot.processEvent(Snapshot.Event.OperationFailed);
                snapshot.processEvent(Event.OperationFailed);
            } catch (Exception e) {
                s_logger.debug("Failed to update snapshot state due to " + e.getMessage());
            }

            snapResult.setResult(result.getResult());
            future.complete(snapResult);
            return null;
        }

        try {
            snapshot.processEvent(Event.OperationSuccessed, result.getAnswer());
            snapshot.processEvent(Snapshot.Event.OperationSucceeded);
        } catch (Exception e) {
            s_logger.debug("Failed to create snapshot: ", e);
            snapResult.setResult(e.toString());
            try {
                snapshot.processEvent(Snapshot.Event.OperationFailed);
            } catch (NoTransitionException e1) {
                s_logger.debug("Failed to change snapshot state: " + e1.toString());
            }
        }

        future.complete(snapResult);
        return null;
    }

    @Override
    public SnapshotResult takeSnapshot(SnapshotInfo snap) {
        SnapshotObject snapshot = (SnapshotObject)snap;

        SnapshotObject snapshotOnPrimary = null;
        try {
            snapshotOnPrimary = (SnapshotObject)snap.getDataStore().create(snapshot);
        } catch (Exception e) {
            s_logger.debug("Failed to create snapshot state on data store due to " + e.getMessage());
            throw new CloudRuntimeException(e);
        }

        try {
            snapshotOnPrimary.processEvent(Snapshot.Event.CreateRequested);
        } catch (NoTransitionException e) {
            s_logger.debug("Failed to change snapshot state: " + e.toString());
            throw new CloudRuntimeException(e);
        }

        try {
            snapshotOnPrimary.processEvent(Event.CreateOnlyRequested);
        } catch (Exception e) {
            s_logger.debug("Failed to change snapshot state: " + e.toString());
            try {
                snapshotOnPrimary.processEvent(Snapshot.Event.OperationFailed);
            } catch (NoTransitionException e1) {
                s_logger.debug("Failed to change snapshot state: " + e1.toString());
            }
            throw new CloudRuntimeException(e);
        }

        AsyncCallFuture<SnapshotResult> future = new AsyncCallFuture<SnapshotResult>();
        try {
            CreateSnapshotContext<CommandResult> context = new CreateSnapshotContext<CommandResult>(null, snap.getBaseVolume(), snapshotOnPrimary, future);
            AsyncCallbackDispatcher<SnapshotServiceImpl, CreateCmdResult> caller = AsyncCallbackDispatcher.create(this);
            caller.setCallback(caller.getTarget().createSnapshotAsyncCallback(null, null)).setContext(context);
            PrimaryDataStoreDriver primaryStore = (PrimaryDataStoreDriver)snapshotOnPrimary.getDataStore().getDriver();
            primaryStore.takeSnapshot(snapshot, caller);
        } catch (Exception e) {
            s_logger.debug("Failed to take snapshot: " + snapshot.getId(), e);
            try {
                snapshot.processEvent(Snapshot.Event.OperationFailed);
                snapshot.processEvent(Event.OperationFailed);
            } catch (NoTransitionException e1) {
                s_logger.debug("Failed to change state for event: OperationFailed", e);
            }
            throw new CloudRuntimeException("Failed to take snapshot" + snapshot.getId());
        }

        SnapshotResult result;

        try {
            result = future.get();
            UsageEventUtils.publishUsageEvent(EventTypes.EVENT_SNAPSHOT_ON_PRIMARY, snap.getAccountId(), snap.getDataCenterId(), snap.getId(),
                    snap.getName(), null, null, snapshotOnPrimary.getSize(), snapshotOnPrimary.getSize(), snap.getClass().getName(), snap.getUuid());
            return result;
        } catch (InterruptedException e) {
            s_logger.debug("Failed to create snapshot", e);
            throw new CloudRuntimeException("Failed to create snapshot", e);
        } catch (ExecutionException e) {
            s_logger.debug("Failed to create snapshot", e);
            throw new CloudRuntimeException("Failed to create snapshot", e);
        }
    }

    // if a snapshot has parent snapshot, the new snapshot should be stored in
    // the same store as its parent since
    // we are taking delta snapshot
    private DataStore findSnapshotImageStore(SnapshotInfo snapshot) {
        Boolean fullSnapshot = true;
        Boolean snapshotFullBackup = snapshot.getFullBackup();
        if (snapshotFullBackup != null) {
            fullSnapshot = snapshotFullBackup;
        }
        if (fullSnapshot) {
            return dataStoreMgr.getImageStoreWithFreeCapacity(snapshot.getDataCenterId());
        } else {
            SnapshotInfo parentSnapshot = snapshot.getParent();
            // Note that DataStore information in parentSnapshot is for primary
            // data store here, we need to
            // find the image store where the parent snapshot backup is located
            SnapshotDataStoreVO parentSnapshotOnBackupStore = null;
            if (parentSnapshot != null) {
                parentSnapshotOnBackupStore = _snapshotStoreDao.findBySnapshot(parentSnapshot.getId(), DataStoreRole.Image);
            }
            if (parentSnapshotOnBackupStore == null) {
                return dataStoreMgr.getImageStoreWithFreeCapacity(snapshot.getDataCenterId());
            }
            return dataStoreMgr.getDataStore(parentSnapshotOnBackupStore.getDataStoreId(), parentSnapshotOnBackupStore.getRole());
        }
    }

    @Override
    public SnapshotInfo backupSnapshot(SnapshotInfo snapshot) {
        SnapshotObject snapObj = (SnapshotObject)snapshot;
        AsyncCallFuture<SnapshotResult> future = new AsyncCallFuture<SnapshotResult>();
        SnapshotResult result = new SnapshotResult(snapshot, null);
        Snapshot.State origState = snapObj.getState();
        try {
            snapObj.processEvent(Snapshot.Event.BackupToSecondary);

            DataStore imageStore = findSnapshotImageStore(snapshot);
            if (imageStore == null) {
                throw new CloudRuntimeException("can not find an image stores");
            }

            SnapshotInfo snapshotOnImageStore = (SnapshotInfo)imageStore.create(snapshot);

            snapshotOnImageStore.processEvent(Event.CreateOnlyRequested);
            CopySnapshotContext<CommandResult> context = new CopySnapshotContext<CommandResult>(null, snapshot, snapshotOnImageStore, future);
            AsyncCallbackDispatcher<SnapshotServiceImpl, CopyCommandResult> caller = AsyncCallbackDispatcher.create(this);
            caller.setCallback(caller.getTarget().copySnapshotAsyncCallback(null, null)).setContext(context);
            motionSrv.copyAsync(snapshot, snapshotOnImageStore, caller);
        } catch (Exception e) {
            s_logger.debug("Failed to copy snapshot", e);
            result.setResult("Failed to copy snapshot:" + e.toString());
            try {
                // When error archiving an already existing snapshot, emit OperationNotPerformed.
                // This will ensure that the original snapshot does not get deleted
                if (origState.equals(Snapshot.State.BackedUp)) {
                    snapObj.processEvent(Snapshot.Event.OperationNotPerformed);
                } else {
                    snapObj.processEvent(Snapshot.Event.OperationFailed);
                }
            } catch (NoTransitionException e1) {
                s_logger.debug("Failed to change state: " + e1.toString());
            }
            future.complete(result);
        }

        try {
            SnapshotResult res = future.get();
            if (res.isFailed()) {
                throw new CloudRuntimeException(res.getResult());
            }
            SnapshotInfo destSnapshot = res.getSnapshot();
            return destSnapshot;
        } catch (InterruptedException e) {
            s_logger.debug("failed copy snapshot", e);
            throw new CloudRuntimeException("Failed to copy snapshot", e);
        } catch (ExecutionException e) {
            s_logger.debug("Failed to copy snapshot", e);
            throw new CloudRuntimeException("Failed to copy snapshot", e);
        }

    }

    protected Void copySnapshotAsyncCallback(AsyncCallbackDispatcher<SnapshotServiceImpl, CopyCommandResult> callback, CopySnapshotContext<CommandResult> context) {
        CopyCommandResult result = callback.getResult();
        SnapshotInfo destSnapshot = context.destSnapshot;
        SnapshotObject srcSnapshot = (SnapshotObject)context.srcSnapshot;
        Object payload = srcSnapshot.getPayload();
        CreateSnapshotPayload createSnapshotPayload = (CreateSnapshotPayload)payload;
        AsyncCallFuture<SnapshotResult> future = context.future;
        SnapshotResult snapResult = new SnapshotResult(destSnapshot, result.getAnswer());
        if (result.isFailed()) {
            try {
                if (createSnapshotPayload.getAsyncBackup()) {
                    destSnapshot.processEvent(Event.OperationFailed);
                    throw new SnapshotBackupException("Failed in creating backup of snapshot with ID "+srcSnapshot.getId());
                } else {
                    destSnapshot.processEvent(Event.OperationFailed);
                    //if backup snapshot failed, mark srcSnapshot in snapshot_store_ref as failed also
                    cleanupOnSnapshotBackupFailure(context.srcSnapshot);
                }
            } catch (SnapshotBackupException e) {
                s_logger.debug("Failed to create backup: " + e.toString());
            }
            snapResult.setResult(result.getResult());
            future.complete(snapResult);
            return null;
        }

        try {
            CopyCmdAnswer copyCmdAnswer = (CopyCmdAnswer)result.getAnswer();
            destSnapshot.processEvent(Event.OperationSuccessed, copyCmdAnswer);
            srcSnapshot.processEvent(Snapshot.Event.OperationSucceeded);
            snapResult = new SnapshotResult(_snapshotFactory.getSnapshot(destSnapshot.getId(), destSnapshot.getDataStore()), copyCmdAnswer);
            future.complete(snapResult);
        } catch (Exception e) {
            s_logger.debug("Failed to update snapshot state", e);
            snapResult.setResult(e.toString());
            future.complete(snapResult);
        }
        return null;
    }

    protected Void deleteSnapshotCallback(AsyncCallbackDispatcher<SnapshotServiceImpl, CommandResult> callback, DeleteSnapshotContext<CommandResult> context) {

        CommandResult result = callback.getResult();
        AsyncCallFuture<SnapshotResult> future = context.future;
        SnapshotInfo snapshot = context.snapshot;
        SnapshotResult res = null;
        try {
            if (result.isFailed()) {
                s_logger.debug("delete snapshot failed" + result.getResult());
                snapshot.processEvent(ObjectInDataStoreStateMachine.Event.OperationFailed);
                res = new SnapshotResult(context.snapshot, null);
                res.setResult(result.getResult());
            } else {
                snapshot.processEvent(ObjectInDataStoreStateMachine.Event.OperationSuccessed);
                res = new SnapshotResult(context.snapshot, null);
            }
        } catch (Exception e) {
            s_logger.debug("Failed to in deleteSnapshotCallback", e);
            res.setResult(e.toString());
        }
        future.complete(res);
        return null;
    }

    protected Void revertSnapshotCallback(AsyncCallbackDispatcher<SnapshotServiceImpl, CommandResult> callback, RevertSnapshotContext<CommandResult> context) {

        CommandResult result = callback.getResult();
        AsyncCallFuture<SnapshotResult> future = context.future;
        SnapshotResult res = null;
        try {
            if (result.isFailed()) {
                s_logger.debug("revert snapshot failed" + result.getResult());
                res = new SnapshotResult(context.snapshot, null);
                res.setResult(result.getResult());
            } else {
                res = new SnapshotResult(context.snapshot, null);
            }
        } catch (Exception e) {
            s_logger.debug("Failed to in revertSnapshotCallback", e);
            res.setResult(e.toString());
        }
        future.complete(res);
        return null;
    }

    @Override
    public boolean deleteSnapshot(SnapshotInfo snapInfo) {
        snapInfo.processEvent(ObjectInDataStoreStateMachine.Event.DestroyRequested);

        AsyncCallFuture<SnapshotResult> future = new AsyncCallFuture<SnapshotResult>();
        DeleteSnapshotContext<CommandResult> context = new DeleteSnapshotContext<CommandResult>(null, snapInfo, future);
        AsyncCallbackDispatcher<SnapshotServiceImpl, CommandResult> caller = AsyncCallbackDispatcher.create(this);
        caller.setCallback(caller.getTarget().deleteSnapshotCallback(null, null)).setContext(context);
        DataStore store = snapInfo.getDataStore();
        store.getDriver().deleteAsync(store, snapInfo, caller);

        SnapshotResult result = null;
        try {
            result = future.get();
            if (result.isFailed()) {
                throw new CloudRuntimeException(result.getResult());
            }
            return true;
        } catch (InterruptedException e) {
            s_logger.debug("delete snapshot is failed: " + e.toString());
        } catch (ExecutionException e) {
            s_logger.debug("delete snapshot is failed: " + e.toString());
        }

        return false;

    }

    @Override
    public boolean revertSnapshot(SnapshotInfo snapshot) {
        SnapshotInfo snapshotOnPrimaryStore = _snapshotFactory.getSnapshot(snapshot.getId(), DataStoreRole.Primary);
        if (snapshotOnPrimaryStore == null) {
            throw new CloudRuntimeException("Cannot find an entry for snapshot " + snapshot.getId() + " on primary storage pools");
        }
        PrimaryDataStore store = (PrimaryDataStore)snapshotOnPrimaryStore.getDataStore();

        AsyncCallFuture<SnapshotResult> future = new AsyncCallFuture<SnapshotResult>();
        RevertSnapshotContext<CommandResult> context = new RevertSnapshotContext<CommandResult>(null, snapshot, future);
        AsyncCallbackDispatcher<SnapshotServiceImpl, CommandResult> caller = AsyncCallbackDispatcher.create(this);
        caller.setCallback(caller.getTarget().revertSnapshotCallback(null, null)).setContext(context);

        ((PrimaryDataStoreDriver)store.getDriver()).revertSnapshot(snapshot, snapshotOnPrimaryStore, caller);

        SnapshotResult result = null;
        try {
            result = future.get();
            if (result.isFailed()) {
                throw new CloudRuntimeException(result.getResult());
            }
            return true;
        } catch (InterruptedException e) {
            s_logger.debug("revert snapshot is failed: " + e.toString());
        } catch (ExecutionException e) {
            s_logger.debug("revert snapshot is failed: " + e.toString());
        }

        return false;
    }

    // This routine is used to push snapshots currently on cache store, but not in region store to region store.
    // used in migrating existing NFS secondary storage to S3. We chose to push all volume related snapshots to handle delta snapshots smoothly.
    @Override
    public void syncVolumeSnapshotsToRegionStore(long volumeId, DataStore store) {
        if (dataStoreMgr.isRegionStore(store)) {
            // list all backed up snapshots for the given volume
            List<SnapshotVO> snapshots = _snapshotDao.listByStatus(volumeId, Snapshot.State.BackedUp);
            if (snapshots != null) {
                for (SnapshotVO snapshot : snapshots) {
                    syncSnapshotToRegionStore(snapshot.getId(), store);
                }
            }
        }
    }

    @Override
    public void cleanupVolumeDuringSnapshotFailure(Long volumeId, Long snapshotId) {
        SnapshotVO snaphsot = _snapshotDao.findById(snapshotId);

        if (snaphsot != null) {
            if (snaphsot.getState() != Snapshot.State.BackedUp) {
                List<SnapshotDataStoreVO> snapshotDataStoreVOs = _snapshotStoreDao.findBySnapshotId(snapshotId);
                for (SnapshotDataStoreVO snapshotDataStoreVO : snapshotDataStoreVOs) {
                    s_logger.debug("Remove snapshot " + snapshotId + ", status " + snapshotDataStoreVO.getState() +
                            " on snapshot_store_ref table with id: " + snapshotDataStoreVO.getId());

                    _snapshotStoreDao.remove(snapshotDataStoreVO.getId());
                }

                s_logger.debug("Remove snapshot " + snapshotId + " status " + snaphsot.getState() + " from snapshot table");
                _snapshotDao.remove(snapshotId);
            }
        }


    }

    // push one individual snapshots currently on cache store to region store if it is not there already
    private void syncSnapshotToRegionStore(long snapshotId, DataStore store){
        // if snapshot is already on region wide object store, check if it is really downloaded there (by checking install_path). Sync snapshot to region
        // wide store if it is not there physically.
        SnapshotInfo snapOnStore = _snapshotFactory.getSnapshot(snapshotId, store);
        if (snapOnStore == null) {
            throw new CloudRuntimeException("Cannot find an entry in snapshot_store_ref for snapshot " + snapshotId + " on region store: " + store.getName());
        }
        if (snapOnStore.getPath() == null || snapOnStore.getPath().length() == 0) {
            if (s_logger.isDebugEnabled()) {
                s_logger.debug("sync snapshot " + snapshotId + " from cache to object store...");
            }
            // snapshot is not on region store yet, sync to region store
            SnapshotInfo srcSnapshot = _snapshotFactory.getReadySnapshotOnCache(snapshotId);
            if (srcSnapshot == null) {
                throw new CloudRuntimeException("Cannot find snapshot " + snapshotId + "  on cache store");
            }
            AsyncCallFuture<SnapshotResult> future = syncToRegionStoreAsync(srcSnapshot, store);
            try {
                SnapshotResult result = future.get();
                if (result.isFailed()) {
                    throw new CloudRuntimeException("sync snapshot from cache to region wide store failed for image store " + store.getName() + ":"
                            + result.getResult());
                }
                _cacheMgr.releaseCacheObject(srcSnapshot); // reduce reference count for template on cache, so it can recycled by schedule
            } catch (Exception ex) {
                throw new CloudRuntimeException("sync snapshot from cache to region wide store failed for image store " + store.getName());
            }
        }

    }

    private AsyncCallFuture<SnapshotResult> syncToRegionStoreAsync(SnapshotInfo snapshot, DataStore store) {
        AsyncCallFuture<SnapshotResult> future = new AsyncCallFuture<SnapshotResult>();
        // no need to create entry on snapshot_store_ref here, since entries are already created when updateCloudToUseObjectStore is invoked.
        // But we need to set default install path so that sync can be done in the right s3 path
        SnapshotInfo snapshotOnStore = _snapshotFactory.getSnapshot(snapshot, store);
        String installPath = TemplateConstants.DEFAULT_SNAPSHOT_ROOT_DIR + "/"
                + snapshot.getAccountId() + "/" + snapshot.getVolumeId();
        ((SnapshotObject)snapshotOnStore).setPath(installPath);
        CopySnapshotContext<CommandResult> context = new CopySnapshotContext<CommandResult>(null, snapshot,
                snapshotOnStore, future);
        AsyncCallbackDispatcher<SnapshotServiceImpl, CopyCommandResult> caller = AsyncCallbackDispatcher
                .create(this);
        caller.setCallback(caller.getTarget().syncSnapshotCallBack(null, null)).setContext(context);
        motionSrv.copyAsync(snapshot, snapshotOnStore, caller);
        return future;
    }

    protected Void syncSnapshotCallBack(AsyncCallbackDispatcher<SnapshotServiceImpl, CopyCommandResult> callback,
            CopySnapshotContext<CommandResult> context) {
        CopyCommandResult result = callback.getResult();
        SnapshotInfo destSnapshot = context.destSnapshot;
        SnapshotResult res = new SnapshotResult(destSnapshot, null);

        AsyncCallFuture<SnapshotResult> future = context.future;
        try {
            if (result.isFailed()) {
                res.setResult(result.getResult());
                // no change to existing snapshot_store_ref, will try to re-sync later if other call triggers this sync operation
            } else {
                // this will update install path properly, next time it will not sync anymore.
                destSnapshot.processEvent(Event.OperationSuccessed, result.getAnswer());
            }
            future.complete(res);
        } catch (Exception e) {
            s_logger.debug("Failed to process sync snapshot callback", e);
            res.setResult(e.toString());
            future.complete(res);
        }

        return null;
    }

    @Override
    public void processEventOnSnapshotObject(SnapshotInfo snapshot, Snapshot.Event event) {
        SnapshotObject object = (SnapshotObject)snapshot;
        try {
            object.processEvent(event);
        } catch (NoTransitionException e) {
            s_logger.debug("Unable to update the state " + e.toString());
        }
    }

    @Override
    public void cleanupOnSnapshotBackupFailure(SnapshotInfo snapshot) {
        Transaction.execute(new TransactionCallbackNoReturn() {
            @Override
            public void doInTransactionWithoutResult(TransactionStatus status) {
                try {
                    SnapshotObject srcSnapshot = (SnapshotObject)snapshot;
                    srcSnapshot.processEvent(Event.DestroyRequested);
                    srcSnapshot.processEvent(Event.OperationSuccessed);

                    srcSnapshot.processEvent(Snapshot.Event.OperationFailed);

                    _snapshotDetailsDao.removeDetail(srcSnapshot.getId(), AsyncJob.Constants.MS_ID);
                    _snapshotDao.remove(srcSnapshot.getId());
                } catch (NoTransitionException ex) {
                    s_logger.debug("Failed to create backup " + ex.toString());
                    throw new CloudRuntimeException("Failed to backup snapshot" + snapshot.getId());
                }
            }
        });

    }

}
