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

import org.apache.cloudstack.engine.subsystem.api.storage.CommandResult;
import org.apache.cloudstack.engine.subsystem.api.storage.CopyCommandResult;
import org.apache.cloudstack.engine.subsystem.api.storage.CreateCmdResult;
import org.apache.cloudstack.engine.subsystem.api.storage.DataMotionService;
import org.apache.cloudstack.engine.subsystem.api.storage.DataObjectInStore;
import org.apache.cloudstack.engine.subsystem.api.storage.DataStore;
import org.apache.cloudstack.engine.subsystem.api.storage.DataStoreManager;
import org.apache.cloudstack.engine.subsystem.api.storage.ObjectInDataStoreStateMachine;
import org.apache.cloudstack.engine.subsystem.api.storage.ObjectInDataStoreStateMachine.Event;
import org.apache.cloudstack.engine.subsystem.api.storage.PrimaryDataStoreDriver;
import org.apache.cloudstack.engine.subsystem.api.storage.SnapshotDataFactory;
import org.apache.cloudstack.engine.subsystem.api.storage.SnapshotInfo;
import org.apache.cloudstack.engine.subsystem.api.storage.SnapshotService;
import org.apache.cloudstack.engine.subsystem.api.storage.VolumeInfo;
import org.apache.cloudstack.engine.subsystem.api.storage.ZoneScope;
import org.apache.cloudstack.engine.subsystem.api.storage.SnapshotResult;
import org.apache.cloudstack.framework.async.AsyncCallFuture;
import org.apache.cloudstack.framework.async.AsyncCallbackDispatcher;
import org.apache.cloudstack.framework.async.AsyncCompletionCallback;
import org.apache.cloudstack.framework.async.AsyncRpcConext;
import org.apache.cloudstack.storage.datastore.ObjectInDataStoreManager;
import org.apache.cloudstack.storage.datastore.db.PrimaryDataStoreDao;
import org.apache.cloudstack.storage.datastore.db.SnapshotDataStoreDao;
import org.apache.cloudstack.storage.datastore.db.SnapshotDataStoreVO;
import org.apache.log4j.Logger;
import org.springframework.stereotype.Component;

import com.cloud.agent.api.BackupSnapshotAnswer;
import com.cloud.configuration.dao.ConfigurationDao;
import com.cloud.dc.dao.ClusterDao;
import com.cloud.exception.InvalidParameterValueException;
import com.cloud.storage.DataStoreRole;
import com.cloud.storage.Snapshot;
import com.cloud.storage.SnapshotVO;
import com.cloud.storage.VolumeManager;
import com.cloud.storage.dao.SnapshotDao;
import com.cloud.storage.dao.VolumeDao;
import com.cloud.storage.snapshot.SnapshotManager;
import com.cloud.utils.db.DB;
import com.cloud.utils.exception.CloudRuntimeException;
import com.cloud.utils.fsm.NoTransitionException;
import com.cloud.vm.dao.UserVmDao;
import com.cloud.vm.snapshot.dao.VMSnapshotDao;

@Component
public class SnapshotServiceImpl implements SnapshotService {
	private static final Logger s_logger = Logger.getLogger(SnapshotServiceImpl.class);
	@Inject
	protected VolumeDao _volsDao;
	@Inject
	protected UserVmDao _vmDao;
	@Inject
	protected PrimaryDataStoreDao _storagePoolDao;
	@Inject
	protected ClusterDao _clusterDao;
	@Inject
	protected SnapshotDao _snapshotDao;
	@Inject
	protected SnapshotDataStoreDao _snapshotStoreDao;

	@Inject
	protected SnapshotManager snapshotMgr;
	@Inject
	protected VolumeManager volumeMgr;
	@Inject
	private ConfigurationDao _configDao;
	@Inject
	protected SnapshotStateMachineManager stateMachineManager;
	@Inject
	private VolumeDao volumeDao;
	@Inject
	SnapshotDataFactory snapshotfactory;
	@Inject
	DataStoreManager dataStoreMgr;
	@Inject
	DataMotionService motionSrv;
	@Inject
	ObjectInDataStoreManager objInStoreMgr;
	@Inject
	VMSnapshotDao _vmSnapshotDao;




	static private class CreateSnapshotContext<T> extends AsyncRpcConext<T> {
		final VolumeInfo volume;
		final SnapshotInfo snapshot;
		final AsyncCallFuture<SnapshotResult> future;
		public CreateSnapshotContext(AsyncCompletionCallback<T> callback, VolumeInfo volume,
				SnapshotInfo snapshot,
				AsyncCallFuture<SnapshotResult> future) {
			super(callback);
			this.volume = volume;
			this.snapshot = snapshot;
			this.future = future;
		}
	}

	static private class DeleteSnapshotContext<T> extends AsyncRpcConext<T> {
		final SnapshotInfo snapshot;
		final AsyncCallFuture<SnapshotResult> future;
		public DeleteSnapshotContext(AsyncCompletionCallback<T> callback, SnapshotInfo snapshot,
				AsyncCallFuture<SnapshotResult> future) {
			super(callback);
			this.snapshot = snapshot;
			this.future = future;
		}

	}

	static private class CopySnapshotContext<T> extends AsyncRpcConext<T> {
		final SnapshotInfo srcSnapshot;
		final SnapshotInfo destSnapshot;
		final AsyncCallFuture<SnapshotResult> future;
		public CopySnapshotContext(AsyncCompletionCallback<T> callback,
				SnapshotInfo srcSnapshot,
				SnapshotInfo destSnapshot,
				AsyncCallFuture<SnapshotResult> future) {
			super(callback);
			this.srcSnapshot = srcSnapshot;
			this.destSnapshot = destSnapshot;
			this.future = future;
		}

	}

	protected Void createSnapshotAsyncCallback(AsyncCallbackDispatcher<SnapshotServiceImpl, CreateCmdResult> callback,
			CreateSnapshotContext<CreateCmdResult> context) {
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
		} catch(Exception e) {
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
		    CreateSnapshotContext<CommandResult> context = new CreateSnapshotContext<CommandResult>(
		            null, snap.getBaseVolume(), snapshotOnPrimary, future);
		    AsyncCallbackDispatcher<SnapshotServiceImpl, CreateCmdResult> caller = AsyncCallbackDispatcher
		            .create(this);
		    caller.setCallback(
		            caller.getTarget().createSnapshotAsyncCallback(null, null))
		            .setContext(context);
		    PrimaryDataStoreDriver primaryStore = (PrimaryDataStoreDriver)snapshotOnPrimary.getDataStore().getDriver();
		    primaryStore.takeSnapshot(snapshot, caller);
		} catch (Exception e) {
		    s_logger.debug("Failed to take snapshot: " + snapshot.getId(), e);
		    try {
                snapshot.processEvent(Snapshot.Event.OperationFailed);
            } catch (NoTransitionException e1) {
                s_logger.debug("Failed to change state for event: OperationFailed" , e);
            }
		    throw new CloudRuntimeException("Failed to take snapshot" + snapshot.getId());
		}

		SnapshotResult result;

		try {
			result = future.get();
			if (result.isFailed()) {
				s_logger.debug("Failed to create snapshot:" + result.getResult());
				throw new CloudRuntimeException(result.getResult());
			}
			return result;
		} catch (InterruptedException e) {
			s_logger.debug("Failed to create snapshot", e);
			throw new CloudRuntimeException("Failed to create snapshot", e);
		} catch (ExecutionException e) {
			s_logger.debug("Failed to create snapshot", e);
			throw new CloudRuntimeException("Failed to create snapshot", e);
		}

	}

	@Override
	public SnapshotInfo backupSnapshot(SnapshotInfo snapshot) {
		SnapshotObject snapObj = (SnapshotObject)snapshot;
		AsyncCallFuture<SnapshotResult> future = new AsyncCallFuture<SnapshotResult>();
		SnapshotResult result = new SnapshotResult(snapshot, null);
		try {

			snapObj.processEvent(Snapshot.Event.BackupToSecondary);

			ZoneScope scope = new ZoneScope(snapshot.getDataCenterId());
			List<DataStore> stores = this.dataStoreMgr.getImageStoresByScope(scope);
			if (stores.size() != 1) {
				throw new CloudRuntimeException("find out more than one image stores");
			}

			DataStore imageStore = stores.get(0);
			SnapshotInfo snapshotOnImageStore = (SnapshotInfo)imageStore.create(snapshot);

			snapshotOnImageStore.processEvent(Event.CreateOnlyRequested);
			CopySnapshotContext<CommandResult> context = new CopySnapshotContext<CommandResult>(null, snapshot,
					snapshotOnImageStore, future);
			AsyncCallbackDispatcher<SnapshotServiceImpl, CopyCommandResult> caller = AsyncCallbackDispatcher
					.create(this);
			caller.setCallback(
					caller.getTarget().copySnapshotAsyncCallback(null, null))
					.setContext(context);
			this.motionSrv.copyAsync(snapshot, snapshotOnImageStore, caller);
		} catch (Exception e) {
			s_logger.debug("Failed to copy snapshot", e);
			result.setResult("Failed to copy snapshot:" +e.toString());
			try {
                snapObj.processEvent(Snapshot.Event.OperationFailed);
            } catch (NoTransitionException e1) {
                s_logger.debug("Failed to change state: " + e1.toString());
            }
			future.complete(result);
		}

		try {
			SnapshotResult res = future.get();
			SnapshotInfo destSnapshot = res.getSnashot();
			return destSnapshot;
		} catch (InterruptedException e) {
			s_logger.debug("failed copy snapshot", e);
			throw new CloudRuntimeException("Failed to copy snapshot" , e);
		} catch (ExecutionException e) {
			s_logger.debug("Failed to copy snapshot", e);
			throw new CloudRuntimeException("Failed to copy snapshot" , e);
		}

	}

	protected Void copySnapshotAsyncCallback(AsyncCallbackDispatcher<SnapshotServiceImpl, CopyCommandResult> callback,
			CopySnapshotContext<CommandResult> context) {
		CopyCommandResult result = callback.getResult();
		SnapshotInfo destSnapshot = context.destSnapshot;
		SnapshotObject srcSnapshot = (SnapshotObject)context.srcSnapshot;
		AsyncCallFuture<SnapshotResult> future = context.future;
		SnapshotResult snapResult = new SnapshotResult(destSnapshot, result.getAnswer());
		if (result.isFailed()) {
			snapResult.setResult(result.getResult());
			future.complete(snapResult);
			return null;
		}

		try {
			BackupSnapshotAnswer answer = (BackupSnapshotAnswer)result.getAnswer();

			DataObjectInStore dataInStore =  objInStoreMgr.findObject(destSnapshot, destSnapshot.getDataStore());
			dataInStore.setInstallPath(answer.getBackupSnapshotName());
			objInStoreMgr.update(destSnapshot, Event.OperationSuccessed);

			srcSnapshot.processEvent(Snapshot.Event.OperationSucceeded);
			snapResult = new SnapshotResult(this.snapshotfactory.getSnapshot(destSnapshot.getId(), destSnapshot.getDataStore()), answer);
			future.complete(snapResult);
		} catch (Exception e) {
			s_logger.debug("Failed to update snapshot state", e);
			snapResult.setResult(e.toString());
			future.complete(snapResult);
		}
		return null;
	}

	@DB
	protected boolean destroySnapshotBackUp(SnapshotVO snapshot) {
	    SnapshotDataStoreVO snapshotStore = this._snapshotStoreDao.findBySnapshot(snapshot.getId(), DataStoreRole.Image);
	    if ( snapshotStore == null ){
            s_logger.debug("Can't find snapshot" + snapshot.getId() + " backed up into image store");
            return false;
	    }
		DataStore store = this.dataStoreMgr.getDataStore(snapshotStore.getDataStoreId(), DataStoreRole.Image);
		if (store == null) {
			s_logger.debug("Can't find mage store " + snapshotStore.getDataStoreId());
			return false;
		}

		try {
			SnapshotInfo snapshotInfo = this.snapshotfactory.getSnapshot(snapshot.getId(), store);
			snapshotInfo.processEvent(ObjectInDataStoreStateMachine.Event.DestroyRequested);

			AsyncCallFuture<SnapshotResult> future = new AsyncCallFuture<SnapshotResult>();
			DeleteSnapshotContext<CommandResult> context = new DeleteSnapshotContext<CommandResult>(null,
					snapshotInfo, future);
			AsyncCallbackDispatcher<SnapshotServiceImpl, CommandResult> caller = AsyncCallbackDispatcher
					.create(this);
			caller.setCallback(
					caller.getTarget().deleteSnapshotCallback(null, null))
					.setContext(context);

			store.getDriver().deleteAsync(snapshotInfo, caller);

			SnapshotResult result = future.get();
			if (result.isFailed()) {
				s_logger.debug("Failed to delete snapsoht: " + result.getResult());
			}
			return result.isSuccess();
		} catch (Exception e) {
			s_logger.debug("Failed to delete snapshot", e);
			return false;
		}
	}

	protected Void deleteSnapshotCallback(AsyncCallbackDispatcher<SnapshotServiceImpl, CommandResult> callback,
			DeleteSnapshotContext<CommandResult> context) {
		CommandResult result = callback.getResult();
		AsyncCallFuture<SnapshotResult> future = context.future;
		SnapshotInfo snapshot = context.snapshot;
		if (result.isFailed()) {
			s_logger.debug("delete snapshot failed" + result.getResult());
			snapshot.processEvent(ObjectInDataStoreStateMachine.Event.OperationFailed);
			SnapshotResult res = new SnapshotResult(context.snapshot, null);
			future.complete(res);
			return null;
		}
		snapshot.processEvent(ObjectInDataStoreStateMachine.Event.OperationSuccessed);
		SnapshotResult res = new SnapshotResult(context.snapshot, null);
		future.complete(res);
		return null;
	}

	@Override
	public boolean deleteSnapshot(SnapshotInfo snapInfo) {
		

	}

	@Override
	public boolean revertSnapshot(SnapshotInfo snapshot) {
		// TODO Auto-generated method stub
		return false;
	}

}
