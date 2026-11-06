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
package com.cloud.kubernetes.cluster.dao;

import java.util.List;

import org.springframework.stereotype.Component;

import com.cloud.kubernetes.cluster.KubernetesClusterNodeGroup;
import com.cloud.kubernetes.cluster.KubernetesClusterNodeGroupVO;
import com.cloud.utils.db.Filter;
import com.cloud.utils.db.GenericDaoBase;
import com.cloud.utils.db.SearchBuilder;
import com.cloud.utils.db.SearchCriteria;

@Component
public class KubernetesClusterNodeGroupDaoImpl extends GenericDaoBase<KubernetesClusterNodeGroupVO, Long>
        implements KubernetesClusterNodeGroupDao {

    private final SearchBuilder<KubernetesClusterNodeGroupVO> clusterIdSearch;
    private final SearchBuilder<KubernetesClusterNodeGroupVO> stateSearch;

    public KubernetesClusterNodeGroupDaoImpl() {
        clusterIdSearch = createSearchBuilder();
        clusterIdSearch.and("clusterId", clusterIdSearch.entity().getClusterId(), SearchCriteria.Op.EQ);
        clusterIdSearch.and("name", clusterIdSearch.entity().getName(), SearchCriteria.Op.EQ);
        clusterIdSearch.and("defaultGroup", clusterIdSearch.entity().isDefaultGroup(), SearchCriteria.Op.EQ);
        clusterIdSearch.done();

        stateSearch = createSearchBuilder();
        stateSearch.and("state", stateSearch.entity().getState(), SearchCriteria.Op.IN);
        stateSearch.done();
    }

    /**
     * @return the node groups of the cluster, the default node group first
     */
    @Override
    public List<KubernetesClusterNodeGroupVO> listByClusterId(long clusterId) {
        SearchCriteria<KubernetesClusterNodeGroupVO> sc = clusterIdSearch.create();
        sc.setParameters("clusterId", clusterId);
        Filter filter = new Filter(KubernetesClusterNodeGroupVO.class, "defaultGroup", Boolean.FALSE, null, null);
        filter.addOrderBy(KubernetesClusterNodeGroupVO.class, "id", Boolean.TRUE);
        return listBy(sc, filter);
    }

    @Override
    public KubernetesClusterNodeGroupVO findDefaultByClusterId(long clusterId) {
        SearchCriteria<KubernetesClusterNodeGroupVO> sc = clusterIdSearch.create();
        sc.setParameters("clusterId", clusterId);
        sc.setParameters("defaultGroup", true);
        return findOneBy(sc);
    }

    @Override
    public List<KubernetesClusterNodeGroupVO> listByStates(KubernetesClusterNodeGroup.State... states) {
        SearchCriteria<KubernetesClusterNodeGroupVO> sc = stateSearch.create();
        sc.setParameters("state", (Object[]) states);
        return listBy(sc);
    }

    @Override
    public KubernetesClusterNodeGroupVO findByClusterIdAndName(long clusterId, String name) {
        SearchCriteria<KubernetesClusterNodeGroupVO> sc = clusterIdSearch.create();
        sc.setParameters("clusterId", clusterId);
        sc.setParameters("name", name);
        return findOneBy(sc);
    }

    @Override
    public int removeByClusterId(long clusterId) {
        SearchCriteria<KubernetesClusterNodeGroupVO> sc = clusterIdSearch.create();
        sc.setParameters("clusterId", clusterId);
        return remove(sc);
    }
}
