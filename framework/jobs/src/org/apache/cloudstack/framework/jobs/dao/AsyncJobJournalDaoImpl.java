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
package org.apache.cloudstack.framework.jobs.dao;

import java.util.List;

import org.apache.cloudstack.framework.jobs.impl.AsyncJobJournalVO;

import com.cloud.utils.db.GenericDaoBase;
import com.cloud.utils.db.SearchBuilder;
import com.cloud.utils.db.SearchCriteria;
import com.cloud.utils.db.SearchCriteria.Op;

public class AsyncJobJournalDaoImpl extends GenericDaoBase<AsyncJobJournalVO, Long> implements AsyncJobJournalDao {

    private final SearchBuilder<AsyncJobJournalVO> JobJournalSearch;

    public AsyncJobJournalDaoImpl() {
        JobJournalSearch = createSearchBuilder();
        JobJournalSearch.and("jobId", JobJournalSearch.entity().getJobId(), Op.EQ);
        JobJournalSearch.done();
    }

    @Override
    public List<AsyncJobJournalVO> getJobJournal(long jobId) {
        SearchCriteria<AsyncJobJournalVO> sc = JobJournalSearch.create();
        sc.setParameters("jobId", jobId);

        return this.listBy(sc);
    }
}
