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

import com.cloud.utils.Pair;
import com.cloud.utils.db.Filter;
import com.cloud.utils.db.GenericDaoBase;
import com.cloud.utils.db.SearchBuilder;
import com.cloud.utils.db.SearchCriteria;
import org.apache.cloudstack.api.ApiConstants;
import org.apache.cloudstack.vm.schedule.VMSchedule;
import org.apache.cloudstack.vm.schedule.VMScheduleVO;
import org.springframework.stereotype.Component;

import java.util.Date;
import java.util.List;

@Component
public class VMScheduleDaoImpl extends GenericDaoBase<VMScheduleVO, Long> implements VMScheduleDao {

    private final SearchBuilder<VMScheduleVO> activeScheduleSearch;

    private final SearchBuilder<VMScheduleVO> scheduleSearchByVmIdAndIds;

    private final SearchBuilder<VMScheduleVO> scheduleSearch;

    public VMScheduleDaoImpl() {
        super();
        activeScheduleSearch = createSearchBuilder();
        activeScheduleSearch.and(ApiConstants.ENABLED, activeScheduleSearch.entity().getEnabled(), SearchCriteria.Op.EQ);
        activeScheduleSearch.and().op(activeScheduleSearch.entity().getEndDate(), SearchCriteria.Op.NULL);
        activeScheduleSearch.or(ApiConstants.END_DATE, activeScheduleSearch.entity().getEndDate(), SearchCriteria.Op.GT);
        activeScheduleSearch.cp();
        activeScheduleSearch.done();

        scheduleSearchByVmIdAndIds = createSearchBuilder();
        scheduleSearchByVmIdAndIds.and(ApiConstants.ID, scheduleSearchByVmIdAndIds.entity().getId(), SearchCriteria.Op.IN);
        scheduleSearchByVmIdAndIds.and(ApiConstants.VIRTUAL_MACHINE_ID, scheduleSearchByVmIdAndIds.entity().getVmId(), SearchCriteria.Op.EQ);
        scheduleSearchByVmIdAndIds.done();

        scheduleSearch = createSearchBuilder();
        scheduleSearch.and(ApiConstants.ID, scheduleSearch.entity().getId(), SearchCriteria.Op.EQ);
        scheduleSearch.and(ApiConstants.VIRTUAL_MACHINE_ID, scheduleSearch.entity().getVmId(), SearchCriteria.Op.EQ);
        scheduleSearch.and(ApiConstants.ACTION, scheduleSearch.entity().getAction(), SearchCriteria.Op.EQ);
        scheduleSearch.and(ApiConstants.ENABLED, scheduleSearch.entity().getEnabled(), SearchCriteria.Op.EQ);
        scheduleSearch.done();

    }

    @Override
    public List<VMScheduleVO> listAllActiveSchedules() {
        // WHERE enabled = true AND (end_date IS NULL OR end_date > current_date)
        SearchCriteria<VMScheduleVO> sc = activeScheduleSearch.create();
        sc.setParameters(ApiConstants.ENABLED, true);
        sc.setParameters(ApiConstants.END_DATE, new Date());
        return search(sc, null);
    }

    @Override
    public long removeSchedulesForVmIdAndIds(Long vmId, List<Long> ids) {
        SearchCriteria<VMScheduleVO> sc = scheduleSearchByVmIdAndIds.create();
        sc.setParameters(ApiConstants.ID, ids.toArray());
        sc.setParameters(ApiConstants.VIRTUAL_MACHINE_ID, vmId);
        return remove(sc);
    }

    @Override
    public Pair<List<VMScheduleVO>, Integer> searchAndCount(Long id, Long vmId, VMSchedule.Action action, Boolean enabled, Long offset, Long limit) {
        SearchCriteria<VMScheduleVO> sc = scheduleSearch.create();

        if (id != null) {
            sc.setParameters(ApiConstants.ID, id);
        }
        if (enabled != null) {
            sc.setParameters(ApiConstants.ENABLED, enabled);
        }
        if (action != null) {
            sc.setParameters(ApiConstants.ACTION, action);
        }
        sc.setParameters(ApiConstants.VIRTUAL_MACHINE_ID, vmId);

        Filter filter = new Filter(VMScheduleVO.class, ApiConstants.ID, false, offset, limit);
        return searchAndCount(sc, filter);
    }

    @Override
    public SearchCriteria<VMScheduleVO> getSearchCriteriaForVMId(Long vmId) {
        SearchCriteria<VMScheduleVO> sc = scheduleSearch.create();
        sc.setParameters(ApiConstants.VIRTUAL_MACHINE_ID, vmId);
        return sc;
    }
}
