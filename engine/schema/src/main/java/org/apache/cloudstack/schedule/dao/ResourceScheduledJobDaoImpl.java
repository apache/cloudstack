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
package org.apache.cloudstack.schedule.dao;

import com.cloud.utils.db.Filter;
import com.cloud.utils.db.GenericDaoBase;
import com.cloud.utils.db.SearchBuilder;
import com.cloud.utils.db.SearchCriteria;
import org.apache.cloudstack.api.ApiCommandResourceType;
import org.apache.cloudstack.schedule.ResourceScheduledJobVO;
import org.apache.commons.lang3.time.DateUtils;
import org.springframework.stereotype.Component;

import java.util.Calendar;
import java.util.Date;
import java.util.List;

@Component
public class ResourceScheduledJobDaoImpl extends GenericDaoBase<ResourceScheduledJobVO, Long> implements ResourceScheduledJobDao {

    private final SearchBuilder<ResourceScheduledJobVO> jobsToStartSearch;
    private final SearchBuilder<ResourceScheduledJobVO> expungeJobsBeforeSearch;
    private final SearchBuilder<ResourceScheduledJobVO> expungeJobForScheduleSearch;
    private final SearchBuilder<ResourceScheduledJobVO> scheduleAndTimestampSearch;

    static final String SCHEDULED_TIMESTAMP = "scheduledTimestamp";
    static final String SCHEDULE_ID = "scheduleId";
    static final String RESOURCE_TYPE = "resourceType";

    public ResourceScheduledJobDaoImpl() {
        super();

        jobsToStartSearch = createSearchBuilder();
        jobsToStartSearch.and(RESOURCE_TYPE, jobsToStartSearch.entity().getResourceType(), SearchCriteria.Op.EQ);
        jobsToStartSearch.and(SCHEDULED_TIMESTAMP, jobsToStartSearch.entity().getScheduledTime(), SearchCriteria.Op.EQ);
        jobsToStartSearch.and("async_job_id", jobsToStartSearch.entity().getAsyncJobId(), SearchCriteria.Op.NULL);
        jobsToStartSearch.done();

        expungeJobsBeforeSearch = createSearchBuilder();
        expungeJobsBeforeSearch.and(RESOURCE_TYPE, expungeJobsBeforeSearch.entity().getResourceType(), SearchCriteria.Op.EQ);
        expungeJobsBeforeSearch.and(SCHEDULED_TIMESTAMP, expungeJobsBeforeSearch.entity().getScheduledTime(), SearchCriteria.Op.LT);
        expungeJobsBeforeSearch.done();

        expungeJobForScheduleSearch = createSearchBuilder();
        expungeJobForScheduleSearch.and(SCHEDULE_ID, expungeJobForScheduleSearch.entity().getScheduleId(), SearchCriteria.Op.IN);
        expungeJobForScheduleSearch.and(SCHEDULED_TIMESTAMP, expungeJobForScheduleSearch.entity().getScheduledTime(), SearchCriteria.Op.GTEQ);
        expungeJobForScheduleSearch.done();

        scheduleAndTimestampSearch = createSearchBuilder();
        scheduleAndTimestampSearch.and(SCHEDULE_ID, scheduleAndTimestampSearch.entity().getScheduleId(), SearchCriteria.Op.EQ);
        scheduleAndTimestampSearch.and(SCHEDULED_TIMESTAMP, scheduleAndTimestampSearch.entity().getScheduledTime(), SearchCriteria.Op.EQ);
        scheduleAndTimestampSearch.done();
    }

    @Override
    public List<ResourceScheduledJobVO> listJobsToStart(ApiCommandResourceType resourceType, Date currentTimestamp) {
        if (currentTimestamp == null) {
            currentTimestamp = new Date();
        }
        Date truncatedTs = DateUtils.round(currentTimestamp, Calendar.MINUTE);
        SearchCriteria<ResourceScheduledJobVO> sc = jobsToStartSearch.create();
        sc.setParameters(RESOURCE_TYPE, resourceType);
        sc.setParameters(SCHEDULED_TIMESTAMP, truncatedTs);
        Filter filter = new Filter(ResourceScheduledJobVO.class, "scheduleId", true, null, null);
        return search(sc, filter);
    }

    @Override
    public int expungeJobsForSchedules(List<Long> scheduleIds, Date dateAfter) {
        SearchCriteria<ResourceScheduledJobVO> sc = expungeJobForScheduleSearch.create();
        sc.setParameters(SCHEDULE_ID, scheduleIds.toArray());
        if (dateAfter != null) {
            sc.setParameters(SCHEDULED_TIMESTAMP, dateAfter);
        }
        return expunge(sc);
    }

    @Override
    public int expungeJobsBefore(ApiCommandResourceType resourceType, Date date) {
        SearchCriteria<ResourceScheduledJobVO> sc = expungeJobsBeforeSearch.create();
        sc.setParameters(RESOURCE_TYPE, resourceType);
        sc.setParameters(SCHEDULED_TIMESTAMP, date);
        return expunge(sc);
    }

    @Override
    public ResourceScheduledJobVO findByScheduleAndTimestamp(long scheduleId, Date scheduledTimestamp) {
        SearchCriteria<ResourceScheduledJobVO> sc = scheduleAndTimestampSearch.create();
        sc.setParameters(SCHEDULE_ID, scheduleId);
        sc.setParameters(SCHEDULED_TIMESTAMP, scheduledTimestamp);
        return findOneBy(sc);
    }
}
