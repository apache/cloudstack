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

package org.apache.cloudstack.api.command.user.userdata;

import com.cloud.exception.ConcurrentOperationException;
import com.cloud.exception.InsufficientCapacityException;
import com.cloud.exception.NetworkRuleConflictException;
import com.cloud.exception.ResourceAllocationException;
import com.cloud.exception.ResourceUnavailableException;
import com.cloud.template.VirtualMachineTemplate;
import com.cloud.user.Account;
import com.cloud.user.UserData;
import com.cloud.utils.exception.CloudRuntimeException;
import org.apache.cloudstack.api.APICommand;
import org.apache.cloudstack.api.ApiConstants;
import org.apache.cloudstack.api.ApiErrorCode;
import org.apache.cloudstack.api.BaseCmd;
import org.apache.cloudstack.api.Parameter;
import org.apache.cloudstack.api.ResponseObject;
import org.apache.cloudstack.api.ServerApiException;
import org.apache.cloudstack.api.command.admin.AdminCmd;
import org.apache.cloudstack.api.response.TemplateResponse;
import org.apache.cloudstack.api.response.UserDataResponse;
import org.apache.log4j.Logger;

@APICommand(name = "linkUserDataToTemplate", description = "Links a userdata to a template.", responseObject = TemplateResponse.class, responseView = ResponseObject.ResponseView.Restricted,
        requestHasSensitiveInfo = false, responseHasSensitiveInfo = false)
public class LinkUserDataToTemplateCmd extends BaseCmd implements AdminCmd {
    public static final Logger s_logger = Logger.getLogger(LinkUserDataToTemplateCmd.class.getName());

    private static final String s_name = "linkUserDataToTemplateResponse";

    /////////////////////////////////////////////////////
    //////////////// API parameters /////////////////////
    /////////////////////////////////////////////////////

    @Parameter(name = ApiConstants.TEMPLATE_ID,
            type = CommandType.UUID,
            entityType = TemplateResponse.class,
            description = "the ID of the template for the virtual machine")
    private Long templateId;

    @Parameter(name = ApiConstants.USER_DATA_ID,
            type = CommandType.UUID,
            entityType = UserDataResponse.class,
            description = "the ID of the userdata that has to be linked to template")
    private Long userdataId;

    @Parameter(name = ApiConstants.USER_DATA_POLICY,
            description = "the ID of the template for the virtual machine")
    private String userdataPolicy;

    /////////////////////////////////////////////////////
    /////////////////// Accessors ///////////////////////
    /////////////////////////////////////////////////////

    public Long getTemplateId() {
        return templateId;
    }

    public Long getUserdataId() {
        return userdataId;
    }

    public UserData.UserDataOverridePolicy getUserdataPolicy() {
        if (userdataPolicy == null) {
            return UserData.UserDataOverridePolicy.allowoverride;
        }
        return UserData.UserDataOverridePolicy.valueOf(userdataPolicy);
    }

    @Override
    public void execute() throws ResourceUnavailableException, InsufficientCapacityException, ServerApiException, ConcurrentOperationException, ResourceAllocationException, NetworkRuleConflictException {
        VirtualMachineTemplate result = null;
        try {
            result = _templateService.linkUserDataToTemplate(this);
        } catch (Exception e) {
            throw new CloudRuntimeException(String.format("Failed to link userdata to template, due to: %s", e.getLocalizedMessage()), e);
        }
        if (result != null) {
            TemplateResponse response = _responseGenerator.createTemplateUpdateResponse(getResponseView(), result);
            response.setObjectName("template");
            response.setTemplateType(result.getTemplateType().toString());//Template can be either USER or ROUTING type
            response.setResponseName(getCommandName());
            setResponseObject(response);
        } else {
            throw new ServerApiException(ApiErrorCode.INTERNAL_ERROR, "Failed to link userdata to template");
        }
    }

    @Override
    public String getCommandName() {
        return s_name;
    }

    @Override
    public long getEntityOwnerId() {
        return Account.ACCOUNT_ID_SYSTEM;
    }
}
