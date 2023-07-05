/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package org.apache.cloudstack.cluster.dao;

import com.cloud.utils.db.Filter;
import com.cloud.utils.db.GenericDaoBase;
import com.cloud.utils.db.SearchBuilder;
import com.cloud.utils.db.SearchCriteria;
import org.apache.cloudstack.cluster.ClusterDrsEvents;
import org.apache.cloudstack.cluster.ClusterDrsEventsVO;
import org.apache.commons.lang.time.DateUtils;

import java.util.Date;
import java.util.List;

public class ClusterDrsEventsDaoImpl extends GenericDaoBase<ClusterDrsEventsVO, Long> implements ClusterDrsEventsDao {

    private static final String EXECUTION_DATE = "executionDate";

    public ClusterDrsEventsDaoImpl() {
    }

    @Override
    public ClusterDrsEventsVO lastAutomatedDrsEventInInterval(Long clusterId, int interval) {
        SearchBuilder<ClusterDrsEventsVO> eventSearch = createSearchBuilder();
        eventSearch.and("cluster_id", eventSearch.entity().getClusterId(), SearchCriteria.Op.EQ);
        eventSearch.and(EXECUTION_DATE, eventSearch.entity().getExecutionDate(), SearchCriteria.Op.GTEQ);
        eventSearch.and("type", eventSearch.entity().getType(), SearchCriteria.Op.EQ);
        eventSearch.done();
        SearchCriteria<ClusterDrsEventsVO> sc = eventSearch.create();
        sc.setParameters("cluster_id", clusterId);
        sc.setParameters(EXECUTION_DATE, DateUtils.addMinutes(new Date(), -1 * interval));
        sc.setParameters("type", ClusterDrsEvents.Type.AUTOMATED);
        List<ClusterDrsEventsVO> eventList = listBy(sc, new Filter(ClusterDrsEventsVO.class, "id", false, 0L, 1L));
        if (eventList.isEmpty()) {
            return null;
        }
        return eventList.get(0);
    }

    @Override
    public int removeDrsEventsBeforeInterval(int interval) {
        SearchBuilder<ClusterDrsEventsVO> eventSearch = createSearchBuilder();
        eventSearch.and(EXECUTION_DATE, eventSearch.entity().getExecutionDate(), SearchCriteria.Op.GTEQ);
        eventSearch.done();
        SearchCriteria<ClusterDrsEventsVO> sc = eventSearch.create();
        sc.setParameters(EXECUTION_DATE, DateUtils.addDays(new Date(), -1 * interval));
        return remove(sc);
    }
}
