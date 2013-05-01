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

import javax.inject.Inject;

import com.cloud.api.ApiSerializerHelper;
import com.cloud.async.AsyncJob;
import com.cloud.async.AsyncJobDispatcher;
import com.cloud.async.AsyncJobManager;
import com.cloud.async.AsyncJobResult;
import com.cloud.user.AccountVO;
import com.cloud.user.UserContext;
import com.cloud.user.dao.AccountDao;
import com.cloud.utils.component.AdapterBase;

public class VmWorkJobDispatcher extends AdapterBase implements AsyncJobDispatcher {

	@Inject private VirtualMachineManager _vmMgr;
	@Inject private AsyncJobManager _asyncJobMgr;
    @Inject private AccountDao _accountDao;
	
	@Override
	public void RunJob(AsyncJob job) {
        try {
        	VmWork work = (VmWork)ApiSerializerHelper.fromSerializedString(job.getCmdInfo());
        	assert(work != null);
        	
            AccountVO account = _accountDao.findById(work.getAccountId());
            UserContext.registerContext(work.getUserId(), account, null, false);
            
            try {
            	
            	
                _asyncJobMgr.completeAsyncJob(job.getId(), AsyncJobResult.STATUS_SUCCEEDED, 0, null);
            } finally {
                UserContext.unregisterContext();
            }
        	
        } catch(Throwable e) {
            _asyncJobMgr.completeAsyncJob(job.getId(), AsyncJobResult.STATUS_FAILED, 0, null);
        }
	}
}
