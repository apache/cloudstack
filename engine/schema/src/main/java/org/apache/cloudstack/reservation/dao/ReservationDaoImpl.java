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

import java.util.List;

import org.apache.cloudstack.reservation.ReservationVO;

import com.cloud.configuration.Resource;
import com.cloud.utils.db.GenericDaoBase;
import com.cloud.utils.db.SearchBuilder;
import com.cloud.utils.db.SearchCriteria;

public class ReservationDaoImpl extends GenericDaoBase<ReservationVO, Long> implements ReservationDao {

    private static final String RESOURCE_TYPE = "resourceType";
    private static final String RESOURCE_TAG = "resourceTag";
    private static final String ACCOUNT_ID = "accountId";
    private static final String DOMAIN_ID = "domainId";
    private final SearchBuilder<ReservationVO> listAccountAndTypeSearch;
    private final SearchBuilder<ReservationVO> listAccountAndTypeAndNoTagSearch;

    private final SearchBuilder<ReservationVO> listDomainAndTypeSearch;
    private final SearchBuilder<ReservationVO> listDomainAndTypeAndNoTagSearch;

    public ReservationDaoImpl() {
        listAccountAndTypeSearch = createSearchBuilder();
        listAccountAndTypeSearch.and(ACCOUNT_ID, listAccountAndTypeSearch.entity().getAccountId(), SearchCriteria.Op.EQ);
        listAccountAndTypeSearch.and(RESOURCE_TYPE, listAccountAndTypeSearch.entity().getResourceType(), SearchCriteria.Op.EQ);
        listAccountAndTypeSearch.and(RESOURCE_TAG, listAccountAndTypeSearch.entity().getTag(), SearchCriteria.Op.EQ);
        listAccountAndTypeSearch.done();

        listAccountAndTypeAndNoTagSearch = createSearchBuilder();
        listAccountAndTypeAndNoTagSearch.and(ACCOUNT_ID, listAccountAndTypeAndNoTagSearch.entity().getAccountId(), SearchCriteria.Op.EQ);
        listAccountAndTypeAndNoTagSearch.and(RESOURCE_TYPE, listAccountAndTypeAndNoTagSearch.entity().getResourceType(), SearchCriteria.Op.EQ);
        listAccountAndTypeAndNoTagSearch.and(RESOURCE_TAG, listAccountAndTypeAndNoTagSearch.entity().getTag(), SearchCriteria.Op.NULL);
        listAccountAndTypeAndNoTagSearch.done();

        listDomainAndTypeSearch = createSearchBuilder();
        listDomainAndTypeSearch.and(DOMAIN_ID, listDomainAndTypeSearch.entity().getDomainId(), SearchCriteria.Op.EQ);
        listDomainAndTypeSearch.and(RESOURCE_TYPE, listDomainAndTypeSearch.entity().getResourceType(), SearchCriteria.Op.EQ);
        listDomainAndTypeSearch.and(RESOURCE_TAG, listDomainAndTypeSearch.entity().getTag(), SearchCriteria.Op.EQ);
        listDomainAndTypeSearch.done();

        listDomainAndTypeAndNoTagSearch = createSearchBuilder();
        listDomainAndTypeAndNoTagSearch.and(ACCOUNT_ID, listDomainAndTypeAndNoTagSearch.entity().getAccountId(), SearchCriteria.Op.EQ);
        listDomainAndTypeAndNoTagSearch.and(RESOURCE_TYPE, listDomainAndTypeAndNoTagSearch.entity().getResourceType(), SearchCriteria.Op.EQ);
        listDomainAndTypeAndNoTagSearch.and(RESOURCE_TAG, listDomainAndTypeAndNoTagSearch.entity().getTag(), SearchCriteria.Op.NULL);
        listDomainAndTypeAndNoTagSearch.done();
    }

    @Override
    public long getAccountReservation(Long accountId, Resource.ResourceType resourceType, String tag) {
        long total = 0;
        SearchCriteria<ReservationVO> sc = tag == null ?
                listAccountAndTypeAndNoTagSearch.create() : listAccountAndTypeSearch.create();
        sc.setParameters(ACCOUNT_ID, accountId);
        sc.setParameters(RESOURCE_TYPE, resourceType);
        if (tag != null) {
            sc.setParameters(RESOURCE_TAG, tag);
        }
        List<ReservationVO> reservations = listBy(sc);
        for (ReservationVO reservation : reservations) {
            total += reservation.getReservedAmount();
        }
        return total;
    }

    @Override
    public long getDomainReservation(Long domainId, Resource.ResourceType resourceType, String tag) {
        long total = 0;
        SearchCriteria<ReservationVO> sc = tag == null ?
                listDomainAndTypeAndNoTagSearch.create() : listDomainAndTypeSearch.create();
        sc.setParameters(DOMAIN_ID, domainId);
        sc.setParameters(RESOURCE_TYPE, resourceType);
        if (tag != null) {
            sc.setParameters(RESOURCE_TAG, tag);
        }
        List<ReservationVO> reservations = listBy(sc);
        for (ReservationVO reservation : reservations) {
            total += reservation.getReservedAmount();
        }
        return total;
    }
}
