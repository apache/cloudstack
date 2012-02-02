/**
 * *  Copyright (C) 2011 Citrix Systems, Inc.  All rights reserved
*
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

package com.cloud.usage.dao;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.TimeZone;

import javax.ejb.Local;

import org.apache.log4j.Logger;

import com.cloud.usage.UsageIPAddressVO;
import com.cloud.user.Account;
import com.cloud.utils.DateUtil;
import com.cloud.utils.db.GenericDaoBase;
import com.cloud.utils.db.Transaction;

@Local(value={UsageIPAddressDao.class})
public class UsageIPAddressDaoImpl extends GenericDaoBase<UsageIPAddressVO, Long> implements UsageIPAddressDao {
	public static final Logger s_logger = Logger.getLogger(UsageIPAddressDaoImpl.class.getName());

	protected static final String UPDATE_RELEASED = "UPDATE usage_ip_address SET released = ? WHERE account_id = ? AND public_ip_address = ? and released IS NULL";
    protected static final String GET_USAGE_RECORDS_BY_ACCOUNT = "SELECT id, account_id, domain_id, zone_id, public_ip_address, is_source_nat, is_elastic, assigned, released " +
                                                                 "FROM usage_ip_address " +
                                                                 "WHERE account_id = ? AND ((released IS NULL AND assigned <= ?) OR (assigned BETWEEN ? AND ?) OR " +
                                                                 "      (released BETWEEN ? AND ?) OR ((assigned <= ?) AND (released >= ?)))";
    protected static final String GET_USAGE_RECORDS_BY_DOMAIN = "SELECT id, account_id, domain_id, zone_id, public_ip_address, is_source_nat, is_elastic, assigned, released " +
                                                                "FROM usage_ip_address " +
                                                                "WHERE domain_id = ? AND ((released IS NULL AND assigned <= ?) OR (assigned BETWEEN ? AND ?) OR " +
                                                                "      (released BETWEEN ? AND ?) OR ((assigned <= ?) AND (released >= ?)))";
    protected static final String GET_ALL_USAGE_RECORDS = "SELECT id, account_id, domain_id, zone_id, public_ip_address, is_source_nat, is_elastic, assigned, released " +
                                                          "FROM usage_ip_address " +
                                                          "WHERE (released IS NULL AND assigned <= ?) OR (assigned BETWEEN ? AND ?) OR " +
                                                          "      (released BETWEEN ? AND ?) OR ((assigned <= ?) AND (released >= ?))";

	public UsageIPAddressDaoImpl() {}

	public void update(UsageIPAddressVO usage) {
	    Transaction txn = Transaction.open(Transaction.USAGE_DB);
		PreparedStatement pstmt = null;
		try {
		    txn.start();
			if (usage.getReleased() != null) {
				pstmt = txn.prepareAutoCloseStatement(UPDATE_RELEASED);
				pstmt.setString(1, DateUtil.getDateDisplayString(TimeZone.getTimeZone("GMT"), usage.getReleased()));
				pstmt.setLong(2, usage.getAccountId());
				pstmt.setString(3, usage.getAddress());
			}
			pstmt.executeUpdate();
			txn.commit();
		} catch (Exception e) {
			txn.rollback();
			s_logger.warn("Error updating usageIPAddressVO", e);
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

        Transaction txn = Transaction.open(Transaction.USAGE_DB);
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
                Boolean isElastic = Boolean.valueOf(rs.getBoolean(7));
                Date assignedDate = null;
                Date releasedDate = null;
                String assignedTS = rs.getString(8);
                String releasedTS = rs.getString(9);

                if (assignedTS != null) {
                    assignedDate = DateUtil.parseDateString(s_gmtTimeZone, assignedTS);
                }
                if (releasedTS != null) {
                    releasedDate = DateUtil.parseDateString(s_gmtTimeZone, releasedTS);
                }

                usageRecords.add(new UsageIPAddressVO(id, acctId, dId, zId, addr, isSourceNat, isElastic, assignedDate, releasedDate));
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
