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
import org.apache.cloudstack.api.command.user.iso.DetachIsoCmd;
import org.apache.cloudstack.api.command.user.vm.DeployVMCmd;
import org.apache.cloudstack.api.response.UserVmResponse;

import com.cloud.uservm.UserVm;

@APICommand(name = "detachIso", description = "Detaches any ISO file (if any) currently attached to a virtual machine.", responseObject = UserVmResponse.class, responseView = ResponseView.Full,
        requestHasSensitiveInfo = false, responseHasSensitiveInfo = true)
public class DetachIsoCmdByAdmin extends DetachIsoCmd {
    public static final Logger s_logger = Logger.getLogger(DetachIsoCmdByAdmin.class.getName());

    @Override
    public void execute(){
        boolean result = _templateService.detachIso(virtualMachineId);
        if (result) {
            UserVm userVm = _entityMgr.findById(UserVm.class, virtualMachineId);
            UserVmResponse response = _responseGenerator.createUserVmResponse(ResponseView.Full, "virtualmachine", userVm).get(0);
            response.setResponseName(DeployVMCmd.getResultObjectName());
            setResponseObject(response);
        } else {
            throw new ServerApiException(ApiErrorCode.INTERNAL_ERROR, "Failed to detach iso");
        }
    }
}
