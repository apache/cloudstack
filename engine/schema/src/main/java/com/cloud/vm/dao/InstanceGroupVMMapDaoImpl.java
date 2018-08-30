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


import org.springframework.stereotype.Component;

import com.cloud.utils.db.GenericDaoBase;
import com.cloud.utils.db.SearchBuilder;
import com.cloud.utils.db.SearchCriteria;
import com.cloud.vm.InstanceGroupVMMapVO;

@Component
public class InstanceGroupVMMapDaoImpl extends GenericDaoBase<InstanceGroupVMMapVO, Long> implements InstanceGroupVMMapDao {

    private SearchBuilder<InstanceGroupVMMapVO> ListByVmId;
    private SearchBuilder<InstanceGroupVMMapVO> ListByGroupId;
    private SearchBuilder<InstanceGroupVMMapVO> ListByVmIdGroupId;

    protected InstanceGroupVMMapDaoImpl() {
        ListByVmId = createSearchBuilder();
        ListByVmId.and("instanceId", ListByVmId.entity().getInstanceId(), SearchCriteria.Op.EQ);
        ListByVmId.done();

        ListByGroupId = createSearchBuilder();
        ListByGroupId.and("groupId", ListByGroupId.entity().getGroupId(), SearchCriteria.Op.EQ);
        ListByGroupId.done();

        ListByVmIdGroupId = createSearchBuilder();
        ListByVmIdGroupId.and("instanceId", ListByVmIdGroupId.entity().getInstanceId(), SearchCriteria.Op.EQ);
        ListByVmIdGroupId.and("groupId", ListByVmIdGroupId.entity().getGroupId(), SearchCriteria.Op.EQ);
        ListByVmIdGroupId.done();
    }

    @Override
    public List<InstanceGroupVMMapVO> listByInstanceId(long vmId) {
        SearchCriteria<InstanceGroupVMMapVO> sc = ListByVmId.create();
        sc.setParameters("instanceId", vmId);
        return listBy(sc);
    }

    @Override
    public List<InstanceGroupVMMapVO> listByGroupId(long groupId) {
        SearchCriteria<InstanceGroupVMMapVO> sc = ListByGroupId.create();
        sc.setParameters("groupId", groupId);
        return listBy(sc);
    }

    @Override
    public InstanceGroupVMMapVO findByVmIdGroupId(long instanceId, long groupId) {
        SearchCriteria<InstanceGroupVMMapVO> sc = ListByVmIdGroupId.create();
        sc.setParameters("groupId", groupId);
        sc.setParameters("instanceId", instanceId);
        return findOneBy(sc);
    }

}
