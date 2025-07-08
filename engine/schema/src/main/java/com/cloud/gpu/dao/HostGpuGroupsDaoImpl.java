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

import java.util.List;


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
}
