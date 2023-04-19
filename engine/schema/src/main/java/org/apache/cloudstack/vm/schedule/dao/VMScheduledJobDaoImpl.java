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
import org.apache.log4j.Logger;
import org.springframework.stereotype.Component;

import java.util.Calendar;
import java.util.Date;
import java.util.List;

@Component
public class VMScheduledJobDaoImpl extends GenericDaoBase<VMScheduledJobVO, Long> implements VMScheduledJobDao {
    private static final Logger LOGGER = Logger.getLogger(VMScheduledJobDaoImpl.class);

    @Override
    public List<VMScheduledJobVO> findByVm(Long vmId) {
        SearchCriteria<VMScheduledJobVO> sc = createSearchCriteria();
        sc.setParameters("vm_id", vmId);
        return listBy(sc, null);
    }

    /**
     * Execution of job wouldn't be at exact seconds. So, we round off and then execute. Should we use truncate or round off here?
     */
    @Override
    public List<VMScheduledJobVO> listJobsToStart(Date currentTimestamp) {
        if (currentTimestamp == null) {
            currentTimestamp = new Date();
        }
        Date truncatedTs = DateUtils.round(currentTimestamp, Calendar.MINUTE);
        SearchBuilder<VMScheduledJobVO> sb = createSearchBuilder();
        sb.and("scheduled_timestamp", sb.entity().getScheduledTime(), SearchCriteria.Op.EQ);
        sb.and("async_job_id", sb.entity().getAsyncJobId(), SearchCriteria.Op.NULL);

        SearchCriteria<VMScheduledJobVO> sc = sb.create();
        sc.setParameters("scheduled_timestamp", truncatedTs);
        Filter filter = new Filter(VMScheduledJobVO.class, "vmScheduleId", true, null,null);
        return search(sc, filter);
    }

    @Override
    public int expungeJobsForSchedules(List<Long> vmScheduleIds, Date currentTimestamp) {
        if (currentTimestamp == null) {
            currentTimestamp = new Date();
        }
        Date truncatedTs = DateUtils.round(new Date(), Calendar.MINUTE);
        SearchBuilder<VMScheduledJobVO> sb = createSearchBuilder();
        sb.and("scheduled_timestamp", sb.entity().getScheduledTime(), SearchCriteria.Op.GT);
        sb.and("vm_schedule_id", sb.entity().getVmScheduleId(), SearchCriteria.Op.EQ);

        SearchCriteria<VMScheduledJobVO> sc = sb.create();
        sc.setParameters("scheduled_timestamp", truncatedTs);
        sc.setParameters("vm_schedule_id", vmScheduleIds.toArray());

        return expunge(sc);
    }
}
