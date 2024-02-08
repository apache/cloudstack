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
package org.apache.cloudstack.api.command.user.resource;

import java.util.ArrayList;


import org.apache.cloudstack.api.APICommand;
import org.apache.cloudstack.api.ApiConstants;
import org.apache.cloudstack.api.ApiErrorCode;
import org.apache.cloudstack.api.BaseCmd;
import org.apache.cloudstack.api.Parameter;
import org.apache.cloudstack.api.ServerApiException;
import org.apache.cloudstack.api.response.CloudIdentifierResponse;
import org.apache.cloudstack.api.response.UserResponse;

import com.cloud.user.Account;

@APICommand(name = "getCloudIdentifier", description = "Retrieves a cloud identifier.", responseObject = CloudIdentifierResponse.class,
        requestHasSensitiveInfo = false, responseHasSensitiveInfo = false)
public class GetCloudIdentifierCmd extends BaseCmd {

    /////////////////////////////////////////////////////
    //////////////// API parameters /////////////////////
    /////////////////////////////////////////////////////

    @Parameter(name = ApiConstants.USER_ID,
               type = CommandType.UUID,
               entityType = UserResponse.class,
               required = true,
               description = "the user ID for the cloud identifier")
    private Long userid;

    /////////////////////////////////////////////////////
    /////////////////// Accessors ///////////////////////
    /////////////////////////////////////////////////////

    public Long getUserId() {
        return userid;
    }

    /////////////////////////////////////////////////////
    /////////////// API Implementation///////////////////
    /////////////////////////////////////////////////////

    @Override
    public long getEntityOwnerId() {
        return Account.ACCOUNT_ID_SYSTEM;
    }

    @Override
    public void execute() {
        ArrayList<String> result = _mgr.getCloudIdentifierResponse(userid);
        CloudIdentifierResponse response = new CloudIdentifierResponse();
        if (result != null) {
            response.setCloudIdentifier(result.get(0));
            response.setSignature(result.get(1));
            response.setObjectName("cloudidentifier");
            response.setResponseName(getCommandName());
            this.setResponseObject(response);
        } else {
            throw new ServerApiException(ApiErrorCode.INTERNAL_ERROR, "Failed to get cloud identifier");
        }

    }
}
