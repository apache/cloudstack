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

import com.cloud.api.ApiDBUtils;
import com.cloud.api.query.vo.StoragePoolJoinVO;
import com.cloud.capacity.CapacityManager;
import com.cloud.storage.DataStoreRole;
import com.cloud.storage.ScopeType;
import com.cloud.storage.Storage;
import com.cloud.storage.StoragePool;
import com.cloud.storage.StoragePoolStatus;
import com.cloud.storage.StorageStats;
import com.cloud.user.AccountManager;
import com.cloud.utils.Pair;
import com.cloud.utils.StringUtils;
import com.cloud.utils.db.Filter;
import com.cloud.utils.db.GenericDaoBase;
import com.cloud.utils.db.SearchBuilder;
import com.cloud.utils.db.SearchCriteria;
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
import org.apache.log4j.Logger;
import org.springframework.stereotype.Component;

import javax.inject.Inject;
import java.util.ArrayList;
import java.util.List;

@Component
public class StoragePoolJoinDaoImpl extends GenericDaoBase<StoragePoolJoinVO, Long> implements StoragePoolJoinDao {
    public static final Logger s_logger = Logger.getLogger(StoragePoolJoinDaoImpl.class);

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

    protected StoragePoolJoinDaoImpl() {

        spSearch = createSearchBuilder();
        spSearch.and("idIN", spSearch.entity().getId(), SearchCriteria.Op.IN);
        spSearch.done();

        spIdSearch = createSearchBuilder();
        spIdSearch.and("id", spIdSearch.entity().getId(), SearchCriteria.Op.EQ);
        spIdSearch.done();

        _count = "select count(distinct id) from storage_pool_view WHERE ";
    }

    @Override
    public StoragePoolResponse newStoragePoolResponse(StoragePoolJoinVO pool) {
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
            poolResponse.setHypervisor(pool.getHypervisor().toString());
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
        poolResponse.setOverProvisionFactor(Double.toString(CapacityManager.StorageOverprovisioningFactor.valueIn(pool.getId())));

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
            poolResponse.setHypervisor(pool.getHypervisor().toString());
        }

        long allocatedSize = pool.getUsedCapacity();
        poolResponse.setDiskSizeTotal(pool.getCapacityBytes());
        poolResponse.setDiskSizeAllocated(allocatedSize);
        poolResponse.setCapacityIops(pool.getCapacityIops());
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
    public Pair<List<StoragePoolJoinVO>, Integer> searchAndCount(Long storagePoolId, String storagePoolName, Long zoneId, String path, Long podId, Long clusterId, String address, ScopeType scopeType, StoragePoolStatus status, String keyword, Filter searchFilter) {
        SearchCriteria<StoragePoolJoinVO> sc = createStoragePoolSearchCriteria(storagePoolId, storagePoolName, zoneId, path, podId, clusterId, address, scopeType, status, keyword);
        return searchAndCount(sc, searchFilter);
    }

    private SearchCriteria<StoragePoolJoinVO> createStoragePoolSearchCriteria(Long storagePoolId, String storagePoolName, Long zoneId, String path, Long podId, Long clusterId, String address, ScopeType scopeType, StoragePoolStatus status, String keyword) {
        SearchBuilder<StoragePoolJoinVO> sb = createSearchBuilder();
        sb.select(null, SearchCriteria.Func.DISTINCT, sb.entity().getId()); // select distinct
        // ids
        sb.and("id", sb.entity().getId(), SearchCriteria.Op.EQ);
        sb.and("name", sb.entity().getName(), SearchCriteria.Op.EQ);
        sb.and("path", sb.entity().getPath(), SearchCriteria.Op.EQ);
        sb.and("dataCenterId", sb.entity().getZoneId(), SearchCriteria.Op.EQ);
        sb.and("podId", sb.entity().getPodId(), SearchCriteria.Op.EQ);
        sb.and("clusterId", sb.entity().getClusterId(), SearchCriteria.Op.EQ);
        sb.and("hostAddress", sb.entity().getHostAddress(), SearchCriteria.Op.EQ);
        sb.and("scope", sb.entity().getScope(), SearchCriteria.Op.EQ);
        sb.and("status", sb.entity().getStatus(), SearchCriteria.Op.EQ);
        sb.and("parent", sb.entity().getParent(), SearchCriteria.Op.EQ);

        SearchCriteria<StoragePoolJoinVO> sc = sb.create();

        if (keyword != null) {
            SearchCriteria<StoragePoolJoinVO> ssc = createSearchCriteria();
            ssc.addOr("name", SearchCriteria.Op.LIKE, "%" + keyword + "%");
            ssc.addOr("poolType", SearchCriteria.Op.LIKE, "%" + keyword + "%");

            sc.addAnd("name", SearchCriteria.Op.SC, ssc);
        }

        if (storagePoolId != null) {
            sc.setParameters("id", storagePoolId);
        }

        if (storagePoolName != null) {
            sc.setParameters("name", storagePoolName);
        }

        if (path != null) {
            sc.setParameters("path", path);
        }
        if (zoneId != null) {
            sc.setParameters("dataCenterId", zoneId);
        }
        if (podId != null) {
            SearchCriteria<StoragePoolJoinVO> ssc = createSearchCriteria();
            ssc.addOr("podId", SearchCriteria.Op.EQ, podId);
            ssc.addOr("podId", SearchCriteria.Op.NULL);

            sc.addAnd("podId", SearchCriteria.Op.SC, ssc);
        }
        if (address != null) {
            sc.setParameters("hostAddress", address);
        }
        if (clusterId != null) {
            SearchCriteria<StoragePoolJoinVO> ssc = createSearchCriteria();
            ssc.addOr("clusterId", SearchCriteria.Op.EQ, clusterId);
            ssc.addOr("clusterId", SearchCriteria.Op.NULL);

            sc.addAnd("clusterId", SearchCriteria.Op.SC, ssc);
        }
        if (scopeType != null) {
            sc.setParameters("scope", scopeType.toString());
        }
        if (status != null) {
            sc.setParameters("status", status.toString());
        }
        sc.setParameters("parent", 0);
        return sc;
    }
}
