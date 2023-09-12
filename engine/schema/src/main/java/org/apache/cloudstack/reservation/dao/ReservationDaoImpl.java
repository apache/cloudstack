//
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
//
package org.apache.cloudstack.reservation.dao;

import com.cloud.configuration.Resource;
import com.cloud.utils.db.GenericDaoBase;
import com.cloud.utils.db.SearchBuilder;
import com.cloud.utils.db.SearchCriteria;
import org.apache.cloudstack.reservation.ReservationVO;

import java.util.List;

public class ReservationDaoImpl extends GenericDaoBase<ReservationVO, Long> implements ReservationDao {

    private static final String RESOURCE_TYPE = "resourceType";
    private static final String ACCOUNT_ID = "accountId";
    private static final String DOMAIN_ID = "domainId";
    private final SearchBuilder<ReservationVO> listAccountAndTypeSearch;

    private final SearchBuilder<ReservationVO> listDomainAndTypeSearch;

    public ReservationDaoImpl() {
        listAccountAndTypeSearch = createSearchBuilder();
        listAccountAndTypeSearch.and(ACCOUNT_ID, listAccountAndTypeSearch.entity().getAccountId(), SearchCriteria.Op.EQ);
        listAccountAndTypeSearch.and(RESOURCE_TYPE, listAccountAndTypeSearch.entity().getResourceType(), SearchCriteria.Op.EQ);
        listAccountAndTypeSearch.done();

        listDomainAndTypeSearch = createSearchBuilder();
        listDomainAndTypeSearch.and(DOMAIN_ID, listDomainAndTypeSearch.entity().getDomainId(), SearchCriteria.Op.EQ);
        listDomainAndTypeSearch.and(RESOURCE_TYPE, listDomainAndTypeSearch.entity().getResourceType(), SearchCriteria.Op.EQ);
        listDomainAndTypeSearch.done();
    }

    @Override
    public long getAccountReservation(Long accountId, Resource.ResourceType resourceType) {
        long total = 0;
        SearchCriteria<ReservationVO> sc = listAccountAndTypeSearch.create();
        sc.setParameters(ACCOUNT_ID, accountId);
        sc.setParameters(RESOURCE_TYPE, resourceType);
        List<ReservationVO> reservations = listBy(sc);
        for (ReservationVO reservation : reservations) {
            total += reservation.getReservedAmount();
        }
        return total;
    }

    @Override
    public long getDomainReservation(Long domainId, Resource.ResourceType resourceType) {
        long total = 0;
        SearchCriteria<ReservationVO> sc = listDomainAndTypeSearch.create();
        sc.setParameters(DOMAIN_ID, domainId);
        sc.setParameters(RESOURCE_TYPE, resourceType);
        List<ReservationVO> reservations = listBy(sc);
        for (ReservationVO reservation : reservations) {
            total += reservation.getReservedAmount();
        }
        return total;
    }
}
