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
package org.apache.cloudstack.quota.dao;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.cloudstack.quota.vo.VMInstanceDetailVO;
import org.springframework.stereotype.Component;

import com.cloud.utils.db.GenericDaoBase;
import com.cloud.utils.db.SearchBuilder;
import com.cloud.utils.db.SearchCriteria;

@Component
public class VMInstanceDetailsDaoImpl extends GenericDaoBase<VMInstanceDetailVO, Long> implements VMInstanceDetailsDao {
    private SearchBuilder<VMInstanceDetailVO> AllFieldsSearch;

    public VMInstanceDetailsDaoImpl() {
        AllFieldsSearch = createSearchBuilder();
        AllFieldsSearch.and("resourceId", AllFieldsSearch.entity().getResourceId(), SearchCriteria.Op.EQ);
        AllFieldsSearch.and("name", AllFieldsSearch.entity().getName(), SearchCriteria.Op.EQ);
        AllFieldsSearch.and("value", AllFieldsSearch.entity().getValue(), SearchCriteria.Op.EQ);
        AllFieldsSearch.and("display", AllFieldsSearch.entity().isDisplay(), SearchCriteria.Op.EQ);
        AllFieldsSearch.done();
    }

    @Override
    public Map<String, String> listDetailsKeyPairs(long resourceId) {
        Map<String, String> details = new HashMap<String, String>();
        SearchCriteria<VMInstanceDetailVO> sc = AllFieldsSearch.create();
        sc.setParameters("resourceId", resourceId);

        List<VMInstanceDetailVO> results = search(sc, null);
        for (VMInstanceDetailVO result : results) {
            details.put(result.getName(), result.getValue());
        }
        return details;
    }

}
