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
package com.cloud.vm.dao;

import java.util.List;

import javax.annotation.PostConstruct;

import org.springframework.stereotype.Component;

import com.cloud.utils.db.DB;
import com.cloud.utils.db.GenericDaoBase;
import com.cloud.utils.db.SearchBuilder;
import com.cloud.utils.db.SearchCriteria;
import com.cloud.utils.db.SearchCriteria.Op;
import com.cloud.vm.UserVmCloneSettingVO;

@Component
@DB()
public class UserVmCloneSettingDaoImpl extends GenericDaoBase<UserVmCloneSettingVO, Long> implements UserVmCloneSettingDao {

    protected SearchBuilder<UserVmCloneSettingVO> vmIdSearch;
    protected SearchBuilder<UserVmCloneSettingVO> cloneTypeSearch;

    public UserVmCloneSettingDaoImpl() {
    }

    @PostConstruct
    public void init() {
        // Initialize the search builders.
        vmIdSearch = createSearchBuilder();
        vmIdSearch.and("vmId", vmIdSearch.entity().getVmId(), Op.EQ);
        vmIdSearch.done();

        cloneTypeSearch = createSearchBuilder();
        cloneTypeSearch.and("cloneType", cloneTypeSearch.entity().getCloneType(), Op.EQ);
        cloneTypeSearch.done();
    }

    @Override
    public UserVmCloneSettingVO findByVmId(long vmId) {
        SearchCriteria<UserVmCloneSettingVO> sc = vmIdSearch.create();
        sc.setParameters("vmId", vmId);
        return findOneBy(sc);
    }

    @Override
    public List<UserVmCloneSettingVO> listByCloneType(String cloneType) {
        SearchCriteria<UserVmCloneSettingVO> sc = cloneTypeSearch.create();
        sc.setParameters("cloneType", cloneType);
        return search(sc, null);
    }

}
