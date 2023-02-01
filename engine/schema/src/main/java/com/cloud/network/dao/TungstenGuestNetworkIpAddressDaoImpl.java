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

import com.cloud.network.TungstenGuestNetworkIpAddressVO;
import com.cloud.utils.db.DB;
import com.cloud.utils.db.GenericDaoBase;
import com.cloud.utils.db.GenericSearchBuilder;
import com.cloud.utils.db.SearchBuilder;
import com.cloud.utils.db.SearchCriteria;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@DB
public class TungstenGuestNetworkIpAddressDaoImpl extends GenericDaoBase<TungstenGuestNetworkIpAddressVO, Long>
    implements TungstenGuestNetworkIpAddressDao {
    private static final String ID = "id";
    private static final String NETWORK_ID = "network_id";
    private static final String GUEST_IP_ADDRESS = "guest_ip_address";
    private static final String PUBLIC_IP_ADDRESS = "public_ip_address";
    private static final String LOGICAL_ROUTER_UUID = "logical_router_uuid";
    final SearchBuilder<TungstenGuestNetworkIpAddressVO> allFieldsSearch;
    final GenericSearchBuilder<TungstenGuestNetworkIpAddressVO, String> networkSearch;

    public TungstenGuestNetworkIpAddressDaoImpl() {
        allFieldsSearch = createSearchBuilder();
        allFieldsSearch.and(ID, allFieldsSearch.entity().getId(), SearchCriteria.Op.EQ);
        allFieldsSearch.and(NETWORK_ID, allFieldsSearch.entity().getNetworkId(), SearchCriteria.Op.EQ);
        allFieldsSearch.and(GUEST_IP_ADDRESS, allFieldsSearch.entity().getGuestIpAddress(), SearchCriteria.Op.EQ);
        allFieldsSearch.and(PUBLIC_IP_ADDRESS, allFieldsSearch.entity().getPublicIpAddress(), SearchCriteria.Op.EQ);
        allFieldsSearch.and(LOGICAL_ROUTER_UUID, allFieldsSearch.entity().getLogicalRouterUuid(), SearchCriteria.Op.EQ);
        allFieldsSearch.done();
        networkSearch = createSearchBuilder(String.class);
        networkSearch.select(null, SearchCriteria.Func.DISTINCT, networkSearch.entity().getGuestIpAddress());
        networkSearch.and(NETWORK_ID, networkSearch.entity().getNetworkId(), SearchCriteria.Op.EQ);
        networkSearch.done();
    }

    @Override
    public List<String> listGuestIpAddressByNetworkId(long networkId) {
        SearchCriteria<String> searchCriteria = networkSearch.create();
        searchCriteria.setParameters(NETWORK_ID, networkId);
        return customSearch(searchCriteria, null);
    }

    @Override
    public List<TungstenGuestNetworkIpAddressVO> listByNetworkId(final long networkId) {
        SearchCriteria<TungstenGuestNetworkIpAddressVO> searchCriteria = allFieldsSearch.create();
        searchCriteria.setParameters(NETWORK_ID, networkId);
        return listBy(searchCriteria);
    }

    @Override
    public TungstenGuestNetworkIpAddressVO findByNetworkIdAndPublicIp(final long networkId, final String publicIp) {
        SearchCriteria<TungstenGuestNetworkIpAddressVO> searchCriteria = allFieldsSearch.create();
        searchCriteria.setParameters(NETWORK_ID, networkId);
        searchCriteria.setParameters(PUBLIC_IP_ADDRESS, publicIp);
        return findOneBy(searchCriteria);
    }

    @Override
    public TungstenGuestNetworkIpAddressVO findByNetworkAndGuestIpAddress(final long networkId, final String guestIp) {
        SearchCriteria<TungstenGuestNetworkIpAddressVO> searchCriteria = allFieldsSearch.create();
        searchCriteria.setParameters(NETWORK_ID, networkId);
        searchCriteria.setParameters(GUEST_IP_ADDRESS, guestIp);
        return findOneBy(searchCriteria);
    }

    @Override
    public TungstenGuestNetworkIpAddressVO findByNetworkAndLogicalRouter(final long networkId,
        final String logicalRouterUuid) {
        SearchCriteria<TungstenGuestNetworkIpAddressVO> searchCriteria = allFieldsSearch.create();
        searchCriteria.setParameters(NETWORK_ID, networkId);
        searchCriteria.setParameters(LOGICAL_ROUTER_UUID, logicalRouterUuid);
        return findOneBy(searchCriteria);
    }
}
