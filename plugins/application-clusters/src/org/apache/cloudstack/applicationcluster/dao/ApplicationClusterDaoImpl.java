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

import org.apache.cloudstack.applicationcluster.ApplicationCluster;
import org.apache.cloudstack.applicationcluster.ApplicationClusterVO;
import com.cloud.utils.db.SearchBuilder;
import com.cloud.utils.db.SearchCriteria;

import org.springframework.stereotype.Component;

import com.cloud.utils.db.GenericDaoBase;
import com.cloud.utils.db.TransactionLegacy;

import java.util.List;

@Component
public class ApplicationClusterDaoImpl extends GenericDaoBase<ApplicationClusterVO, Long> implements ApplicationClusterDao {

    private final SearchBuilder<ApplicationClusterVO> AccountIdSearch;
    private final SearchBuilder<ApplicationClusterVO> GarbageCollectedSearch;
    private final SearchBuilder<ApplicationClusterVO> StateSearch;
    private final SearchBuilder<ApplicationClusterVO> SameNetworkSearch;

    public ApplicationClusterDaoImpl() {
        AccountIdSearch = createSearchBuilder();
        AccountIdSearch.and("account", AccountIdSearch.entity().getAccountId(), SearchCriteria.Op.EQ);
        AccountIdSearch.done();

        GarbageCollectedSearch = createSearchBuilder();
        GarbageCollectedSearch.and("gc", GarbageCollectedSearch.entity().ischeckForGc(), SearchCriteria.Op.EQ);
        GarbageCollectedSearch.and("state", GarbageCollectedSearch.entity().getState(), SearchCriteria.Op.NEQ);
        GarbageCollectedSearch.done();

        StateSearch = createSearchBuilder();
        StateSearch.and("state", StateSearch.entity().getState(), SearchCriteria.Op.EQ);
        StateSearch.done();

        SameNetworkSearch = createSearchBuilder();
        SameNetworkSearch.and("network_id", SameNetworkSearch.entity().getNetworkId(), SearchCriteria.Op.EQ);
        SameNetworkSearch.done();
    }

    @Override
    public List<ApplicationClusterVO> listByAccount(long accountId) {
        SearchCriteria<ApplicationClusterVO> sc = AccountIdSearch.create();
        sc.setParameters("account", accountId);
        return listBy(sc, null);
    }

    @Override
    public List<ApplicationClusterVO> findClustersToGarbageCollect() {
        SearchCriteria<ApplicationClusterVO> sc = GarbageCollectedSearch.create();
        sc.setParameters("gc", true);
        sc.setParameters("state", ApplicationCluster.State.Destroying);
        return listBy(sc);
    }

    @Override
    public List<ApplicationClusterVO> findClustersInState(ApplicationCluster.State state) {
        SearchCriteria<ApplicationClusterVO> sc = StateSearch.create();
        sc.setParameters("state", state);
        return listBy(sc);
    }

    @Override
    public boolean updateState(ApplicationCluster.State currentState, ApplicationCluster.Event event, ApplicationCluster.State nextState,
                               ApplicationCluster vo, Object data) {
        // TODO: ensure this update is correct
        TransactionLegacy txn = TransactionLegacy.currentTxn();
        txn.start();

        ApplicationClusterVO ccVo = (ApplicationClusterVO)vo;
        ccVo.setState(nextState);
        super.update(ccVo.getId(), ccVo);

        txn.commit();
        return true;
    }

    public List<ApplicationClusterVO> listByNetworkId(long networkId) {
        SearchCriteria<ApplicationClusterVO> sc = SameNetworkSearch.create();
        sc.setParameters("network_id", networkId);
        return this.listBy(sc);
    }
}
