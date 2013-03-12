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

import java.util.ArrayList;
import java.util.List;

import javax.ejb.Local;

import org.apache.log4j.Logger;

import com.cloud.api.ApiResponseHelper;
import com.cloud.api.query.vo.EventJoinVO;

import org.apache.cloudstack.api.response.EventResponse;
import org.springframework.stereotype.Component;

import com.cloud.event.Event;
import com.cloud.event.Event.State;
import com.cloud.utils.db.Filter;
import com.cloud.utils.db.GenericDaoBase;
import com.cloud.utils.db.SearchBuilder;
import com.cloud.utils.db.SearchCriteria;

@Component
@Local(value={EventJoinDao.class})
public class EventJoinDaoImpl extends GenericDaoBase<EventJoinVO, Long> implements EventJoinDao {
    public static final Logger s_logger = Logger.getLogger(EventJoinDaoImpl.class);

    private SearchBuilder<EventJoinVO> vrSearch;

    private SearchBuilder<EventJoinVO> vrIdSearch;

    private SearchBuilder<EventJoinVO> CompletedEventSearch;

    protected EventJoinDaoImpl() {

        vrSearch = createSearchBuilder();
        vrSearch.and("idIN", vrSearch.entity().getId(), SearchCriteria.Op.IN);
        vrSearch.done();

        vrIdSearch = createSearchBuilder();
        vrIdSearch.and("id", vrIdSearch.entity().getId(), SearchCriteria.Op.EQ);
        vrIdSearch.done();

        CompletedEventSearch = createSearchBuilder();
        CompletedEventSearch.and("state",CompletedEventSearch.entity().getState(),SearchCriteria.Op.EQ);
        CompletedEventSearch.and("startId", CompletedEventSearch.entity().getStartId(), SearchCriteria.Op.EQ);
        CompletedEventSearch.done();


        this._count = "select count(distinct id) from event_view WHERE ";
    }



    @Override
    public List<EventJoinVO> searchAllEvents(SearchCriteria<EventJoinVO> sc, Filter filter) {
        return listIncludingRemovedBy(sc, filter);
    }



    @Override
    public EventJoinVO findCompletedEvent(long startId) {
        SearchCriteria<EventJoinVO> sc = CompletedEventSearch.create();
        sc.setParameters("state", State.Completed);
        sc.setParameters("startId", startId);
        return findOneIncludingRemovedBy(sc);
    }



    @Override
    public EventResponse newEventResponse(EventJoinVO event) {
        EventResponse responseEvent = new EventResponse();
        responseEvent.setCreated(event.getCreateDate());
        responseEvent.setDescription(event.getDescription());
        responseEvent.setEventType(event.getType());
        responseEvent.setId(event.getUuid());
        responseEvent.setLevel(event.getLevel());
        responseEvent.setParentId(event.getStartUuid());
        responseEvent.setState(event.getState());
        responseEvent.setUsername(event.getUserName());

        ApiResponseHelper.populateOwner(responseEvent, event);
        responseEvent.setObjectName("event");
        return responseEvent;
    }



    @Override
    public List<EventJoinVO> searchByIds(Long... ids) {
        SearchCriteria<EventJoinVO> sc = vrSearch.create();
        sc.setParameters("idIN", ids);
        return searchIncludingRemoved(sc, null, null, false);
    }


    @Override
    public EventJoinVO newEventView(Event vr) {

        SearchCriteria<EventJoinVO> sc = vrIdSearch.create();
        sc.setParameters("id", vr.getId());
        List<EventJoinVO> vms = searchIncludingRemoved(sc, null, null, false);
        assert vms != null && vms.size() == 1 : "No event found for event id " + vr.getId();
        return vms.get(0);

    }

}
