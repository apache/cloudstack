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

import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import javax.inject.Inject;

import org.apache.cloudstack.api.APICommand;
import org.apache.cloudstack.api.ResponseObject;
import org.apache.cloudstack.api.response.AsyncJobResponse;
import org.apache.cloudstack.api.response.ExceptionResponse;
import org.apache.cloudstack.jobs.JobInfo;
import org.apache.cloudstack.framework.jobs.AsyncJob;
import org.apache.cloudstack.framework.jobs.AsyncJobManager;
import org.apache.commons.collections.CollectionUtils;
import org.springframework.stereotype.Component;

import com.cloud.api.ApiResponseHelper;
import com.cloud.api.ApiSerializerHelper;
import com.cloud.api.SerializationContext;
import com.cloud.api.query.vo.AsyncJobJoinVO;
import com.cloud.cluster.ManagementServerHostVO;
import com.cloud.cluster.dao.ManagementServerHostDao;
import com.cloud.utils.db.GenericDaoBase;
import com.cloud.utils.db.SearchBuilder;
import com.cloud.utils.db.SearchCriteria;

@Component
public class AsyncJobJoinDaoImpl extends GenericDaoBase<AsyncJobJoinVO, Long> implements AsyncJobJoinDao {

    @Inject
    private ManagementServerHostDao managementServerHostDao;

    private final SearchBuilder<AsyncJobJoinVO> jobIdSearch;

    protected AsyncJobJoinDaoImpl() {

        jobIdSearch = createSearchBuilder();
        jobIdSearch.and("id", jobIdSearch.entity().getId(), SearchCriteria.Op.EQ);
        jobIdSearch.done();

        _count = "select count(distinct id) from async_job_view WHERE ";
    }

    private String managementServerName(final Long msid) {
        if (msid == null) {
            return null;
        }
        final ManagementServerHostVO managementServer = managementServerHostDao.findByMsid(msid);
        return managementServer != null ? managementServer.getName() : null;
    }

    @Inject
    private AsyncJobManager asyncJobManager;

    private static final Map<String, Boolean> cancellableCommands = new ConcurrentHashMap<>();

    /** The same annotation cancelAsyncJob checks; a command class that cannot be loaded is reported as not cancellable. */
    static boolean isCancellableCommand(final String cmd) {
        if (cmd == null) {
            return false;
        }
        return cancellableCommands.computeIfAbsent(cmd, name -> {
            try {
                final APICommand annotation = Class.forName(name).getAnnotation(APICommand.class);
                return annotation != null && annotation.cancellable();
            } catch (final ClassNotFoundException e) {
                return false;
            }
        });
    }

    @Override
    public AsyncJobResponse newAsyncJobResponse(final AsyncJobJoinVO job) {
        final AsyncJobResponse jobResponse = new AsyncJobResponse();
        jobResponse.setAccountId(job.getAccountUuid());
        jobResponse.setAccount(job.getAccountName());
        jobResponse.setDomainId(job.getDomainUuid());
        jobResponse.setDomain(job.getDomainName());
        jobResponse.setDomainPath(ApiResponseHelper.getPrettyDomainPath(job.getDomainPath()));
        jobResponse.setUserId(job.getUserUuid());
        jobResponse.setCmd(job.getCmd());
        jobResponse.setCancellable(isCancellableCommand(job.getCmd())
                || (job.getStatus() == JobInfo.Status.IN_PROGRESS.value() && asyncJobManager.hasOnlyQueuedWork(job.getId())));
        jobResponse.setCreated(job.getCreated());
        jobResponse.setRemoved(job.getRemoved());
        jobResponse.setId(job.getUuid());
        jobResponse.setJobId(job.getUuid());
        jobResponse.setJobStatus(job.getStatus());
        // while the cancellation is carried out the job's result holds the request; it is reported as the reason, not as a result
        final boolean cancelling = job.isCancelRequested() && job.getStatus() == JobInfo.Status.IN_PROGRESS.value();
        if (job.isCancelRequested()) {
            jobResponse.setCancelRequested(true);
        }
        jobResponse.setJobProcStatus(job.getProcessStatus());
        // a finished job no longer has an executing server; the one that completed it is the answer then
        final Long msid = job.getExecutingMsid() != null ? job.getExecutingMsid() : job.getCompleteMsid();
        if (msid != null) {
            ManagementServerHostVO managementServer = managementServerHostDao.findByMsid(msid);
            if (managementServer != null) {
                jobResponse.setManagementServerId(managementServer.getUuid());
                jobResponse.setManagementServerName(managementServer.getName());
            }
        }

        jobResponse.setInitManagementServerName(managementServerName(job.getInitMsid()));
        jobResponse.setExecutingManagementServerName(managementServerName(job.getExecutingMsid()));
        jobResponse.setCompleteManagementServerName(managementServerName(job.getCompleteMsid()));
        if (job.getInstanceType() != null && job.getInstanceId() != null) {
            jobResponse.setJobInstanceType(job.getInstanceType().toString());
            jobResponse.setJobInstanceId(job.getInstanceUuid());
            jobResponse.setJobInstanceName(job.getInstanceName());
        }
        jobResponse.setJobResultCode(job.getResultCode());

        final boolean savedValue = SerializationContext.current().getUuidTranslation();
        SerializationContext.current().setUuidTranslation(false);

        final Object resultObject = ApiSerializerHelper.fromSerializedString(job.getResult());
        if (cancelling) {
            if (resultObject instanceof ExceptionResponse) {
                jobResponse.setCancelReason(((ExceptionResponse)resultObject).getErrorText());
            }
        } else {
            jobResponse.setJobResult((ResponseObject)resultObject);
        }
        SerializationContext.current().setUuidTranslation(savedValue);

        if (resultObject != null && !cancelling) {
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

    @Override
    public List<AsyncJobJoinVO> listByIds(List<Long> ids) {
        if (CollectionUtils.isEmpty(ids)) {
            return Collections.emptyList();
        }
        SearchBuilder<AsyncJobJoinVO> idsSearch = createSearchBuilder();
        idsSearch.and("ids", idsSearch.entity().getId(), SearchCriteria.Op.IN);
        idsSearch.done();
        SearchCriteria<AsyncJobJoinVO> sc = idsSearch.create();
        sc.setParameters("ids", ids.toArray());
        return listBy(sc);
    }
}
