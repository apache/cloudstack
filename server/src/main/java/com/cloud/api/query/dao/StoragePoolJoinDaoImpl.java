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
package com.cloud.api.query.dao;

import java.util.ArrayList;
import java.util.List;

import javax.inject.Inject;

import org.apache.cloudstack.annotation.AnnotationService;
import org.apache.cloudstack.annotation.dao.AnnotationDao;
import org.apache.cloudstack.api.response.StoragePoolResponse;
import org.apache.cloudstack.context.CallContext;
import org.apache.cloudstack.engine.subsystem.api.storage.DataStore;
import org.apache.cloudstack.engine.subsystem.api.storage.DataStoreManager;
import org.apache.cloudstack.engine.subsystem.api.storage.PrimaryDataStoreDriver;
import org.apache.cloudstack.framework.config.dao.ConfigurationDao;
import org.apache.cloudstack.storage.datastore.db.PrimaryDataStoreDao;
import org.apache.cloudstack.storage.datastore.db.StoragePoolDetailVO;
import org.apache.cloudstack.storage.datastore.db.StoragePoolDetailsDao;
import org.apache.cloudstack.storage.datastore.db.StoragePoolVO;
import org.apache.cloudstack.utils.jsinterpreter.TagAsRuleHelper;
import org.springframework.stereotype.Component;

import com.cloud.api.ApiDBUtils;
import com.cloud.api.query.vo.StoragePoolJoinVO;
import com.cloud.capacity.CapacityManager;
import com.cloud.storage.DataStoreRole;
import com.cloud.storage.ScopeType;
import com.cloud.storage.Storage;
import com.cloud.storage.StoragePool;
import com.cloud.storage.StorageStats;
import com.cloud.storage.VolumeApiServiceImpl;
import com.cloud.user.AccountManager;
import com.cloud.utils.StringUtils;
import com.cloud.utils.db.GenericDaoBase;
import com.cloud.utils.db.SearchBuilder;
import com.cloud.utils.db.SearchCriteria;
import org.apache.commons.collections.MapUtils;

import java.util.Map;

@Component
public class StoragePoolJoinDaoImpl extends GenericDaoBase<StoragePoolJoinVO, Long> implements StoragePoolJoinDao {

    @Inject
    private ConfigurationDao _configDao;

    @Inject
    private DataStoreManager dataStoreMgr;

    @Inject
    protected PrimaryDataStoreDao storagePoolDao;
    @Inject
    private AnnotationDao annotationDao;
    @Inject
    private AccountManager accountManager;

    @Inject
    private StoragePoolDetailsDao storagePoolDetailsDao;


    private final SearchBuilder<StoragePoolJoinVO> spSearch;

    private final SearchBuilder<StoragePoolJoinVO> spIdSearch;

    private final SearchBuilder<StoragePoolJoinVO> findByDatacenterAndScopeSb;

    protected StoragePoolJoinDaoImpl() {

        spSearch = createSearchBuilder();
        spSearch.and("idIN", spSearch.entity().getId(), SearchCriteria.Op.IN);
        spSearch.done();

        spIdSearch = createSearchBuilder();
        spIdSearch.and("id", spIdSearch.entity().getId(), SearchCriteria.Op.EQ);
        spIdSearch.done();

        findByDatacenterAndScopeSb = createSearchBuilder();
        findByDatacenterAndScopeSb.and("zoneId", findByDatacenterAndScopeSb.entity().getZoneId(), SearchCriteria.Op.EQ);
        findByDatacenterAndScopeSb.and("clusterId", findByDatacenterAndScopeSb.entity().getClusterId(), SearchCriteria.Op.EQ);
        findByDatacenterAndScopeSb.and("podId", findByDatacenterAndScopeSb.entity().getPodId(), SearchCriteria.Op.EQ);
        findByDatacenterAndScopeSb.and("scope", findByDatacenterAndScopeSb.entity().getScope(), SearchCriteria.Op.EQ);
        findByDatacenterAndScopeSb.and("status", findByDatacenterAndScopeSb.entity().getStatus(), SearchCriteria.Op.EQ);
        findByDatacenterAndScopeSb.and("is_tag_a_rule", findByDatacenterAndScopeSb.entity().getIsTagARule(), SearchCriteria.Op.EQ);
        findByDatacenterAndScopeSb.done();

        _count = "select count(distinct id) from storage_pool_view WHERE ";
    }

    @Override
    public StoragePoolResponse newStoragePoolResponse(StoragePoolJoinVO pool, boolean customStats) {
        StoragePool storagePool = storagePoolDao.findById(pool.getId());
        StoragePoolResponse poolResponse = new StoragePoolResponse();
        poolResponse.setId(pool.getUuid());
        poolResponse.setName(pool.getName());
        poolResponse.setState(pool.getStatus());
        String path = pool.getPath();
        //cifs store may contain password entry, remove the password
        path = StringUtils.cleanString(path);
        poolResponse.setPath(path);
        poolResponse.setIpAddress(pool.getHostAddress());
        poolResponse.setZoneId(pool.getZoneUuid());
        poolResponse.setZoneName(pool.getZoneName());
        poolResponse.setType(pool.getPoolType().toString());
        poolResponse.setPodId(pool.getPodUuid());
        poolResponse.setPodName(pool.getPodName());
        poolResponse.setCreated(pool.getCreated());
        if (pool.getScope() != null) {
            poolResponse.setScope(pool.getScope().toString());
        }
        if (pool.getHypervisor() != null) {
            poolResponse.setHypervisor(pool.getHypervisor().getHypervisorDisplayName());
        }

        StoragePoolDetailVO poolType = storagePoolDetailsDao.findDetail(pool.getId(), "pool_type");
        if (poolType != null) {
            poolResponse.setType(poolType.getValue());
        }
        long allocatedSize = pool.getUsedCapacity() + pool.getReservedCapacity();
        if (pool.getPoolType() == Storage.StoragePoolType.DatastoreCluster) {
            List<StoragePoolVO> childDatastores = storagePoolDao.listChildStoragePoolsInDatastoreCluster(pool.getId());
            if (childDatastores != null) {
                for (StoragePoolVO childDatastore: childDatastores) {
                    StoragePoolJoinVO childDSJoinVO = findById(childDatastore.getId());
                    allocatedSize += (childDSJoinVO.getUsedCapacity() + childDSJoinVO.getReservedCapacity());
                }
            }
        }
        poolResponse.setDiskSizeTotal(pool.getCapacityBytes());
        poolResponse.setDiskSizeAllocated(allocatedSize);
        poolResponse.setCapacityIops(pool.getCapacityIops());

        if (storagePool.isManaged()) {
            DataStore store = dataStoreMgr.getDataStore(pool.getId(), DataStoreRole.Primary);
            PrimaryDataStoreDriver driver = (PrimaryDataStoreDriver) store.getDriver();
            long usedIops = driver.getUsedIops(storagePool);
            poolResponse.setAllocatedIops(usedIops);

            if (customStats && driver.poolProvidesCustomStorageStats()) {
                Map<String, String> storageCustomStats = driver.getCustomStorageStats(storagePool);
                if (MapUtils.isNotEmpty(storageCustomStats)) {
                    poolResponse.setCustomStats(storageCustomStats);
                }
            }
        }

        // TODO: StatsCollector does not persist data
        StorageStats stats = ApiDBUtils.getStoragePoolStatistics(pool.getId());
        if (stats != null) {
            Long used = stats.getByteUsed();
            poolResponse.setDiskSizeUsed(used);
        }

        poolResponse.setClusterId(pool.getClusterUuid());
        poolResponse.setClusterName(pool.getClusterName());
        poolResponse.setProvider(pool.getStorageProviderName());
        poolResponse.setTags(pool.getTag());
        poolResponse.setIsTagARule(pool.getIsTagARule());
        poolResponse.setOverProvisionFactor(Double.toString(CapacityManager.StorageOverprovisioningFactor.valueIn(pool.getId())));
        poolResponse.setManaged(storagePool.isManaged());

        // set async job
        if (pool.getJobId() != null) {
            poolResponse.setJobId(pool.getJobUuid());
            poolResponse.setJobStatus(pool.getJobStatus());
        }
        poolResponse.setHasAnnotation(annotationDao.hasAnnotations(pool.getUuid(), AnnotationService.EntityType.PRIMARY_STORAGE.name(),
                accountManager.isRootAdmin(CallContext.current().getCallingAccount().getId())));

        poolResponse.setObjectName("storagepool");
        return poolResponse;
    }

    @Override
    public StoragePoolResponse setStoragePoolResponse(StoragePoolResponse response, StoragePoolJoinVO sp) {
        String tag = sp.getTag();
        if (tag != null) {
            if (response.getTags() != null && response.getTags().length() > 0) {
                response.setTags(response.getTags() + "," + tag);
            } else {
                response.setTags(tag);
            }
        }
        if (response.hasAnnotation() == null) {
            response.setHasAnnotation(annotationDao.hasAnnotations(sp.getUuid(), AnnotationService.EntityType.PRIMARY_STORAGE.name(),
                    accountManager.isRootAdmin(CallContext.current().getCallingAccount().getId())));
        }
        return response;
    }

    @Override
    public StoragePoolResponse newStoragePoolForMigrationResponse(StoragePoolJoinVO pool) {
        StoragePool storagePool = storagePoolDao.findById(pool.getId());
        StoragePoolResponse poolResponse = new StoragePoolResponse();
        poolResponse.setId(pool.getUuid());
        poolResponse.setName(pool.getName());
        poolResponse.setState(pool.getStatus());
        String path = pool.getPath();
        //cifs store may contain password entry, remove the password
        path = StringUtils.cleanString(path);
        poolResponse.setPath(path);
        poolResponse.setIpAddress(pool.getHostAddress());
        poolResponse.setZoneId(pool.getZoneUuid());
        poolResponse.setZoneName(pool.getZoneName());
        if (pool.getPoolType() != null) {
            poolResponse.setType(pool.getPoolType().toString());
        }
        poolResponse.setPodId(pool.getPodUuid());
        poolResponse.setPodName(pool.getPodName());
        poolResponse.setCreated(pool.getCreated());
        poolResponse.setScope(pool.getScope().toString());
        if (pool.getHypervisor() != null) {
            poolResponse.setHypervisor(pool.getHypervisor().getHypervisorDisplayName());
        }

        long allocatedSize = pool.getUsedCapacity();
        poolResponse.setDiskSizeTotal(pool.getCapacityBytes());
        poolResponse.setDiskSizeAllocated(allocatedSize);
        poolResponse.setCapacityIops(pool.getCapacityIops());

        if (storagePool != null) {
            poolResponse.setManaged(storagePool.isManaged());
            if (storagePool.isManaged()) {
                DataStore store = dataStoreMgr.getDataStore(pool.getId(), DataStoreRole.Primary);
                PrimaryDataStoreDriver driver = (PrimaryDataStoreDriver) store.getDriver();
                long usedIops = driver.getUsedIops(storagePool);
                poolResponse.setAllocatedIops(usedIops);
            }
        }

        poolResponse.setOverProvisionFactor(Double.toString(CapacityManager.StorageOverprovisioningFactor.valueIn(pool.getId())));

        // TODO: StatsCollector does not persist data
        StorageStats stats = ApiDBUtils.getStoragePoolStatistics(pool.getId());
        if (stats != null) {
            Long used = stats.getByteUsed();
            poolResponse.setDiskSizeUsed(used);
        }

        poolResponse.setClusterId(pool.getClusterUuid());
        poolResponse.setClusterName(pool.getClusterName());
        poolResponse.setProvider(pool.getStorageProviderName());
        poolResponse.setTags(pool.getTag());
        poolResponse.setIsTagARule(pool.getIsTagARule());

        // set async job
        poolResponse.setJobId(pool.getJobUuid());
        poolResponse.setJobStatus(pool.getJobStatus());

        poolResponse.setObjectName("storagepool");
        return poolResponse;
    }

    @Override
    public StoragePoolResponse setStoragePoolForMigrationResponse(StoragePoolResponse response, StoragePoolJoinVO sp) {
        String tag = sp.getTag();
        if (tag != null) {
            if (response.getTags() != null && response.getTags().length() > 0) {
                response.setTags(response.getTags() + "," + tag);
            } else {
                response.setTags(tag);
            }
        }
        return response;
    }

    @Override
    public List<StoragePoolJoinVO> newStoragePoolView(StoragePool host) {
        SearchCriteria<StoragePoolJoinVO> sc = spIdSearch.create();
        sc.setParameters("id", host.getId());
        return searchIncludingRemoved(sc, null, null, false);

    }

    @Override
    public List<StoragePoolJoinVO> searchByIds(Long... spIds) {
        // set detail batch query size
        int DETAILS_BATCH_SIZE = 2000;
        String batchCfg = _configDao.getValue("detail.batch.query.size");
        if (batchCfg != null) {
            DETAILS_BATCH_SIZE = Integer.parseInt(batchCfg);
        }
        // query details by batches
        List<StoragePoolJoinVO> uvList = new ArrayList<StoragePoolJoinVO>();
        // query details by batches
        int curr_index = 0;
        if (spIds.length > DETAILS_BATCH_SIZE) {
            while ((curr_index + DETAILS_BATCH_SIZE) <= spIds.length) {
                Long[] ids = new Long[DETAILS_BATCH_SIZE];
                for (int k = 0, j = curr_index; j < curr_index + DETAILS_BATCH_SIZE; j++, k++) {
                    ids[k] = spIds[j];
                }
                SearchCriteria<StoragePoolJoinVO> sc = spSearch.create();
                sc.setParameters("idIN", ids);
                List<StoragePoolJoinVO> vms = searchIncludingRemoved(sc, null, null, false);
                if (vms != null) {
                    uvList.addAll(vms);
                }
                curr_index += DETAILS_BATCH_SIZE;
            }
        }
        if (curr_index < spIds.length) {
            int batch_size = (spIds.length - curr_index);
            // set the ids value
            Long[] ids = new Long[batch_size];
            for (int k = 0, j = curr_index; j < curr_index + batch_size; j++, k++) {
                ids[k] = spIds[j];
            }
            SearchCriteria<StoragePoolJoinVO> sc = spSearch.create();
            sc.setParameters("idIN", ids);
            List<StoragePoolJoinVO> vms = searchIncludingRemoved(sc, null, null, false);
            if (vms != null) {
                uvList.addAll(vms);
            }
        }
        return uvList;
    }

    @Override
    public List<StoragePoolVO> findStoragePoolByScopeAndRuleTags(Long datacenterId, Long podId, Long clusterId, ScopeType scopeType, List<String> tags) {
        SearchCriteria<StoragePoolJoinVO> sc =  findByDatacenterAndScopeSb.create();
        if (datacenterId != null) {
            sc.setParameters("zoneId", datacenterId);
        }
        if (clusterId != null) {
            sc.setParameters("clusterId", clusterId);
        }
        if (podId != null) {
            sc.setParameters("podId", podId);
        }

        sc.setParameters("scope", scopeType);
        sc.setParameters("status", "Up");
        sc.setParameters("is_tag_a_rule", true);
        List<StoragePoolJoinVO> storagePools = search(sc, null, false, false);

        List<StoragePoolVO> filteredPools = new ArrayList<>();

        StringBuilder injectableTagsBuilder = new StringBuilder();
        for (String tag : tags) {
            injectableTagsBuilder.append(tag).append(",");
        }
        if (!tags.isEmpty()) {
            injectableTagsBuilder.deleteCharAt(injectableTagsBuilder.length() - 1);
        }
        String injectableTag = injectableTagsBuilder.toString();

        for (StoragePoolJoinVO storagePoolJoinVO : storagePools) {
            if (TagAsRuleHelper.interpretTagAsRule(storagePoolJoinVO.getTag(), injectableTag, VolumeApiServiceImpl.storageTagRuleExecutionTimeout.value())) {
                StoragePoolVO storagePoolVO = storagePoolDao.findById(storagePoolJoinVO.getId());
                if (storagePoolVO != null) {
                    filteredPools.add(storagePoolVO);
                } else {
                    logger.warn(String.format("Unable to find Storage Pool [%s] in the DB.", storagePoolJoinVO.getUuid()));
                }
            }
        }
        return filteredPools;
    }

}
