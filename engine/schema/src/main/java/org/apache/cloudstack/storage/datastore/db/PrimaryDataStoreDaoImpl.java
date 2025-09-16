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
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import javax.inject.Inject;
import javax.naming.ConfigurationException;

import com.cloud.storage.StoragePoolAndAccessGroupMapVO;
import com.cloud.storage.dao.StoragePoolAndAccessGroupMapDao;
import org.apache.commons.collections.CollectionUtils;

import com.cloud.host.Status;
import com.cloud.hypervisor.Hypervisor.HypervisorType;
import com.cloud.storage.ScopeType;
import com.cloud.storage.Storage;
import com.cloud.storage.StoragePoolHostVO;
import com.cloud.storage.StoragePoolStatus;
import com.cloud.storage.StoragePoolTagVO;
import com.cloud.storage.dao.StoragePoolHostDao;
import com.cloud.storage.dao.StoragePoolTagsDao;
import com.cloud.utils.Pair;
import com.cloud.utils.db.DB;
import com.cloud.utils.db.Filter;
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
    private final SearchBuilder<StoragePoolVO> AllFieldSearch;
    private final SearchBuilder<StoragePoolVO> DcPodSearch;
    private final SearchBuilder<StoragePoolVO> DcPodAnyClusterSearch;
    private final SearchBuilder<StoragePoolVO> DeleteLvmSearch;
    private final SearchBuilder<StoragePoolVO> DcLocalStorageSearch;
    private final GenericSearchBuilder<StoragePoolVO, Long> StatusCountSearch;
    private final SearchBuilder<StoragePoolVO> ClustersSearch;
    private final SearchBuilder<StoragePoolVO> IdsSearch;

    @Inject
    private StoragePoolDetailsDao _detailsDao;
    @Inject
    private StoragePoolHostDao _hostDao;
    @Inject
    private StoragePoolTagsDao _tagsDao;
    @Inject
    StoragePoolAndAccessGroupMapDao _storagePoolAccessGroupMapDao;

    protected final String DetailsSqlPrefix = "SELECT storage_pool.* from storage_pool LEFT JOIN storage_pool_details ON storage_pool.id = storage_pool_details.pool_id WHERE storage_pool.removed is null and storage_pool.status = 'Up' and storage_pool.data_center_id = ? and (storage_pool.pod_id = ? or storage_pool.pod_id is null) and storage_pool.scope = ? and (";
    protected final String DetailsSqlSuffix = ") GROUP BY storage_pool_details.pool_id HAVING COUNT(storage_pool_details.name) >= ?";
    protected final String DetailsForHostConnectionSqlSuffix = ") GROUP BY storage_pool_details.pool_id";
    private final String ZoneWideTagsSqlPrefix = "SELECT storage_pool.* from storage_pool LEFT JOIN storage_pool_tags ON storage_pool.id = storage_pool_tags.pool_id WHERE storage_pool.removed is null and storage_pool.status = 'Up' AND storage_pool_tags.is_tag_a_rule = 0 and storage_pool.data_center_id = ? and storage_pool.scope = ? and (";
    private final String ZoneWideTagsSqlSuffix = ") GROUP BY storage_pool_tags.pool_id HAVING COUNT(storage_pool_tags.tag) >= ?";
    private final String ZoneWideStorageAccessGroupsForHostConnectionSqlPrefix = "SELECT storage_pool.* from storage_pool LEFT JOIN storage_pool_and_access_group_map ON storage_pool.id = storage_pool_and_access_group_map.pool_id WHERE storage_pool.removed is null and storage_pool.status = 'Up' and storage_pool.data_center_id = ? and storage_pool.scope = ? and (";
    private final String ZoneWideStorageAccessGroupsForHostConnectionSqlSuffix = ") GROUP BY storage_pool_and_access_group_map.pool_id";
    private final String ZoneWideStorageAccessGroupsWithHypervisorTypeSqlPrefix = "SELECT storage_pool.* from storage_pool LEFT JOIN storage_pool_and_access_group_map ON storage_pool.id = storage_pool_and_access_group_map.pool_id WHERE storage_pool.removed is null and storage_pool.status = 'Up' and storage_pool.hypervisor = ? and storage_pool.data_center_id = ? and storage_pool.scope = ? and (";
    private final String ZoneWideStorageAccessGroupsWithHypervisorTypeSqlSuffix = ") GROUP BY storage_pool_and_access_group_map.pool_id";

    // Storage tags are now separate from storage_pool_details, leaving only details on that table
    protected final String TagsSqlPrefix = "SELECT storage_pool.* from storage_pool LEFT JOIN storage_pool_tags ON storage_pool.id = storage_pool_tags.pool_id WHERE storage_pool.removed is null and storage_pool.status = 'Up' AND storage_pool_tags.is_tag_a_rule = 0 and storage_pool.data_center_id = ? and (storage_pool.pod_id = ? or storage_pool.pod_id is null) and storage_pool.scope = ? and (";
    protected final String TagsSqlSuffix = ") GROUP BY storage_pool_tags.pool_id HAVING COUNT(storage_pool_tags.tag) >= ?";
    protected final String SAGsForHostConnectionSqlPrefix = "SELECT storage_pool.* from storage_pool LEFT JOIN storage_pool_and_access_group_map ON storage_pool.id = storage_pool_and_access_group_map.pool_id WHERE storage_pool.removed is null and storage_pool.status = 'Up' and storage_pool.data_center_id = ? and (storage_pool.pod_id = ? or storage_pool.pod_id is null) and storage_pool.scope = ? and (";

    protected final String SAGsForHostConnectionSqlSuffix = ") GROUP BY storage_pool_and_access_group_map.pool_id";

    private static final String GET_STORAGE_POOLS_OF_VOLUMES_WITHOUT_OR_NOT_HAVING_TAGS = "SELECT s.* " +
            "FROM volumes vol " +
            "JOIN storage_pool s ON vol.pool_id = s.id " +
            "WHERE vol.disk_offering_id = ? AND vol.state NOT IN (\"Destroy\", \"Error\", \"Expunging\", \"Expunged\") GROUP BY s.id";

    /**
     * Used in method findPoolsByDetailsOrTagsInternal
     */
    protected enum ValueType {
        DETAILS, TAGS;
    }

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
        AllFieldSearch.and("storage_provider_name", AllFieldSearch.entity().getStorageProviderName(), Op.EQ);
        AllFieldSearch.and("poolType", AllFieldSearch.entity().getPoolType(), Op.EQ);
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

        ClustersSearch = createSearchBuilder();
        ClustersSearch.and("clusterIds", ClustersSearch.entity().getClusterId(), Op.IN);
        ClustersSearch.and("status", ClustersSearch.entity().getStatus(), Op.EQ);
        ClustersSearch.done();

        IdsSearch = createSearchBuilder();
        IdsSearch.and("ids", IdsSearch.entity().getId(), SearchCriteria.Op.IN);
        IdsSearch.done();

    }

    @Override
    public List<StoragePoolVO> findPoolByName(String name) {
        SearchCriteria<StoragePoolVO> sc = AllFieldSearch.create();
        sc.setParameters("name", name);
        return listIncludingRemovedBy(sc);
    }

    @Override
    public List<StoragePoolVO> findPoolsByProvider(String provider) {
        SearchCriteria<StoragePoolVO> sc = AllFieldSearch.create();
        sc.setParameters("storage_provider_name", provider);
        return listBy(sc);
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
        return listBy(datacenterId, podId, clusterId, scope, null);
    }

    @Override
    public List<StoragePoolVO> listBy(long datacenterId, Long podId, Long clusterId, ScopeType scope, String keyword) {
        SearchCriteria<StoragePoolVO> sc = null;
        if (clusterId != null) {
            sc = DcPodSearch.create();
            sc.setParameters("cluster", clusterId);
        } else {
            sc = DcPodAnyClusterSearch.create();

        }
        sc.setParameters("datacenterId", datacenterId);
        sc.setParameters("podId", podId);
        sc.setParameters("status", Status.Up);
        if (keyword != null) {
            sc.addAnd("name", Op.LIKE,  "%" + keyword + "%");
        }
        if (scope != null) {
            sc.setParameters("scope", scope);
        }
        return listBy(sc);
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
    public StoragePoolVO persist(StoragePoolVO pool, Map<String, String> details, List<String> tags, Boolean isTagARule, List<String> storageAccessGroups) {
        return persist(pool, details, tags, isTagARule, true, storageAccessGroups);
    }

    @Override
    @DB
    public StoragePoolVO persist(StoragePoolVO pool, Map<String, String> details, List<String> tags, Boolean isTagARule, boolean displayDetails, List<String> storageAccessGroups) {
        TransactionLegacy txn = TransactionLegacy.currentTxn();
        txn.start();
        pool = super.persist(pool);
        if (details != null) {
            for (Map.Entry<String, String> detail : details.entrySet()) {
                StoragePoolDetailVO vo = new StoragePoolDetailVO(pool.getId(), detail.getKey(), detail.getValue(), displayDetails);
                _detailsDao.persist(vo);
            }
        }
        if (CollectionUtils.isNotEmpty(tags)) {
            _tagsDao.persist(pool.getId(), tags, isTagARule);
        }
        if (CollectionUtils.isNotEmpty(storageAccessGroups)) {
            _storagePoolAccessGroupMapDao.persist(pool.getId(), storageAccessGroups);
        }
        txn.commit();
        return pool;
    }

    /**
     * Internal helper method to retrieve storage pools by given details or storage tags.
     * @param dcId data center id
     * @param podId pod id
     * @param clusterId cluster id
     * @param scope score
     * @param sqlValues sql string containing details or storage tags values required to query
     * @param valuesType enumerate to indicate if values are related to details or storage tags
     * @param valuesLength values length
     * @return list of storage pools matching conditions
     */
    protected List<StoragePoolVO> findPoolsByDetailsOrTagsInternal(long dcId, long podId, Long clusterId, ScopeType scope, String sqlValues, ValueType valuesType,
            int valuesLength) {
        String sqlPrefix = valuesType.equals(ValueType.DETAILS) ? DetailsSqlPrefix : TagsSqlPrefix;
        String sqlSuffix = valuesType.equals(ValueType.DETAILS) ? DetailsSqlSuffix : TagsSqlSuffix;
        String sql = getSqlPreparedStatement(sqlPrefix, sqlSuffix, sqlValues, clusterId);
        return searchStoragePoolsPreparedStatement(sql, dcId, podId, clusterId, scope, valuesLength);
    }

    protected List<StoragePoolVO> findPoolsByDetailsOrTagsForHostConnectionInternal(long dcId, long podId, Long clusterId, ScopeType scope, String sqlValues, ValueType valuesType) {
        String sqlPrefix = valuesType.equals(ValueType.DETAILS) ? DetailsSqlPrefix : SAGsForHostConnectionSqlPrefix;
        String sqlSuffix = valuesType.equals(ValueType.DETAILS) ? DetailsForHostConnectionSqlSuffix : SAGsForHostConnectionSqlSuffix;
        String sql = getSqlPreparedStatement(sqlPrefix, sqlSuffix, sqlValues, clusterId);
        return searchStoragePoolsPreparedStatement(sql, dcId, podId, clusterId, scope, null);
    }

    /**
     * Search storage pools in a transaction
     * @param sql prepared statement sql
     * @param dcId data center id
     * @param podId pod id
     * @param clusterId cluster id
     * @param scope scope
     * @param valuesLength values length
     * @return storage pools matching criteria
     */
    @DB
    protected List<StoragePoolVO> searchStoragePoolsWithHypervisorTypesPreparedStatement(String sql, HypervisorType type, long dcId, Long podId, Long clusterId, ScopeType scope, Integer valuesLength) {
        TransactionLegacy txn = TransactionLegacy.currentTxn();
        List<StoragePoolVO> pools = new ArrayList<StoragePoolVO>();
        try (PreparedStatement pstmt = txn.prepareStatement(sql);) {
            if (pstmt != null) {
                int i = 1;
                pstmt.setString(i++, type.toString());
                pstmt.setLong(i++, dcId);
                if (podId != null) {
                    pstmt.setLong(i++, podId);
                }
                pstmt.setString(i++, scope.toString());
                if (clusterId != null) {
                    pstmt.setLong(i++, clusterId);
                }
                if (valuesLength != null) {
                    pstmt.setInt(i++, valuesLength);
                }
                try (ResultSet rs = pstmt.executeQuery();) {
                    while (rs.next()) {
                        pools.add(toEntityBean(rs, false));
                    }
                } catch (SQLException e) {
                    throw new CloudRuntimeException("Unable to execute :" + e.getMessage(), e);
                }
            }
        } catch (SQLException e) {
            throw new CloudRuntimeException("Unable to execute :" + e.getMessage(), e);
        }
        return pools;
    }

    /**
     * Search storage pools in a transaction
     * @param sql prepared statement sql
     * @param dcId data center id
     * @param podId pod id
     * @param clusterId cluster id
     * @param scope scope
     * @param valuesLength values length
     * @return storage pools matching criteria
     */
    @DB
    protected List<StoragePoolVO> searchStoragePoolsPreparedStatement(String sql, long dcId, Long podId, Long clusterId, ScopeType scope, Integer valuesLength) {
        TransactionLegacy txn = TransactionLegacy.currentTxn();
        List<StoragePoolVO> pools = new ArrayList<StoragePoolVO>();
        try (PreparedStatement pstmt = txn.prepareStatement(sql);) {
            if (pstmt != null) {
                int i = 1;
                pstmt.setLong(i++, dcId);
                if (podId != null) {
                    pstmt.setLong(i++, podId);
                }
                pstmt.setString(i++, scope.toString());
                if (clusterId != null) {
                    pstmt.setLong(i++, clusterId);
                }
                if (valuesLength != null) {
                    pstmt.setInt(i++, valuesLength);
                }
                try (ResultSet rs = pstmt.executeQuery();) {
                    while (rs.next()) {
                        pools.add(toEntityBean(rs, false));
                    }
                } catch (SQLException e) {
                    throw new CloudRuntimeException("Unable to execute :" + e.getMessage(), e);
                }
            }
        } catch (SQLException e) {
            throw new CloudRuntimeException("Unable to execute :" + e.getMessage(), e);
        }
        return pools;
    }

    protected String getSqlPreparedStatement(String sqlPrefix, String sqlSuffix, String sqlValues, Long clusterId) {
        StringBuilder sql = new StringBuilder(sqlPrefix);
        if (clusterId != null) {
            sql.append("storage_pool.cluster_id = ? OR storage_pool.cluster_id IS NULL) AND (");
        }
        sql.append(sqlValues);
        sql.append(sqlSuffix);
        return sql.toString();
    }

    /**
     * Return SQL string from details, to be placed between SQL Prefix and SQL Suffix when creating storage tags PreparedStatement.
     * @param details storage pool details
     * @return SQL string containing storage tag values to be Prefix and Suffix when creating PreparedStatement.
     * @throws NullPointerException if details is null
     * @throws IndexOutOfBoundsException if details is not null, but empty
     */
    protected String getSqlValuesFromDetails(Map<String, String> details) {
        StringBuilder sqlValues = new StringBuilder();
        for (Map.Entry<String, String> detail : details.entrySet()) {
            sqlValues.append("((storage_pool_details.name='").append(detail.getKey()).append("') AND (storage_pool_details.value='").append(detail.getValue()).append("')) OR ");
        }
        sqlValues.delete(sqlValues.length() - 4, sqlValues.length());
        return sqlValues.toString();
    }

    /**
     * Return SQL string from storage tags, to be placed between SQL Prefix and SQL Suffix when creating storage tags PreparedStatement.
     * @param tags storage tags array
     * @return SQL string containing storage tag values to be placed between Prefix and Suffix when creating PreparedStatement.
     * @throws NullPointerException if tags is null
     * @throws IndexOutOfBoundsException if tags is not null, but empty
     */
    protected String getSqlValuesFromStorageTags(String[] tags) throws NullPointerException, IndexOutOfBoundsException {
        StringBuilder sqlValues = new StringBuilder();
        for (String tag : tags) {
            sqlValues.append("(storage_pool_tags.tag='").append(tag).append("') OR ");
        }
        sqlValues.delete(sqlValues.length() - 4, sqlValues.length());
        return sqlValues.toString();
    }

    /**
     * Return SQL string from storage pool access group map, to be placed between SQL Prefix and SQL Suffix when creating storage tags PreparedStatement.
     * @param storageAccessGroups storage tags array
     * @return SQL string containing storage tag values to be placed between Prefix and Suffix when creating PreparedStatement.
     * @throws NullPointerException if tags is null
     * @throws IndexOutOfBoundsException if tags is not null, but empty
     */
    protected String getSqlValuesFromStorageAccessGroups(String[] storageAccessGroups) throws NullPointerException, IndexOutOfBoundsException {
        StringBuilder sqlValues = new StringBuilder();
        for (String tag : storageAccessGroups) {
            sqlValues.append("(storage_pool_and_access_group_map.storage_access_group='").append(tag).append("') OR ");
        }
        sqlValues.delete(sqlValues.length() - 4, sqlValues.length());
        return sqlValues.toString();
    }

    @DB
    @Override
    public List<StoragePoolVO> findPoolsByDetails(long dcId, long podId, Long clusterId, Map<String, String> details, ScopeType scope) {
        String sqlValues = getSqlValuesFromDetails(details);
        return findPoolsByDetailsOrTagsInternal(dcId, podId, clusterId, scope, sqlValues, ValueType.DETAILS, details.size());
    }

    @Override
    public List<StoragePoolVO> findPoolsByTags(long dcId, long podId, Long clusterId, ScopeType scope, String[] tags, boolean validateTagRule, long ruleExecuteTimeout) {
        List<StoragePoolVO> storagePools = null;
        if (tags == null || tags.length == 0) {
            storagePools = listBy(dcId, podId, clusterId, scope);

            if (validateTagRule) {
                storagePools = getPoolsWithoutTagRule(storagePools);
            }

        } else {
            String sqlValues = getSqlValuesFromStorageTags(tags);
            storagePools = findPoolsByDetailsOrTagsInternal(dcId, podId, clusterId, scope, sqlValues, ValueType.TAGS, tags.length);
        }

        return storagePools;
    }

    @Override
    public List<StoragePoolVO> findPoolsByAccessGroupsForHostConnection(Long dcId, Long podId, Long clusterId, ScopeType scope, String[] storageAccessGroups) {
        List<StoragePoolVO> storagePools = null;
        if (storageAccessGroups == null || storageAccessGroups.length == 0) {
            storagePools = listBy(dcId, podId, clusterId, scope);
        } else {
            String sqlValues = getSqlValuesFromStorageAccessGroups(storageAccessGroups);
            storagePools = findPoolsByDetailsOrTagsForHostConnectionInternal(dcId, podId, clusterId, scope, sqlValues, ValueType.TAGS);
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
    public List<StoragePoolVO> findLocalStoragePoolsByTags(long dcId, long podId, Long clusterId, String[] tags, boolean validateTagRule) {
        return findLocalStoragePoolsByTags(dcId, podId, clusterId, tags, validateTagRule, null);
    }

    @Override
    public List<StoragePoolVO> findLocalStoragePoolsByTags(long dcId, long podId, Long clusterId, String[] tags, boolean validateTagRule, String keyword) {
        List<StoragePoolVO> storagePools = null;
        if (tags == null || tags.length == 0) {
            storagePools = listBy(dcId, podId, clusterId, ScopeType.HOST, keyword);

            if (validateTagRule) {
                storagePools = getPoolsWithoutTagRule(storagePools);
            }
        } else {
            String sqlValues = getSqlValuesFromStorageTags(tags);
            storagePools = findPoolsByDetailsOrTagsInternal(dcId, podId, clusterId, ScopeType.HOST, sqlValues, ValueType.TAGS, tags.length);
        }

        return storagePools;
    }

    @Override
    public List<StoragePoolVO> findLocalStoragePoolsByHostAndTags(long hostId, String[] tags) {
        SearchBuilder<StoragePoolVO> hostSearch = createSearchBuilder();
        SearchBuilder<StoragePoolHostVO> hostPoolSearch = _hostDao.createSearchBuilder();
        SearchBuilder<StoragePoolTagVO> tagPoolSearch = _tagsDao.createSearchBuilder();
        ;

        // Search for pools on the host
        hostPoolSearch.and("hostId", hostPoolSearch.entity().getHostId(), Op.EQ);
        // Set criteria for pools
        hostSearch.and("scope", hostSearch.entity().getScope(), Op.EQ);
        hostSearch.and("removed", hostSearch.entity().getRemoved(), Op.NULL);
        hostSearch.and("status", hostSearch.entity().getStatus(), Op.EQ);
        hostSearch.join("hostJoin", hostPoolSearch, hostSearch.entity().getId(), hostPoolSearch.entity().getPoolId(), JoinBuilder.JoinType.INNER);

        if (!(tags == null || tags.length == 0)) {
            tagPoolSearch.and("tag", tagPoolSearch.entity().getTag(), Op.EQ);
            hostSearch.join("tagJoin", tagPoolSearch, hostSearch.entity().getId(), tagPoolSearch.entity().getPoolId(), JoinBuilder.JoinType.INNER);
        }

        SearchCriteria<StoragePoolVO> sc = hostSearch.create();
        sc.setJoinParameters("hostJoin", "hostId", hostId);
        sc.setParameters("scope", ScopeType.HOST.toString());
        sc.setParameters("status", Status.Up.toString());

        if (!(tags == null || tags.length == 0)) {
            for (String tag : tags) {
                sc.setJoinParameters("tagJoin", "tag", tag);
            }
        }
        return listBy(sc);
    }

    @Override
    public List<StoragePoolVO> findZoneWideStoragePoolsByTags(long dcId, String[] tags, boolean validateTagRule) {
        if (tags == null || tags.length == 0) {
            QueryBuilder<StoragePoolVO> sc = QueryBuilder.create(StoragePoolVO.class);
            sc.and(sc.entity().getDataCenterId(), Op.EQ, dcId);
            sc.and(sc.entity().getStatus(), Op.EQ, Status.Up);
            sc.and(sc.entity().getScope(), Op.EQ, ScopeType.ZONE);

            List<StoragePoolVO> storagePools = sc.list();

            if (validateTagRule) {
                storagePools = getPoolsWithoutTagRule(storagePools);
            }

            return storagePools;
        } else {
            String sqlValues = getSqlValuesFromStorageTags(tags);
            String sql = getSqlPreparedStatement(ZoneWideTagsSqlPrefix, ZoneWideTagsSqlSuffix, sqlValues, null);
            return searchStoragePoolsPreparedStatement(sql, dcId, null, null, ScopeType.ZONE, tags.length);
        }
    }

    protected List<StoragePoolVO> getPoolsWithoutTagRule(List<StoragePoolVO> storagePools) {
        List<StoragePoolVO> storagePoolsToReturn = new ArrayList<>();
        for (StoragePoolVO storagePool : storagePools) {

            List<StoragePoolTagVO> poolTags = _tagsDao.findStoragePoolTags(storagePool.getId());

            if (CollectionUtils.isEmpty(poolTags) || !poolTags.get(0).isTagARule()) {
                storagePoolsToReturn.add(storagePool);
            }
        }

        return storagePoolsToReturn;
    }

    @Override
    public List<StoragePoolVO> findZoneWideStoragePoolsByAccessGroupsForHostConnection(long dcId, String[] storageAccessGroups) {
        if (storageAccessGroups == null || storageAccessGroups.length == 0) {
            QueryBuilder<StoragePoolVO> sc = QueryBuilder.create(StoragePoolVO.class);
            sc.and(sc.entity().getDataCenterId(), Op.EQ, dcId);
            sc.and(sc.entity().getStatus(), Op.EQ, Status.Up);
            sc.and(sc.entity().getScope(), Op.EQ, ScopeType.ZONE);
            return sc.list();
        } else {
            String sqlValues = getSqlValuesFromStorageAccessGroups(storageAccessGroups);
            String sql = getSqlPreparedStatement(ZoneWideStorageAccessGroupsForHostConnectionSqlPrefix, ZoneWideStorageAccessGroupsForHostConnectionSqlSuffix, sqlValues, null);
            return searchStoragePoolsPreparedStatement(sql, dcId, null, null, ScopeType.ZONE, null);
        }
    }

    @Override
    public List<StoragePoolVO> findZoneWideStoragePoolsByAccessGroupsAndHypervisorTypeForHostConnection(long dcId, String[] storageAccessGroups, HypervisorType type) {
        if (storageAccessGroups == null || storageAccessGroups.length == 0) {
            QueryBuilder<StoragePoolVO> sc = QueryBuilder.create(StoragePoolVO.class);
            sc.and(sc.entity().getDataCenterId(), Op.EQ, dcId);
            sc.and(sc.entity().getStatus(), Op.EQ, Status.Up);
            sc.and(sc.entity().getScope(), Op.EQ, ScopeType.ZONE);
            sc.and(sc.entity().getHypervisor(), Op.EQ, type);
            return sc.list();
        } else {
            String sqlValues = getSqlValuesFromStorageAccessGroups(storageAccessGroups);
            String sql = getSqlPreparedStatement(ZoneWideStorageAccessGroupsWithHypervisorTypeSqlPrefix, ZoneWideStorageAccessGroupsWithHypervisorTypeSqlSuffix, sqlValues, null);
            return searchStoragePoolsWithHypervisorTypesPreparedStatement(sql, type, dcId, null, null, ScopeType.ZONE, null);
        }
    }

    @Override
    public List<StoragePoolVO> findStoragePoolsByEmptyStorageAccessGroups(Long dcId, Long podId, Long clusterId, ScopeType scope, HypervisorType hypervisorType) {
        SearchBuilder<StoragePoolVO> poolSearch = createSearchBuilder();
        SearchBuilder<StoragePoolAndAccessGroupMapVO> storageAccessGroupsPoolSearch = _storagePoolAccessGroupMapDao.createSearchBuilder();
        // Set criteria for pools
        poolSearch.and("scope", poolSearch.entity().getScope(), Op.EQ);
        poolSearch.and("removed", poolSearch.entity().getRemoved(), Op.NULL);
        poolSearch.and("status", poolSearch.entity().getStatus(), Op.EQ);
        poolSearch.and("datacenterid", poolSearch.entity().getDataCenterId(), Op.EQ);
        poolSearch.and("podid", poolSearch.entity().getPodId(), Op.EQ);
        poolSearch.and("clusterid", poolSearch.entity().getClusterId(), Op.EQ);
        poolSearch.and("hypervisortype", poolSearch.entity().getHypervisor(), Op.EQ);

        // Set StoragePoolAccessGroupMapVO.pool_id IS NULL. This ensures only pools without tags are returned
        storageAccessGroupsPoolSearch.and("poolid", storageAccessGroupsPoolSearch.entity().getPoolId(), Op.NULL);
        poolSearch.join("tagJoin", storageAccessGroupsPoolSearch, poolSearch.entity().getId(), storageAccessGroupsPoolSearch.entity().getPoolId(), JoinBuilder.JoinType.LEFT);

        SearchCriteria<StoragePoolVO> sc = poolSearch.create();
        sc.setParameters("scope", scope.toString());
        sc.setParameters("status", Status.Up.toString());

        if (dcId != null) {
            sc.setParameters("datacenterid", dcId);
        }

        if (podId != null) {
            sc.setParameters("podid", podId);
        }

        if (clusterId != null) {
            sc.setParameters("clusterid", clusterId);
        }

        if (hypervisorType != null) {
            sc.setParameters("hypervisortype", hypervisorType);
        }

        return listBy(sc);
    }

    @Override
    public List<String> searchForStoragePoolTags(long poolId) {
        return _tagsDao.getStoragePoolTags(poolId);
    }

    @Override
    public void updateDetails(long poolId, Map<String, String> details) {
        if (details != null) {
            List<StoragePoolDetailVO> detailsVO = new ArrayList<StoragePoolDetailVO>();
            for (String key : details.keySet()) {
                detailsVO.add(new StoragePoolDetailVO(poolId, key, details.get(key), true));
            }
            _detailsDao.saveDetails(detailsVO);
            if (details.size() == 0) {
                _detailsDao.removeDetails(poolId);
            }
        }
    }

    @Override
    public void removeDetails(long poolId) {
        _detailsDao.removeDetails(poolId);
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
        return findZoneWideStoragePoolsByHypervisor(dataCenterId, hypervisorType, null);
    }

    @Override
    public List<StoragePoolVO> findZoneWideStoragePoolsByHypervisor(long dataCenterId, HypervisorType hypervisorType, String keyword) {
        QueryBuilder<StoragePoolVO> sc = QueryBuilder.create(StoragePoolVO.class);
        sc.and(sc.entity().getDataCenterId(), Op.EQ, dataCenterId);
        sc.and(sc.entity().getStatus(), Op.EQ, Status.Up);
        sc.and(sc.entity().getScope(), Op.EQ, ScopeType.ZONE);
        sc.and(sc.entity().getHypervisor(), Op.EQ, hypervisorType);
        if (keyword != null) {
            sc.and(sc.entity().getName(), Op.LIKE,  "%" + keyword + "%");
        }
        return sc.list();
    }

    @Override
    public List<StoragePoolVO> findZoneWideStoragePoolsByHypervisorAndPoolType(long dataCenterId, HypervisorType hypervisorType, Storage.StoragePoolType poolType) {
        QueryBuilder<StoragePoolVO> sc = QueryBuilder.create(StoragePoolVO.class);
        sc.and(sc.entity().getDataCenterId(), Op.EQ, dataCenterId);
        sc.and(sc.entity().getStatus(), Op.EQ, StoragePoolStatus.Up);
        sc.and(sc.entity().getScope(), Op.EQ, ScopeType.ZONE);
        sc.and(sc.entity().getHypervisor(), Op.EQ, hypervisorType);
        sc.and(sc.entity().getPoolType(), Op.EQ, poolType);
        return sc.list();
    }

    @Override
    public List<StoragePoolVO> findClusterWideStoragePoolsByHypervisorAndPoolType(long clusterId, HypervisorType hypervisorType, Storage.StoragePoolType poolType) {
        QueryBuilder<StoragePoolVO> sc = QueryBuilder.create(StoragePoolVO.class);
        sc.and(sc.entity().getClusterId(), Op.EQ, clusterId);
        sc.and(sc.entity().getStatus(), Op.EQ, StoragePoolStatus.Up);
        sc.and(sc.entity().getScope(), Op.EQ, ScopeType.CLUSTER);
        sc.and(sc.entity().getHypervisor(), Op.EQ, hypervisorType);
        sc.and(sc.entity().getPoolType(), Op.EQ, poolType);
        return sc.list();
    }

    @Override
    public void deletePoolTags(long poolId) {
        _tagsDao.deleteTags(poolId);
    }

    @Override
    public void deleteStoragePoolAccessGroups(long poolId) {
        _storagePoolAccessGroupMapDao.deleteStorageAccessGroups(poolId);
    }

    @Override
    public List<StoragePoolVO> listChildStoragePoolsInDatastoreCluster(long poolId) {
        QueryBuilder<StoragePoolVO> sc = QueryBuilder.create(StoragePoolVO.class);
        sc.and(sc.entity().getParent(), Op.EQ, poolId);
        return sc.list();
    }

    @Override
    public Integer countAll() {
        SearchCriteria<StoragePoolVO> sc = createSearchCriteria();
        sc.addAnd("parent", SearchCriteria.Op.EQ, 0);
        sc.addAnd("removed", SearchCriteria.Op.NULL);
        return getCount(sc);
    }

    @Override
    public List<StoragePoolVO> findPoolsInClusters(List<Long> clusterIds, String keyword) {
        SearchCriteria<StoragePoolVO> sc = ClustersSearch.create();
        sc.setParameters("clusterIds", clusterIds.toArray());
        sc.setParameters("status", StoragePoolStatus.Up);
        if (keyword != null) {
            sc.addAnd("name", Op.LIKE, "%" + keyword + "%");
        }
        return listBy(sc);
    }

    @Override
    public List<StoragePoolVO> findPoolsByStorageType(Storage.StoragePoolType storageType) {
        SearchCriteria<StoragePoolVO> sc = AllFieldSearch.create();
        sc.setParameters("poolType", storageType);
        return listBy(sc);
    }

    @Override
    public StoragePoolVO findPoolByZoneAndPath(long zoneId, String datastorePath) {
        SearchCriteria<StoragePoolVO> sc = AllFieldSearch.create();
        sc.setParameters("datacenterId", zoneId);
        if (datastorePath != null) {
            sc.addAnd("path", Op.LIKE,  "%/" + datastorePath);
        }
        return findOneBy(sc);
    }

    @Override
    public List<StoragePoolVO> listStoragePoolsWithActiveVolumesByOfferingId(long offeringId) {
        TransactionLegacy txn = TransactionLegacy.currentTxn();
        PreparedStatement pstmt = null;
        List<StoragePoolVO> result = new ArrayList<>();
        StringBuilder sql = new StringBuilder(GET_STORAGE_POOLS_OF_VOLUMES_WITHOUT_OR_NOT_HAVING_TAGS);
        try {
            pstmt = txn.prepareAutoCloseStatement(sql.toString());
            pstmt.setLong(1, offeringId);
            ResultSet rs = pstmt.executeQuery();
            while (rs.next()) {
                result.add(toEntityBean(rs, false));
            }
            return result;
        } catch (SQLException e) {
            throw new CloudRuntimeException("DB Exception on: " + sql, e);
        } catch (Throwable e) {
            throw new CloudRuntimeException("Caught: " + sql, e);
        }
    }

    @Override
    public Pair<List<Long>, Integer> searchForIdsAndCount(Long storagePoolId, String storagePoolName, Long zoneId,
            String path, Long podId, Long clusterId, Long hostId, String address, ScopeType scopeType, StoragePoolStatus status,
            String keyword, String storageAccessGroup, Filter searchFilter) {
        SearchCriteria<StoragePoolVO> sc = createStoragePoolSearchCriteria(storagePoolId, storagePoolName, zoneId, path, podId, clusterId,
                hostId, address, scopeType, status, keyword, storageAccessGroup);
        Pair<List<StoragePoolVO>, Integer> uniquePair = searchAndCount(sc, searchFilter);
        List<Long> idList = uniquePair.first().stream().map(StoragePoolVO::getId).collect(Collectors.toList());
        return new Pair<>(idList, uniquePair.second());
    }

    @Override
    public List<StoragePoolVO> listByIds(List<Long> ids) {
        if (CollectionUtils.isEmpty(ids)) {
            return Collections.emptyList();
        }
        SearchCriteria<StoragePoolVO> sc = IdsSearch.create();
        sc.setParameters("ids", ids.toArray());
        return listBy(sc);
    }

    @Override
    public List<StoragePoolVO> findPoolsByStorageTypeAndZone(Storage.StoragePoolType storageType, Long zoneId) {
        SearchCriteria<StoragePoolVO> sc = AllFieldSearch.create();
        sc.setParameters("poolType", storageType);
        sc.addAnd("dataCenterId", Op.EQ, zoneId);
        return listBy(sc);
    }

    private SearchCriteria<StoragePoolVO> createStoragePoolSearchCriteria(Long storagePoolId, String storagePoolName,
                                                                          Long zoneId, String path, Long podId, Long clusterId, Long hostId, String address, ScopeType scopeType,
                                                                          StoragePoolStatus status, String keyword, String storageAccessGroup) {
        SearchBuilder<StoragePoolVO> sb = createSearchBuilder();
        sb.select(null, SearchCriteria.Func.DISTINCT, sb.entity().getId()); // select distinct
        // ids
        sb.and("id", sb.entity().getId(), SearchCriteria.Op.EQ);
        sb.and("name", sb.entity().getName(), SearchCriteria.Op.EQ);
        sb.and("path", sb.entity().getPath(), SearchCriteria.Op.EQ);
        sb.and("dataCenterId", sb.entity().getDataCenterId(), SearchCriteria.Op.EQ);
        sb.and("podId", sb.entity().getPodId(), SearchCriteria.Op.EQ);
        sb.and("clusterId", sb.entity().getClusterId(), SearchCriteria.Op.EQ);
        sb.and("hostAddress", sb.entity().getHostAddress(), SearchCriteria.Op.EQ);
        sb.and("scope", sb.entity().getScope(), SearchCriteria.Op.EQ);
        sb.and("status", sb.entity().getStatus(), SearchCriteria.Op.EQ);
        sb.and("parent", sb.entity().getParent(), SearchCriteria.Op.EQ);

        if (hostId != null) {
            SearchBuilder<StoragePoolHostVO> hostJoin = _hostDao.createSearchBuilder();
            hostJoin.and("hostId", hostJoin.entity().getHostId(), SearchCriteria.Op.EQ);
            sb.join("poolHostJoin", hostJoin, sb.entity().getId(), hostJoin.entity().getPoolId(), JoinBuilder.JoinType.INNER);
        }

        if (storageAccessGroup != null) {
            SearchBuilder<StoragePoolAndAccessGroupMapVO> storageAccessGroupJoin = _storagePoolAccessGroupMapDao.createSearchBuilder();
            storageAccessGroupJoin.and("storageAccessGroup", storageAccessGroupJoin.entity().getStorageAccessGroup(), SearchCriteria.Op.EQ);
            sb.join("poolStorageAccessGroupJoin", storageAccessGroupJoin, sb.entity().getId(), storageAccessGroupJoin.entity().getPoolId(), JoinBuilder.JoinType.INNER);
        }

        SearchCriteria<StoragePoolVO> sc = sb.create();

        if (keyword != null) {
            SearchCriteria<StoragePoolVO> ssc = createSearchCriteria();
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
            SearchCriteria<StoragePoolVO> ssc = createSearchCriteria();
            ssc.addOr("podId", SearchCriteria.Op.EQ, podId);
            ssc.addOr("podId", SearchCriteria.Op.NULL);

            sc.addAnd("podId", SearchCriteria.Op.SC, ssc);
        }
        if (address != null) {
            sc.setParameters("hostAddress", address);
        }
        if (clusterId != null) {
            SearchCriteria<StoragePoolVO> ssc = createSearchCriteria();
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

        if (hostId != null) {
            sc.setJoinParameters("poolHostJoin", "hostId", hostId);
        }

        if (storageAccessGroup != null) {
            sc.setJoinParameters("poolStorageAccessGroupJoin", "storageAccessGroup", storageAccessGroup);
        }

        return sc;
    }
}
