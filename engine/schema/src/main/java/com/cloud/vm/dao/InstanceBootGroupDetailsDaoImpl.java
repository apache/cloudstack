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

import org.apache.cloudstack.resourcedetail.ResourceDetailsDaoBase;
import org.apache.cloudstack.vm.bootgroup.InstanceBootGroupDetailsVO;
import org.springframework.stereotype.Component;

import com.cloud.utils.db.SearchBuilder;
import com.cloud.utils.db.SearchCriteria;

@Component
public class InstanceBootGroupDetailsDaoImpl extends ResourceDetailsDaoBase<InstanceBootGroupDetailsVO> implements InstanceBootGroupDetailsDao {

    private final SearchBuilder<InstanceBootGroupDetailsVO> bootGroupSearch;
    private final SearchBuilder<InstanceBootGroupDetailsVO> bootGroupNameSearch;

    public InstanceBootGroupDetailsDaoImpl() {
        bootGroupSearch = createSearchBuilder();
        bootGroupSearch.and("bootGroupId", bootGroupSearch.entity().getResourceId(), SearchCriteria.Op.EQ);
        bootGroupSearch.done();

        bootGroupNameSearch = createSearchBuilder();
        bootGroupNameSearch.and("bootGroupId", bootGroupNameSearch.entity().getResourceId(), SearchCriteria.Op.EQ);
        bootGroupNameSearch.and("name", bootGroupNameSearch.entity().getName(), SearchCriteria.Op.EQ);
        bootGroupNameSearch.done();
    }

    @Override
    public void addDetail(long resourceId, String key, String value, boolean display) {
        super.addDetail(new InstanceBootGroupDetailsVO(resourceId, key, value, display));
    }

    @Override
    public String getDetail(long bootGroupId, String name) {
        SearchCriteria<InstanceBootGroupDetailsVO> sc = bootGroupNameSearch.create();
        sc.setParameters("bootGroupId", bootGroupId);
        sc.setParameters("name", name);
        InstanceBootGroupDetailsVO detail = findOneBy(sc);
        return detail == null ? null : detail.getValue();
    }

    @Override
    public void setDetail(long bootGroupId, String name, String value) {
        SearchCriteria<InstanceBootGroupDetailsVO> sc = bootGroupNameSearch.create();
        sc.setParameters("bootGroupId", bootGroupId);
        sc.setParameters("name", name);
        InstanceBootGroupDetailsVO existing = findOneBy(sc);
        if (existing == null) {
            persist(new InstanceBootGroupDetailsVO(bootGroupId, name, value));
        } else {
            existing.setValue(value);
            update(existing.getId(), existing);
        }
    }
}
