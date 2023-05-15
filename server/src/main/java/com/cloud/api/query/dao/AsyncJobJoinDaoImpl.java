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
package com.cloud.api.query.dao;

import java.util.Date;
import java.util.List;


import org.apache.log4j.Logger;
import org.springframework.stereotype.Component;

import org.apache.cloudstack.api.ResponseObject;
import org.apache.cloudstack.api.response.AsyncJobResponse;
import org.apache.cloudstack.framework.jobs.AsyncJob;

import com.cloud.api.ApiSerializerHelper;
import com.cloud.api.SerializationContext;
import com.cloud.api.query.vo.AsyncJobJoinVO;
import com.cloud.utils.db.GenericDaoBase;
import com.cloud.utils.db.SearchBuilder;
import com.cloud.utils.db.SearchCriteria;

@Component
public class AsyncJobJoinDaoImpl extends GenericDaoBase<AsyncJobJoinVO, Long> implements AsyncJobJoinDao {
    public static final Logger s_logger = Logger.getLogger(AsyncJobJoinDaoImpl.class);

    private final SearchBuilder<AsyncJobJoinVO> jobIdSearch;

    protected AsyncJobJoinDaoImpl() {

        jobIdSearch = createSearchBuilder();
        jobIdSearch.and("id", jobIdSearch.entity().getId(), SearchCriteria.Op.EQ);
        jobIdSearch.done();

        _count = "select count(distinct id) from async_job_view WHERE ";
    }

    @Override
    public AsyncJobResponse newAsyncJobResponse(final AsyncJobJoinVO job) {
        final AsyncJobResponse jobResponse = new AsyncJobResponse();
        jobResponse.setAccountId(job.getAccountUuid());
        jobResponse.setAccount(job.getAccountName());
        jobResponse.setDomainId(job.getDomainUuid());
        StringBuilder domainPath = new StringBuilder("ROOT");
        (domainPath.append(job.getDomainPath())).deleteCharAt(domainPath.length() - 1);
        jobResponse.setDomainPath(domainPath.toString());
        jobResponse.setUserId(job.getUserUuid());
        jobResponse.setCmd(job.getCmd());
        jobResponse.setCreated(job.getCreated());
        jobResponse.setRemoved(job.getRemoved());
        jobResponse.setJobId(job.getUuid());
        jobResponse.setJobStatus(job.getStatus());
        jobResponse.setJobProcStatus(job.getProcessStatus());
        jobResponse.setMsid(job.getExecutingMsid());

        if (job.getInstanceType() != null && job.getInstanceId() != null) {
            jobResponse.setJobInstanceType(job.getInstanceType().toString());

            jobResponse.setJobInstanceId(job.getInstanceUuid());

        }
        jobResponse.setJobResultCode(job.getResultCode());

        final boolean savedValue = SerializationContext.current().getUuidTranslation();
        SerializationContext.current().setUuidTranslation(false);

        final Object resultObject = ApiSerializerHelper.fromSerializedString(job.getResult());
        jobResponse.setJobResult((ResponseObject)resultObject);
        SerializationContext.current().setUuidTranslation(savedValue);

        if (resultObject != null) {
            final Class<?> clz = resultObject.getClass();
            if (clz.isPrimitive() || clz.getSuperclass() == Number.class || clz == String.class || clz == Date.class) {
                jobResponse.setJobResultType("text");
            } else {
                jobResponse.setJobResultType("object");
            }
        }

        jobResponse.setObjectName("asyncjobs");
        return jobResponse;
    }

    @Override
    public AsyncJobJoinVO newAsyncJobView(AsyncJob job) {
        SearchCriteria<AsyncJobJoinVO> sc = jobIdSearch.create();
        sc.setParameters("id", job.getId());
        List<AsyncJobJoinVO> accounts = searchIncludingRemoved(sc, null, null, false);
        assert accounts != null && accounts.size() == 1 : "No async job found for job id " + job.getId();
        return accounts.get(0);

    }

}
