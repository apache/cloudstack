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
package org.apache.cloudstack.api.command.user.snapshot;

import org.apache.cloudstack.api.APICommand;
import org.apache.cloudstack.api.ApiCommandResourceType;
import org.apache.cloudstack.api.ApiConstants;
import org.apache.cloudstack.api.ApiErrorCode;
import org.apache.cloudstack.api.BaseAsyncCmd;
import org.apache.cloudstack.api.BaseAsyncCreateCmd;
import org.apache.cloudstack.api.Parameter;
import org.apache.cloudstack.api.ServerApiException;
import org.apache.cloudstack.api.response.SnapshotResponse;
import org.apache.cloudstack.api.response.VMSnapshotResponse;
import org.apache.cloudstack.api.response.VolumeResponse;
import org.apache.cloudstack.context.CallContext;

import com.cloud.event.EventTypes;
import com.cloud.exception.InvalidParameterValueException;
import com.cloud.exception.PermissionDeniedException;
import com.cloud.exception.ResourceAllocationException;
import com.cloud.projects.Project;
import com.cloud.storage.Snapshot;
import com.cloud.user.Account;
import com.cloud.uservm.UserVm;
import com.cloud.vm.snapshot.VMSnapshot;

@APICommand(name = "createSnapshotFromVMSnapshot", description = "Creates an instant snapshot of a volume from existing vm snapshot.", responseObject = SnapshotResponse.class, entityType = {Snapshot.class}, since = "4.10.0",
        requestHasSensitiveInfo = false, responseHasSensitiveInfo = false)
public class CreateSnapshotFromVMSnapshotCmd extends BaseAsyncCreateCmd {

    // ///////////////////////////////////////////////////
    // ////////////// API parameters /////////////////////
    // ///////////////////////////////////////////////////

    @Parameter(name = ApiConstants.VOLUME_ID, type = CommandType.UUID, entityType = VolumeResponse.class, required = true, description = "The ID of the disk volume")
    private Long volumeId;

    @Parameter(name=ApiConstants.VM_SNAPSHOT_ID, type=CommandType.UUID, entityType=VMSnapshotResponse.class,
            required=true, description="The ID of the VM snapshot")
    private Long vmSnapshotId;

    @Parameter(name = ApiConstants.NAME, type = CommandType.STRING, description = "the name of the snapshot")
    private String snapshotName;

    private String syncObjectType = BaseAsyncCmd.snapshotHostSyncObject;

    // ///////////////////////////////////////////////////
    // ///////////////// Accessors ///////////////////////
    // ///////////////////////////////////////////////////

    public Long getVolumeId() {
        return volumeId;
    }

    public Long getVMSnapshotId() {
        return vmSnapshotId;
    }

    public String getSnapshotName() {
        return snapshotName;
    }

    private Long getVmId() {
        VMSnapshot vmsnapshot = _entityMgr.findById(VMSnapshot.class, getVMSnapshotId());
        if (vmsnapshot == null) {
            throw new InvalidParameterValueException("Unable to find vm snapshot by id=" + getVMSnapshotId());
        }
        UserVm vm = _entityMgr.findById(UserVm.class, vmsnapshot.getVmId());
        if (vm == null) {
            throw new InvalidParameterValueException("Unable to find vm by vm snapshot id=" + getVMSnapshotId());
        }
        return vm.getId();
    }
    private Long getHostId() {
        VMSnapshot vmsnapshot = _entityMgr.findById(VMSnapshot.class, getVMSnapshotId());
        if (vmsnapshot == null) {
            throw new InvalidParameterValueException("Unable to find vm snapshot by id=" + getVMSnapshotId());
        }
        UserVm vm = _entityMgr.findById(UserVm.class, vmsnapshot.getVmId());
        if (vm != null) {
            if(vm.getHostId() != null) {
                return vm.getHostId();
            } else if(vm.getLastHostId() != null) {
                return vm.getLastHostId();
            }
        }
        return null;
    }


    // ///////////////////////////////////////////////////
    // ///////////// API Implementation///////////////////
    // ///////////////////////////////////////////////////

    public static String getResultObjectName() {
        return ApiConstants.SNAPSHOT;
    }

    @Override
    public long getEntityOwnerId() {

        VMSnapshot vmsnapshot = _entityMgr.findById(VMSnapshot.class, getVMSnapshotId());
        if (vmsnapshot == null) {
            throw new InvalidParameterValueException("Unable to find vmsnapshot by id=" + getVMSnapshotId());
        }

        Account account = _accountService.getAccount(vmsnapshot.getAccountId());
        //Can create templates for enabled projects/accounts only
        if (account.getType() == Account.Type.PROJECT) {
            Project project = _projectService.findByProjectAccountId(vmsnapshot.getAccountId());
            if (project == null) {
                throw new InvalidParameterValueException("Unable to find project by account id=" + account.getUuid());
            }
            if (project.getState() != Project.State.Active) {
                throw new PermissionDeniedException("Can't add resources to the project id=" + project.getUuid() + " in state=" + project.getState() + " as it's no longer active");
            }
        } else if (account.getState() == Account.State.DISABLED) {
            throw new PermissionDeniedException("The owner of template is disabled: " + account);
        }

        return vmsnapshot.getAccountId();
    }

    @Override
    public String getEventType() {
        return EventTypes.EVENT_SNAPSHOT_CREATE;
    }

    @Override
    public String getEventDescription() {
        return "creating snapshot from vm snapshot : " + this._uuidMgr.getUuid(VMSnapshot.class, getVMSnapshotId());
    }

    @Override
    public ApiCommandResourceType getApiResourceType() {
        return ApiCommandResourceType.Snapshot;
    }

    @Override
    public void create() throws ResourceAllocationException {
        Snapshot snapshot = this._volumeService.allocSnapshotForVm(getVmId(), getVolumeId(), getSnapshotName());
        if (snapshot != null) {
            this.setEntityId(snapshot.getId());
            this.setEntityUuid(snapshot.getUuid());
        } else {
            throw new ServerApiException(ApiErrorCode.INTERNAL_ERROR, "Failed to create snapshot from vm snapshot");
        }
    }

    @Override
    public void execute() {
        logger.info("CreateSnapshotFromVMSnapshotCmd with vm snapshot id:" + getVMSnapshotId() + " and snapshot id:" + getEntityId() + " starts:" + System.currentTimeMillis());
        CallContext.current().setEventDetails("Vm Snapshot Id: "+ this._uuidMgr.getUuid(VMSnapshot.class, getVMSnapshotId()));
        Snapshot snapshot = null;
        try {
            snapshot = _snapshotService.backupSnapshotFromVmSnapshot(getEntityId(), getVmId(), getVolumeId(), getVMSnapshotId());
            if (snapshot != null) {
                SnapshotResponse response = _responseGenerator.createSnapshotResponse(snapshot);
                response.setResponseName(getCommandName());
                this.setResponseObject(response);
            } else {
                throw new ServerApiException(ApiErrorCode.INTERNAL_ERROR, "Failed to create snapshot due to an internal error creating snapshot from vm snapshot " + getVMSnapshotId());
            }
        } catch (InvalidParameterValueException ex) {
            throw ex;
        } catch (Exception e) {
            logger.debug("Failed to create snapshot", e);
            throw new ServerApiException(ApiErrorCode.INTERNAL_ERROR, "Failed to create snapshot due to an internal error creating snapshot from vm snapshot " + getVMSnapshotId());
        } finally {
            if (snapshot == null) {
                try {
                    _snapshotService.deleteSnapshot(getEntityId(), null);
                } catch (Exception e) {
                    logger.debug("Failed to clean failed snapshot" + getEntityId());
                }
            }
        }
    }


    @Override
    public String getSyncObjType() {
        if (getSyncObjId() != null) {
            return syncObjectType;
        }
        return null;
    }

    @Override
    public Long getSyncObjId() {
        if (getHostId() != null) {
            return getHostId();
        }
        return null;
    }
}
