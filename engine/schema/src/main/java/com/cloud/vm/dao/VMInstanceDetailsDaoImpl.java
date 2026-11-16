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


import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;

import org.apache.cloudstack.resourcedetail.ResourceDetailsDaoBase;

import com.cloud.utils.db.SearchBuilder;
import com.cloud.utils.db.SearchCriteria;
import com.cloud.vm.VMInstanceDetailVO;

@Component
public class VMInstanceDetailsDaoImpl extends ResourceDetailsDaoBase<VMInstanceDetailVO> implements VMInstanceDetailsDao {

    @Override
    public void addDetail(long resourceId, String key, String value, boolean display) {
        super.addDetail(new VMInstanceDetailVO(resourceId, key, value, display));
    }

    @Override
    public int removeDetailsWithPrefix(long vmId, String prefix) {
        if (StringUtils.isBlank(prefix)) {
            return 0;
        }
        SearchBuilder<VMInstanceDetailVO> sb = createSearchBuilder();
        sb.and("vmId", sb.entity().getResourceId(), SearchCriteria.Op.EQ);
        sb.and("prefix", sb.entity().getName(), SearchCriteria.Op.LIKE);
        sb.done();
        SearchCriteria<VMInstanceDetailVO> sc = sb.create();
        sc.setParameters("vmId", vmId);
        sc.setParameters("prefix", prefix + "%");
        return super.remove(sc);
    }
}
