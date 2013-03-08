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

import javax.ejb.Local;

import org.apache.log4j.Logger;
import org.springframework.stereotype.Component;

import com.cloud.event.Event.State;
import com.cloud.event.EventVO;
import com.cloud.utils.db.Filter;
import com.cloud.utils.db.GenericDaoBase;
import com.cloud.utils.db.SearchBuilder;
import com.cloud.utils.db.SearchCriteria;
import com.cloud.utils.db.Transaction;
import com.cloud.utils.db.SearchCriteria.Op;

@Component
@Local(value={EventDao.class})
public class EventDaoImpl extends GenericDaoBase<EventVO, Long> implements EventDao {
    public static final Logger s_logger = Logger.getLogger(EventDaoImpl.class.getName());
    protected final SearchBuilder<EventVO> CompletedEventSearch;
    protected final SearchBuilder<EventVO> ToArchiveOrDeleteEventSearch;

    public EventDaoImpl () {
        CompletedEventSearch = createSearchBuilder();
        CompletedEventSearch.and("state",CompletedEventSearch.entity().getState(),SearchCriteria.Op.EQ);
        CompletedEventSearch.and("startId", CompletedEventSearch.entity().getStartId(), SearchCriteria.Op.EQ);
        CompletedEventSearch.done();

        ToArchiveOrDeleteEventSearch = createSearchBuilder();
        ToArchiveOrDeleteEventSearch.and("id", ToArchiveOrDeleteEventSearch.entity().getId(), Op.IN);
        ToArchiveOrDeleteEventSearch.and("type", ToArchiveOrDeleteEventSearch.entity().getType(), Op.EQ);
        ToArchiveOrDeleteEventSearch.and("accountId", ToArchiveOrDeleteEventSearch.entity().getAccountId(), Op.EQ);
        ToArchiveOrDeleteEventSearch.and("createDateL", ToArchiveOrDeleteEventSearch.entity().getCreateDate(), Op.LT);
        ToArchiveOrDeleteEventSearch.done();
    }

    @Override
    public List<EventVO> searchAllEvents(SearchCriteria<EventVO> sc, Filter filter) {
        return listIncludingRemovedBy(sc, filter);
    }

    @Override
    public List<EventVO> listOlderEvents(Date oldTime) {
        if (oldTime == null) return null;
        SearchCriteria<EventVO> sc = createSearchCriteria();
        sc.addAnd("createDate", SearchCriteria.Op.LT, oldTime);
        return listIncludingRemovedBy(sc, null);
    }

    @Override
    public EventVO findCompletedEvent(long startId) {
        SearchCriteria<EventVO> sc = CompletedEventSearch.create();
        sc.setParameters("state", State.Completed);
        sc.setParameters("startId", startId);
        return findOneIncludingRemovedBy(sc);
    }

    @Override
    public List<EventVO> listToArchiveOrDeleteEvents(List<Long> ids, String type, Date olderThan, Long accountId) {
        SearchCriteria<EventVO> sc = ToArchiveOrDeleteEventSearch.create();
        if (ids != null) {
            sc.setParameters("id", ids.toArray(new Object[ids.size()]));
        }
        if (type != null) {
            sc.setParameters("type", type);
        }
        if (olderThan != null) {
            sc.setParameters("createDateL", olderThan);
        }
        if (accountId != null) {
            sc.setParameters("accountId", accountId);
        }
        return search(sc, null);
    }

    @Override
    public void archiveEvents(List<EventVO> events) {

        Transaction txn = Transaction.currentTxn();
        txn.start();
        for (EventVO event : events) {
            event = lockRow(event.getId(), true);
            event.setArchived(true);
            update(event.getId(), event);
            txn.commit();
        }
        txn.close();
    }
}
