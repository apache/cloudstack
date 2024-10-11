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


import com.cloud.exception.CloudException;
import org.springframework.stereotype.Component;

import com.cloud.usage.UsageNetworkOfferingVO;
import com.cloud.utils.DateUtil;
import com.cloud.utils.db.GenericDaoBase;
import com.cloud.utils.db.TransactionLegacy;

@Component
public class UsageNetworkOfferingDaoImpl extends GenericDaoBase<UsageNetworkOfferingVO, Long> implements UsageNetworkOfferingDao {

    protected static final String UPDATE_DELETED =
        "UPDATE usage_network_offering SET deleted = ? WHERE account_id = ? AND vm_instance_id = ? AND network_offering_id = ? and deleted IS NULL";
    protected static final String GET_USAGE_RECORDS_BY_ACCOUNT =
        "SELECT zone_id, account_id, domain_id, vm_instance_id, network_offering_id, nic_id, is_default, created, deleted " + "FROM usage_network_offering "
            + "WHERE account_id = ? AND ((deleted IS NULL) OR (created BETWEEN ? AND ?) OR " + "      (deleted BETWEEN ? AND ?) OR ((created <= ?) AND (deleted >= ?)))";
    protected static final String GET_USAGE_RECORDS_BY_DOMAIN =
        "SELECT zone_id, account_id, domain_id, vm_instance_id, network_offering_id, nic_id, is_default, created, deleted " + "FROM usage_network_offering "
            + "WHERE domain_id = ? AND ((deleted IS NULL) OR (created BETWEEN ? AND ?) OR " + "      (deleted BETWEEN ? AND ?) OR ((created <= ?) AND (deleted >= ?)))";
    protected static final String GET_ALL_USAGE_RECORDS =
        "SELECT zone_id, account_id, domain_id, vm_instance_id, network_offering_id, nic_id, is_default, created, deleted " + "FROM usage_network_offering "
            + "WHERE (deleted IS NULL) OR (created BETWEEN ? AND ?) OR " + "      (deleted BETWEEN ? AND ?) OR ((created <= ?) AND (deleted >= ?))";

    public UsageNetworkOfferingDaoImpl() {
    }

    @Override
    public void update(UsageNetworkOfferingVO usage) {
        TransactionLegacy txn = TransactionLegacy.open(TransactionLegacy.USAGE_DB);
        try {
            txn.start();
            if (usage.getDeleted() != null) {
                try(PreparedStatement pstmt = txn.prepareStatement(UPDATE_DELETED);) {
                    if (pstmt != null) {
                        pstmt.setString(1, DateUtil.getDateDisplayString(TimeZone.getTimeZone("GMT"), usage.getDeleted()));
                        pstmt.setLong(2, usage.getAccountId());
                        pstmt.setLong(3, usage.getVmInstanceId());
                        pstmt.setLong(4, usage.getNetworkOfferingId());
                        pstmt.executeUpdate();
                    }
                  }catch (SQLException e) {
                    throw new CloudException("Error updating UsageNetworkOfferingVO:"+e.getMessage(), e);
                }
            }
            txn.commit();
        } catch (Exception e) {
            txn.rollback();
            logger.warn("Error updating UsageNetworkOfferingVO:"+e.getMessage(), e);
        } finally {
            txn.close();
        }
    }

    @Override
    public List<UsageNetworkOfferingVO> getUsageRecords(Long accountId, Long domainId, Date startDate, Date endDate, boolean limit, int page) {
        List<UsageNetworkOfferingVO> usageRecords = new ArrayList<UsageNetworkOfferingVO>();

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
                //zoneId, account_id, domain_id, vm_instance_id, network_offering_id, nic_id, is_default, created, deleted
                Long zoneId = Long.valueOf(rs.getLong(1));
                Long acctId = Long.valueOf(rs.getLong(2));
                Long dId = Long.valueOf(rs.getLong(3));
                long vmId = Long.valueOf(rs.getLong(4));
                long noId = Long.valueOf(rs.getLong(5));
                long nicId = Long.valueOf(rs.getLong(6));
                boolean isDefault = Boolean.valueOf(rs.getBoolean(7));
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

                usageRecords.add(new UsageNetworkOfferingVO(zoneId, acctId, dId, vmId, noId, nicId, isDefault, createdDate, deletedDate));
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
