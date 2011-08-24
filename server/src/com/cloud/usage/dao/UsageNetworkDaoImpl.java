/**
 *  Copyright (C) 2011 Cloud.com, Inc.  All rights reserved.
 */

package com.cloud.usage.dao;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.HashMap;
import java.util.Map;

import javax.ejb.Local;

import org.apache.log4j.Logger;

import com.cloud.usage.UsageNetworkVO;
import com.cloud.utils.db.GenericDaoBase;
import com.cloud.utils.db.Transaction;

@Local(value={UsageNetworkDao.class})
public class UsageNetworkDaoImpl extends GenericDaoBase<UsageNetworkVO, Long> implements UsageNetworkDao {
	private static final Logger s_logger = Logger.getLogger(UsageVMInstanceDaoImpl.class.getName());
	private static final String SELECT_LATEST_STATS = "SELECT u.account_id, u.zone_id, u.host_id, u.host_type, u.network_id, u.bytes_sent, u.bytes_received, u.net_bytes_received, u.net_bytes_sent, " +
	                                                         "u.current_bytes_received, u.current_bytes_sent, u.event_time_millis " +
	                                                    "FROM cloud_usage.usage_network u INNER JOIN (SELECT netusage.account_id as acct_id, netusage.zone_id as z_id, max(netusage.event_time_millis) as max_date " +
	                                                                                                 "FROM cloud_usage.usage_network netusage " +
	                                                                                                 "GROUP BY netusage.account_id, netusage.zone_id " +
	                                                                                                 ") joinnet on u.account_id = joinnet.acct_id and u.zone_id = joinnet.z_id and u.event_time_millis = joinnet.max_date";
	private static final String DELETE_OLD_STATS = "DELETE FROM cloud_usage.usage_network WHERE event_time_millis < ?";

	public UsageNetworkDaoImpl() {
	}

	@Override
	public Map<String, UsageNetworkVO> getRecentNetworkStats() {
        Transaction txn = Transaction.open(Transaction.USAGE_DB);
        String sql = SELECT_LATEST_STATS;
        PreparedStatement pstmt = null;
        try {
            pstmt = txn.prepareAutoCloseStatement(sql);
            ResultSet rs = pstmt.executeQuery();
            Map<String, UsageNetworkVO> returnMap = new HashMap<String, UsageNetworkVO>();
            while (rs.next()) {
                long accountId = rs.getLong(1);
                long zoneId = rs.getLong(2);
                Long hostId = rs.getLong(3);
                String hostType = rs.getString(4);
                Long networkId = rs.getLong(5);
                long bytesSent = rs.getLong(6);
                long bytesReceived = rs.getLong(7);
                long netBytesReceived = rs.getLong(8);
                long netBytesSent = rs.getLong(9);
                long currentBytesReceived = rs.getLong(10);
                long currentBytesSent = rs.getLong(11);
                long eventTimeMillis = rs.getLong(12);
                if(hostId != 0){
                    returnMap.put(zoneId + "-" + accountId+ "-Host-" + hostId, new UsageNetworkVO(accountId, zoneId, hostId, hostType, networkId, bytesSent, bytesReceived, netBytesReceived, netBytesSent, 
                            currentBytesReceived, currentBytesSent, eventTimeMillis));
                } else {
                    returnMap.put(zoneId + "-" + accountId, new UsageNetworkVO(accountId, zoneId, hostId, hostType, networkId, bytesSent, bytesReceived, netBytesReceived, netBytesSent, currentBytesReceived, 
                            currentBytesSent, eventTimeMillis));
                }
            }
            return returnMap;
        } catch (Exception ex) {
            s_logger.error("error getting recent usage network stats", ex);
        } finally {
            txn.close();
        }
        return null;
	}

    @Override
	public void deleteOldStats(long maxEventTime) {
        Transaction txn = Transaction.currentTxn();
        String sql = DELETE_OLD_STATS;
        PreparedStatement pstmt = null;
        try {
            txn.start();
            pstmt = txn.prepareAutoCloseStatement(sql);
            pstmt.setLong(1, maxEventTime);
            pstmt.executeUpdate();
            txn.commit();
        } catch (Exception ex) {
            txn.rollback();
            s_logger.error("error deleting old usage network stats", ex);
        }
	}
}
