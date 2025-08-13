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

import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;


import com.cloud.utils.db.DB;
import com.cloud.utils.db.Filter;
import com.cloud.utils.exception.CloudRuntimeException;
import org.apache.commons.collections4.CollectionUtils;
import org.springframework.stereotype.Component;

import com.cloud.event.EventVO;
import com.cloud.utils.db.GenericDaoBase;
import com.cloud.utils.db.SearchBuilder;
import com.cloud.utils.db.SearchCriteria;
import com.cloud.utils.db.SearchCriteria.Op;
import com.cloud.utils.db.TransactionLegacy;

@Component
public class EventDaoImpl extends GenericDaoBase<EventVO, Long> implements EventDao {

    protected final SearchBuilder<EventVO> ToArchiveOrDeleteEventSearch;

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
        Filter filter = null;
        if (limitPerQuery > 0) {
            filter = new Filter(limitPerQuery);
        }

        long archived;
        long totalArchived = 0L;

        do {
            List<EventVO> events = search(sc, filter);
            if (events.isEmpty()) {
                break;
            }

            archived = archiveEventsInternal(events);
            totalArchived += archived;
        } while (limitPerQuery > 0 && archived >= limitPerQuery);

        return totalArchived;
    }

    @DB
    private long archiveEventsInternal(List<EventVO> events) {
        final String idsAsString = events.stream()
                .map(e -> Long.toString(e.getId()))
                .collect(Collectors.joining(","));
        final String query = String.format("UPDATE event SET archived=true WHERE id IN (%s)", idsAsString);

        try (TransactionLegacy txn = TransactionLegacy.currentTxn();
             PreparedStatement pstmt = txn.prepareStatement(query)) {
            return pstmt.executeUpdate();
        } catch (SQLException e) {
            throw new CloudRuntimeException(e);
        }
    }

    @Override
    public long purgeAll(List<Long> ids, Date startDate, Date endDate, Date limitDate, String type, Long accountId,
                         List<Long> domainIds, long limitPerQuery) {
        SearchCriteria<EventVO> sc = createEventSearchCriteria(ids, type, startDate, endDate, limitDate, accountId, domainIds);
        return batchExpunge(sc, limitPerQuery);
    }
}
