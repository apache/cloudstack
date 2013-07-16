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
package org.apache.cloudstack.api.command.user.template;

import java.util.List;

import org.apache.cloudstack.api.APICommand;
import org.apache.cloudstack.api.ApiCommandJobType;
import org.apache.cloudstack.api.ApiConstants;
import org.apache.cloudstack.api.ApiErrorCode;
import org.apache.cloudstack.api.BaseAsyncCmd;
import org.apache.cloudstack.api.Parameter;
import org.apache.cloudstack.api.ServerApiException;
import org.apache.cloudstack.api.response.TemplateResponse;
import org.apache.cloudstack.api.response.UserVmResponse;
import org.apache.cloudstack.api.response.ZoneResponse;
import org.apache.cloudstack.context.CallContext;

import org.apache.log4j.Logger;

import com.cloud.event.EventTypes;
import com.cloud.exception.ResourceAllocationException;
import com.cloud.exception.StorageUnavailableException;
import com.cloud.template.VirtualMachineTemplate;
import com.cloud.user.Account;

@APICommand(name = "copyTemplate", description="Copies a template from one zone to another.", responseObject=TemplateResponse.class)
public class CopyTemplateCmd extends BaseAsyncCmd {
    public static final Logger s_logger = Logger.getLogger(CopyTemplateCmd.class.getName());
    private static final String s_name = "copytemplateresponse";

    /////////////////////////////////////////////////////
    //////////////// API parameters /////////////////////
    /////////////////////////////////////////////////////

    @Parameter(name=ApiConstants.DESTINATION_ZONE_ID, type=CommandType.UUID, entityType = ZoneResponse.class,
            required=true, description="ID of the zone the template is being copied to.")
    private Long destZoneId;

    @Parameter(name=ApiConstants.ID, type=CommandType.UUID, entityType = TemplateResponse.class,
            required=true, description="Template ID.")
    private Long id;

    @Parameter(name=ApiConstants.SOURCE_ZONE_ID, type=CommandType.UUID, entityType = ZoneResponse.class,
            required=true, description="ID of the zone the template is currently hosted on.")
    private Long sourceZoneId;


    /////////////////////////////////////////////////////
    /////////////////// Accessors ///////////////////////
    /////////////////////////////////////////////////////

    public Long getDestinationZoneId() {
        return destZoneId;
    }

    public Long getId() {
        return id;
    }

    public Long getSourceZoneId() {
        return sourceZoneId;
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

    @Override
    public long getEntityOwnerId() {
        VirtualMachineTemplate template = _entityMgr.findById(VirtualMachineTemplate.class, getId());
        if (template != null) {
            return template.getAccountId();
        }

        // bad id given, parent this command to SYSTEM so ERROR events are tracked
        return Account.ACCOUNT_ID_SYSTEM;
    }

    @Override
    public String getEventType() {
        return EventTypes.EVENT_TEMPLATE_COPY;
    }

    @Override
    public String getEventDescription() {
        return  "copying template: " + getId() + " from zone: " + getSourceZoneId() + " to zone: " + getDestinationZoneId();
    }

    public ApiCommandJobType getInstanceType() {
        return ApiCommandJobType.Template;
    }

    public Long getInstanceId() {
        return getId();
    }

    @Override
    public void execute() throws ResourceAllocationException{
        try {
            CallContext.current().setEventDetails(getEventDescription());
            VirtualMachineTemplate template = _templateService.copyTemplate(this);

            if (template != null){
                List<TemplateResponse> listResponse = _responseGenerator.createTemplateResponses(template, getDestinationZoneId(), false);
                TemplateResponse response = new TemplateResponse();
                if (listResponse != null && !listResponse.isEmpty()) {
                    response = listResponse.get(0);
                }

                response.setResponseName(getCommandName());
                this.setResponseObject(response);
            } else {
                throw new ServerApiException(ApiErrorCode.INTERNAL_ERROR, "Failed to copy template");
            }
        } catch (StorageUnavailableException ex) {
            s_logger.warn("Exception: ", ex);
            throw new ServerApiException(ApiErrorCode.RESOURCE_UNAVAILABLE_ERROR, ex.getMessage());
        }
    }
}

