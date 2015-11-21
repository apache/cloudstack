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
package org.apache.cloudstack.storage.datastore.db;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.inject.Inject;
import javax.naming.ConfigurationException;

import com.cloud.host.Status;
import com.cloud.hypervisor.Hypervisor.HypervisorType;
import com.cloud.storage.ScopeType;
import com.cloud.storage.StoragePoolHostVO;
import com.cloud.storage.StoragePoolStatus;
import com.cloud.storage.dao.StoragePoolHostDao;
import com.cloud.utils.db.DB;
import com.cloud.utils.db.GenericDaoBase;
import com.cloud.utils.db.GenericSearchBuilder;
import com.cloud.utils.db.JoinBuilder;
import com.cloud.utils.db.QueryBuilder;
import com.cloud.utils.db.SearchBuilder;
import com.cloud.utils.db.SearchCriteria;
import com.cloud.utils.db.SearchCriteria.Func;
import com.cloud.utils.db.SearchCriteria.Op;
import com.cloud.utils.db.TransactionLegacy;
import com.cloud.utils.exception.CloudRuntimeException;

@DB()
public class PrimaryDataStoreDaoImpl extends GenericDaoBase<StoragePoolVO, Long> implements PrimaryDataStoreDao {
    protected final SearchBuilder<StoragePoolVO> AllFieldSearch;
    protected final SearchBuilder<StoragePoolVO> DcPodSearch;
    protected final SearchBuilder<StoragePoolVO> DcPodAnyClusterSearch;
    protected final SearchBuilder<StoragePoolVO> DeleteLvmSearch;
    protected final SearchBuilder<StoragePoolVO> DcLocalStorageSearch;
    protected final GenericSearchBuilder<StoragePoolVO, Long> StatusCountSearch;

    @Inject
    protected StoragePoolDetailsDao _detailsDao;
    @Inject
    protected StoragePoolHostDao _hostDao;

    private final String DetailsSqlPrefix =
        "SELECT storage_pool.* from storage_pool LEFT JOIN storage_pool_details ON storage_pool.id = storage_pool_details.pool_id WHERE storage_pool.removed is null and storage_pool.status = 'Up' and storage_pool.data_center_id = ? and (storage_pool.pod_id = ? or storage_pool.pod_id is null) and storage_pool.scope = ? and (";
    private final String DetailsSqlSuffix = ") GROUP BY storage_pool_details.pool_id HAVING COUNT(storage_pool_details.name) >= ?";
    private final String ZoneWideDetailsSqlPrefix =
        "SELECT storage_pool.* from storage_pool LEFT JOIN storage_pool_details ON storage_pool.id = storage_pool_details.pool_id WHERE storage_pool.removed is null and storage_pool.status = 'Up' and storage_pool.data_center_id = ? and storage_pool.scope = ? and (";
    private final String ZoneWideDetailsSqlSuffix = ") GROUP BY storage_pool_details.pool_id HAVING COUNT(storage_pool_details.name) >= ?";

    private final String FindPoolTagDetails = "SELECT storage_pool_details.name FROM storage_pool_details WHERE pool_id = ? and value = ?";

    public PrimaryDataStoreDaoImpl() {
        AllFieldSearch = createSearchBuilder();
        AllFieldSearch.and("name", AllFieldSearch.entity().getName(), SearchCriteria.Op.EQ);
        AllFieldSearch.and("uuid", AllFieldSearch.entity().getUuid(), SearchCriteria.Op.EQ);
        AllFieldSearch.and("datacenterId", AllFieldSearch.entity().getDataCenterId(), SearchCriteria.Op.EQ);
        AllFieldSearch.and("hostAddress", AllFieldSearch.entity().getHostAddress(), SearchCriteria.Op.EQ);
        AllFieldSearch.and("status", AllFieldSearch.entity().getStatus(), SearchCriteria.Op.EQ);
        AllFieldSearch.and("scope", AllFieldSearch.entity().getScope(), SearchCriteria.Op.EQ);
        AllFieldSearch.and("path", AllFieldSearch.entity().getPath(), SearchCriteria.Op.EQ);
        AllFieldSearch.and("podId", AllFieldSearch.entity().getPodId(), Op.EQ);
        AllFieldSearch.and("clusterId", AllFieldSearch.entity().getClusterId(), Op.EQ);
        AllFieldSearch.done();

        DcPodSearch = createSearchBuilder();
        DcPodSearch.and("datacenterId", DcPodSearch.entity().getDataCenterId(), SearchCriteria.Op.EQ);
        DcPodSearch.and("status", DcPodSearch.entity().getStatus(), SearchCriteria.Op.EQ);
        DcPodSearch.and("scope", DcPodSearch.entity().getScope(), SearchCriteria.Op.EQ);
        DcPodSearch.and().op("nullpod", DcPodSearch.entity().getPodId(), SearchCriteria.Op.NULL);
        DcPodSearch.or("podId", DcPodSearch.entity().getPodId(), SearchCriteria.Op.EQ);
        DcPodSearch.cp();
        DcPodSearch.and().op("nullcluster", DcPodSearch.entity().getClusterId(), SearchCriteria.Op.NULL);
        DcPodSearch.or("cluster", DcPodSearch.entity().getClusterId(), SearchCriteria.Op.EQ);
        DcPodSearch.cp();
        DcPodSearch.done();

        DcPodAnyClusterSearch = createSearchBuilder();
        DcPodAnyClusterSearch.and("datacenterId", DcPodAnyClusterSearch.entity().getDataCenterId(), SearchCriteria.Op.EQ);
        DcPodAnyClusterSearch.and("status", DcPodAnyClusterSearch.entity().getStatus(), SearchCriteria.Op.EQ);
        DcPodAnyClusterSearch.and("scope", DcPodAnyClusterSearch.entity().getScope(), SearchCriteria.Op.EQ);
        DcPodAnyClusterSearch.and().op("nullpod", DcPodAnyClusterSearch.entity().getPodId(), SearchCriteria.Op.NULL);
        DcPodAnyClusterSearch.or("podId", DcPodAnyClusterSearch.entity().getPodId(), SearchCriteria.Op.EQ);
        DcPodAnyClusterSearch.cp();
        DcPodAnyClusterSearch.done();

        DeleteLvmSearch = createSearchBuilder();
        DeleteLvmSearch.and("ids", DeleteLvmSearch.entity().getId(), SearchCriteria.Op.IN);
        DeleteLvmSearch.and().op("LVM", DeleteLvmSearch.entity().getPoolType(), SearchCriteria.Op.EQ);
        DeleteLvmSearch.or("Filesystem", DeleteLvmSearch.entity().getPoolType(), SearchCriteria.Op.EQ);
        DeleteLvmSearch.cp();
        DeleteLvmSearch.done();

        StatusCountSearch = createSearchBuilder(Long.class);
        StatusCountSearch.and("status", StatusCountSearch.entity().getStatus(), SearchCriteria.Op.IN);
        StatusCountSearch.select(null, Func.COUNT, null);
        StatusCountSearch.done();

        DcLocalStorageSearch = createSearchBuilder();
        DcLocalStorageSearch.and("datacenterId", DcLocalStorageSearch.entity().getDataCenterId(), SearchCriteria.Op.EQ);
        DcLocalStorageSearch.and("path", DcLocalStorageSearch.entity().getPath(), SearchCriteria.Op.EQ);
        DcLocalStorageSearch.and("scope", DcLocalStorageSearch.entity().getScope(), SearchCriteria.Op.EQ);
        DcLocalStorageSearch.done();
    }

    @Override
    public List<StoragePoolVO> findPoolByName(String name) {
        SearchCriteria<StoragePoolVO> sc = AllFieldSearch.create();
        sc.setParameters("name", name);
        return listIncludingRemovedBy(sc);
    }

    @Override
    public StoragePoolVO findPoolByUUID(String uuid) {
        SearchCriteria<StoragePoolVO> sc = AllFieldSearch.create();
        sc.setParameters("uuid", uuid);
        return findOneIncludingRemovedBy(sc);
    }

    @Override
    public List<StoragePoolVO> findIfDuplicatePoolsExistByUUID(String uuid) {
        SearchCriteria<StoragePoolVO> sc = AllFieldSearch.create();
        sc.setParameters("uuid", uuid);
        return listBy(sc);
    }

    @Override
    public List<StoragePoolVO> listByDataCenterId(long datacenterId) {
        SearchCriteria<StoragePoolVO> sc = AllFieldSearch.create();
        sc.setParameters("datacenterId", datacenterId);
        return listBy(sc);
    }

    @Override
    public void updateCapacityBytes(long id, long capacityBytes) {
        StoragePoolVO pool = createForUpdate(id);
        pool.setCapacityBytes(capacityBytes);
        update(id, pool);
    }

    @Override
    public void updateCapacityIops(long id, long capacityIops) {
        StoragePoolVO pool = createForUpdate(id);
        pool.setCapacityIops(capacityIops);
        update(id, pool);
    }

    @Override
    public List<StoragePoolVO> listByStorageHost(String hostFqdnOrIp) {
        SearchCriteria<StoragePoolVO> sc = AllFieldSearch.create();
        sc.setParameters("hostAddress", hostFqdnOrIp);
        return listIncludingRemovedBy(sc);
    }

    @Override
    public List<StoragePoolVO> listByStatus(StoragePoolStatus status) {
        SearchCriteria<StoragePoolVO> sc = AllFieldSearch.create();
        sc.setParameters("status", status);
        return listBy(sc);
    }

    @Override
    public List<StoragePoolVO> listByStatusInZone(long dcId, StoragePoolStatus status) {
        SearchCriteria<StoragePoolVO> sc = AllFieldSearch.create();
        sc.setParameters("status", status);
        sc.setParameters("datacenterId", dcId);
        return listBy(sc);
    }

    @Override
    public StoragePoolVO findPoolByHostPath(long datacenterId, Long podId, String host, String path, String uuid) {
        SearchCriteria<StoragePoolVO> sc = AllFieldSearch.create();
        sc.setParameters("hostAddress", host);
        if (path != null) {
            sc.setParameters("path", path);
        }
        sc.setParameters("datacenterId", datacenterId);
        sc.setParameters("podId", podId);
        sc.setParameters("uuid", uuid);

        return findOneBy(sc);
    }

    @Override
    public List<StoragePoolVO> listLocalStoragePoolByPath(long datacenterId, String path) {
        SearchCriteria<StoragePoolVO> sc = DcLocalStorageSearch.create();
        sc.setParameters("path", path);
        sc.setParameters("datacenterId", datacenterId);
        sc.setParameters("scope", ScopeType.HOST);

        return listBy(sc);
    }

    @Override
    public List<StoragePoolVO> listBy(long datacenterId, Long podId, Long clusterId, ScopeType scope) {
        if (clusterId != null) {
            SearchCriteria<StoragePoolVO> sc = DcPodSearch.create();
            sc.setParameters("datacenterId", datacenterId);
            sc.setParameters("podId", podId);
            sc.setParameters("status", Status.Up);
            sc.setParameters("scope", scope);

            sc.setParameters("cluster", clusterId);
            return listBy(sc);
        } else {
            SearchCriteria<StoragePoolVO> sc = DcPodAnyClusterSearch.create();
            sc.setParameters("datacenterId", datacenterId);
            sc.setParameters("podId", podId);
            sc.setParameters("status", Status.Up);
            sc.setParameters("scope", scope);
            return listBy(sc);
        }
    }

    @Override
    public List<StoragePoolVO> listPoolByHostPath(String host, String path) {
        SearchCriteria<StoragePoolVO> sc = AllFieldSearch.create();
        sc.setParameters("hostAddress", host);
        sc.setParameters("path", path);

        return listBy(sc);
    }

    public StoragePoolVO listById(Integer id) {
        SearchCriteria<StoragePoolVO> sc = AllFieldSearch.create();
        sc.setParameters("id", id);

        return findOneIncludingRemovedBy(sc);
    }

    @Override
    @DB
    public StoragePoolVO persist(StoragePoolVO pool, Map<String, String> details) {
        TransactionLegacy txn = TransactionLegacy.currentTxn();
        txn.start();
        pool = super.persist(pool);
        if (details != null) {
            for (Map.Entry<String, String> detail : details.entrySet()) {
                StoragePoolDetailVO vo = new StoragePoolDetailVO(pool.getId(), detail.getKey(), detail.getValue(), true);
                _detailsDao.persist(vo);
            }
        }
        txn.commit();
        return pool;
    }

    @DB
    @Override
    public List<StoragePoolVO> findPoolsByDetails(long dcId, long podId, Long clusterId, Map<String, String> details, ScopeType scope) {
        StringBuilder sql = new StringBuilder(DetailsSqlPrefix);
        if (clusterId != null) {
            sql.append("storage_pool.cluster_id = ? OR storage_pool.cluster_id IS NULL) AND (");
        }

        for (Map.Entry<String, String> detail : details.entrySet()) {
            sql.append("((storage_pool_details.name='")
                .append(detail.getKey())
                .append("') AND (storage_pool_details.value='")
                .append(detail.getValue())
                .append("')) OR ");
        }
        sql.delete(sql.length() - 4, sql.length());
        sql.append(DetailsSqlSuffix);
        TransactionLegacy txn = TransactionLegacy.currentTxn();
        try (PreparedStatement pstmt = txn.prepareStatement(sql.toString());){
            List<StoragePoolVO> pools = new ArrayList<StoragePoolVO>();
            int i = 1;
            pstmt.setLong(i++, dcId);
            pstmt.setLong(i++, podId);
            pstmt.setString(i++, scope.toString());
            if (clusterId != null) {
                pstmt.setLong(i++, clusterId);
            }
            pstmt.setInt(i++, details.size());
            try(ResultSet rs = pstmt.executeQuery();) {
                while (rs.next()) {
                    pools.add(toEntityBean(rs, false));
                }
            }catch (SQLException e) {
                throw new CloudRuntimeException("Unable to execute :" + e.getMessage(), e);
            }
            return pools;
        } catch (SQLException e) {
            throw new CloudRuntimeException("Unable to execute :" + e.getMessage(), e);
        }
    }

    protected Map<String, String> tagsToDetails(String[] tags) {
        Map<String, String> details = new HashMap<String, String>(tags.length);
        for (String tag : tags) {
            details.put(tag, "true");
        }
        return details;
    }

    @Override
    public List<StoragePoolVO> findPoolsByTags(long dcId, long podId, Long clusterId, String[] tags) {
        List<StoragePoolVO> storagePools = null;
        if (tags == null || tags.length == 0) {
            storagePools = listBy(dcId, podId, clusterId, ScopeType.CLUSTER);
        } else {
            Map<String, String> details = tagsToDetails(tags);
            storagePools = findPoolsByDetails(dcId, podId, clusterId, details, ScopeType.CLUSTER);
        }

        return storagePools;
    }

    @Override
    public List<StoragePoolVO> findDisabledPoolsByScope(long dcId, Long podId, Long clusterId, ScopeType scope) {
        List<StoragePoolVO> storagePools = null;
        SearchCriteria<StoragePoolVO> sc = AllFieldSearch.create();
        sc.setParameters("status", StoragePoolStatus.Disabled);
        sc.setParameters("scope", scope);

        if (scope == ScopeType.ZONE) {
            sc.setParameters("datacenterId", dcId);
            storagePools = listBy(sc);
        } else if ((scope == ScopeType.CLUSTER || scope == ScopeType.HOST) && podId != null && clusterId != null) {
            sc.setParameters("datacenterId", dcId);
            sc.setParameters("podId", podId);
            sc.setParameters("clusterId", clusterId);
            storagePools = listBy(sc);
        }

        return storagePools;
    }

    @Override
    public List<StoragePoolVO> findLocalStoragePoolsByTags(long dcId, long podId, Long clusterId, String[] tags) {
        List<StoragePoolVO> storagePools = null;
        if (tags == null || tags.length == 0) {
            storagePools = listBy(dcId, podId, clusterId, ScopeType.HOST);
        } else {
            Map<String, String> details = tagsToDetails(tags);
            storagePools = findPoolsByDetails(dcId, podId, clusterId, details, ScopeType.HOST);
        }

        return storagePools;
    }

    @Override
    public List<StoragePoolVO> findLocalStoragePoolsByHostAndTags(long hostId, String[] tags) {
        SearchBuilder<StoragePoolVO> hostSearch = createSearchBuilder();
        SearchBuilder<StoragePoolHostVO> hostPoolSearch = _hostDao.createSearchBuilder();
        SearchBuilder<StoragePoolDetailVO> tagPoolSearch = _detailsDao.createSearchBuilder();;

        // Search for pools on the host
        hostPoolSearch.and("hostId", hostPoolSearch.entity().getHostId(), Op.EQ);
        // Set criteria for pools
        hostSearch.and("scope", hostSearch.entity().getScope(), Op.EQ);
        hostSearch.and("removed", hostSearch.entity().getRemoved(), Op.NULL);
        hostSearch.and("status", hostSearch.entity().getStatus(), Op.EQ);
        hostSearch.join("hostJoin", hostPoolSearch, hostSearch.entity().getId(), hostPoolSearch.entity().getPoolId(), JoinBuilder.JoinType.INNER);

        if (!(tags == null || tags.length == 0 )) {
            tagPoolSearch.and("name", tagPoolSearch.entity().getName(), Op.EQ);
            tagPoolSearch.and("value", tagPoolSearch.entity().getValue(), Op.EQ);
            hostSearch.join("tagJoin", tagPoolSearch, hostSearch.entity().getId(), tagPoolSearch.entity().getResourceId(), JoinBuilder.JoinType.INNER);
        }

        SearchCriteria<StoragePoolVO> sc = hostSearch.create();
        sc.setJoinParameters("hostJoin", "hostId", hostId );
        sc.setParameters("scope", ScopeType.HOST.toString());
        sc.setParameters("status", Status.Up.toString());

        if (!(tags == null || tags.length == 0 )) {
            Map<String, String> details = tagsToDetails(tags);
            for (Map.Entry<String, String> detail : details.entrySet()) {
                sc.setJoinParameters("tagJoin","name", detail.getKey());
                sc.setJoinParameters("tagJoin", "value", detail.getValue());
            }
        }
        return listBy(sc);
    }

    @Override
    public List<StoragePoolVO> findZoneWideStoragePoolsByTags(long dcId, String[] tags) {
        if (tags == null || tags.length == 0) {
            QueryBuilder<StoragePoolVO> sc = QueryBuilder.create(StoragePoolVO.class);
            sc.and(sc.entity().getDataCenterId(), Op.EQ, dcId);
            sc.and(sc.entity().getStatus(), Op.EQ, Status.Up);
            sc.and(sc.entity().getScope(), Op.EQ, ScopeType.ZONE);
            return sc.list();
        } else {
            Map<String, String> details = tagsToDetails(tags);

            StringBuilder sql = new StringBuilder(ZoneWideDetailsSqlPrefix);

            for (int i=0;i<details.size();i++){
                sql.append("((storage_pool_details.name=?) AND (storage_pool_details.value=?)) OR ");
            }
            sql.delete(sql.length() - 4, sql.length());
            sql.append(ZoneWideDetailsSqlSuffix);
            TransactionLegacy txn = TransactionLegacy.currentTxn();
            try (PreparedStatement pstmt = txn.prepareStatement(sql.toString());){
                List<StoragePoolVO> pools = new ArrayList<StoragePoolVO>();
                if (pstmt != null) {
                    int i = 1;

                    pstmt.setLong(i++, dcId);
                    pstmt.setString(i++, ScopeType.ZONE.toString());

                    for (Map.Entry<String, String> detail : details.entrySet()) {
                        pstmt.setString(i++, detail.getKey());
                        pstmt.setString(i++, detail.getValue());
                    }

                    pstmt.setInt(i++, details.size());

                    try(ResultSet rs = pstmt.executeQuery();) {
                        while (rs.next()) {
                            pools.add(toEntityBean(rs, false));
                        }
                    }catch (SQLException e) {
                        throw new CloudRuntimeException("findZoneWideStoragePoolsByTags:Exception:" + e.getMessage(), e);
                    }
                }
                return pools;
            } catch (SQLException e) {
                throw new CloudRuntimeException("findZoneWideStoragePoolsByTags:Exception:" + e.getMessage(), e);
            }
        }
    }

    @Override
    @DB
    public List<String> searchForStoragePoolDetails(long poolId, String value) {
        StringBuilder sql = new StringBuilder(FindPoolTagDetails);
        TransactionLegacy txn = TransactionLegacy.currentTxn();
        List<String> tags = new ArrayList<String>();
        try(PreparedStatement pstmt = txn.prepareStatement(sql.toString());) {
            if (pstmt != null) {
                pstmt.setLong(1, poolId);
                pstmt.setString(2, value);
                try(ResultSet rs = pstmt.executeQuery();) {
                    while (rs.next()) {
                        tags.add(rs.getString("name"));
                    }
                }catch (SQLException e) {
                    throw new CloudRuntimeException("searchForStoragePoolDetails:Exception:" + e.getMessage(), e);
                }
            }
            return tags;
        } catch (SQLException e) {
            throw new CloudRuntimeException("searchForStoragePoolDetails:Exception:" + e.getMessage(), e);
        }
    }

    @Override
    public void updateDetails(long poolId, Map<String, String> details) {
        if (details != null) {
            List<StoragePoolDetailVO> detailsVO = new ArrayList<StoragePoolDetailVO>();
            for (String key : details.keySet()) {
                detailsVO.add(new StoragePoolDetailVO(poolId, key, details.get(key), true));
            }
            _detailsDao.saveDetails(detailsVO);
            if(details.size() == 0) {
                _detailsDao.removeDetails(poolId);
            }
        }
    }

    @Override
    public Map<String, String> getDetails(long poolId) {
        return _detailsDao.listDetailsKeyPairs(poolId);
    }

    @Override
    public boolean configure(String name, Map<String, Object> params) throws ConfigurationException {
        super.configure(name, params);
        _detailsDao.configure("DetailsDao", params);
        return true;
    }

    @Override
    public long countPoolsByStatus(StoragePoolStatus... statuses) {
        SearchCriteria<Long> sc = StatusCountSearch.create();

        sc.setParameters("status", (Object[])statuses);

        List<Long> rs = customSearchIncludingRemoved(sc, null);
        if (rs.size() == 0) {
            return 0;
        }

        return rs.get(0);
    }

    @Override
    public List<StoragePoolVO> listPoolsByCluster(long clusterId) {
        SearchCriteria<StoragePoolVO> sc = AllFieldSearch.create();
        sc.setParameters("clusterId", clusterId);

        return listBy(sc);
    }

    @Override
    public List<StoragePoolVO> findZoneWideStoragePoolsByHypervisor(long dataCenterId, HypervisorType hypervisorType) {
        QueryBuilder<StoragePoolVO> sc = QueryBuilder.create(StoragePoolVO.class);
        sc.and(sc.entity().getDataCenterId(), Op.EQ, dataCenterId);
        sc.and(sc.entity().getStatus(), Op.EQ, Status.Up);
        sc.and(sc.entity().getScope(), Op.EQ, ScopeType.ZONE);
        sc.and(sc.entity().getHypervisor(), Op.EQ, hypervisorType);
        return sc.list();
    }
}
