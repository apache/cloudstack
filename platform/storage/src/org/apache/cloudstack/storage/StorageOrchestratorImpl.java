/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.cloudstack.storage;

import java.util.List;

import org.apache.cloudstack.platform.subsystem.api.storage.DataStore;
import org.apache.cloudstack.platform.subsystem.api.storage.StorageProvider;
import org.apache.cloudstack.platform.subsystem.api.storage.VolumeStrategy;
import org.apache.cloudstack.storage.volume.VolumeManager;
import org.apache.log4j.Logger;

import com.cloud.deploy.DeploymentPlan;
import com.cloud.offering.DiskOffering;
import com.cloud.storage.DiskOfferingVO;
import com.cloud.storage.StoragePool;
import com.cloud.storage.Volume;
import com.cloud.storage.VolumeHostVO;
import com.cloud.storage.VolumeVO;
import com.cloud.storage.dao.DiskOfferingDao;
import com.cloud.storage.dao.StoragePoolDao;
import com.cloud.storage.dao.VolumeDao;
import com.cloud.storage.dao.VolumeHostDao;
import com.cloud.utils.component.Inject;
import com.cloud.utils.db.DB;
import com.cloud.utils.db.Transaction;
import com.cloud.utils.exception.CloudRuntimeException;
import com.cloud.utils.fsm.NoTransitionException;
import com.cloud.vm.VirtualMachine;
import com.cloud.vm.dao.VMInstanceDao;

public class StorageOrchestratorImpl implements StorageOrchestrator {
	private static final Logger s_logger = Logger.getLogger(StorageOrchestratorImpl.class);
	@Inject
	StoragePoolDao _storagePoolDao;
	@Inject
	StorageProviderManager _spManager;
	@Inject
	VolumeDao _volumeDao;
	@Inject
	VMInstanceDao _vmDao;
	@Inject
	DiskOfferingDao _diskOfferingDao;
	@Inject
	VolumeHostDao _volumeHostDao;
	@Inject
	StorageProviderManager _storageProviderMgr;
	@Inject
	VolumeManager _volumeMgr;
	
	protected Volume copyVolumeFromBackupStorage(VolumeVO volume, DataStore destStore, String reservationId) {
		StorageProvider sp = _storageProviderMgr.getBackupStorageProvider(volume.getDataCenterId());
		
		VolumeHostVO volumeHostVO = _volumeHostDao.findByVolumeId(volume.getId());
		long poolId = volumeHostVO.getHostId();
		DataStore srcStore = _storageProviderMgr.getDataStore(poolId);
		
		
		
	}
	
	protected Volume migrateVolume(VolumeVO volume, DataStore srcStore, DataStore destStore, String reservationId) throws NoTransitionException {
		VolumeStrategy vs = srcStore.getVolumeStrategy();
		
		Transaction txn = Transaction.currentTxn();
		txn.start();
		volume.setReservationId(reservationId);
		volume = _volumeMgr.processEvent(volume, Volume.Event.MigrationRequested);
		Volume destVolume = _volumeMgr.allocateDuplicateVolume(volume);
		destVolume = _volumeMgr.processEvent(destVolume, Volume.Event.CreateRequested);
		txn.commit();
		
		vs.migrateVolume(volume, srcStore, destVolume, destStore);
		
		txn.start();
		volume = _volumeMgr.processEvent(volume, Volume.Event.OperationSucceeded);
		destVolume = _volumeMgr.processEvent(destVolume, Volume.Event.OperationSucceeded);
		txn.commit();
		_volumeDao.remove(volume.getId());
		return destVolume;
	}
	
	@DB
	protected void prepareVolumes(List<VolumeVO> vols, Long destPoolId, String reservationId) throws NoTransitionException {
		DataStore destStore = null;
		if (destPoolId != null) {
			destStore = _storageProviderMgr.getDataStore(destPoolId);
		}
		
		for (VolumeVO volume : vols) {
			if (volume.getPoolId() == null && destStore == null) {
				throw new CloudRuntimeException("Volume has no pool associate and also no storage pool assigned in DeployDestination, Unable to create.");
			}
			if (destStore == null) {
				continue;
			}

			DataStore srcStore = _storageProviderMgr.getDataStore(volume.getPoolId());
			boolean needToCreateVolume = false;
			boolean needToRecreateVolume = false;
			boolean needToMigrateVolume = false;
			boolean needToCopyFromSec = false;

			Volume.State state = volume.getState();
			if (state == Volume.State.Allocated) {
				needToCreateVolume = true;
			} else if (state == Volume.State.UploadOp) {
				needToCopyFromSec = true;
			} else if (destStore.getId() != srcStore.getId()) {
				if (s_logger.isDebugEnabled()) {
					s_logger.debug("Mismatch in storage pool " + destStore.getId() + " assigned by deploymentPlanner and the one associated with volume " + volume);
				}

				if (Volume.Type.ROOT == volume.getVolumeType()) {
					needToMigrateVolume = true;
				} else {
					if (destStore.getCluterId() != srcStore.getCluterId()) {
						needToMigrateVolume = true;
					} else if (!srcStore.isSharedStorage() && srcStore.getId() != destStore.getId()) {
						needToMigrateVolume = true;
					} else {
						continue;
					}
				}	
			} else {
				continue;
			}
			
			VolumeStrategy vs = srcStore.getVolumeStrategy();
			if (needToCreateVolume) {
				volume.setReservationId(reservationId);
				volume = _volumeMgr.processEvent(volume, Volume.Event.CreateRequested);
				
				vs.createVolume(volume, destStore);
				
				volume = _volumeMgr.processEvent(volume, Volume.Event.OperationSucceeded);
			} else if (needToMigrateVolume) {
				migrateVolume(volume, srcStore, destStore, reservationId);
			} else if (needToCopyFromSec) {
				_volumeMgr.processEvent(volume, Volume.Event.CopyRequested);
			} else if (needToRecreateVolume) {
				
			}
		}
	}
	
	public void prepare(long vmId, DeploymentPlan plan, String reservationId) {
        VirtualMachine vm = _vmDao.findById(vmId);
       

        List<VolumeVO> vols = _volumeDao.findUsableVolumesForInstance(vm.getId());
        if (s_logger.isDebugEnabled()) {
            s_logger.debug("Prepare " + vols.size() + " volumes for " + vm.getInstanceName());
        }

        
    }


	public void release(long vmId, String reservationId) {
		// TODO Auto-generated method stub
		
	}

	public void destroy(List<Long> disks, String reservationId) {
		// TODO Auto-generated method stub
		
	}

	public void cancel(String reservationId) {
		// TODO Auto-generated method stub
		
	}

	public void prepareAttachDiskToVM(long disk, long vm, String reservationId) {
		// TODO Auto-generated method stub
		
	}

}
