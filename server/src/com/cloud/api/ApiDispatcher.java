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

import java.util.Map;

import javax.annotation.PostConstruct;
import javax.inject.Inject;

import org.apache.cloudstack.api.ApiConstants;
import org.apache.cloudstack.api.BaseAsyncCmd;
import org.apache.cloudstack.api.BaseAsyncCreateCmd;
import org.apache.cloudstack.api.BaseAsyncCustomIdCmd;
import org.apache.cloudstack.api.BaseCmd;
import org.apache.cloudstack.api.BaseCustomIdCmd;
import org.apache.cloudstack.context.CallContext;
import org.apache.cloudstack.framework.jobs.AsyncJob;
import org.apache.cloudstack.framework.jobs.AsyncJobManager;
import org.apache.log4j.Logger;

import com.cloud.api.dispatch.DispatchChain;
import com.cloud.api.dispatch.DispatchChainFactory;

public class ApiDispatcher {
    private static final Logger s_logger = Logger.getLogger(ApiDispatcher.class.getName());

    Long _createSnapshotQueueSizeLimit;

    @Inject
    AsyncJobManager _asyncMgr = null;

    @Inject()
    protected DispatchChainFactory dispatchChainFactory = null;

    protected DispatchChain standardDispatchChain = null;

    protected DispatchChain asyncCreationDispatchChain = null;

    public ApiDispatcher() {
    }

    @PostConstruct
    public void setup() {
        standardDispatchChain = dispatchChainFactory.getStandardDispatchChain();
        asyncCreationDispatchChain = dispatchChainFactory.getAsyncCreationDispatchChain();
    }

    public void setCreateSnapshotQueueSizeLimit(final Long snapshotLimit) {
        _createSnapshotQueueSizeLimit = snapshotLimit;
    }

    public void dispatchCreateCmd(final BaseAsyncCreateCmd cmd, final Map<String, Object> params) throws Exception {
        asyncCreationDispatchChain.dispatch(cmd, params);
    }

    public void dispatch(final BaseCmd cmd, final Map<String, Object> params, final boolean execute) throws Exception {
        // Let the chain of responsibility dispatch gradually
        standardDispatchChain.dispatch(cmd, params);

        final CallContext ctx = CallContext.current();

        if (cmd instanceof BaseAsyncCmd) {

            final BaseAsyncCmd asyncCmd = (BaseAsyncCmd)cmd;
            final String startEventId = (String) params.get(ApiConstants.CTX_START_EVENT_ID);
            ctx.setStartEventId(Long.valueOf(startEventId));

            // Synchronise job on the object if needed
            if (asyncCmd.getJob() != null && asyncCmd.getSyncObjId() != null && asyncCmd.getSyncObjType() != null) {
                Long queueSizeLimit = null;
                if (asyncCmd.getSyncObjType() != null && asyncCmd.getSyncObjType().equalsIgnoreCase(BaseAsyncCmd.snapshotHostSyncObject)) {
                    queueSizeLimit = _createSnapshotQueueSizeLimit;
                } else {
                    queueSizeLimit = 1L;
                }

                if (queueSizeLimit != null) {
                    if (!execute) {
                        // if we are not within async-execution context, enqueue the command
                        _asyncMgr.syncAsyncJobExecution((AsyncJob)asyncCmd.getJob(), asyncCmd.getSyncObjType(), asyncCmd.getSyncObjId().longValue(), queueSizeLimit);
                        return;
                    }
                } else {
                    s_logger.trace("The queue size is unlimited, skipping the synchronizing");
                }
            }
        }

        if (cmd instanceof BaseAsyncCustomIdCmd) {
            ((BaseAsyncCustomIdCmd)cmd).checkUuid();
        } else if (cmd instanceof BaseCustomIdCmd) {
            ((BaseCustomIdCmd)cmd).checkUuid();
        }

        cmd.execute();
    }

}
