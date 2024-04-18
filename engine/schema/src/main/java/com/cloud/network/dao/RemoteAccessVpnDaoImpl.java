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
package com.cloud.network.dao;

import java.util.List;


import org.springframework.stereotype.Component;

import com.cloud.network.RemoteAccessVpn;
import com.cloud.utils.db.GenericDaoBase;
import com.cloud.utils.db.SearchBuilder;
import com.cloud.utils.db.SearchCriteria;

@Component
public class RemoteAccessVpnDaoImpl extends GenericDaoBase<RemoteAccessVpnVO, Long> implements RemoteAccessVpnDao {

    private final SearchBuilder<RemoteAccessVpnVO> AllFieldsSearch;

    protected RemoteAccessVpnDaoImpl() {
        AllFieldsSearch = createSearchBuilder();
        AllFieldsSearch.and("accountId", AllFieldsSearch.entity().getAccountId(), SearchCriteria.Op.EQ);
        AllFieldsSearch.and("networkId", AllFieldsSearch.entity().getNetworkId(), SearchCriteria.Op.EQ);
        AllFieldsSearch.and("vpcId", AllFieldsSearch.entity().getVpcId(), SearchCriteria.Op.EQ);
        AllFieldsSearch.and("ipAddress", AllFieldsSearch.entity().getServerAddressId(), SearchCriteria.Op.EQ);
        AllFieldsSearch.and("state", AllFieldsSearch.entity().getState(), SearchCriteria.Op.EQ);
        AllFieldsSearch.done();
    }

    @Override
    public RemoteAccessVpnVO findByPublicIpAddress(long ipAddressId) {
        SearchCriteria<RemoteAccessVpnVO> sc = AllFieldsSearch.create();
        sc.setParameters("ipAddress", ipAddressId);
        return findOneBy(sc);
    }

    @Override
    public RemoteAccessVpnVO findByAccountAndNetwork(Long accountId, Long networkId) {
        SearchCriteria<RemoteAccessVpnVO> sc = AllFieldsSearch.create();
        sc.setParameters("accountId", accountId);
        sc.setParameters("networkId", networkId);
        return findOneBy(sc);
    }

    @Override
    public RemoteAccessVpnVO findByAccountAndVpc(Long accountId, Long vpcId) {
        SearchCriteria<RemoteAccessVpnVO> sc = AllFieldsSearch.create();
        sc.setParameters("accountId", accountId);
        sc.setParameters("vpcId", vpcId);
        return findOneBy(sc);
    }

    @Override
    public List<RemoteAccessVpnVO> findByAccount(Long accountId) {
        SearchCriteria<RemoteAccessVpnVO> sc = AllFieldsSearch.create();
        sc.setParameters("accountId", accountId);
        return listBy(sc);
    }

    @Override
    public RemoteAccessVpnVO findByPublicIpAddressAndState(long ipAddressId, RemoteAccessVpn.State state) {
        SearchCriteria<RemoteAccessVpnVO> sc = AllFieldsSearch.create();
        sc.setParameters("ipAddress", ipAddressId);
        sc.setParameters("state", state);
        return findOneBy(sc);
    }

    @Override
    public List<RemoteAccessVpnVO> listByNetworkId(Long networkId) {
        SearchCriteria<RemoteAccessVpnVO> sc = AllFieldsSearch.create();
        sc.setParameters("networkId", networkId);
        return listBy(sc);
    }
}
