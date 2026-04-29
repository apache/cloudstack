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
package com.cloud.deploy.dao;

import java.util.List;

import javax.annotation.PostConstruct;

import com.cloud.deploy.DeploymentPlanner.PlannerResourceUsage;
import com.cloud.deploy.PlannerHostReservationVO;
import com.cloud.utils.db.GenericDaoBase;
import com.cloud.utils.db.SearchBuilder;
import com.cloud.utils.db.SearchCriteria;

public class PlannerHostReservationDaoImpl extends GenericDaoBase<PlannerHostReservationVO, Long> implements PlannerHostReservationDao {

    private SearchBuilder<PlannerHostReservationVO> _hostIdSearch;
    private SearchBuilder<PlannerHostReservationVO> _reservedHostSearch;
    private SearchBuilder<PlannerHostReservationVO> _dedicatedHostSearch;;

    public PlannerHostReservationDaoImpl() {

    }

    @PostConstruct
    protected void init() {
        _hostIdSearch = createSearchBuilder();
        _hostIdSearch.and("hostId", _hostIdSearch.entity().getHostId(), SearchCriteria.Op.EQ);
        _hostIdSearch.done();

        _reservedHostSearch = createSearchBuilder();
        _reservedHostSearch.and("usage", _reservedHostSearch.entity().getResourceUsage(), SearchCriteria.Op.NNULL);
        _reservedHostSearch.done();

        _dedicatedHostSearch = createSearchBuilder();
        _dedicatedHostSearch.and("usage", _dedicatedHostSearch.entity().getResourceUsage(), SearchCriteria.Op.EQ);
        _dedicatedHostSearch.done();
    }

    @Override
    public PlannerHostReservationVO findByHostId(long hostId) {
        SearchCriteria<PlannerHostReservationVO> sc = _hostIdSearch.create();
        sc.setParameters("hostId", hostId);
        return findOneBy(sc);
    }

    @Override
    public List<PlannerHostReservationVO> listAllReservedHosts() {
        SearchCriteria<PlannerHostReservationVO> sc = _reservedHostSearch.create();
        return listBy(sc);
    }

    @Override
    public List<PlannerHostReservationVO> listAllDedicatedHosts() {
        SearchCriteria<PlannerHostReservationVO> sc = _dedicatedHostSearch.create();
        sc.setParameters("usage", PlannerResourceUsage.Dedicated);
        return listBy(sc);
    }
}
