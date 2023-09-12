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
package com.cloud.storage.dao;

import java.util.Date;
import java.util.List;


import org.springframework.stereotype.Component;

import com.cloud.storage.Snapshot;
import com.cloud.storage.SnapshotScheduleVO;
import com.cloud.utils.db.GenericDaoBase;
import com.cloud.utils.db.SearchBuilder;
import com.cloud.utils.db.SearchCriteria;

@Component
public class SnapshotScheduleDaoImpl extends GenericDaoBase<SnapshotScheduleVO, Long> implements SnapshotScheduleDao {
    protected final SearchBuilder<SnapshotScheduleVO> executableSchedulesSearch;
    protected final SearchBuilder<SnapshotScheduleVO> coincidingSchedulesSearch;
    private final SearchBuilder<SnapshotScheduleVO> VolumeIdSearch;
    private final SearchBuilder<SnapshotScheduleVO> VolumeIdPolicyIdSearch;

    protected SnapshotScheduleDaoImpl() {

        executableSchedulesSearch = createSearchBuilder();
        executableSchedulesSearch.and("scheduledTimestamp", executableSchedulesSearch.entity().getScheduledTimestamp(), SearchCriteria.Op.LT);
        executableSchedulesSearch.and("asyncJobId", executableSchedulesSearch.entity().getAsyncJobId(), SearchCriteria.Op.NULL);
        executableSchedulesSearch.done();

        coincidingSchedulesSearch = createSearchBuilder();
        coincidingSchedulesSearch.and("volumeId", coincidingSchedulesSearch.entity().getVolumeId(), SearchCriteria.Op.EQ);
        coincidingSchedulesSearch.and("scheduledTimestamp", coincidingSchedulesSearch.entity().getScheduledTimestamp(), SearchCriteria.Op.LT);
        coincidingSchedulesSearch.and("asyncJobId", coincidingSchedulesSearch.entity().getAsyncJobId(), SearchCriteria.Op.NULL);
        coincidingSchedulesSearch.done();

        VolumeIdSearch = createSearchBuilder();
        VolumeIdSearch.and("volumeId", VolumeIdSearch.entity().getVolumeId(), SearchCriteria.Op.EQ);
        VolumeIdSearch.done();

        VolumeIdPolicyIdSearch = createSearchBuilder();
        VolumeIdPolicyIdSearch.and("volumeId", VolumeIdPolicyIdSearch.entity().getVolumeId(), SearchCriteria.Op.EQ);
        VolumeIdPolicyIdSearch.and("policyId", VolumeIdPolicyIdSearch.entity().getPolicyId(), SearchCriteria.Op.EQ);
        VolumeIdPolicyIdSearch.done();

    }

    /**
     * {@inheritDoc}
     */
    @Override
    public List<SnapshotScheduleVO> getCoincidingSnapshotSchedules(long volumeId, Date date) {
        SearchCriteria<SnapshotScheduleVO> sc = coincidingSchedulesSearch.create();
        sc.setParameters("volumeId", volumeId);
        sc.setParameters("scheduledTimestamp", date);
        // Don't return manual snapshots. They will be executed through another
        // code path.
        sc.addAnd("policyId", SearchCriteria.Op.NEQ, 1L);
        return listBy(sc);
    }

    @Override
    public SnapshotScheduleVO findOneByVolume(long volumeId) {
        SearchCriteria<SnapshotScheduleVO> sc = VolumeIdSearch.create();
        sc.setParameters("volumeId", volumeId);
        return findOneBy(sc);
    }

    @Override
    public SnapshotScheduleVO findOneByVolumePolicy(long volumeId, long policyId) {
        SearchCriteria<SnapshotScheduleVO> sc = VolumeIdPolicyIdSearch.create();
        sc.setParameters("volumeId", volumeId);
        sc.setParameters("policyId", policyId);
        return findOneBy(sc);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public List<SnapshotScheduleVO> getSchedulesToExecute(Date currentTimestamp) {
        SearchCriteria<SnapshotScheduleVO> sc = executableSchedulesSearch.create();
        sc.setParameters("scheduledTimestamp", currentTimestamp);
        return listBy(sc);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public SnapshotScheduleVO getCurrentSchedule(Long volumeId, Long policyId, boolean executing) {
        assert volumeId != null;
        SearchCriteria<SnapshotScheduleVO> sc = createSearchCriteria();
        SearchCriteria.Op op = executing ? SearchCriteria.Op.NNULL : SearchCriteria.Op.NULL;
        sc.addAnd("volumeId", SearchCriteria.Op.EQ, volumeId);
        if (policyId != null) {
            sc.addAnd("policyId", SearchCriteria.Op.EQ, policyId);
            if (policyId != Snapshot.MANUAL_POLICY_ID) {
                // manual policies aren't scheduled by the snapshot poller, so
                // don't look for the jobId here
                sc.addAnd("asyncJobId", op);
            }
        } else {
            sc.addAnd("asyncJobId", op);
        }

        List<SnapshotScheduleVO> snapshotSchedules = listBy(sc);
        // This will return only one schedule because of a DB uniqueness
        // constraint.
        assert (snapshotSchedules.size() <= 1);
        if (snapshotSchedules.isEmpty()) {
            return null;
        } else {
            return snapshotSchedules.get(0);
        }
    }

}
