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

import org.apache.cloudstack.acl.RoleType;
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

import com.cloud.template.VirtualMachineTemplate;
import com.cloud.user.Account;
import com.cloud.user.UserData;
import com.cloud.utils.exception.CloudRuntimeException;

@APICommand(name = "linkUserDataToTemplate", description = "Link or unlink a userdata to a template.", responseObject = TemplateResponse.class, responseView = ResponseObject.ResponseView.Restricted,
        requestHasSensitiveInfo = false, responseHasSensitiveInfo = false, since = "4.18.0",
        authorized = {RoleType.Admin, RoleType.ResourceAdmin, RoleType.DomainAdmin, RoleType.User})
public class LinkUserDataToTemplateCmd extends BaseCmd implements AdminCmd {
    public static final Logger s_logger = Logger.getLogger(LinkUserDataToTemplateCmd.class.getName());


    /////////////////////////////////////////////////////
    //////////////// API parameters /////////////////////
    /////////////////////////////////////////////////////

    @Parameter(name = ApiConstants.TEMPLATE_ID,
            type = CommandType.UUID,
            entityType = TemplateResponse.class,
            description = "the ID of the template for the virtual machine")
    private Long templateId;

    @Parameter(name = ApiConstants.ISO_ID,
            type = CommandType.UUID,
            entityType = TemplateResponse.class,
            description = "the ID of the ISO for the virtual machine")
    private Long isoId;

    @Parameter(name = ApiConstants.USER_DATA_ID,
            type = CommandType.UUID,
            entityType = UserDataResponse.class,
            description = "the ID of the userdata that has to be linked to template/ISO. If not provided existing userdata will be unlinked from the template/ISO")
    private Long userdataId;

    @Parameter(name = ApiConstants.USER_DATA_POLICY,
            type = CommandType.STRING,
            description = "an optional override policy of the userdata. Possible values are - ALLOWOVERRIDE, APPEND, DENYOVERRIDE. Default policy is allowoverride")
    private String userdataPolicy;

    /////////////////////////////////////////////////////
    /////////////////// Accessors ///////////////////////
    /////////////////////////////////////////////////////

    public Long getTemplateId() {
        return templateId;
    }

    public Long getIsoId() {
        return isoId;
    }

    public Long getUserdataId() {
        return userdataId;
    }

    public UserData.UserDataOverridePolicy getUserdataPolicy() {
        if (userdataPolicy == null) {
            return UserData.UserDataOverridePolicy.ALLOWOVERRIDE;
        }
        return UserData.UserDataOverridePolicy.valueOf(userdataPolicy.toUpperCase());
    }

    @Override
    public void execute() {
        VirtualMachineTemplate result = null;
        try {
            result = _templateService.linkUserDataToTemplate(this);
        } catch (Exception e) {
            throw new CloudRuntimeException(String.format("Failed to link userdata to template, due to: %s", e.getLocalizedMessage()), e);
        }
        if (result != null) {
            TemplateResponse response = _responseGenerator.createTemplateUpdateResponse(getResponseView(), result);
            if (getTemplateId() != null) {
                response.setObjectName("template");
            } else {
                response.setObjectName("iso");
            }
            response.setTemplateType(result.getTemplateType().toString());//Template can be either USER or ROUTING type
            response.setResponseName(getCommandName());
            setResponseObject(response);
        } else {
            throw new ServerApiException(ApiErrorCode.INTERNAL_ERROR, "Failed to link userdata to template");
        }
    }

    @Override
    public long getEntityOwnerId() {
        VirtualMachineTemplate template = _entityMgr.findById(VirtualMachineTemplate.class, getTemplateId());
        if (template != null) {
            return template.getAccountId();
        }

        return Account.ACCOUNT_ID_SYSTEM; // no account info given, parent this command to SYSTEM so ERROR events are tracked
    }
}
