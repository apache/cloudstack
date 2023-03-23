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
package com.cloud.vm.schedule.dao;

import com.cloud.utils.db.GenericDaoBase;
import com.cloud.utils.db.SearchBuilder;
import com.cloud.utils.db.SearchCriteria;
import com.cloud.vm.schedule.VMScheduleVO;
import org.apache.log4j.Logger;

import java.util.Date;
import java.util.List;

public class VMScheduleDaoImpl extends GenericDaoBase<VMScheduleVO, Long> implements VMScheduleDao {
    private static final Logger LOGGER = Logger.getLogger(VMScheduleDaoImpl.class);

    private final SearchBuilder<VMScheduleVO> VMScheduleSearch;
    private SearchBuilder<VMScheduleVO> executableSchedulesSearch;

    protected VMScheduleDaoImpl() {
        VMScheduleSearch = createSearchBuilder();
        VMScheduleSearch.and("vm_id", VMScheduleSearch.entity().getVmId(), SearchCriteria.Op.EQ);
        VMScheduleSearch.and("async_job_id", VMScheduleSearch.entity().getAsyncJobId(), SearchCriteria.Op.EQ);
        VMScheduleSearch.done();
        VMScheduleSearch.done();

        executableSchedulesSearch = createSearchBuilder();
        executableSchedulesSearch.and("scheduledTimestamp", executableSchedulesSearch.entity().getScheduledTimestamp(), SearchCriteria.Op.LT);
        executableSchedulesSearch.and("asyncJobId", executableSchedulesSearch.entity().getAsyncJobId(), SearchCriteria.Op.NULL);
        executableSchedulesSearch.done();
    }

    @Override
    public List<VMScheduleVO> findByVm(Long vmId) {
        SearchCriteria<VMScheduleVO> sc = VMScheduleSearch.create();
        sc.setParameters("vm_id", vmId);
        return listBy(sc, null);
    }

    @Override
    public List<VMScheduleVO> getSchedulesToExecute(Date currentTimestamp) {
        SearchCriteria<VMScheduleVO> sc = executableSchedulesSearch.create();
        sc.setParameters("scheduledTimestamp", currentTimestamp);
        return listBy(sc);
    }
}
