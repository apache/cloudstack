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

import org.apache.log4j.Logger;

import org.apache.cloudstack.api.APICommand;
import org.apache.cloudstack.api.ApiErrorCode;
import org.apache.cloudstack.api.ResponseObject.ResponseView;
import org.apache.cloudstack.api.ServerApiException;
import org.apache.cloudstack.api.command.user.iso.UpdateIsoCmd;
import org.apache.cloudstack.api.response.TemplateResponse;

import com.cloud.template.VirtualMachineTemplate;

@APICommand(name = "updateIso", description = "Updates an ISO file.", responseObject = TemplateResponse.class, responseView = ResponseView.Full,
        requestHasSensitiveInfo = false, responseHasSensitiveInfo = false)
public class UpdateIsoCmdByAdmin extends UpdateIsoCmd {
    public static final Logger s_logger = Logger.getLogger(UpdateIsoCmdByAdmin.class.getName());

    @Override
    public void execute(){
        VirtualMachineTemplate result = _templateService.updateTemplate(this);
        if (result != null) {
            TemplateResponse response = _responseGenerator.createTemplateUpdateResponse(ResponseView.Full, result);
            response.setResponseName(getCommandName());
            setResponseObject(response);
        } else {
            throw new ServerApiException(ApiErrorCode.INTERNAL_ERROR, "Failed to update iso");
        }
    }
}
