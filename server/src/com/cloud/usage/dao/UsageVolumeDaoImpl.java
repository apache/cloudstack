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

import com.cloud.usage.UsageVolumeVO;
import com.cloud.utils.DateUtil;
import com.cloud.utils.db.GenericDaoBase;
import com.cloud.utils.db.Transaction;

@Local(value={UsageVolumeDao.class})
public class UsageVolumeDaoImpl extends GenericDaoBase<UsageVolumeVO, Long> implements UsageVolumeDao {
	public static final Logger s_logger = Logger.getLogger(UsageVolumeDaoImpl.class.getName());

	protected static final String REMOVE_BY_USERID_VOLID = "DELETE FROM usage_volume WHERE account_id = ? AND id = ?";
	protected static final String UPDATE_DELETED = "UPDATE usage_volume SET deleted = ? WHERE account_id = ? AND id = ? and deleted IS NULL";
    protected static final String GET_USAGE_RECORDS_BY_ACCOUNT = "SELECT id, zone_id, account_id, domain_id, disk_offering_id, template_id, size, created, deleted " +
                                                                 "FROM usage_volume " +
                                                                 "WHERE account_id = ? AND ((deleted IS NULL) OR (created BETWEEN ? AND ?) OR " +
                                                                 "      (deleted BETWEEN ? AND ?) OR ((created <= ?) AND (deleted >= ?)))";
    protected static final String GET_USAGE_RECORDS_BY_DOMAIN = "SELECT id, zone_id, account_id, domain_id, disk_offering_id, template_id, size, created, deleted " +
                                                                "FROM usage_volume " +
                                                                "WHERE domain_id = ? AND ((deleted IS NULL) OR (created BETWEEN ? AND ?) OR " +
                                                                "      (deleted BETWEEN ? AND ?) OR ((created <= ?) AND (deleted >= ?)))";
    protected static final String GET_ALL_USAGE_RECORDS = "SELECT id, zone_id, account_id, domain_id, disk_offering_id, template_id, size, created, deleted " +
                                                          "FROM usage_volume " +
                                                          "WHERE (deleted IS NULL) OR (created BETWEEN ? AND ?) OR " +
                                                          "      (deleted BETWEEN ? AND ?) OR ((created <= ?) AND (deleted >= ?))";

	public UsageVolumeDaoImpl() {}

	public void removeBy(long accountId, long volId) {
	    Transaction txn = Transaction.open(Transaction.USAGE_DB);
		PreparedStatement pstmt = null;
		try {
		    txn.start();
			String sql = REMOVE_BY_USERID_VOLID;
			pstmt = txn.prepareAutoCloseStatement(sql);
			pstmt.setLong(1, accountId);
			pstmt.setLong(2, volId);
			pstmt.executeUpdate();
			txn.commit();
		} catch (Exception e) {
			txn.rollback();
			s_logger.warn("Error removing usageVolumeVO", e);
		} finally {
		    txn.close();
		}
	}

	public void update(UsageVolumeVO usage) {
	    Transaction txn = Transaction.open(Transaction.USAGE_DB);
		PreparedStatement pstmt = null;
		try {
		    txn.start();
			if (usage.getDeleted() != null) {
				pstmt = txn.prepareAutoCloseStatement(UPDATE_DELETED);
				pstmt.setString(1, DateUtil.getDateDisplayString(TimeZone.getTimeZone("GMT"), usage.getDeleted()));
				pstmt.setLong(2, usage.getAccountId());
				pstmt.setLong(3, usage.getId());
			}
			pstmt.executeUpdate();
			txn.commit();
		} catch (Exception e) {
			txn.rollback();
			s_logger.warn("Error updating UsageVolumeVO", e);
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
                //id, zoneId, account_id, domain_id, disk_offering_id, template_id created, deleted
            	Long vId = Long.valueOf(rs.getLong(1));
            	Long zoneId = Long.valueOf(rs.getLong(2));
                Long acctId = Long.valueOf(rs.getLong(3));
                Long dId = Long.valueOf(rs.getLong(4));
                Long doId = Long.valueOf(rs.getLong(5));
                if(doId == 0){
                    doId = null;
                }
                Long tId = Long.valueOf(rs.getLong(6));
                if(tId == 0){
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
            s_logger.warn("Error getting usage records", e);
        } finally {
            txn.close();
        }

        return usageRecords;
	}
}
