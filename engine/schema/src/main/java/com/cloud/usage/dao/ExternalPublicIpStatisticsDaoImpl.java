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
package com.cloud.usage.dao;

import java.util.List;


import org.springframework.stereotype.Component;

import com.cloud.usage.ExternalPublicIpStatisticsVO;
import com.cloud.utils.db.GenericDaoBase;
import com.cloud.utils.db.SearchBuilder;
import com.cloud.utils.db.SearchCriteria;

@Component
public class ExternalPublicIpStatisticsDaoImpl extends GenericDaoBase<ExternalPublicIpStatisticsVO, Long> implements ExternalPublicIpStatisticsDao {

    private final SearchBuilder<ExternalPublicIpStatisticsVO> AccountZoneSearch;
    private final SearchBuilder<ExternalPublicIpStatisticsVO> SingleRowSearch;

    public ExternalPublicIpStatisticsDaoImpl() {
        AccountZoneSearch = createSearchBuilder();
        AccountZoneSearch.and("accountId", AccountZoneSearch.entity().getAccountId(), SearchCriteria.Op.EQ);
        AccountZoneSearch.and("zoneId", AccountZoneSearch.entity().getZoneId(), SearchCriteria.Op.EQ);
        AccountZoneSearch.done();

        SingleRowSearch = createSearchBuilder();
        SingleRowSearch.and("accountId", SingleRowSearch.entity().getAccountId(), SearchCriteria.Op.EQ);
        SingleRowSearch.and("zoneId", SingleRowSearch.entity().getZoneId(), SearchCriteria.Op.EQ);
        SingleRowSearch.and("publicIp", SingleRowSearch.entity().getPublicIpAddress(), SearchCriteria.Op.EQ);
        SingleRowSearch.done();
    }

    @Override
    public ExternalPublicIpStatisticsVO lock(long accountId, long zoneId, String publicIpAddress) {
        SearchCriteria<ExternalPublicIpStatisticsVO> sc = getSingleRowSc(accountId, zoneId, publicIpAddress);
        return lockOneRandomRow(sc, true);
    }

    @Override
    public ExternalPublicIpStatisticsVO findBy(long accountId, long zoneId, String publicIpAddress) {
        SearchCriteria<ExternalPublicIpStatisticsVO> sc = getSingleRowSc(accountId, zoneId, publicIpAddress);
        return findOneBy(sc);
    }

    private SearchCriteria<ExternalPublicIpStatisticsVO> getSingleRowSc(long accountId, long zoneId, String publicIpAddress) {
        SearchCriteria<ExternalPublicIpStatisticsVO> sc = SingleRowSearch.create();
        sc.setParameters("accountId", accountId);
        sc.setParameters("zoneId", zoneId);
        sc.setParameters("publicIp", publicIpAddress);
        return sc;
    }

    @Override
    public List<ExternalPublicIpStatisticsVO> listBy(long accountId, long zoneId) {
        SearchCriteria<ExternalPublicIpStatisticsVO> sc = AccountZoneSearch.create();
        sc.setParameters("accountId", accountId);
        sc.setParameters("zoneId", zoneId);
        return search(sc, null);
    }

}
