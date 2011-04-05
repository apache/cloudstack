/**
 *  Copyright (C) 2010 Cloud.com, Inc.  All rights reserved.
 * 
 * This software is licensed under the GNU General Public License v3 or later.
 * 
 * It is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or any later version.
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 * 
 */

package com.cloud.capacity.dao;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import javax.ejb.Local;

import org.apache.log4j.Logger;

import com.cloud.capacity.CapacityVO;
import com.cloud.utils.db.GenericDaoBase;
import com.cloud.utils.db.SearchBuilder;
import com.cloud.utils.db.SearchCriteria;
import com.cloud.utils.db.Transaction;
import com.cloud.utils.exception.CloudRuntimeException;

@Local(value = { CapacityDao.class })
public class CapacityDaoImpl extends GenericDaoBase<CapacityVO, Long> implements CapacityDao {
    private static final Logger s_logger = Logger.getLogger(CapacityDaoImpl.class);

    private static final String ADD_ALLOCATED_SQL = "UPDATE `cloud`.`op_host_capacity` SET used_capacity = used_capacity + ? WHERE host_id = ? AND capacity_type = ?";
    private static final String SUBTRACT_ALLOCATED_SQL = "UPDATE `cloud`.`op_host_capacity` SET used_capacity = used_capacity - ? WHERE host_id = ? AND capacity_type = ?";
    private static final String CLEAR_STORAGE_CAPACITIES = "DELETE FROM `cloud`.`op_host_capacity` WHERE capacity_type=2 OR capacity_type=3 OR capacity_type=6"; //clear storage and secondary_storage capacities
    private static final String CLEAR_NON_STORAGE_CAPACITIES = "DELETE FROM `cloud`.`op_host_capacity` WHERE capacity_type<>2 AND capacity_type<>3 AND capacity_type<>6"; //clear non-storage and non-secondary_storage capacities
    private static final String CLEAR_NON_STORAGE_CAPACITIES2 = "DELETE FROM `cloud`.`op_host_capacity` WHERE capacity_type<>2 AND capacity_type<>3 AND capacity_type<>6 AND capacity_type<>0 AND capacity_type<>1"; //clear non-storage and non-secondary_storage capacities

    private static final String LIST_CLUSTERSINZONE_BY_HOST_CAPACITIES_PART1 = "SELECT DISTINCT cluster_id  FROM `cloud`.`op_host_capacity` WHERE ";
    private static final String LIST_CLUSTERSINZONE_BY_HOST_CAPACITIES_PART2 = " AND capacity_type = ? AND (total_capacity - used_capacity + reserved_capacity) >= ? " +
    		"AND cluster_id IN (SELECT distinct cluster_id  FROM `cloud`.`op_host_capacity` WHERE ";
    private static final String LIST_CLUSTERSINZONE_BY_HOST_CAPACITIES_PART3 = " AND capacity_type = ? AND (total_capacity - used_capacity + reserved_capacity) >= ?) " +
    		"ORDER BY (total_capacity - used_capacity + reserved_capacity) DESC";
    
    private SearchBuilder<CapacityVO> _hostIdTypeSearch;
	private SearchBuilder<CapacityVO> _hostOrPoolIdSearch;
	
	private static final String LIST_HOSTS_IN_CLUSTER_WITH_ENOUGH_CAPACITY = "SELECT a.host_id FROM (host JOIN op_host_capacity a ON host.id = a.host_id AND host.cluster_id = ? AND host.type = ? " +
			"AND a.total_capacity - a.used_capacity >= ? and a.capacity_type = 1) " +
			"JOIN op_host_capacity b ON a.host_id = b.host_id AND b.total_capacity - b.used_capacity >= ? AND b.capacity_type = 0";
    
    public CapacityDaoImpl() {
    	_hostIdTypeSearch = createSearchBuilder();
    	_hostIdTypeSearch.and("hostId", _hostIdTypeSearch.entity().getHostOrPoolId(), SearchCriteria.Op.EQ);
    	_hostIdTypeSearch.and("type", _hostIdTypeSearch.entity().getCapacityType(), SearchCriteria.Op.EQ);
    	_hostIdTypeSearch.done();
    	
    	_hostOrPoolIdSearch = createSearchBuilder();
    	_hostOrPoolIdSearch.and("hostId", _hostOrPoolIdSearch.entity().getHostOrPoolId(), SearchCriteria.Op.EQ);
    	_hostOrPoolIdSearch.done();
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
    public void clearNonStorageCapacities() {
        Transaction txn = Transaction.currentTxn();
        PreparedStatement pstmt = null;
        try {
            txn.start();
            String sql = CLEAR_NON_STORAGE_CAPACITIES;
            pstmt = txn.prepareAutoCloseStatement(sql);
            pstmt.executeUpdate();
            txn.commit();
        } catch (Exception e) {
            txn.rollback();
            s_logger.warn("Exception clearing non storage capacities", e);
        }
    }
    
    @Override
    public void clearNonStorageCapacities2() {
        Transaction txn = Transaction.currentTxn();
        PreparedStatement pstmt = null;
        try {
            txn.start();
            String sql = CLEAR_NON_STORAGE_CAPACITIES2;
            pstmt = txn.prepareAutoCloseStatement(sql);
            pstmt.executeUpdate();
            txn.commit();
        } catch (Exception e) {
            txn.rollback();
            s_logger.warn("Exception clearing non storage capacities", e);
        }
    }

    @Override
    public void clearStorageCapacities() {
        Transaction txn = Transaction.currentTxn();
        PreparedStatement pstmt = null;
        try {
            txn.start();
            String sql = CLEAR_STORAGE_CAPACITIES;
            pstmt = txn.prepareAutoCloseStatement(sql);
            pstmt.executeUpdate();
            txn.commit();
        } catch (Exception e) {
            txn.rollback();
            s_logger.warn("Exception clearing storage capacities", e);
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
    public java.util.List<CapacityVO> findByHostorPoolId(Long hostorPoolId){
    	SearchCriteria<CapacityVO> sc = _hostOrPoolIdSearch.create();
    	sc.setParameters("hostId", hostorPoolId);
    	return listBy(sc);
    }
    
    
    @Override
    public List<Long> orderClustersInZoneOrPodByHostCapacities(long id, int requiredCpu, long requiredRam, short capacityTypeForOrdering, boolean isZone){
    	Transaction txn = Transaction.currentTxn();
        PreparedStatement pstmt = null;
        List<Long> result = new ArrayList<Long>();

        StringBuilder sql = new StringBuilder(LIST_CLUSTERSINZONE_BY_HOST_CAPACITIES_PART1);
        if(isZone){
        	sql.append("data_center_id = ?");
        }else{
        	sql.append("pod_id = ?");
        }
        sql.append(LIST_CLUSTERSINZONE_BY_HOST_CAPACITIES_PART2);
        if(isZone){
        	sql.append("data_center_id = ?");
        }else{
        	sql.append("pod_id = ?");
        }
        sql.append(LIST_CLUSTERSINZONE_BY_HOST_CAPACITIES_PART3);

        try {
            pstmt = txn.prepareAutoCloseStatement(sql.toString());
            pstmt.setLong(1, id);
            if(capacityTypeForOrdering == CapacityVO.CAPACITY_TYPE_CPU){
            	pstmt.setShort(2, CapacityVO.CAPACITY_TYPE_CPU);
            	pstmt.setLong(3, requiredCpu);
            	pstmt.setLong(4, id);
            	pstmt.setShort(5, CapacityVO.CAPACITY_TYPE_MEMORY);
            	pstmt.setLong(6, requiredRam);
            }else{
            	pstmt.setShort(2, CapacityVO.CAPACITY_TYPE_MEMORY);
            	pstmt.setLong(3, requiredRam);
            	pstmt.setLong(4, id);
            	pstmt.setShort(5, CapacityVO.CAPACITY_TYPE_CPU);
            	pstmt.setLong(6, requiredCpu);
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
}
