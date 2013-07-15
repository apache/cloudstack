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
package org.apache.cloudstack.storage.volume.datastore;

import java.util.List;
import java.util.Map;

import javax.inject.Inject;

import org.apache.cloudstack.engine.subsystem.api.storage.DataStore;
import org.apache.cloudstack.engine.subsystem.api.storage.DataStoreManager;
import org.apache.cloudstack.engine.subsystem.api.storage.HostScope;
import org.apache.cloudstack.engine.subsystem.api.storage.PrimaryDataStoreParameters;
import org.apache.cloudstack.storage.datastore.db.PrimaryDataStoreDao;
import org.apache.cloudstack.storage.datastore.db.StoragePoolVO;
import org.apache.log4j.Logger;
import org.springframework.stereotype.Component;

import com.cloud.agent.api.StoragePoolInfo;
import com.cloud.capacity.Capacity;
import com.cloud.capacity.CapacityVO;
import com.cloud.capacity.dao.CapacityDao;
import com.cloud.storage.DataStoreRole;
import com.cloud.storage.ScopeType;
import com.cloud.hypervisor.Hypervisor.HypervisorType;
import com.cloud.storage.StorageManager;
import com.cloud.storage.StoragePoolHostVO;
import com.cloud.storage.StoragePoolStatus;
import com.cloud.storage.dao.StoragePoolHostDao;
import com.cloud.utils.db.Transaction;
import com.cloud.utils.exception.CloudRuntimeException;

@Component
public class PrimaryDataStoreHelper {
    private static final Logger s_logger = Logger.getLogger(PrimaryDataStoreHelper.class);
    @Inject
    private PrimaryDataStoreDao dataStoreDao;
    @Inject
    DataStoreManager dataStoreMgr;
    @Inject
    StorageManager storageMgr;
    @Inject
    protected CapacityDao _capacityDao;
    @Inject
    protected StoragePoolHostDao storagePoolHostDao;

    public DataStore createPrimaryDataStore(PrimaryDataStoreParameters params) {
        StoragePoolVO dataStoreVO = dataStoreDao.findPoolByUUID(params.getUuid());
        if (dataStoreVO != null) {
            throw new CloudRuntimeException("duplicate uuid: " + params.getUuid());
        }

        dataStoreVO = new StoragePoolVO();
        dataStoreVO.setStorageProviderName(params.getProviderName());
        dataStoreVO.setHostAddress(params.getHost());
        dataStoreVO.setPath(params.getPath());
        dataStoreVO.setPoolType(params.getType());
        dataStoreVO.setPort(params.getPort());
        dataStoreVO.setName(params.getName());
        dataStoreVO.setUuid(params.getUuid());
        dataStoreVO.setDataCenterId(params.getZoneId());
        dataStoreVO.setPodId(params.getPodId());
        dataStoreVO.setClusterId(params.getClusterId());
        dataStoreVO.setStatus(StoragePoolStatus.Initialized);
        dataStoreVO.setUserInfo(params.getUserInfo());
        dataStoreVO.setManaged(params.isManaged());
        dataStoreVO.setCapacityIops(params.getCapacityIops());
        dataStoreVO.setCapacityBytes(params.getCapacityBytes());
        dataStoreVO.setUsedBytes(params.getUsedBytes());
        dataStoreVO.setHypervisor(params.getHypervisorType());

        Map<String, String> details = params.getDetails();
        String tags = params.getTags();
        if (tags != null) {
            String[] tokens = tags.split(",");

            for (String tag : tokens) {
                tag = tag.trim();
                if (tag.length() == 0) {
                    continue;
                }
                details.put(tag, "true");
            }
        }

        dataStoreVO = dataStoreDao.persist(dataStoreVO, details);

        return dataStoreMgr.getDataStore(dataStoreVO.getId(), DataStoreRole.Primary);
    }

    public DataStore attachHost(DataStore store, HostScope scope, StoragePoolInfo existingInfo) {
        StoragePoolHostVO poolHost = storagePoolHostDao.findByPoolHost(store.getId(), scope.getScopeId());
        if (poolHost == null) {
            poolHost = new StoragePoolHostVO(store.getId(), scope.getScopeId(), existingInfo.getLocalPath());
            storagePoolHostDao.persist(poolHost);
        }

        StoragePoolVO pool = this.dataStoreDao.findById(store.getId());
        pool.setScope(scope.getScopeType());
        pool.setUsedBytes(existingInfo.getCapacityBytes() - existingInfo.getAvailableBytes());
        pool.setCapacityBytes(existingInfo.getCapacityBytes());
        pool.setStatus(StoragePoolStatus.Up);
        this.dataStoreDao.update(pool.getId(), pool);
        this.storageMgr.createCapacityEntry(pool, Capacity.CAPACITY_TYPE_LOCAL_STORAGE,
                pool.getUsedBytes());
        return dataStoreMgr.getDataStore(pool.getId(), DataStoreRole.Primary);
    }

    public DataStore attachCluster(DataStore store) {
        StoragePoolVO pool = this.dataStoreDao.findById(store.getId());

        storageMgr.createCapacityEntry(pool.getId());

        pool.setScope(ScopeType.CLUSTER);
        pool.setStatus(StoragePoolStatus.Up);
        this.dataStoreDao.update(pool.getId(), pool);
        return dataStoreMgr.getDataStore(store.getId(), DataStoreRole.Primary);
    }

    public DataStore attachZone(DataStore store) {
        StoragePoolVO pool = this.dataStoreDao.findById(store.getId());
        pool.setScope(ScopeType.ZONE);
        pool.setStatus(StoragePoolStatus.Up);
        this.dataStoreDao.update(pool.getId(), pool);
        return dataStoreMgr.getDataStore(store.getId(), DataStoreRole.Primary);
    }

    public DataStore attachZone(DataStore store, HypervisorType hypervisor) {
        StoragePoolVO pool = this.dataStoreDao.findById(store.getId());
        pool.setScope(ScopeType.ZONE);
        pool.setHypervisor(hypervisor);
        pool.setStatus(StoragePoolStatus.Up);
        this.dataStoreDao.update(pool.getId(), pool);
        return dataStoreMgr.getDataStore(store.getId(), DataStoreRole.Primary);
    }

    public boolean maintain(DataStore store) {
        StoragePoolVO pool = this.dataStoreDao.findById(store.getId());
        pool.setStatus(StoragePoolStatus.Maintenance);
        this.dataStoreDao.update(pool.getId(), pool);
        return true;
    }

    public boolean cancelMaintain(DataStore store) {
        StoragePoolVO pool = this.dataStoreDao.findById(store.getId());
        pool.setStatus(StoragePoolStatus.Up);
        dataStoreDao.update(store.getId(), pool);
        return true;
    }

    protected boolean deletePoolStats(Long poolId) {
        CapacityVO capacity1 = _capacityDao.findByHostIdType(poolId, Capacity.CAPACITY_TYPE_STORAGE);
        CapacityVO capacity2 = _capacityDao.findByHostIdType(poolId, Capacity.CAPACITY_TYPE_STORAGE_ALLOCATED);
        if (capacity1 != null) {
            _capacityDao.remove(capacity1.getId());
        }

        if (capacity2 != null) {
            _capacityDao.remove(capacity2.getId());
        }

        return true;
    }

    public boolean deletePrimaryDataStore(DataStore store) {
        List<StoragePoolHostVO> hostPoolRecords = this.storagePoolHostDao.listByPoolId(store.getId());
        StoragePoolVO poolVO = this.dataStoreDao.findById(store.getId());
        Transaction txn = Transaction.currentTxn();
        txn.start();
        for (StoragePoolHostVO host : hostPoolRecords) {
            storagePoolHostDao.deleteStoragePoolHostDetails(host.getHostId(), host.getPoolId());
        }
        poolVO.setUuid(null);
        this.dataStoreDao.update(poolVO.getId(), poolVO);
        dataStoreDao.remove(poolVO.getId());
        deletePoolStats(poolVO.getId());
        // Delete op_host_capacity entries
        this._capacityDao.removeBy(Capacity.CAPACITY_TYPE_STORAGE_ALLOCATED, null, null, null, poolVO.getId());
        txn.commit();

        s_logger.debug("Storage pool id=" + poolVO.getId() + " is removed successfully");
        return true;
    }

}
