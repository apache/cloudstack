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
package org.apache.cloudstack.network.dao;

import java.util.ArrayList;
import java.util.List;

import org.springframework.stereotype.Component;

import org.apache.cloudstack.network.NetworkPermissionVO;

import com.cloud.utils.db.GenericDaoBase;
import com.cloud.utils.db.GenericSearchBuilder;
import com.cloud.utils.db.SearchBuilder;
import com.cloud.utils.db.SearchCriteria;

@Component
public class NetworkPermissionDaoImpl extends GenericDaoBase<NetworkPermissionVO, Long> implements NetworkPermissionDao {

    private SearchBuilder<NetworkPermissionVO> NetworkAndAccountSearch;
    private SearchBuilder<NetworkPermissionVO> NetworkIdSearch;
    private GenericSearchBuilder<NetworkPermissionVO, Long> FindNetworkIdsByAccount;

    protected NetworkPermissionDaoImpl() {
        NetworkAndAccountSearch = createSearchBuilder();
        NetworkAndAccountSearch.and("networkId", NetworkAndAccountSearch.entity().getNetworkId(), SearchCriteria.Op.EQ);
        NetworkAndAccountSearch.and("accountId", NetworkAndAccountSearch.entity().getAccountId(), SearchCriteria.Op.IN);
        NetworkAndAccountSearch.done();

        NetworkIdSearch = createSearchBuilder();
        NetworkIdSearch.and("networkId", NetworkIdSearch.entity().getNetworkId(), SearchCriteria.Op.EQ);
        NetworkIdSearch.done();

        FindNetworkIdsByAccount = createSearchBuilder(Long.class);
        FindNetworkIdsByAccount.select(null, SearchCriteria.Func.DISTINCT, FindNetworkIdsByAccount.entity().getNetworkId());
        FindNetworkIdsByAccount.and("account", FindNetworkIdsByAccount.entity().getAccountId(), SearchCriteria.Op.IN);
        FindNetworkIdsByAccount.done();
    }

    @Override
    public void removePermissions(long networkId, List<Long> accountIds) {
        if (accountIds.isEmpty()) {
            return;
        }
        SearchCriteria<NetworkPermissionVO> sc = NetworkAndAccountSearch.create();
        sc.setParameters("networkId", networkId);
        sc.setParameters("accountId", accountIds.toArray());
        expunge(sc);
    }

    @Override
    public void removeAllPermissions(long networkId) {
        SearchCriteria<NetworkPermissionVO> sc = NetworkIdSearch.create();
        sc.setParameters("networkId", networkId);
        expunge(sc);
    }

    @Override
    public NetworkPermissionVO findByNetworkAndAccount(long networkId, long accountId) {
        SearchCriteria<NetworkPermissionVO> sc = NetworkAndAccountSearch.create();
        sc.setParameters("networkId", networkId);
        sc.setParameters("accountId", accountId);
        return findOneBy(sc);
    }

    @Override
    public List<NetworkPermissionVO> findByNetwork(long networkId) {
        SearchCriteria<NetworkPermissionVO> sc = NetworkIdSearch.create();
        sc.setParameters("networkId", networkId);
        return listBy(sc);
    }

    @Override
    public List<Long> listPermittedNetworkIdsByAccounts(List<Long> permittedAccounts) {
        SearchCriteria<Long> sc = FindNetworkIdsByAccount.create();
        if (permittedAccounts != null && !permittedAccounts.isEmpty()) {
            sc.setParameters("account", permittedAccounts.toArray());
            return customSearch(sc, null);
        }
        return new ArrayList<Long>();
    }
}
