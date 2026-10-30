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
package com.cloud.event.dao;

import java.util.Date;
import java.util.List;


import org.apache.commons.collections4.CollectionUtils;
import org.springframework.stereotype.Component;

import com.cloud.event.Event.State;
import com.cloud.event.EventVO;
import com.cloud.utils.db.GenericDaoBase;
import com.cloud.utils.db.SearchBuilder;
import com.cloud.utils.db.SearchCriteria;
import com.cloud.utils.db.SearchCriteria.Op;
import com.cloud.utils.db.UpdateBuilder;

@Component
public class EventDaoImpl extends GenericDaoBase<EventVO, Long> implements EventDao {

    protected final SearchBuilder<EventVO> ToArchiveOrDeleteEventSearch;
    protected final SearchBuilder<EventVO> LastStartEventSearch;

    public EventDaoImpl() {
        ToArchiveOrDeleteEventSearch = createSearchBuilder();
        ToArchiveOrDeleteEventSearch.select("id", SearchCriteria.Func.NATIVE, ToArchiveOrDeleteEventSearch.entity().getId());
        ToArchiveOrDeleteEventSearch.and("id", ToArchiveOrDeleteEventSearch.entity().getId(), Op.IN);
        ToArchiveOrDeleteEventSearch.and("type", ToArchiveOrDeleteEventSearch.entity().getType(), Op.EQ);
        ToArchiveOrDeleteEventSearch.and("accountId", ToArchiveOrDeleteEventSearch.entity().getAccountId(), Op.EQ);
        ToArchiveOrDeleteEventSearch.and("domainIds", ToArchiveOrDeleteEventSearch.entity().getDomainId(), Op.IN);
        ToArchiveOrDeleteEventSearch.and("createdDateB", ToArchiveOrDeleteEventSearch.entity().getCreateDate(), Op.BETWEEN);
        ToArchiveOrDeleteEventSearch.and("createdDateL", ToArchiveOrDeleteEventSearch.entity().getCreateDate(), Op.LTEQ);
        ToArchiveOrDeleteEventSearch.and("createdDateLT", ToArchiveOrDeleteEventSearch.entity().getCreateDate(), Op.LT);
        ToArchiveOrDeleteEventSearch.and("archived", ToArchiveOrDeleteEventSearch.entity().getArchived(), Op.EQ);
        ToArchiveOrDeleteEventSearch.done();

        LastStartEventSearch = createSearchBuilder();
        LastStartEventSearch.and("type", LastStartEventSearch.entity().getType(), Op.EQ);
        LastStartEventSearch.and("state", LastStartEventSearch.entity().getState(), Op.EQ);
        LastStartEventSearch.and("resourceId", LastStartEventSearch.entity().getResourceId(), Op.EQ);
        LastStartEventSearch.and("resourceType", LastStartEventSearch.entity().getResourceType(), Op.EQ);
        LastStartEventSearch.and("archived", LastStartEventSearch.entity().getArchived(), Op.EQ);
        LastStartEventSearch.done();
    }

    @Override
    public EventVO findLastEvent(String type, State state, Long resourceId, String resourceType) {
        SearchCriteria<EventVO> sc = LastStartEventSearch.create();
        sc.setParameters("type", type);
        sc.setParameters("state", state);
        sc.setParameters("resourceId", resourceId);
        sc.setParameters("resourceType", resourceType);
        sc.setParameters("archived", false);
        return findLastOneBy(sc);
    }

    private SearchCriteria<EventVO> createEventSearchCriteria(List<Long> ids, String type, Date startDate, Date endDate,
                                                              Date limitDate, Long accountId, List<Long> domainIds) {
        SearchCriteria<EventVO> sc = ToArchiveOrDeleteEventSearch.create();

        if (CollectionUtils.isNotEmpty(ids)) {
            sc.setParameters("id", ids.toArray(new Object[0]));
        }
        if (CollectionUtils.isNotEmpty(domainIds)) {
            sc.setParameters("domainIds", domainIds.toArray(new Object[0]));
        }
        if (startDate != null && endDate != null) {
            sc.setParameters("createdDateB", startDate, endDate);
        } else if (endDate != null) {
            sc.setParameters("createdDateL", endDate);
        }
        sc.setParametersIfNotNull("accountId", accountId);
        sc.setParametersIfNotNull("createdDateLT", limitDate);
        sc.setParametersIfNotNull("type", type);
        sc.setParameters("archived", false);

        return sc;
    }

    @Override
    public long archiveEvents(List<Long> ids, String type, Date startDate, Date endDate, Long accountId, List<Long> domainIds,
                              long limitPerQuery) {
        SearchCriteria<EventVO> sc = createEventSearchCriteria(ids, type, startDate, endDate, null, accountId, domainIds);

        long totalArchived = 0L;
        int archived;
        do {
            EventVO eventForUpdate = createForUpdate();
            eventForUpdate.setArchived(true);
            UpdateBuilder ub = getUpdateBuilder(eventForUpdate);
            archived = update(ub, sc, limitPerQuery > 0 ? (int) Math.min(limitPerQuery, Integer.MAX_VALUE) : null);
            totalArchived += archived;
        } while (limitPerQuery > 0 && archived >= limitPerQuery);

        return totalArchived;
    }

    @Override
    public long purgeAll(List<Long> ids, Date startDate, Date endDate, Date limitDate, String type, Long accountId,
                         List<Long> domainIds, long limitPerQuery) {
        SearchCriteria<EventVO> sc = createEventSearchCriteria(ids, type, startDate, endDate, limitDate, accountId, domainIds);
        return batchExpunge(sc, limitPerQuery);
    }
}
