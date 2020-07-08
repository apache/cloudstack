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

import java.util.ArrayList;
import java.util.List;

import com.cloud.dc.DataCenter;
import org.apache.log4j.Logger;

import org.apache.cloudstack.api.APICommand;
import org.apache.cloudstack.api.ApiCommandJobType;
import org.apache.cloudstack.api.ApiConstants;
import org.apache.cloudstack.api.ApiErrorCode;
import org.apache.cloudstack.api.BaseAsyncCmd;
import org.apache.cloudstack.api.Parameter;
import org.apache.cloudstack.api.ResponseObject.ResponseView;
import org.apache.cloudstack.api.ServerApiException;
import org.apache.cloudstack.api.command.user.UserCmd;
import org.apache.cloudstack.api.response.TemplateResponse;
import org.apache.cloudstack.api.response.ZoneResponse;
import org.apache.cloudstack.context.CallContext;

import com.cloud.event.EventTypes;
import com.cloud.exception.ResourceAllocationException;
import com.cloud.exception.StorageUnavailableException;
import com.cloud.template.VirtualMachineTemplate;
import com.cloud.user.Account;

@APICommand(name = "copyTemplate", description = "Copies a template from one zone to another.", responseObject = TemplateResponse.class, responseView = ResponseView.Restricted,
        requestHasSensitiveInfo = false, responseHasSensitiveInfo = false)
public class CopyTemplateCmd extends BaseAsyncCmd implements UserCmd {
    public static final Logger s_logger = Logger.getLogger(CopyTemplateCmd.class.getName());
    private static final String s_name = "copytemplateresponse";

    /////////////////////////////////////////////////////
    //////////////// API parameters /////////////////////
    /////////////////////////////////////////////////////

    @Parameter(name = ApiConstants.DESTINATION_ZONE_ID,
               type = CommandType.UUID,
               entityType = ZoneResponse.class,
               required = false,
               description = "ID of the zone the template is being copied to.")
    protected Long destZoneId;

    @Parameter(name = ApiConstants.ID, type = CommandType.UUID,
            entityType = TemplateResponse.class, required = true, description = "Template ID.")
    private Long id;

    @Parameter(name = ApiConstants.SOURCE_ZONE_ID,
               type = CommandType.UUID,
               entityType = ZoneResponse.class,
            description = "ID of the zone the template is currently hosted on. " +
                    "If not specified and template is cross-zone, " +
                    "then we will sync this template to region wide image store.")
    private Long sourceZoneId;

    @Parameter(name = ApiConstants.DESTINATION_ZONE_ID_LIST,
                    type=CommandType.LIST,
                    collectionType = CommandType.UUID,
                    entityType = ZoneResponse.class,
                    required = false,
                    description = "A list of IDs of the zones that the template needs to be copied to." +
                            "Specify this list if the template needs to copied to multiple zones in one go. " +
                            "Do not specify destzoneid and destzoneids together, however one of them is required.")
    protected List<Long> destZoneIds;

    /////////////////////////////////////////////////////
    /////////////////// Accessors ///////////////////////
    /////////////////////////////////////////////////////

    public List<Long> getDestinationZoneIds() {
        if (destZoneIds != null && destZoneIds.size() != 0) {
            return destZoneIds;
        }
        if (destZoneId != null) {
            List < Long > destIds = new ArrayList<>();
            destIds.add(destZoneId);
            return destIds;
        }
        return null;
    }

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
        StringBuilder descBuilder = new StringBuilder();
        if (getDestinationZoneIds() != null) {

            for (Long destId : getDestinationZoneIds()) {
                descBuilder.append(", ");
                descBuilder.append(this._uuidMgr.getUuid(DataCenter.class, destId));
            }
            if (descBuilder.length() > 0) {
                descBuilder.deleteCharAt(0);
            }
        }

        return  "copying template: " + this._uuidMgr.getUuid(VirtualMachineTemplate.class, getId()) +((getSourceZoneId() != null) ? " from zone: " + this._uuidMgr.getUuid(DataCenter.class, getSourceZoneId()) : "") + ((descBuilder.length() > 0) ? " to zones: " + descBuilder.toString() : "");
    }

    @Override
    public ApiCommandJobType getInstanceType() {
        return ApiCommandJobType.Template;
    }

    @Override
    public Long getInstanceId() {
        return getId();
    }

    @Override
    public void execute() throws ResourceAllocationException {
        try {
            if (destZoneId == null && (destZoneIds == null || destZoneIds.size() == 0))
                throw new ServerApiException(ApiErrorCode.PARAM_ERROR,
                        "Either destzoneid or destzoneids parameters have to be specified.");

            if (destZoneId != null && destZoneIds != null && destZoneIds.size() != 0)
                throw new ServerApiException(ApiErrorCode.PARAM_ERROR,
                        "Both destzoneid and destzoneids cannot be specified at the same time.");

            CallContext.current().setEventDetails(getEventDescription());
            VirtualMachineTemplate template = _templateService.copyTemplate(this);

            if (template != null){
                List<TemplateResponse> listResponse = _responseGenerator.createTemplateResponses(getResponseView(),
                                                            template, getDestinationZoneIds(), false);
                TemplateResponse response = new TemplateResponse();
                if (listResponse != null && !listResponse.isEmpty()) {
                    response = listResponse.get(0);
                }

                response.setResponseName(getCommandName());
                setResponseObject(response);
            } else {
                throw new ServerApiException(ApiErrorCode.INTERNAL_ERROR, "Failed to copy template");
            }
        } catch (StorageUnavailableException ex) {
            s_logger.warn("Exception: ", ex);
            throw new ServerApiException(ApiErrorCode.RESOURCE_UNAVAILABLE_ERROR, ex.getMessage());
        }
    }
}
