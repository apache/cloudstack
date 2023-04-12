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
package org.apache.cloudstack.api.command.user.autoscale;

import org.apache.log4j.Logger;

import org.apache.cloudstack.acl.SecurityChecker.AccessType;
import org.apache.cloudstack.api.ACL;
import org.apache.cloudstack.api.APICommand;
import org.apache.cloudstack.api.ApiCommandResourceType;
import org.apache.cloudstack.api.ApiConstants;
import org.apache.cloudstack.api.ApiErrorCode;
import org.apache.cloudstack.api.BaseAsyncCmd;
import org.apache.cloudstack.api.Parameter;
import org.apache.cloudstack.api.ServerApiException;
import org.apache.cloudstack.api.response.AutoScaleVmGroupResponse;
import org.apache.cloudstack.api.response.SuccessResponse;
import org.apache.cloudstack.context.CallContext;

import com.cloud.event.EventTypes;
import com.cloud.network.as.AutoScaleVmGroup;
import com.cloud.user.Account;

@APICommand(name = "deleteAutoScaleVmGroup", description = "Deletes a autoscale vm group.", responseObject = SuccessResponse.class, entityType = {AutoScaleVmGroup.class},
        requestHasSensitiveInfo = false, responseHasSensitiveInfo = false)
public class DeleteAutoScaleVmGroupCmd extends BaseAsyncCmd {
    public static final Logger s_logger = Logger.getLogger(DeleteAutoScaleVmGroupCmd.class.getName());
    // ///////////////////////////////////////////////////
    // ////////////// API parameters /////////////////////
    // ///////////////////////////////////////////////////

    @ACL(accessType = AccessType.OperateEntry)
    @Parameter(name = ApiConstants.ID,
               type = CommandType.UUID,
               entityType = AutoScaleVmGroupResponse.class,
               required = true,
               description = "the ID of the autoscale group")
    private Long id;

    @Parameter(name = ApiConstants.CLEANUP,
            type = CommandType.BOOLEAN,
            description = "true if all VMs have to be cleaned up, false otherwise",
            since = "4.18.0")
    private Boolean cleanup;

    // ///////////////////////////////////////////////////
    // ///////////////// Accessors ///////////////////////
    // ///////////////////////////////////////////////////

    public Long getId() {
        return id;
    }

    public Boolean getCleanup() {
        return cleanup != null && cleanup;
    }

    // ///////////////////////////////////////////////////
    // ///////////// API Implementation///////////////////
    // ///////////////////////////////////////////////////

    @Override
    public long getEntityOwnerId() {
        AutoScaleVmGroup autoScaleVmGroup = _entityMgr.findById(AutoScaleVmGroup.class, getId());
        if (autoScaleVmGroup != null) {
            return autoScaleVmGroup.getAccountId();
        }

        return Account.ACCOUNT_ID_SYSTEM; // no account info given, parent this command to SYSTEM so ERROR events are
        // tracked
    }

    @Override
    public String getEventType() {
        return EventTypes.EVENT_AUTOSCALEVMGROUP_DELETE;
    }

    @Override
    public String getEventDescription() {
        return "deleting autoscale vm group: " + getId();
    }

    @Override
    public void execute() {
        CallContext.current().setEventDetails("AutoScale Vm Group Id: " + getId());
        boolean result = _autoScaleService.deleteAutoScaleVmGroup(id, getCleanup());

        if (result) {
            SuccessResponse response = new SuccessResponse(getCommandName());
            setResponseObject(response);
        } else {
            s_logger.warn("Failed to delete autoscale vm group " + getId());
            throw new ServerApiException(ApiErrorCode.INTERNAL_ERROR, "Failed to delete autoscale vm group");
        }
    }

    @Override
    public ApiCommandResourceType getApiResourceType() {
        return ApiCommandResourceType.AutoScaleVmGroup;
    }
}
