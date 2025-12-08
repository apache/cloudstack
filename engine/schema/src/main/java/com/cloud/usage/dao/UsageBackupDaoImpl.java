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
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.TimeZone;

import org.springframework.stereotype.Component;

import com.cloud.exception.CloudException;
import com.cloud.usage.UsageBackupVO;
import com.cloud.utils.DateUtil;
import com.cloud.utils.db.GenericDaoBase;
import com.cloud.utils.db.SearchCriteria;
import com.cloud.utils.db.TransactionLegacy;

@Component
public class UsageBackupDaoImpl extends GenericDaoBase<UsageBackupVO, Long> implements UsageBackupDao {
    protected static final String UPDATE_DELETED = "UPDATE usage_backup SET removed = ? WHERE account_id = ? AND vm_id = ? and removed IS NULL";
    protected static final String GET_USAGE_RECORDS_BY_ACCOUNT = "SELECT id, zone_id, account_id, domain_id, vm_id, backup_offering_id, size, protected_size, created, removed FROM usage_backup WHERE " +
            " account_id = ? AND ((removed IS NULL AND created <= ?) OR (created BETWEEN ? AND ?) OR (removed BETWEEN ? AND ?) " +
            " OR ((created <= ?) AND (removed >= ?)))";

    @Override
    public void updateMetrics(final Long vmId, final Long size, final Long virtualSize) {
        try (TransactionLegacy txn = TransactionLegacy.open(TransactionLegacy.USAGE_DB)) {
            SearchCriteria<UsageBackupVO> sc = this.createSearchCriteria();
            sc.addAnd("vmId", SearchCriteria.Op.EQ, vmId);
            UsageBackupVO vo = findOneBy(sc);
            if (vo != null) {
                vo.setSize(size);
                vo.setProtectedSize(virtualSize);
                update(vo.getId(), vo);
            }
        } catch (final Exception e) {
            logger.error("Error updating backup metrics: " + e.getMessage(), e);
        }
    }

    @Override
    public void removeUsage(Long accountId, Long vmId, Date eventDate) {
        TransactionLegacy txn = TransactionLegacy.open(TransactionLegacy.USAGE_DB);
        try {
            txn.start();
            try (PreparedStatement pstmt = txn.prepareStatement(UPDATE_DELETED);) {
                if (pstmt != null) {
                    pstmt.setString(1, DateUtil.getDateDisplayString(TimeZone.getTimeZone("GMT"), eventDate));
                    pstmt.setLong(2, accountId);
                    pstmt.setLong(3, vmId);
                    pstmt.executeUpdate();
                }
            } catch (SQLException e) {
                logger.error("Error removing UsageBackupVO: " + e.getMessage(), e);
                throw new CloudException("Remove backup usage exception: " + e.getMessage(), e);
            }
            txn.commit();
        } catch (Exception e) {
            txn.rollback();
            logger.error("Exception caught while removing UsageBackupVO: " + e.getMessage(), e);
        } finally {
            txn.close();
        }
    }

    @Override
    public List<UsageBackupVO> getUsageRecords(Long accountId, Date startDate, Date endDate) {
        List<UsageBackupVO> usageRecords = new ArrayList<UsageBackupVO>();
        TransactionLegacy txn = TransactionLegacy.open(TransactionLegacy.USAGE_DB);
        PreparedStatement pstmt;
        try {
            int i = 1;
            pstmt = txn.prepareAutoCloseStatement(GET_USAGE_RECORDS_BY_ACCOUNT);
            pstmt.setLong(i++, accountId);

            pstmt.setString(i++, DateUtil.getDateDisplayString(TimeZone.getTimeZone("GMT"), endDate));
            pstmt.setString(i++, DateUtil.getDateDisplayString(TimeZone.getTimeZone("GMT"), startDate));
            pstmt.setString(i++, DateUtil.getDateDisplayString(TimeZone.getTimeZone("GMT"), endDate));
            pstmt.setString(i++, DateUtil.getDateDisplayString(TimeZone.getTimeZone("GMT"), startDate));
            pstmt.setString(i++, DateUtil.getDateDisplayString(TimeZone.getTimeZone("GMT"), endDate));
            pstmt.setString(i++, DateUtil.getDateDisplayString(TimeZone.getTimeZone("GMT"), startDate));
            pstmt.setString(i++, DateUtil.getDateDisplayString(TimeZone.getTimeZone("GMT"), endDate));

            ResultSet rs = pstmt.executeQuery();
            while (rs.next()) {
                //id, zone_id, account_id, domain_id, vm_id, disk_offering_id, size, created, processed
                Long id = Long.valueOf(rs.getLong(1));
                Long zoneId = Long.valueOf(rs.getLong(2));
                Long acctId = Long.valueOf(rs.getLong(3));
                Long domId = Long.valueOf(rs.getLong(4));
                Long vmId = Long.valueOf(rs.getLong(5));
                Long backupOfferingId = Long.valueOf(rs.getLong(6));
                Long size = Long.valueOf(rs.getLong(7));
                Long pSize = Long.valueOf(rs.getLong(8));
                Date createdDate = null;
                Date removedDate = null;
                String createdTS = rs.getString(9);
                String removedTS = rs.getString(10);

                if (createdTS != null) {
                    createdDate = DateUtil.parseDateString(s_gmtTimeZone, createdTS);
                }
                if (removedTS != null) {
                    removedDate = DateUtil.parseDateString(s_gmtTimeZone, removedTS);
                }
                usageRecords.add(new UsageBackupVO(id, zoneId, acctId, domId, vmId, backupOfferingId, size, pSize, createdDate, removedDate));
            }
        } catch (Exception e) {
            txn.rollback();
            logger.warn("Error getting VM backup usage records", e);
        } finally {
            txn.close();
        }

        return usageRecords;
    }
}
