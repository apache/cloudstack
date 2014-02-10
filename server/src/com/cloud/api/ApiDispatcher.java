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

import com.cloud.event.EventTypes;
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
import com.cloud.api.dispatch.DispatchTask;

public class ApiDispatcher {
    private static final Logger s_logger = Logger.getLogger(ApiDispatcher.class.getName());

    Long _createSnapshotQueueSizeLimit;

    @Inject
    AsyncJobManager _asyncMgr;

    @Inject()
    protected DispatchChainFactory dispatchChainFactory;

    protected DispatchChain standardDispatchChain;

    protected DispatchChain asyncCreationDispatchChain;

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


    public void dispatchCreateCmd(final BaseAsyncCreateCmd cmd, final Map<String, String> params) throws Exception {
        asyncCreationDispatchChain.dispatch(new DispatchTask(cmd, params));
        CallContext.current().setEventDisplayEnabled(cmd.isDisplayResourceEnabled());
    }

    public void dispatch(final BaseCmd cmd, final Map<String, String> params, final boolean execute) throws Exception {
        // Let the chain of responsibility dispatch gradually
        standardDispatchChain.dispatch(new DispatchTask(cmd, params));

        final CallContext ctx = CallContext.current();
        ctx.setEventDisplayEnabled(cmd.isDisplayResourceEnabled());

        // TODO This if shouldn't be here. Use polymorphism and move it to validateSpecificParameters
        if (cmd instanceof BaseAsyncCmd) {

            final BaseAsyncCmd asyncCmd = (BaseAsyncCmd)cmd;
            final String startEventId = params.get(ApiConstants.CTX_START_EVENT_ID);
            String uuid = params.get("uuid");
            ctx.setStartEventId(Long.valueOf(startEventId));

            // Fow now use the key from EventTypes.java rather than getInstanceType bcz the later doesn't refer to the interfaces
            if(EventTypes.getEntityForEvent(asyncCmd.getEventType()) != null){
                ctx.putContextParameter(EventTypes.getEntityForEvent(asyncCmd.getEventType()), uuid);
            }

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

        // TODO This if shouldn't be here. Use polymorphism and move it to validateSpecificParameters
        if (cmd instanceof BaseAsyncCustomIdCmd) {
            ((BaseAsyncCustomIdCmd)cmd).checkUuid();
        } else if (cmd instanceof BaseCustomIdCmd) {
            ((BaseCustomIdCmd)cmd).checkUuid();
        }

        cmd.execute();
    }

}
