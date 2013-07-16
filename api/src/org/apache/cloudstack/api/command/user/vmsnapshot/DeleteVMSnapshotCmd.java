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

package org.apache.cloudstack.api.command.user.vmsnapshot;

import org.apache.cloudstack.api.APICommand;
import org.apache.cloudstack.api.ApiConstants;
import org.apache.cloudstack.api.ApiErrorCode;
import org.apache.cloudstack.api.BaseAsyncCmd;
import org.apache.cloudstack.api.Parameter;
import org.apache.cloudstack.api.ServerApiException;
import org.apache.cloudstack.api.response.SuccessResponse;
import org.apache.cloudstack.api.response.VMSnapshotResponse;
import org.apache.cloudstack.context.CallContext;

import org.apache.log4j.Logger;

import com.cloud.event.EventTypes;
import com.cloud.user.Account;
import com.cloud.vm.snapshot.VMSnapshot;

@APICommand(name="deleteVMSnapshot", description = "Deletes a vmsnapshot.", responseObject = SuccessResponse.class, since="4.2.0")
public class DeleteVMSnapshotCmd extends BaseAsyncCmd {
    public static final Logger s_logger = Logger
            .getLogger(DeleteVMSnapshotCmd.class.getName());
    private static final String s_name = "deletevmsnapshotresponse";

    @Parameter(name=ApiConstants.VM_SNAPSHOT_ID, type=CommandType.UUID, entityType=VMSnapshotResponse.class,
        required=true, description="The ID of the VM snapshot")
    private Long id;

    public Long getId() {
        return id;
    }

    @Override
    public String getCommandName() {
        return s_name;
    }

    @Override
    public long getEntityOwnerId() {
        VMSnapshot vmSnapshot = _entityMgr.findById(VMSnapshot.class, getId());
        if (vmSnapshot != null) {
            return vmSnapshot.getAccountId();
        }
        return Account.ACCOUNT_ID_SYSTEM;
    }

    @Override
    public void execute() {
        CallContext.current().setEventDetails("vmsnapshot id: " + getId());
        boolean result = _vmSnapshotService.deleteVMSnapshot(getId());
        if (result) {
            SuccessResponse response = new SuccessResponse(getCommandName());
            this.setResponseObject(response);
        } else {
            throw new ServerApiException(ApiErrorCode.INTERNAL_ERROR, "Failed to delete vm snapshot");
        }
    }

    @Override
    public String getEventDescription() {
        return "Delete VM snapshot: " + getId();
    }

    @Override
    public String getEventType() {
        return EventTypes.EVENT_VM_SNAPSHOT_DELETE;
    }

}
