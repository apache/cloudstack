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
package org.apache.cloudstack.applicationcluster.dao;

import org.apache.cloudstack.applicationcluster.ApplicationClusterDetailsVO;
import com.cloud.utils.db.SearchBuilder;
import com.cloud.utils.db.SearchCriteria;
import org.springframework.stereotype.Component;

import com.cloud.utils.db.GenericDaoBase;


@Component
public class ApplicationClusterDetailsDaoImpl extends GenericDaoBase<ApplicationClusterDetailsVO, Long> implements ApplicationClusterDetailsDao {

    private final SearchBuilder<ApplicationClusterDetailsVO> clusterIdSearch;

    public ApplicationClusterDetailsDaoImpl() {
        clusterIdSearch = createSearchBuilder();
        clusterIdSearch.and("clusterId", clusterIdSearch.entity().getClusterId(), SearchCriteria.Op.EQ);
        clusterIdSearch.done();
    }

    @Override
    public ApplicationClusterDetailsVO findByClusterId(long clusterId) {
        SearchCriteria<ApplicationClusterDetailsVO> sc = clusterIdSearch.create();
        sc.setParameters("clusterId", clusterId);
        return findOneBy(sc);
    }
}
