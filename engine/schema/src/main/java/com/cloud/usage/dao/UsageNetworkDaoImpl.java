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
import java.util.HashMap;
import java.util.List;
import java.util.Map;


import org.apache.log4j.Logger;
import org.springframework.stereotype.Component;

import com.cloud.usage.UsageNetworkVO;
import com.cloud.utils.db.GenericDaoBase;
import com.cloud.utils.db.TransactionLegacy;
import com.cloud.utils.exception.CloudRuntimeException;

@Component
public class UsageNetworkDaoImpl extends GenericDaoBase<UsageNetworkVO, Long> implements UsageNetworkDao {
    private static final Logger s_logger = Logger.getLogger(UsageNetworkDaoImpl.class.getName());
    private static final String SELECT_LATEST_STATS =
        "SELECT u.account_id, u.zone_id, u.host_id, u.host_type, u.network_id, u.bytes_sent, u.bytes_received, u.agg_bytes_received, u.agg_bytes_sent, u.event_time_millis "
            + "FROM cloud_usage.usage_network u INNER JOIN (SELECT netusage.account_id as acct_id, netusage.zone_id as z_id, max(netusage.event_time_millis) as max_date "
            + "FROM cloud_usage.usage_network netusage " + "GROUP BY netusage.account_id, netusage.zone_id "
            + ") joinnet on u.account_id = joinnet.acct_id and u.zone_id = joinnet.z_id and u.event_time_millis = joinnet.max_date";
    private static final String DELETE_OLD_STATS = "DELETE FROM cloud_usage.usage_network WHERE event_time_millis < ?";

    private static final String INSERT_USAGE_NETWORK =
        "INSERT INTO cloud_usage.usage_network (account_id, zone_id, host_id, host_type, network_id, bytes_sent, bytes_received, agg_bytes_received, agg_bytes_sent, event_time_millis) VALUES (?,?,?,?,?,?,?,?,?,?)";

    public UsageNetworkDaoImpl() {
    }

    @Override
    public Map<String, UsageNetworkVO> getRecentNetworkStats() {
        TransactionLegacy txn = TransactionLegacy.open(TransactionLegacy.USAGE_DB);
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
                long aggBytesReceived = rs.getLong(8);
                long aggBytesSent = rs.getLong(9);
                long eventTimeMillis = rs.getLong(10);
                if (hostId != 0) {
                    returnMap.put(zoneId + "-" + accountId + "-Host-" + hostId + "-Network-" + networkId, new UsageNetworkVO(accountId, zoneId, hostId, hostType, networkId, bytesSent,
                        bytesReceived, aggBytesReceived, aggBytesSent, eventTimeMillis));
                } else {
                    returnMap.put(zoneId + "-" + accountId, new UsageNetworkVO(accountId, zoneId, hostId, hostType, networkId, bytesSent, bytesReceived,
                        aggBytesReceived, aggBytesSent, eventTimeMillis));
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
        TransactionLegacy txn = TransactionLegacy.currentTxn();
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

    @Override
    public void saveUsageNetworks(List<UsageNetworkVO> usageNetworks) {
        TransactionLegacy txn = TransactionLegacy.currentTxn();
        try {
            txn.start();
            String sql = INSERT_USAGE_NETWORK;
            PreparedStatement pstmt = null;
            pstmt = txn.prepareAutoCloseStatement(sql); // in reality I just want CLOUD_USAGE dataSource connection
            for (UsageNetworkVO usageNetwork : usageNetworks) {
                pstmt.setLong(1, usageNetwork.getAccountId());
                pstmt.setLong(2, usageNetwork.getZoneId());
                pstmt.setLong(3, usageNetwork.getHostId());
                pstmt.setString(4, usageNetwork.getHostType());
                pstmt.setLong(5, usageNetwork.getNetworkId());
                pstmt.setLong(6, usageNetwork.getBytesSent());
                pstmt.setLong(7, usageNetwork.getBytesReceived());
                pstmt.setLong(8, usageNetwork.getAggBytesReceived());
                pstmt.setLong(9, usageNetwork.getAggBytesSent());
                pstmt.setLong(10, usageNetwork.getEventTimeMillis());
                pstmt.addBatch();
            }
            pstmt.executeBatch();
            txn.commit();
        } catch (Exception ex) {
            txn.rollback();
            s_logger.error("error saving usage_network to cloud_usage db", ex);
            throw new CloudRuntimeException(ex.getMessage());
        }
    }
}
