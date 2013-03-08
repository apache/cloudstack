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

import static org.junit.Assert.fail;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import javax.inject.Inject;

import junit.framework.Assert;

import org.apache.cloudstack.engine.subsystem.api.storage.DataStoreProvider;
import org.apache.cloudstack.engine.subsystem.api.storage.DataStoreProviderManager;
import org.apache.cloudstack.engine.subsystem.api.storage.ScopeType;
import org.apache.cloudstack.engine.subsystem.api.storage.StoragePoolAllocator;
import org.apache.cloudstack.storage.datastore.db.PrimaryDataStoreDao;
import org.apache.cloudstack.storage.datastore.db.StoragePoolDetailVO;
import org.apache.cloudstack.storage.datastore.db.StoragePoolDetailsDao;
import org.apache.cloudstack.storage.datastore.db.StoragePoolVO;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import com.cloud.dc.ClusterVO;
import com.cloud.dc.DataCenterVO;
import com.cloud.dc.HostPodVO;
import com.cloud.dc.DataCenter.NetworkType;
import com.cloud.dc.dao.ClusterDao;
import com.cloud.dc.dao.DataCenterDao;
import com.cloud.dc.dao.HostPodDao;
import com.cloud.deploy.DataCenterDeployment;
import com.cloud.deploy.DeploymentPlan;
import com.cloud.deploy.DeploymentPlanner.ExcludeList;
import com.cloud.hypervisor.Hypervisor.HypervisorType;
import com.cloud.org.Cluster.ClusterType;
import com.cloud.org.Managed.ManagedState;
import com.cloud.storage.DiskOfferingVO;
import com.cloud.storage.StorageManager;
import com.cloud.storage.StoragePool;
import com.cloud.storage.StoragePoolStatus;
import com.cloud.storage.Volume;
import com.cloud.storage.VolumeVO;
import com.cloud.storage.Storage.StoragePoolType;
import com.cloud.storage.dao.DiskOfferingDao;
import com.cloud.storage.dao.VolumeDao;
import com.cloud.utils.component.ComponentContext;
import com.cloud.vm.DiskProfile;
import com.cloud.vm.VirtualMachineProfile;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = "classpath:/storageContext.xml")
public class StorageAllocatorTest {
	@Inject
	PrimaryDataStoreDao storagePoolDao;
	@Inject
	StorageManager storageMgr;
	@Inject
	DiskOfferingDao diskOfferingDao;
	@Inject
	VolumeDao volumeDao;
	@Inject
	HostPodDao podDao;
	@Inject
	ClusterDao clusterDao;
	@Inject
	DataCenterDao dcDao;
	@Inject
	StoragePoolDetailsDao poolDetailsDao;
	@Inject
	DataStoreProviderManager providerMgr;
	Long dcId = 1l;
	Long podId = 1l;
	Long clusterId = 1l;
	Long volumeId = null;
	Long diskOfferingId = null;
	Long storagePoolId = null;
	VolumeVO volume = null;
	DiskOfferingVO diskOffering = null;
	StoragePoolVO storage = null;

	@Before
	public void setup() throws Exception {
		ComponentContext.initComponentsLifeCycle();

	}

	protected void createDb() {
		DataCenterVO dc = new DataCenterVO(UUID.randomUUID().toString(), "test", "8.8.8.8", null, "10.0.0.1", null,  "10.0.0.1/24", 
				null, null, NetworkType.Basic, null, null, true,  true, null, null);
		dc = dcDao.persist(dc);
		dcId = dc.getId();

		HostPodVO pod = new HostPodVO(UUID.randomUUID().toString(), dc.getId(), "255.255.255.255", "", 8, "test");
		pod = podDao.persist(pod);
		podId = pod.getId();
		
		ClusterVO cluster = new ClusterVO(dc.getId(), pod.getId(), "devcloud cluster");
		cluster.setHypervisorType(HypervisorType.XenServer.toString());
		cluster.setClusterType(ClusterType.CloudManaged);
		cluster.setManagedState(ManagedState.Managed);
		cluster = clusterDao.persist(cluster);
		clusterId = cluster.getId();

		DataStoreProvider provider = providerMgr.getDataStoreProvider("ancient primary data store provider");
		storage = new StoragePoolVO();
		storage.setDataCenterId(dcId);
		storage.setPodId(podId);
		storage.setPoolType(StoragePoolType.NetworkFilesystem);
		storage.setClusterId(clusterId);
		storage.setStatus(StoragePoolStatus.Up);
		storage.setScope(ScopeType.CLUSTER);
		storage.setAvailableBytes(1000);
		storage.setCapacityBytes(20000);
		storage.setHostAddress(UUID.randomUUID().toString());
		storage.setPath(UUID.randomUUID().toString());
		storage.setStorageProviderId(provider.getId());
		storage = storagePoolDao.persist(storage);
		storagePoolId = storage.getId();

		storageMgr.createCapacityEntry(storage.getId());

		diskOffering = new DiskOfferingVO();
		diskOffering.setDiskSize(500);
		diskOffering.setName("test-disk");
		diskOffering.setSystemUse(false);
		diskOffering.setUseLocalStorage(false);
		diskOffering.setCustomized(false);
		diskOffering.setRecreatable(false);
		diskOffering = diskOfferingDao.persist(diskOffering);
		diskOfferingId = diskOffering.getId();

		volume = new VolumeVO(Volume.Type.ROOT, "volume", dcId, 1, 1, diskOffering.getId(), diskOffering.getDiskSize());
		volume = volumeDao.persist(volume);
		volumeId = volume.getId();
	}
	
	
	
	@Inject
	List<StoragePoolAllocator> allocators;
	@Test
	public void testClusterAllocatorMultiplePools() {
		Long newStorageId = null;
		try {
			createDb();
			
			DataStoreProvider provider = providerMgr.getDataStoreProvider("ancient primary data store provider");
			storage = new StoragePoolVO();
			storage.setDataCenterId(dcId);
			storage.setPodId(podId);
			storage.setPoolType(StoragePoolType.NetworkFilesystem);
			storage.setClusterId(clusterId);
			storage.setStatus(StoragePoolStatus.Up);
			storage.setScope(ScopeType.CLUSTER);
			storage.setAvailableBytes(1000);
			storage.setCapacityBytes(20000);
			storage.setHostAddress(UUID.randomUUID().toString());
			storage.setPath(UUID.randomUUID().toString());
			storage.setStorageProviderId(provider.getId());
			StoragePoolVO newStorage = storagePoolDao.persist(storage);
			newStorageId = newStorage.getId();
			
			DiskProfile profile = new DiskProfile(volume, diskOffering, HypervisorType.XenServer);
			VirtualMachineProfile vmProfile = Mockito.mock(VirtualMachineProfile.class);
			Mockito.when(storageMgr.storagePoolHasEnoughSpace(
					Mockito.anyListOf(Volume.class), Mockito.any(StoragePool.class))).thenReturn(true);
			DeploymentPlan plan = new DataCenterDeployment(dcId, podId, clusterId, null, null, null);
			int foundAcct = 0;
			for (StoragePoolAllocator allocator : allocators) {
				List<StoragePool> pools = allocator.allocateToPool(profile, vmProfile, plan, new ExcludeList(), 1);
				if (!pools.isEmpty()) {
					Assert.assertEquals(pools.size(), 1);
					foundAcct++;
				}
			}

			if (foundAcct > 1 || foundAcct == 0) {
				Assert.fail();
			}
		} catch (Exception e) {
			cleanDb();
			
			if (newStorageId != null) {
				storagePoolDao.remove(newStorageId);
			}
			Assert.fail();
		}
	}
	
	@Test
	public void testClusterAllocator() {
		try {
			createDb();
			DiskProfile profile = new DiskProfile(volume, diskOffering, HypervisorType.XenServer);
			VirtualMachineProfile vmProfile = Mockito.mock(VirtualMachineProfile.class);
			Mockito.when(storageMgr.storagePoolHasEnoughSpace(
					Mockito.anyListOf(Volume.class), Mockito.any(StoragePool.class))).thenReturn(true);
			DeploymentPlan plan = new DataCenterDeployment(dcId, podId, clusterId, null, null, null);
			int foundAcct = 0;
			for (StoragePoolAllocator allocator : allocators) {
				List<StoragePool> pools = allocator.allocateToPool(profile, vmProfile, plan, new ExcludeList(), 1);
				if (!pools.isEmpty()) {
					Assert.assertEquals(pools.get(0).getId(), storage.getId());
					foundAcct++;
				}
			}

			if (foundAcct > 1 || foundAcct == 0) {
				Assert.fail();
			}
		} catch (Exception e) {
			cleanDb();
			Assert.fail();
		}
	}
	
	
	@Test
	public void testClusterAllocatorWithTags() {
		try {
			createDb();
			StoragePoolDetailVO detailVO = new StoragePoolDetailVO(this.storagePoolId, "high", "true");
			poolDetailsDao.persist(detailVO);
			DiskOfferingVO diskOff = this.diskOfferingDao.findById(diskOffering.getId());
			List<String> tags = new ArrayList<String>();
			tags.add("high");
			diskOff.setTagsArray(tags);
			diskOfferingDao.update(diskOff.getId(), diskOff);
			
			DiskProfile profile = new DiskProfile(volume, diskOff, HypervisorType.XenServer);
			VirtualMachineProfile vmProfile = Mockito.mock(VirtualMachineProfile.class);
			Mockito.when(storageMgr.storagePoolHasEnoughSpace(
					Mockito.anyListOf(Volume.class), Mockito.any(StoragePool.class))).thenReturn(true);
			DeploymentPlan plan = new DataCenterDeployment(dcId, podId, clusterId, null, null, null);
			int foundAcct = 0;
			for (StoragePoolAllocator allocator : allocators) {
				List<StoragePool> pools = allocator.allocateToPool(profile, vmProfile, plan, new ExcludeList(), 1);
				if (!pools.isEmpty()) {
					Assert.assertEquals(pools.get(0).getId(), storage.getId());
					foundAcct++;
				}
			}

			if (foundAcct > 1 || foundAcct == 0) {
				Assert.fail();
			}
		} catch (Exception e) {
			cleanDb();
			Assert.fail();
		}
	}
	
	@Test
	public void testClusterAllocatorWithWrongTag() {
		try {
			createDb();
			StoragePoolDetailVO detailVO = new StoragePoolDetailVO(this.storagePoolId, "high", "true");
			poolDetailsDao.persist(detailVO);
			DiskOfferingVO diskOff = this.diskOfferingDao.findById(diskOffering.getId());
			List<String> tags = new ArrayList<String>();
			tags.add("low");
			diskOff.setTagsArray(tags);
			diskOfferingDao.update(diskOff.getId(), diskOff);
			
			DiskProfile profile = new DiskProfile(volume, diskOff, HypervisorType.XenServer);
			VirtualMachineProfile vmProfile = Mockito.mock(VirtualMachineProfile.class);
			Mockito.when(storageMgr.storagePoolHasEnoughSpace(
					Mockito.anyListOf(Volume.class), Mockito.any(StoragePool.class))).thenReturn(true);
			DeploymentPlan plan = new DataCenterDeployment(dcId, podId, clusterId, null, null, null);
			int foundAcct = 0;
			for (StoragePoolAllocator allocator : allocators) {
				List<StoragePool> pools = allocator.allocateToPool(profile, vmProfile, plan, new ExcludeList(), 1);
				if (!pools.isEmpty()) {
					foundAcct++;
				}
			}

			if (foundAcct != 0) {
				Assert.fail();
			}
		} catch (Exception e) {
			cleanDb();
			Assert.fail();
		}
	}
	
	@Test
	public void testZoneWideStorageAllocator() {
		try {
			createDb();
			
			StoragePoolVO pool = storagePoolDao.findById(storagePoolId);
			pool.setScope(ScopeType.ZONE);
			storagePoolDao.update(pool.getId(), pool);
			
			DiskProfile profile = new DiskProfile(volume, diskOffering, HypervisorType.KVM);
			VirtualMachineProfile vmProfile = Mockito.mock(VirtualMachineProfile.class);
			Mockito.when(vmProfile.getHypervisorType()).thenReturn(HypervisorType.KVM);
			Mockito.when(storageMgr.storagePoolHasEnoughSpace(
					Mockito.anyListOf(Volume.class), Mockito.any(StoragePool.class))).thenReturn(true);
			DeploymentPlan plan = new DataCenterDeployment(dcId, podId, clusterId, null, null, null);
			int foundAcct = 0;
			for (StoragePoolAllocator allocator : allocators) {
				List<StoragePool> pools = allocator.allocateToPool(profile, vmProfile, plan, new ExcludeList(), 1);
				if (!pools.isEmpty()) {
					Assert.assertEquals(pools.get(0).getId(), storage.getId());
					foundAcct++;
				}
			}

			if (foundAcct > 1 || foundAcct == 0) {
				Assert.fail();
			}
		} catch (Exception e) {
			cleanDb();
			Assert.fail();
		}
	}
	
	@Test
	public void testPoolStateIsNotUp() {
		try {
			createDb();
			
			StoragePoolVO pool = storagePoolDao.findById(storagePoolId);
			pool.setScope(ScopeType.ZONE);
			pool.setStatus(StoragePoolStatus.Maintenance);
			storagePoolDao.update(pool.getId(), pool);
			
			DiskProfile profile = new DiskProfile(volume, diskOffering, HypervisorType.XenServer);
			VirtualMachineProfile vmProfile = Mockito.mock(VirtualMachineProfile.class);
			Mockito.when(storageMgr.storagePoolHasEnoughSpace(
					Mockito.anyListOf(Volume.class), Mockito.any(StoragePool.class))).thenReturn(true);
			DeploymentPlan plan = new DataCenterDeployment(dcId, podId, clusterId, null, null, null);
			int foundAcct = 0;
			for (StoragePoolAllocator allocator : allocators) {
				List<StoragePool> pools = allocator.allocateToPool(profile, vmProfile, plan, new ExcludeList(), 1);
				if (!pools.isEmpty()) {
					Assert.assertEquals(pools.get(0).getId(), storage.getId());
					foundAcct++;
				}
			}

			if (foundAcct == 1) {
				Assert.fail();
			}
		} catch (Exception e) {
			cleanDb();
			Assert.fail();
		}
	}
	
	
	
	
	@Test
	public void testLocalStorageAllocator() {
		try {
			createDb();
			
			StoragePoolVO pool = storagePoolDao.findById(storagePoolId);
			pool.setScope(ScopeType.HOST);
			storagePoolDao.update(pool.getId(), pool);
			
			DiskOfferingVO diskOff = diskOfferingDao.findById(diskOfferingId);
			diskOff.setUseLocalStorage(true);
			diskOfferingDao.update(diskOfferingId, diskOff);
			
			DiskProfile profile = new DiskProfile(volume, diskOff, HypervisorType.XenServer);
			VirtualMachineProfile vmProfile = Mockito.mock(VirtualMachineProfile.class);
			Mockito.when(storageMgr.storagePoolHasEnoughSpace(
					Mockito.anyListOf(Volume.class), Mockito.any(StoragePool.class))).thenReturn(true);
			DeploymentPlan plan = new DataCenterDeployment(dcId, podId, clusterId, null, null, null);
			int foundAcct = 0;
			for (StoragePoolAllocator allocator : allocators) {
				List<StoragePool> pools = allocator.allocateToPool(profile, vmProfile, plan, new ExcludeList(), 1);
				if (!pools.isEmpty()) {
					Assert.assertEquals(pools.get(0).getId(), storage.getId());
					foundAcct++;
				}
			}

			if (foundAcct > 1 || foundAcct == 0) {
				Assert.fail();
			}
		} catch (Exception e) {
			cleanDb();
			Assert.fail();
		}
	}
	
	protected void cleanDb() {
		if (volumeId != null) {
			volumeDao.remove(volumeId);
			volumeId = null;
		}
		if (diskOfferingId != null) {
			diskOfferingDao.remove(diskOfferingId);
			diskOfferingId = null;
		}
		if (storagePoolId != null) {
			storagePoolDao.remove(storagePoolId);
			storagePoolId = null;
		}
		if (clusterId != null) {
			clusterDao.remove(clusterId);
			clusterId = null;
		}
		if (podId != null) {
			podDao.remove(podId);
			podId = null;
		}
		if (dcId != null) {
			dcDao.remove(dcId);
			dcId = null;
		}
	}

}
