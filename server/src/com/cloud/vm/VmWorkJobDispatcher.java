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

import org.apache.log4j.Logger;

import org.apache.cloudstack.context.CallContext;
import org.apache.cloudstack.framework.jobs.AsyncJob;
import org.apache.cloudstack.framework.jobs.AsyncJobConstants;
import org.apache.cloudstack.framework.jobs.AsyncJobDispatcher;
import org.apache.cloudstack.framework.jobs.AsyncJobManager;

import com.cloud.api.ApiSerializerHelper;
import com.cloud.dao.EntityManager;
import com.cloud.user.dao.AccountDao;
import com.cloud.utils.component.AdapterBase;
import com.cloud.vm.dao.VMInstanceDao;

public class VmWorkJobDispatcher extends AdapterBase implements AsyncJobDispatcher {
    private static final Logger s_logger = Logger.getLogger(VmWorkJobDispatcher.class);

    public static final String VM_WORK_QUEUE = "VmWorkJobQueue";
    public static final String VM_WORK_JOB_DISPATCHER = "VmWorkJobDispatcher";
    public static final String VM_WORK_JOB_WAKEUP_DISPATCHER = "VmWorkJobWakeupDispatcher";
    public final static String Start = "start";
    public final static String Stop = "stop";

    @Inject
    private VirtualMachineManagerImpl _vmMgr;
	@Inject private AsyncJobManager _asyncJobMgr;
    @Inject private AccountDao _accountDao;
    @Inject private VMInstanceDao _instanceDao;
    @Inject
    private EntityManager _entityMgr;
    
	@Override
    public void runJob(AsyncJob job) {
        VmWork work = null;
        try {
        	String cmd = job.getCmd();
        	assert(cmd != null);
        	
            work = (VmWork)ApiSerializerHelper.fromSerializedString(job.getCmdInfo());
        	assert(work != null);
        	
            CallContext context = CallContext.register(work.getUserId(), work.getAccountId(), "job-" + job.getShortUuid());

            VMInstanceVO vm = _instanceDao.findById(work.getVmId());
            if (vm == null) {
                s_logger.info("Unable to find vm " + work.getVmId());
            }
            assert(vm != null);
    
            if (cmd.equals(Start)) {
                VmWorkStart start = (VmWorkStart)work;
                _vmMgr.orchestrateStart(vm.getUuid(), start.getParams(), start.getPlan());
            } else if (cmd.equals(Stop)) {
                VmWorkStop stop = (VmWorkStop)work;
                _vmMgr.processVmStopWork(vm.getUuid(), stop.isForceStop());
            }
            _asyncJobMgr.completeAsyncJob(job.getId(), AsyncJobConstants.STATUS_SUCCEEDED, 0, null);
        } catch(Throwable e) {
            s_logger.error("Unable to complete " + job, e);
            _asyncJobMgr.completeAsyncJob(job.getId(), AsyncJobConstants.STATUS_FAILED, 0, e);
        } finally {
            CallContext.unregister();
        }
	}
}
