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
import com.cloud.network.Ipv6GuestPrefixSubnetNetworkMapVO;
import com.cloud.utils.db.DB;
import com.cloud.utils.db.Filter;
import com.cloud.utils.db.GenericDaoBase;
import com.cloud.utils.db.SearchBuilder;
import com.cloud.utils.db.SearchCriteria;

@Component
@DB
public class Ipv6GuestPrefixSubnetNetworkMapDaoImpl extends GenericDaoBase<Ipv6GuestPrefixSubnetNetworkMapVO, Long> implements Ipv6GuestPrefixSubnetNetworkMapDao {

    protected SearchBuilder<Ipv6GuestPrefixSubnetNetworkMapVO> PrefixStateSearch;
    protected SearchBuilder<Ipv6GuestPrefixSubnetNetworkMapVO> PrefixIdSearch;
    protected SearchBuilder<Ipv6GuestPrefixSubnetNetworkMapVO> NetworkIdSearch;
    protected SearchBuilder<Ipv6GuestPrefixSubnetNetworkMapVO> SubnetSearch;
    protected SearchBuilder<Ipv6GuestPrefixSubnetNetworkMapVO> StatesSearch;

    @PostConstruct
    public void init() {
        PrefixStateSearch = createSearchBuilder();
        PrefixStateSearch.and("prefixId", PrefixStateSearch.entity().getPrefixId(), SearchCriteria.Op.EQ);
        PrefixStateSearch.and("state", PrefixStateSearch.entity().getState(), SearchCriteria.Op.IN);
        PrefixStateSearch.done();
        PrefixIdSearch = createSearchBuilder();
        PrefixIdSearch.and("prefixId", PrefixIdSearch.entity().getPrefixId(), SearchCriteria.Op.EQ);
        PrefixIdSearch.done();
        NetworkIdSearch = createSearchBuilder();
        NetworkIdSearch.and("networkId", NetworkIdSearch.entity().getNetworkId(), SearchCriteria.Op.EQ);
        NetworkIdSearch.done();
        SubnetSearch = createSearchBuilder();
        SubnetSearch.and("subnet", SubnetSearch.entity().getSubnet(), SearchCriteria.Op.EQ);
        SubnetSearch.done();
        StatesSearch = createSearchBuilder();
        StatesSearch.and("state", StatesSearch.entity().getState(), SearchCriteria.Op.IN);
        StatesSearch.done();
    }

    @Override
    public List<Ipv6GuestPrefixSubnetNetworkMapVO> listUsedByPrefix(long prefixId) {
        SearchCriteria<Ipv6GuestPrefixSubnetNetworkMapVO> sc = PrefixStateSearch.create();
        sc.setParameters("prefixId", prefixId);
        sc.setParameters("state", (Object[]) new Ipv6GuestPrefixSubnetNetworkMap.State[]{Ipv6GuestPrefixSubnetNetworkMap.State.Allocated, Ipv6GuestPrefixSubnetNetworkMap.State.Allocating});
        Filter searchFilter = new Filter(Ipv6GuestPrefixSubnetNetworkMapVO.class, "id", true, null, 1L);
        return listBy(sc, searchFilter);
    }

    @Override
    public Ipv6GuestPrefixSubnetNetworkMapVO findFirstAvailable(long prefixId) {
        SearchCriteria<Ipv6GuestPrefixSubnetNetworkMapVO> sc = PrefixStateSearch.create();
        sc.setParameters("prefixId", prefixId);
        sc.setParameters("state", (Object[]) new Ipv6GuestPrefixSubnetNetworkMap.State[]{Ipv6GuestPrefixSubnetNetworkMap.State.Free});
        Filter searchFilter = new Filter(Ipv6GuestPrefixSubnetNetworkMapVO.class, "id", true, null, 1L);
        List<Ipv6GuestPrefixSubnetNetworkMapVO> list = listBy(sc, searchFilter);
        return CollectionUtils.isNotEmpty(list) ? list.get(0) : null;
    }

    @Override
    public Ipv6GuestPrefixSubnetNetworkMapVO findByNetworkId(long networkId) {
        SearchCriteria<Ipv6GuestPrefixSubnetNetworkMapVO> sc = NetworkIdSearch.create();
        sc.setParameters("networkId", networkId);
        return findOneBy(sc);
    }

    @Override
    public Ipv6GuestPrefixSubnetNetworkMapVO findBySubnet(String subnet) {
        SearchCriteria<Ipv6GuestPrefixSubnetNetworkMapVO> sc = SubnetSearch.create();
        sc.setParameters("subnet", subnet);
        return findOneBy(sc);
    }

    @Override
    public List<Ipv6GuestPrefixSubnetNetworkMapVO> findPrefixesInStates(Ipv6GuestPrefixSubnetNetworkMap.State... states) {
        SearchCriteria<Ipv6GuestPrefixSubnetNetworkMapVO> sc = StatesSearch.create();
        sc.setParameters("state", (Object[])states);
        return listBy(sc);
    }

    @Override
    public void deleteByPrefixId(long prefixId) {
        SearchCriteria<Ipv6GuestPrefixSubnetNetworkMapVO> sc = PrefixIdSearch.create();
        sc.setParameters("prefixId", prefixId);
        remove(sc);
    }
}
