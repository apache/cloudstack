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

import javax.ejb.Local;
import javax.inject.Inject;

import org.apache.cloudstack.storage.datastore.db.PrimaryDataStoreDao;
import org.apache.cloudstack.storage.datastore.db.StoragePoolVO;
import org.apache.log4j.Logger;
import org.springframework.stereotype.Component;

import com.cloud.capacity.Capacity;
import com.cloud.capacity.CapacityVO;
import com.cloud.storage.Storage;
import com.cloud.utils.Pair;
import com.cloud.utils.db.Filter;
import com.cloud.utils.db.GenericDaoBase;
import com.cloud.utils.db.GenericSearchBuilder;
import com.cloud.utils.db.JoinBuilder.JoinType;
import com.cloud.utils.db.SearchBuilder;
import com.cloud.utils.db.SearchCriteria;
import com.cloud.utils.db.SearchCriteria.Func;
import com.cloud.utils.db.SearchCriteria.Op;
import com.cloud.utils.db.Transaction;
import com.cloud.utils.exception.CloudRuntimeException;

@Component
@Local(value = { CapacityDao.class })
public class CapacityDaoImpl extends GenericDaoBase<CapacityVO, Long> implements CapacityDao {
    private static final Logger s_logger = Logger.getLogger(CapacityDaoImpl.class);

    private static final String ADD_ALLOCATED_SQL = "UPDATE `cloud`.`op_host_capacity` SET used_capacity = used_capacity + ? WHERE host_id = ? AND capacity_type = ?";
    private static final String SUBTRACT_ALLOCATED_SQL = "UPDATE `cloud`.`op_host_capacity` SET used_capacity = used_capacity - ? WHERE host_id = ? AND capacity_type = ?";

    private static final String LIST_CLUSTERSINZONE_BY_HOST_CAPACITIES_PART1 = "SELECT DISTINCT capacity.cluster_id  FROM `cloud`.`op_host_capacity` capacity INNER JOIN `cloud`.`cluster` cluster on (cluster.id = capacity.cluster_id AND cluster.removed is NULL)   INNER JOIN `cloud`.`cluster_details` cluster_details ON (cluster.id = cluster_details.cluster_id ) WHERE ";
    private static final String LIST_CLUSTERSINZONE_BY_HOST_CAPACITIES_PART2 = " AND capacity_type = ? AND cluster_details.name= ? AND ((total_capacity * cluster_details.value ) - used_capacity + reserved_capacity) >= ? AND capacity.cluster_id IN (SELECT distinct capacity.cluster_id  FROM `cloud`.`op_host_capacity` capacity INNER JOIN  `cloud`.`cluster_details` cluster_details ON (capacity.cluster_id = cluster_details.cluster_id ) WHERE ";
    private static final String LIST_CLUSTERSINZONE_BY_HOST_CAPACITIES_PART3 = " AND capacity_type = ? AND cluster_details.name= ? AND ((total_capacity * cluster_details.value) - used_capacity + reserved_capacity) >= ?) ";

    private final SearchBuilder<CapacityVO> _hostIdTypeSearch;
    private final SearchBuilder<CapacityVO> _hostOrPoolIdSearch;
    private final SearchBuilder<CapacityVO> _allFieldsSearch;
    @Inject protected PrimaryDataStoreDao _storagePoolDao;


    private static final String LIST_HOSTS_IN_CLUSTER_WITH_ENOUGH_CAPACITY = " SELECT  host_capacity.host_id FROM (`cloud`.`host` JOIN `cloud`.`op_host_capacity` host_capacity ON (host.id = host_capacity.host_id AND host.cluster_id = ?) JOIN `cloud`.`cluster_details` cluster_details ON (host_capacity.cluster_id = cluster_details.cluster_id) AND  host.type = ? AND cluster_details.name='cpuOvercommitRatio' AND ((host_capacity.total_capacity *cluster_details.value ) - host_capacity.used_capacity) >= ? and host_capacity.capacity_type = '1' " +
               " AND  host_capacity.host_id IN (SELECT capacity.host_id FROM `cloud`.`op_host_capacity` capacity JOIN `cloud`.`cluster_details` cluster_details ON (capacity.cluster_id= cluster_details.cluster_id) where capacity_type='0' AND cluster_details.name='memoryOvercommitRatio' AND ((total_capacity* cluster_details.value) - used_capacity ) >= ?)) ";

    private static final String ORDER_CLUSTERS_BY_AGGREGATE_CAPACITY_PART1= "SELECT capacity.cluster_id, SUM(used_capacity+reserved_capacity)/SUM(total_capacity ) FROM `cloud`.`op_host_capacity` capacity WHERE ";

    private static final String ORDER_CLUSTERS_BY_AGGREGATE_CAPACITY_PART2= " AND capacity_type = ?  AND cluster_details.name =? GROUP BY capacity.cluster_id ORDER BY SUM(used_capacity+reserved_capacity)/SUM(total_capacity * cluster_details.value) ASC";

    private static final String ORDER_CLUSTERS_BY_AGGREGATE_OVERCOMMIT_CAPACITY_PART1= "SELECT capacity.cluster_id, SUM(used_capacity+reserved_capacity)/SUM(total_capacity * cluster_details.value) FROM `cloud`.`op_host_capacity` capacity INNER JOIN `cloud`.`cluster_details` cluster_details ON (capacity.cluster_id = cluster_details.cluster_id) WHERE ";

    private static final String ORDER_CLUSTERS_BY_AGGREGATE_OVERCOMMIT_CAPACITY_PART2= " AND capacity_type = ?  AND cluster_details.name =? GROUP BY capacity.cluster_id ORDER BY SUM(used_capacity+reserved_capacity)/SUM(total_capacity * cluster_details.value) ASC";

    private static final String LIST_PODSINZONE_BY_HOST_CAPACITY_TYPE = "SELECT DISTINCT capacity.pod_id  FROM `cloud`.`op_host_capacity` capacity INNER JOIN `cloud`.`host_pod_ref` pod " +
                                                          " ON (pod.id = capacity.pod_id AND pod.removed is NULL) INNER JOIN `cloud`.`cluster_details` cluster ON (capacity.cluster_id = cluster.cluster_id ) WHERE capacity.data_center_id = ? AND capacity_type = ? AND cluster_details.name= ? ((total_capacity * cluster.value ) - used_capacity + reserved_capacity) >= ? ";

    private static final String ORDER_PODS_BY_AGGREGATE_CAPACITY = " SELECT capacity.pod_id, SUM(used_capacity+reserved_capacity)/SUM(total_capacity) FROM `cloud`.`op_host_capacity` capacity WHERE data_center_id= ? AND capacity_type = ? GROUP BY capacity.pod_id ORDER BY SUM(used_capacity+reserved_capacity)/SUM(total_capacity) ASC ";

    private static final String ORDER_PODS_BY_AGGREGATE_OVERCOMMIT_CAPACITY ="SELECT capacity.pod_id, SUM(used_capacity+reserved_capacity)/SUM(total_capacity * cluster_details.value) FROM `cloud`.`op_host_capacity` capacity INNER JOIN `cloud`.`cluster_details` cluster_details ON (capacity.cluster_id = cluster_details.cluster_id) WHERE data_center_id=? AND capacity_type = ?  AND cluster_details.name = ? GROUP BY capacity.pod_id ORDER BY SUM(used_capacity+reserved_capacity)/SUM(total_capacity * cluster_details.value) ASC";

    private static final String LIST_CAPACITY_BY_RESOURCE_STATE = "SELECT capacity.data_center_id, sum(capacity.used_capacity), sum(capacity.reserved_quantity), sum(capacity.total_capacity), capacity_capacity_type "+
                                                                  "FROM `cloud`.`op_host_capacity` capacity INNER JOIN `cloud`.`data_center` dc ON (dc.id = capacity.data_center_id AND dc.removed is NULL)"+
                                                                  "FROM `cloud`.`op_host_capacity` capacity INNER JOIN `cloud`.`host_pod_ref` pod ON (pod.id = capacity.pod_id AND pod.removed is NULL)"+
                                                                  "FROM `cloud`.`op_host_capacity` capacity INNER JOIN `cloud`.`cluster` cluster ON (cluster.id = capacity.cluster_id AND cluster.removed is NULL)"+
                                                                  "FROM `cloud`.`op_host_capacity` capacity INNER JOIN `cloud`.`host` host ON (host.id = capacity.host_id AND host.removed is NULL)"+
                                                                  "WHERE dc.allocation_state = ? AND pod.allocation_state = ? AND cluster.allocation_state = ? AND host.resource_state = ? AND capacity_type not in (3,4) ";
    
    private static final String LIST_CAPACITY_GROUP_BY_ZONE_TYPE_PART1 = "SELECT (sum(capacity.used_capacity) + sum(capacity.reserved_capacity)), (case capacity_type when 1 then (sum(total_capacity) * (select value from `cloud`.`configuration` where name like 'cpu.overprovisioning.factor')) else sum(total_capacity) end), " +
                                                                         "((sum(capacity.used_capacity) + sum(capacity.reserved_capacity)) / (case capacity_type when 1 then (sum(total_capacity) * (select value from `cloud`.`configuration` where name like 'cpu.overprovisioning.factor')) else sum(total_capacity) end)) percent,"+
                                                                         " capacity.capacity_type, capacity.data_center_id "+
                                                                         "FROM `cloud`.`op_host_capacity` capacity "+
                                                                         "WHERE  total_capacity > 0 AND data_center_id is not null AND capacity_state='Enabled'";
    private static final String LIST_CAPACITY_GROUP_BY_ZONE_TYPE_PART2 = " GROUP BY data_center_id, capacity_type order by percent desc limit ";
    private static final String LIST_CAPACITY_GROUP_BY_POD_TYPE_PART1 =  "SELECT (sum(capacity.used_capacity) + sum(capacity.reserved_capacity))," +
            " (case capacity_type when 1 then (sum(total_capacity) * (select value from `cloud`.`cluster_details` where cluster_details.name= 'cpuOvercommitRatio' AND cluster_details.cluster_id=capacity.cluster_id)) " +
            "when '0' then (sum(total_capacity) * (select value from `cloud`.`cluster_details` where cluster_details.name= 'memoryOvercommitRatio' AND cluster_details.cluster_id=capacity.cluster_id))else sum(total_capacity) end)," +
            "((sum(capacity.used_capacity) + sum(capacity.reserved_capacity)) / ( case capacity_type when 1 then (sum(total_capacity) * (select value from `cloud`.`cluster_details` where cluster_details.name= 'cpuOvercommitRatio' AND cluster_details.cluster_id=capacity.cluster_id)) " +
            "when '0' then (sum(total_capacity) * (select value from `cloud`.`cluster_details` where cluster_details.name= 'memoryOvercommitRatio' AND cluster_details.cluster_id=capacity.cluster_id))else sum(total_capacity) end)) percent," +
            "capacity.capacity_type, capacity.data_center_id, pod_id FROM `cloud`.`op_host_capacity` capacity WHERE  total_capacity > 0 AND data_center_id is not null AND capacity_state='Enabled' ";

    private static final String LIST_CAPACITY_GROUP_BY_POD_TYPE_PART2 = " GROUP BY pod_id, capacity_type order by percent desc limit ";

    private static final String LIST_CAPACITY_GROUP_BY_CLUSTER_TYPE_PART1 = "SELECT (sum(capacity.used_capacity) + sum(capacity.reserved_capacity))," +
            " (case capacity_type when 1 then (sum(total_capacity) * (select value from `cloud`.`cluster_details` where cluster_details.name= 'cpuOvercommitRatio' AND cluster_details.cluster_id=capacity.cluster_id)) " +
            "when '0' then (sum(total_capacity) * (select value from `cloud`.`cluster_details` where cluster_details.name= 'memoryOvercommitRatio' AND cluster_details.cluster_id=capacity.cluster_id))else sum(total_capacity) end)," +
            "((sum(capacity.used_capacity) + sum(capacity.reserved_capacity)) / ( case capacity_type when 1 then (sum(total_capacity) * (select value from `cloud`.`cluster_details` where cluster_details.name= 'cpuOvercommitRatio' AND cluster_details.cluster_id=capacity.cluster_id)) " +
            "when '0' then (sum(total_capacity) * (select value from `cloud`.`cluster_details` where cluster_details.name= 'memoryOvercommitRatio' AND cluster_details.cluster_id=capacity.cluster_id))else sum(total_capacity) end)) percent," +
            "capacity.capacity_type, capacity.data_center_id, pod_id, cluster_id FROM `cloud`.`op_host_capacity` capacity WHERE  total_capacity > 0 AND data_center_id is not null AND capacity_state='Enabled' ";


    private static final String LIST_CAPACITY_GROUP_BY_CLUSTER_TYPE_PART2 = " GROUP BY cluster_id, capacity_type order by percent desc limit ";
    private static final String UPDATE_CAPACITY_STATE = "UPDATE `cloud`.`op_host_capacity` SET capacity_state = ? WHERE ";
    private static final String LIST_CLUSTERS_CROSSING_THRESHOLD = "SELECT cluster_id " +
            "FROM (SELECT cluster_id, ( (sum(capacity.used_capacity) + sum(capacity.reserved_capacity) + ?)/sum(total_capacity) ) ratio "+
            "FROM `cloud`.`op_host_capacity` capacity "+
            "WHERE capacity.data_center_id = ? AND capacity.capacity_type = ? AND capacity.total_capacity > 0 "+    		    		
            "GROUP BY cluster_id) tmp " +
            "WHERE tmp.ratio > ? ";


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
    public  List<Long> listClustersCrossingThreshold(short capacityType, Long zoneId, Float disableThreshold, long compute_requested){

         Transaction txn = Transaction.currentTxn();
         PreparedStatement pstmt = null;
         List<Long> result = new ArrayList<Long>();         
         StringBuilder sql = new StringBuilder(LIST_CLUSTERS_CROSSING_THRESHOLD);
         
 
         try {
             pstmt = txn.prepareAutoCloseStatement(sql.toString());
             pstmt.setLong(1,compute_requested);
             pstmt.setShort(2,capacityType);
             pstmt.setFloat(3,disableThreshold);
             pstmt.setLong(4,zoneId);

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
    public  List<SummedCapacity> findCapacityBy(Integer capacityType, Long zoneId, Long podId, Long clusterId, String resource_state){

        Transaction txn = Transaction.currentTxn();
        PreparedStatement pstmt = null;
        List<SummedCapacity> result = new ArrayList<SummedCapacity>();

        StringBuilder sql = new StringBuilder(LIST_CAPACITY_BY_RESOURCE_STATE);           
        List<Long> resourceIdList = new ArrayList<Long>();

        if (zoneId != null){
            sql.append(" AND capacity.data_center_id = ?");
            resourceIdList.add(zoneId);
        }
        if (podId != null){
            sql.append(" AND capacity.pod_id = ?");
            resourceIdList.add(podId);
        }
        if (clusterId != null){
            sql.append(" AND capacity.cluster_id = ?");
            resourceIdList.add(clusterId);
        }
        if (capacityType != null){
            sql.append(" AND capacity.capacity_type = ?");
            resourceIdList.add(capacityType.longValue());
        }   

        try {
            pstmt = txn.prepareAutoCloseStatement(sql.toString());
            pstmt.setString(1, resource_state);
            pstmt.setString(2, resource_state);
            pstmt.setString(3, resource_state);
            pstmt.setString(4, resource_state);
            for (int i = 0; i < resourceIdList.size(); i++){                
                pstmt.setLong( 5+i, resourceIdList.get(i));
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
    public  List<SummedCapacity> listCapacitiesGroupedByLevelAndType(Integer capacityType, Long zoneId, Long podId, Long clusterId, int level, Long limit){

        StringBuilder finalQuery = new StringBuilder(); 
        Transaction txn = Transaction.currentTxn();
        PreparedStatement pstmt = null;
        List<SummedCapacity> result = new ArrayList<SummedCapacity>();

        List<Long> resourceIdList = new ArrayList<Long>();

        switch(level){
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

        if (zoneId != null){
            finalQuery.append(" AND data_center_id = ?" );
            resourceIdList.add(zoneId);
        }
        if (podId != null){
            finalQuery.append(" AND pod_id = ?" );
            resourceIdList.add(podId);
        }
        if (clusterId != null){
            finalQuery.append(" AND cluster_id = ?" );
            resourceIdList.add(clusterId );
        }
        if (capacityType != null){
            finalQuery.append(" AND capacity_type = ?");
            resourceIdList.add(capacityType.longValue() );
        }

        switch(level){
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
        resourceIdList.add((long) limit);
        
        try {
            pstmt = txn.prepareAutoCloseStatement(finalQuery.toString());
            for (int i = 0; i < resourceIdList.size(); i++){
                pstmt.setLong(1+i, resourceIdList.get(i));
            }
            ResultSet rs = pstmt.executeQuery();
            while (rs.next()) {
                Long capacityPodId = null;
                Long capacityClusterId = null;

                if(level != 1 && rs.getLong(6) != 0)
                    capacityPodId = rs.getLong(6);
                if(level == 3 && rs.getLong(7) != 0)
                    capacityClusterId = rs.getLong(7);                   

                SummedCapacity summedCapacity = new SummedCapacity( rs.getLong(1), rs.getLong(2), rs.getFloat(3),
                        (short)rs.getLong(4), rs.getLong(5),
                        capacityPodId, capacityClusterId);

                result.add(summedCapacity);
            }
            return result;
        } catch (SQLException e) {
            throw new CloudRuntimeException("DB Exception on: " + finalQuery, e);
        } catch (Throwable e) {
            throw new CloudRuntimeException("Caught: " + finalQuery, e);
        }                     

    }

    @Override
    public  List<SummedCapacity> findCapacityBy(Integer capacityType, Long zoneId, Long podId, Long clusterId){

        GenericSearchBuilder<CapacityVO, SummedCapacity> SummedCapacitySearch = createSearchBuilder(SummedCapacity.class);
        SummedCapacitySearch.select("dcId", Func.NATIVE, SummedCapacitySearch.entity().getDataCenterId());
        SummedCapacitySearch.select("sumUsed", Func.SUM, SummedCapacitySearch.entity().getUsedCapacity());
        SummedCapacitySearch.select("sumReserved", Func.SUM, SummedCapacitySearch.entity().getReservedCapacity());
        SummedCapacitySearch.select("sumTotal", Func.SUM, SummedCapacitySearch.entity().getTotalCapacity());
        SummedCapacitySearch.select("capacityType", Func.NATIVE, SummedCapacitySearch.entity().getCapacityType());        

        if (zoneId==null && podId==null && clusterId==null){ // List all the capacities grouped by zone, capacity Type
            SummedCapacitySearch.groupBy(SummedCapacitySearch.entity().getDataCenterId(), SummedCapacitySearch.entity().getCapacityType());            
        }else {
            SummedCapacitySearch.groupBy(SummedCapacitySearch.entity().getCapacityType());
        }

        if (zoneId != null){
            SummedCapacitySearch.and("dcId", SummedCapacitySearch.entity().getDataCenterId(), Op.EQ);
        }
        if (podId != null){
            SummedCapacitySearch.and("podId", SummedCapacitySearch.entity().getPodId(), Op.EQ);
        }
        if (clusterId != null){
            SummedCapacitySearch.and("clusterId", SummedCapacitySearch.entity().getClusterId(), Op.EQ);
        }
        if (capacityType != null){
            SummedCapacitySearch.and("capacityType", SummedCapacitySearch.entity().getCapacityType(), Op.EQ);	
        }        

        SummedCapacitySearch.done();


        SearchCriteria<SummedCapacity> sc = SummedCapacitySearch.create();
        if (zoneId != null){
            sc.setParameters("dcId", zoneId);
        }
        if (podId != null){
            sc.setParameters("podId", podId);
        }
        if (clusterId != null){
            sc.setParameters("clusterId", clusterId);
        }
        if (capacityType != null){
            sc.setParameters("capacityType", capacityType);
        }

        Filter filter = new Filter(CapacityVO.class, null, true, null, null);
        List<SummedCapacity> results = customSearchIncludingRemoved(sc, filter);
        return results;        

    }

    public void updateAllocated(Long hostId, long allocatedAmount, short capacityType, boolean add) {
        Transaction txn = Transaction.currentTxn();
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
    public List<Long> listClustersInZoneOrPodByHostCapacities(long id, int requiredCpu, long requiredRam, short capacityTypeForOrdering, boolean isZone){
    Transaction txn = Transaction.currentTxn();
        PreparedStatement pstmt = null;
        List<Long> result = new ArrayList<Long>();

        StringBuilder sql = new StringBuilder(LIST_CLUSTERSINZONE_BY_HOST_CAPACITIES_PART1);

        if(isZone){
            sql.append("capacity.data_center_id = ?");
        }else{
            sql.append("capacity.pod_id = ?");
        }
        sql.append(LIST_CLUSTERSINZONE_BY_HOST_CAPACITIES_PART2);
        if(isZone){
            sql.append("capacity.data_center_id = ?");
        }else{
            sql.append("capacity.pod_id = ?");
        }
        sql.append(LIST_CLUSTERSINZONE_BY_HOST_CAPACITIES_PART3);

        try {
            pstmt = txn.prepareAutoCloseStatement(sql.toString());
            pstmt.setLong(1, id);
            pstmt.setShort(2, CapacityVO.CAPACITY_TYPE_CPU);
            pstmt.setString(3,"cpuOvercommitRatio");
            pstmt.setLong(4, requiredCpu);
            pstmt.setLong(5, id);
            pstmt.setShort(6, CapacityVO.CAPACITY_TYPE_MEMORY);
            pstmt.setString(7,"memoryOvercommitRatio");
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
    public List<Long> listHostsWithEnoughCapacity(int requiredCpu, long requiredRam, Long clusterId, String hostType){
    Transaction txn = Transaction.currentTxn();
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
        public SummedCapacity(long sumUsed, long sumReserved, long sumTotal,
                short capacityType, Long clusterId, Long podId) {
            super();
            this.sumUsed = sumUsed;
            this.sumReserved = sumReserved;
            this.sumTotal = sumTotal;
            this.capacityType = capacityType;
            this.clusterId = clusterId;
            this.podId = podId;
        }
        public SummedCapacity(long sumUsed, long sumReserved, long sumTotal,
                short capacityType, Long clusterId, Long podId, Long zoneId) {
            this(sumUsed, sumReserved, sumTotal, capacityType, clusterId, podId);
            this.dcId = zoneId;
        }

        public SummedCapacity(long sumUsed, long sumTotal, float percentUsed, short capacityType, Long zoneId, Long podId, Long clusterId) {
            super();
            this.sumUsed = sumUsed;
            this.sumTotal = sumTotal;
            this.percentUsed = percentUsed;
            this.capacityType = capacityType;
            this.clusterId = clusterId;
            this.podId = podId;
            this.dcId = zoneId;
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
    }
    @Override
    public List<SummedCapacity> findByClusterPodZone(Long zoneId, Long podId, Long clusterId){

        GenericSearchBuilder<CapacityVO, SummedCapacity> SummedCapacitySearch = createSearchBuilder(SummedCapacity.class);
        SummedCapacitySearch.select("sumUsed", Func.SUM, SummedCapacitySearch.entity().getUsedCapacity());
        SummedCapacitySearch.select("sumTotal", Func.SUM, SummedCapacitySearch.entity().getTotalCapacity());   
        SummedCapacitySearch.select("capacityType", Func.NATIVE, SummedCapacitySearch.entity().getCapacityType());                                
        SummedCapacitySearch.groupBy(SummedCapacitySearch.entity().getCapacityType());

        if(zoneId != null){
            SummedCapacitySearch.and("zoneId", SummedCapacitySearch.entity().getDataCenterId(), Op.EQ);
        }
        if (podId != null){
            SummedCapacitySearch.and("podId", SummedCapacitySearch.entity().getPodId(), Op.EQ);
        }
        if (clusterId != null){
            SummedCapacitySearch.and("clusterId", SummedCapacitySearch.entity().getClusterId(), Op.EQ);
        }
        SummedCapacitySearch.done();


        SearchCriteria<SummedCapacity> sc = SummedCapacitySearch.create();
        if (zoneId != null){
            sc.setParameters("zoneId", zoneId);
        }
        if (podId != null){
            sc.setParameters("podId", podId);
        }
        if (clusterId != null){
            sc.setParameters("clusterId", clusterId);
        }

        return customSearchIncludingRemoved(sc, null);         
    }

    @Override
    public List<SummedCapacity> findNonSharedStorageForClusterPodZone(Long zoneId, Long podId, Long clusterId){

        GenericSearchBuilder<CapacityVO, SummedCapacity> SummedCapacitySearch = createSearchBuilder(SummedCapacity.class);
        SummedCapacitySearch.select("sumUsed", Func.SUM, SummedCapacitySearch.entity().getUsedCapacity());
        SummedCapacitySearch.select("sumTotal", Func.SUM, SummedCapacitySearch.entity().getTotalCapacity());   
        SummedCapacitySearch.select("capacityType", Func.NATIVE, SummedCapacitySearch.entity().getCapacityType());
        SummedCapacitySearch.and("capacityType", SummedCapacitySearch.entity().getCapacityType(), Op.EQ);

        SearchBuilder<StoragePoolVO>  nonSharedStorage = _storagePoolDao.createSearchBuilder();
        nonSharedStorage.and("poolTypes", nonSharedStorage.entity().getPoolType(), SearchCriteria.Op.IN);
        SummedCapacitySearch.join("nonSharedStorage", nonSharedStorage, nonSharedStorage.entity().getId(), SummedCapacitySearch.entity().getHostOrPoolId(), JoinType.INNER);
        nonSharedStorage.done();        

        if(zoneId != null){
            SummedCapacitySearch.and("zoneId", SummedCapacitySearch.entity().getDataCenterId(), Op.EQ);
        }
        if (podId != null){
            SummedCapacitySearch.and("podId", SummedCapacitySearch.entity().getPodId(), Op.EQ);
        }
        if (clusterId != null){
            SummedCapacitySearch.and("clusterId", SummedCapacitySearch.entity().getClusterId(), Op.EQ);
        }
        SummedCapacitySearch.done();


        SearchCriteria<SummedCapacity> sc = SummedCapacitySearch.create();
        sc.setJoinParameters("nonSharedStorage", "poolTypes", Storage.getNonSharedStoragePoolTypes().toArray());
        sc.setParameters("capacityType", Capacity.CAPACITY_TYPE_STORAGE_ALLOCATED);
        if (zoneId != null){
            sc.setParameters("zoneId", zoneId);
        }
        if (podId != null){
            sc.setParameters("podId", podId);
        }
        if (clusterId != null){
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
    public Pair<List<Long>, Map<Long, Double>> orderClustersByAggregateCapacity(long id, short capacityTypeForOrdering, boolean isZone){
        Transaction txn = Transaction.currentTxn();
        PreparedStatement pstmt = null;
        List<Long> result = new ArrayList<Long>();
        Map<Long, Double> clusterCapacityMap = new HashMap<Long, Double>();
        StringBuilder sql = new StringBuilder();
        if (capacityTypeForOrdering != Capacity.CAPACITY_TYPE_CPU && capacityTypeForOrdering != Capacity.CAPACITY_TYPE_MEMORY)  {
             sql.append(ORDER_CLUSTERS_BY_AGGREGATE_CAPACITY_PART1);
        }
        else {
             sql.append(ORDER_CLUSTERS_BY_AGGREGATE_OVERCOMMIT_CAPACITY_PART1);
        }


        if(isZone){
            sql.append(" data_center_id = ?");
        }else{
            sql.append(" pod_id = ?");
        }
        if (capacityTypeForOrdering != Capacity.CAPACITY_TYPE_CPU && capacityTypeForOrdering != Capacity.CAPACITY_TYPE_MEMORY){
           sql.append(ORDER_CLUSTERS_BY_AGGREGATE_CAPACITY_PART2);
        }
        else {
           sql.append(ORDER_CLUSTERS_BY_AGGREGATE_OVERCOMMIT_CAPACITY_PART2);
        }

        try {
            pstmt = txn.prepareAutoCloseStatement(sql.toString());
            pstmt.setLong(1, id);
            pstmt.setShort(2,capacityTypeForOrdering);

            if (capacityTypeForOrdering == Capacity.CAPACITY_TYPE_CPU){
                pstmt.setString(3,"cpuOvercommitRatio");
            }
            else if (capacityTypeForOrdering == Capacity.CAPACITY_TYPE_MEMORY){
                pstmt.setString(3,"memoryOvercommitRatio");
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
    public List<Long> listPodsByHostCapacities(long zoneId, int requiredCpu, long requiredRam, short capacityType) {
        Transaction txn = Transaction.currentTxn();
        PreparedStatement pstmt = null;
        List<Long> result = new ArrayList<Long>();

        StringBuilder sql = new StringBuilder(LIST_PODSINZONE_BY_HOST_CAPACITY_TYPE);
                      sql.append("AND capacity.pod_id IN (");
                      sql.append(LIST_PODSINZONE_BY_HOST_CAPACITY_TYPE);
                      sql.append(")");

        try {
            pstmt = txn.prepareAutoCloseStatement(sql.toString());
            pstmt.setLong(1, zoneId);
            pstmt.setShort(2, CapacityVO.CAPACITY_TYPE_CPU);
            pstmt.setString(3, "cpuOvercommitRatio");
            pstmt.setLong(4, requiredCpu);
            pstmt.setLong(5, zoneId);
            pstmt.setShort(6, CapacityVO.CAPACITY_TYPE_MEMORY);
            pstmt.setString(7,"memoryOvercommitRatio" );
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
        Transaction txn = Transaction.currentTxn();
        PreparedStatement pstmt = null;
        List<Long> result = new ArrayList<Long>();
        Map<Long, Double> podCapacityMap = new HashMap<Long, Double>();
        
        StringBuilder sql = new StringBuilder(ORDER_PODS_BY_AGGREGATE_CAPACITY);
        try {
            pstmt = txn.prepareAutoCloseStatement(sql.toString());
            pstmt.setLong(2, zoneId);
            pstmt.setShort(3, capacityTypeForOrdering);
            
            if(capacityTypeForOrdering == CapacityVO.CAPACITY_TYPE_CPU){
                pstmt.setString(3, "cpuOvercommitRatio");
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
    public void updateCapacityState(Long dcId, Long podId, Long clusterId, Long hostId, String capacityState) {
        Transaction txn = Transaction.currentTxn();
        StringBuilder sql = new StringBuilder(UPDATE_CAPACITY_STATE);
        List<Long> resourceIdList = new ArrayList<Long>();

        if (dcId != null){
            sql.append(" data_center_id = ?");
            resourceIdList.add(dcId);
        }
        if (podId != null){
            sql.append(" pod_id = ?");
            resourceIdList.add(podId);
        }
        if (clusterId != null){
            sql.append(" cluster_id = ?");
            resourceIdList.add(clusterId);
        }
        if (hostId != null){
            sql.append(" host_id = ?");
            resourceIdList.add(hostId);
        }

        PreparedStatement pstmt = null;
        try {       
            pstmt = txn.prepareAutoCloseStatement(sql.toString());
            pstmt.setString(1, capacityState);
            for (int i = 0; i < resourceIdList.size(); i++){                
                pstmt.setLong( 2+i, resourceIdList.get(i));
            }            
            pstmt.executeUpdate();
        } catch (Exception e) {
            s_logger.warn("Error updating CapacityVO", e);
        }
    }
}
