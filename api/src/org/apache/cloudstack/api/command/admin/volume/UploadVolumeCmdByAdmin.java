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
import org.apache.cloudstack.api.command.user.volume.UploadVolumeCmd;
import org.apache.cloudstack.api.response.VolumeResponse;

import com.cloud.exception.ConcurrentOperationException;
import com.cloud.exception.InsufficientCapacityException;
import com.cloud.exception.NetworkRuleConflictException;
import com.cloud.exception.ResourceAllocationException;
import com.cloud.exception.ResourceUnavailableException;
import com.cloud.storage.Volume;

@APICommand(name = "uploadVolume", description = "Uploads a data disk.", responseObject = VolumeResponse.class, responseView = ResponseView.Full, entityType = {Volume.class},
        requestHasSensitiveInfo = false, responseHasSensitiveInfo = false)
public class UploadVolumeCmdByAdmin extends UploadVolumeCmd {
    public static final Logger s_logger = Logger.getLogger(UploadVolumeCmdByAdmin.class.getName());


    @Override
    public void execute() throws ResourceUnavailableException,
            InsufficientCapacityException, ServerApiException,
            ConcurrentOperationException, ResourceAllocationException,
            NetworkRuleConflictException {

            Volume volume = _volumeService.uploadVolume(this);
            if (volume != null){
            VolumeResponse response = _responseGenerator.createVolumeResponse(ResponseView.Full, volume);
                response.setResponseName(getCommandName());
                setResponseObject(response);
            } else {
                throw new ServerApiException(ApiErrorCode.INTERNAL_ERROR, "Failed to upload volume");
            }
    }


}
