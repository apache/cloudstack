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

import javax.ejb.Local;

import com.cloud.exception.CloudException;
import org.springframework.stereotype.Component;

import com.cloud.usage.UsageVolumeVO;
import com.cloud.utils.DateUtil;
import com.cloud.utils.db.GenericDaoBase;
import com.cloud.utils.db.TransactionLegacy;

@Component
@Local(value = {UsageVolumeDao.class})
public class UsageVolumeDaoImpl extends GenericDaoBase<UsageVolumeVO, Long> implements UsageVolumeDao {

    protected static final String REMOVE_BY_USERID_VOLID = "DELETE FROM usage_volume WHERE account_id = ? AND id = ?";
    protected static final String UPDATE_DELETED = "UPDATE usage_volume SET deleted = ? WHERE account_id = ? AND id = ? and deleted IS NULL";
    protected static final String GET_USAGE_RECORDS_BY_ACCOUNT = "SELECT id, zone_id, account_id, domain_id, disk_offering_id, template_id, size, created, deleted "
        + "FROM usage_volume " + "WHERE account_id = ? AND ((deleted IS NULL) OR (created BETWEEN ? AND ?) OR "
        + "      (deleted BETWEEN ? AND ?) OR ((created <= ?) AND (deleted >= ?)))";
    protected static final String GET_USAGE_RECORDS_BY_DOMAIN = "SELECT id, zone_id, account_id, domain_id, disk_offering_id, template_id, size, created, deleted "
        + "FROM usage_volume " + "WHERE domain_id = ? AND ((deleted IS NULL) OR (created BETWEEN ? AND ?) OR "
        + "      (deleted BETWEEN ? AND ?) OR ((created <= ?) AND (deleted >= ?)))";
    protected static final String GET_ALL_USAGE_RECORDS = "SELECT id, zone_id, account_id, domain_id, disk_offering_id, template_id, size, created, deleted "
        + "FROM usage_volume " + "WHERE (deleted IS NULL) OR (created BETWEEN ? AND ?) OR " + "      (deleted BETWEEN ? AND ?) OR ((created <= ?) AND (deleted >= ?))";

    public UsageVolumeDaoImpl() {
    }

    @Override
    public void removeBy(long accountId, long volId) {
        TransactionLegacy txn = TransactionLegacy.open(TransactionLegacy.USAGE_DB);
        try {
            txn.start();
            try(PreparedStatement pstmt = txn.prepareStatement(REMOVE_BY_USERID_VOLID);) {
                if (pstmt != null) {
                    pstmt.setLong(1, accountId);
                    pstmt.setLong(2, volId);
                    pstmt.executeUpdate();
                }
            }catch (SQLException e) {
                throw new CloudException("Error removing usageVolumeVO:"+e.getMessage(), e);
            }
            txn.commit();
        } catch (Exception e) {
            txn.rollback();
            logger.warn("Error removing usageVolumeVO:"+e.getMessage(), e);
        } finally {
            txn.close();
        }
    }

    @Override
    public void update(UsageVolumeVO usage) {
        TransactionLegacy txn = TransactionLegacy.open(TransactionLegacy.USAGE_DB);
        PreparedStatement pstmt = null;
        try {
            txn.start();
            if (usage.getDeleted() != null) {
                pstmt = txn.prepareAutoCloseStatement(UPDATE_DELETED);
                pstmt.setString(1, DateUtil.getDateDisplayString(TimeZone.getTimeZone("GMT"), usage.getDeleted()));
                pstmt.setLong(2, usage.getAccountId());
                pstmt.setLong(3, usage.getId());
                pstmt.executeUpdate();
            }
            txn.commit();
        } catch (Exception e) {
            txn.rollback();
            logger.warn("Error updating UsageVolumeVO", e);
        } finally {
            txn.close();
        }
    }

    @Override
    public List<UsageVolumeVO> getUsageRecords(Long accountId, Long domainId, Date startDate, Date endDate, boolean limit, int page) {
        List<UsageVolumeVO> usageRecords = new ArrayList<UsageVolumeVO>();

        Long param1 = null;
        String sql = null;
        if (accountId != null) {
            sql = GET_USAGE_RECORDS_BY_ACCOUNT;
            param1 = accountId;
        } else if (domainId != null) {
            sql = GET_USAGE_RECORDS_BY_DOMAIN;
            param1 = domainId;
        } else {
            sql = GET_ALL_USAGE_RECORDS;
        }

        if (limit) {
            int startIndex = 0;
            if (page > 0) {
                startIndex = 500 * (page - 1);
            }
            sql += " LIMIT " + startIndex + ",500";
        }

        TransactionLegacy txn = TransactionLegacy.open(TransactionLegacy.USAGE_DB);
        PreparedStatement pstmt = null;

        try {
            int i = 1;
            pstmt = txn.prepareAutoCloseStatement(sql);
            if (param1 != null) {
                pstmt.setLong(i++, param1);
            }
            pstmt.setString(i++, DateUtil.getDateDisplayString(TimeZone.getTimeZone("GMT"), startDate));
            pstmt.setString(i++, DateUtil.getDateDisplayString(TimeZone.getTimeZone("GMT"), endDate));
            pstmt.setString(i++, DateUtil.getDateDisplayString(TimeZone.getTimeZone("GMT"), startDate));
            pstmt.setString(i++, DateUtil.getDateDisplayString(TimeZone.getTimeZone("GMT"), endDate));
            pstmt.setString(i++, DateUtil.getDateDisplayString(TimeZone.getTimeZone("GMT"), startDate));
            pstmt.setString(i++, DateUtil.getDateDisplayString(TimeZone.getTimeZone("GMT"), endDate));

            ResultSet rs = pstmt.executeQuery();
            while (rs.next()) {
                //id, zoneId, account_id, domain_id, disk_offering_id, template_id created, deleted
                Long vId = Long.valueOf(rs.getLong(1));
                Long zoneId = Long.valueOf(rs.getLong(2));
                Long acctId = Long.valueOf(rs.getLong(3));
                Long dId = Long.valueOf(rs.getLong(4));
                Long doId = Long.valueOf(rs.getLong(5));
                if (doId == 0) {
                    doId = null;
                }
                Long tId = Long.valueOf(rs.getLong(6));
                if (tId == 0) {
                    tId = null;
                }
                long size = Long.valueOf(rs.getLong(7));
                Date createdDate = null;
                Date deletedDate = null;
                String createdTS = rs.getString(8);
                String deletedTS = rs.getString(9);

                if (createdTS != null) {
                    createdDate = DateUtil.parseDateString(s_gmtTimeZone, createdTS);
                }
                if (deletedTS != null) {
                    deletedDate = DateUtil.parseDateString(s_gmtTimeZone, deletedTS);
                }

                usageRecords.add(new UsageVolumeVO(vId, zoneId, acctId, dId, doId, tId, size, createdDate, deletedDate));
            }
        } catch (Exception e) {
            txn.rollback();
            logger.warn("Error getting usage records", e);
        } finally {
            txn.close();
        }

        return usageRecords;
    }
}
