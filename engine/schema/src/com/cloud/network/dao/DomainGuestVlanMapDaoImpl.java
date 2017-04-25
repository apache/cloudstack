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

import com.cloud.utils.db.DB;
import com.cloud.utils.db.GenericDaoBase;
import com.cloud.utils.db.SearchBuilder;
import com.cloud.utils.db.SearchCriteria;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@DB
public class DomainGuestVlanMapDaoImpl extends GenericDaoBase<DomainGuestVlanMapVO, Long> implements DomainGuestVlanMapDao {
    protected SearchBuilder<DomainGuestVlanMapVO> DomainSearch;
    protected SearchBuilder<DomainGuestVlanMapVO> GuestVlanSearch;

    @Override
    public List<DomainGuestVlanMapVO> listDomainGuestVlanMapsByDomain(long domainId) {
        SearchCriteria<DomainGuestVlanMapVO> sc = DomainSearch.create();
        sc.setParameters("domainId", domainId);
        return listIncludingRemovedBy(sc);
    }

    @Override
    public List<DomainGuestVlanMapVO> listDomainGuestVlanMapsByVlan(long guestVlanId) {
        SearchCriteria<DomainGuestVlanMapVO> sc = GuestVlanSearch.create();
        sc.setParameters("guestVlanId", guestVlanId);
        return listIncludingRemovedBy(sc);
    }

    @Override
    public List<DomainGuestVlanMapVO> listDomainGuestVlanMapsByPhysicalNetwork(long physicalNetworkId) {
        SearchCriteria<DomainGuestVlanMapVO> sc = GuestVlanSearch.create();
        sc.setParameters("physicalNetworkId", physicalNetworkId);
        return listIncludingRemovedBy(sc);
    }

    @Override
    public int removeByDomainId(long domainId) {
        SearchCriteria<DomainGuestVlanMapVO> sc = DomainSearch.create();
        sc.setParameters("domainId", domainId);
        return expunge(sc);
    }

    public DomainGuestVlanMapDaoImpl() {
        super();
        DomainSearch = createSearchBuilder();
        DomainSearch.and("domainId", DomainSearch.entity().getDomainId(), SearchCriteria.Op.EQ);
        DomainSearch.done();

        GuestVlanSearch = createSearchBuilder();
        GuestVlanSearch.and("guestVlanId", GuestVlanSearch.entity().getId(), SearchCriteria.Op.EQ);
        GuestVlanSearch.and("physicalNetworkId", GuestVlanSearch.entity().getPhysicalNetworkId(), SearchCriteria.Op.EQ);
        GuestVlanSearch.done();

    }
}
