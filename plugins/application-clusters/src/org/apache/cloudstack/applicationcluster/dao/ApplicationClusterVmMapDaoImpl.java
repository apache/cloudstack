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

import org.apache.cloudstack.applicationcluster.ApplicationClusterVmMapVO;
import com.cloud.utils.db.SearchBuilder;
import com.cloud.utils.db.SearchCriteria;
import org.springframework.stereotype.Component;

import com.cloud.utils.db.GenericDaoBase;

import java.util.List;


@Component
public class ApplicationClusterVmMapDaoImpl extends GenericDaoBase<ApplicationClusterVmMapVO, Long> implements ApplicationClusterVmMapDao {

    private final SearchBuilder<ApplicationClusterVmMapVO> clusterIdSearch;

    public ApplicationClusterVmMapDaoImpl() {
        clusterIdSearch = createSearchBuilder();
        clusterIdSearch.and("clusterId", clusterIdSearch.entity().getClusterId(), SearchCriteria.Op.EQ);
        clusterIdSearch.done();
    }

    @Override
    public List<ApplicationClusterVmMapVO> listByClusterId(long clusterId) {
        SearchCriteria<ApplicationClusterVmMapVO> sc = clusterIdSearch.create();
        sc.setParameters("clusterId", clusterId);
        return listBy(sc, null);
    }
}
