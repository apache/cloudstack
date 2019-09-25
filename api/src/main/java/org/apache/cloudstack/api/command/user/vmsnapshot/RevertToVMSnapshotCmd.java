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

import java.util.logging.Logger;

import org.apache.cloudstack.acl.SecurityChecker.AccessType;
import org.apache.cloudstack.api.ACL;
import org.apache.cloudstack.api.APICommand;
import org.apache.cloudstack.api.ApiConstants;
import org.apache.cloudstack.api.ApiErrorCode;
import org.apache.cloudstack.api.BaseAsyncCmd;
import org.apache.cloudstack.api.Parameter;
import org.apache.cloudstack.api.ResponseObject.ResponseView;
import org.apache.cloudstack.api.ServerApiException;
import org.apache.cloudstack.api.command.user.UserCmd;
import org.apache.cloudstack.api.response.UserVmResponse;
import org.apache.cloudstack.api.response.VMSnapshotResponse;
import org.apache.cloudstack.context.CallContext;

import com.cloud.event.EventTypes;
import com.cloud.exception.ConcurrentOperationException;
import com.cloud.exception.InsufficientCapacityException;
import com.cloud.exception.ResourceAllocationException;
import com.cloud.exception.ResourceUnavailableException;
import com.cloud.user.Account;
import com.cloud.uservm.UserVm;
import com.cloud.vm.snapshot.VMSnapshot;

@APICommand(name = "revertToVMSnapshot", description = "Revert VM from a vmsnapshot.", responseObject = UserVmResponse.class, since = "4.2.0", responseView = ResponseView.Restricted,
        requestHasSensitiveInfo = false, responseHasSensitiveInfo = true)
public class RevertToVMSnapshotCmd extends BaseAsyncCmd implements UserCmd {
    public static final Logger s_logger = Logger.getLogger(RevertToVMSnapshotCmd.class.getName());
    private static final String s_name = "reverttovmsnapshotresponse";

    @ACL(accessType = AccessType.OperateEntry, pointerToEntity = "getVmId()")
    @Parameter(name = ApiConstants.VM_SNAPSHOT_ID,
               type = CommandType.UUID,
               required = true,
               entityType = VMSnapshotResponse.class,
               description = "The ID of the vm snapshot")
    private Long vmSnapShotId;

    public Long getVmSnapShotId() {
        return vmSnapShotId;
    }

    @Override
    public String getCommandName() {
        return s_name;
    }

    @Override
    public long getEntityOwnerId() {
        VMSnapshot vmSnapshot = _entityMgr.findById(VMSnapshot.class, getVmSnapShotId());
        if (vmSnapshot != null) {
            return vmSnapshot.getAccountId();
        }
        return Account.ACCOUNT_ID_SYSTEM;
    }

    @Override
    public void execute() throws  ResourceUnavailableException, InsufficientCapacityException, ResourceAllocationException, ConcurrentOperationException {
        CallContext.current().setEventDetails("vmsnapshot id: " + this._uuidMgr.getUuid(VMSnapshot.class, getVmSnapShotId()));
        UserVm result = _vmSnapshotService.revertToSnapshot(getVmSnapShotId());
        if (result != null) {
            UserVmResponse response = _responseGenerator.createUserVmResponse(getResponseView(),
                    "virtualmachine", result).get(0);
            response.setResponseName(getCommandName());
            setResponseObject(response);
        } else {
            throw new ServerApiException(ApiErrorCode.INTERNAL_ERROR, "Failed to revert VM snapshot");
        }
    }

    @Override
    public String getEventDescription() {
        return "Revert from VM snapshot: " + this._uuidMgr.getUuid(VMSnapshot.class, getVmSnapShotId());
    }

    @Override
    public String getEventType() {
        return EventTypes.EVENT_VM_SNAPSHOT_REVERT;
    }

}
