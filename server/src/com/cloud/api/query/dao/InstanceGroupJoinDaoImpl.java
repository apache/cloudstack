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
package com.cloud.api.query.dao;

import java.util.List;

import javax.ejb.Local;

import org.apache.log4j.Logger;

import com.cloud.api.ApiResponseHelper;
import com.cloud.api.query.vo.InstanceGroupJoinVO;

import org.apache.cloudstack.api.response.InstanceGroupResponse;
import org.springframework.stereotype.Component;

import com.cloud.utils.db.GenericDaoBase;
import com.cloud.utils.db.SearchBuilder;
import com.cloud.utils.db.SearchCriteria;
import com.cloud.vm.InstanceGroup;


@Component
@Local(value={InstanceGroupJoinDao.class})
public class InstanceGroupJoinDaoImpl extends GenericDaoBase<InstanceGroupJoinVO, Long> implements InstanceGroupJoinDao {
    public static final Logger s_logger = Logger.getLogger(InstanceGroupJoinDaoImpl.class);

    private SearchBuilder<InstanceGroupJoinVO> vrIdSearch;


    protected InstanceGroupJoinDaoImpl() {

        vrIdSearch = createSearchBuilder();
        vrIdSearch.and("id", vrIdSearch.entity().getId(), SearchCriteria.Op.EQ);
        vrIdSearch.done();

        this._count = "select count(distinct id) from instance_group_view WHERE ";
    }



    @Override
    public InstanceGroupResponse newInstanceGroupResponse(InstanceGroupJoinVO group) {
        InstanceGroupResponse groupResponse = new InstanceGroupResponse();
        groupResponse.setId(group.getUuid());
        groupResponse.setName(group.getName());
        groupResponse.setCreated(group.getCreated());

        ApiResponseHelper.populateOwner(groupResponse, group);

        groupResponse.setObjectName("instancegroup");
        return groupResponse;
    }



    @Override
    public InstanceGroupJoinVO newInstanceGroupView(InstanceGroup group) {
        SearchCriteria<InstanceGroupJoinVO> sc = vrIdSearch.create();
        sc.setParameters("id", group.getId());
        List<InstanceGroupJoinVO> grps = searchIncludingRemoved(sc, null, null, false);
        assert grps != null && grps.size() == 1 : "No vm group found for group id " + group.getId();
        return grps.get(0);

    }




}
