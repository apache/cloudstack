//Licensed to the Apache Software Foundation (ASF) under one or more
// contributor license agreements.  See the NOTICE file distributed with
// this work for additional information regarding copyright ownership.
// The ASF licenses this file to You under the Apache License, Version 2.0
// (the "License"); you may not use this file except in compliance with
// the License.  You may obtain a copy of the License at
//
//   http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.
package com.cloud.api.commands;

import javax.inject.Inject;
import javax.persistence.EntityExistsException;

import org.apache.log4j.Logger;

import org.apache.cloudstack.api.APICommand;
import org.apache.cloudstack.api.ApiConstants;
import org.apache.cloudstack.api.ApiErrorCode;
import org.apache.cloudstack.api.BaseCmd;
import org.apache.cloudstack.api.Parameter;
import org.apache.cloudstack.api.ServerApiException;
import org.apache.cloudstack.context.CallContext;

import com.cloud.api.response.NetScalerServicePackageResponse;
import com.cloud.exception.ConcurrentOperationException;
import com.cloud.exception.InvalidParameterValueException;
import com.cloud.network.element.NetscalerLoadBalancerElementService;
import com.cloud.utils.exception.CloudRuntimeException;

@APICommand(name = "registerNetscalerServicePackage", responseObject = NetScalerServicePackageResponse.class, description = "Registers NCC Service Package")
public class RegisterServicePackageCmd extends BaseCmd {

    public static final Logger s_logger = Logger.getLogger(RegisterServicePackageCmd.class.getName());
    private static final String s_name = "registerNetscalerServicePackage";

    @Inject
    NetscalerLoadBalancerElementService _netsclarLbService;

    /////////////////////////////////////////////////////
    //////////////// API parameters /////////////////////
    /////////////////////////////////////////////////////

    @Parameter(name = ApiConstants.NAME, type = CommandType.STRING, required = true, description = "Name of the service Package.")
    private String spName;

    @Parameter(name = ApiConstants.DESCRIPTION, type = CommandType.STRING, required = true, description = "Description of Service Package")
    private String description;


    @Override
    public void execute() throws ServerApiException, ConcurrentOperationException, EntityExistsException {
        try {
            NetScalerServicePackageResponse response = _netsclarLbService.registerNetscalerServicePackage(this);
            if (response != null) {
                response.setObjectName("netscalerservicepackage");
                response.setResponseName(getCommandName());
                this.setResponseObject(response);
            } else {
                throw new ServerApiException(ApiErrorCode.INTERNAL_ERROR, "Failed to add Service Package due to internal error.");
            }
        } catch (InvalidParameterValueException invalidParamExcp) {
            throw new ServerApiException(ApiErrorCode.PARAM_ERROR, invalidParamExcp.getMessage());
        } catch (CloudRuntimeException runtimeExcp) {
            throw new ServerApiException(ApiErrorCode.INTERNAL_ERROR, runtimeExcp.getMessage());
        } catch (EntityExistsException runtimeExcp) {
            throw new ServerApiException(ApiErrorCode.INTERNAL_ERROR, "Service Package Already Exists with Name " + getSpName());
        }

    }

    @Override
    public String getCommandName() {
        return s_name;
    }

    @Override
    public long getEntityOwnerId() {
        return CallContext.current().getCallingAccount().getId();
    }

    public String getSpName() {
        return spName;
    }

    public String getDescription() {
        return description;
    }

}
