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

import org.apache.cloudstack.backup.Backup;
import org.apache.log4j.Logger;
import org.springframework.stereotype.Component;

import com.cloud.usage.UsageBackupVO;
import com.cloud.utils.DateUtil;
import com.cloud.utils.db.GenericDaoBase;
import com.cloud.utils.db.QueryBuilder;
import com.cloud.utils.db.SearchCriteria;
import com.cloud.utils.db.Transaction;
import com.cloud.utils.db.TransactionCallback;
import com.cloud.utils.db.TransactionLegacy;
import com.cloud.utils.db.TransactionStatus;
import com.cloud.vm.VirtualMachine;

@Component
public class UsageBackupDaoImpl extends GenericDaoBase<UsageBackupVO, Long> implements UsageBackupDao {
    public static final Logger LOGGER = Logger.getLogger(UsageBackupDaoImpl.class);
    protected static final String GET_USAGE_RECORDS_BY_ACCOUNT = "SELECT id, zone_id, account_id, domain_id, vm_id, backup_offering_id, size, protected_size, created, removed FROM cloud_usage.usage_backup WHERE " +
            " account_id = ? AND ((removed IS NULL AND created <= ?) OR (created BETWEEN ? AND ?) OR (removed BETWEEN ? AND ?) " +
            " OR ((created <= ?) AND (removed >= ?)))";

    @Override
    public void updateMetrics(final VirtualMachine vm, Backup.Metric metric) {
        boolean result = Transaction.execute(TransactionLegacy.USAGE_DB, new TransactionCallback<Boolean>() {
            @Override
            public Boolean doInTransaction(final TransactionStatus status) {
                final QueryBuilder<UsageBackupVO> qb = QueryBuilder.create(UsageBackupVO.class);
                qb.and(qb.entity().getVmId(), SearchCriteria.Op.EQ, vm.getId());
                final UsageBackupVO entry = findOneBy(qb.create());
                if (entry == null) {
                    return false;
                }
                entry.setSize(metric.getBackupSize());
                entry.setProtectedSize(metric.getDataSize());
                return update(entry.getId(), entry);
            }
        });
        if (!result) {
            LOGGER.trace("Failed to update backup metrics for VM ID: " + vm.getId());
        }
    }

    @Override
    public void removeUsage(Long accountId, Long zoneId, Long vmId) {
        boolean result = Transaction.execute(TransactionLegacy.USAGE_DB, new TransactionCallback<Boolean>() {
            @Override
            public Boolean doInTransaction(final TransactionStatus status) {
                final QueryBuilder<UsageBackupVO> qb = QueryBuilder.create(UsageBackupVO.class);
                qb.and(qb.entity().getAccountId(), SearchCriteria.Op.EQ, accountId);
                qb.and(qb.entity().getZoneId(), SearchCriteria.Op.EQ, zoneId);
                qb.and(qb.entity().getVmId(), SearchCriteria.Op.EQ, vmId);
                final UsageBackupVO entry = findOneBy(qb.create());
                return remove(qb.create()) > 0;
            }
        });
        if (!result) {
            LOGGER.warn("Failed to remove usage entry for backup of VM ID: " + vmId);
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
            LOGGER.warn("Error getting VM backup usage records", e);
        } finally {
            txn.close();
        }

        return usageRecords;
    }
}
