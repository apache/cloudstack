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

import com.cloud.dc.PodVlanMapVO;
import com.cloud.utils.db.GenericDaoBase;
import com.cloud.utils.db.SearchBuilder;
import com.cloud.utils.db.SearchCriteria;

@Component
public class PodVlanMapDaoImpl extends GenericDaoBase<PodVlanMapVO, Long> implements PodVlanMapDao {

    protected SearchBuilder<PodVlanMapVO> PodSearch;
    protected SearchBuilder<PodVlanMapVO> VlanSearch;
    protected SearchBuilder<PodVlanMapVO> PodVlanSearch;

    @Override
    public List<PodVlanMapVO> listPodVlanMapsByPod(long podId) {
        SearchCriteria<PodVlanMapVO> sc = PodSearch.create();
        sc.setParameters("podId", podId);
        return listIncludingRemovedBy(sc);
    }

    @Override
    public PodVlanMapVO listPodVlanMapsByVlan(long vlanDbId) {
        SearchCriteria<PodVlanMapVO> sc = VlanSearch.create();
        sc.setParameters("vlanDbId", vlanDbId);
        return findOneBy(sc);
    }

    @Override
    public PodVlanMapVO findPodVlanMap(long podId, long vlanDbId) {
        SearchCriteria<PodVlanMapVO> sc = PodVlanSearch.create();
        sc.setParameters("podId", podId);
        sc.setParameters("vlanDbId", vlanDbId);
        return findOneIncludingRemovedBy(sc);
    }

    public PodVlanMapDaoImpl() {
        PodSearch = createSearchBuilder();
        PodSearch.and("podId", PodSearch.entity().getPodId(), SearchCriteria.Op.EQ);
        PodSearch.done();

        VlanSearch = createSearchBuilder();
        VlanSearch.and("vlanDbId", VlanSearch.entity().getVlanDbId(), SearchCriteria.Op.EQ);
        VlanSearch.done();

        PodVlanSearch = createSearchBuilder();
        PodVlanSearch.and("podId", PodVlanSearch.entity().getPodId(), SearchCriteria.Op.EQ);
        PodVlanSearch.and("vlanDbId", PodVlanSearch.entity().getVlanDbId(), SearchCriteria.Op.EQ);
        PodVlanSearch.done();
    }

}
