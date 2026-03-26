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

import org.apache.cloudstack.api.ApiConstants;
import org.springframework.stereotype.Component;

import org.apache.cloudstack.resourcedetail.ResourceDetailsDaoBase;

import com.cloud.utils.db.SearchBuilder;
import com.cloud.utils.db.SearchCriteria;
import com.cloud.vm.NicDetailVO;

@Component
public class NicDetailsDaoImpl extends ResourceDetailsDaoBase<NicDetailVO> implements NicDetailsDao {
    private final SearchBuilder<NicDetailVO> NameValuesSearch;

    public NicDetailsDaoImpl() {
        super();
        NameValuesSearch = createSearchBuilder();
        NameValuesSearch.and(ApiConstants.NAME, NameValuesSearch.entity().getName(), SearchCriteria.Op.EQ);
        NameValuesSearch.and(ApiConstants.VALUE, NameValuesSearch.entity().getValue(), SearchCriteria.Op.IN);
        NameValuesSearch.done();
    }


    @Override
    public void addDetail(long resourceId, String key, String value, boolean display) {
        super.addDetail(new NicDetailVO(resourceId, key, value, display));
    }

    @Override
    public void removeDetailsForValuesIn(String resourceName, List<String> values) {
        SearchCriteria<NicDetailVO> sc = NameValuesSearch.create();
        sc.setParameters(ApiConstants.NAME, resourceName);
        sc.setParameters(ApiConstants.VALUE, values.toArray());
        remove(sc);
    }
}
