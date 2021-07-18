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
package com.cloud.capacity.dao;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.inject.Inject;

import org.apache.log4j.Logger;
import org.springframework.stereotype.Component;

import org.apache.cloudstack.storage.datastore.db.PrimaryDataStoreDao;
import org.apache.cloudstack.storage.datastore.db.StoragePoolVO;

import com.cloud.capacity.Capacity;
import com.cloud.capacity.CapacityVO;
import com.cloud.dc.ClusterDetailsDao;
import com.cloud.storage.Storage;
import com.cloud.utils.Pair;
import com.cloud.utils.Ternary;
import com.cloud.utils.db.GenericDaoBase;
import com.cloud.utils.db.GenericSearchBuilder;
import com.cloud.utils.db.JoinBuilder.JoinType;
import com.cloud.utils.db.SearchBuilder;
import com.cloud.utils.db.SearchCriteria;
import com.cloud.utils.db.SearchCriteria.Func;
import com.cloud.utils.db.SearchCriteria.Op;
import com.cloud.utils.db.TransactionLegacy;
import com.cloud.utils.exception.CloudRuntimeException;

@Component
public class CapacityDaoImpl extends GenericDaoBase<CapacityVO, Long> implements CapacityDao {
    private static final Logger s_logger = Logger.getLogger(CapacityDaoImpl.class);

    private static final String ADD_ALLOCATED_SQL = "UPDATE `cloud`.`op_host_capacity` SET used_capacity = used_capacity + ? WHERE host_id = ? AND capacity_type = ?";
    private static final String SUBTRACT_ALLOCATED_SQL =
            "UPDATE `cloud`.`op_host_capacity` SET used_capacity = used_capacity - ? WHERE host_id = ? AND capacity_type = ?";

    private static final String LIST_CLUSTERSINZONE_BY_HOST_CAPACITIES_PART1 =
            "SELECT DISTINCT capacity.cluster_id  FROM `cloud`.`op_host_capacity` capacity INNER JOIN `cloud`.`cluster` cluster on (cluster.id = capacity.cluster_id AND cluster.removed is NULL)   INNER JOIN `cloud`.`cluster_details` cluster_details ON (cluster.id = cluster_details.cluster_id ) WHERE ";
    private static final String LIST_CLUSTERSINZONE_BY_HOST_CAPACITIES_PART2 =
            " AND capacity_type = ? AND cluster_details.name= ? AND ((total_capacity * cluster_details.value ) - used_capacity + reserved_capacity) >= ? AND capacity.cluster_id IN (SELECT distinct capacity.cluster_id  FROM `cloud`.`op_host_capacity` capacity INNER JOIN  `cloud`.`cluster_details` cluster_details ON (capacity.cluster_id = cluster_details.cluster_id ) WHERE ";
    private static final String LIST_CLUSTERSINZONE_BY_HOST_CAPACITIES_PART3 =
            " AND capacity_type = ? AND cluster_details.name= ? AND ((total_capacity * cluster_details.value) - used_capacity + reserved_capacity) >= ?) ";

    private final SearchBuilder<CapacityVO> _hostIdTypeSearch;
    private final SearchBuilder<CapacityVO> _hostOrPoolIdSearch;
    private final SearchBuilder<CapacityVO> _allFieldsSearch;
    @Inject
    protected PrimaryDataStoreDao _storagePoolDao;
    @Inject
    protected ClusterDetailsDao _clusterDetailsDao;

    private static final String LIST_HOSTS_IN_CLUSTER_WITH_ENOUGH_CAPACITY =
            " SELECT  host_capacity.host_id FROM (`cloud`.`host` JOIN `cloud`.`op_host_capacity` host_capacity ON (host.id = host_capacity.host_id AND host.cluster_id = ?) JOIN `cloud`.`cluster_details` cluster_details ON (host_capacity.cluster_id = cluster_details.cluster_id) AND  host.type = ? AND cluster_details.name='cpuOvercommitRatio' AND ((host_capacity.total_capacity *cluster_details.value ) - host_capacity.used_capacity) >= ? and host_capacity.capacity_type = '1' "
                    + " AND  host_capacity.host_id IN (SELECT capacity.host_id FROM `cloud`.`op_host_capacity` capacity JOIN `cloud`.`cluster_details` cluster_details ON (capacity.cluster_id= cluster_details.cluster_id) where capacity_type='0' AND cluster_details.name='memoryOvercommitRatio' AND ((total_capacity* cluster_details.value) - used_capacity ) >= ?)) ";

    private static final String ORDER_CLUSTERS_BY_AGGREGATE_CAPACITY_PART1 =
            "SELECT capacity.cluster_id, SUM(used_capacity+reserved_capacity)/SUM(total_capacity ) FROM `cloud`.`op_host_capacity` capacity WHERE ";

    private static final String ORDER_CLUSTERS_BY_AGGREGATE_CAPACITY_PART2 =
            " AND capacity_type = ?  AND cluster_details.name =? GROUP BY capacity.cluster_id ORDER BY SUM(used_capacity+reserved_capacity)/SUM(total_capacity * cluster_details.value) ASC";

    private static final String ORDER_CLUSTERS_BY_AGGREGATE_OVERCOMMIT_CAPACITY_PART1 =
            "SELECT capacity.cluster_id, SUM(used_capacity+reserved_capacity)/SUM(total_capacity * cluster_details.value) FROM `cloud`.`op_host_capacity` capacity INNER JOIN `cloud`.`cluster_details` cluster_details ON (capacity.cluster_id = cluster_details.cluster_id) WHERE ";

    private static final String ORDER_CLUSTERS_BY_AGGREGATE_OVERCOMMIT_CAPACITY_PART2 =
            " AND capacity_type = ?  AND cluster_details.name =? GROUP BY capacity.cluster_id ORDER BY SUM(used_capacity+reserved_capacity)/SUM(total_capacity * cluster_details.value) ASC";

    private static final String LIST_PODSINZONE_BY_HOST_CAPACITY_TYPE =
            "SELECT DISTINCT capacity.pod_id FROM `cloud`.`op_host_capacity` capacity INNER JOIN `cloud`.`host_pod_ref` pod "
                    + " ON (pod.id = capacity.pod_id AND pod.removed is NULL) INNER JOIN `cloud`.`cluster_details` cluster_details ON (capacity.cluster_id = cluster_details.cluster_id ) WHERE capacity.data_center_id = ? AND capacity_type = ? AND cluster_details.name= ? AND ((total_capacity * cluster_details.value ) - used_capacity + reserved_capacity) >= ? ";

    private static final String ORDER_PODS_BY_AGGREGATE_CAPACITY =
            " SELECT capacity.pod_id, SUM(used_capacity+reserved_capacity)/SUM(total_capacity) FROM `cloud`.`op_host_capacity` capacity WHERE data_center_id= ? AND capacity_type = ? GROUP BY capacity.pod_id ORDER BY SUM(used_capacity+reserved_capacity)/SUM(total_capacity) ASC ";

    private static final String ORDER_PODS_BY_AGGREGATE_OVERCOMMIT_CAPACITY =
            "SELECT capacity.pod_id, SUM(used_capacity+reserved_capacity)/SUM(total_capacity * cluster_details.value) FROM `cloud`.`op_host_capacity` capacity INNER JOIN `cloud`.`cluster_details` cluster_details ON (capacity.cluster_id = cluster_details.cluster_id) WHERE data_center_id=? AND capacity_type = ?  AND cluster_details.name = ? GROUP BY capacity.pod_id ORDER BY SUM(used_capacity+reserved_capacity)/SUM(total_capacity * cluster_details.value) ASC";
    private static final String ORDER_HOSTS_BY_FREE_CAPACITY_PART1 = "SELECT host_id, SUM(total_capacity - (used_capacity+reserved_capacity))/SUM(total_capacity) FROM `cloud`.`op_host_capacity` WHERE "
                    + "capacity_type = ? ";
    private static final String ORDER_HOSTS_BY_FREE_CAPACITY_PART2 = " GROUP BY host_id ORDER BY SUM(total_capacity - (used_capacity+reserved_capacity))/SUM(total_capacity) DESC ";
    private static final String LIST_CAPACITY_BY_RESOURCE_STATE =
            "SELECT capacity.data_center_id, sum(capacity.used_capacity), sum(capacity.reserved_quantity), sum(capacity.total_capacity), capacity_capacity_type "
                    + "FROM `cloud`.`op_host_capacity` capacity INNER JOIN `cloud`.`data_center` dc ON (dc.id = capacity.data_center_id AND dc.removed is NULL)"
                    + "FROM `cloud`.`op_host_capacity` capacity INNER JOIN `cloud`.`host_pod_ref` pod ON (pod.id = capacity.pod_id AND pod.removed is NULL)"
                    + "FROM `cloud`.`op_host_capacity` capacity INNER JOIN `cloud`.`cluster` cluster ON (cluster.id = capacity.cluster_id AND cluster.removed is NULL)"
                    + "FROM `cloud`.`op_host_capacity` capacity INNER JOIN `cloud`.`host` host ON (host.id = capacity.host_id AND host.removed is NULL)"
                    + "WHERE dc.allocation_state = ? AND pod.allocation_state = ? AND cluster.allocation_state = ? AND host.resource_state = ? AND capacity_type not in (3,4) ";

    private static final String LIST_CAPACITY_GROUP_BY_ZONE_TYPE_PART1 =
            "SELECT sum(capacity.used_capacity), sum(capacity.reserved_capacity),"
                    + " (case capacity_type when 1 then (sum(total_capacity) * CAST((select value from `cloud`.`cluster_details` where cluster_details.name= 'cpuOvercommitRatio' AND cluster_details.cluster_id=capacity.cluster_id) AS DECIMAL (10,4)))"
                    + "when '0' then (sum(total_capacity) * CAST((select value from `cloud`.`cluster_details` where cluster_details.name= 'memoryOvercommitRatio' AND cluster_details.cluster_id=capacity.cluster_id) AS DECIMAL (10,4)))"
                    + "else sum(total_capacity) end),"
                    + "((sum(capacity.used_capacity) + sum(capacity.reserved_capacity)) / ( case capacity_type when 1 then (sum(total_capacity) * CAST((select value from `cloud`.`cluster_details` where cluster_details.name= 'cpuOvercommitRatio' AND cluster_details.cluster_id=capacity.cluster_id) AS DECIMAL (10,4)))"
                    + "when '0' then (sum(total_capacity) * CAST((select value from `cloud`.`cluster_details` where cluster_details.name='memoryOvercommitRatio' AND cluster_details.cluster_id=capacity.cluster_id) AS DECIMAL (10,4)))else sum(total_capacity) end)) percent,"
                    + "capacity.capacity_type, capacity.data_center_id, pod_id, cluster_id FROM `cloud`.`op_host_capacity` capacity WHERE  total_capacity > 0 AND data_center_id is not null AND capacity_state='Enabled'";

    private static final String LIST_CAPACITY_GROUP_BY_ZONE_TYPE_PART2 = " GROUP BY data_center_id, capacity_type order by percent desc limit ";
    private static final String LIST_CAPACITY_GROUP_BY_POD_TYPE_PART1 =
            "SELECT sum(capacity.used_capacity), sum(capacity.reserved_capacity),"
                    + " (case capacity_type when 1 then (sum(total_capacity) * CAST((select value from `cloud`.`cluster_details` where cluster_details.name= 'cpuOvercommitRatio' AND cluster_details.cluster_id=capacity.cluster_id) AS DECIMAL (10,4))) "
                    + "when '0' then (sum(total_capacity) * CAST((select value from `cloud`.`cluster_details` where cluster_details.name= 'memoryOvercommitRatio' AND cluster_details.cluster_id=capacity.cluster_id) AS DECIMAL (10,4)))else sum(total_capacity) end),"
                    + "((sum(capacity.used_capacity) + sum(capacity.reserved_capacity)) / ( case capacity_type when 1 then (sum(total_capacity) * CAST((select value from `cloud`.`cluster_details` where cluster_details.name= 'cpuOvercommitRatio' AND cluster_details.cluster_id=capacity.cluster_id) AS DECIMAL (10,4))) "
                    + "when '0' then (sum(total_capacity) * CAST((select value from `cloud`.`cluster_details` where cluster_details.name= 'memoryOvercommitRatio' AND cluster_details.cluster_id=capacity.cluster_id) AS DECIMAL (10,4)))else sum(total_capacity) end)) percent,"
                    + "capacity.capacity_type, capacity.data_center_id, pod_id, cluster_id  FROM `cloud`.`op_host_capacity` capacity WHERE  total_capacity > 0 AND data_center_id is not null AND capacity_state='Enabled' ";

    private static final String LIST_CAPACITY_GROUP_BY_POD_TYPE_PART2 = " GROUP BY pod_id, capacity_type order by percent desc limit ";

    private static final String LIST_CAPACITY_GROUP_BY_CLUSTER_TYPE_PART1 =
            "SELECT sum(capacity.used_capacity), sum(capacity.reserved_capacity),"
                    + " (case capacity_type when 1 then (sum(total_capacity) * CAST((select value from `cloud`.`cluster_details` where cluster_details.name= 'cpuOvercommitRatio' AND cluster_details.cluster_id=capacity.cluster_id) AS DECIMAL (10,4))) "
                    + "when '0' then (sum(total_capacity) * CAST((select value from `cloud`.`cluster_details` where cluster_details.name= 'memoryOvercommitRatio' AND cluster_details.cluster_id=capacity.cluster_id) AS DECIMAL (10,4)))else sum(total_capacity) end),"
                    + "((sum(capacity.used_capacity) + sum(capacity.reserved_capacity)) / ( case capacity_type when 1 then (sum(total_capacity) * CAST((select value from `cloud`.`cluster_details` where cluster_details.name= 'cpuOvercommitRatio' AND cluster_details.cluster_id=capacity.cluster_id) AS DECIMAL (10,4))) "
                    + "when '0' then (sum(total_capacity) * CAST((select value from `cloud`.`cluster_details` where cluster_details.name= 'memoryOvercommitRatio' AND cluster_details.cluster_id=capacity.cluster_id) AS DECIMAL (10,4)))else sum(total_capacity) end)) percent,"
                    + "capacity.capacity_type, capacity.data_center_id, pod_id, cluster_id FROM `cloud`.`op_host_capacity` capacity WHERE  total_capacity > 0 AND data_center_id is not null AND capacity_state='Enabled' ";

    private static final String LIST_CAPACITY_GROUP_BY_CLUSTER_TYPE_PART2 = " GROUP BY cluster_id, capacity_type, pod_id order by percent desc limit ";
    private static final String UPDATE_CAPACITY_STATE = "UPDATE `cloud`.`op_host_capacity` SET capacity_state = ? WHERE ";

    private static final String LIST_CAPACITY_GROUP_BY_CAPACITY_PART1=
            "SELECT sum(capacity.used_capacity), sum(capacity.reserved_capacity),"
                    + " (case capacity_type when 1 then sum(total_capacity * CAST((select value from `cloud`.`cluster_details` where cluster_details.name= 'cpuOvercommitRatio' AND cluster_details.cluster_id=capacity.cluster_id) AS DECIMAL (10,4))) "
                    + "when '0' then sum(total_capacity * CAST((select value from `cloud`.`cluster_details` where cluster_details.name= 'memoryOvercommitRatio' AND cluster_details.cluster_id=capacity.cluster_id) AS DECIMAL(10,4)))else sum(total_capacity) end),"
                    + "((sum(capacity.used_capacity) + sum(capacity.reserved_capacity)) / ( case capacity_type when 1 then sum(total_capacity * CAST((select value from `cloud`.`cluster_details` where cluster_details.name= 'cpuOvercommitRatio' AND cluster_details.cluster_id=capacity.cluster_id) AS DECIMAL(10,4))) "
                    + "when '0' then sum(total_capacity * CAST((select value from `cloud`.`cluster_details` where cluster_details.name= 'memoryOvercommitRatio' AND cluster_details.cluster_id=capacity.cluster_id) AS DECIMAL(10,4))) else sum(total_capacity) end)) percent,"
                    + "capacity.capacity_type, capacity.data_center_id, pod_id, cluster_id FROM `cloud`.`op_host_capacity` capacity WHERE  total_capacity > 0 AND data_center_id is not null AND capacity_state='Enabled' ";

    private static final String LIST_CAPACITY_GROUP_BY_CAPACITY_PART2 = " GROUP BY capacity_type";
    private static final String LIST_CAPACITY_GROUP_BY_CAPACITY_DATA_CENTER_POD_CLUSTER = " GROUP BY data_center_id, pod_id, cluster_id, capacity_type";

    /* In the below query"LIST_CLUSTERS_CROSSING_THRESHOLD" the threshold value is getting from the cluster_details table if not present then it gets from the global configuration
     *
     * CASE statement works like
     * if (cluster_details table has threshold value)
     * then
     *     if (value from the cluster_details table is not null)
     *     then
     *         query from the cluster_details table
     *     else
     *         query from the configuration table
     * else
     *     query from the configuration table
     *
     *     */

    private static final String LIST_CLUSTERS_CROSSING_THRESHOLD = "SELECT clusterList.cluster_id "
            +
            "FROM (SELECT cluster.cluster_id cluster_id, ( (sum(cluster.used) + sum(cluster.reserved) + ?)/sum(cluster.total) ) ratio, cluster.configValue value "
            +
            "FROM (SELECT capacity.cluster_id cluster_id, capacity.used_capacity used, capacity.reserved_capacity reserved, capacity.total_capacity * overcommit.value total, "
            +
            "CASE (SELECT count(*) FROM `cloud`.`cluster_details` details WHERE details.cluster_id = capacity.cluster_id AND details.name = ? ) "
            +
            "WHEN 1 THEN (CASE WHEN (SELECT details.value FROM `cloud`.`cluster_details` details WHERE details.cluster_id = capacity.cluster_id AND details.name = ?) is NULL "
            +
            "THEN (SELECT config.value FROM `cloud`.`configuration` config WHERE config.name = ?)" +
            "ELSE (SELECT details.value FROM `cloud`.`cluster_details` details WHERE details.cluster_id = capacity.cluster_id AND details.name = ? ) END )" +
            "ELSE (SELECT config.value FROM `cloud`.`configuration` config WHERE config.name = ?) " +
            "END configValue " +
            "FROM `cloud`.`op_host_capacity` capacity INNER JOIN `cloud`.`cluster_details` overcommit ON overcommit.cluster_id = capacity.cluster_id " +
            "WHERE capacity.data_center_id = ? AND capacity.capacity_type = ? AND capacity.total_capacity > 0 AND overcommit.name = ? AND capacity.capacity_state='Enabled') cluster " +

        "GROUP BY cluster.cluster_id)  clusterList " +
        "WHERE clusterList.ratio > clusterList.value; ";

    private static final String FIND_CLUSTER_CONSUMPTION_RATIO = "select ( (sum(capacity.used_capacity) + sum(capacity.reserved_capacity) + ?)/sum(capacity.total_capacity) ) "
            +
            "from op_host_capacity capacity where cluster_id = ? and capacity_type = ?;";

    private static final String LIST_ALLOCATED_CAPACITY_GROUP_BY_CAPACITY_AND_ZONE_PART1 = "SELECT v.data_center_id, SUM(cpu) AS cpucore, " +
                "SUM(cpu * speed) AS cpu, SUM(ram_size * 1024 * 1024) AS memory " +
                "FROM (SELECT vi.data_center_id, (CASE WHEN ISNULL(service_offering.cpu) THEN custom_cpu.value ELSE service_offering.cpu end) AS cpu, " +
                "(CASE WHEN ISNULL(service_offering.speed) THEN custom_speed.value ELSE service_offering.speed end) AS speed, " +
                "(CASE WHEN ISNULL(service_offering.ram_size) THEN custom_ram_size.value ELSE service_offering.ram_size end) AS ram_size " +
                "FROM vm_instance vi LEFT JOIN service_offering ON(((vi.service_offering_id = service_offering.id))) " +
                "LEFT JOIN user_vm_details custom_cpu ON(((custom_cpu.vm_id = vi.id) AND (custom_cpu.name = 'CpuNumber'))) " +
                "LEFT JOIN user_vm_details custom_speed ON(((custom_speed.vm_id = vi.id) AND (custom_speed.name = 'CpuSpeed'))) " +
                "LEFT JOIN user_vm_details custom_ram_size ON(((custom_ram_size.vm_id = vi.id) AND (custom_ram_size.name = 'memory'))) ";

    private static final String LIST_ALLOCATED_CAPACITY_GROUP_BY_CAPACITY_AND_ZONE_PART2 =
            "WHERE ISNULL(vi.removed) AND vi.state NOT IN ('Destroyed', 'Error', 'Expunging')";

    public CapacityDaoImpl() {
        _hostIdTypeSearch = createSearchBuilder();
        _hostIdTypeSearch.and("hostId", _hostIdTypeSearch.entity().getHostOrPoolId(), SearchCriteria.Op.EQ);
        _hostIdTypeSearch.and("type", _hostIdTypeSearch.entity().getCapacityType(), SearchCriteria.Op.EQ);
        _hostIdTypeSearch.done();

        _hostOrPoolIdSearch = createSearchBuilder();
        _hostOrPoolIdSearch.and("hostId", _hostOrPoolIdSearch.entity().getHostOrPoolId(), SearchCriteria.Op.EQ);
        _hostOrPoolIdSearch.done();

        _allFieldsSearch = createSearchBuilder();
        _allFieldsSearch.and("id", _allFieldsSearch.entity().getId(), SearchCriteria.Op.EQ);
        _allFieldsSearch.and("hostId", _allFieldsSearch.entity().getHostOrPoolId(), SearchCriteria.Op.EQ);
        _allFieldsSearch.and("zoneId", _allFieldsSearch.entity().getDataCenterId(), SearchCriteria.Op.EQ);
        _allFieldsSearch.and("podId", _allFieldsSearch.entity().getPodId(), SearchCriteria.Op.EQ);
        _allFieldsSearch.and("clusterId", _allFieldsSearch.entity().getClusterId(), SearchCriteria.Op.EQ);
        _allFieldsSearch.and("capacityType", _allFieldsSearch.entity().getCapacityType(), SearchCriteria.Op.EQ);
        _allFieldsSearch.and("capacityState", _allFieldsSearch.entity().getCapacityState(), SearchCriteria.Op.EQ);

        _allFieldsSearch.done();
    }

    @Override
    public List<Long> listClustersCrossingThreshold(short capacityType, Long zoneId, String configName, long computeRequested) {

        TransactionLegacy txn = TransactionLegacy.currentTxn();
        PreparedStatement pstmt = null;
        List<Long> result = new ArrayList<Long>();
        StringBuilder sql = new StringBuilder(LIST_CLUSTERS_CROSSING_THRESHOLD);
        // during listing the clusters that cross the threshold
        // we need to check with disabled thresholds of each cluster if not defined at cluster consider the global value
        try {
            pstmt = txn.prepareAutoCloseStatement(sql.toString());
            pstmt.setLong(1, computeRequested);
            pstmt.setString(2, configName);
            pstmt.setString(3, configName);
            pstmt.setString(4, configName);
            pstmt.setString(5, configName);
            pstmt.setString(6, configName);
            pstmt.setLong(7, zoneId);
            pstmt.setShort(8, capacityType);
            if (capacityType == 0) {
                pstmt.setString(9, "memoryOvercommitRatio");
            } else if (capacityType == 1) {
                pstmt.setString(9, "cpuOvercommitRatio");
            }

            ResultSet rs = pstmt.executeQuery();
            while (rs.next()) {
                result.add(rs.getLong(1));
            }
            return result;
        } catch (SQLException e) {
            throw new CloudRuntimeException("DB Exception on: " + sql, e);
        } catch (Throwable e) {
            throw new CloudRuntimeException("Caught: " + sql, e);
        }
    }

    /*public static String preparePlaceHolders(int length) {
        StringBuilder builder = new StringBuilder();
        for (int i = 0; i < length;) {
            builder.append("?");
            if (++i < length) {
                builder.append(",");
            }
        }
        return builder.toString();
    }

    public static void setValues(PreparedStatement preparedStatement, Object... values) throws SQLException {
        for (int i = 0; i < values.length; i++) {
            preparedStatement.setObject(i + 1, values[i]);
        }
    }*/

    @Override
    public List<SummedCapacity> findCapacityBy(Integer capacityType, Long zoneId, Long podId, Long clusterId, String resourceState) {

        TransactionLegacy txn = TransactionLegacy.currentTxn();
        PreparedStatement pstmt = null;
        List<SummedCapacity> result = new ArrayList<SummedCapacity>();

        StringBuilder sql = new StringBuilder(LIST_CAPACITY_BY_RESOURCE_STATE);
        List<Long> resourceIdList = new ArrayList<Long>();

        if (zoneId != null) {
            sql.append(" AND capacity.data_center_id = ?");
            resourceIdList.add(zoneId);
        }
        if (podId != null) {
            sql.append(" AND capacity.pod_id = ?");
            resourceIdList.add(podId);
        }
        if (clusterId != null) {
            sql.append(" AND capacity.cluster_id = ?");
            resourceIdList.add(clusterId);
        }
        if (capacityType != null) {
            sql.append(" AND capacity.capacity_type = ?");
            resourceIdList.add(capacityType.longValue());
        }

        try {
            pstmt = txn.prepareAutoCloseStatement(sql.toString());
            pstmt.setString(1, resourceState);
            pstmt.setString(2, resourceState);
            pstmt.setString(3, resourceState);
            pstmt.setString(4, resourceState);
            for (int i = 0; i < resourceIdList.size(); i++) {
                pstmt.setLong(5 + i, resourceIdList.get(i));
            }
            ResultSet rs = pstmt.executeQuery();
            while (rs.next()) {
                SummedCapacity summedCapacity = new SummedCapacity(rs.getLong(2), rs.getLong(3), rs.getLong(4), (short)rs.getLong(5), null, null, rs.getLong(1));
                result.add(summedCapacity);
            }
            return result;
        } catch (SQLException e) {
            throw new CloudRuntimeException("DB Exception on: " + sql, e);
        } catch (Throwable e) {
            throw new CloudRuntimeException("Caught: " + sql, e);
        }
    }

    @Override
    public List<SummedCapacity> listCapacitiesGroupedByLevelAndType(Integer capacityType, Long zoneId, Long podId, Long clusterId, int level, Long limit) {

        StringBuilder finalQuery = new StringBuilder();
        TransactionLegacy txn = TransactionLegacy.currentTxn();
        PreparedStatement pstmt = null;
        List<SummedCapacity> results = new ArrayList<SummedCapacity>();

        List<Long> resourceIdList = new ArrayList<Long>();

        switch (level) {
        case 1: // List all the capacities grouped by zone, capacity Type
            finalQuery.append(LIST_CAPACITY_GROUP_BY_ZONE_TYPE_PART1);
            break;

        case 2: // List all the capacities grouped by pod, capacity Type
            finalQuery.append(LIST_CAPACITY_GROUP_BY_POD_TYPE_PART1);
            break;

        case 3: // List all the capacities grouped by cluster, capacity Type
            finalQuery.append(LIST_CAPACITY_GROUP_BY_CLUSTER_TYPE_PART1);
            break;
        }

        if (zoneId != null) {
            finalQuery.append(" AND data_center_id = ?");
            resourceIdList.add(zoneId);
        }
        if (podId != null) {
            finalQuery.append(" AND pod_id = ?");
            resourceIdList.add(podId);
        }
        if (clusterId != null) {
            finalQuery.append(" AND cluster_id = ?");
            resourceIdList.add(clusterId);
        }
        if (capacityType != null) {
            finalQuery.append(" AND capacity_type = ?");
            resourceIdList.add(capacityType.longValue());
        }

        switch (level) {
        case 1: // List all the capacities grouped by zone, capacity Type
            finalQuery.append(LIST_CAPACITY_GROUP_BY_ZONE_TYPE_PART2);
            break;

        case 2: // List all the capacities grouped by pod, capacity Type
            finalQuery.append(LIST_CAPACITY_GROUP_BY_POD_TYPE_PART2);
            break;

        case 3: // List all the capacities grouped by cluster, capacity Type
            finalQuery.append(LIST_CAPACITY_GROUP_BY_CLUSTER_TYPE_PART2);
            break;
        }

        finalQuery.append("?");
        resourceIdList.add((long)limit);

        try {
            pstmt = txn.prepareAutoCloseStatement(finalQuery.toString());
            for (int i = 0; i < resourceIdList.size(); i++) {
                pstmt.setLong(1 + i, resourceIdList.get(i));
            }
            ResultSet rs = pstmt.executeQuery();
            while (rs.next()) {
                Long capacityPodId = null;
                Long capacityClusterId = null;

                if (level != 1 && rs.getLong(6) != 0)
                    capacityPodId = rs.getLong(6);
                if (level == 3 && rs.getLong(7) != 0)
                    capacityClusterId = rs.getLong(7);

                SummedCapacity summedCapacity =
                        new SummedCapacity(rs.getLong(1), rs.getLong(2), rs.getLong(3), rs.getFloat(4), (short)rs.getLong(5), rs.getLong(6), capacityPodId, capacityClusterId);

                results.add(summedCapacity);
            }

            return results;
        } catch (SQLException e) {
            throw new CloudRuntimeException("DB Exception on: " + finalQuery, e);
        } catch (Throwable e) {
            throw new CloudRuntimeException("Caught: " + finalQuery, e);
        }

    }

    @Override
    public Ternary<Long, Long, Long> findCapacityByZoneAndHostTag(Long zoneId, String hostTag) {
        TransactionLegacy txn = TransactionLegacy.currentTxn();
        PreparedStatement pstmt = null;
        List<SummedCapacity> results = new ArrayList<SummedCapacity>();

        StringBuilder allocatedSql = new StringBuilder(LIST_ALLOCATED_CAPACITY_GROUP_BY_CAPACITY_AND_ZONE_PART1);
        if (hostTag != null && ! hostTag.isEmpty()) {
            allocatedSql.append("LEFT JOIN vm_template ON vm_template.id = vi.vm_template_id ");
        }
        allocatedSql.append(LIST_ALLOCATED_CAPACITY_GROUP_BY_CAPACITY_AND_ZONE_PART2);
        if (zoneId != null){
            allocatedSql.append(" AND vi.data_center_id = ?");
        }
        if (hostTag != null && ! hostTag.isEmpty()) {
            allocatedSql.append(" AND (vm_template.template_tag = '").append(hostTag).append("'");
            allocatedSql.append(" OR service_offering.host_tag = '").append(hostTag).append("')");
        }
        allocatedSql.append(" ) AS v GROUP BY v.data_center_id");
        try {
            // add allocated capacity of zone in result
            pstmt = txn.prepareAutoCloseStatement(allocatedSql.toString());
            if (zoneId != null){
                pstmt.setLong(1, zoneId);
            }
            ResultSet rs = pstmt.executeQuery();
            if (rs.next()) {
                return new Ternary(rs.getLong(2), rs.getLong(3), rs.getLong(4)); // cpu cores, cpu, memory
            }
            return new Ternary(0L, 0L, 0L);
        } catch (SQLException e) {
            throw new CloudRuntimeException("DB Exception on: " + allocatedSql, e);
        }
    }

    @Override
    public List<SummedCapacity> findCapacityBy(Integer capacityType, Long zoneId, Long podId, Long clusterId) {

        TransactionLegacy txn = TransactionLegacy.currentTxn();
        PreparedStatement pstmt = null;
        List<SummedCapacity> results = new ArrayList<SummedCapacity>();

        StringBuilder allocatedSql = new StringBuilder(LIST_ALLOCATED_CAPACITY_GROUP_BY_CAPACITY_AND_ZONE_PART1);
        allocatedSql.append(LIST_ALLOCATED_CAPACITY_GROUP_BY_CAPACITY_AND_ZONE_PART2);

        HashMap<Long, Long> sumCpuCore  = new HashMap<Long, Long>();
        HashMap<Long, Long> sumCpu = new HashMap<Long, Long>();
        HashMap<Long, Long> sumMemory = new HashMap<Long, Long>();
        if (zoneId != null){
            allocatedSql.append(" AND vi.data_center_id = ?");
        }
        allocatedSql.append(" ) AS v GROUP BY v.data_center_id");
        try {
            if (podId == null && clusterId == null) {
                // add allocated capacity of zone in result
                pstmt = txn.prepareAutoCloseStatement(allocatedSql.toString());
                if (zoneId != null){
                    pstmt.setLong(1, zoneId);
                }
                ResultSet rs = pstmt.executeQuery();
                if (rs.next()) {
                    sumCpuCore.put(rs.getLong(1), rs.getLong(2));
                    sumCpu.put(rs.getLong(1), rs.getLong(3));
                    sumMemory.put(rs.getLong(1), rs.getLong(4));
                }
            }
        } catch (SQLException e) {
            throw new CloudRuntimeException("DB Exception on: " + allocatedSql, e);
        }

        StringBuilder sql = new StringBuilder(LIST_CAPACITY_GROUP_BY_CAPACITY_PART1);
        List<Long> resourceIdList = new ArrayList<Long>();

        if (zoneId != null) {
            sql.append(" AND capacity.data_center_id = ?");
            resourceIdList.add(zoneId);
        }
        if (podId != null) {
            sql.append(" AND capacity.pod_id = ?");
            resourceIdList.add(podId);
        }
        if (clusterId != null) {
            sql.append(" AND capacity.cluster_id = ?");
            resourceIdList.add(clusterId);
        }
        if (capacityType != null) {
            sql.append(" AND capacity.capacity_type = ?");
            resourceIdList.add(capacityType.longValue());
        }

        if (podId == null && clusterId == null) {
            sql.append(" GROUP BY capacity_type, data_center_id");
        } else {
            sql.append(LIST_CAPACITY_GROUP_BY_CAPACITY_DATA_CENTER_POD_CLUSTER);
        }

        try {
            pstmt = txn.prepareAutoCloseStatement(sql.toString());

            for (int i = 0; i < resourceIdList.size(); i++) {
                pstmt.setLong(i + 1, resourceIdList.get(i));
            }
            ResultSet rs = pstmt.executeQuery();
            while (rs.next()) {

                Long capacityZoneId = rs.getLong(6);
                Long capacityPodId = null;
                Long capacityClusterId = null;

                if(rs.getLong(7) != 0)
                    capacityPodId = rs.getLong(7);
                if(rs.getLong(8) != 0)
                    capacityClusterId = rs.getLong(8);

                SummedCapacity summedCapacity = new SummedCapacity( rs.getLong(1), rs.getLong(2), rs.getLong(3), rs.getFloat(4),
                        (short)rs.getLong(5), rs.getLong(6),
                        capacityPodId, capacityClusterId);

                if (podId == null && clusterId == null) {
                    Short sumCapacityType = summedCapacity.getCapacityType();
                    if (sumCapacityType == CapacityVO.CAPACITY_TYPE_MEMORY) {
                        summedCapacity.setAllocatedCapacity(sumMemory.get(capacityZoneId));
                    } else if (sumCapacityType == CapacityVO.CAPACITY_TYPE_CPU) {
                        summedCapacity.setAllocatedCapacity(sumCpu.get(capacityZoneId));
                    } else if (sumCapacityType == CapacityVO.CAPACITY_TYPE_CPU_CORE) {
                        summedCapacity.setAllocatedCapacity(sumCpuCore.get(capacityZoneId));
                    }
                }
                results.add(summedCapacity);
            }
            HashMap<String, SummedCapacity> capacityMap = new HashMap<String, SummedCapacity>();
            for (SummedCapacity result: results) {
                String key;
                if (zoneId != null || podId!= null) {
                    key=String.valueOf(result.getCapacityType());
                }
                else {
                    // sum the values based on the zoneId.
                    key=String.valueOf(result.getDataCenterId()) + "-" + String.valueOf(result.getCapacityType());
                }
                SummedCapacity tempCapacity=null;
                if (capacityMap.containsKey(key)) {
                    tempCapacity = capacityMap.get(key);
                    tempCapacity.setUsedCapacity(tempCapacity.getUsedCapacity()+result.getUsedCapacity());
                    tempCapacity.setReservedCapacity(tempCapacity.getReservedCapacity()+result.getReservedCapacity());
                    tempCapacity.setSumTotal(tempCapacity.getTotalCapacity()+result.getTotalCapacity());
                }else {
                    capacityMap.put(key, result);
                }
                tempCapacity = capacityMap.get(key);
                tempCapacity.setPodId(podId);
                tempCapacity.setClusterId(clusterId);
            }
            List<SummedCapacity> summedCapacityList = new ArrayList<SummedCapacity>();
            for (String key : capacityMap.keySet()) {
                summedCapacityList.add(capacityMap.get(key));
            }
            return summedCapacityList;
        } catch (SQLException e) {
            throw new CloudRuntimeException("DB Exception on: " + sql, e);
        } catch (Throwable e) {
            throw new CloudRuntimeException("Caught: " + sql, e);
        }
    }

    public void updateAllocated(Long hostId, long allocatedAmount, short capacityType, boolean add) {
        TransactionLegacy txn = TransactionLegacy.currentTxn();
        PreparedStatement pstmt = null;
        try {
            txn.start();
            String sql = null;
            if (add) {
                sql = ADD_ALLOCATED_SQL;
            } else {
                sql = SUBTRACT_ALLOCATED_SQL;
            }
            pstmt = txn.prepareAutoCloseStatement(sql);
            pstmt.setLong(1, allocatedAmount);
            pstmt.setLong(2, hostId);
            pstmt.setShort(3, capacityType);
            pstmt.executeUpdate(); // TODO:  Make sure exactly 1 row was updated?
            txn.commit();
        } catch (Exception e) {
            txn.rollback();
            s_logger.warn("Exception updating capacity for host: " + hostId, e);
        }
    }

    @Override
    public CapacityVO findByHostIdType(Long hostId, short capacityType) {
        SearchCriteria<CapacityVO> sc = _hostIdTypeSearch.create();
        sc.setParameters("hostId", hostId);
        sc.setParameters("type", capacityType);
        return findOneBy(sc);
    }

    @Override
    public List<Long> listClustersInZoneOrPodByHostCapacities(long id, int requiredCpu, long requiredRam, short capacityTypeForOrdering, boolean isZone) {
        TransactionLegacy txn = TransactionLegacy.currentTxn();
        PreparedStatement pstmt = null;
        List<Long> result = new ArrayList<Long>();

        StringBuilder sql = new StringBuilder(LIST_CLUSTERSINZONE_BY_HOST_CAPACITIES_PART1);

        if (isZone) {
            sql.append("capacity.data_center_id = ?");
        } else {
            sql.append("capacity.pod_id = ?");
        }
        sql.append(LIST_CLUSTERSINZONE_BY_HOST_CAPACITIES_PART2);
        if (isZone) {
            sql.append("capacity.data_center_id = ?");
        } else {
            sql.append("capacity.pod_id = ?");
        }
        sql.append(LIST_CLUSTERSINZONE_BY_HOST_CAPACITIES_PART3);

        try {
            pstmt = txn.prepareAutoCloseStatement(sql.toString());
            pstmt.setLong(1, id);
            pstmt.setShort(2, Capacity.CAPACITY_TYPE_CPU);
            pstmt.setString(3, "cpuOvercommitRatio");
            pstmt.setLong(4, requiredCpu);
            pstmt.setLong(5, id);
            pstmt.setShort(6, Capacity.CAPACITY_TYPE_MEMORY);
            pstmt.setString(7, "memoryOvercommitRatio");
            pstmt.setLong(8, requiredRam);

            ResultSet rs = pstmt.executeQuery();
            while (rs.next()) {
                result.add(rs.getLong(1));
            }
            return result;
        } catch (SQLException e) {
            throw new CloudRuntimeException("DB Exception on: " + sql, e);
        } catch (Throwable e) {
            throw new CloudRuntimeException("Caught: " + sql, e);
        }
    }

    @Override
    public List<Long> listHostsWithEnoughCapacity(int requiredCpu, long requiredRam, Long clusterId, String hostType) {
        TransactionLegacy txn = TransactionLegacy.currentTxn();
        PreparedStatement pstmt = null;
        List<Long> result = new ArrayList<Long>();

        StringBuilder sql = new StringBuilder(LIST_HOSTS_IN_CLUSTER_WITH_ENOUGH_CAPACITY);
        try {
            pstmt = txn.prepareAutoCloseStatement(sql.toString());
            pstmt.setLong(1, clusterId);
            pstmt.setString(2, hostType);
            pstmt.setLong(3, requiredCpu);
            pstmt.setLong(4, requiredRam);

            ResultSet rs = pstmt.executeQuery();
            while (rs.next()) {
                result.add(rs.getLong(1));
            }
            return result;
        } catch (SQLException e) {
            throw new CloudRuntimeException("DB Exception on: " + sql, e);
        } catch (Throwable e) {
            throw new CloudRuntimeException("Caught: " + sql, e);
        }
    }

    public static class SummedCapacity {
        public Long sumAllocated;
        public long sumUsed;
        public long sumReserved;
        public long sumTotal;
        public Float percentUsed;
        public short capacityType;
        public Long clusterId;
        public Long podId;
        public Long dcId;

        public SummedCapacity() {
        }

        public SummedCapacity(long sumUsed, long sumReserved, long sumTotal, short capacityType, Long clusterId, Long podId) {
            super();
            this.sumUsed = sumUsed;
            this.sumReserved = sumReserved;
            this.sumTotal = sumTotal;
            this.capacityType = capacityType;
            this.clusterId = clusterId;
            this.podId = podId;
        }

        public SummedCapacity(long sumUsed, long sumReserved, long sumTotal, short capacityType, Long clusterId, Long podId, Long zoneId) {
            this(sumUsed, sumReserved, sumTotal, capacityType, clusterId, podId);
            dcId = zoneId;
        }

        public SummedCapacity(long sumUsed, long sumTotal, float percentUsed, short capacityType, Long zoneId, Long podId, Long clusterId) {
            super();
            this.sumUsed = sumUsed;
            this.sumTotal = sumTotal;
            this.percentUsed = percentUsed;
            this.capacityType = capacityType;
            this.clusterId = clusterId;
            this.podId = podId;
            dcId = zoneId;
        }

        public SummedCapacity(long sumUsed, long sumReserved, long sumTotal, float percentUsed, short capacityType, Long zoneId, Long podId, Long clusterId) {
            this(sumUsed, sumTotal, percentUsed, capacityType, zoneId, podId, clusterId);
            this.sumReserved = sumReserved;
        }

        public Short getCapacityType() {
            return capacityType;
        }

        public Long getUsedCapacity() {
            return sumUsed;
        }

        public long getReservedCapacity() {
            return sumReserved;
        }

        public Long getTotalCapacity() {
            return sumTotal;
        }

        public Long getDataCenterId() {
            return dcId;
        }

        public Long getClusterId() {
            return clusterId;
        }

        public Long getPodId() {
            return podId;
        }

        public Float getPercentUsed() {
            return percentUsed;
        }
        public void setUsedCapacity(long sumUsed) {
            this.sumUsed= sumUsed;
        }
        public void setReservedCapacity(long sumReserved) {
            this.sumReserved=sumReserved;
        }
        public void setSumTotal(long sumTotal) {
            this.sumTotal=sumTotal;
        }

        public void setPodId(Long podId) {
            this.podId=podId;
        }
        public void setClusterId(Long clusterId) {
            this.clusterId=clusterId;
        }
        public Long getAllocatedCapacity() {
            return sumAllocated;
        }
        public void setAllocatedCapacity(Long sumAllocated) {
            this.sumAllocated = sumAllocated;
        }
    }

    @Override
    public List<SummedCapacity> findByClusterPodZone(Long zoneId, Long podId, Long clusterId) {

        TransactionLegacy txn = TransactionLegacy.currentTxn();
        PreparedStatement pstmt = null;
        List<SummedCapacity> result = new ArrayList<SummedCapacity>();

        StringBuilder sql = new StringBuilder(LIST_CAPACITY_GROUP_BY_CAPACITY_PART1);
        List<Long> resourceIdList = new ArrayList<Long>();

        if (zoneId != null) {
            sql.append(" AND capacity.data_center_id = ?");
            resourceIdList.add(zoneId);
        }
        if (podId != null) {
            sql.append(" AND capacity.pod_id = ?");
            resourceIdList.add(podId);
        }
        if (clusterId != null) {
            sql.append(" AND capacity.cluster_id = ?");
            resourceIdList.add(clusterId);
        }
        sql.append(LIST_CAPACITY_GROUP_BY_CAPACITY_PART2);

        try {
            pstmt = txn.prepareAutoCloseStatement(sql.toString());

            for (int i = 0; i < resourceIdList.size(); i++) {
                pstmt.setLong(i + 1, resourceIdList.get(i));
            }
            ResultSet rs = pstmt.executeQuery();
            while (rs.next()) {
                SummedCapacity summedCapacity = new SummedCapacity(rs.getLong(1), rs.getLong(2), rs.getLong(3), (short)rs.getLong(5), null, null, rs.getLong(6));
                result.add(summedCapacity);
            }
            return result;
        } catch (SQLException e) {
            throw new CloudRuntimeException("DB Exception on: " + sql, e);
        } catch (Throwable e) {
            throw new CloudRuntimeException("Caught: " + sql, e);
        }
    }

    @Override
    public List<SummedCapacity> findNonSharedStorageForClusterPodZone(Long zoneId, Long podId, Long clusterId) {

        GenericSearchBuilder<CapacityVO, SummedCapacity> SummedCapacitySearch = createSearchBuilder(SummedCapacity.class);
        SummedCapacitySearch.select("sumUsed", Func.SUM, SummedCapacitySearch.entity().getUsedCapacity());
        SummedCapacitySearch.select("sumTotal", Func.SUM, SummedCapacitySearch.entity().getTotalCapacity());
        SummedCapacitySearch.select("capacityType", Func.NATIVE, SummedCapacitySearch.entity().getCapacityType());
        SummedCapacitySearch.and("capacityType", SummedCapacitySearch.entity().getCapacityType(), Op.EQ);

        SearchBuilder<StoragePoolVO> nonSharedStorage = _storagePoolDao.createSearchBuilder();
        nonSharedStorage.and("poolTypes", nonSharedStorage.entity().getPoolType(), SearchCriteria.Op.IN);
        SummedCapacitySearch.join("nonSharedStorage", nonSharedStorage, nonSharedStorage.entity().getId(), SummedCapacitySearch.entity().getHostOrPoolId(),
                JoinType.INNER);
        nonSharedStorage.done();

        if (zoneId != null) {
            SummedCapacitySearch.and("zoneId", SummedCapacitySearch.entity().getDataCenterId(), Op.EQ);
        }
        if (podId != null) {
            SummedCapacitySearch.and("podId", SummedCapacitySearch.entity().getPodId(), Op.EQ);
        }
        if (clusterId != null) {
            SummedCapacitySearch.and("clusterId", SummedCapacitySearch.entity().getClusterId(), Op.EQ);
        }
        SummedCapacitySearch.done();

        SearchCriteria<SummedCapacity> sc = SummedCapacitySearch.create();
        sc.setJoinParameters("nonSharedStorage", "poolTypes", Storage.getNonSharedStoragePoolTypes().toArray());
        sc.setParameters("capacityType", Capacity.CAPACITY_TYPE_STORAGE_ALLOCATED);
        if (zoneId != null) {
            sc.setParameters("zoneId", zoneId);
        }
        if (podId != null) {
            sc.setParameters("podId", podId);
        }
        if (clusterId != null) {
            sc.setParameters("clusterId", clusterId);
        }

        return customSearchIncludingRemoved(sc, null);
    }

    @Override
    public boolean removeBy(Short capacityType, Long zoneId, Long podId, Long clusterId, Long hostId) {
        SearchCriteria<CapacityVO> sc = _allFieldsSearch.create();

        if (capacityType != null) {
            sc.setParameters("capacityType", capacityType);
        }

        if (zoneId != null) {
            sc.setParameters("zoneId", zoneId);
        }

        if (podId != null) {
            sc.setParameters("podId", podId);
        }

        if (clusterId != null) {
            sc.setParameters("clusterId", clusterId);
        }

        if (hostId != null) {
            sc.setParameters("hostId", hostId);
        }

        return remove(sc) > 0;
    }

    @Override
    public Pair<List<Long>, Map<Long, Double>> orderClustersByAggregateCapacity(long id, short capacityTypeForOrdering, boolean isZone) {
        TransactionLegacy txn = TransactionLegacy.currentTxn();
        PreparedStatement pstmt = null;
        List<Long> result = new ArrayList<Long>();
        Map<Long, Double> clusterCapacityMap = new HashMap<Long, Double>();
        StringBuilder sql = new StringBuilder();
        if (capacityTypeForOrdering != Capacity.CAPACITY_TYPE_CPU && capacityTypeForOrdering != Capacity.CAPACITY_TYPE_MEMORY) {
            sql.append(ORDER_CLUSTERS_BY_AGGREGATE_CAPACITY_PART1);
        } else {
            sql.append(ORDER_CLUSTERS_BY_AGGREGATE_OVERCOMMIT_CAPACITY_PART1);
        }

        if (isZone) {
            sql.append(" data_center_id = ?");
        } else {
            sql.append(" pod_id = ?");
        }
        if (capacityTypeForOrdering != Capacity.CAPACITY_TYPE_CPU && capacityTypeForOrdering != Capacity.CAPACITY_TYPE_MEMORY) {
            sql.append(ORDER_CLUSTERS_BY_AGGREGATE_CAPACITY_PART2);
        } else {
            sql.append(ORDER_CLUSTERS_BY_AGGREGATE_OVERCOMMIT_CAPACITY_PART2);
        }

        try {
            pstmt = txn.prepareAutoCloseStatement(sql.toString());
            pstmt.setLong(1, id);
            pstmt.setShort(2, capacityTypeForOrdering);

            if (capacityTypeForOrdering == Capacity.CAPACITY_TYPE_CPU) {
                pstmt.setString(3, "cpuOvercommitRatio");
            } else if (capacityTypeForOrdering == Capacity.CAPACITY_TYPE_MEMORY) {
                pstmt.setString(3, "memoryOvercommitRatio");
            }

            ResultSet rs = pstmt.executeQuery();
            while (rs.next()) {
                Long clusterId = rs.getLong(1);
                result.add(clusterId);
                clusterCapacityMap.put(clusterId, rs.getDouble(2));
            }
            return new Pair<List<Long>, Map<Long, Double>>(result, clusterCapacityMap);
        } catch (SQLException e) {
            throw new CloudRuntimeException("DB Exception on: " + sql, e);
        } catch (Throwable e) {
            throw new CloudRuntimeException("Caught: " + sql, e);
        }
    }

    @Override
    public List<Long> orderHostsByFreeCapacity(Long zoneId, Long clusterId, short capacityTypeForOrdering){
         TransactionLegacy txn = TransactionLegacy.currentTxn();
         PreparedStatement pstmt = null;
         List<Long> result = new ArrayList<Long>();
         StringBuilder sql = new StringBuilder(ORDER_HOSTS_BY_FREE_CAPACITY_PART1);
         if (zoneId != null) {
             sql.append(" AND data_center_id = ?");
         }
         if (clusterId != null) {
             sql.append(" AND cluster_id = ?");
         }
         sql.append(ORDER_HOSTS_BY_FREE_CAPACITY_PART2);
         try {
             pstmt = txn.prepareAutoCloseStatement(sql.toString());
             pstmt.setShort(1, capacityTypeForOrdering);
             int index = 2;
             if (zoneId != null) {
                 pstmt.setLong(index, zoneId);
                 index ++;
             }
             if (clusterId != null) {
                 pstmt.setLong(index, clusterId);
             }

             ResultSet rs = pstmt.executeQuery();
             while (rs.next()) {
                 result.add(rs.getLong(1));
             }
             return result;
         } catch (SQLException e) {
             throw new CloudRuntimeException("DB Exception on: " + sql, e);
         } catch (Throwable e) {
             throw new CloudRuntimeException("Caught: " + sql, e);
         }
    }

    @Override
    public List<Long> listPodsByHostCapacities(long zoneId, int requiredCpu, long requiredRam, short capacityType) {
        TransactionLegacy txn = TransactionLegacy.currentTxn();
        PreparedStatement pstmt = null;
        List<Long> result = new ArrayList<Long>();

        StringBuilder sql = new StringBuilder(LIST_PODSINZONE_BY_HOST_CAPACITY_TYPE);
        sql.append("AND capacity.pod_id IN (");
        sql.append(LIST_PODSINZONE_BY_HOST_CAPACITY_TYPE);
        sql.append(")");

        try {
            pstmt = txn.prepareAutoCloseStatement(sql.toString());
            pstmt.setLong(1, zoneId);
            pstmt.setShort(2, Capacity.CAPACITY_TYPE_CPU);
            pstmt.setString(3, "cpuOvercommitRatio");
            pstmt.setLong(4, requiredCpu);
            pstmt.setLong(5, zoneId);
            pstmt.setShort(6, Capacity.CAPACITY_TYPE_MEMORY);
            pstmt.setString(7, "memoryOvercommitRatio");
            pstmt.setLong(8, requiredRam);

            ResultSet rs = pstmt.executeQuery();
            while (rs.next()) {
                result.add(rs.getLong(1));
            }
            return result;
        } catch (SQLException e) {
            throw new CloudRuntimeException("DB Exception on: " + sql, e);
        } catch (Throwable e) {
            throw new CloudRuntimeException("Caught: " + sql, e);
        }
    }

    @Override
    public Pair<List<Long>, Map<Long, Double>> orderPodsByAggregateCapacity(long zoneId, short capacityTypeForOrdering) {
        TransactionLegacy txn = TransactionLegacy.currentTxn();
        PreparedStatement pstmt = null;
        List<Long> result = new ArrayList<Long>();
        Map<Long, Double> podCapacityMap = new HashMap<Long, Double>();
        StringBuilder sql = null;
        try {
            if (capacityTypeForOrdering == Capacity.CAPACITY_TYPE_CPU | capacityTypeForOrdering == Capacity.CAPACITY_TYPE_MEMORY) {
                sql = new StringBuilder(ORDER_PODS_BY_AGGREGATE_OVERCOMMIT_CAPACITY);
                pstmt = txn.prepareAutoCloseStatement(sql.toString());
                pstmt.setLong(1, zoneId);
                pstmt.setShort(2, capacityTypeForOrdering);

                if (capacityTypeForOrdering == Capacity.CAPACITY_TYPE_CPU) {
                    pstmt.setString(3, "cpuOvercommitRatio");
                } else if (capacityTypeForOrdering == Capacity.CAPACITY_TYPE_MEMORY) {
                    pstmt.setString(3, "memoryOvercommitRatio");
                }
            } else {
                sql = new StringBuilder(ORDER_PODS_BY_AGGREGATE_CAPACITY);
                pstmt = txn.prepareAutoCloseStatement(sql.toString());
                pstmt.setLong(1, zoneId);
                pstmt.setShort(2, capacityTypeForOrdering);
            }

            ResultSet rs = pstmt.executeQuery();
            while (rs.next()) {
                Long podId = rs.getLong(1);
                result.add(podId);
                podCapacityMap.put(podId, rs.getDouble(2));
            }
            return new Pair<List<Long>, Map<Long, Double>>(result, podCapacityMap);
        } catch (SQLException e) {
            throw new CloudRuntimeException("DB Exception on: " + sql, e);
        } catch (Throwable e) {
            throw new CloudRuntimeException("Caught: " + sql, e);
        }
    }

    @Override
    public void updateCapacityState(Long dcId, Long podId, Long clusterId, Long hostId, String capacityState, short[] capacityType) {
        TransactionLegacy txn = TransactionLegacy.currentTxn();
        StringBuilder sql = new StringBuilder(UPDATE_CAPACITY_STATE);
        List<Long> resourceIdList = new ArrayList<Long>();
        StringBuilder where = new StringBuilder();

        if (dcId != null) {
            where.append(" data_center_id = ? ");
            resourceIdList.add(dcId);
        }
        if (podId != null) {
            where.append((where.length() > 0) ? " and pod_id = ? " : " pod_id = ? ");
            resourceIdList.add(podId);
        }
        if (clusterId != null) {
            where.append((where.length() > 0) ? " and cluster_id = ? " : " cluster_id = ? ");
            resourceIdList.add(clusterId);
        }
        if (hostId != null) {
            where.append((where.length() > 0) ? " and host_id = ? " : " host_id = ? ");
            resourceIdList.add(hostId);
        }

        if (capacityType != null && capacityType.length > 0) {
            where.append((where.length() > 0) ? " and capacity_type in " : " capacity_type in ");

            StringBuilder builder = new StringBuilder();
            for( int i = 0 ; i < capacityType.length; i++ ) {
                if(i==0){
                    builder.append(" (? ");
                }else{
                    builder.append(" ,? ");
                }
            }
            builder.append(" ) ");

            where.append(builder);
        }
        sql.append(where);

        PreparedStatement pstmt = null;
        try {
            pstmt = txn.prepareAutoCloseStatement(sql.toString());
            int i = 1;
            pstmt.setString(i, capacityState);
            i = i + 1;
            for (int j=0 ; j < resourceIdList.size(); j++, i++) {
                pstmt.setLong(i, resourceIdList.get(j));
            }
            for(int j=0; j < capacityType.length; i++, j++ ) {
                pstmt.setShort(i, capacityType[j]);
            }

            pstmt.executeUpdate();
        } catch (Exception e) {
            s_logger.warn("Error updating CapacityVO", e);
        }
    }

    @Override
    public float findClusterConsumption(Long clusterId, short capacityType, long computeRequested) {
        TransactionLegacy txn = TransactionLegacy.currentTxn();
        StringBuilder sql = new StringBuilder(FIND_CLUSTER_CONSUMPTION_RATIO);
        PreparedStatement pstmt = null;
        try {
            pstmt = txn.prepareAutoCloseStatement(sql.toString());

            pstmt.setLong(1, computeRequested);
            pstmt.setLong(2, clusterId);
            pstmt.setShort(3, capacityType);
            ResultSet rs = pstmt.executeQuery();
            while (rs.next()) {
                return rs.getFloat(1);
            }
        } catch (Exception e) {
            s_logger.warn("Error checking cluster threshold", e);
        }
        return 0;
    }

}
