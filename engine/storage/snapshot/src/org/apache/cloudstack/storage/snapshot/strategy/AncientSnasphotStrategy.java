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

package org.apache.cloudstack.storage.snapshot.strategy;

import java.util.List;
import java.util.concurrent.ExecutionException;

import javax.inject.Inject;

import org.apache.cloudstack.engine.subsystem.api.storage.CommandResult;
import org.apache.cloudstack.engine.subsystem.api.storage.CopyCommandResult;
import org.apache.cloudstack.engine.subsystem.api.storage.CreateCmdResult;
import org.apache.cloudstack.engine.subsystem.api.storage.DataObject;
import org.apache.cloudstack.engine.subsystem.api.storage.DataObjectInStore;
import org.apache.cloudstack.engine.subsystem.api.storage.DataObjectType;
import org.apache.cloudstack.engine.subsystem.api.storage.DataStore;
import org.apache.cloudstack.engine.subsystem.api.storage.DataStoreManager;
import org.apache.cloudstack.engine.subsystem.api.storage.DataStoreRole;
import org.apache.cloudstack.engine.subsystem.api.storage.ObjectInDataStoreStateMachine;
import org.apache.cloudstack.engine.subsystem.api.storage.ObjectInDataStoreStateMachine.Event;
import org.apache.cloudstack.engine.subsystem.api.storage.PrimaryDataStoreDriver;
import org.apache.cloudstack.engine.subsystem.api.storage.SnapshotDataFactory;
import org.apache.cloudstack.engine.subsystem.api.storage.SnapshotInfo;
import org.apache.cloudstack.engine.subsystem.api.storage.SnapshotStrategy;
import org.apache.cloudstack.engine.subsystem.api.storage.VolumeInfo;
import org.apache.cloudstack.engine.subsystem.api.storage.ZoneScope;
import org.apache.cloudstack.framework.async.AsyncCallFuture;
import org.apache.cloudstack.framework.async.AsyncCallbackDispatcher;
import org.apache.cloudstack.framework.async.AsyncCompletionCallback;
import org.apache.cloudstack.framework.async.AsyncRpcConext;
import org.apache.cloudstack.storage.datastore.ObjectInDataStoreManager;
import org.apache.cloudstack.storage.datastore.db.PrimaryDataStoreDao;
import org.apache.cloudstack.storage.motion.DataMotionService;
import org.apache.cloudstack.storage.snapshot.SnapshotObject;
import org.apache.cloudstack.storage.snapshot.SnapshotStateMachineManager;
import org.apache.log4j.Logger;
import org.springframework.stereotype.Component;

import com.cloud.agent.api.Answer;
import com.cloud.agent.api.BackupSnapshotAnswer;
import com.cloud.agent.api.DeleteSnapshotBackupCommand;
import com.cloud.agent.api.to.S3TO;
import com.cloud.agent.api.to.SwiftTO;
import com.cloud.configuration.Resource.ResourceType;
import com.cloud.configuration.dao.ConfigurationDao;
import com.cloud.dc.ClusterVO;
import com.cloud.dc.dao.ClusterDao;
import com.cloud.event.EventTypes;
import com.cloud.event.UsageEventUtils;
import com.cloud.exception.InvalidParameterValueException;
import com.cloud.host.HostVO;
import com.cloud.hypervisor.Hypervisor.HypervisorType;
import com.cloud.resource.ResourceManager;
import com.cloud.storage.Snapshot;
import com.cloud.storage.SnapshotVO;
import com.cloud.storage.StoragePool;
import com.cloud.storage.VolumeManager;
import com.cloud.storage.VolumeVO;
import com.cloud.storage.dao.SnapshotDao;
import com.cloud.storage.dao.VolumeDao;
import com.cloud.storage.s3.S3Manager;
import com.cloud.storage.snapshot.SnapshotManager;
import com.cloud.storage.swift.SwiftManager;
import com.cloud.utils.NumbersUtil;
import com.cloud.utils.db.DB;
import com.cloud.utils.db.Transaction;
import com.cloud.utils.exception.CloudRuntimeException;
import com.cloud.utils.fsm.NoTransitionException;
import com.cloud.vm.UserVmVO;
import com.cloud.vm.VirtualMachine.State;
import com.cloud.vm.dao.UserVmDao;
import com.cloud.vm.snapshot.VMSnapshot;
import com.cloud.vm.snapshot.VMSnapshotVO;
import com.cloud.vm.snapshot.dao.VMSnapshotDao;

@Component
public class AncientSnasphotStrategy implements SnapshotStrategy {
	private static final Logger s_logger = Logger.getLogger(AncientSnasphotStrategy.class);
	@Inject
	protected VolumeDao _volsDao;
	@Inject
	protected UserVmDao _vmDao;
	@Inject
	protected PrimaryDataStoreDao _storagePoolDao;
	@Inject
	protected ClusterDao _clusterDao;
	@Inject
	protected SnapshotDao snapshotDao;
	@Inject
	private ResourceManager _resourceMgr;
	@Inject
	protected SnapshotDao _snapshotDao;
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


	@Override
	public boolean canHandle(SnapshotInfo snapshot) {
		return true;
	}

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

	protected Void createSnapshotAsyncCallback(AsyncCallbackDispatcher<AncientSnasphotStrategy, CreateCmdResult> callback, 
			CreateSnapshotContext<CreateCmdResult> context) {
		CreateCmdResult result = callback.getResult();
		SnapshotObject snapshot = (SnapshotObject)context.snapshot;
		VolumeInfo volume = context.volume;
		AsyncCallFuture<SnapshotResult> future = context.future;
		SnapshotResult snapResult = new SnapshotResult(snapshot);
		if (result.isFailed()) {
			s_logger.debug("create snapshot " + context.snapshot.getName() + " failed: " + result.getResult());
			try {
				snapshot.processEvent(Snapshot.Event.OperationFailed);
			} catch (NoTransitionException nte) {
				s_logger.debug("Failed to update snapshot state due to " + nte.getMessage());
			}


			snapResult.setResult(result.getResult());
			future.complete(snapResult);
			return null;
		}

		try {
			SnapshotVO preSnapshotVO = this.snapshotMgr.getParentSnapshot(volume, snapshot);
			String preSnapshotPath = null;
			if (preSnapshotVO != null) {
			    preSnapshotPath = preSnapshotVO.getPath();
			}
			SnapshotVO snapshotVO = this.snapshotDao.findById(snapshot.getId());
			// The snapshot was successfully created
			if (preSnapshotPath != null && preSnapshotPath.equals(result.getPath())) {
				// empty snapshot
				s_logger.debug("CreateSnapshot: this is empty snapshot ");

				snapshotVO.setPath(preSnapshotPath);
				snapshotVO.setBackupSnapshotId(preSnapshotVO.getBackupSnapshotId());
				snapshotVO.setSwiftId(preSnapshotVO.getSwiftId());
				snapshotVO.setPrevSnapshotId(preSnapshotVO.getId());
				snapshotVO.setSecHostId(preSnapshotVO.getSecHostId());
				snapshot.processEvent(Snapshot.Event.OperationNotPerformed);
			} else {
				long preSnapshotId = 0;

				if (preSnapshotVO != null && preSnapshotVO.getBackupSnapshotId() != null) {
					preSnapshotId = preSnapshotVO.getId();
					int _deltaSnapshotMax = NumbersUtil.parseInt(_configDao.getValue("snapshot.delta.max"), SnapshotManager.DELTAMAX);
					int deltaSnap = _deltaSnapshotMax;

					int i;
					for (i = 1; i < deltaSnap; i++) {
						String prevBackupUuid = preSnapshotVO.getBackupSnapshotId();
						// previous snapshot doesn't have backup, create a full snapshot
						if (prevBackupUuid == null) {
							preSnapshotId = 0;
							break;
						}
						long preSSId = preSnapshotVO.getPrevSnapshotId();
						if (preSSId == 0) {
							break;
						}
						preSnapshotVO = _snapshotDao.findByIdIncludingRemoved(preSSId);
					}
					if (i >= deltaSnap) {
						preSnapshotId = 0;
					}
				}

				//If the volume is moved around, backup a full snapshot to secondary storage
				if (volume.getLastPoolId() != null && !volume.getLastPoolId().equals(volume.getPoolId())) {
					preSnapshotId = 0;
					//TODO: fix this hack
					VolumeVO volumeVO = this.volumeDao.findById(volume.getId());
					volumeVO.setLastPoolId(volume.getPoolId());
					this.volumeDao.update(volume.getId(), volumeVO);
				}

				snapshot.setPath(result.getPath());
				snapshot.setPrevSnapshotId(preSnapshotId);

				snapshot.processEvent(Snapshot.Event.OperationSucceeded);
				snapResult = new SnapshotResult(this.snapshotfactory.getSnapshot(snapshot.getId()));
			} 
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

	class SnapshotResult extends CommandResult {
		SnapshotInfo snashot;
		public SnapshotResult(SnapshotInfo snapshot) {
			this.snashot = snapshot;
		}
	}

	protected SnapshotInfo createSnapshotOnPrimary(VolumeInfo volume, Long snapshotId) {
		SnapshotObject snapshot = (SnapshotObject)this.snapshotfactory.getSnapshot(snapshotId);
		if (snapshot == null) {
			throw new CloudRuntimeException("Can not find snapshot " + snapshotId);
		}

		try {
			snapshot.processEvent(Snapshot.Event.CreateRequested);
		} catch (NoTransitionException nte) {
			s_logger.debug("Failed to update snapshot state due to " + nte.getMessage());
			throw new CloudRuntimeException("Failed to update snapshot state due to " + nte.getMessage());
		}

		AsyncCallFuture<SnapshotResult> future = new AsyncCallFuture<SnapshotResult>();
		try {
		    CreateSnapshotContext<CommandResult> context = new CreateSnapshotContext<CommandResult>(
		            null, volume, snapshot, future);
		    AsyncCallbackDispatcher<AncientSnasphotStrategy, CreateCmdResult> caller = AsyncCallbackDispatcher
		            .create(this);
		    caller.setCallback(
		            caller.getTarget().createSnapshotAsyncCallback(null, null))
		            .setContext(context);
		    PrimaryDataStoreDriver primaryStore = (PrimaryDataStoreDriver)volume.getDataStore().getDriver();
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
			return result.snashot;
		} catch (InterruptedException e) {
			s_logger.debug("Failed to create snapshot", e);
			throw new CloudRuntimeException("Failed to create snapshot", e);
		} catch (ExecutionException e) {
			s_logger.debug("Failed to create snapshot", e);
			throw new CloudRuntimeException("Failed to create snapshot", e);
		}

	}

	private boolean hostSupportSnapsthot(HostVO host) {
		if (host.getHypervisorType() != HypervisorType.KVM) {
			return true;
		}
		// Determine host capabilities
		String caps = host.getCapabilities();

		if (caps != null) {
			String[] tokens = caps.split(",");
			for (String token : tokens) {
				if (token.contains("snapshot")) {
					return true;
				}
			}
		}
		return false;
	}

	protected boolean supportedByHypervisor(VolumeInfo volume) {
		if (volume.getHypervisorType().equals(HypervisorType.KVM)) {
			StoragePool storagePool = (StoragePool)volume.getDataStore();
			ClusterVO cluster = _clusterDao.findById(storagePool.getClusterId());
			List<HostVO> hosts = _resourceMgr.listAllHostsInCluster(cluster.getId());
			if (hosts != null && !hosts.isEmpty()) {
				HostVO host = hosts.get(0);
				if (!hostSupportSnapsthot(host)) {
					throw new CloudRuntimeException("KVM Snapshot is not supported on cluster: " + host.getId());
				}
			}
		}

		// if volume is attached to a vm in destroyed or expunging state; disallow
		if (volume.getInstanceId() != null) {
			UserVmVO userVm = _vmDao.findById(volume.getInstanceId());
			if (userVm != null) {
				if (userVm.getState().equals(State.Destroyed) || userVm.getState().equals(State.Expunging)) {
					throw new CloudRuntimeException("Creating snapshot failed due to volume:" + volume.getId() + " is associated with vm:" + userVm.getInstanceName() + " is in "
							+ userVm.getState().toString() + " state");
				}

				if(userVm.getHypervisorType() == HypervisorType.VMware || userVm.getHypervisorType() == HypervisorType.KVM) {
					List<SnapshotVO> activeSnapshots = _snapshotDao.listByInstanceId(volume.getInstanceId(), Snapshot.State.Creating,  Snapshot.State.CreatedOnPrimary,  Snapshot.State.BackingUp);
					if(activeSnapshots.size() > 1)
						throw new CloudRuntimeException("There is other active snapshot tasks on the instance to which the volume is attached, please try again later");
				}
				
				List<VMSnapshotVO> activeVMSnapshots = _vmSnapshotDao.listByInstanceId(userVm.getId(),
                        VMSnapshot.State.Creating, VMSnapshot.State.Reverting, VMSnapshot.State.Expunging);
                if (activeVMSnapshots.size() > 0) {
                    throw new CloudRuntimeException(
                            "There is other active vm snapshot tasks on the instance to which the volume is attached, please try again later");
                }           
			}
		}

		return true;
	}

	@Override
	public SnapshotInfo takeSnapshot(VolumeInfo volume, Long snapshotId) {

		supportedByHypervisor(volume);

		SnapshotInfo snapshot = createSnapshotOnPrimary(volume, snapshotId);
		return snapshot;
	}

	@Override
	public SnapshotInfo backupSnapshot(SnapshotInfo snapshot) {
		SnapshotObject snapObj = (SnapshotObject)snapshot;
		AsyncCallFuture<SnapshotResult> future = new AsyncCallFuture<SnapshotResult>();
		SnapshotResult result = new SnapshotResult(snapshot);
		try {

			snapObj.processEvent(Snapshot.Event.BackupToSecondary);

			ZoneScope scope = new ZoneScope(snapshot.getDataCenterId());
			List<DataStore> stores = this.dataStoreMgr.getImageStores(scope);
			if (stores.size() != 1) {
				throw new CloudRuntimeException("find out more than one image stores");
			}

			DataStore imageStore = stores.get(0);
			SnapshotInfo snapshotOnImageStore = (SnapshotInfo)imageStore.create(snapshot);

			snapshotOnImageStore.processEvent(Event.CreateOnlyRequested);
			CopySnapshotContext<CommandResult> context = new CopySnapshotContext<CommandResult>(null, snapshot,
					snapshotOnImageStore, future);
			AsyncCallbackDispatcher<AncientSnasphotStrategy, CopyCommandResult> caller = AsyncCallbackDispatcher
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
			SnapshotInfo destSnapshot = res.snashot;
			return destSnapshot;
		} catch (InterruptedException e) {
			s_logger.debug("failed copy snapshot", e);
			throw new CloudRuntimeException("Failed to copy snapshot" , e);
		} catch (ExecutionException e) {
			s_logger.debug("Failed to copy snapshot", e);
			throw new CloudRuntimeException("Failed to copy snapshot" , e);
		}

	}

	protected Void copySnapshotAsyncCallback(AsyncCallbackDispatcher<AncientSnasphotStrategy, CopyCommandResult> callback, 
			CopySnapshotContext<CommandResult> context) {
		CopyCommandResult result = callback.getResult();
		SnapshotInfo destSnapshot = context.destSnapshot;
		SnapshotObject srcSnapshot = (SnapshotObject)context.srcSnapshot;
		AsyncCallFuture<SnapshotResult> future = context.future;
		SnapshotResult snapResult = new SnapshotResult(destSnapshot);
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
			snapResult = new SnapshotResult(this.snapshotfactory.getSnapshot(destSnapshot.getId()));
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
		DataStore store = objInStoreMgr.findStore(snapshot.getUuid(), DataObjectType.SNAPSHOT, DataStoreRole.Image);
		if (store == null) {
			s_logger.debug("Can't find snapshot" + snapshot.getId() + " backed up into image store");
			return false;
		}

		try {
			SnapshotInfo snapshotInfo = this.snapshotfactory.getSnapshot(snapshot.getId(), store);
			snapshotInfo.processEvent(ObjectInDataStoreStateMachine.Event.DestroyRequested);
			
			AsyncCallFuture<SnapshotResult> future = new AsyncCallFuture<SnapshotResult>();
			DeleteSnapshotContext<CommandResult> context = new DeleteSnapshotContext<CommandResult>(null,
					snapshotInfo, future);
			AsyncCallbackDispatcher<AncientSnasphotStrategy, CommandResult> caller = AsyncCallbackDispatcher
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
	
	protected Void deleteSnapshotCallback(AsyncCallbackDispatcher<AncientSnasphotStrategy, CommandResult> callback, 
			DeleteSnapshotContext<CommandResult> context) {
		CommandResult result = callback.getResult();
		AsyncCallFuture<SnapshotResult> future = context.future;
		SnapshotInfo snapshot = context.snapshot;
		if (result.isFailed()) {
			s_logger.debug("delete snapshot failed" + result.getResult());
			snapshot.processEvent(ObjectInDataStoreStateMachine.Event.OperationFailed);
			SnapshotResult res = new SnapshotResult(context.snapshot);
			future.complete(res);
			return null;
		}
		snapshot.processEvent(ObjectInDataStoreStateMachine.Event.OperationSuccessed);
		SnapshotResult res = new SnapshotResult(context.snapshot);
		future.complete(res);
		return null;
	}

	@Override
	public boolean deleteSnapshot(SnapshotInfo snapInfo) {
		Long snapshotId = snapInfo.getId();
		SnapshotObject snapshot = (SnapshotObject)snapInfo;

		if (!Snapshot.State.BackedUp.equals(snapshot.getState())) {
			throw new InvalidParameterValueException("Can't delete snapshotshot " + snapshotId + " due to it is not in BackedUp Status");
		}
		
		if (s_logger.isDebugEnabled()) {
			s_logger.debug("Calling deleteSnapshot for snapshotId: " + snapshotId);
		}
		SnapshotVO lastSnapshot = null;
		if (snapshot.getBackupSnapshotId() != null) {
			List<SnapshotVO> snaps = _snapshotDao.listByBackupUuid(snapshot.getVolumeId(), snapshot.getBackupSnapshotId());
			if (snaps != null && snaps.size() > 1) {
				snapshot.setBackupSnapshotId(null);
				SnapshotVO snapshotVO = this._snapshotDao.findById(snapshotId);
				_snapshotDao.update(snapshot.getId(), snapshotVO);
			}
		}
		
		_snapshotDao.remove(snapshotId);

		long lastId = snapshotId;
		boolean destroy = false;
		while (true) {
			lastSnapshot = _snapshotDao.findNextSnapshot(lastId);
			if (lastSnapshot == null) {
				// if all snapshots after this snapshot in this chain are removed, remove those snapshots.
				destroy = true;
				break;
			}
			if (lastSnapshot.getRemoved() == null) {
				// if there is one child not removed, then can not remove back up snapshot.
				break;
			}
			lastId = lastSnapshot.getId();
		}
		if (destroy) {
			lastSnapshot = _snapshotDao.findByIdIncludingRemoved(lastId);
			while (lastSnapshot.getRemoved() != null) {
				String BackupSnapshotId = lastSnapshot.getBackupSnapshotId();
				if (BackupSnapshotId != null) {
					List<SnapshotVO> snaps = _snapshotDao.listByBackupUuid(lastSnapshot.getVolumeId(), BackupSnapshotId);
					if (snaps != null && snaps.size() > 1) {
						lastSnapshot.setBackupSnapshotId(null);
						_snapshotDao.update(lastSnapshot.getId(), lastSnapshot);
					} else {
						if (destroySnapshotBackUp(lastSnapshot)) {

						} else {
							s_logger.debug("Destroying snapshot backup failed " + lastSnapshot);
							break;
						}
					}
				}
				lastId = lastSnapshot.getPrevSnapshotId();
				if (lastId == 0) {
					break;
				}
				lastSnapshot = _snapshotDao.findByIdIncludingRemoved(lastId);
			}
		}
		return true;

	}

	@Override
	public boolean revertSnapshot(SnapshotInfo snapshot) {
		// TODO Auto-generated method stub
		return false;
	}

}
