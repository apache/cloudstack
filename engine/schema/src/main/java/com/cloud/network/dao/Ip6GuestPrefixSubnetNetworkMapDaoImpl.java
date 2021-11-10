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

import com.cloud.network.Ip6GuestPrefixSubnetNetworkMap;
import com.cloud.network.Ip6GuestPrefixSubnetNetworkMapVO;
import com.cloud.utils.db.DB;
import com.cloud.utils.db.GenericDaoBase;
import com.cloud.utils.db.SearchBuilder;
import com.cloud.utils.db.SearchCriteria;

@Component
@DB
public class Ip6GuestPrefixSubnetNetworkMapDaoImpl extends GenericDaoBase<Ip6GuestPrefixSubnetNetworkMapVO, Long> implements Ip6GuestPrefixSubnetNetworkMapDao {

    protected SearchBuilder<Ip6GuestPrefixSubnetNetworkMapVO> FreeSubnetSearch;
    protected SearchBuilder<Ip6GuestPrefixSubnetNetworkMapVO> PrefixIdSearch;

    @PostConstruct
    public void init() {
        FreeSubnetSearch = createSearchBuilder();
        FreeSubnetSearch.and("prefixId", FreeSubnetSearch.entity().getPrefixId(), SearchCriteria.Op.EQ);
        FreeSubnetSearch.and("state", FreeSubnetSearch.entity().getState(), SearchCriteria.Op.EQ);
        FreeSubnetSearch.done();
        PrefixIdSearch = createSearchBuilder();
        PrefixIdSearch.and("prefixId", FreeSubnetSearch.entity().getPrefixId(), SearchCriteria.Op.EQ);
        PrefixIdSearch.done();
    }

    @Override
    public Ip6GuestPrefixSubnetNetworkMapVO findFirstAvailable(long prefixId) {
        SearchCriteria<Ip6GuestPrefixSubnetNetworkMapVO> sc = FreeSubnetSearch.create();
        sc.setParameters("prefixId", prefixId);
        sc.setParameters("state", Ip6GuestPrefixSubnetNetworkMap.State.Free);
        return findOneBy(sc);
    }

    @Override
    public Ip6GuestPrefixSubnetNetworkMapVO findLast(long prefixId) {
        SearchCriteria<Ip6GuestPrefixSubnetNetworkMapVO> sc = PrefixIdSearch.create();
        sc.setParameters("prefixId", prefixId);
        List<Ip6GuestPrefixSubnetNetworkMapVO> list = listBy(sc);
        return CollectionUtils.isNotEmpty(list) ? list.get(list.size() - 1) : null;
    }
}
