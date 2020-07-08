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
package org.apache.cloudstack.api.command.user.volume;

import org.apache.log4j.Logger;

import org.apache.cloudstack.acl.RoleType;
import org.apache.cloudstack.api.APICommand;
import org.apache.cloudstack.api.ApiCommandJobType;
import org.apache.cloudstack.api.ApiConstants;
import org.apache.cloudstack.api.ApiErrorCode;
import org.apache.cloudstack.api.BaseAsyncCreateCustomIdCmd;
import org.apache.cloudstack.api.BaseCmd;
import org.apache.cloudstack.api.Parameter;
import org.apache.cloudstack.api.ResponseObject.ResponseView;
import org.apache.cloudstack.api.ServerApiException;
import org.apache.cloudstack.api.command.user.UserCmd;
import org.apache.cloudstack.api.response.DiskOfferingResponse;
import org.apache.cloudstack.api.response.DomainResponse;
import org.apache.cloudstack.api.response.ProjectResponse;
import org.apache.cloudstack.api.response.SnapshotResponse;
import org.apache.cloudstack.api.response.UserVmResponse;
import org.apache.cloudstack.api.response.VolumeResponse;
import org.apache.cloudstack.api.response.ZoneResponse;
import org.apache.cloudstack.context.CallContext;

import com.cloud.event.EventTypes;
import com.cloud.exception.ResourceAllocationException;
import com.cloud.storage.Snapshot;
import com.cloud.storage.Volume;
import com.cloud.vm.VirtualMachine;

@APICommand(name = "createVolume", responseObject = VolumeResponse.class, description = "Creates a disk volume from a disk offering. This disk volume must still be attached to a virtual machine to make use of it.", responseView = ResponseView.Restricted, entityType = {
        Volume.class, VirtualMachine.class},
            requestHasSensitiveInfo = false, responseHasSensitiveInfo = false)
public class CreateVolumeCmd extends BaseAsyncCreateCustomIdCmd implements UserCmd {
    public static final Logger s_logger = Logger.getLogger(CreateVolumeCmd.class.getName());
    private static final String s_name = "createvolumeresponse";

    /////////////////////////////////////////////////////
    //////////////// API parameters /////////////////////
    /////////////////////////////////////////////////////

    @Parameter(name = ApiConstants.ACCOUNT,
               type = BaseCmd.CommandType.STRING,
               description = "the account associated with the disk volume. Must be used with the domainId parameter.")
    private String accountName;

    @Parameter(name = ApiConstants.PROJECT_ID,
               type = CommandType.UUID,
               entityType = ProjectResponse.class,
               description = "the project associated with the volume. Mutually exclusive with account parameter")
    private Long projectId;

    @Parameter(name = ApiConstants.DOMAIN_ID,
               type = CommandType.UUID,
               entityType = DomainResponse.class,
               description = "the domain ID associated with the disk offering. If used with the account parameter"
                   + " returns the disk volume associated with the account for the specified domain.")
    private Long domainId;

    @Parameter(name = ApiConstants.DISK_OFFERING_ID,
               required = false,
               type = CommandType.UUID,
               entityType = DiskOfferingResponse.class,
               description = "the ID of the disk offering. Either diskOfferingId or snapshotId must be passed in.")
    private Long diskOfferingId;

    @Parameter(name = ApiConstants.NAME, type = CommandType.STRING, description = "the name of the disk volume")
    private String volumeName;

    @Parameter(name = ApiConstants.SIZE, type = CommandType.LONG, description = "Arbitrary volume size")
    private Long size;

    @Parameter(name = ApiConstants.MIN_IOPS, type = CommandType.LONG, description = "min iops")
    private Long minIops;

    @Parameter(name = ApiConstants.MAX_IOPS, type = CommandType.LONG, description = "max iops")
    private Long maxIops;

    @Parameter(name = ApiConstants.SNAPSHOT_ID,
               type = CommandType.UUID,
               entityType = SnapshotResponse.class,
               description = "the snapshot ID for the disk volume. Either diskOfferingId or snapshotId must be passed in.")
    private Long snapshotId;

    @Parameter(name = ApiConstants.ZONE_ID, type = CommandType.UUID, entityType = ZoneResponse.class, description = "the ID of the availability zone")
    private Long zoneId;

    @Parameter(name = ApiConstants.DISPLAY_VOLUME, type = CommandType.BOOLEAN, description = "an optional field, whether to display the volume to the end user or not.", authorized = {RoleType.Admin})
    private Boolean displayVolume;

    @Parameter(name = ApiConstants.VIRTUAL_MACHINE_ID,
               type = CommandType.UUID,
               entityType = UserVmResponse.class,
               description = "the ID of the virtual machine; to be used with snapshot Id, VM to which the volume gets attached after creation")
    private Long virtualMachineId;

    /////////////////////////////////////////////////////
    /////////////////// Accessors ///////////////////////
    /////////////////////////////////////////////////////

    public String getAccountName() {
        return accountName;
    }

    public Long getDiskOfferingId() {
        return diskOfferingId;
    }

    public Long getDomainId() {
        return domainId;
    }

    public String getVolumeName() {
        return volumeName;
    }

    public Long getSize() {
        return size;
    }

    public Long getMinIops() {
        return minIops;
    }

    public Long getMaxIops() {
        return maxIops;
    }

    public Long getSnapshotId() {
        return snapshotId;
    }

    public Long getZoneId() {
        return zoneId;
    }

    private Long getProjectId() {
        return projectId;
    }

    public Boolean getDisplayVolume() {
        return displayVolume;
    }

    @Override
    public boolean isDisplay() {
        if(displayVolume == null)
            return true;
        else
            return displayVolume;
    }

    public Long getVirtualMachineId() {
        return virtualMachineId;
    }

    /////////////////////////////////////////////////////
    /////////////// API Implementation///////////////////
    /////////////////////////////////////////////////////
    @Override
    public String getCommandName() {
        return s_name;
    }

    public static String getResultObjectName() {
        return "volume";
    }

    @Override
    public ApiCommandJobType getInstanceType() {
        return ApiCommandJobType.Volume;
    }

    @Override
    public long getEntityOwnerId() {
        Long accountId = _accountService.finalyzeAccountId(accountName, domainId, projectId, true);
        if (accountId == null) {
            return CallContext.current().getCallingAccount().getId();
        }

        return accountId;
    }

    @Override
    public String getEventType() {
        return EventTypes.EVENT_VOLUME_CREATE;
    }

    @Override
    public String getEventDescription() {
        return  "creating volume: " + getVolumeName() + ((getSnapshotId() == null) ? "" : " from snapshot: " + this._uuidMgr.getUuid(Snapshot.class, getSnapshotId()));
    }

    @Override
    public void create() throws ResourceAllocationException {

        Volume volume = _volumeService.allocVolume(this);
        if (volume != null) {
            setEntityId(volume.getId());
            setEntityUuid(volume.getUuid());
        } else {
            throw new ServerApiException(ApiErrorCode.INTERNAL_ERROR, "Failed to create volume");
        }
    }

    @Override
    public void execute() {
        CallContext.current().setEventDetails("Volume Id: " + getEntityUuid() + ((getSnapshotId() == null) ? "" : " from snapshot: " + this._uuidMgr.getUuid(Snapshot.class, getSnapshotId())));
        Volume volume = _volumeService.createVolume(this);
        if (volume != null) {
            VolumeResponse response = _responseGenerator.createVolumeResponse(getResponseView(), volume);
            //FIXME - have to be moved to ApiResponseHelper
            if (getSnapshotId() != null) {
                Snapshot snap = _entityMgr.findById(Snapshot.class, getSnapshotId());
                if (snap != null) {
                    response.setSnapshotId(snap.getUuid()); // if the volume was
                    // created from a
                    // snapshot,
                    // snapshotId will
                    // be set so we pass
                    // it back in the
                    // response
                }
            }
            response.setResponseName(getCommandName());
            setResponseObject(response);
        } else {
            throw new ServerApiException(ApiErrorCode.INTERNAL_ERROR, "Failed to create a volume");
        }
    }
}
