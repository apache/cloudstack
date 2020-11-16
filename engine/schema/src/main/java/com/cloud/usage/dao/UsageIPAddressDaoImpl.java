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
import org.apache.log4j.Logger;
import org.springframework.stereotype.Component;

import com.cloud.usage.UsageIPAddressVO;
import com.cloud.utils.DateUtil;
import com.cloud.utils.db.GenericDaoBase;
import com.cloud.utils.db.TransactionLegacy;

@Component
public class UsageIPAddressDaoImpl extends GenericDaoBase<UsageIPAddressVO, Long> implements UsageIPAddressDao {
    public static final Logger s_logger = Logger.getLogger(UsageIPAddressDaoImpl.class.getName());

    protected static final String UPDATE_RELEASED = "UPDATE usage_ip_address SET released = ? WHERE account_id = ? AND public_ip_address = ? and released IS NULL";
    protected static final String GET_USAGE_RECORDS_BY_ACCOUNT =
        "SELECT id, account_id, domain_id, zone_id, public_ip_address, is_source_nat, is_system, assigned, released, is_hidden "
            + "FROM usage_ip_address "
            + "WHERE account_id = ? AND ((released IS NULL AND assigned <= ?) OR (assigned BETWEEN ? AND ?) OR "
            + "      (released BETWEEN ? AND ?) OR ((assigned <= ?) AND (released >= ?)))";
    protected static final String GET_USAGE_RECORDS_BY_DOMAIN =
        "SELECT id, account_id, domain_id, zone_id, public_ip_address, is_source_nat, is_system, assigned, released, is_hidden "
            + "FROM usage_ip_address "
            + "WHERE domain_id = ? AND ((released IS NULL AND assigned <= ?) OR (assigned BETWEEN ? AND ?) OR "
            + "      (released BETWEEN ? AND ?) OR ((assigned <= ?) AND (released >= ?)))";
    protected static final String GET_ALL_USAGE_RECORDS =
        "SELECT id, account_id, domain_id, zone_id, public_ip_address, is_source_nat, is_system, assigned, released, is_hidden "
            + "FROM usage_ip_address "
            + "WHERE (released IS NULL AND assigned <= ?) OR (assigned BETWEEN ? AND ?) OR "
            + "      (released BETWEEN ? AND ?) OR ((assigned <= ?) AND (released >= ?))";

    public UsageIPAddressDaoImpl() {
    }

    @Override
    public void update(UsageIPAddressVO usage) {
        TransactionLegacy txn = TransactionLegacy.open(TransactionLegacy.USAGE_DB);
        try {
            txn.start();
            if (usage.getReleased() != null) {
                try(PreparedStatement pstmt = txn.prepareStatement(UPDATE_RELEASED);) {
                    if (pstmt != null) {
                        pstmt.setString(1, DateUtil.getDateDisplayString(TimeZone.getTimeZone("GMT"), usage.getReleased()));
                        pstmt.setLong(2, usage.getAccountId());
                        pstmt.setString(3, usage.getAddress());
                        pstmt.executeUpdate();
                    }
                }catch(SQLException e)
                {
                   throw new CloudException("update:Exception:"+e.getMessage(), e);
                }
            }
            txn.commit();
        } catch (Exception e) {
            txn.rollback();
            s_logger.error("Error updating usageIPAddressVO:"+e.getMessage(), e);
        } finally {
            txn.close();
        }
    }

    @Override
    public List<UsageIPAddressVO> getUsageRecords(Long accountId, Long domainId, Date startDate, Date endDate) {
        List<UsageIPAddressVO> usageRecords = new ArrayList<UsageIPAddressVO>();

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

        TransactionLegacy txn = TransactionLegacy.open(TransactionLegacy.USAGE_DB);
        PreparedStatement pstmt = null;

        try {
            int i = 1;
            pstmt = txn.prepareAutoCloseStatement(sql);
            if (param1 != null) {
                pstmt.setLong(i++, param1);
            }
            pstmt.setString(i++, DateUtil.getDateDisplayString(TimeZone.getTimeZone("GMT"), endDate));
            pstmt.setString(i++, DateUtil.getDateDisplayString(TimeZone.getTimeZone("GMT"), startDate));
            pstmt.setString(i++, DateUtil.getDateDisplayString(TimeZone.getTimeZone("GMT"), endDate));
            pstmt.setString(i++, DateUtil.getDateDisplayString(TimeZone.getTimeZone("GMT"), startDate));
            pstmt.setString(i++, DateUtil.getDateDisplayString(TimeZone.getTimeZone("GMT"), endDate));
            pstmt.setString(i++, DateUtil.getDateDisplayString(TimeZone.getTimeZone("GMT"), startDate));
            pstmt.setString(i++, DateUtil.getDateDisplayString(TimeZone.getTimeZone("GMT"), endDate));

            ResultSet rs = pstmt.executeQuery();
            while (rs.next()) {
                //account_id, domain_id, zone_id, address, assigned, released
                Long id = Long.valueOf(rs.getLong(1));
                Long acctId = Long.valueOf(rs.getLong(2));
                Long dId = Long.valueOf(rs.getLong(3));
                Long zId = Long.valueOf(rs.getLong(4));
                String addr = rs.getString(5);
                Boolean isSourceNat = Boolean.valueOf(rs.getBoolean(6));
                Boolean isSystem = Boolean.valueOf(rs.getBoolean(7));
                Date assignedDate = null;
                Date releasedDate = null;
                String assignedTS = rs.getString(8);
                String releasedTS = rs.getString(9);
                Boolean isHidden = Boolean.valueOf(rs.getBoolean(10));

                if (assignedTS != null) {
                    assignedDate = DateUtil.parseDateString(s_gmtTimeZone, assignedTS);
                }
                if (releasedTS != null) {
                    releasedDate = DateUtil.parseDateString(s_gmtTimeZone, releasedTS);
                }

                usageRecords.add(new UsageIPAddressVO(id, acctId, dId, zId, addr, isSourceNat, isSystem, assignedDate, releasedDate, isHidden));
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
