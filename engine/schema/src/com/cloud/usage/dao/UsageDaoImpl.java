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

import com.cloud.usage.UsageVO;
import com.cloud.user.AccountVO;
import com.cloud.user.UserStatisticsVO;
import com.cloud.user.VmDiskStatisticsVO;
import com.cloud.utils.DateUtil;
import com.cloud.utils.Pair;
import com.cloud.utils.db.Filter;
import com.cloud.utils.db.GenericDaoBase;
import com.cloud.utils.db.SearchCriteria;
import com.cloud.utils.db.TransactionLegacy;
import com.cloud.utils.exception.CloudRuntimeException;
import org.springframework.stereotype.Component;

import javax.ejb.Local;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Types;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.TimeZone;

@Component
@Local(value = {UsageDao.class})
public class UsageDaoImpl extends GenericDaoBase<UsageVO, Long> implements UsageDao {
    private static final String DELETE_ALL = "DELETE FROM cloud_usage";
    private static final String DELETE_ALL_BY_ACCOUNTID = "DELETE FROM cloud_usage WHERE account_id = ?";
    private static final String DELETE_ALL_BY_INTERVAL = "DELETE FROM cloud_usage WHERE end_date < DATE_SUB(CURRENT_DATE(), INTERVAL ? DAY)";
    private static final String INSERT_ACCOUNT = "INSERT INTO cloud_usage.account (id, account_name, type, domain_id, removed, cleanup_needed) VALUES (?,?,?,?,?,?)";
    private static final String INSERT_USER_STATS =
            "INSERT INTO cloud_usage.user_statistics (id, data_center_id, account_id, public_ip_address, device_id, device_type, network_id, net_bytes_received,"
                    + " net_bytes_sent, current_bytes_received, current_bytes_sent, agg_bytes_received, agg_bytes_sent) VALUES (?,?,?,?,?,?,?,?,?,?, ?, ?, ?)";

    private static final String UPDATE_ACCOUNT = "UPDATE cloud_usage.account SET account_name=?, removed=? WHERE id=?";
    private static final String UPDATE_USER_STATS =
            "UPDATE cloud_usage.user_statistics SET net_bytes_received=?, net_bytes_sent=?, current_bytes_received=?, current_bytes_sent=?, agg_bytes_received=?, agg_bytes_sent=? WHERE id=?";

    private static final String GET_LAST_ACCOUNT = "SELECT id FROM cloud_usage.account ORDER BY id DESC LIMIT 1";
    private static final String GET_LAST_USER_STATS = "SELECT id FROM cloud_usage.user_statistics ORDER BY id DESC LIMIT 1";
    private static final String GET_PUBLIC_TEMPLATES_BY_ACCOUNTID = "SELECT id FROM cloud.vm_template WHERE account_id = ? AND public = '1' AND removed IS NULL";

    private static final String GET_LAST_VM_DISK_STATS = "SELECT id FROM cloud_usage.vm_disk_statistics ORDER BY id DESC LIMIT 1";
    private static final String INSERT_VM_DISK_STATS =
            "INSERT INTO cloud_usage.vm_disk_statistics (id, data_center_id, account_id, vm_id, volume_id, net_io_read, net_io_write, current_io_read, "
                    + "current_io_write, agg_io_read, agg_io_write, net_bytes_read, net_bytes_write, current_bytes_read, current_bytes_write, agg_bytes_read, agg_bytes_write) "
                    + " VALUES (?,?,?,?,?,?,?,?,?,?, ?, ?, ?, ?,?, ?, ?)";
    private static final String UPDATE_VM_DISK_STATS =
            "UPDATE cloud_usage.vm_disk_statistics SET net_io_read=?, net_io_write=?, current_io_read=?, current_io_write=?, agg_io_read=?, agg_io_write=?, "
                    + "net_bytes_read=?, net_bytes_write=?, current_bytes_read=?, current_bytes_write=?, agg_bytes_read=?, agg_bytes_write=?  WHERE id=?";
    private static final String INSERT_USAGE_RECORDS = "INSERT INTO cloud_usage.cloud_usage (zone_id, account_id, domain_id, description, usage_display, "
            +
            "usage_type, raw_usage, vm_instance_id, vm_name, offering_id, template_id, "
            + "usage_id, type, size, network_id, start_date, end_date, virtual_size) VALUES (?,?,?,?,?,?,?,?,?, ?, ?, ?,?,?,?,?,?,?)";

    protected final static TimeZone s_gmtTimeZone = TimeZone.getTimeZone("GMT");

    public UsageDaoImpl() {
    }

    @Override
    public void deleteRecordsForAccount(Long accountId) {
        String sql = ((accountId == null) ? DELETE_ALL : DELETE_ALL_BY_ACCOUNTID);
        TransactionLegacy txn = TransactionLegacy.open(TransactionLegacy.USAGE_DB);
        PreparedStatement pstmt = null;
        try {
            txn.start();
            pstmt = txn.prepareAutoCloseStatement(sql);
            if (accountId != null) {
                pstmt.setLong(1, accountId.longValue());
            }
            pstmt.executeUpdate();
            txn.commit();
        } catch (Exception ex) {
            txn.rollback();
            logger.error("error retrieving usage vm instances for account id: " + accountId);
        } finally {
            txn.close();
        }
    }

    @Override
    public Pair<List<UsageVO>, Integer> searchAndCountAllRecords(SearchCriteria<UsageVO> sc, Filter filter) {
        return listAndCountIncludingRemovedBy(sc, filter);
    }

    @Override
    public void saveAccounts(List<AccountVO> accounts) {
        TransactionLegacy txn = TransactionLegacy.currentTxn();
        try {
            txn.start();
            String sql = INSERT_ACCOUNT;
            PreparedStatement pstmt = null;
            pstmt = txn.prepareAutoCloseStatement(sql); // in reality I just want CLOUD_USAGE dataSource connection
            for (AccountVO acct : accounts) {
                pstmt.setLong(1, acct.getId());
                pstmt.setString(2, acct.getAccountName());
                pstmt.setShort(3, acct.getType());
                pstmt.setLong(4, acct.getDomainId());

                Date removed = acct.getRemoved();
                if (removed == null) {
                    pstmt.setString(5, null);
                } else {
                    pstmt.setString(5, DateUtil.getDateDisplayString(TimeZone.getTimeZone("GMT"), acct.getRemoved()));
                }

                pstmt.setBoolean(6, acct.getNeedsCleanup());

                pstmt.addBatch();
            }
            pstmt.executeBatch();
            txn.commit();
        } catch (Exception ex) {
            txn.rollback();
            logger.error("error saving account to cloud_usage db", ex);
            throw new CloudRuntimeException(ex.getMessage());
        }
    }

    @Override
    public void updateAccounts(List<AccountVO> accounts) {
        TransactionLegacy txn = TransactionLegacy.currentTxn();
        try {
            txn.start();
            String sql = UPDATE_ACCOUNT;
            PreparedStatement pstmt = null;
            pstmt = txn.prepareAutoCloseStatement(sql); // in reality I just want CLOUD_USAGE dataSource connection
            for (AccountVO acct : accounts) {
                pstmt.setString(1, acct.getAccountName());

                Date removed = acct.getRemoved();
                if (removed == null) {
                    pstmt.setString(2, null);
                } else {
                    pstmt.setString(2, DateUtil.getDateDisplayString(TimeZone.getTimeZone("GMT"), acct.getRemoved()));
                }

                pstmt.setLong(3, acct.getId());
                pstmt.addBatch();
            }
            pstmt.executeBatch();
            txn.commit();
        } catch (Exception ex) {
            txn.rollback();
            logger.error("error saving account to cloud_usage db", ex);
            throw new CloudRuntimeException(ex.getMessage());
        }
    }

    @Override
    public void saveUserStats(List<UserStatisticsVO> userStats) {
        TransactionLegacy txn = TransactionLegacy.currentTxn();
        try {
            txn.start();
            String sql = INSERT_USER_STATS;
            PreparedStatement pstmt = null;
            pstmt = txn.prepareAutoCloseStatement(sql); // in reality I just want CLOUD_USAGE dataSource connection
            for (UserStatisticsVO userStat : userStats) {
                pstmt.setLong(1, userStat.getId());
                pstmt.setLong(2, userStat.getDataCenterId());
                pstmt.setLong(3, userStat.getAccountId());
                pstmt.setString(4, userStat.getPublicIpAddress());
                if (userStat.getDeviceId() != null) {
                    pstmt.setLong(5, userStat.getDeviceId());
                } else {
                    pstmt.setNull(5, Types.BIGINT);
                }
                pstmt.setString(6, userStat.getDeviceType());
                if (userStat.getNetworkId() != null) {
                    pstmt.setLong(7, userStat.getNetworkId());
                } else {
                    pstmt.setNull(7, Types.BIGINT);
                }
                pstmt.setLong(8, userStat.getNetBytesReceived());
                pstmt.setLong(9, userStat.getNetBytesSent());
                pstmt.setLong(10, userStat.getCurrentBytesReceived());
                pstmt.setLong(11, userStat.getCurrentBytesSent());
                pstmt.setLong(12, userStat.getAggBytesReceived());
                pstmt.setLong(13, userStat.getAggBytesSent());
                pstmt.addBatch();
            }
            pstmt.executeBatch();
            txn.commit();
        } catch (Exception ex) {
            txn.rollback();
            logger.error("error saving user stats to cloud_usage db", ex);
            throw new CloudRuntimeException(ex.getMessage());
        }
    }

    @Override
    public void updateUserStats(List<UserStatisticsVO> userStats) {
        TransactionLegacy txn = TransactionLegacy.currentTxn();
        try {
            txn.start();
            String sql = UPDATE_USER_STATS;
            PreparedStatement pstmt = null;
            pstmt = txn.prepareAutoCloseStatement(sql);  // in reality I just want CLOUD_USAGE dataSource connection
            for (UserStatisticsVO userStat : userStats) {
                pstmt.setLong(1, userStat.getNetBytesReceived());
                pstmt.setLong(2, userStat.getNetBytesSent());
                pstmt.setLong(3, userStat.getCurrentBytesReceived());
                pstmt.setLong(4, userStat.getCurrentBytesSent());
                pstmt.setLong(5, userStat.getAggBytesReceived());
                pstmt.setLong(6, userStat.getAggBytesSent());
                pstmt.setLong(7, userStat.getId());
                pstmt.addBatch();
            }
            pstmt.executeBatch();
            txn.commit();
        } catch (Exception ex) {
            txn.rollback();
            logger.error("error saving user stats to cloud_usage db", ex);
            throw new CloudRuntimeException(ex.getMessage());
        }
    }

    @Override
    public Long getLastAccountId() {
        TransactionLegacy txn = TransactionLegacy.currentTxn();
        PreparedStatement pstmt = null;
        String sql = GET_LAST_ACCOUNT;
        try {
            pstmt = txn.prepareAutoCloseStatement(sql);
            ResultSet rs = pstmt.executeQuery();
            if (rs.next()) {
                return Long.valueOf(rs.getLong(1));
            }
        } catch (Exception ex) {
            logger.error("error getting last account id", ex);
        }
        return null;
    }

    @Override
    public Long getLastUserStatsId() {
        TransactionLegacy txn = TransactionLegacy.currentTxn();
        PreparedStatement pstmt = null;
        String sql = GET_LAST_USER_STATS;
        try {
            pstmt = txn.prepareAutoCloseStatement(sql);
            ResultSet rs = pstmt.executeQuery();
            if (rs.next()) {
                return Long.valueOf(rs.getLong(1));
            }
        } catch (Exception ex) {
            logger.error("error getting last user stats id", ex);
        }
        return null;
    }

    @Override
    public List<Long> listPublicTemplatesByAccount(long accountId) {
        TransactionLegacy txn = TransactionLegacy.currentTxn();
        PreparedStatement pstmt = null;
        String sql = GET_PUBLIC_TEMPLATES_BY_ACCOUNTID;
        List<Long> templateList = new ArrayList<Long>();
        try {
            pstmt = txn.prepareAutoCloseStatement(sql);
            pstmt.setLong(1, accountId);
            ResultSet rs = pstmt.executeQuery();
            if (rs.next()) {
                templateList.add(Long.valueOf(rs.getLong(1)));
            }
        } catch (Exception ex) {
            logger.error("error listing public templates", ex);
        }
        return templateList;
    }

    @Override
    public Long getLastVmDiskStatsId() {
        TransactionLegacy txn = TransactionLegacy.currentTxn();
        PreparedStatement pstmt = null;
        String sql = GET_LAST_VM_DISK_STATS;
        try {
            pstmt = txn.prepareAutoCloseStatement(sql);
            ResultSet rs = pstmt.executeQuery();
            if (rs.next()) {
                return Long.valueOf(rs.getLong(1));
            }
        } catch (Exception ex) {
            logger.error("error getting last vm disk stats id", ex);
        }
        return null;
    }

    @Override
    public void updateVmDiskStats(List<VmDiskStatisticsVO> vmDiskStats) {
        TransactionLegacy txn = TransactionLegacy.currentTxn();
        try {
            txn.start();
            String sql = UPDATE_VM_DISK_STATS;
            PreparedStatement pstmt = null;
            pstmt = txn.prepareAutoCloseStatement(sql);  // in reality I just want CLOUD_USAGE dataSource connection
            for (VmDiskStatisticsVO vmDiskStat : vmDiskStats) {
                pstmt.setLong(1, vmDiskStat.getNetIORead());
                pstmt.setLong(2, vmDiskStat.getNetIOWrite());
                pstmt.setLong(3, vmDiskStat.getCurrentIORead());
                pstmt.setLong(4, vmDiskStat.getCurrentIOWrite());
                pstmt.setLong(5, vmDiskStat.getAggIORead());
                pstmt.setLong(6, vmDiskStat.getAggIOWrite());
                pstmt.setLong(7, vmDiskStat.getNetBytesRead());
                pstmt.setLong(8, vmDiskStat.getNetBytesWrite());
                pstmt.setLong(9, vmDiskStat.getCurrentBytesRead());
                pstmt.setLong(10, vmDiskStat.getCurrentBytesWrite());
                pstmt.setLong(11, vmDiskStat.getAggBytesRead());
                pstmt.setLong(12, vmDiskStat.getAggBytesWrite());
                pstmt.setLong(13, vmDiskStat.getId());
                pstmt.addBatch();
            }
            pstmt.executeBatch();
            txn.commit();
        } catch (Exception ex) {
            txn.rollback();
            logger.error("error saving vm disk stats to cloud_usage db", ex);
            throw new CloudRuntimeException(ex.getMessage());
        }

    }

    @Override
    public void saveVmDiskStats(List<VmDiskStatisticsVO> vmDiskStats) {
        TransactionLegacy txn = TransactionLegacy.currentTxn();
        try {
            txn.start();
            String sql = INSERT_VM_DISK_STATS;
            PreparedStatement pstmt = null;
            pstmt = txn.prepareAutoCloseStatement(sql); // in reality I just want CLOUD_USAGE dataSource connection
            for (VmDiskStatisticsVO vmDiskStat : vmDiskStats) {
                pstmt.setLong(1, vmDiskStat.getId());
                pstmt.setLong(2, vmDiskStat.getDataCenterId());
                pstmt.setLong(3, vmDiskStat.getAccountId());
                if (vmDiskStat.getVmId() != null) {
                    pstmt.setLong(4, vmDiskStat.getVmId());
                } else {
                    pstmt.setNull(4, Types.BIGINT);
                }
                if (vmDiskStat.getVolumeId() != null) {
                    pstmt.setLong(5, vmDiskStat.getVolumeId());
                } else {
                    pstmt.setNull(5, Types.BIGINT);
                }
                pstmt.setLong(6, vmDiskStat.getNetIORead());
                pstmt.setLong(7, vmDiskStat.getNetIOWrite());
                pstmt.setLong(8, vmDiskStat.getCurrentIORead());
                pstmt.setLong(9, vmDiskStat.getCurrentIOWrite());
                pstmt.setLong(10, vmDiskStat.getAggIORead());
                pstmt.setLong(11, vmDiskStat.getAggIOWrite());
                pstmt.setLong(12, vmDiskStat.getNetBytesRead());
                pstmt.setLong(13, vmDiskStat.getNetBytesWrite());
                pstmt.setLong(14, vmDiskStat.getCurrentBytesRead());
                pstmt.setLong(15, vmDiskStat.getCurrentBytesWrite());
                pstmt.setLong(16, vmDiskStat.getAggBytesRead());
                pstmt.setLong(17, vmDiskStat.getAggBytesWrite());
                pstmt.addBatch();
            }
            pstmt.executeBatch();
            txn.commit();
        } catch (Exception ex) {
            txn.rollback();
            logger.error("error saving vm disk stats to cloud_usage db", ex);
            throw new CloudRuntimeException(ex.getMessage());
        }

    }

    @Override
    public void saveUsageRecords(List<UsageVO> usageRecords) {
        TransactionLegacy txn = TransactionLegacy.currentTxn();
        try {
            txn.start();
            String sql = INSERT_USAGE_RECORDS;
            PreparedStatement pstmt = null;
            pstmt = txn.prepareAutoCloseStatement(sql); // in reality I just want CLOUD_USAGE dataSource connection
            for (UsageVO usageRecord : usageRecords) {
                pstmt.setLong(1, usageRecord.getZoneId());
                pstmt.setLong(2, usageRecord.getAccountId());
                pstmt.setLong(3, usageRecord.getDomainId());
                pstmt.setString(4, usageRecord.getDescription());
                pstmt.setString(5, usageRecord.getUsageDisplay());
                pstmt.setInt(6, usageRecord.getUsageType());
                pstmt.setDouble(7, usageRecord.getRawUsage());
                if (usageRecord.getVmInstanceId() != null) {
                    pstmt.setLong(8, usageRecord.getVmInstanceId());
                } else {
                    pstmt.setNull(8, Types.BIGINT);
                }
                pstmt.setString(9, usageRecord.getVmName());
                if (usageRecord.getOfferingId() != null) {
                    pstmt.setLong(10, usageRecord.getOfferingId());
                } else {
                    pstmt.setNull(10, Types.BIGINT);
                }
                if (usageRecord.getTemplateId() != null) {
                    pstmt.setLong(11, usageRecord.getTemplateId());
                } else {
                    pstmt.setNull(11, Types.BIGINT);
                }
                if (usageRecord.getUsageId() != null) {
                    pstmt.setLong(12, usageRecord.getUsageId());
                } else {
                    pstmt.setNull(12, Types.BIGINT);
                }
                pstmt.setString(13, usageRecord.getType());
                if (usageRecord.getSize() != null) {
                    pstmt.setLong(14, usageRecord.getSize());
                } else {
                    pstmt.setNull(14, Types.BIGINT);
                }
                if (usageRecord.getNetworkId() != null) {
                    pstmt.setLong(15, usageRecord.getNetworkId());
                } else {
                    pstmt.setNull(15, Types.BIGINT);
                }
                pstmt.setString(16, DateUtil.getDateDisplayString(s_gmtTimeZone, usageRecord.getStartDate()));
                pstmt.setString(17, DateUtil.getDateDisplayString(s_gmtTimeZone, usageRecord.getEndDate()));
                if (usageRecord.getVirtualSize() != null) {
                    pstmt.setLong(18, usageRecord.getVirtualSize());
                } else {
                    pstmt.setNull(18, Types.BIGINT);
                }
                pstmt.addBatch();
            }
            pstmt.executeBatch();
            txn.commit();
        } catch (Exception ex) {
            txn.rollback();
            logger.error("error saving usage records to cloud_usage db", ex);
            throw new CloudRuntimeException(ex.getMessage());
        }
    }

    @Override
    public void removeOldUsageRecords(int days) {
        String sql = DELETE_ALL_BY_INTERVAL;
        TransactionLegacy txn = TransactionLegacy.open(TransactionLegacy.USAGE_DB);
        PreparedStatement pstmt = null;
        try {
            txn.start();
            pstmt = txn.prepareAutoCloseStatement(sql);
            pstmt.setLong(1, days);
            pstmt.executeUpdate();
            txn.commit();
        } catch (Exception ex) {
            txn.rollback();
            logger.error("error removing old cloud_usage records for interval: " + days);
        } finally {
            txn.close();
        }
    }
}
