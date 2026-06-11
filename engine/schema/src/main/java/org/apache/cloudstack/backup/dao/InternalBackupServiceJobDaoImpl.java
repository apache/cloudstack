//Licensed to the Apache Software Foundation (ASF) under one
//or more contributor license agreements.  See the NOTICE file
//distributed with this work for additional information
//regarding copyright ownership.  The ASF licenses this file
//to you under the Apache License, Version 2.0 (the
//"License"); you may not use this file except in compliance
//the License.  You may obtain a copy of the License at
//
//http://www.apache.org/licenses/LICENSE-2.0
//
//Unless required by applicable law or agreed to in writing,
//software distributed under the License is distributed on an
//"AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
//KIND, either express or implied.  See the License for the
//specific language governing permissions and limitations
//under the License.
package org.apache.cloudstack.backup.dao;

import com.cloud.utils.DateUtil;
import com.cloud.utils.db.DB;
import com.cloud.utils.db.Filter;
import com.cloud.utils.db.GenericDaoBase;
import com.cloud.utils.db.SearchBuilder;
import com.cloud.utils.db.SearchCriteria;
import org.apache.cloudstack.backup.InternalBackupServiceJobType;
import org.apache.cloudstack.backup.InternalBackupServiceJobVO;

import javax.annotation.PostConstruct;
import java.util.Date;
import java.util.List;

public class InternalBackupServiceJobDaoImpl extends GenericDaoBase<InternalBackupServiceJobVO, Long> implements InternalBackupServiceJobDao {
    private SearchBuilder<InternalBackupServiceJobVO> executingBeforeAndHostInAndTypeInSearch;
    private SearchBuilder<InternalBackupServiceJobVO> scheduledAndNotStartedSearch;

    private SearchBuilder<InternalBackupServiceJobVO> executingAndZoneIdAndTypeSearch;

    private static final String HOST_ID = "host_id";
    private static final String TYPE = "type";
    private static final String START_TIME = "start_time";
    private static final String SCHEDULED = "scheduled";
    private static final String ZONE_ID = "zone_id";

    @PostConstruct
    protected void init() {
        executingBeforeAndHostInAndTypeInSearch = createSearchBuilder();
        executingBeforeAndHostInAndTypeInSearch.and(HOST_ID, executingBeforeAndHostInAndTypeInSearch.entity().getHostId(), SearchCriteria.Op.IN);
        executingBeforeAndHostInAndTypeInSearch.and(START_TIME, executingBeforeAndHostInAndTypeInSearch.entity().getStartTime(), SearchCriteria.Op.LTEQ);
        executingBeforeAndHostInAndTypeInSearch.and(TYPE, executingBeforeAndHostInAndTypeInSearch.entity().getType(), SearchCriteria.Op.IN);
        executingBeforeAndHostInAndTypeInSearch.done();

        scheduledAndNotStartedSearch = createSearchBuilder();
        scheduledAndNotStartedSearch.and(SCHEDULED, scheduledAndNotStartedSearch.entity().getScheduledStartTime(), SearchCriteria.Op.LTEQ);
        scheduledAndNotStartedSearch.and(START_TIME, scheduledAndNotStartedSearch.entity().getStartTime(), SearchCriteria.Op.NULL);
        scheduledAndNotStartedSearch.and(ZONE_ID, scheduledAndNotStartedSearch.entity().getZoneId(), SearchCriteria.Op.EQ);
        scheduledAndNotStartedSearch.and(TYPE, scheduledAndNotStartedSearch.entity().getType(), SearchCriteria.Op.IN);
        scheduledAndNotStartedSearch.done();

        executingAndZoneIdAndTypeSearch = createSearchBuilder();
        executingAndZoneIdAndTypeSearch.and(START_TIME, executingAndZoneIdAndTypeSearch.entity().getStartTime(), SearchCriteria.Op.NNULL);
        executingAndZoneIdAndTypeSearch.and(ZONE_ID, executingAndZoneIdAndTypeSearch.entity().getZoneId(), SearchCriteria.Op.EQ);
        executingAndZoneIdAndTypeSearch.and(TYPE, executingAndZoneIdAndTypeSearch.entity().getType(), SearchCriteria.Op.IN);
        executingAndZoneIdAndTypeSearch.done();
    }

    @Override
    public List<InternalBackupServiceJobVO> listExecutingJobsByZoneIdAndJobType(long zoneId, InternalBackupServiceJobType... jobTypes) {
        SearchCriteria<InternalBackupServiceJobVO> sc = executingAndZoneIdAndTypeSearch.create();
        sc.setParameters(TYPE, (Object[]) jobTypes);
        sc.setParameters(ZONE_ID, zoneId);

        return listBy(sc);
    }

    @Override
    public List<InternalBackupServiceJobVO> listWaitingJobsAndScheduledToBeforeNow(long zoneId, InternalBackupServiceJobType... jobTypes) {
        SearchCriteria<InternalBackupServiceJobVO> sc = scheduledAndNotStartedSearch.create();

        sc.setParameters(SCHEDULED, DateUtil.now());
        sc.setParameters(ZONE_ID, zoneId);
        sc.setParameters(TYPE, (Object[]) jobTypes);

        Filter filter = new Filter(InternalBackupServiceJobVO.class, "scheduledStartTime", true);
        return listBy(sc, filter);
    }

    @Override
    public List<InternalBackupServiceJobVO> listExecutingJobsByHostsAndStartTimeBeforeAndTypeIn(Object[] hostIds, Date date, InternalBackupServiceJobType... jobTypes) {
        SearchCriteria<InternalBackupServiceJobVO> sc = executingBeforeAndHostInAndTypeInSearch.create();
        sc.setParameters(HOST_ID, hostIds);
        sc.setParameters(START_TIME, date);
        sc.setParameters(TYPE, (Object[]) jobTypes);

        return listBy(sc);
    }


    @Override
    @DB
    public void update(InternalBackupServiceJobVO job) {
        super.update(job.getId(), job);
    }
}
