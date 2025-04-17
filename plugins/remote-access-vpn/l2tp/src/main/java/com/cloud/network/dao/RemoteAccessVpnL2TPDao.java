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

import com.cloud.network.RemoteAccessVpn;
import com.cloud.utils.db.GenericDaoBase;
import com.cloud.utils.db.SearchBuilder;
import com.cloud.utils.db.SearchCriteria;
import org.apache.log4j.Logger;

import java.util.List;
import java.util.stream.Collectors;

public class RemoteAccessVpnL2TPDao extends GenericDaoBase<RemoteAccessVpnL2TPVO, Long> {

    private static final Logger s_logger = Logger.getLogger(RemoteAccessVpnL2TPDao.class);

    private final SearchBuilder<RemoteAccessVpnL2TPVO> AllFieldsSearch;

    public RemoteAccessVpnL2TPDao() {
        AllFieldsSearch = createSearchBuilder();
        AllFieldsSearch.and("accountId", AllFieldsSearch.entity().getAccountId(), SearchCriteria.Op.EQ);
        AllFieldsSearch.and("networkId", AllFieldsSearch.entity().getNetworkId(), SearchCriteria.Op.EQ);
        AllFieldsSearch.and("vpcId", AllFieldsSearch.entity().getVpcId(), SearchCriteria.Op.EQ);
        AllFieldsSearch.and("ipAddress", AllFieldsSearch.entity().getServerAddressId(), SearchCriteria.Op.EQ);
        AllFieldsSearch.and("state", AllFieldsSearch.entity().getState(), SearchCriteria.Op.EQ);
        AllFieldsSearch.done();
    }


    public RemoteAccessVpn findByPublicIpAddressAndPort(long ipAddressId, Integer port) {

        SearchCriteria<RemoteAccessVpnL2TPVO> sc = AllFieldsSearch.create();
        sc.setParameters("ipAddress", ipAddressId);
        sc.setParameters("port", port);
        return findOneBy(sc);
    }

    public RemoteAccessVpn findByPublicIpAddressAndState(long ipAddressId, RemoteAccessVpn.State state) {
        return null;
    }

    public RemoteAccessVpn findByAccountNetworkAndPort(Long accountId, Long networkId, Integer port) {
        SearchCriteria<RemoteAccessVpnL2TPVO> sc = AllFieldsSearch.create();
        sc.setParameters("accountId", accountId);
        sc.setParameters("networkId", networkId);
        sc.setParameters("port", port);
        return findOneBy(sc);
    }

    public RemoteAccessVpn findByAccountVpcAndPort(Long accountId, Long vpcId, Integer port) {
        SearchCriteria<RemoteAccessVpnL2TPVO> sc = AllFieldsSearch.create();
        sc.setParameters("accountId", accountId);
        sc.setParameters("vpcId", vpcId);
        sc.setParameters("port", port);
        return findOneBy(sc);
    }

    public List<RemoteAccessVpn> findByAccount(Long accountId) {
        SearchCriteria<RemoteAccessVpnL2TPVO> sc = AllFieldsSearch.create();
        sc.setParameters("accountId", accountId);
        return listBy(sc)
                .stream()
                .map(vpn -> (RemoteAccessVpn) vpn)
                .collect(Collectors.toList());
    }

    public List<RemoteAccessVpn> listByNetworkId(Long networkId) {
        SearchCriteria<RemoteAccessVpnL2TPVO> sc = AllFieldsSearch.create();
        sc.setParameters("networkId", networkId);
        return listBy(sc)
                .stream()
                .map(vpn -> (RemoteAccessVpn) vpn)
                .collect(Collectors.toList());
    }
}
