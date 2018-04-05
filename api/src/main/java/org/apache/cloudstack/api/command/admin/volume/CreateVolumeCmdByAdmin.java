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
package org.apache.cloudstack.api.command.admin.volume;

import org.apache.log4j.Logger;

import org.apache.cloudstack.api.APICommand;
import org.apache.cloudstack.api.ApiErrorCode;
import org.apache.cloudstack.api.ResponseObject.ResponseView;
import org.apache.cloudstack.api.ServerApiException;
import org.apache.cloudstack.api.command.user.volume.CreateVolumeCmd;
import org.apache.cloudstack.api.response.VolumeResponse;
import org.apache.cloudstack.context.CallContext;

import com.cloud.storage.Snapshot;
import com.cloud.storage.Volume;
import com.cloud.vm.VirtualMachine;

@APICommand(name = "createVolume", responseObject = VolumeResponse.class, description = "Creates a disk volume from a disk offering. This disk volume must still be attached to a virtual machine to make use of it.", responseView = ResponseView.Full, entityType = {
        Volume.class, VirtualMachine.class},
        requestHasSensitiveInfo = false, responseHasSensitiveInfo = false)
public class CreateVolumeCmdByAdmin extends CreateVolumeCmd {
    public static final Logger s_logger = Logger.getLogger(CreateVolumeCmdByAdmin.class.getName());

    @Override
    public void execute(){
        CallContext.current().setEventDetails("Volume Id: "+getEntityId()+((getSnapshotId() == null) ? "" : " from snapshot: " + getSnapshotId()));
        Volume volume = _volumeService.createVolume(this);
        if (volume != null) {
            VolumeResponse response = _responseGenerator.createVolumeResponse(ResponseView.Full, volume);
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
