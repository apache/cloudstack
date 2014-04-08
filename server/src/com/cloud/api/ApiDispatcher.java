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

import org.apache.log4j.Logger;

import org.apache.cloudstack.acl.ControlledEntity;
import org.apache.cloudstack.acl.InfrastructureEntity;
import org.apache.cloudstack.acl.SecurityChecker.AccessType;
import org.apache.cloudstack.api.APICommand;
import org.apache.cloudstack.api.ApiConstants;
import org.apache.cloudstack.api.BaseAsyncCmd;
import org.apache.cloudstack.api.BaseAsyncCreateCmd;
import org.apache.cloudstack.api.BaseAsyncCustomIdCmd;
import org.apache.cloudstack.api.BaseCmd;
import org.apache.cloudstack.api.BaseCustomIdCmd;
import org.apache.cloudstack.context.CallContext;
import org.apache.cloudstack.framework.jobs.AsyncJob;
import org.apache.cloudstack.framework.jobs.AsyncJobManager;

import com.cloud.api.dispatch.DispatchChain;
import com.cloud.api.dispatch.DispatchChainFactory;
import com.cloud.api.dispatch.DispatchTask;
import com.cloud.event.EventTypes;
import com.cloud.user.Account;
import com.cloud.user.AccountManager;
import com.cloud.utils.ReflectUtil;
import com.cloud.vm.VirtualMachine;

public class ApiDispatcher {
    private static final Logger s_logger = Logger.getLogger(ApiDispatcher.class.getName());

    Long _createSnapshotQueueSizeLimit;

    @Inject
    AsyncJobManager _asyncMgr;

    @Inject
    AccountManager _accountMgr;

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

    private void doAccessChecks(BaseCmd cmd, Map<Object, AccessType> entitiesToAccess) {
        Account caller = CallContext.current().getCallingAccount();

        APICommand commandAnnotation = cmd.getClass().getAnnotation(APICommand.class);
        String apiName = commandAnnotation != null ? commandAnnotation.name() : null;

        if (!entitiesToAccess.isEmpty()) {
            for (Object entity : entitiesToAccess.keySet()) {
                if (entity instanceof ControlledEntity) {
                    _accountMgr.checkAccess(caller, entitiesToAccess.get(entity), false, apiName, (ControlledEntity) entity);
                } else if (entity instanceof InfrastructureEntity) {
                    //FIXME: Move this code in adapter, remove code from Account manager
                }
            }
        }
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
            String uuid = params.get(ApiConstants.UUID);
            ctx.setStartEventId(Long.valueOf(startEventId));

            // Fow now use the key from EventTypes.java rather than getInstanceType bcz the later doesn't refer to the interfaces
            // Add the resource id in the call context, also add some other first class object ids (for now vm) if available.
            // TODO - this should be done for all the uuids passed in the cmd - so should be moved where uuid to id conversion happens.
            if(EventTypes.getEntityForEvent(asyncCmd.getEventType()) != null){
                ctx.putContextParameter(EventTypes.getEntityForEvent(asyncCmd.getEventType()), uuid);
            }
            if(params.get(ApiConstants.VIRTUAL_MACHINE_ID) != null){
                ctx.putContextParameter(ReflectUtil.getEntityName(VirtualMachine.class), params.get(ApiConstants.VIRTUAL_MACHINE_ID));
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
