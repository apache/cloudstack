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

import javax.ejb.Local;
import javax.inject.Inject;

import org.springframework.stereotype.Component;

import com.cloud.network.Networks.TrafficType;
import com.cloud.utils.db.DB;
import com.cloud.utils.db.GenericDaoBase;
import com.cloud.utils.db.JoinBuilder;
import com.cloud.utils.db.SearchBuilder;
import com.cloud.utils.db.SearchCriteria;
import com.cloud.utils.db.SearchCriteria.Op;

@Component
@Local(value=PhysicalNetworkDao.class) @DB(txn=false)
public class PhysicalNetworkDaoImpl extends GenericDaoBase<PhysicalNetworkVO, Long> implements PhysicalNetworkDao {
    final SearchBuilder<PhysicalNetworkVO> ZoneSearch;

    @Inject protected PhysicalNetworkTrafficTypeDao _trafficTypeDao;

    protected PhysicalNetworkDaoImpl() {
        super();
        ZoneSearch = createSearchBuilder();
        ZoneSearch.and("dataCenterId", ZoneSearch.entity().getDataCenterId(), Op.EQ);
        ZoneSearch.done();
    }

    @Override
    public List<PhysicalNetworkVO> listByZone(long zoneId) {
        SearchCriteria<PhysicalNetworkVO> sc = ZoneSearch.create();
        sc.setParameters("dataCenterId", zoneId);
        return search(sc, null);
    }

    @Override
    public List<PhysicalNetworkVO> listByZoneIncludingRemoved(long zoneId) {
        SearchCriteria<PhysicalNetworkVO> sc = ZoneSearch.create();
        sc.setParameters("dataCenterId", zoneId);
        return listIncludingRemovedBy(sc);
    }

    @Override
    public List<PhysicalNetworkVO> listByZoneAndTrafficType(long dataCenterId, TrafficType trafficType) {

        SearchBuilder<PhysicalNetworkTrafficTypeVO> trafficTypeSearch = _trafficTypeDao.createSearchBuilder();
        PhysicalNetworkTrafficTypeVO trafficTypeEntity = trafficTypeSearch.entity();
        trafficTypeSearch.and("trafficType", trafficTypeSearch.entity().getTrafficType(), SearchCriteria.Op.EQ);

        SearchBuilder<PhysicalNetworkVO> pnSearch = createSearchBuilder();
        pnSearch.and("dataCenterId", pnSearch.entity().getDataCenterId(), Op.EQ);
        pnSearch.join("trafficTypeSearch", trafficTypeSearch, pnSearch.entity().getId(), trafficTypeEntity.getPhysicalNetworkId(), JoinBuilder.JoinType.INNER);

        SearchCriteria<PhysicalNetworkVO> sc = pnSearch.create();
        sc.setJoinParameters("trafficTypeSearch", "trafficType", trafficType);
        sc.setParameters("dataCenterId", dataCenterId);

        return listBy(sc);  
    }
}
