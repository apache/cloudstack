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
package com.cloud.storage.dao;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import javax.ejb.Local;

import com.cloud.storage.StoragePoolWorkVO;
import com.cloud.utils.db.DB;
import com.cloud.utils.db.GenericDaoBase;
import com.cloud.utils.db.SearchBuilder;
import com.cloud.utils.db.SearchCriteria;
import com.cloud.utils.db.Transaction;
import com.cloud.utils.exception.CloudRuntimeException;

@Local(value={StoragePoolWorkDao.class}) @DB(txn=false)
public class StoragePoolWorkDaoImpl extends GenericDaoBase<StoragePoolWorkVO, Long>  implements StoragePoolWorkDao {

    protected final SearchBuilder<StoragePoolWorkVO> PendingWorkForPrepareForMaintenanceSearch;
    protected final SearchBuilder<StoragePoolWorkVO> PendingWorkForCancelMaintenanceSearch;
    protected final SearchBuilder<StoragePoolWorkVO> PoolAndVmIdSearch;
    protected final SearchBuilder<StoragePoolWorkVO> PendingJobsForDeadMs;
    
    private final String FindPoolIds = "SELECT distinct storage_pool_work.pool_id FROM storage_pool_work WHERE mgmt_server_id = ?";
    
    protected StoragePoolWorkDaoImpl() {
        PendingWorkForPrepareForMaintenanceSearch = createSearchBuilder();
        PendingWorkForPrepareForMaintenanceSearch.and("poolId", PendingWorkForPrepareForMaintenanceSearch.entity().getPoolId(), SearchCriteria.Op.EQ);
        PendingWorkForPrepareForMaintenanceSearch.and("stoppedForMaintenance", PendingWorkForPrepareForMaintenanceSearch.entity().isStoppedForMaintenance(), SearchCriteria.Op.EQ);
        PendingWorkForPrepareForMaintenanceSearch.and("startedAfterMaintenance", PendingWorkForPrepareForMaintenanceSearch.entity().isStartedAfterMaintenance(), SearchCriteria.Op.EQ);
        PendingWorkForPrepareForMaintenanceSearch.done();
        
        PendingWorkForCancelMaintenanceSearch = createSearchBuilder();
        PendingWorkForCancelMaintenanceSearch.and("poolId", PendingWorkForCancelMaintenanceSearch.entity().getPoolId(), SearchCriteria.Op.EQ);
        PendingWorkForCancelMaintenanceSearch.and("stoppedForMaintenance", PendingWorkForCancelMaintenanceSearch.entity().isStoppedForMaintenance(), SearchCriteria.Op.EQ);
        PendingWorkForCancelMaintenanceSearch.and("startedAfterMaintenance", PendingWorkForCancelMaintenanceSearch.entity().isStartedAfterMaintenance(), SearchCriteria.Op.EQ);
        PendingWorkForCancelMaintenanceSearch.done();

        PoolAndVmIdSearch = createSearchBuilder();
        PoolAndVmIdSearch.and("poolId", PoolAndVmIdSearch.entity().getPoolId(), SearchCriteria.Op.EQ);
        PoolAndVmIdSearch.and("vmId", PoolAndVmIdSearch.entity().getVmId(), SearchCriteria.Op.EQ);
        PoolAndVmIdSearch.done();
        
        PendingJobsForDeadMs = createSearchBuilder();
        PendingJobsForDeadMs.and("managementServerId", PendingJobsForDeadMs.entity().getManagementServerId(), SearchCriteria.Op.EQ);
        PendingJobsForDeadMs.and("poolId", PendingJobsForDeadMs.entity().getPoolId(), SearchCriteria.Op.EQ);
        PendingJobsForDeadMs.and("stoppedForMaintenance", PendingJobsForDeadMs.entity().isStoppedForMaintenance(), SearchCriteria.Op.EQ);
        PendingJobsForDeadMs.and("startedAfterMaintenance", PendingJobsForDeadMs.entity().isStartedAfterMaintenance(), SearchCriteria.Op.EQ);
        PendingJobsForDeadMs.done();
        
    }
    
    @Override
    public List<StoragePoolWorkVO> listPendingWorkForPrepareForMaintenanceByPoolId(long poolId) {
        SearchCriteria<StoragePoolWorkVO> sc = PendingWorkForPrepareForMaintenanceSearch.create();
        sc.setParameters("poolId", poolId);
        sc.setParameters("stoppedForMaintenance", false);
        sc.setParameters("startedAfterMaintenance", false);
        return listBy(sc);
    }
    
    @Override
    public List<StoragePoolWorkVO> listPendingWorkForCancelMaintenanceByPoolId(long poolId) {
        SearchCriteria<StoragePoolWorkVO> sc = PendingWorkForCancelMaintenanceSearch.create();
        sc.setParameters("poolId", poolId);
        sc.setParameters("stoppedForMaintenance", true);
        sc.setParameters("startedAfterMaintenance", false);
        return listBy(sc);
    }
    
    @Override
    public StoragePoolWorkVO findByPoolIdAndVmId(long poolId, long vmId) {
        SearchCriteria<StoragePoolWorkVO> sc = PoolAndVmIdSearch.create();
        sc.setParameters("poolId", poolId);
        sc.setParameters("vmId", vmId);
        return listBy(sc).get(0);
    }
    
    @Override
    public void removePendingJobsOnMsRestart(long msId, long poolId) {
        //hung jobs are those which are stopped, but never started
        SearchCriteria<StoragePoolWorkVO> sc = PendingJobsForDeadMs.create();
        sc.setParameters("managementServerId", msId);
        sc.setParameters("poolId", poolId);
        sc.setParameters("stoppedForMaintenance", true);
        sc.setParameters("startedAfterMaintenance", false);        
        remove(sc);
    }
    
    @Override
    @DB
    public List<Long> searchForPoolIdsForPendingWorkJobs(long msId){
        
        StringBuilder sql = new StringBuilder(FindPoolIds);

        Transaction txn = Transaction.currentTxn();
        PreparedStatement pstmt = null;
        try {
            pstmt = txn.prepareAutoCloseStatement(sql.toString());
            pstmt.setLong(1, msId);

            ResultSet rs = pstmt.executeQuery();
            List<Long> poolIds = new ArrayList<Long>();

            while (rs.next()) {
                poolIds.add(rs.getLong("pool_id"));
            }           
            return poolIds;
        } catch (SQLException e) {
            throw new CloudRuntimeException("Unable to execute " + pstmt.toString(), e);
        }

    }
}
