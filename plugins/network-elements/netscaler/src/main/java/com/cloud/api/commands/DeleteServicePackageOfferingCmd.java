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

package com.cloud.api.commands;

import javax.inject.Inject;
import javax.persistence.EntityExistsException;


import org.apache.cloudstack.api.APICommand;
import org.apache.cloudstack.api.ApiConstants;
import org.apache.cloudstack.api.ApiErrorCode;
import org.apache.cloudstack.api.BaseCmd;
import org.apache.cloudstack.api.Parameter;
import org.apache.cloudstack.api.ServerApiException;
import org.apache.cloudstack.context.CallContext;

import org.apache.cloudstack.api.response.SuccessResponse;

import com.cloud.api.response.NetScalerServicePackageResponse;
import com.cloud.exception.ConcurrentOperationException;
import com.cloud.network.element.NetscalerLoadBalancerElementService;
import com.cloud.utils.exception.CloudRuntimeException;

@APICommand(name = "deleteServicePackageOffering", responseObject = SuccessResponse.class, description = "Delete Service Package")
public class DeleteServicePackageOfferingCmd extends BaseCmd {

    private static final String s_name = "deleteServicePackage";
    @Inject
    NetscalerLoadBalancerElementService _netsclarLbService;

    /////////////////////////////////////////////////////
    //////////////// API parameters /////////////////////
    /////////////////////////////////////////////////////

    @Parameter(name = ApiConstants.ID, type = CommandType.STRING, entityType = NetScalerServicePackageResponse.class, required = true, description = "the service offering ID")
    private String ID;

    @Override
    public void execute() throws ServerApiException, ConcurrentOperationException, EntityExistsException {
        SuccessResponse response = new SuccessResponse();
        try {
            boolean result = _netsclarLbService.deleteServicePackageOffering(this);

            if (response != null && result) {
                response.setDisplayText("Deleted Successfully");
                response.setSuccess(result);
                response.setResponseName(getCommandName());
                this.setResponseObject(response);
            }
        } catch (CloudRuntimeException runtimeExcp) {
            response.setDisplayText(runtimeExcp.getMessage());
            response.setSuccess(false);
            response.setResponseName(getCommandName());
            this.setResponseObject(response);
            return;
        } catch (Exception e) {
            throw new ServerApiException(ApiErrorCode.INTERNAL_ERROR, "Failed to delete Service Package due to internal error.");
        }
    }

    public String getId() {
        return ID;
    }

    @Override
    public String getCommandName() {
        return s_name;
    }

    @Override
    public long getEntityOwnerId() {
        return CallContext.current().getCallingAccount().getId();
    }

}
