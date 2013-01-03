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

import java.util.ArrayList;
import java.util.Date;
import java.util.Iterator;
import java.util.List;

import javax.ejb.Local;

import org.apache.log4j.Logger;

import com.cloud.api.ApiDBUtils;
import com.cloud.api.ApiResponseHelper;
import com.cloud.api.ApiSerializerHelper;
import com.cloud.api.SerializationContext;
import com.cloud.api.query.ViewResponseHelper;
import com.cloud.api.query.vo.AccountJoinVO;
import com.cloud.api.query.vo.AsyncJobJoinVO;
import com.cloud.api.query.vo.InstanceGroupJoinVO;
import com.cloud.api.query.vo.ResourceTagJoinVO;
import com.cloud.api.query.vo.UserAccountJoinVO;
import com.cloud.api.query.vo.UserVmJoinVO;
import com.cloud.api.query.vo.VolumeJoinVO;
import com.cloud.async.AsyncJob;
import com.cloud.configuration.Resource.ResourceType;
import com.cloud.dc.DataCenter;
import com.cloud.domain.Domain;

import org.apache.cloudstack.api.BaseCmd;
import org.apache.cloudstack.api.ResponseObject;
import org.apache.cloudstack.api.ServerApiException;
import org.apache.cloudstack.api.response.AccountResponse;
import org.apache.cloudstack.api.response.AsyncJobResponse;
import org.apache.cloudstack.api.response.ListResponse;
import org.apache.cloudstack.api.response.UserResponse;
import org.apache.cloudstack.api.response.VolumeResponse;

import com.cloud.offering.ServiceOffering;
import com.cloud.server.Criteria;
import com.cloud.storage.Storage;
import com.cloud.storage.VMTemplateHostVO;
import com.cloud.storage.Volume;
import com.cloud.storage.VMTemplateStorageResourceAssoc.Status;
import com.cloud.user.Account;
import com.cloud.user.User;
import com.cloud.user.UserContext;
import com.cloud.user.UserStatisticsVO;
import com.cloud.user.UserVO;
import com.cloud.utils.db.GenericDaoBase;
import com.cloud.utils.db.SearchBuilder;
import com.cloud.utils.db.SearchCriteria;
import com.cloud.vm.VirtualMachine.State;


@Local(value={AsyncJobJoinDao.class})
public class AsyncJobJoinDaoImpl extends GenericDaoBase<AsyncJobJoinVO, Long> implements AsyncJobJoinDao {
    public static final Logger s_logger = Logger.getLogger(AsyncJobJoinDaoImpl.class);

    private SearchBuilder<AsyncJobJoinVO> jobSearch;

    private SearchBuilder<AsyncJobJoinVO> jobIdSearch;

    protected AsyncJobJoinDaoImpl() {

        jobSearch = createSearchBuilder();
        jobSearch.and("idIN", jobSearch.entity().getId(), SearchCriteria.Op.IN);
        jobSearch.done();

        jobIdSearch = createSearchBuilder();
        jobIdSearch.and("id", jobIdSearch.entity().getId(), SearchCriteria.Op.EQ);
        jobIdSearch.done();

        this._count = "select count(distinct id) from async_job_view WHERE ";
    }





    @Override
    public AsyncJobResponse newAsyncJobResponse(AsyncJobJoinVO job) {
        AsyncJobResponse jobResponse = new AsyncJobResponse();
        jobResponse.setAccountId(job.getAccountUuid());
        jobResponse.setUserId(job.getUserUuid());
        jobResponse.setCmd(job.getCmd());
        jobResponse.setCreated(job.getCreated());
        jobResponse.setJobId(job.getUuid());
        jobResponse.setJobStatus(job.getStatus());
        jobResponse.setJobProcStatus(job.getProcessStatus());

        if (job.getInstanceType() != null && job.getInstanceId() != null) {
            jobResponse.setJobInstanceType(job.getInstanceType().toString());

            jobResponse.setJobInstanceId(job.getInstanceUuid());

        }
        jobResponse.setJobResultCode(job.getResultCode());

        boolean savedValue = SerializationContext.current().getUuidTranslation();
        SerializationContext.current().setUuidTranslation(false);

        Object resultObject = ApiSerializerHelper.fromSerializedString(job.getResult());
        jobResponse.setJobResult((ResponseObject) resultObject);
        SerializationContext.current().setUuidTranslation(savedValue);

        if (resultObject != null) {
            Class<?> clz = resultObject.getClass();
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
    public List<AsyncJobJoinVO> searchByIds(Long... ids) {
        SearchCriteria<AsyncJobJoinVO> sc = jobSearch.create();
        sc.setParameters("idIN", ids);
        return searchIncludingRemoved(sc, null, null, false);
    }



}
