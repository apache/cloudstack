//Licensed to the Apache Software Foundation (ASF) under one
//or more contributor license agreements.  See the NOTICE file
//distributed with this work for additional information
//regarding copyright ownership.  The ASF licenses this file
//to you under the Apache License, Version 2.0 (the
//"License"); you may not use this file except in compliance
//with the License.  You may obtain a copy of the License at
//
//http://www.apache.org/licenses/LICENSE-2.0
//
//Unless required by applicable law or agreed to in writing,
//software distributed under the License is distributed on an
//"AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
//KIND, either express or implied.  See the License for the
//specific language governing permissions and limitations
//under the License.
package org.apache.cloudstack.api.command.admin.address;

import org.apache.log4j.Logger;

import org.apache.cloudstack.api.APICommand;
import org.apache.cloudstack.api.ApiConstants;
import org.apache.cloudstack.api.ApiErrorCode;
import org.apache.cloudstack.api.BaseCmd;
import org.apache.cloudstack.api.Parameter;
import org.apache.cloudstack.api.ServerApiException;
import org.apache.cloudstack.api.response.AcquireIPAddressResponse;
import org.apache.cloudstack.api.response.SuccessResponse;

import com.cloud.user.Account;

@APICommand(name = "releasePodIpAddress", description = "Releases a Pod IP back to the Pod", responseObject = SuccessResponse.class, requestHasSensitiveInfo = false, responseHasSensitiveInfo = false)
public class ReleasePodIpCmdByAdmin extends BaseCmd {
    public static final Logger s_logger = Logger.getLogger(ReleasePodIpCmdByAdmin.class.getName());

    private static final String s_name = "releasepodipresponse";

    /////////////////////////////////////////////////////
    //////////////// API parameters /////////////////////
    /////////////////////////////////////////////////////

    @Parameter(name = ApiConstants.ID, type = CommandType.LONG, entityType = AcquireIPAddressResponse.class, required = true, description = "UUID of the Pod IP")
    private Long id;

    /////////////////////////////////////////////////////
    /////////////////// Accessors ///////////////////////
    /////////////////////////////////////////////////////

    public long getId() {
        return id;
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
        boolean result = _networkService.releasePodIp(this);
        if (result) {
            SuccessResponse response = new SuccessResponse(getCommandName());
            response.setDisplayText("IP is released successfully");
            setResponseObject(response);
        } else {
            throw new ServerApiException(ApiErrorCode.INTERNAL_ERROR, "Failed to release  Pod ip ");
        }
    }
}
