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

import javax.annotation.PostConstruct;

import org.apache.commons.collections.CollectionUtils;
import org.springframework.stereotype.Component;

import com.cloud.network.Ipv6GuestPrefixSubnetNetworkMap;
import com.cloud.network.PublicIpv6AddressNetworkMapVO;
import com.cloud.utils.db.DB;
import com.cloud.utils.db.GenericDaoBase;
import com.cloud.utils.db.SearchBuilder;
import com.cloud.utils.db.SearchCriteria;

@Component
@DB
public class PublicIpv6AddressNetworkMapDaoImpl extends GenericDaoBase<PublicIpv6AddressNetworkMapVO, Long> implements PublicIpv6AddressNetworkMapDao {

    protected SearchBuilder<PublicIpv6AddressNetworkMapVO> FreeAddressSearch;
    protected SearchBuilder<PublicIpv6AddressNetworkMapVO> RangeIdSearch;
    protected SearchBuilder<PublicIpv6AddressNetworkMapVO> NetworkIdSearch;

    @PostConstruct
    public void init() {
        FreeAddressSearch = createSearchBuilder();
        FreeAddressSearch.and("prefixId", FreeAddressSearch.entity().getRangeId(), SearchCriteria.Op.EQ);
        FreeAddressSearch.and("state", FreeAddressSearch.entity().getState(), SearchCriteria.Op.EQ);
        FreeAddressSearch.done();
        RangeIdSearch = createSearchBuilder();
        RangeIdSearch.and("prefixId", RangeIdSearch.entity().getRangeId(), SearchCriteria.Op.EQ);
        RangeIdSearch.done();
        NetworkIdSearch = createSearchBuilder();
        NetworkIdSearch.and("networkId", NetworkIdSearch.entity().getNetworkId(), SearchCriteria.Op.EQ);
        NetworkIdSearch.done();
    }

    @Override
    public PublicIpv6AddressNetworkMapVO findFirstAvailable(long prefixId) {
        SearchCriteria<PublicIpv6AddressNetworkMapVO> sc = FreeAddressSearch.create();
        sc.setParameters("prefixId", prefixId);
        sc.setParameters("state", Ipv6GuestPrefixSubnetNetworkMap.State.Free);
        return findOneBy(sc);
    }

    @Override
    public PublicIpv6AddressNetworkMapVO findLast(long prefixId) {
        SearchCriteria<PublicIpv6AddressNetworkMapVO> sc = RangeIdSearch.create();
        sc.setParameters("prefixId", prefixId);
        List<PublicIpv6AddressNetworkMapVO> list = listBy(sc);
        return CollectionUtils.isNotEmpty(list) ? list.get(list.size() - 1) : null;
    }

    @Override
    public PublicIpv6AddressNetworkMapVO findByNetworkId(long networkId) {
        SearchCriteria<PublicIpv6AddressNetworkMapVO> sc = NetworkIdSearch.create();
        sc.setParameters("networkId", networkId);
        return findOneBy(sc);
    }
}