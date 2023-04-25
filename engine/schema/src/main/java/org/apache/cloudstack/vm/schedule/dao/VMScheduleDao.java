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
import com.cloud.utils.db.GenericDao;
import com.cloud.utils.db.SearchCriteria;
import org.apache.cloudstack.vm.schedule.VMSchedule;
import org.apache.cloudstack.vm.schedule.VMScheduleVO;

import java.util.List;

public interface VMScheduleDao extends GenericDao<VMScheduleVO, Long> {
    List<VMScheduleVO> listAllActiveSchedules();

    long removeSchedulesForVmIdAndIds(Long vmId, List<Long> ids);

    Pair<List<VMScheduleVO>, Integer> searchAndCount(Long id, Long vmId, VMSchedule.Action action, Boolean enabled, Long offset, Long limit);

    SearchCriteria<VMScheduleVO> getSearchCriteriaForVMId(Long vmId);
}
