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

import com.cloud.event.Event;
import com.cloud.event.EventVO;
import com.cloud.utils.db.Filter;
import com.cloud.utils.db.GenericDao;
import com.cloud.utils.db.SearchCriteria;

public interface EventDao extends GenericDao<EventVO, Long> {
    public List<EventVO> searchAllEvents(SearchCriteria<EventVO> sc, Filter filter);

    public List<EventVO> listOlderEvents(Date oldTime);

    EventVO findCompletedEvent(long startId);

    /**
     * Finds the last non-archived start event matching the specified criteria.
     * Events are ordered by ID in descending order, returning the most recent one.
     *
     * @param type         the event type to search for
     * @param state        the event state to search for (e.g., {@link Event.State#Scheduled})
     * @param resourceId   the resource ID associated with the event
     * @param resourceType the resource type associated with the event
     * @return the most recent EventVO matching the criteria, or null if not found
     */
    EventVO findLastEvent(String type, Event.State state, Long resourceId, String resourceType);

    public List<EventVO> listToArchiveOrDeleteEvents(List<Long> ids, String type, Date startDate, Date endDate, List<Long> accountIds);

    public void archiveEvents(List<EventVO> events);

    /**
     * Returns the most recent events of the given type for a resource, newest first.
     * Used to derive how many consecutive attempts have failed for that resource without a dedicated counter column.
     */
    List<EventVO> listLatestEventsByResource(long resourceId, String resourceType, String type, int limit);

}
