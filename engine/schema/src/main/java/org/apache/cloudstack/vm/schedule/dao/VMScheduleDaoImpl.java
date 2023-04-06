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

import com.cloud.utils.db.GenericDaoBase;
import com.cloud.utils.db.SearchBuilder;
import com.cloud.utils.db.SearchCriteria;
import org.apache.cloudstack.vm.schedule.VMScheduleVO;
import org.apache.log4j.Logger;
import org.springframework.stereotype.Component;

import java.util.Date;
import java.util.List;

@Component
public class VMScheduleDaoImpl extends GenericDaoBase<VMScheduleVO, Long> implements VMScheduleDao {
    private static final Logger LOGGER = Logger.getLogger(VMScheduleDaoImpl.class);

    @Override
    public List<VMScheduleVO> listAllActiveSchedules() {
        // WHERE enabled = true AND (end_date IS NULL OR end_date < current_date)
        SearchBuilder<VMScheduleVO> sb = createSearchBuilder();
        sb.and("enabled", sb.entity().getEnabled(), SearchCriteria.Op.EQ);
        sb.and().op(sb.entity().getEndDate(), SearchCriteria.Op.NULL);
        sb.or("end_date", sb.entity().getEndDate(), SearchCriteria.Op.LT);
        sb.cp();

        SearchCriteria<VMScheduleVO> sc = sb.create();
        sc.setParameters("enabled", true);
        sc.setParameters("end_date", new Date());
        // TODO: Check if we need to take lock on schedules here.
        return search(sc, null);
    }
}
