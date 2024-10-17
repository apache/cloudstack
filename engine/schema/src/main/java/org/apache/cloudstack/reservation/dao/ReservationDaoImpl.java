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

import java.util.Date;
import java.util.List;

import org.apache.cloudstack.context.CallContext;
import org.apache.cloudstack.reservation.ReservationVO;

import com.cloud.configuration.Resource;
import com.cloud.utils.db.GenericDaoBase;
import com.cloud.utils.db.SearchBuilder;
import com.cloud.utils.db.SearchCriteria;
import org.apache.cloudstack.user.ResourceReservation;
import org.apache.commons.collections.CollectionUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class ReservationDaoImpl extends GenericDaoBase<ReservationVO, Long> implements ReservationDao {

    protected transient Logger logger = LogManager.getLogger(getClass());
    private static final String RESOURCE_TYPE = "resourceType";
    private static final String RESOURCE_TAG = "resourceTag";
    private static final String RESOURCE_ID = "resourceId";
    private static final String ACCOUNT_ID = "accountId";
    private static final String DOMAIN_ID = "domainId";
    private static final String IDS = "ids";
    private static final String MS_ID = "managementServerId";
    private static final String CREATED = "created";
    private final SearchBuilder<ReservationVO> listResourceByAccountAndTypeSearch;
    private final SearchBuilder<ReservationVO> listAccountAndTypeSearch;
    private final SearchBuilder<ReservationVO> listAccountAndTypeAndNoTagSearch;

    private final SearchBuilder<ReservationVO> listDomainAndTypeSearch;
    private final SearchBuilder<ReservationVO> listDomainAndTypeAndNoTagSearch;
    private final SearchBuilder<ReservationVO> listResourceByAccountAndTypeAndNoTagSearch;
    private final SearchBuilder<ReservationVO> listIdsSearch;
    private final SearchBuilder<ReservationVO> listMsIdSearch;

    public ReservationDaoImpl() {

        listResourceByAccountAndTypeSearch = createSearchBuilder();
        listResourceByAccountAndTypeSearch.and(ACCOUNT_ID, listResourceByAccountAndTypeSearch.entity().getAccountId(), SearchCriteria.Op.EQ);
        listResourceByAccountAndTypeSearch.and(RESOURCE_TYPE, listResourceByAccountAndTypeSearch.entity().getResourceType(), SearchCriteria.Op.EQ);
        listResourceByAccountAndTypeSearch.and(RESOURCE_ID, listResourceByAccountAndTypeSearch.entity().getResourceId(), SearchCriteria.Op.NNULL);
        listResourceByAccountAndTypeSearch.and(RESOURCE_TAG, listResourceByAccountAndTypeSearch.entity().getTag(), SearchCriteria.Op.EQ);
        listResourceByAccountAndTypeSearch.done();

        listResourceByAccountAndTypeAndNoTagSearch = createSearchBuilder();
        listResourceByAccountAndTypeAndNoTagSearch.and(ACCOUNT_ID, listResourceByAccountAndTypeAndNoTagSearch.entity().getAccountId(), SearchCriteria.Op.EQ);
        listResourceByAccountAndTypeAndNoTagSearch.and(RESOURCE_TYPE, listResourceByAccountAndTypeAndNoTagSearch.entity().getResourceType(), SearchCriteria.Op.EQ);
        listResourceByAccountAndTypeAndNoTagSearch.and(RESOURCE_ID, listResourceByAccountAndTypeAndNoTagSearch.entity().getResourceId(), SearchCriteria.Op.NNULL);
        listResourceByAccountAndTypeAndNoTagSearch.and(RESOURCE_TAG, listResourceByAccountAndTypeAndNoTagSearch.entity().getTag(), SearchCriteria.Op.NULL);
        listResourceByAccountAndTypeAndNoTagSearch.done();

        listAccountAndTypeSearch = createSearchBuilder();
        listAccountAndTypeSearch.and(ACCOUNT_ID, listAccountAndTypeSearch.entity().getAccountId(), SearchCriteria.Op.EQ);
        listAccountAndTypeSearch.and(RESOURCE_TYPE, listAccountAndTypeSearch.entity().getResourceType(), SearchCriteria.Op.EQ);
        listAccountAndTypeSearch.and(RESOURCE_TAG, listAccountAndTypeSearch.entity().getTag(), SearchCriteria.Op.EQ);
        listAccountAndTypeSearch.and(CREATED, listAccountAndTypeSearch.entity().getCreated(), SearchCriteria.Op.LT);
        listAccountAndTypeSearch.done();

        listAccountAndTypeAndNoTagSearch = createSearchBuilder();
        listAccountAndTypeAndNoTagSearch.and(ACCOUNT_ID, listAccountAndTypeAndNoTagSearch.entity().getAccountId(), SearchCriteria.Op.EQ);
        listAccountAndTypeAndNoTagSearch.and(RESOURCE_TYPE, listAccountAndTypeAndNoTagSearch.entity().getResourceType(), SearchCriteria.Op.EQ);
        listAccountAndTypeAndNoTagSearch.and(RESOURCE_TAG, listAccountAndTypeAndNoTagSearch.entity().getTag(), SearchCriteria.Op.NULL);
        listAccountAndTypeAndNoTagSearch.and(CREATED, listAccountAndTypeAndNoTagSearch.entity().getCreated(), SearchCriteria.Op.LT);
        listAccountAndTypeAndNoTagSearch.done();

        listDomainAndTypeSearch = createSearchBuilder();
        listDomainAndTypeSearch.and(DOMAIN_ID, listDomainAndTypeSearch.entity().getDomainId(), SearchCriteria.Op.EQ);
        listDomainAndTypeSearch.and(RESOURCE_TYPE, listDomainAndTypeSearch.entity().getResourceType(), SearchCriteria.Op.EQ);
        listDomainAndTypeSearch.and(RESOURCE_TAG, listDomainAndTypeSearch.entity().getTag(), SearchCriteria.Op.EQ);
        listDomainAndTypeSearch.done();

        listDomainAndTypeAndNoTagSearch = createSearchBuilder();
        listDomainAndTypeAndNoTagSearch.and(DOMAIN_ID, listDomainAndTypeAndNoTagSearch.entity().getDomainId(), SearchCriteria.Op.EQ);
        listDomainAndTypeAndNoTagSearch.and(RESOURCE_TYPE, listDomainAndTypeAndNoTagSearch.entity().getResourceType(), SearchCriteria.Op.EQ);
        listDomainAndTypeAndNoTagSearch.and(RESOURCE_TAG, listDomainAndTypeAndNoTagSearch.entity().getTag(), SearchCriteria.Op.NULL);
        listDomainAndTypeAndNoTagSearch.done();

        listIdsSearch = createSearchBuilder();
        listIdsSearch.and(IDS, listIdsSearch.entity().getId(), SearchCriteria.Op.IN);
        listIdsSearch.done();

        listMsIdSearch = createSearchBuilder();
        listMsIdSearch.and(MS_ID, listMsIdSearch.entity().getManagementServerId(), SearchCriteria.Op.EQ);
        listMsIdSearch.done();
    }

    @Override
    public long getAccountReservation(Long accountId, Resource.ResourceType resourceType, String tag) {
        long total = 0;
        SearchCriteria<ReservationVO> sc;
        if (tag == null) {
            sc = listAccountAndTypeAndNoTagSearch.create();
        } else {
            sc = listAccountAndTypeSearch.create();
            sc.setParameters(RESOURCE_TAG, tag);
        }
        sc.setParameters(ACCOUNT_ID, accountId);
        sc.setParameters(RESOURCE_TYPE, resourceType);
        List<ReservationVO> reservations = listBy(sc);
        for (ReservationVO reservation : reservations) {
            total += reservation.getReservedAmount();
        }
        return total;
    }

    @Override
    public long getDomainReservation(Long domainId, Resource.ResourceType resourceType, String tag) {
        long total = 0;
        SearchCriteria<ReservationVO> sc;
        if (tag == null) {
            sc = listDomainAndTypeAndNoTagSearch.create();
        } else {
            sc = listDomainAndTypeSearch.create();
            sc.setParameters(RESOURCE_TAG, tag);
        }
        sc.setParameters(DOMAIN_ID, domainId);
        sc.setParameters(RESOURCE_TYPE, resourceType);
        List<ReservationVO> reservations = listBy(sc);
        for (ReservationVO reservation : reservations) {
            total += reservation.getReservedAmount();
        }
        return total;
    }

    @Override
    public void setResourceId(Resource.ResourceType type, Long resourceId) {
        Object obj = CallContext.current().getContextParameter(String.format("%s-%s", ResourceReservation.class.getSimpleName(), type.getName()));
        if (obj instanceof List) {
            try {
                List<Long> reservationIds = (List<Long>)obj;
                for (Long reservationId : reservationIds) {
                    ReservationVO reservation = findById(reservationId);
                    if (reservation != null) {
                        reservation.setResourceId(resourceId);
                        persist(reservation);
                    }
                }
            } catch (Exception e) {
                logger.warn("Failed to persist reservation for resource type " + type.getName() + " for resource id " + resourceId, e);
            }
        }
    }

    @Override
    public List<ReservationVO> getReservationsForAccount(long accountId, Resource.ResourceType type, String tag) {
        SearchCriteria<ReservationVO> sc;
        if (tag == null) {
            sc = listResourceByAccountAndTypeAndNoTagSearch.create();
        } else {
            sc = listResourceByAccountAndTypeSearch.create();
            sc.setParameters(RESOURCE_TAG, tag);
        }
        sc.setParameters(ACCOUNT_ID, accountId);
        sc.setParameters(RESOURCE_TYPE, type);
        return listBy(sc);
    }

    @Override
    public void removeByIds(List<Long> reservationIds) {
        if (CollectionUtils.isNotEmpty(reservationIds)) {
            SearchCriteria<ReservationVO> sc = listIdsSearch.create();
            sc.setParameters(IDS, reservationIds.toArray());
            remove(sc);
        }
    }

    @Override
    public int removeByMsId(long managementServerId) {
        SearchCriteria<ReservationVO> sc = listMsIdSearch.create();
        sc.setParameters(MS_ID, managementServerId);
        return remove(sc);
    }

    @Override
    public int removeStaleReservations(Long accountId, Resource.ResourceType resourceType, String tag,
            Date createdBefore) {
        SearchCriteria<ReservationVO> sc;
        if (tag == null) {
            sc = listAccountAndTypeAndNoTagSearch.create();
        } else {
            sc = listAccountAndTypeSearch.create();
            sc.setParameters(RESOURCE_TAG, tag);
        }
        sc.setParameters(ACCOUNT_ID, accountId);
        sc.setParameters(RESOURCE_TYPE, resourceType);
        sc.setParameters(CREATED, createdBefore);
        return remove(sc);
    }

}
