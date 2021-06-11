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
package com.cloud.storage.dao;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import org.apache.log4j.Logger;
import org.springframework.stereotype.Component;

import com.cloud.host.Status;
import com.cloud.storage.StoragePoolHostVO;
import com.cloud.utils.Pair;
import com.cloud.utils.db.GenericDaoBase;
import com.cloud.utils.db.SearchBuilder;
import com.cloud.utils.db.SearchCriteria;
import com.cloud.utils.db.TransactionLegacy;

@Component
public class StoragePoolHostDaoImpl extends GenericDaoBase<StoragePoolHostVO, Long> implements StoragePoolHostDao {
    public static final Logger s_logger = Logger.getLogger(StoragePoolHostDaoImpl.class.getName());

    protected final SearchBuilder<StoragePoolHostVO> PoolSearch;
    protected final SearchBuilder<StoragePoolHostVO> HostSearch;
    protected final SearchBuilder<StoragePoolHostVO> PoolHostSearch;

    protected static final String HOST_FOR_POOL_SEARCH = "SELECT * FROM storage_pool_host_ref ph,  host h where  ph.host_id = h.id and ph.pool_id=? and h.status=? ";

    protected static final String HOSTS_FOR_POOLS_SEARCH = "SELECT DISTINCT(ph.host_id) FROM storage_pool_host_ref ph, host h WHERE ph.host_id = h.id AND h.status = 'Up' AND resource_state = 'Enabled' AND ph.pool_id IN (?)";

    protected static final String STORAGE_POOL_HOST_INFO = "SELECT p.data_center_id,  count(ph.host_id) " + " FROM storage_pool p, storage_pool_host_ref ph "
        + " WHERE p.id = ph.pool_id AND p.data_center_id = ? " + " GROUP by p.data_center_id";

    protected static final String SHARED_STORAGE_POOL_HOST_INFO = "SELECT p.data_center_id,  count(ph.host_id) " + " FROM storage_pool p, storage_pool_host_ref ph "
        + " WHERE p.id = ph.pool_id AND p.data_center_id = ? " + " AND p.pool_type NOT IN ('LVM', 'Filesystem')" + " GROUP by p.data_center_id";

    protected static final String DELETE_PRIMARY_RECORDS = "DELETE " + "FROM storage_pool_host_ref " + "WHERE host_id = ?";

    public StoragePoolHostDaoImpl() {
        PoolSearch = createSearchBuilder();
        PoolSearch.and("pool_id", PoolSearch.entity().getPoolId(), SearchCriteria.Op.EQ);
        PoolSearch.done();

        HostSearch = createSearchBuilder();
        HostSearch.and("host_id", HostSearch.entity().getHostId(), SearchCriteria.Op.EQ);
        HostSearch.done();

        PoolHostSearch = createSearchBuilder();
        PoolHostSearch.and("pool_id", PoolHostSearch.entity().getPoolId(), SearchCriteria.Op.EQ);
        PoolHostSearch.and("host_id", PoolHostSearch.entity().getHostId(), SearchCriteria.Op.EQ);
        PoolHostSearch.done();

    }

    @Override
    public List<StoragePoolHostVO> listByPoolId(long id) {
        SearchCriteria<StoragePoolHostVO> sc = PoolSearch.create();
        sc.setParameters("pool_id", id);
        return listIncludingRemovedBy(sc);
    }

    @Override
    public List<StoragePoolHostVO> listByHostIdIncludingRemoved(long hostId) {
        SearchCriteria<StoragePoolHostVO> sc = HostSearch.create();
        sc.setParameters("host_id", hostId);
        return listIncludingRemovedBy(sc);
    }

    @Override
    public List<StoragePoolHostVO> listByHostId(long hostId) {
        SearchCriteria<StoragePoolHostVO> sc = HostSearch.create();
        sc.setParameters("host_id", hostId);
        return listBy(sc);
    }

    @Override
    public StoragePoolHostVO findByPoolHost(long poolId, long hostId) {
        SearchCriteria<StoragePoolHostVO> sc = PoolHostSearch.create();
        sc.setParameters("pool_id", poolId);
        sc.setParameters("host_id", hostId);
        return findOneIncludingRemovedBy(sc);
    }

    @Override
    public List<StoragePoolHostVO> listByHostStatus(long poolId, Status hostStatus) {
        TransactionLegacy txn = TransactionLegacy.currentTxn();
        String sql = HOST_FOR_POOL_SEARCH;
        List<StoragePoolHostVO> result = new ArrayList<StoragePoolHostVO>();
        try(PreparedStatement pstmt = txn.prepareStatement(sql);) {
            pstmt.setLong(1, poolId);
            pstmt.setString(2, hostStatus.toString());
            try(ResultSet rs = pstmt.executeQuery();) {
                while (rs.next()) {
                    // result.add(toEntityBean(rs, false)); TODO: this is buggy in
                    // GenericDaoBase for hand constructed queries
                    long id = rs.getLong(1); // ID column
                    result.add(findById(id));
                }
            }catch (SQLException e) {
                s_logger.warn("listByHostStatus:Exception: ", e);
            }
        } catch (Exception e) {
            s_logger.warn("listByHostStatus:Exception: ", e);
        }
        return result;
    }

    @Override
    public List<Long> findHostsConnectedToPools(List<Long> poolIds) {
        List<Long> hosts = new ArrayList<Long>();
        if (poolIds == null || poolIds.isEmpty()) {
            return hosts;
        }

        String poolIdsInStr = poolIds.stream().map(poolId -> String.valueOf(poolId)).collect(Collectors.joining(",", "(", ")"));
        String sql = HOSTS_FOR_POOLS_SEARCH.replace("(?)", poolIdsInStr);

        TransactionLegacy txn = TransactionLegacy.currentTxn();
        try(PreparedStatement pstmt = txn.prepareStatement(sql);) {
            try(ResultSet rs = pstmt.executeQuery();) {
                while (rs.next()) {
                    long hostId = rs.getLong(1); // host_id column
                    hosts.add(hostId);
                }
            } catch (SQLException e) {
                s_logger.warn("findHostsConnectedToPools:Exception: ", e);
            }
        } catch (Exception e) {
            s_logger.warn("findHostsConnectedToPools:Exception: ", e);
        }

        return hosts;
    }

    @Override
    public List<Pair<Long, Integer>> getDatacenterStoragePoolHostInfo(long dcId, boolean sharedOnly) {
        ArrayList<Pair<Long, Integer>> l = new ArrayList<Pair<Long, Integer>>();
        String sql = sharedOnly ? SHARED_STORAGE_POOL_HOST_INFO : STORAGE_POOL_HOST_INFO;
        TransactionLegacy txn = TransactionLegacy.currentTxn();
        PreparedStatement pstmt = null;
        try {
            pstmt = txn.prepareAutoCloseStatement(sql);
            pstmt.setLong(1, dcId);

            ResultSet rs = pstmt.executeQuery();
            while (rs.next()) {
                l.add(new Pair<Long, Integer>(rs.getLong(1), rs.getInt(2)));
            }
        } catch (SQLException e) {
            s_logger.debug("SQLException: ", e);
        }
        return l;
    }

    /**
     * This method deletes the primary records from the host
     *
     * @param hostId
     *            -- id of the host
     */
    @Override
    public void deletePrimaryRecordsForHost(long hostId) {
        SearchCriteria<StoragePoolHostVO> sc = HostSearch.create();
        sc.setParameters("host_id", hostId);
        TransactionLegacy txn = TransactionLegacy.currentTxn();
        txn.start();
        remove(sc);
        txn.commit();
    }

    @Override
    public void deleteStoragePoolHostDetails(long hostId, long poolId) {
        SearchCriteria<StoragePoolHostVO> sc = PoolHostSearch.create();
        sc.setParameters("host_id", hostId);
        sc.setParameters("pool_id", poolId);
        TransactionLegacy txn = TransactionLegacy.currentTxn();
        txn.start();
        remove(sc);
        txn.commit();
    }
}
