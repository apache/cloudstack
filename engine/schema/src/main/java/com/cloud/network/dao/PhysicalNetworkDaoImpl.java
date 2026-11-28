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

import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.annotation.PostConstruct;
import javax.inject.Inject;

import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;

import com.cloud.network.Networks.TrafficType;
import com.cloud.utils.db.DB;
import com.cloud.utils.db.GenericDaoBase;
import com.cloud.utils.db.GenericSearchBuilder;
import com.cloud.utils.db.JoinBuilder;
import com.cloud.utils.db.SearchBuilder;
import com.cloud.utils.db.SearchCriteria;
import com.cloud.utils.db.SearchCriteria.Op;

@Component
@DB()
public class PhysicalNetworkDaoImpl extends GenericDaoBase<PhysicalNetworkVO, Long> implements PhysicalNetworkDao {
    protected SearchBuilder<PhysicalNetworkVO> ZoneSearch;
    protected GenericSearchBuilder<PhysicalNetworkTrafficTypeVO, String> KvmNetworkLabelsInZoneSearch;

    @Inject
    protected PhysicalNetworkTrafficTypeDao _trafficTypeDao;

    protected PhysicalNetworkDaoImpl() {
        super();
    }

    @PostConstruct
    protected void init() {
        ZoneSearch = createSearchBuilder();
        ZoneSearch.and("dataCenterId", ZoneSearch.entity().getDataCenterId(), Op.EQ);
        ZoneSearch.done();

        KvmNetworkLabelsInZoneSearch = _trafficTypeDao.createSearchBuilder(String.class);
        KvmNetworkLabelsInZoneSearch.selectFields(KvmNetworkLabelsInZoneSearch.entity().getKvmNetworkLabel());

        final SearchBuilder<PhysicalNetworkVO> pnSearch = createSearchBuilder();
        pnSearch.and("dataCenterId", pnSearch.entity().getDataCenterId(), Op.EQ);
        pnSearch.and("removed", pnSearch.entity().getRemoved(), Op.NULL);

        KvmNetworkLabelsInZoneSearch.join("pnSearch", pnSearch, KvmNetworkLabelsInZoneSearch.entity().getPhysicalNetworkId(),
                pnSearch.entity().getId(), JoinBuilder.JoinType.INNER);
        KvmNetworkLabelsInZoneSearch.done();
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

    @Override
    public Set<String> getKvmNetworkLabelsInZone(long zoneId) {
        SearchCriteria<String> sc = KvmNetworkLabelsInZoneSearch.create();
        sc.setJoinParameters("pnSearch", "dataCenterId", zoneId);

        return filterKvmNetworkLabels(_trafficTypeDao.customSearch(sc, null));
    }

    private Set<String> filterKvmNetworkLabels(List<String> labels) {
        if (labels == null) {
            return Collections.emptySet();
        }
        Set<String> result = new HashSet<>();
        for (String label : labels) {
            if (StringUtils.isNotBlank(label)) {
                result.add(label);
            }
        }
        return result;
    }
}
