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
package com.cloud.dc.dao;

import java.util.List;

import org.springframework.stereotype.Component;

import com.cloud.dc.DomainVlanMapVO;
import com.cloud.utils.db.GenericDaoBase;
import com.cloud.utils.db.SearchBuilder;
import com.cloud.utils.db.SearchCriteria;

@Component
public class DomainVlanMapDaoImpl extends GenericDaoBase<DomainVlanMapVO, Long> implements DomainVlanMapDao {
    protected SearchBuilder<DomainVlanMapVO> DomainSearch;
    protected SearchBuilder<DomainVlanMapVO> VlanSearch;
    protected SearchBuilder<DomainVlanMapVO> DomainVlanSearch;

    @Override
    public List<DomainVlanMapVO> listDomainVlanMapsByDomain(long domainId) {
            SearchCriteria<DomainVlanMapVO> sc = DomainSearch.create();
            sc.setParameters("domainId", domainId);
            return listIncludingRemovedBy(sc);
    }

    @Override
    public List<DomainVlanMapVO> listDomainVlanMapsByVlan(long vlanDbId) {
        SearchCriteria<DomainVlanMapVO> sc = VlanSearch.create();
        sc.setParameters("vlanDbId", vlanDbId);
        return listIncludingRemovedBy(sc);
    }

    @Override
    public DomainVlanMapVO findDomainVlanMap(long domainId, long vlanDbId) {
        SearchCriteria<DomainVlanMapVO> sc = DomainVlanSearch.create();
        sc.setParameters("domainId", domainId);
        sc.setParameters("vlanDbId", vlanDbId);
        return findOneIncludingRemovedBy(sc);
    }

    public DomainVlanMapDaoImpl() {
        DomainSearch = createSearchBuilder();
        DomainSearch.and("domainId", DomainSearch.entity().getDomainId(), SearchCriteria.Op.EQ);
        DomainSearch.done();

        VlanSearch = createSearchBuilder();
        VlanSearch.and("vlanDbId", VlanSearch.entity().getVlanDbId(), SearchCriteria.Op.EQ);
        VlanSearch.done();

        DomainVlanSearch = createSearchBuilder();
        DomainVlanSearch.and("domainId", DomainVlanSearch.entity().getDomainId(), SearchCriteria.Op.EQ);
        DomainVlanSearch.and("vlanDbId", DomainVlanSearch.entity().getVlanDbId(), SearchCriteria.Op.EQ);
        DomainVlanSearch.done();
    }

}
