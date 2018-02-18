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
package org.apache.cloudstack.api.command.admin.iso;

import java.util.List;

import org.apache.log4j.Logger;

import org.apache.cloudstack.api.APICommand;
import org.apache.cloudstack.api.ApiErrorCode;
import org.apache.cloudstack.api.ResponseObject.ResponseView;
import org.apache.cloudstack.api.ServerApiException;
import org.apache.cloudstack.api.command.user.iso.RegisterIsoCmd;
import org.apache.cloudstack.api.response.ListResponse;
import org.apache.cloudstack.api.response.TemplateResponse;

import com.cloud.exception.ResourceAllocationException;
import com.cloud.template.VirtualMachineTemplate;

@APICommand(name = "registerIso", responseObject = TemplateResponse.class, description = "Registers an existing ISO into the CloudStack Cloud.", responseView = ResponseView.Full,
        requestHasSensitiveInfo = false, responseHasSensitiveInfo = false)
public class RegisterIsoCmdByAdmin extends RegisterIsoCmd {
    public static final Logger s_logger = Logger.getLogger(RegisterIsoCmdByAdmin.class.getName());


    @Override
    public void execute() throws ResourceAllocationException{
        VirtualMachineTemplate template = _templateService.registerIso(this);
        if (template != null) {
            ListResponse<TemplateResponse> response = new ListResponse<TemplateResponse>();
            List<TemplateResponse> templateResponses = _responseGenerator.createIsoResponses(ResponseView.Full, template, zoneId, false);
            response.setResponses(templateResponses);
            response.setResponseName(getCommandName());
            setResponseObject(response);
        } else {
            throw new ServerApiException(ApiErrorCode.INTERNAL_ERROR, "Failed to register iso");
        }

    }
}
