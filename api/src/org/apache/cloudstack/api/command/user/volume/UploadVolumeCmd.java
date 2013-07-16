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

import org.apache.cloudstack.api.APICommand;
import org.apache.cloudstack.api.ApiConstants;
import org.apache.cloudstack.api.ApiErrorCode;
import org.apache.cloudstack.api.BaseAsyncCmd;
import org.apache.cloudstack.api.Parameter;
import org.apache.cloudstack.api.ServerApiException;
import org.apache.cloudstack.api.BaseCmd.CommandType;
import org.apache.cloudstack.api.response.DomainResponse;
import org.apache.cloudstack.api.response.ProjectResponse;
import org.apache.cloudstack.api.response.VolumeResponse;
import org.apache.cloudstack.api.response.ZoneResponse;
import org.apache.cloudstack.context.CallContext;

import org.apache.log4j.Logger;

import com.cloud.event.EventTypes;
import com.cloud.exception.ConcurrentOperationException;
import com.cloud.exception.InsufficientCapacityException;
import com.cloud.exception.NetworkRuleConflictException;
import com.cloud.exception.ResourceAllocationException;
import com.cloud.exception.ResourceUnavailableException;
import com.cloud.storage.Volume;

@APICommand(name = "uploadVolume", description="Uploads a data disk.", responseObject=VolumeResponse.class)
public class UploadVolumeCmd extends BaseAsyncCmd {
    public static final Logger s_logger = Logger.getLogger(UploadVolumeCmd.class.getName());
    private static final String s_name = "uploadvolumeresponse";

    /////////////////////////////////////////////////////
    //////////////// API parameters /////////////////////
    /////////////////////////////////////////////////////

    @Parameter(name=ApiConstants.FORMAT, type=CommandType.STRING, required=true, description="the format for the volume. Possible values include QCOW2, OVA, and VHD.")
    private String format;

    @Parameter(name=ApiConstants.NAME, type=CommandType.STRING, required=true, description="the name of the volume")
    private String volumeName;

    @Parameter(name=ApiConstants.URL, type=CommandType.STRING, required=true, description="the URL of where the volume is hosted. Possible URL include http:// and https://")
    private String url;

    @Parameter(name=ApiConstants.ZONE_ID, type=CommandType.UUID, entityType = ZoneResponse.class,
            required=true, description="the ID of the zone the volume is to be hosted on")
    private Long zoneId;

    @Parameter(name=ApiConstants.DOMAIN_ID, type=CommandType.UUID, entityType = DomainResponse.class,
            description="an optional domainId. If the account parameter is used, domainId must also be used.")
    private Long domainId;

    @Parameter(name=ApiConstants.ACCOUNT, type=CommandType.STRING, description="an optional accountName. Must be used with domainId.")
    private String accountName;

    @Parameter(name=ApiConstants.CHECKSUM, type=CommandType.STRING, description="the MD5 checksum value of this volume")
    private String checksum;
    
    @Parameter(name=ApiConstants.IMAGE_STORE_UUID, type=CommandType.STRING,
            description="Image store uuid")
    private String imageStoreUuid;

    @Parameter(name=ApiConstants.PROJECT_ID, type=CommandType.UUID, entityType = ProjectResponse.class,
            description="Upload volume for the project")
    private Long projectId;    

    /////////////////////////////////////////////////////
    /////////////////// Accessors ///////////////////////
    /////////////////////////////////////////////////////

    public String getFormat() {
        return format;
    }

    public String getVolumeName() {
        return volumeName;
    }

    public String getUrl() {
        return url;
    }

    public Long getZoneId() {
        return zoneId;
    }

    public Long getDomainId() {
        return domainId;
    }

    public String getAccountName() {
        return accountName;
    }

    public String getChecksum() {
        return checksum;
    }
    
    public String getImageStoreUuid() {
        return this.imageStoreUuid;
    }

    /////////////////////////////////////////////////////
    /////////////// API Implementation///////////////////
    /////////////////////////////////////////////////////

    @Override
    public void execute() throws ResourceUnavailableException,
            InsufficientCapacityException, ServerApiException,
            ConcurrentOperationException, ResourceAllocationException,
            NetworkRuleConflictException {

            Volume volume = _volumeService.uploadVolume(this);
            if (volume != null){
                VolumeResponse response = _responseGenerator.createVolumeResponse(volume);
                response.setResponseName(getCommandName());
                this.setResponseObject(response);
            } else {
                throw new ServerApiException(ApiErrorCode.INTERNAL_ERROR, "Failed to upload volume");
            }
    }

    @Override
    public String getCommandName() {
           return s_name;
    }

    @Override
    public long getEntityOwnerId() {
        Long accountId = finalyzeAccountId(accountName, domainId, projectId, true);
        if (accountId == null) {
            return CallContext.current().getCallingAccount().getId();
        }

        return accountId;
    }

    @Override
    public String getEventDescription() {
        return  "uploading volume: " + getVolumeName() + " in the zone " + getZoneId();
    }

    @Override
    public String getEventType() {
        return EventTypes.EVENT_VOLUME_UPLOAD;
    }

}
