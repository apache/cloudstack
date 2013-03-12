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
package org.apache.cloudstack.api.command.admin.user;

import org.apache.cloudstack.api.APICommand;
import org.apache.cloudstack.api.ApiConstants;
import org.apache.cloudstack.api.BaseListAccountResourcesCmd;
import org.apache.cloudstack.api.Parameter;
import org.apache.cloudstack.api.response.ListResponse;
import org.apache.cloudstack.api.response.UserResponse;
import org.apache.log4j.Logger;

@APICommand(name = "listUsers", description="Lists user accounts", responseObject=UserResponse.class)
public class ListUsersCmd extends BaseListAccountResourcesCmd {
    public static final Logger s_logger = Logger.getLogger(ListUsersCmd.class.getName());

    private static final String s_name = "listusersresponse";

    /////////////////////////////////////////////////////
    //////////////// API parameters /////////////////////
    /////////////////////////////////////////////////////

    @Parameter(name=ApiConstants.ACCOUNT_TYPE, type=CommandType.LONG, description="List users by account type. Valid types include admin, domain-admin, read-only-admin, or user.")
    private Long accountType;

    @Parameter(name=ApiConstants.ID, type=CommandType.UUID, entityType=UserResponse.class, description="List user by ID.")
    private Long id;

    @Parameter(name=ApiConstants.STATE, type=CommandType.STRING, description="List users by state of the user account.")
    private String state;

    @Parameter(name=ApiConstants.USERNAME, type=CommandType.STRING, description="List user by the username")
    private String username;

    /////////////////////////////////////////////////////
    /////////////////// Accessors ///////////////////////
    /////////////////////////////////////////////////////


    public Long getAccountType() {
        return accountType;
    }

    public Long getId() {
        return id;
    }

    public String getState() {
        return state;
    }

    public String getUsername() {
        return username;
    }

    /////////////////////////////////////////////////////
    /////////////// API Implementation///////////////////
    /////////////////////////////////////////////////////

    @Override
    public String getCommandName() {
        return s_name;
    }

    @Override
    public void execute(){
        ListResponse<UserResponse> response = _queryService.searchForUsers(this);
        response.setResponseName(getCommandName());
        this.setResponseObject(response);
    }
}
