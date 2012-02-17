/**
 * *  Copyright (C) 2012 Citrix Systems, Inc.  All rights reserved
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

import com.cloud.usage.UsageSecurityGroupVO;
import com.cloud.utils.DateUtil;
import com.cloud.utils.db.GenericDaoBase;
import com.cloud.utils.db.Transaction;

@Local(value={UsageSecurityGroupDao.class})
public class UsageSecurityGroupDaoImpl extends GenericDaoBase<UsageSecurityGroupVO, Long> implements UsageSecurityGroupDao {
	public static final Logger s_logger = Logger.getLogger(UsageSecurityGroupDaoImpl.class.getName());

	protected static final String UPDATE_DELETED = "UPDATE usage_security_group SET deleted = ? WHERE account_id = ? AND vm_instance_id = ? AND security_group_id = ? and deleted IS NULL";
    protected static final String GET_USAGE_RECORDS_BY_ACCOUNT = "SELECT zone_id, account_id, domain_id, vm_instance_id, security_group_id, created, deleted " +
                                                                 "FROM usage_security_group " +
                                                                 "WHERE account_id = ? AND ((deleted IS NULL) OR (created BETWEEN ? AND ?) OR " +
                                                                 "      (deleted BETWEEN ? AND ?) OR ((created <= ?) AND (deleted >= ?)))";
    protected static final String GET_USAGE_RECORDS_BY_DOMAIN = "SELECT zone_id, account_id, domain_id, vm_instance_id, security_group_id, created, deleted " +
                                                                "FROM usage_security_group " +
                                                                "WHERE domain_id = ? AND ((deleted IS NULL) OR (created BETWEEN ? AND ?) OR " +
                                                                "      (deleted BETWEEN ? AND ?) OR ((created <= ?) AND (deleted >= ?)))";
    protected static final String GET_ALL_USAGE_RECORDS = "SELECT zone_id, account_id, domain_id, vm_instance_id, security_group_id, created, deleted " +
                                                          "FROM usage_security_group " +
                                                          "WHERE (deleted IS NULL) OR (created BETWEEN ? AND ?) OR " +
                                                          "      (deleted BETWEEN ? AND ?) OR ((created <= ?) AND (deleted >= ?))";

	public UsageSecurityGroupDaoImpl() {}

	public void update(UsageSecurityGroupVO usage) {
	    Transaction txn = Transaction.open(Transaction.USAGE_DB);
		PreparedStatement pstmt = null;
		try {
		    txn.start();
			if (usage.getDeleted() != null) {
				pstmt = txn.prepareAutoCloseStatement(UPDATE_DELETED);
				pstmt.setString(1, DateUtil.getDateDisplayString(TimeZone.getTimeZone("GMT"), usage.getDeleted()));
				pstmt.setLong(2, usage.getAccountId());
				pstmt.setLong(3, usage.getVmInstanceId());
				pstmt.setLong(4, usage.getSecurityGroupId());
			}
			pstmt.executeUpdate();
			txn.commit();
		} catch (Exception e) {
			txn.rollback();
			s_logger.warn("Error updating UsageSecurityGroupVO", e);
		} finally {
		    txn.close();
		}
	}

    @Override
	public List<UsageSecurityGroupVO> getUsageRecords(Long accountId, Long domainId, Date startDate, Date endDate, boolean limit, int page) {
        List<UsageSecurityGroupVO> usageRecords = new ArrayList<UsageSecurityGroupVO>();

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
                startIndex = 500 * (page-1);
            }
            sql += " LIMIT " + startIndex + ",500";
        }

        Transaction txn = Transaction.open(Transaction.USAGE_DB);
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
                //zoneId, account_id, domain_id, vm_instance_id, security_group_id, created, deleted
            	Long zoneId = Long.valueOf(rs.getLong(1));
                Long acctId = Long.valueOf(rs.getLong(2));
                Long dId = Long.valueOf(rs.getLong(3));
                long vmId = Long.valueOf(rs.getLong(4));
                long sgId = Long.valueOf(rs.getLong(5));
                Date createdDate = null;
                Date deletedDate = null;
                String createdTS = rs.getString(6);
                String deletedTS = rs.getString(7);
                

                if (createdTS != null) {
                	createdDate = DateUtil.parseDateString(s_gmtTimeZone, createdTS);
                }
                if (deletedTS != null) {
                	deletedDate = DateUtil.parseDateString(s_gmtTimeZone, deletedTS);
                }

                usageRecords.add(new UsageSecurityGroupVO(zoneId, acctId, dId, vmId, sgId, createdDate, deletedDate));
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
