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
package org.apache.cloudstack.network.tungsten.dao;

import com.cloud.utils.db.GenericDaoBase;
import com.cloud.utils.db.SearchBuilder;
import com.cloud.utils.db.SearchCriteria;

public class TungstenFabricLBHealthMonitorDaoImpl extends GenericDaoBase<TungstenFabricLBHealthMonitorVO, Long>
    implements TungstenFabricLBHealthMonitorDao {
    final SearchBuilder<TungstenFabricLBHealthMonitorVO> AllFieldsSearch;

    public TungstenFabricLBHealthMonitorDaoImpl() {
        AllFieldsSearch = createSearchBuilder();
        AllFieldsSearch.and("id", AllFieldsSearch.entity().getId(), SearchCriteria.Op.EQ);
        AllFieldsSearch.and("load_balancer_id", AllFieldsSearch.entity().getLoadBalancerId(), SearchCriteria.Op.EQ);
        AllFieldsSearch.and("method", AllFieldsSearch.entity().getType(), SearchCriteria.Op.EQ);
        AllFieldsSearch.and("retry", AllFieldsSearch.entity().getRetry(), SearchCriteria.Op.EQ);
        AllFieldsSearch.and("timeout", AllFieldsSearch.entity().getTimeout(), SearchCriteria.Op.EQ);
        AllFieldsSearch.and("interval", AllFieldsSearch.entity().getInterval(), SearchCriteria.Op.EQ);
        AllFieldsSearch.and("http", AllFieldsSearch.entity().getHttpMethod(), SearchCriteria.Op.EQ);
        AllFieldsSearch.and("expected_code", AllFieldsSearch.entity().getExpectedCode(), SearchCriteria.Op.EQ);
        AllFieldsSearch.and("url_path", AllFieldsSearch.entity().getUrlPath(), SearchCriteria.Op.EQ);
    }

    @Override
    public TungstenFabricLBHealthMonitorVO findByLbId(final long lbId) {
        SearchCriteria<TungstenFabricLBHealthMonitorVO> searchCriteria = AllFieldsSearch.create();
        searchCriteria.setParameters("load_balancer_id", lbId);
        return findOneBy(searchCriteria);
    }
}
