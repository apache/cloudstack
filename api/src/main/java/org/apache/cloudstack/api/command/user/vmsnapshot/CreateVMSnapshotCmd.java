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

import com.cloud.utils.exception.CloudRuntimeException;
import org.apache.cloudstack.acl.SecurityChecker.AccessType;
import org.apache.cloudstack.api.ACL;
import org.apache.cloudstack.api.APICommand;
import org.apache.cloudstack.api.ApiCommandResourceType;
import org.apache.cloudstack.api.ApiConstants;
import org.apache.cloudstack.api.ApiErrorCode;
import org.apache.cloudstack.api.BaseAsyncCreateCmd;
import org.apache.cloudstack.api.Parameter;
import org.apache.cloudstack.api.ServerApiException;
import org.apache.cloudstack.api.response.UserVmResponse;
import org.apache.cloudstack.api.response.VMSnapshotResponse;
import org.apache.cloudstack.context.CallContext;

import com.cloud.event.EventTypes;
import com.cloud.exception.ResourceAllocationException;
import com.cloud.uservm.UserVm;
import com.cloud.vm.VirtualMachine;
import com.cloud.vm.snapshot.VMSnapshot;

@APICommand(name = "createVMSnapshot", description = "Creates snapshot for a vm.", responseObject = VMSnapshotResponse.class, since = "4.2.0", entityType = {VMSnapshot.class},
        requestHasSensitiveInfo = false, responseHasSensitiveInfo = false)
public class CreateVMSnapshotCmd extends BaseAsyncCreateCmd {


    @ACL(accessType = AccessType.OperateEntry)
    @Parameter(name = ApiConstants.VIRTUAL_MACHINE_ID, type = CommandType.UUID, required = true, entityType = UserVmResponse.class, description = "The ID of the vm")
    private Long vmId;

    @Parameter(name = ApiConstants.VM_SNAPSHOT_DESCRIPTION, type = CommandType.STRING, required = false, description = "The description of the snapshot")
    private String description;

    @Parameter(name = ApiConstants.VM_SNAPSHOT_DISPLAYNAME, type = CommandType.STRING, required = false, description = "The display name of the snapshot")
    private String displayName;

    @Parameter(name = ApiConstants.VM_SNAPSHOT_MEMORY, type = CommandType.BOOLEAN, required = false, description = "snapshot memory if true")
    private Boolean snapshotMemory;

    @Parameter(name = ApiConstants.VM_SNAPSHOT_QUIESCEVM, type = CommandType.BOOLEAN, required = false, description = "quiesce vm if true")
    private Boolean quiescevm;

    public Boolean snapshotMemory() {
        if (snapshotMemory == null) {
            return false;
        } else {
            return snapshotMemory;
        }
    }

    public Boolean getQuiescevm() {
        if (quiescevm == null) {
            return false;
        } else {
            return quiescevm;
        }
    }

    public String getDisplayName() {
        return displayName;
    }

    public String getDescription() {
        return description;
    }

    public Long getVmId() {
        return vmId;
    }

    @Override
    public void create() throws ResourceAllocationException {
        VMSnapshot vmsnapshot;
        try {
            vmsnapshot = _vmSnapshotService.allocVMSnapshot(getVmId(), getDisplayName(), getDescription(), snapshotMemory());
        } catch (CloudRuntimeException e) {
            throw new ServerApiException(ApiErrorCode.INTERNAL_ERROR, "Failed to create vm snapshot: " + e.getMessage(), e);
        }

        if (vmsnapshot != null) {
            setEntityId(vmsnapshot.getId());
            setEntityUuid(vmsnapshot.getUuid());
        } else {
            throw new ServerApiException(ApiErrorCode.INTERNAL_ERROR, "Failed to create vm snapshot");
        }
    }

    @Override
    public String getEventDescription() {
        return "creating snapshot for VM: " + this._uuidMgr.getUuid(VirtualMachine.class, getVmId());
    }

    @Override
    public String getEventType() {
        return EventTypes.EVENT_VM_SNAPSHOT_CREATE;
    }

    @Override
    public void execute() {
        CallContext.current().setEventDetails("VM Id: " + this._uuidMgr.getUuid(VirtualMachine.class, getVmId()));
        VMSnapshot result = _vmSnapshotService.createVMSnapshot(getVmId(), getEntityId(), getQuiescevm());
        if (result != null) {
            VMSnapshotResponse response = _responseGenerator.createVMSnapshotResponse(result);
            response.setResponseName(getCommandName());
            setResponseObject(response);
        } else {
            throw new ServerApiException(ApiErrorCode.INTERNAL_ERROR, "Failed to create vm snapshot due to an internal error creating snapshot for vm " + getVmId());
        }
    }

    @Override
    public long getEntityOwnerId() {
        UserVm userVM = _userVmService.getUserVm(vmId);
        return userVM.getAccountId();
    }

    @Override
    public ApiCommandResourceType getApiResourceType() {
        return ApiCommandResourceType.VmSnapshot;
    }

    @Override
    public Long getApiResourceId() {
        return getEntityId();
    }

}
