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
package org.apache.cloudstack.storage.manager;

import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.apache.cloudstack.platform.subsystem.api.storage.DataStore;
import org.apache.cloudstack.platform.subsystem.api.storage.DataStore.StoreType;
import org.apache.cloudstack.platform.subsystem.api.storage.DataStoreLifeCycle;
import org.apache.cloudstack.platform.subsystem.api.storage.StorageProvider;

import com.cloud.dc.ClusterVO;
import com.cloud.dc.DataCenterVO;
import com.cloud.dc.dao.ClusterDao;
import com.cloud.dc.dao.DataCenterDao;
import com.cloud.dc.dao.HostPodDao;
import com.cloud.hypervisor.Hypervisor.HypervisorType;
import com.cloud.storage.StoragePool;
import com.cloud.storage.StoragePoolStatus;
import com.cloud.storage.StoragePoolVO;
import com.cloud.storage.dao.StoragePoolDao;
import com.cloud.utils.component.Adapters;
import com.cloud.utils.component.Inject;
import com.cloud.utils.exception.CloudRuntimeException;

public class StoragePoolManagerImpl implements StoragePoolService {
	@Inject(adapter = StorageProvider.class)
	protected Adapters<StorageProvider> _storageProviders;
	@Inject
	protected DataCenterDao _dcDao;
	@Inject
	protected HostPodDao _podDao;
	@Inject
	protected ClusterDao _clusterDao;
	@Inject
	protected StoragePoolDao _storagePoolDao;

	public void deleteStoragePool(long poolId) {
		StoragePool spool = _storagePoolDao.findById(poolId);
		StorageProvider sp = findStorageProvider(spool.getStorageProvider());
		DataStore ds = sp.getDataStore(spool);
		DataStoreLifeCycle dslc = ds.getLifeCycle();
		dslc.delete();
	}

	public void enableStoragePool(long poolId) {
		// TODO Auto-generated method stub
		
	}

	public void disableStoragePool(long poolId) {
		// TODO Auto-generated method stub
		
	}

	public Map<String, List<String>> getSupportedPrimaryStorages(long zoneId, HypervisorType hypervisor) {
		// TODO Auto-generated method stub
		return null;
	}

	public Map<String, List<String>> getSupportedSecondaryStorages(long zoneId) {
		// TODO Auto-generated method stub
		return null;
	}
	
	protected StorageProvider findStorageProvider(String name) {
		Iterator<StorageProvider> spIter = _storageProviders.iterator();
		StorageProvider sp = null;
		while (spIter.hasNext()) {
			sp = spIter.next();
			if (sp.getProviderName().equalsIgnoreCase(name)) {
				break;
			}
		}
		
		return sp;
	}
	
	public StoragePool addStoragePool(long zoneId, long podId, long clusterId, long hostId, String URI, String storageType, String poolName, String storageProviderName, Map<String, String> params) {
		StoragePoolVO spool = new StoragePoolVO();
		long poolId = _storagePoolDao.getNextInSequence(Long.class, "id");
        spool.setId(poolId);
        spool.setDataCenterId(zoneId);
        spool.setPodId(podId);
        spool.setName(poolName);
        spool.setClusterId(clusterId);
        spool.setStorageProvider(storageProviderName);
        spool.setStorageType(storageType);
        spool.setStatus(StoragePoolStatus.Creating);
        spool = _storagePoolDao.persist(spool);
        
        StorageProvider sp = findStorageProvider(storageProviderName);
        DataStore ds = sp.addDataStore((StoragePool)spool, URI, params);
        
        DataStoreLifeCycle dslc = ds.getLifeCycle();
        try {
        	dslc.add();
        } catch (CloudRuntimeException e) {
        	_storagePoolDao.remove(spool.getId());
        	throw e;
        }
     
        spool.setPath(ds.getURI());
        spool.setUuid(ds.getUUID());
        spool.setStatus(StoragePoolStatus.Up);
        _storagePoolDao.update(spool.getId(), spool);
        spool = _storagePoolDao.findById(spool.getId());
        return spool;
	}

}
