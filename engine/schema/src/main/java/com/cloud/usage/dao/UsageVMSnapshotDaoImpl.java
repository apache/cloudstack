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

import com.cloud.usage.UsageVMSnapshotVO;
import com.cloud.utils.DateUtil;
import com.cloud.utils.db.GenericDaoBase;
import com.cloud.utils.db.TransactionLegacy;

@Component
public class UsageVMSnapshotDaoImpl extends GenericDaoBase<UsageVMSnapshotVO, Long> implements UsageVMSnapshotDao {
    public static final Logger s_logger = Logger.getLogger(UsageVMSnapshotDaoImpl.class.getName());
    protected static final String GET_USAGE_RECORDS_BY_ACCOUNT = "SELECT id, zone_id, account_id, domain_id, vm_id, disk_offering_id, size, created, processed, vm_snapshot_id "
        + " FROM usage_vmsnapshot" + " WHERE account_id = ? " + " AND ( (created BETWEEN ? AND ?) OR "
        + "      (created < ? AND processed is NULL) ) ORDER BY created asc";
    protected static final String UPDATE_DELETED = "UPDATE usage_vmsnapshot SET processed = ? WHERE account_id = ? AND id = ? and vm_id = ?  and created = ?";

    protected static final String PREVIOUS_QUERY = "SELECT id, zone_id, account_id, domain_id, vm_id, disk_offering_id,size, created, processed, vm_snapshot_id "
        + "FROM usage_vmsnapshot " + "WHERE account_id = ? AND id = ? AND vm_id = ? AND created < ? AND processed IS NULL " + "ORDER BY created desc limit 1";

    @Override
    public void update(UsageVMSnapshotVO usage) {
        TransactionLegacy txn = TransactionLegacy.open(TransactionLegacy.USAGE_DB);
        PreparedStatement pstmt = null;
        try {
            txn.start();
            pstmt = txn.prepareAutoCloseStatement(UPDATE_DELETED);
            pstmt.setString(1, DateUtil.getDateDisplayString(TimeZone.getTimeZone("GMT"), usage.getProcessed()));
            pstmt.setLong(2, usage.getAccountId());
            pstmt.setLong(3, usage.getId());
            pstmt.setLong(4, usage.getVmId());
            pstmt.setString(5, DateUtil.getDateDisplayString(TimeZone.getTimeZone("GMT"), usage.getCreated()));
            pstmt.executeUpdate();
            txn.commit();
        } catch (Exception e) {
            txn.rollback();
            s_logger.warn("Error updating UsageVMSnapshotVO", e);
        } finally {
            txn.close();
        }
    }

    @Override
    public List<UsageVMSnapshotVO> getUsageRecords(Long accountId, Long domainId, Date startDate, Date endDate) {
        List<UsageVMSnapshotVO> usageRecords = new ArrayList<UsageVMSnapshotVO>();

        String sql = GET_USAGE_RECORDS_BY_ACCOUNT;
        TransactionLegacy txn = TransactionLegacy.open(TransactionLegacy.USAGE_DB);
        PreparedStatement pstmt = null;

        try {
            int i = 1;
            pstmt = txn.prepareAutoCloseStatement(sql);
            pstmt.setLong(i++, accountId);
            pstmt.setString(i++, DateUtil.getDateDisplayString(TimeZone.getTimeZone("GMT"), startDate));
            pstmt.setString(i++, DateUtil.getDateDisplayString(TimeZone.getTimeZone("GMT"), endDate));
            pstmt.setString(i++, DateUtil.getDateDisplayString(TimeZone.getTimeZone("GMT"), startDate));

            ResultSet rs = pstmt.executeQuery();
            while (rs.next()) {
                //id, zone_id, account_id, domain_iVMSnapshotVOd, vm_id, disk_offering_id, size, created, processed
                Long vId = Long.valueOf(rs.getLong(1));
                Long zoneId = Long.valueOf(rs.getLong(2));
                Long acctId = Long.valueOf(rs.getLong(3));
                Long dId = Long.valueOf(rs.getLong(4));
                Long vmId = Long.valueOf(rs.getLong(5));
                Long doId = Long.valueOf(rs.getLong(6));
                if (doId == 0) {
                    doId = null;
                }
                Long size = Long.valueOf(rs.getLong(7));
                Date createdDate = null;
                Date processDate = null;
                String createdTS = rs.getString(8);
                String processed = rs.getString(9);
                String snapId = rs.getString(10);
                Long vmSnapshotId = StringUtils.isNotBlank(snapId) ? Long.valueOf(snapId) : null;

                if (createdTS != null) {
                    createdDate = DateUtil.parseDateString(s_gmtTimeZone, createdTS);
                }
                if (processed != null) {
                    processDate = DateUtil.parseDateString(s_gmtTimeZone, processed);
                }
                UsageVMSnapshotVO usageVMSnapshotVO = new UsageVMSnapshotVO(vId, zoneId, acctId, dId, vmId, doId, size, createdDate, processDate);
                usageVMSnapshotVO.setVmSnapshotId(vmSnapshotId);
                usageRecords.add(usageVMSnapshotVO);
            }
        } catch (Exception e) {
            txn.rollback();
            s_logger.warn("Error getting usage records", e);
        } finally {
            txn.close();
        }

        return usageRecords;
    }

    @Override
    public UsageVMSnapshotVO getPreviousUsageRecord(UsageVMSnapshotVO rec) {
        List<UsageVMSnapshotVO> usageRecords = new ArrayList<UsageVMSnapshotVO>();

        String sql = PREVIOUS_QUERY;
        TransactionLegacy txn = TransactionLegacy.open(TransactionLegacy.USAGE_DB);
        PreparedStatement pstmt = null;
        try {
            int i = 1;
            pstmt = txn.prepareAutoCloseStatement(sql);
            pstmt.setLong(i++, rec.getAccountId());
            pstmt.setLong(i++, rec.getId());
            pstmt.setLong(i++, rec.getVmId());
            pstmt.setString(i++, DateUtil.getDateDisplayString(TimeZone.getTimeZone("GMT"), rec.getCreated()));

            ResultSet rs = pstmt.executeQuery();
            while (rs.next()) {
                //id, zone_id, account_id, domain_iVMSnapshotVOd, vm_id, disk_offering_id, size, created, processed
                Long vId = Long.valueOf(rs.getLong(1));
                Long zoneId = Long.valueOf(rs.getLong(2));
                Long acctId = Long.valueOf(rs.getLong(3));
                Long dId = Long.valueOf(rs.getLong(4));
                Long vmId = Long.valueOf(rs.getLong(5));
                Long doId = Long.valueOf(rs.getLong(6));
                if (doId == 0) {
                    doId = null;
                }
                Long size = Long.valueOf(rs.getLong(7));
                Date createdDate = null;
                Date processDate = null;
                String createdTS = rs.getString(8);
                String processed = rs.getString(9);
                String snapId = rs.getString(10);
                Long vmSnapshotId = StringUtils.isNotBlank(snapId) ? Long.valueOf(snapId) : null;

                if (createdTS != null) {
                    createdDate = DateUtil.parseDateString(s_gmtTimeZone, createdTS);
                }
                if (processed != null) {
                    processDate = DateUtil.parseDateString(s_gmtTimeZone, processed);
                }
                UsageVMSnapshotVO usageVMSnapshotVO = new UsageVMSnapshotVO(vId, zoneId, acctId, dId, vmId, doId, size, createdDate, processDate);
                usageVMSnapshotVO.setVmSnapshotId(vmSnapshotId);
                usageRecords.add(usageVMSnapshotVO);
            }
        } catch (Exception e) {
            txn.rollback();
            s_logger.warn("Error getting usage records", e);
        } finally {
            txn.close();
        }

        if (usageRecords.size() > 0)
            return usageRecords.get(0);
        return null;
    }
}
