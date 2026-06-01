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
package com.cloud.gpu.dao;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;


import com.cloud.utils.Pair;
import com.cloud.utils.db.TransactionLegacy;
import com.cloud.utils.exception.CloudRuntimeException;
import org.springframework.stereotype.Component;

import com.cloud.gpu.HostGpuGroupsVO;
import com.cloud.utils.db.GenericDaoBase;
import com.cloud.utils.db.GenericSearchBuilder;
import com.cloud.utils.db.SearchBuilder;
import com.cloud.utils.db.SearchCriteria;

@Component
public class HostGpuGroupsDaoImpl extends GenericDaoBase<HostGpuGroupsVO, Long> implements HostGpuGroupsDao {

    private final SearchBuilder<HostGpuGroupsVO> _hostIdGroupNameSearch;
    private final SearchBuilder<HostGpuGroupsVO> _searchByHostId;
    private final GenericSearchBuilder<HostGpuGroupsVO, Long> _searchHostIds;

    public HostGpuGroupsDaoImpl() {

        _hostIdGroupNameSearch = createSearchBuilder();
        _hostIdGroupNameSearch.and("hostId", _hostIdGroupNameSearch.entity().getHostId(), SearchCriteria.Op.EQ);
        _hostIdGroupNameSearch.and("groupName", _hostIdGroupNameSearch.entity().getGroupName(), SearchCriteria.Op.EQ);
        _hostIdGroupNameSearch.done();

        _searchByHostId = createSearchBuilder();
        _searchByHostId.and("hostId", _searchByHostId.entity().getHostId(), SearchCriteria.Op.EQ);
        _searchByHostId.done();

        _searchHostIds = createSearchBuilder(Long.class);
        _searchHostIds.selectFields(_searchHostIds.entity().getHostId());
        _searchHostIds.done();
    }

    @Override
    public HostGpuGroupsVO findByHostIdGroupName(long hostId, String groupName) {
        SearchCriteria<HostGpuGroupsVO> sc = _hostIdGroupNameSearch.create();
        sc.setParameters("hostId", hostId);
        sc.setParameters("groupName", groupName);
        return findOneBy(sc);
    }

    @Override
    public List<Long> listHostIds() {
        SearchCriteria<Long> sc = _searchHostIds.create();
        return customSearch(sc, null);
    }

    @Override
    public List<HostGpuGroupsVO> listByHostId(long hostId) {
        SearchCriteria<HostGpuGroupsVO> sc = _searchByHostId.create();
        sc.setParameters("hostId", hostId);
        return listBy(sc);
    }

    @Override
    public void persist(long hostId, List<String> gpuGroups) {
        for (String groupName : gpuGroups) {
            if (findByHostIdGroupName(hostId, groupName) == null) {
                HostGpuGroupsVO record = new HostGpuGroupsVO(hostId, groupName);
                persist(record);
            }
        }
    }

    @Override
    public void deleteGpuEntries(long hostId) {
        SearchCriteria<HostGpuGroupsVO> sc = _searchByHostId.create();
        sc.setParameters("hostId", hostId);
        remove(sc);
    }

    @Override
    public Pair<Long, Long> getGpuStats(Long dcId, Long podId, Long clusterId, Long hostId) {
        TransactionLegacy txn = TransactionLegacy.currentTxn();
        Pair<Long, Long> result = null;
        List<Long> resourceIdList = new ArrayList<>();
        String query = getStatsQuery(resourceIdList, dcId, podId, clusterId, hostId);

        try {
            PreparedStatement pstmt = txn.prepareAutoCloseStatement(query);
            for (int i = 0; i < resourceIdList.size(); i++) {
                pstmt.setLong(1 + i, resourceIdList.get(i));
            }

            ResultSet rs = pstmt.executeQuery();
            while (rs.next()) {
                result = new Pair<>(rs.getLong(1), rs.getLong(2));
            }
            return result;
        } catch (SQLException e) {
            throw new CloudRuntimeException("Error while fetching GPU stats: " + e.getMessage(), e);
        } catch (Throwable e) {
            throw new CloudRuntimeException("Caught: " + query, e);
        }
    }

    private String getStatsQuery(List<Long> resourceIdList, Long dcId, Long podId, Long clusterId, Long hostId) {
        StringBuilder query = new StringBuilder("SELECT SUM(max_capacity), SUM(remaining_capacity)" +
                                                "FROM vgpu_types " +
                                                "WHERE" +
                                                "    gpu_group_id IN (" +
                                                "        SELECT" +
                                                "            host_gpu_groups.id" +
                                                "        FROM" +
                                                "            host_gpu_groups" +
                                                "            INNER JOIN host ON host.id = host_gpu_groups.host_id ");
        if (dcId != null) {
            query.append("WHERE host.data_center_id = ? ");
            resourceIdList.add(dcId);
        }

        if (podId != null) {
            if (resourceIdList.isEmpty()) {
                query.append("WHERE ");
            } else {
                query.append("AND ");
            }
            query.append(" host.pod_id = ? ");
            resourceIdList.add(podId);
        }
        if (clusterId != null) {
            if (resourceIdList.isEmpty()) {
                query.append("WHERE ");
            } else {
                query.append("AND ");
            }
            query.append(" host.cluster_id = ? ");
            resourceIdList.add(clusterId);
        }
        if (hostId != null) {
            if (resourceIdList.isEmpty()) {
                query.append("WHERE ");
            } else {
                query.append("AND ");
            }
            query.append(" host.id = ? ");
            resourceIdList.add(hostId);
        }
        query.append("    )");
        return query.toString();
    }
}
