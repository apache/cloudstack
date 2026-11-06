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

import javax.inject.Inject;

import com.cloud.kubernetes.cluster.KubernetesClusterNodeGroupVO;
import com.cloud.kubernetes.cluster.KubernetesServiceHelper;
import org.springframework.stereotype.Component;

import com.cloud.kubernetes.cluster.KubernetesClusterVmMapVO;
import com.cloud.utils.db.Filter;
import com.cloud.utils.db.GenericDaoBase;
import com.cloud.utils.db.SearchBuilder;
import com.cloud.utils.db.SearchCriteria;

import static com.cloud.kubernetes.cluster.KubernetesServiceHelper.KubernetesClusterNodeType.CONTROL;
import static com.cloud.kubernetes.cluster.KubernetesServiceHelper.KubernetesClusterNodeType.ETCD;


@Component
public class KubernetesClusterVmMapDaoImpl extends GenericDaoBase<KubernetesClusterVmMapVO, Long> implements KubernetesClusterVmMapDao {

    @Inject
    private KubernetesClusterNodeGroupDao kubernetesClusterNodeGroupDao;

    private final SearchBuilder<KubernetesClusterVmMapVO> clusterIdSearch;
    private final SearchBuilder<KubernetesClusterVmMapVO> vmIdSearch;
    private final SearchBuilder<KubernetesClusterVmMapVO> defaultPoolWorkerSearch;
    private final SearchBuilder<KubernetesClusterVmMapVO> nodeGroupSearch;

    public KubernetesClusterVmMapDaoImpl() {
        clusterIdSearch = createSearchBuilder();
        clusterIdSearch.and("clusterId", clusterIdSearch.entity().getClusterId(), SearchCriteria.Op.EQ);
        clusterIdSearch.and("vmIdsIN", clusterIdSearch.entity().getVmId(), SearchCriteria.Op.IN);
        clusterIdSearch.and("controlNode", clusterIdSearch.entity().isControlNode(), SearchCriteria.Op.EQ);
        clusterIdSearch.and("etcdNode", clusterIdSearch.entity().isEtcdNode(), SearchCriteria.Op.EQ);
        clusterIdSearch.done();

        vmIdSearch = createSearchBuilder();
        vmIdSearch.and("vmId", vmIdSearch.entity().getVmId(), SearchCriteria.Op.EQ);
        vmIdSearch.done();

        defaultPoolWorkerSearch = createSearchBuilder();
        defaultPoolWorkerSearch.and("clusterId", defaultPoolWorkerSearch.entity().getClusterId(), SearchCriteria.Op.EQ);
        defaultPoolWorkerSearch.and("controlNode", defaultPoolWorkerSearch.entity().isControlNode(), SearchCriteria.Op.EQ);
        defaultPoolWorkerSearch.and("etcdNode", defaultPoolWorkerSearch.entity().isEtcdNode(), SearchCriteria.Op.EQ);
        defaultPoolWorkerSearch.and().op("noNodeGroup", defaultPoolWorkerSearch.entity().getNodeGroupId(), SearchCriteria.Op.NULL);
        defaultPoolWorkerSearch.or("defaultNodeGroupId", defaultPoolWorkerSearch.entity().getNodeGroupId(), SearchCriteria.Op.EQ);
        defaultPoolWorkerSearch.cp();
        defaultPoolWorkerSearch.done();

        nodeGroupSearch = createSearchBuilder();
        nodeGroupSearch.and("nodeGroupId", nodeGroupSearch.entity().getNodeGroupId(), SearchCriteria.Op.EQ);
        nodeGroupSearch.done();
    }

    @Override
    public List<KubernetesClusterVmMapVO> listByClusterId(long clusterId) {
        SearchCriteria<KubernetesClusterVmMapVO> sc = clusterIdSearch.create();
        sc.setParameters("clusterId", clusterId);
        Filter filter = new Filter(KubernetesClusterVmMapVO.class, "id", Boolean.TRUE, null, null);
        return listBy(sc, filter);
    }

    @Override
    public KubernetesClusterVmMapVO getClusterMapFromVmId(long vmId) {
        SearchCriteria<KubernetesClusterVmMapVO> sc = vmIdSearch.create();
        sc.setParameters("vmId", vmId);
        return findOneBy(sc);
    }

    @Override
    public List<KubernetesClusterVmMapVO> listByClusterIdAndVmIdsIn(long clusterId, List<Long> vmIds) {
        SearchCriteria<KubernetesClusterVmMapVO> sc = clusterIdSearch.create();
        sc.setParameters("clusterId", clusterId);
        sc.setParameters("vmIdsIN", vmIds.toArray());
        return listBy(sc);
    }

    @Override
    public int removeByClusterIdAndVmIdsIn(long clusterId, List<Long> vmIds) {
        SearchCriteria<KubernetesClusterVmMapVO> sc = clusterIdSearch.create();
        sc.setParameters("clusterId", clusterId);
        sc.setParameters("vmIdsIN", vmIds.toArray());
        return remove(sc);
    }

    @Override
    public int removeByClusterId(long clusterId) {
        SearchCriteria<KubernetesClusterVmMapVO> sc = clusterIdSearch.create();
        sc.setParameters("clusterId", clusterId);
        return remove(sc);
    }

    @Override
    public List<KubernetesClusterVmMapVO> listByClusterIdAndVmType(long clusterId, KubernetesServiceHelper.KubernetesClusterNodeType nodeType) {
        SearchCriteria<KubernetesClusterVmMapVO> sc = clusterIdSearch.create();
        sc.setParameters("clusterId", clusterId);
        if (CONTROL == nodeType) {
            sc.setParameters("controlNode", true);
            sc.setParameters("etcdNode", false);
        } else if (ETCD == nodeType) {
            sc.setParameters("controlNode", false);
            sc.setParameters("etcdNode", true);
        } else {
            // The worker nodes of the cluster size are the nodes of the default node group and the external nodes,
            // the nodes of the other node groups are managed through their node group
            sc = defaultPoolWorkerSearch.create();
            sc.setParameters("clusterId", clusterId);
            sc.setParameters("controlNode", false);
            sc.setParameters("etcdNode", false);
            KubernetesClusterNodeGroupVO defaultNodeGroup = kubernetesClusterNodeGroupDao.findDefaultByClusterId(clusterId);
            sc.setParameters("defaultNodeGroupId", defaultNodeGroup != null ? defaultNodeGroup.getId() : -1L);
        }
        return listBy(sc);
    }

    @Override
    public List<KubernetesClusterVmMapVO> listByNodeGroupId(long nodeGroupId) {
        SearchCriteria<KubernetesClusterVmMapVO> sc = nodeGroupSearch.create();
        sc.setParameters("nodeGroupId", nodeGroupId);
        Filter filter = new Filter(KubernetesClusterVmMapVO.class, "id", Boolean.TRUE, null, null);
        return listBy(sc, filter);
    }

    @Override
    public KubernetesClusterVmMapVO findByVmId(long vmId) {
        SearchBuilder<KubernetesClusterVmMapVO> sb = createSearchBuilder();
        sb.and("vmId", sb.entity().getVmId(), SearchCriteria.Op.EQ);
        sb.done();
        SearchCriteria<KubernetesClusterVmMapVO> sc = sb.create();
        sc.setParameters("vmId", vmId);
        return findOneBy(sc);
    }
}
