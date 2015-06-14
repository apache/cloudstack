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

import org.apache.cloudstack.context.CallContext;
import org.apache.cloudstack.framework.jobs.AsyncJob;
import org.apache.cloudstack.framework.jobs.AsyncJobDispatcher;
import org.apache.cloudstack.framework.jobs.AsyncJobManager;
import org.apache.cloudstack.jobs.JobInfo;

import com.cloud.exception.InvalidParameterValueException;
import com.cloud.utils.Pair;
import com.cloud.utils.component.AdapterBase;
import com.cloud.vm.dao.VMInstanceDao;

public class VmWorkJobDispatcher extends AdapterBase implements AsyncJobDispatcher {
    private static final Logger s_logger = Logger.getLogger(VmWorkJobDispatcher.class);

    @Inject private VirtualMachineManagerImpl _vmMgr;
    @Inject
    private AsyncJobManager _asyncJobMgr;
    @Inject private VMInstanceDao _instanceDao;

    private Map<String, VmWorkJobHandler> _handlers;

    public VmWorkJobDispatcher() {
    }

    public Map<String, VmWorkJobHandler> getHandlers() {
        return _handlers;
    }

    public void setHandlers(Map<String, VmWorkJobHandler> handlers) {
        _handlers = handlers;
    }

    @Override
    public void runJob(AsyncJob job) {
        VmWork work = null;
        try {
            String cmd = job.getCmd();
            assert (cmd != null);

            Class<?> workClz = null;
            try {
                workClz = Class.forName(job.getCmd());
            } catch (ClassNotFoundException e) {
                s_logger.error("VM work class " + cmd + " is not found" + ", job origin: " + job.getRelated(), e);
                _asyncJobMgr.completeAsyncJob(job.getId(), JobInfo.Status.FAILED, 0, e.getMessage());
                return;
            }

            work = VmWorkSerializer.deserialize(workClz, job.getCmdInfo());
            if(work == null) {
                s_logger.error("Unable to deserialize VM work " + job.getCmd() + ", job info: " + job.getCmdInfo() + ", job origin: " + job.getRelated());
                _asyncJobMgr.completeAsyncJob(job.getId(), JobInfo.Status.FAILED, 0, "Unable to deserialize VM work");
                return;
            }

            if (s_logger.isDebugEnabled())
                s_logger.debug("Run VM work job: " + cmd + " for VM " + work.getVmId() + ", job origin: " + job.getRelated());
            try {
                if (_handlers == null || _handlers.isEmpty()) {
                    s_logger.error("Invalid startup configuration, no work job handler is found. cmd: " + job.getCmd() + ", job info: " + job.getCmdInfo()
                            + ", job origin: " + job.getRelated());
                    _asyncJobMgr.completeAsyncJob(job.getId(), JobInfo.Status.FAILED, 0, "Invalid startup configuration. no job handler is found");
                    return;
                }

                VmWorkJobHandler handler = _handlers.get(work.getHandlerName());

                if (handler == null) {
                    s_logger.error("Unable to find work job handler. handler name: " + work.getHandlerName() + ", job cmd: " + job.getCmd()
                            + ", job info: " + job.getCmdInfo() + ", job origin: " + job.getRelated());
                    _asyncJobMgr.completeAsyncJob(job.getId(), JobInfo.Status.FAILED, 0, "Unable to find work job handler");
                    return;
                }

                CallContext.register(work.getUserId(), work.getAccountId());

                try {
                    Pair<JobInfo.Status, String> result = handler.handleVmWorkJob(work);
                    _asyncJobMgr.completeAsyncJob(job.getId(), result.first(), 0, result.second());
                } finally {
                    CallContext.unregister();
                }
            } finally {
                if (s_logger.isDebugEnabled())
                    s_logger.debug("Done with run of VM work job: " + cmd + " for VM " + work.getVmId() + ", job origin: " + job.getRelated());
            }
        } catch(InvalidParameterValueException e) {
            s_logger.error("Unable to complete " + job + ", job origin:" + job.getRelated());
            _asyncJobMgr.completeAsyncJob(job.getId(), JobInfo.Status.FAILED, 0, _asyncJobMgr.marshallResultObject(e));
        } catch(Throwable e) {
            s_logger.error("Unable to complete " + job + ", job origin:" + job.getRelated(), e);

            //RuntimeException ex = new RuntimeException("Job failed due to exception " + e.getMessage());
            _asyncJobMgr.completeAsyncJob(job.getId(), JobInfo.Status.FAILED, 0, _asyncJobMgr.marshallResultObject(e));
        }
    }
}
