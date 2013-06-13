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

import java.util.Map;

import javax.inject.Inject;

import org.apache.log4j.Logger;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import org.apache.cloudstack.context.CallContext;
import org.apache.cloudstack.framework.jobs.AsyncJob;
import org.apache.cloudstack.framework.jobs.AsyncJobConstants;
import org.apache.cloudstack.framework.jobs.AsyncJobDispatcher;
import org.apache.cloudstack.framework.jobs.AsyncJobManager;

import com.cloud.api.StringMapTypeAdapter;
import com.cloud.dao.EntityManager;
import com.cloud.user.dao.AccountDao;
import com.cloud.utils.component.AdapterBase;
import com.cloud.vm.dao.VMInstanceDao;

public class VmWorkJobDispatcher extends AdapterBase implements AsyncJobDispatcher {
    private static final Logger s_logger = Logger.getLogger(VmWorkJobDispatcher.class);

    protected static Gson s_gson;
    static {
        GsonBuilder gBuilder = new GsonBuilder();
        gBuilder.setVersion(1.3);
        gBuilder.registerTypeAdapter(Map.class, new StringMapTypeAdapter());
        s_gson = gBuilder.create();
    }

    public static String serialize(VmWork work) {
        return s_gson.toJson(work);
    }

    public static <T extends VmWork> T deserialize(Class<T> clazz, String work) {
        return s_gson.fromJson(work, clazz);
    }

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
        	
        	if (cmd.equals(Start)) {
                work = deserialize(VmWorkStart.class, job.getCmdInfo());
            } else {
                work = deserialize(VmWorkStop.class, job.getCmdInfo());
            }
        	assert(work != null);
        	
            CallContext.register(work.getUserId(), work.getAccountId(), job.getRelated());

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
                _vmMgr.orchestrateStop(vm.getUuid(), stop.isForceStop());
            }
            _asyncJobMgr.completeAsyncJob(job.getId(), AsyncJobConstants.STATUS_SUCCEEDED, 0, null);
        } catch(Throwable e) {
            s_logger.error("Unable to complete " + job, e);
            _asyncJobMgr.completeAsyncJob(job.getId(), AsyncJobConstants.STATUS_FAILED, 0, e.getMessage());
        } finally {
            CallContext.unregister();
        }
	}
}
