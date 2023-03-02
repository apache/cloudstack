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
package org.apache.cloudstack.api.command.user.iso;

import org.apache.log4j.Logger;

import org.apache.cloudstack.api.APICommand;
import org.apache.cloudstack.api.ApiCommandResourceType;
import org.apache.cloudstack.api.ApiConstants;
import org.apache.cloudstack.api.ApiErrorCode;
import org.apache.cloudstack.api.BaseAsyncCmd;
import org.apache.cloudstack.api.Parameter;
import org.apache.cloudstack.api.ServerApiException;
import org.apache.cloudstack.api.response.ExtractResponse;
import org.apache.cloudstack.api.response.TemplateResponse;
import org.apache.cloudstack.api.response.ZoneResponse;
import org.apache.cloudstack.context.CallContext;

import com.cloud.event.EventTypes;
import com.cloud.exception.InternalErrorException;
import com.cloud.template.VirtualMachineTemplate;
import com.cloud.user.Account;

@APICommand(name = "extractIso", description = "Extracts an ISO", responseObject = ExtractResponse.class,
        requestHasSensitiveInfo = false, responseHasSensitiveInfo = false)
public class ExtractIsoCmd extends BaseAsyncCmd {
    public static final Logger s_logger = Logger.getLogger(ExtractIsoCmd.class.getName());


    /////////////////////////////////////////////////////
    //////////////// API parameters /////////////////////
    /////////////////////////////////////////////////////

    @Parameter(name = ApiConstants.ID, type = CommandType.UUID, entityType = TemplateResponse.class, required = true, description = "the ID of the ISO file")
    private Long id;

    @Parameter(name = ApiConstants.URL, type = CommandType.STRING, required = false, length = 2048, description = "the URL to which the ISO would be extracted")
    private String url;

    @Parameter(name = ApiConstants.ZONE_ID,
               type = CommandType.UUID,
               entityType = ZoneResponse.class,
               required = false,
               description = "the ID of the zone where the ISO is originally located")
    private Long zoneId;

    @Parameter(name = ApiConstants.MODE, type = CommandType.STRING, required = true, description = "the mode of extraction - HTTP_DOWNLOAD or FTP_UPLOAD")
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
    public String getEventType() {
        return EventTypes.EVENT_ISO_EXTRACT;
    }

    @Override
    public long getEntityOwnerId() {
        VirtualMachineTemplate iso = _entityMgr.findById(VirtualMachineTemplate.class, getId());
        if (iso != null) {
            return iso.getAccountId();
        }

        // invalid id, parent this command to SYSTEM so ERROR events are tracked
        return Account.ACCOUNT_ID_SYSTEM;
    }

    @Override
    public String getEventDescription() {
        return "extracting ISO: " + getId() + " from zone: " + getZoneId();
    }

    @Override
    public ApiCommandResourceType getApiResourceType() {
        return ApiCommandResourceType.Iso;
    }

    @Override
    public Long getApiResourceId() {
        return getId();
    }

    @Override
    public void execute() {
        try {
            CallContext.current().setEventDetails(getEventDescription());
            String uploadUrl = _templateService.extract(this);
            if (uploadUrl != null) {
                ExtractResponse response = _responseGenerator.createExtractResponse(id, zoneId, getEntityOwnerId(), mode, uploadUrl);
                response.setResponseName(getCommandName());
                response.setObjectName("iso");
                this.setResponseObject(response);
            } else {
                throw new ServerApiException(ApiErrorCode.INTERNAL_ERROR, "Failed to extract ISO");
            }
        } catch (InternalErrorException ex) {
            s_logger.warn("Exception: ", ex);
            throw new ServerApiException(ApiErrorCode.INTERNAL_ERROR, ex.getMessage());
        }
    }
}
