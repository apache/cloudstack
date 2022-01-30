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

import com.cloud.usage.UsageStorageVO;
import com.cloud.utils.DateUtil;
import com.cloud.utils.db.GenericDaoBase;
import com.cloud.utils.db.SearchBuilder;
import com.cloud.utils.db.SearchCriteria;
import com.cloud.utils.db.TransactionLegacy;

@Component
public class UsageStorageDaoImpl extends GenericDaoBase<UsageStorageVO, Long> implements UsageStorageDao {
    public static final Logger s_logger = Logger.getLogger(UsageStorageDaoImpl.class.getName());

    protected static final String REMOVE_BY_USERID_STORAGEID = "DELETE FROM usage_storage WHERE account_id = ? AND entity_id = ? AND storage_type = ?";
    protected static final String UPDATE_DELETED = "UPDATE usage_storage SET deleted = ? WHERE account_id = ? AND entity_id = ? AND storage_type = ? AND zone_id = ? and deleted IS NULL";
    protected static final String GET_USAGE_RECORDS_BY_ACCOUNT =
        "SELECT entity_id, zone_id, account_id, domain_id, storage_type, source_id, size, created, deleted, virtual_size " + "FROM usage_storage "
            + "WHERE account_id = ? AND ((deleted IS NULL) OR (created BETWEEN ? AND ?) OR " + "      (deleted BETWEEN ? AND ?) OR ((created <= ?) AND (deleted >= ?)))";
    protected static final String GET_USAGE_RECORDS_BY_DOMAIN =
        "SELECT entity_id, zone_id, account_id, domain_id, storage_type, source_id, size, created, deleted, virtual_size " + "FROM usage_storage "
            + "WHERE domain_id = ? AND ((deleted IS NULL) OR (created BETWEEN ? AND ?) OR " + "      (deleted BETWEEN ? AND ?) OR ((created <= ?) AND (deleted >= ?)))";
    protected static final String GET_ALL_USAGE_RECORDS = "SELECT entity_id, zone_id, account_id, domain_id, storage_type, source_id, size, created, deleted, virtual_size "
        + "FROM usage_storage " + "WHERE (deleted IS NULL) OR (created BETWEEN ? AND ?) OR " + "      (deleted BETWEEN ? AND ?) OR ((created <= ?) AND (deleted >= ?))";

    private final SearchBuilder<UsageStorageVO> IdSearch;
    private final SearchBuilder<UsageStorageVO> IdZoneSearch;

    public UsageStorageDaoImpl() {
        IdSearch = createSearchBuilder();
        IdSearch.and("accountId", IdSearch.entity().getAccountId(), SearchCriteria.Op.EQ);
        IdSearch.and("id", IdSearch.entity().getEntityId(), SearchCriteria.Op.EQ);
        IdSearch.and("type", IdSearch.entity().getStorageType(), SearchCriteria.Op.EQ);
        IdSearch.done();

        IdZoneSearch = createSearchBuilder();
        IdZoneSearch.and("accountId", IdZoneSearch.entity().getAccountId(), SearchCriteria.Op.EQ);
        IdZoneSearch.and("id", IdZoneSearch.entity().getEntityId(), SearchCriteria.Op.EQ);
        IdZoneSearch.and("type", IdZoneSearch.entity().getStorageType(), SearchCriteria.Op.EQ);
        IdZoneSearch.and("dcId", IdZoneSearch.entity().getZoneId(), SearchCriteria.Op.EQ);
        IdZoneSearch.and("deleted", IdZoneSearch.entity().getDeleted(), SearchCriteria.Op.NULL);
        IdZoneSearch.done();
    }

    @Override
    public List<UsageStorageVO> listById(long accountId, long id, int type) {
        SearchCriteria<UsageStorageVO> sc = IdSearch.create();
        sc.setParameters("accountId", accountId);
        sc.setParameters("id", id);
        sc.setParameters("type", type);
        return listBy(sc, null);
    }

    @Override
    public List<UsageStorageVO> listByIdAndZone(long accountId, long id, int type, long dcId) {
        SearchCriteria<UsageStorageVO> sc = IdZoneSearch.create();
        sc.setParameters("accountId", accountId);
        sc.setParameters("id", id);
        sc.setParameters("type", type);
        sc.setParameters("dcId", dcId);
        sc.setParameters("deleted", null);
        return listBy(sc, null);
    }

    @Override
    public void removeBy(long accountId, long volId, int storageType) {
        TransactionLegacy txn = TransactionLegacy.open(TransactionLegacy.USAGE_DB);
        try {
            txn.start();
            String sql = REMOVE_BY_USERID_STORAGEID;
            try( PreparedStatement pstmt = txn.prepareStatement(sql);) {
                pstmt.setLong(1, accountId);
                pstmt.setLong(2, volId);
                pstmt.setInt(3, storageType);
                pstmt.executeUpdate();
            }catch(SQLException e)
            {
                throw new CloudException("removeBy:Exception:"+e.getMessage(),e);
            }
            txn.commit();
        } catch (Exception e) {
            txn.rollback();
            s_logger.error("Error removing usageStorageVO", e);
        } finally {
            txn.close();
        }
    }

    @Override
    public void update(UsageStorageVO usage) {
        TransactionLegacy txn = TransactionLegacy.open(TransactionLegacy.USAGE_DB);
        try {
            txn.start();
            if (usage.getDeleted() != null) {
                try(PreparedStatement pstmt = txn.prepareStatement(UPDATE_DELETED);) {
                    if (pstmt != null) {
                        pstmt.setString(1, DateUtil.getDateDisplayString(TimeZone.getTimeZone("GMT"), usage.getDeleted()));
                        pstmt.setLong(2, usage.getAccountId());
                        pstmt.setLong(3, usage.getEntityId());
                        pstmt.setInt(4, usage.getStorageType());
                        pstmt.setLong(5, usage.getZoneId());
                        pstmt.executeUpdate();
                    }
                }catch (SQLException e)
                {
                    throw new CloudException("UsageStorageVO update Error:"+e.getMessage(),e);
                }
            }
            txn.commit();
        } catch (Exception e) {
            txn.rollback();
            s_logger.error("Error updating UsageStorageVO:"+e.getMessage(), e);
        } finally {
            txn.close();
        }
    }

    @Override
    public List<UsageStorageVO> getUsageRecords(Long accountId, Long domainId, Date startDate, Date endDate, boolean limit, int page) {
        List<UsageStorageVO> usageRecords = new ArrayList<UsageStorageVO>();

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
        int i = 1;
        try (PreparedStatement pstmt = txn.prepareStatement(sql);){
            if (param1 != null) {
                pstmt.setLong(i++, param1);
            }
            pstmt.setString(i++, DateUtil.getDateDisplayString(TimeZone.getTimeZone("GMT"), startDate));
            pstmt.setString(i++, DateUtil.getDateDisplayString(TimeZone.getTimeZone("GMT"), endDate));
            pstmt.setString(i++, DateUtil.getDateDisplayString(TimeZone.getTimeZone("GMT"), startDate));
            pstmt.setString(i++, DateUtil.getDateDisplayString(TimeZone.getTimeZone("GMT"), endDate));
            pstmt.setString(i++, DateUtil.getDateDisplayString(TimeZone.getTimeZone("GMT"), startDate));
            pstmt.setString(i++, DateUtil.getDateDisplayString(TimeZone.getTimeZone("GMT"), endDate));

            try(ResultSet rs = pstmt.executeQuery();) {
                while (rs.next()) {
                    //id, zone_id, account_id, domain_id, storage_type, size, created, deleted
                    Long id = Long.valueOf(rs.getLong(1));
                    Long zoneId = Long.valueOf(rs.getLong(2));
                    Long acctId = Long.valueOf(rs.getLong(3));
                    Long dId = Long.valueOf(rs.getLong(4));
                    Integer type = Integer.valueOf(rs.getInt(5));
                    Long sourceId = Long.valueOf(rs.getLong(6));
                    Long size = Long.valueOf(rs.getLong(7));
                    Long virtualSize = Long.valueOf(rs.getLong(10));
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

                    usageRecords.add(new UsageStorageVO(id, zoneId, acctId, dId, type, sourceId, size, virtualSize, createdDate, deletedDate));
                }
            }catch(SQLException e)
            {
                throw new CloudException("getUsageRecords:"+e.getMessage(),e);
            }
        }catch (Exception e) {
            txn.rollback();
            s_logger.error("getUsageRecords:Exception:"+e.getMessage(), e);
        } finally {
            txn.close();
        }
        return usageRecords;
    }
}
