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

package com.cloud.usage.dao;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.TimeZone;


import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.springframework.stereotype.Component;

import com.cloud.usage.UsageSnapshotOnPrimaryVO;
import com.cloud.utils.DateUtil;
import com.cloud.utils.db.GenericDaoBase;
import com.cloud.utils.db.TransactionLegacy;

@Component
public class UsageVMSnapshotOnPrimaryDaoImpl extends GenericDaoBase<UsageSnapshotOnPrimaryVO, Long> implements UsageVMSnapshotOnPrimaryDao {
    public static final Logger s_logger = Logger.getLogger(UsageVMSnapshotOnPrimaryDaoImpl.class.getName());
    protected static final String GET_USAGE_RECORDS_BY_ACCOUNT = "SELECT id, zone_id, account_id, domain_id, vm_id, name, type, physicalsize, virtualsize, created, deleted, vm_snapshot_id "
        + " FROM usage_snapshot_on_primary" + " WHERE account_id = ? " + " AND ( (created < ? AND deleted is NULL)"
        + "     OR ( deleted BETWEEN ? AND ?)) ORDER BY created asc";
    protected static final String UPDATE_DELETED = "UPDATE usage_snapshot_on_primary SET deleted = ? WHERE account_id = ? AND id = ? and vm_id = ?  and created = ?";

    @Override
    public void updateDeleted(UsageSnapshotOnPrimaryVO usage) {
        TransactionLegacy txn = TransactionLegacy.open(TransactionLegacy.USAGE_DB);
        PreparedStatement pstmt = null;
        try {
            txn.start();
            pstmt = txn.prepareAutoCloseStatement(UPDATE_DELETED);
            pstmt.setString(1, DateUtil.getDateDisplayString(TimeZone.getTimeZone("GMT"), usage.getDeleted()));
            pstmt.setLong(2, usage.getAccountId());
            pstmt.setLong(3, usage.getId());
            pstmt.setLong(4, usage.getVmId());
            pstmt.setString(5, DateUtil.getDateDisplayString(TimeZone.getTimeZone("GMT"), usage.getCreated()));
            pstmt.executeUpdate();
            txn.commit();
        } catch (Exception e) {
            txn.rollback();
            s_logger.warn("Error updating UsageSnapshotOnPrimaryVO", e);
        } finally {
            txn.close();
        }
    }

    @Override
    public List<UsageSnapshotOnPrimaryVO> getUsageRecords(Long accountId, Long domainId, Date startDate, Date endDate) {
        List<UsageSnapshotOnPrimaryVO> usageRecords = new ArrayList<UsageSnapshotOnPrimaryVO>();

        String sql = GET_USAGE_RECORDS_BY_ACCOUNT;
        TransactionLegacy txn = TransactionLegacy.open(TransactionLegacy.USAGE_DB);
        PreparedStatement pstmt = null;

        try {
            int i = 1;
            pstmt = txn.prepareAutoCloseStatement(sql);
            pstmt.setLong(i++, accountId);
            pstmt.setString(i++, DateUtil.getDateDisplayString(TimeZone.getTimeZone("GMT"), endDate));
            pstmt.setString(i++, DateUtil.getDateDisplayString(TimeZone.getTimeZone("GMT"), startDate));
            pstmt.setString(i++, DateUtil.getDateDisplayString(TimeZone.getTimeZone("GMT"), endDate));
            s_logger.debug("GET_USAGE_RECORDS_BY_ACCOUNT " + pstmt);
            ResultSet rs = pstmt.executeQuery();
            while (rs.next()) {
                //id, zone_id, account_id, domain_iVMSnapshotVOd, vm_id, disk_offering_id, size, created, deleted
                Long vId = Long.valueOf(rs.getLong(1));
                Long zoneId = Long.valueOf(rs.getLong(2));
                Long acctId = Long.valueOf(rs.getLong(3));
                Long dId = Long.valueOf(rs.getLong(4));
                Long vmId = Long.valueOf(rs.getLong(5));
                String name = String.valueOf(rs.getString(6));
                Integer type = Integer.valueOf(rs.getInt(7));
                Long physicalSize = Long.valueOf(rs.getLong(8));
                Long virtaulSize = Long.valueOf(rs.getLong(9));
                Date createdDate = null;
                Date deleteDate = null;
                String createdTS = rs.getString(10);
                String deleted = rs.getString(11);
                String snapId = rs.getString(12);
                Long vmSnapshotId = StringUtils.isNotBlank(snapId) ? Long.valueOf(snapId) : null;

                if (createdTS != null) {
                    createdDate = DateUtil.parseDateString(s_gmtTimeZone, createdTS);
                }
                if (deleted != null) {
                    deleteDate = DateUtil.parseDateString(s_gmtTimeZone, deleted);
                }
                UsageSnapshotOnPrimaryVO usageSnapshotOnPrimaryVO = new UsageSnapshotOnPrimaryVO(vId, zoneId, acctId, dId, vmId, name, type, virtaulSize, physicalSize, createdDate, deleteDate);
                usageSnapshotOnPrimaryVO.setVmSnapshotId(vmSnapshotId);
                usageRecords.add(usageSnapshotOnPrimaryVO);
            }
        } catch (Exception e) {
            txn.rollback();
            s_logger.warn("Error getting usage records", e);
        } finally {
            txn.close();
        }

        return usageRecords;
    }

}
