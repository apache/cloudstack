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

import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.inject.Inject;

import org.apache.log4j.Logger;

import org.apache.cloudstack.context.CallContext;
import org.apache.cloudstack.framework.jobs.AsyncJob;
import org.apache.cloudstack.framework.jobs.AsyncJobDispatcher;
import org.apache.cloudstack.framework.jobs.AsyncJobManager;
import org.apache.cloudstack.framework.jobs.dao.AsyncJobJoinMapDao;
import org.apache.cloudstack.framework.jobs.dao.VmWorkJobDao;
import org.apache.cloudstack.framework.jobs.impl.AsyncJobJoinMapVO;
import org.apache.cloudstack.framework.jobs.impl.VmWorkJobVO;

import com.cloud.user.AccountVO;
import com.cloud.user.dao.AccountDao;
import com.cloud.utils.component.AdapterBase;
import com.cloud.vm.dao.VMInstanceDao;

/**
 * Please note: VmWorkJobWakeupDispatcher is not currently in use. It is designed for event-driven based
 * job processing model.
 *
 * Current code base uses blocking calls to wait for job completion
 */
public class VmWorkJobWakeupDispatcher extends AdapterBase implements AsyncJobDispatcher {
    private static final Logger s_logger = Logger.getLogger(VmWorkJobWakeupDispatcher.class);

    @Inject
    private VmWorkJobDao _workjobDao;
    @Inject
    private AsyncJobJoinMapDao _joinMapDao;
    @Inject
    private AccountDao _accountDao;
    @Inject
    private VMInstanceDao _instanceDao;
    @Inject
    private VirtualMachineManager _vmMgr;
    @Inject
    private AsyncJobManager _asyncJobMgr;

    private final Map<String, Method> _handlerMap = new HashMap<String, Method>();

    @Override
    public void runJob(AsyncJob job) {
        try {
            List<AsyncJobJoinMapVO> joinRecords = _joinMapDao.listJoinRecords(job.getId());
            if (joinRecords.size() != 1) {
                s_logger.warn("AsyncJob-" + job.getId()
                        + " received wakeup call with un-supported joining job number: " + joinRecords.size());

                // if we fail wakeup-execution for any reason, avoid release sync-source if there is any
                job.setSyncSource(null);
                return;
            }

            AsyncJobJoinMapVO joinRecord = joinRecords.get(0);
            VmWorkJobVO joinedJob = _workjobDao.findById(joinRecord.getJoinJobId());

            Class<?> workClz = null;
            try {
                workClz = Class.forName(job.getCmd());
            } catch (ClassNotFoundException e) {
                s_logger.error("VM work class " + job.getCmd() + " is not found", e);
                return;
            }

            // get original work context information from joined job
            VmWork work = VmWorkSerializer.deserialize(workClz, joinedJob.getCmdInfo());
            assert (work != null);

            AccountVO account = _accountDao.findById(work.getAccountId());
            assert (account != null);

            VMInstanceVO vm = _instanceDao.findById(work.getVmId());
            assert (vm != null);

            CallContext.register(work.getUserId(), work.getAccountId(), job.getRelated());
            try {
                Method handler = getHandler(joinRecord.getWakeupHandler());
                if (handler != null) {
                    handler.invoke(_vmMgr);
                } else {
                    assert (false);
                    s_logger.error("Unable to find wakeup handler " + joinRecord.getWakeupHandler() +
                            " when waking up job-" + job.getId());
                }
            } finally {
                CallContext.unregister();
            }
        } catch (Throwable e) {
            s_logger.warn("Unexpected exception in waking up job-" + job.getId());

            // if we fail wakeup-execution for any reason, avoid release sync-source if there is any
            job.setSyncSource(null);
        }
    }

    private Method getHandler(String wakeupHandler) {

        synchronized (_handlerMap) {
            Class<?> clz = _vmMgr.getClass();
            Method method = _handlerMap.get(wakeupHandler);
            if (method != null)
                return method;

            try {
                method = clz.getMethod(wakeupHandler);
                method.setAccessible(true);
            } catch (SecurityException e) {
                assert (false);
                s_logger.error("Unexpected exception", e);
                return null;
            } catch (NoSuchMethodException e) {
                assert (false);
                s_logger.error("Unexpected exception", e);
                return null;
            }

            _handlerMap.put(wakeupHandler, method);
            return method;
        }
    }
}
