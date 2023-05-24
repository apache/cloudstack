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
package org.apache.cloudstack.vm.schedule.dao;

import com.cloud.utils.db.Filter;
import com.cloud.utils.db.GenericDaoBase;
import com.cloud.utils.db.SearchBuilder;
import com.cloud.utils.db.SearchCriteria;
import org.apache.cloudstack.vm.schedule.VMScheduledJobVO;
import org.apache.commons.lang3.time.DateUtils;
import org.springframework.stereotype.Component;

import java.util.Calendar;
import java.util.Date;
import java.util.List;

@Component
public class VMScheduledJobDaoImpl extends GenericDaoBase<VMScheduledJobVO, Long> implements VMScheduledJobDao {

    private final SearchBuilder<VMScheduledJobVO> jobsToStartSearch;

    private final SearchBuilder<VMScheduledJobVO> expungeJobsBeforeSearch;

    private final SearchBuilder<VMScheduledJobVO> expungeJobForScheduleSearch;

    static final String SCHEDULED_TIMESTAMP = "scheduled_timestamp";

    static final String VM_SCHEDULE_ID = "vm_schedule_id";

    public VMScheduledJobDaoImpl() {
        super();
        jobsToStartSearch = createSearchBuilder();
        jobsToStartSearch.and(SCHEDULED_TIMESTAMP, jobsToStartSearch.entity().getScheduledTime(), SearchCriteria.Op.EQ);
        jobsToStartSearch.and("async_job_id", jobsToStartSearch.entity().getAsyncJobId(), SearchCriteria.Op.NULL);
        jobsToStartSearch.done();

        expungeJobsBeforeSearch = createSearchBuilder();
        expungeJobsBeforeSearch.and(SCHEDULED_TIMESTAMP, expungeJobsBeforeSearch.entity().getScheduledTime(), SearchCriteria.Op.LT);
        expungeJobsBeforeSearch.done();

        expungeJobForScheduleSearch = createSearchBuilder();
        expungeJobForScheduleSearch.and(VM_SCHEDULE_ID, expungeJobForScheduleSearch.entity().getVmScheduleId(), SearchCriteria.Op.IN);
        expungeJobForScheduleSearch.and(SCHEDULED_TIMESTAMP, expungeJobForScheduleSearch.entity().getScheduledTime(), SearchCriteria.Op.GTEQ);
        expungeJobForScheduleSearch.done();
    }

    /**
     * Execution of job wouldn't be at exact seconds. So, we round off and then execute.
     */
    @Override
    public List<VMScheduledJobVO> listJobsToStart(Date currentTimestamp) {
        if (currentTimestamp == null) {
            currentTimestamp = new Date();
        }
        Date truncatedTs = DateUtils.round(currentTimestamp, Calendar.MINUTE);

        SearchCriteria<VMScheduledJobVO> sc = jobsToStartSearch.create();
        sc.setParameters(SCHEDULED_TIMESTAMP, truncatedTs);
        Filter filter = new Filter(VMScheduledJobVO.class, "vmScheduleId", true, null, null);
        return search(sc, filter);
    }

    @Override
    public int expungeJobsForSchedules(List<Long> vmScheduleIds, Date dateAfter) {
        SearchCriteria<VMScheduledJobVO> sc = expungeJobForScheduleSearch.create();
        sc.setParameters(VM_SCHEDULE_ID, vmScheduleIds.toArray());
        if (dateAfter != null) {
            sc.setParameters(SCHEDULED_TIMESTAMP, dateAfter);
        }
        return expunge(sc);
    }

    @Override
    public int expungeJobsBefore(Date date) {
        SearchCriteria<VMScheduledJobVO> sc = expungeJobsBeforeSearch.create();
        sc.setParameters(SCHEDULED_TIMESTAMP, date);
        return expunge(sc);
    }
}
