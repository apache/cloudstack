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
package com.cloud.api;

import java.lang.reflect.Type;
import java.util.Map;

import javax.inject.Inject;

import org.apache.log4j.Logger;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import org.apache.cloudstack.api.ApiErrorCode;
import org.apache.cloudstack.api.BaseAsyncCmd;
import org.apache.cloudstack.api.BaseAsyncCreateCmd;
import org.apache.cloudstack.api.ServerApiException;
import org.apache.cloudstack.api.response.ExceptionResponse;
import org.apache.cloudstack.context.CallContext;
import org.apache.cloudstack.framework.jobs.AsyncJob;
import org.apache.cloudstack.framework.jobs.AsyncJobConstants;
import org.apache.cloudstack.framework.jobs.AsyncJobDispatcher;
import org.apache.cloudstack.framework.jobs.AsyncJobManager;

import com.cloud.user.Account;
import com.cloud.user.dao.AccountDao;
import com.cloud.utils.component.AdapterBase;
import com.cloud.utils.component.ComponentContext;

public class ApiAsyncJobDispatcher extends AdapterBase implements AsyncJobDispatcher {
    private static final Logger s_logger = Logger.getLogger(ApiAsyncJobDispatcher.class);

    @Inject private ApiDispatcher _dispatcher;
    
    @Inject private AsyncJobManager _asyncJobMgr;
    @Inject private AccountDao _accountDao;
    
    public ApiAsyncJobDispatcher() {
    }
    
	@Override
    public void runJob(AsyncJob job) {
        BaseAsyncCmd cmdObj = null;
        try {
            Class<?> cmdClass = Class.forName(job.getCmd());
            cmdObj = (BaseAsyncCmd)cmdClass.newInstance();
            cmdObj = ComponentContext.inject(cmdObj);
            cmdObj.configure();
            cmdObj.setJob(job);
            
            Type mapType = new TypeToken<Map<String, String>>() {}.getType();
            Gson gson = ApiGsonHelper.getBuilder().create();
            Map<String, String> params = gson.fromJson(job.getCmdInfo(), mapType);

            // whenever we deserialize, the UserContext needs to be updated
            String userIdStr = params.get("ctxUserId");
            String acctIdStr = params.get("ctxAccountId");
            Long userId = null;
            Account accountObject = null;

            if (cmdObj instanceof BaseAsyncCreateCmd) {
                BaseAsyncCreateCmd create = (BaseAsyncCreateCmd)cmdObj;
                create.setEntityId(Long.parseLong(params.get("id")));
                create.setEntityUuid(params.get("uuid"));
            }

            if (userIdStr != null) {
                userId = Long.parseLong(userIdStr);
            }

            if (acctIdStr != null) {
                accountObject = _accountDao.findById(Long.parseLong(acctIdStr));
            }

            CallContext.register(userId, accountObject, job.getRelated(), false);
            try {
                // dispatch could ultimately queue the job
                _dispatcher.dispatch(cmdObj, params, true);

                // serialize this to the async job table
                _asyncJobMgr.completeAsyncJob(job.getId(), AsyncJobConstants.STATUS_SUCCEEDED, 0, cmdObj.getResponseObject());
            } finally {
                CallContext.unregister();
            }
        } catch(Throwable e) {
            String errorMsg = null;
            int errorCode = ApiErrorCode.INTERNAL_ERROR.getHttpCode();
            if (!(e instanceof ServerApiException)) {
                s_logger.error("Unexpected exception while executing " + job.getCmd(), e);
                errorMsg = e.getMessage();
            } else {
                ServerApiException sApiEx = (ServerApiException)e;
                errorMsg = sApiEx.getDescription();
                errorCode = sApiEx.getErrorCode().getHttpCode();
            }

            ExceptionResponse response = new ExceptionResponse();
            response.setErrorCode(errorCode);
            response.setErrorText(errorMsg);
            response.setResponseName((cmdObj == null) ? "unknowncommandresponse" : cmdObj.getCommandName());

            // FIXME:  setting resultCode to ApiErrorCode.INTERNAL_ERROR is not right, usually executors have their exception handling
            //         and we need to preserve that as much as possible here
            _asyncJobMgr.completeAsyncJob(job.getId(), AsyncJobConstants.STATUS_FAILED, ApiErrorCode.INTERNAL_ERROR.getHttpCode(), response);
        }
	}
}
