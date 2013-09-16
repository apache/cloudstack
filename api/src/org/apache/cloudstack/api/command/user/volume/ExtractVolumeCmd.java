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

import java.net.URISyntaxException;

import org.apache.cloudstack.api.APICommand;
import org.apache.cloudstack.api.ApiCommandJobType;
import org.apache.cloudstack.api.ApiConstants;
import org.apache.cloudstack.api.ApiErrorCode;
import org.apache.cloudstack.api.BaseAsyncCmd;
import org.apache.cloudstack.api.Parameter;
import org.apache.cloudstack.api.ServerApiException;
import org.apache.cloudstack.api.response.ExtractResponse;
import org.apache.cloudstack.api.response.VolumeResponse;
import org.apache.cloudstack.api.response.ZoneResponse;
import org.apache.cloudstack.context.CallContext;

import org.apache.log4j.Logger;

import com.cloud.dc.DataCenter;
import com.cloud.event.EventTypes;
import com.cloud.storage.Upload;
import com.cloud.storage.Volume;
import com.cloud.user.Account;

@APICommand(name = "extractVolume", description="Extracts volume", responseObject=ExtractResponse.class)
public class ExtractVolumeCmd extends BaseAsyncCmd {
    public static final Logger s_logger = Logger.getLogger(ExtractVolumeCmd.class.getName());

    private static final String s_name = "extractvolumeresponse";

    /////////////////////////////////////////////////////
    //////////////// API parameters /////////////////////
    /////////////////////////////////////////////////////

    @Parameter(name=ApiConstants.ID, type=CommandType.UUID, entityType=VolumeResponse.class,
            required=true, description="the ID of the volume")
    private Long id;


    @Parameter(name=ApiConstants.URL, type=CommandType.STRING, required=false, description="the url to which the volume would be extracted")
    private String url;

    @Parameter(name=ApiConstants.ZONE_ID, type=CommandType.UUID, entityType=ZoneResponse.class,
            required=true, description="the ID of the zone where the volume is located")
    private Long zoneId;

    @Parameter(name=ApiConstants.MODE, type=CommandType.STRING, required=true, description="the mode of extraction - HTTP_DOWNLOAD or FTP_UPLOAD")
    private String mode;

    /////////////////////////////////////////////////////
    /////////////////// Accessors ///////////////////////
    /////////////////////////////////////////////////////

    public Long getId() {
        return id;
    }

    public String getUrl() {
        return url;
    }

    public Long getZoneId() {
        return zoneId;
    }

    public String getMode() {
        return mode;
    }

    /////////////////////////////////////////////////////
    /////////////// API Implementation///////////////////
    /////////////////////////////////////////////////////

    @Override
    public String getCommandName() {
        return s_name;
    }

    public static String getStaticName() {
        return s_name;
    }

    public ApiCommandJobType getInstanceType() {
        return ApiCommandJobType.Volume;
    }

    public Long getInstanceId() {
        return getId();
    }

    @Override
    public long getEntityOwnerId() {
        Volume volume = _entityMgr.findById(Volume.class, getId());
        if (volume != null) {
            return volume.getAccountId();
        }

        // invalid id, parent this command to SYSTEM so ERROR events are tracked
        return Account.ACCOUNT_ID_SYSTEM;
    }

    @Override
    public String getEventType() {
        return EventTypes.EVENT_VOLUME_EXTRACT;
    }

    @Override
    public String getEventDescription() {
        return  "Extraction job";
    }

    @Override
    public void execute(){
        CallContext.current().setEventDetails("Volume Id: " + getId());
        String uploadUrl = _volumeService.extractVolume(this);
        if (uploadUrl != null) {
            ExtractResponse response = new ExtractResponse();
            response.setResponseName(getCommandName());
            response.setObjectName("volume");
            Volume vol = _entityMgr.findById(Volume.class, id);
            response.setId(vol.getUuid());
            response.setName(vol.getName());
            DataCenter zone = _entityMgr.findById(DataCenter.class, zoneId);
            response.setZoneId(zone.getUuid());
            response.setZoneName(zone.getName());
            response.setMode(mode);
            response.setState(Upload.Status.DOWNLOAD_URL_CREATED.toString());
            Account account = _entityMgr.findById(Account.class, getEntityOwnerId());
            response.setAccountId(account.getUuid());
            response.setUrl(uploadUrl);
            this.setResponseObject(response);
        } else {
            throw new ServerApiException(ApiErrorCode.INTERNAL_ERROR, "Failed to extract volume");
        }
    }
}
