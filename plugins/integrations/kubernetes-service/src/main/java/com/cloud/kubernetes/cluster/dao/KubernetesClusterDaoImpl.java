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
import com.cloud.utils.db.TransactionLegacy;

@Component
public class KubernetesClusterDaoImpl extends GenericDaoBase<KubernetesClusterVO, Long> implements KubernetesClusterDao {

    private final SearchBuilder<KubernetesClusterVO> AccountIdSearch;
    private final SearchBuilder<KubernetesClusterVO> GarbageCollectedSearch;
    private final SearchBuilder<KubernetesClusterVO> ManagedStateSearch;
    private final SearchBuilder<KubernetesClusterVO> SameNetworkSearch;
    private final SearchBuilder<KubernetesClusterVO> KubernetesVersionSearch;

    public KubernetesClusterDaoImpl() {
        AccountIdSearch = createSearchBuilder();
        AccountIdSearch.and("account", AccountIdSearch.entity().getAccountId(), SearchCriteria.Op.EQ);
        AccountIdSearch.done();

        GarbageCollectedSearch = createSearchBuilder();
        GarbageCollectedSearch.and("gc", GarbageCollectedSearch.entity().isCheckForGc(), SearchCriteria.Op.EQ);
        GarbageCollectedSearch.and("state", GarbageCollectedSearch.entity().getState(), SearchCriteria.Op.EQ);
        GarbageCollectedSearch.and("cluster_type", GarbageCollectedSearch.entity().getClusterType(), SearchCriteria.Op.EQ);
        GarbageCollectedSearch.done();

        ManagedStateSearch = createSearchBuilder();
        ManagedStateSearch.and("state", ManagedStateSearch.entity().getState(), SearchCriteria.Op.EQ);
        ManagedStateSearch.and("cluster_type", ManagedStateSearch.entity().getClusterType(), SearchCriteria.Op.EQ);
        ManagedStateSearch.done();

        SameNetworkSearch = createSearchBuilder();
        SameNetworkSearch.and("network_id", SameNetworkSearch.entity().getNetworkId(), SearchCriteria.Op.EQ);
        SameNetworkSearch.done();

        KubernetesVersionSearch = createSearchBuilder();
        KubernetesVersionSearch.and("kubernetesVersionId", KubernetesVersionSearch.entity().getKubernetesVersionId(), SearchCriteria.Op.EQ);
        KubernetesVersionSearch.done();
    }

    @Override
    public List<KubernetesClusterVO> listByAccount(long accountId) {
        SearchCriteria<KubernetesClusterVO> sc = AccountIdSearch.create();
        sc.setParameters("account", accountId);
        return listBy(sc, null);
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
    public boolean updateState(KubernetesCluster.State currentState, KubernetesCluster.Event event, KubernetesCluster.State nextState,
                               KubernetesCluster vo, Object data) {
        // TODO: ensure this update is correct
        TransactionLegacy txn = TransactionLegacy.currentTxn();
        txn.start();

        KubernetesClusterVO ccVo = (KubernetesClusterVO)vo;
        ccVo.setState(nextState);
        super.update(ccVo.getId(), ccVo);

        txn.commit();
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
