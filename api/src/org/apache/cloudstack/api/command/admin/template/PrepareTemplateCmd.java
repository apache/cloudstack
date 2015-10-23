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
package org.apache.cloudstack.api.command.admin.template;

import java.util.List;

import org.apache.log4j.Logger;

import org.apache.cloudstack.acl.SecurityChecker.AccessType;
import org.apache.cloudstack.api.ACL;
import org.apache.cloudstack.api.APICommand;
import org.apache.cloudstack.api.ApiConstants;
import org.apache.cloudstack.api.BaseCmd;
import org.apache.cloudstack.api.Parameter;
import org.apache.cloudstack.api.ResponseObject.ResponseView;
import org.apache.cloudstack.api.response.ListResponse;
import org.apache.cloudstack.api.response.StoragePoolResponse;
import org.apache.cloudstack.api.response.TemplateResponse;
import org.apache.cloudstack.api.response.ZoneResponse;

import com.cloud.template.VirtualMachineTemplate;
import com.cloud.user.Account;

@APICommand(name = "prepareTemplate", responseObject = TemplateResponse.class, description = "load template into primary storage", entityType = {VirtualMachineTemplate.class},
        requestHasSensitiveInfo = false, responseHasSensitiveInfo = false)
public class PrepareTemplateCmd extends BaseCmd {
    public static final Logger s_logger = Logger.getLogger(PrepareTemplateCmd.class.getName());

    private static final String s_name = "preparetemplateresponse";

    /////////////////////////////////////////////////////
    //////////////// API parameters /////////////////////
    /////////////////////////////////////////////////////

    @Parameter(name = ApiConstants.ZONE_ID,
               type = CommandType.UUID,
               entityType = ZoneResponse.class,
               required = true,
               description = "zone ID of the template to be prepared in primary storage(s).")
    private Long zoneId;

    @ACL(accessType = AccessType.OperateEntry)
    @Parameter(name = ApiConstants.TEMPLATE_ID,
               type = CommandType.UUID,
               entityType = TemplateResponse.class,
               required = true,
               description = "template ID of the template to be prepared in primary storage(s).")
    private Long templateId;

    @ACL(accessType = AccessType.OperateEntry)
    @Parameter(name = ApiConstants.STORAGE_ID,
            type = CommandType.UUID,
            entityType = StoragePoolResponse.class,
            required = false,
            description = "storage pool ID of the primary storage pool to which the template should be prepared. If it is not provided the template" +
                    " is prepared on all the available primary storage pools.")
    private Long storageId;

    /////////////////////////////////////////////////////
    /////////////////// Accessors ///////////////////////
    /////////////////////////////////////////////////////

    public Long getZoneId() {
        return zoneId;
    }

    public Long getTemplateId() {
        return templateId;
    }

    public Long getStorageId() {
        return storageId;
    }

    /////////////////////////////////////////////////////
    /////////////// API Implementation///////////////////
    /////////////////////////////////////////////////////

    @Override
    public String getCommandName() {
        return s_name;
    }

    @Override
    public long getEntityOwnerId() {
        return Account.ACCOUNT_ID_SYSTEM;
    }

    @Override
    public void execute() {
        ListResponse<TemplateResponse> response = new ListResponse<TemplateResponse>();

        VirtualMachineTemplate vmTemplate = _templateService.prepareTemplate(templateId, zoneId, storageId);
        List<TemplateResponse> templateResponses = _responseGenerator.createTemplateResponses(ResponseView.Full, vmTemplate, zoneId, true);
        response.setResponses(templateResponses);
        response.setResponseName(getCommandName());
        setResponseObject(response);
    }
}
