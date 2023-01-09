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

import com.cloud.event.EventTypes;
import com.cloud.exception.ConcurrentOperationException;
import com.cloud.exception.NetworkRuleConflictException;
import com.cloud.exception.InsufficientCapacityException;
import com.cloud.exception.ResourceAllocationException;
import com.cloud.exception.ResourceUnavailableException;
import com.cloud.offering.DiskOffering;
import com.cloud.storage.Volume;
import org.apache.cloudstack.acl.RoleType;
import org.apache.cloudstack.api.APICommand;
import org.apache.cloudstack.api.ApiConstants;
import org.apache.cloudstack.api.ApiErrorCode;
import org.apache.cloudstack.api.BaseAsyncCmd;
import org.apache.cloudstack.api.Parameter;
import org.apache.cloudstack.api.ResponseObject;
import org.apache.cloudstack.api.ServerApiException;
import org.apache.cloudstack.api.command.user.UserCmd;
import org.apache.cloudstack.api.response.DiskOfferingResponse;
import org.apache.cloudstack.api.response.VolumeResponse;
import org.apache.cloudstack.context.CallContext;

@APICommand(name = "changeOfferingForVolume",
        description = "Change disk offering of the volume and also an option to auto migrate if required to apply the new disk offering",
        responseObject = VolumeResponse.class,
        requestHasSensitiveInfo = false,
        responseHasSensitiveInfo = false,
        authorized = { RoleType.Admin, RoleType.ResourceAdmin, RoleType.DomainAdmin, RoleType.User},
        since = "4.17")
public class ChangeOfferingForVolumeCmd extends BaseAsyncCmd implements UserCmd {

    /////////////////////////////////////////////////////
    //////////////// API parameters /////////////////////
    /////////////////////////////////////////////////////

    @Parameter(name = ApiConstants.ID, entityType = VolumeResponse.class, required = true, type = CommandType.UUID, description = "the ID of the volume")
    private Long id;

    @Parameter(name = ApiConstants.DISK_OFFERING_ID,
            entityType = DiskOfferingResponse.class,
            type = CommandType.UUID,
            required = true,
            description = "new disk offering id")
    private Long newDiskOfferingId;

    @Parameter(name = ApiConstants.SIZE, type = CommandType.LONG, required = false, description = "New volume size in GB for the custom disk offering")
    private Long size;

    @Parameter(name = ApiConstants.MIN_IOPS, type = CommandType.LONG, required = false, description = "New minimum number of IOPS for the custom disk offering")
    private Long minIops;

    @Parameter(name = ApiConstants.MAX_IOPS, type = CommandType.LONG, required = false, description = "New maximum number of IOPS for the custom disk offering")
    private Long maxIops;

    @Parameter(name = ApiConstants.AUTO_MIGRATE, type = CommandType.BOOLEAN, required = false, description = "Flag for automatic migration of the volume " +
            "with new disk offering whenever migration is required to apply the offering")
    private Boolean autoMigrate;

    @Parameter(name = ApiConstants.SHRINK_OK, type = CommandType.BOOLEAN, required = false, description = "Verify OK to Shrink")
    private Boolean shrinkOk;

    /////////////////////////////////////////////////////
    /////////////////// Accessors ///////////////////////
    /////////////////////////////////////////////////////

    public ChangeOfferingForVolumeCmd() {}

    public ChangeOfferingForVolumeCmd(Long volumeId, long newDiskOfferingId, Long minIops, Long maxIops, boolean autoMigrate, boolean shrinkOk) {
        this.id = volumeId;
        this.minIops = minIops;
        this.maxIops = maxIops;
        this.newDiskOfferingId = newDiskOfferingId;
        this.autoMigrate = autoMigrate;
        this.shrinkOk = shrinkOk;
    }

    public Long getId() {
        return id;
    }

    public Long getNewDiskOfferingId() {
        return newDiskOfferingId;
    }

    public Long getSize() {
        return size;
    }

    public void setSize(Long size) {
        this.size = size;
    }

    public Long getMinIops() {
        return minIops;
    }

    public Long getMaxIops() {
        return maxIops;
    }

    public boolean getAutoMigrate() {
        return autoMigrate == null ? false : autoMigrate;
    }

    public boolean isShrinkOk() {
        return shrinkOk == null ? false: shrinkOk;
    }

    @Override
    public String getEventType() {
        return EventTypes.EVENT_VOLUME_CHANGE_DISK_OFFERING;
    }

    @Override
    public String getEventDescription() {
        return "Changing Disk offering of Volume Id: " + this._uuidMgr.getUuid(Volume.class, getId()) + " to " + this._uuidMgr.getUuid(DiskOffering.class, getNewDiskOfferingId());
    }

    @Override
    public void execute() throws ResourceUnavailableException, InsufficientCapacityException, ServerApiException, ConcurrentOperationException, ResourceAllocationException, NetworkRuleConflictException {
        CallContext.current().setEventDetails("Volume Id: " + getId());
        Volume result = _volumeService.changeDiskOfferingForVolume(this);
        if (result != null) {
            VolumeResponse response = _responseGenerator.createVolumeResponse(ResponseObject.ResponseView.Restricted, result);
            response.setResponseName(getCommandName());
            setResponseObject(response);
        } else {
            throw new ServerApiException(ApiErrorCode.INTERNAL_ERROR, "Failed to change disk offering of volume");
        }
    }

    @Override
    public long getEntityOwnerId() {
        return CallContext.current().getCallingAccountId();
    }
}
