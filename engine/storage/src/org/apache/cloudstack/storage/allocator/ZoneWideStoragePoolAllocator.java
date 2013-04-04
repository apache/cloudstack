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
package org.apache.cloudstack.storage.allocator;

import java.util.ArrayList;
import java.util.List;

import javax.inject.Inject;

import org.apache.cloudstack.engine.subsystem.api.storage.DataStoreManager;
import org.apache.cloudstack.storage.datastore.db.PrimaryDataStoreDao;
import org.apache.cloudstack.storage.datastore.db.StoragePoolVO;
import org.apache.log4j.Logger;
import org.springframework.stereotype.Component;

import com.cloud.deploy.DeploymentPlan;
import com.cloud.deploy.DeploymentPlanner.ExcludeList;
import com.cloud.hypervisor.Hypervisor.HypervisorType;
import com.cloud.storage.StoragePool;
import com.cloud.storage.Volume;
import com.cloud.vm.DiskProfile;
import com.cloud.vm.VirtualMachine;
import com.cloud.vm.VirtualMachineProfile;

@Component
public class ZoneWideStoragePoolAllocator extends AbstractStoragePoolAllocator {
	private static final Logger s_logger = Logger.getLogger(ZoneWideStoragePoolAllocator.class);
	@Inject PrimaryDataStoreDao _storagePoolDao; 
	@Inject DataStoreManager dataStoreMgr; 
	
	@Override
	protected boolean filter(ExcludeList avoid, StoragePool pool, DiskProfile dskCh, 
			 DeploymentPlan plan) {
        Volume volume =  _volumeDao.findById(dskCh.getVolumeId());
        List<Volume> requestVolumes = new ArrayList<Volume>();
        requestVolumes.add(volume);
        return storageMgr.storagePoolHasEnoughSpace(requestVolumes, pool);
	}
	
	@Override
	protected List<StoragePool> select(DiskProfile dskCh,
			VirtualMachineProfile<? extends VirtualMachine> vmProfile,
			DeploymentPlan plan, ExcludeList avoid, int returnUpTo) {
	    s_logger.debug("ZoneWideStoragePoolAllocator to find storage pool");
		List<StoragePool> suitablePools = new ArrayList<StoragePool>();
		HypervisorType hypervisor = dskCh.getHypersorType();
		if (hypervisor != null) {
			if (hypervisor != HypervisorType.KVM) {
				s_logger.debug("Only kvm supports zone wide storage");
				return suitablePools;
			}
		}
		
		List<StoragePoolVO> storagePools = _storagePoolDao.findZoneWideStoragePoolsByTags(plan.getDataCenterId(), dskCh.getTags());
	
		for (StoragePoolVO storage : storagePools) {
			if (suitablePools.size() == returnUpTo) {
        		break;
        	}
			StoragePool pol = (StoragePool)this.dataStoreMgr.getPrimaryDataStore(storage.getId());
			if (filter(avoid, pol, dskCh, plan)) {
				suitablePools.add(pol);
			}
		}
		return suitablePools;
	}
}
