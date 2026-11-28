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

import com.cloud.kubernetes.cluster.KubernetesCluster;
import com.cloud.kubernetes.cluster.KubernetesClusterVO;
import com.cloud.utils.db.GenericDaoBase;
import com.cloud.utils.db.SearchBuilder;
import com.cloud.utils.db.SearchCriteria;
import com.cloud.utils.db.UpdateBuilder;

@Component
public class KubernetesClusterDaoImpl extends GenericDaoBase<KubernetesClusterVO, Long> implements KubernetesClusterDao {

    private final SearchBuilder<KubernetesClusterVO> CleanupAccountIdSearch;
    private final SearchBuilder<KubernetesClusterVO> NotForGCByAccountIDCount;
    private final SearchBuilder<KubernetesClusterVO> GarbageCollectedSearch;
    private final SearchBuilder<KubernetesClusterVO> ManagedStateSearch;
    private final SearchBuilder<KubernetesClusterVO> SameNetworkSearch;
    private final SearchBuilder<KubernetesClusterVO> KubernetesVersionSearch;
    private final SearchBuilder<KubernetesClusterVO> StateChangeSearch;
    private final SearchBuilder<KubernetesClusterVO> ManagedStatesSearch;

    public KubernetesClusterDaoImpl() {
        CleanupAccountIdSearch = createSearchBuilder();
        CleanupAccountIdSearch.and("account", CleanupAccountIdSearch.entity().getAccountId(), SearchCriteria.Op.EQ);
        CleanupAccountIdSearch.and("cluster_type", CleanupAccountIdSearch.entity().getClusterType(), SearchCriteria.Op.EQ);
        CleanupAccountIdSearch.done();

        NotForGCByAccountIDCount = createSearchBuilder();
        NotForGCByAccountIDCount.and("gc", NotForGCByAccountIDCount.entity().isCheckForGc(), SearchCriteria.Op.EQ);
        NotForGCByAccountIDCount.and("account", NotForGCByAccountIDCount.entity().getAccountId(), SearchCriteria.Op.EQ);
        NotForGCByAccountIDCount.and("cluster_type", NotForGCByAccountIDCount.entity().getClusterType(), SearchCriteria.Op.EQ);
        NotForGCByAccountIDCount.select(null, SearchCriteria.Func.COUNT, null);
        NotForGCByAccountIDCount.done();

        GarbageCollectedSearch = createSearchBuilder();
        GarbageCollectedSearch.and("gc", GarbageCollectedSearch.entity().isCheckForGc(), SearchCriteria.Op.EQ);
        GarbageCollectedSearch.and("state", GarbageCollectedSearch.entity().getState(), SearchCriteria.Op.EQ);
        GarbageCollectedSearch.and("cluster_type", GarbageCollectedSearch.entity().getClusterType(), SearchCriteria.Op.EQ);
        GarbageCollectedSearch.done();

        ManagedStateSearch = createSearchBuilder();
        ManagedStateSearch.and("state", ManagedStateSearch.entity().getState(), SearchCriteria.Op.EQ);
        ManagedStateSearch.and("cluster_type", ManagedStateSearch.entity().getClusterType(), SearchCriteria.Op.EQ);
        ManagedStateSearch.done();

        ManagedStatesSearch = createSearchBuilder();
        ManagedStatesSearch.and("state", ManagedStatesSearch.entity().getState(), SearchCriteria.Op.IN);
        ManagedStatesSearch.and("cluster_type", ManagedStatesSearch.entity().getClusterType(), SearchCriteria.Op.EQ);
        ManagedStatesSearch.done();

        SameNetworkSearch = createSearchBuilder();
        SameNetworkSearch.and("network_id", SameNetworkSearch.entity().getNetworkId(), SearchCriteria.Op.EQ);
        SameNetworkSearch.done();

        KubernetesVersionSearch = createSearchBuilder();
        KubernetesVersionSearch.and("kubernetesVersionId", KubernetesVersionSearch.entity().getKubernetesVersionId(), SearchCriteria.Op.EQ);
        KubernetesVersionSearch.done();

        StateChangeSearch = createSearchBuilder();
        StateChangeSearch.and("id", StateChangeSearch.entity().getId(), SearchCriteria.Op.EQ);
        StateChangeSearch.and("state", StateChangeSearch.entity().getState(), SearchCriteria.Op.EQ);
        StateChangeSearch.done();
    }

    @Override
    public List<KubernetesClusterVO> listForCleanupByAccount(long accountId) {
        SearchCriteria<KubernetesClusterVO> sc = CleanupAccountIdSearch.create();
        sc.setParameters("cluster_type", KubernetesCluster.ClusterType.CloudManaged);
        sc.setParameters("account", accountId);
        return listBy(sc);
    }

    @Override
    public int countNotForGCByAccount(long accountId) {
        SearchCriteria<KubernetesClusterVO> sc = NotForGCByAccountIDCount.create();
        sc.setParameters("cluster_type", KubernetesCluster.ClusterType.CloudManaged);
        sc.setParameters("account", accountId);
        sc.setParameters("gc", false);
        return getCount(sc);
    }

    @Override
    public List<KubernetesClusterVO> findKubernetesClustersToGarbageCollect() {
        SearchCriteria<KubernetesClusterVO> sc = GarbageCollectedSearch.create();
        sc.setParameters("gc", true);
        sc.setParameters("state", KubernetesCluster.State.Destroying);
        sc.setParameters("cluster_type", KubernetesCluster.ClusterType.CloudManaged);
        return listBy(sc);
    }

    @Override
    public List<KubernetesClusterVO> findManagedKubernetesClustersInState(KubernetesCluster.State state) {
        SearchCriteria<KubernetesClusterVO> sc = ManagedStateSearch.create();
        sc.setParameters("state", state);
        sc.setParameters("cluster_type", KubernetesCluster.ClusterType.CloudManaged);
        return listBy(sc);
    }

    @Override
    public List<KubernetesClusterVO> findManagedKubernetesClustersInStates(List<KubernetesCluster.State> states) {
        SearchCriteria<KubernetesClusterVO> sc = ManagedStatesSearch.create();
        sc.setParameters("state", states.toArray());
        sc.setParameters("cluster_type", KubernetesCluster.ClusterType.CloudManaged);
        return listBy(sc);
    }

    /**
     * Moves the cluster to the next state only if it is still in the given current state, so that of two operations
     * started on the same cluster at once only one gets past its first transition.
     */
    @Override
    public boolean updateState(KubernetesCluster.State currentState, KubernetesCluster.Event event, KubernetesCluster.State nextState,
                               KubernetesCluster vo, Object data) {
        KubernetesClusterVO ccVo = (KubernetesClusterVO)vo;
        SearchCriteria<KubernetesClusterVO> sc = StateChangeSearch.create();
        sc.setParameters("id", ccVo.getId());
        sc.setParameters("state", currentState);

        UpdateBuilder builder = getUpdateBuilder(ccVo);
        builder.set(ccVo, "state", nextState);
        int rows = update(builder, sc, null);
        if (rows == 0) {
            // the builder set the next state on the cluster object too: put back the state the caller read, which
            // the database no longer has either, without leaving it as a change for a later update to write
            builder.set(ccVo, "state", currentState);
            builder.clear();
            if (!logger.isDebugEnabled()) {
                return false;
            }
            KubernetesClusterVO dbCluster = findByIdIncludingRemoved(ccVo.getId());
            logger.debug("Unable to move Kubernetes cluster {} from state {} to {} on event {}, its state is now {}", ccVo, currentState, nextState, event,
                    dbCluster == null ? null : dbCluster.getState());
            return false;
        }
        return true;
    }

    @Override
    public List<KubernetesClusterVO> listByNetworkId(long networkId) {
        SearchCriteria<KubernetesClusterVO> sc = SameNetworkSearch.create();
        sc.setParameters("network_id", networkId);
        return this.listBy(sc);
    }

    @Override
    public List<KubernetesClusterVO> listAllByKubernetesVersion(long kubernetesVersionId) {
        SearchCriteria<KubernetesClusterVO> sc = KubernetesVersionSearch.create();
        sc.setParameters("kubernetesVersionId", kubernetesVersionId);
        return this.listBy(sc);
    }
}
