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
package com.cloud.vm;

import java.util.List;


import org.springframework.stereotype.Component;

import com.cloud.utils.db.GenericDaoBase;
import com.cloud.utils.db.SearchBuilder;
import com.cloud.utils.db.SearchCriteria;
import com.cloud.utils.db.SearchCriteria.Op;
import com.cloud.utils.time.InaccurateClock;
import com.cloud.vm.ItWorkVO.Step;
import com.cloud.vm.VirtualMachine.State;

@Component
public class ItWorkDaoImpl extends GenericDaoBase<ItWorkVO, String> implements ItWorkDao {
    protected final SearchBuilder<ItWorkVO> AllFieldsSearch;
    protected final SearchBuilder<ItWorkVO> CleanupSearch;
    protected final SearchBuilder<ItWorkVO> OutstandingWorkSearch;
    protected final SearchBuilder<ItWorkVO> WorkInProgressSearch;

    protected ItWorkDaoImpl() {
        super();

        AllFieldsSearch = createSearchBuilder();
        AllFieldsSearch.and("instance", AllFieldsSearch.entity().getInstanceId(), Op.EQ);
        AllFieldsSearch.and("op", AllFieldsSearch.entity().getType(), Op.EQ);
        AllFieldsSearch.and("step", AllFieldsSearch.entity().getStep(), Op.EQ);
        AllFieldsSearch.done();

        CleanupSearch = createSearchBuilder();
        CleanupSearch.and("step", CleanupSearch.entity().getType(), Op.IN);
        CleanupSearch.and("time", CleanupSearch.entity().getUpdatedAt(), Op.LT);
        CleanupSearch.done();

        OutstandingWorkSearch = createSearchBuilder();
        OutstandingWorkSearch.and("instance", OutstandingWorkSearch.entity().getInstanceId(), Op.EQ);
        OutstandingWorkSearch.and("op", OutstandingWorkSearch.entity().getType(), Op.EQ);
        OutstandingWorkSearch.and("step", OutstandingWorkSearch.entity().getStep(), Op.NEQ);
        OutstandingWorkSearch.done();

        WorkInProgressSearch = createSearchBuilder();
        WorkInProgressSearch.and("server", WorkInProgressSearch.entity().getManagementServerId(), Op.EQ);
        WorkInProgressSearch.and("step", WorkInProgressSearch.entity().getStep(), Op.NIN);
        WorkInProgressSearch.done();
    }

    @Override
    public ItWorkVO findByOutstandingWork(long instanceId, State state) {
        SearchCriteria<ItWorkVO> sc = OutstandingWorkSearch.create();
        sc.setParameters("instance", instanceId);
        sc.setParameters("op", state);
        sc.setParameters("step", Step.Done);

        return findOneBy(sc);
    }

    @Override
    public void cleanup(long wait) {
        SearchCriteria<ItWorkVO> sc = CleanupSearch.create();
        sc.setParameters("step", Step.Done);
        sc.setParameters("time", InaccurateClock.getTimeInSeconds() - wait);

        remove(sc);
    }

    @Override
    public boolean update(String id, ItWorkVO work) {
        work.setUpdatedAt(InaccurateClock.getTimeInSeconds());

        return super.update(id, work);
    }

    @Override
    public boolean updateStep(ItWorkVO work, Step step) {
        work.setStep(step);
        return update(work.getId(), work);
    }

    @Override
    public List<ItWorkVO> listWorkInProgressFor(long nodeId) {
        SearchCriteria<ItWorkVO> sc = WorkInProgressSearch.create();
        sc.setParameters("server", nodeId);
        sc.setParameters("step", Step.Done);

        return search(sc, null);

    }
}
