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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.apache.cloudstack.acl.RoleType;
import org.apache.cloudstack.api.APICommand;
import org.apache.cloudstack.api.ApiConstants;
import org.apache.cloudstack.api.BaseCmd;
import org.apache.cloudstack.api.Parameter;
import org.apache.cloudstack.api.ServerApiException;
import org.apache.cloudstack.api.response.DomainResponse;
import org.apache.cloudstack.api.response.ProjectResponse;
import org.apache.cloudstack.api.response.SuccessResponse;
import org.apache.cloudstack.api.response.UserDataResponse;
import org.apache.cloudstack.context.CallContext;
import org.apache.commons.lang3.StringUtils;

import com.cloud.exception.ConcurrentOperationException;
import com.cloud.exception.InsufficientCapacityException;
import com.cloud.exception.InvalidParameterValueException;
import com.cloud.exception.NetworkRuleConflictException;
import com.cloud.exception.ResourceAllocationException;
import com.cloud.exception.ResourceUnavailableException;
import com.cloud.network.NetworkModel;
import com.cloud.user.UserData;

@APICommand(name = "registerUserData",
        description = "Register a new userdata.",
        since = "4.18",
        responseObject = SuccessResponse.class,
        requestHasSensitiveInfo = false,
        responseHasSensitiveInfo = false,
        authorized = {RoleType.Admin, RoleType.ResourceAdmin, RoleType.DomainAdmin, RoleType.User})
public class RegisterUserDataCmd extends BaseCmd {


    /////////////////////////////////////////////////////
    //////////////// API parameters /////////////////////
    /////////////////////////////////////////////////////

    @Parameter(name = ApiConstants.NAME, type = CommandType.STRING, required = true, description = "Name of the userdata")
    private String name;

    //Owner information
    @Parameter(name = ApiConstants.ACCOUNT, type = CommandType.STRING, description = "an optional account for the userdata. Must be used with domainId.")
    private String accountName;

    @Parameter(name = ApiConstants.DOMAIN_ID,
            type = CommandType.UUID,
            entityType = DomainResponse.class,
            description = "an optional domainId for the userdata. If the account parameter is used, domainId must also be used.")
    private Long domainId;

    @Parameter(name = ApiConstants.PROJECT_ID, type = CommandType.UUID, entityType = ProjectResponse.class, description = "an optional project for the userdata")
    private Long projectId;

    @Parameter(name = ApiConstants.USER_DATA,
            type = CommandType.STRING,
            required = true,
            description = "Base64 encoded userdata content. " +
                    "Using HTTP GET (via querystring), you can send up to 4KB of data after base64 encoding. " +
                    "Using HTTP POST (via POST body), you can send up to 1MB of data after base64 encoding. " +
                    "You also need to change vm.userdata.max.length value",
            length = 1048576)
    private String userData;

    @Parameter(name = ApiConstants.PARAMS, type = CommandType.STRING, description = "comma separated list of variables declared in userdata content")
    private String params;


    /////////////////////////////////////////////////////
    /////////////////// Accessors ///////////////////////
    /////////////////////////////////////////////////////

    public String getName() {
        return name;
    }

    public String getAccountName() {
        return accountName;
    }

    public Long getDomainId() {
        return domainId;
    }

    public Long getProjectId() {
        return projectId;
    }

    public String getUserData() {
        return userData;
    }

    public String getParams() {
        checkForVRMetadataFileNames(params);
        return params;
    }

    public void checkForVRMetadataFileNames(String params) {
        if (StringUtils.isNotEmpty(params)) {
            List<String> keyValuePairs = new ArrayList<>(Arrays.asList(params.split(",")));
            keyValuePairs.retainAll(NetworkModel.metadataFileNames);
            if (!keyValuePairs.isEmpty()) {
                throw new InvalidParameterValueException(String.format("Params passed here have a few virtual router metadata file names %s", keyValuePairs));
            }
        }
    }

    /////////////////////////////////////////////////////
    /////////////// API Implementation///////////////////
    /////////////////////////////////////////////////////

    @Override
    public long getEntityOwnerId() {
        Long accountId = _accountService.finalyzeAccountId(accountName, domainId, projectId, true);
        if (accountId == null) {
            return CallContext.current().getCallingAccount().getId();
        }

        return accountId;
    }

    @Override
    public void execute() throws ResourceUnavailableException, InsufficientCapacityException, ServerApiException, ConcurrentOperationException, ResourceAllocationException, NetworkRuleConflictException {
        UserData result = _mgr.registerUserData(this);
        UserDataResponse response = _responseGenerator.createUserDataResponse(result);
        response.setResponseName(getCommandName());
        response.setObjectName(ApiConstants.USER_DATA);
        setResponseObject(response);
    }
}
