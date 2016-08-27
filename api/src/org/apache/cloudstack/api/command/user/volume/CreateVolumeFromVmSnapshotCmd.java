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

import org.apache.cloudstack.api.APICommand;
import org.apache.cloudstack.api.ApiConstants;
import org.apache.cloudstack.api.ApiErrorCode;
import org.apache.cloudstack.api.Parameter;
import org.apache.cloudstack.api.ResponseObject.ResponseView;
import org.apache.cloudstack.api.ServerApiException;
import org.apache.cloudstack.api.response.VMSnapshotResponse;
import org.apache.cloudstack.api.response.VolumeResponse;
import org.apache.cloudstack.context.CallContext;

import com.cloud.event.EventTypes;
import com.cloud.storage.Volume;
import com.cloud.vm.VirtualMachine;
import com.cloud.vm.snapshot.VMSnapshot;

@APICommand(name = "createVolumeFromVmSnapshot", responseObject = VolumeResponse.class, description = "Creates a disk volume from a disk volume in a VM snapshot. This disk volume must still be attached to a virtual machine to make use of it.", responseView = ResponseView.Restricted, entityType = {
        Volume.class, VirtualMachine.class},
        requestHasSensitiveInfo = false, responseHasSensitiveInfo = false)
public class CreateVolumeFromVmSnapshotCmd extends CreateVolumeCmd {
    public static final Logger s_logger = Logger.getLogger(CreateVolumeFromVmSnapshotCmd.class);

    /////////////////////////////////////////////////////
    //////////////// API parameters /////////////////////
    /////////////////////////////////////////////////////

    @Parameter(name = ApiConstants.VM_SNAPSHOT_ID,
            type = CommandType.UUID,
            entityType = VMSnapshotResponse.class,
            required = true,
            description = "VM Snapshot ID to be used to create data volume. This parameter is mutually exclusive w.r.t. parameters diskOfferingId and snapshotId")
    private Long vmSnapshotId;

    @Parameter(name = ApiConstants.VOLUME_ID,
            type = CommandType.UUID,
            entityType = VolumeResponse.class,
            required = true,
            description = "UUID of volume that is attached to VM related to specified vm snapshot. New volume would be created with the content of this volume in specified vm snapshot.")
    private Long volumeId;

    public Long getVmSnapshotId() {
        return vmSnapshotId;
    }

    public void setVmSnapshotId(Long vmSnapshotId) {
        this.vmSnapshotId = vmSnapshotId;
    }

    public Long getVolumeId() {
        return volumeId;
    }

    public void setVolumeId(Long volumeId) {
        this.volumeId = volumeId;
    }

    @Override
    public String getEventType() {
        return EventTypes.EVENT_VOLUME_CREATE;
    }

    @Override
    public String getEventDescription() {
        return "creating volume: " + getVolumeName() + ((getVmSnapshotId() == null) ? "" : " from vm snapshot: " + getVmSnapshotId());
    }

    @Override
    public void execute() {
        CallContext.current().setEventDetails("Creating volume with Id: " + getEntityId() + ((getVmSnapshotId() == null) ? "" : " from vm snapshot: " + getVmSnapshotId()));
        Volume volume = _volumeService.createVolumeFromVmSnapshot(this);
        if (volume != null) {
            VolumeResponse response = _responseGenerator.createVolumeResponse(ResponseView.Restricted, volume);
            if (getVmSnapshotId() != null) {
                VMSnapshot snap = _entityMgr.findById(VMSnapshot.class, getVmSnapshotId());
                if (snap != null) {
                    //response.setSnapshotId(snap.getUuid()); // if the volume was
                }
            }
            response.setResponseName(getCommandName());
            setResponseObject(response);
        } else {
            throw new ServerApiException(ApiErrorCode.INTERNAL_ERROR, "Failed to create a volume");
        }
    }
}
