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
    final SearchBuilder<TungstenGuestNetworkIpAddressVO> allFieldsSearch;
    final GenericSearchBuilder<TungstenGuestNetworkIpAddressVO, String> networkSearch;

    public TungstenGuestNetworkIpAddressDaoImpl() {
        allFieldsSearch = createSearchBuilder();
        allFieldsSearch.and("id", allFieldsSearch.entity().getId(), SearchCriteria.Op.EQ);
        allFieldsSearch.and("network_id", allFieldsSearch.entity().getNetworkId(), SearchCriteria.Op.EQ);
        allFieldsSearch.and("guest_ip_address", allFieldsSearch.entity().getGuestIpAddress(), SearchCriteria.Op.EQ);
        allFieldsSearch.and("public_ip_address", allFieldsSearch.entity().getPublicIpAddress(), SearchCriteria.Op.EQ);
        allFieldsSearch.and("logical_router_uuid", allFieldsSearch.entity().getLogicalRouterUuid(), SearchCriteria.Op.EQ);
        allFieldsSearch.done();
        networkSearch = createSearchBuilder(String.class);
        networkSearch.select(null, SearchCriteria.Func.DISTINCT, networkSearch.entity().getGuestIpAddress());
        networkSearch.and("network_id", networkSearch.entity().getNetworkId(), SearchCriteria.Op.EQ);
        networkSearch.done();
    }

    @Override
    public List<String> listGuestIpAddressByNetworkId(long networkId) {
        SearchCriteria<String> searchCriteria = networkSearch.create();
        searchCriteria.setParameters("network_id", networkId);
        return customSearch(searchCriteria, null);
    }

    @Override
    public List<TungstenGuestNetworkIpAddressVO> listByNetworkId(final long networkId) {
        SearchCriteria<TungstenGuestNetworkIpAddressVO> searchCriteria = allFieldsSearch.create();
        searchCriteria.setParameters("network_id", networkId);
        return listBy(searchCriteria);
    }

    @Override
    public TungstenGuestNetworkIpAddressVO findByNetworkIdAndPublicIp(final long networkId, final String publicIp) {
        SearchCriteria<TungstenGuestNetworkIpAddressVO> searchCriteria = allFieldsSearch.create();
        searchCriteria.setParameters("network_id", networkId);
        searchCriteria.setParameters("public_ip_address", publicIp);
        return findOneBy(searchCriteria);
    }

    @Override
    public TungstenGuestNetworkIpAddressVO findByNetworkAndGuestIpAddress(final long networkId, final String guestIp) {
        SearchCriteria<TungstenGuestNetworkIpAddressVO> searchCriteria = allFieldsSearch.create();
        searchCriteria.setParameters("network_id", networkId);
        searchCriteria.setParameters("guest_ip_address", guestIp);
        return findOneBy(searchCriteria);
    }

    @Override
    public TungstenGuestNetworkIpAddressVO findByNetworkAndLogicalRouter(final long networkId,
        final String logicalRouterUuid) {
        SearchCriteria<TungstenGuestNetworkIpAddressVO> searchCriteria = allFieldsSearch.create();
        searchCriteria.setParameters("network_id", networkId);
        searchCriteria.setParameters("logical_router_uuid", logicalRouterUuid);
        return findOneBy(searchCriteria);
    }
}
