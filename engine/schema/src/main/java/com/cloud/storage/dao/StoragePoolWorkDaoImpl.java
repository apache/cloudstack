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
package com.cloud.storage.dao;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;


import org.springframework.stereotype.Component;

import com.cloud.storage.StoragePoolWorkVO;
import com.cloud.utils.db.DB;
import com.cloud.utils.db.GenericDaoBase;
import com.cloud.utils.db.SearchBuilder;
import com.cloud.utils.db.SearchCriteria;
import com.cloud.utils.db.TransactionLegacy;
import com.cloud.utils.exception.CloudRuntimeException;

@Component
@DB()
public class StoragePoolWorkDaoImpl extends GenericDaoBase<StoragePoolWorkVO, Long> implements StoragePoolWorkDao {

    protected final SearchBuilder<StoragePoolWorkVO> PendingWorkForPrepareForMaintenanceSearch;
    protected final SearchBuilder<StoragePoolWorkVO> PendingWorkForCancelMaintenanceSearch;
    protected final SearchBuilder<StoragePoolWorkVO> PoolAndVmIdSearch;
    protected final SearchBuilder<StoragePoolWorkVO> PendingJobsForDeadMs;

    private final String FindPoolIds = "SELECT distinct storage_pool_work.pool_id FROM storage_pool_work WHERE mgmt_server_id = ?";

    protected StoragePoolWorkDaoImpl() {
        PendingWorkForPrepareForMaintenanceSearch = createSearchBuilder();
        PendingWorkForPrepareForMaintenanceSearch.and("poolId", PendingWorkForPrepareForMaintenanceSearch.entity().getPoolId(), SearchCriteria.Op.EQ);
        PendingWorkForPrepareForMaintenanceSearch.and("stoppedForMaintenance", PendingWorkForPrepareForMaintenanceSearch.entity().isStoppedForMaintenance(),
            SearchCriteria.Op.EQ);
        PendingWorkForPrepareForMaintenanceSearch.and("startedAfterMaintenance", PendingWorkForPrepareForMaintenanceSearch.entity().isStartedAfterMaintenance(),
            SearchCriteria.Op.EQ);
        PendingWorkForPrepareForMaintenanceSearch.done();

        PendingWorkForCancelMaintenanceSearch = createSearchBuilder();
        PendingWorkForCancelMaintenanceSearch.and("poolId", PendingWorkForCancelMaintenanceSearch.entity().getPoolId(), SearchCriteria.Op.EQ);
        PendingWorkForCancelMaintenanceSearch.and("stoppedForMaintenance", PendingWorkForCancelMaintenanceSearch.entity().isStoppedForMaintenance(), SearchCriteria.Op.EQ);
        PendingWorkForCancelMaintenanceSearch.and("startedAfterMaintenance", PendingWorkForCancelMaintenanceSearch.entity().isStartedAfterMaintenance(),
            SearchCriteria.Op.EQ);
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
        // hung jobs are those which are stopped, but never started
        SearchCriteria<StoragePoolWorkVO> sc = PendingJobsForDeadMs.create();
        sc.setParameters("managementServerId", msId);
        sc.setParameters("poolId", poolId);
        sc.setParameters("stoppedForMaintenance", true);
        sc.setParameters("startedAfterMaintenance", false);
        remove(sc);
    }

    @Override
    @DB
    public List<Long> searchForPoolIdsForPendingWorkJobs(long msId) {
        StringBuilder sql = new StringBuilder(FindPoolIds);
        TransactionLegacy txn = TransactionLegacy.currentTxn();
        List<Long> poolIds = new ArrayList<Long>();
        try (PreparedStatement  pstmt = txn.prepareStatement(sql.toString());){
            if(pstmt != null) {
                pstmt.setLong(1, msId);
                try (ResultSet rs = pstmt.executeQuery();) {
                    while (rs.next()) {
                        poolIds.add(rs.getLong("pool_id"));
                    }
                } catch (SQLException e) {
                    throw new CloudRuntimeException("searchForPoolIdsForPendingWorkJobs:Exception:" + e.getMessage(), e);
                }
            }
            return poolIds;
        } catch (SQLException e) {
            throw new CloudRuntimeException("searchForPoolIdsForPendingWorkJobs:Exception:" + e.getMessage(), e);
        }
    }
}
