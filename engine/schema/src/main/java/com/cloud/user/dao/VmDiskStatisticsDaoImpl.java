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
package com.cloud.user.dao;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.TimeZone;


import org.springframework.stereotype.Component;

import com.cloud.user.VmDiskStatisticsVO;
import com.cloud.utils.DateUtil;
import com.cloud.utils.db.GenericDaoBase;
import com.cloud.utils.db.SearchBuilder;
import com.cloud.utils.db.SearchCriteria;
import com.cloud.utils.db.TransactionLegacy;

@Component
public class VmDiskStatisticsDaoImpl extends GenericDaoBase<VmDiskStatisticsVO, Long> implements VmDiskStatisticsDao {
    private static final String ACTIVE_AND_RECENTLY_DELETED_SEARCH =
        "SELECT bcf.id, bcf.data_center_id, bcf.account_id, bcf.vm_id, bcf.volume_id, bcf.agg_io_read, bcf.agg_io_write, bcf.agg_bytes_read, bcf.agg_bytes_write "
            + "FROM vm_disk_statistics bcf, account a " + "WHERE bcf.account_id = a.id AND (a.removed IS NULL OR a.removed >= ?) " + "ORDER BY bcf.id";
    private static final String UPDATED_VM_NETWORK_STATS_SEARCH = "SELECT id, current_io_read, current_io_write, net_io_read, net_io_write, agg_io_read, agg_io_write, "
        + "current_bytes_read, current_bytes_write, net_bytes_read, net_bytes_write, agg_bytes_read, agg_bytes_write " + "from  vm_disk_statistics "
        + "where (agg_io_read < net_io_read + current_io_read) OR (agg_io_write < net_io_write + current_io_write) OR "
        + "(agg_bytes_read < net_bytes_read + current_bytes_read) OR (agg_bytes_write < net_bytes_write + current_bytes_write)";
    private final SearchBuilder<VmDiskStatisticsVO> AllFieldsSearch;
    private final SearchBuilder<VmDiskStatisticsVO> AccountSearch;

    public VmDiskStatisticsDaoImpl() {
        AccountSearch = createSearchBuilder();
        AccountSearch.and("account", AccountSearch.entity().getAccountId(), SearchCriteria.Op.EQ);
        AccountSearch.done();

        AllFieldsSearch = createSearchBuilder();
        AllFieldsSearch.and("account", AllFieldsSearch.entity().getAccountId(), SearchCriteria.Op.EQ);
        AllFieldsSearch.and("dc", AllFieldsSearch.entity().getDataCenterId(), SearchCriteria.Op.EQ);
        AllFieldsSearch.and("volume", AllFieldsSearch.entity().getVolumeId(), SearchCriteria.Op.EQ);
        AllFieldsSearch.and("vm", AllFieldsSearch.entity().getVmId(), SearchCriteria.Op.EQ);
        AllFieldsSearch.done();
    }

    @Override
    public VmDiskStatisticsVO findBy(long accountId, long dcId, long vmId, long volumeId) {
        SearchCriteria<VmDiskStatisticsVO> sc = AllFieldsSearch.create();
        sc.setParameters("account", accountId);
        sc.setParameters("dc", dcId);
        sc.setParameters("volume", volumeId);
        sc.setParameters("vm", vmId);
        return findOneBy(sc);
    }

    @Override
    public VmDiskStatisticsVO lock(long accountId, long dcId, long vmId, long volumeId) {
        SearchCriteria<VmDiskStatisticsVO> sc = AllFieldsSearch.create();
        sc.setParameters("account", accountId);
        sc.setParameters("dc", dcId);
        sc.setParameters("volume", volumeId);
        sc.setParameters("vm", vmId);
        return lockOneRandomRow(sc, true);
    }

    @Override
    public List<VmDiskStatisticsVO> listBy(long accountId) {
        SearchCriteria<VmDiskStatisticsVO> sc = AccountSearch.create();
        sc.setParameters("account", accountId);
        return search(sc, null);
    }

    @Override
    public List<VmDiskStatisticsVO> listActiveAndRecentlyDeleted(Date minRemovedDate, int startIndex, int limit) {
        List<VmDiskStatisticsVO> vmDiskStats = new ArrayList<VmDiskStatisticsVO>();
        if (minRemovedDate == null)
            return vmDiskStats;

        TransactionLegacy txn = TransactionLegacy.currentTxn();
        try {
            String sql = ACTIVE_AND_RECENTLY_DELETED_SEARCH + " LIMIT " + startIndex + "," + limit;
            PreparedStatement pstmt = null;
            pstmt = txn.prepareAutoCloseStatement(sql);
            pstmt.setString(1, DateUtil.getDateDisplayString(TimeZone.getTimeZone("GMT"), minRemovedDate));
            ResultSet rs = pstmt.executeQuery();
            while (rs.next()) {
                vmDiskStats.add(toEntityBean(rs, false));
            }
        } catch (Exception ex) {
            logger.error("error saving vm disk stats to cloud_usage db", ex);
        }
        return vmDiskStats;
    }

    @Override
    public List<VmDiskStatisticsVO> listUpdatedStats() {
        List<VmDiskStatisticsVO> vmDiskStats = new ArrayList<VmDiskStatisticsVO>();

        TransactionLegacy txn = TransactionLegacy.currentTxn();
        try {
            PreparedStatement pstmt = null;
            pstmt = txn.prepareAutoCloseStatement(UPDATED_VM_NETWORK_STATS_SEARCH);
            ResultSet rs = pstmt.executeQuery();
            while (rs.next()) {
                vmDiskStats.add(toEntityBean(rs, false));
            }
        } catch (Exception ex) {
            logger.error("error lisitng updated vm disk stats", ex);
        }
        return vmDiskStats;
    }

}
