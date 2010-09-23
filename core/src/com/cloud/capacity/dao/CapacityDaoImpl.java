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

import javax.ejb.Local;

import org.apache.log4j.Logger;

import com.cloud.capacity.CapacityVO;
import com.cloud.utils.db.GenericDaoBase;
import com.cloud.utils.db.Transaction;

@Local(value = { CapacityDao.class })
public class CapacityDaoImpl extends GenericDaoBase<CapacityVO, Long> implements CapacityDao {
    private static final Logger s_logger = Logger.getLogger(CapacityDaoImpl.class);

    private static final String ADD_ALLOCATED_SQL = "UPDATE `cloud`.`op_host_capacity` SET used_capacity = used_capacity + ? WHERE host_id = ? AND capacity_type = ?";
    private static final String SUBTRACT_ALLOCATED_SQL = "UPDATE `cloud`.`op_host_capacity` SET used_capacity = used_capacity - ? WHERE host_id = ? AND capacity_type = ?";
    private static final String CLEAR_STORAGE_CAPACITIES = "DELETE FROM `cloud`.`op_host_capacity` WHERE capacity_type=2 OR capacity_type=3 OR capacity_type=6"; //clear storage and secondary_storage capacities
    private static final String CLEAR_NON_STORAGE_CAPACITIES = "DELETE FROM `cloud`.`op_host_capacity` WHERE capacity_type<>2 AND capacity_type<>3 AND capacity_type<>6"; //clear non-storage and non-secondary_storage capacities

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
}
