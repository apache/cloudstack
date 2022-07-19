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

import com.cloud.server.ResourceIcon;
import com.cloud.server.ResourceTag;
import com.cloud.user.Account;
import org.apache.cloudstack.api.response.ResourceIconResponse;
import org.apache.log4j.Logger;

import org.apache.cloudstack.api.APICommand;
import org.apache.cloudstack.api.ApiConstants;
import org.apache.cloudstack.api.BaseListAccountResourcesCmd;
import org.apache.cloudstack.api.Parameter;
import org.apache.cloudstack.api.response.ListResponse;
import org.apache.cloudstack.api.response.UserResponse;

import java.util.List;

@APICommand(name = "listUsers", description = "Lists user accounts", responseObject = UserResponse.class,
        requestHasSensitiveInfo = false, responseHasSensitiveInfo = true)
public class ListUsersCmd extends BaseListAccountResourcesCmd {
    public static final Logger s_logger = Logger.getLogger(ListUsersCmd.class.getName());

    private static final String s_name = "listusersresponse";

    /////////////////////////////////////////////////////
    //////////////// API parameters /////////////////////
    /////////////////////////////////////////////////////

    @Parameter(name = ApiConstants.ACCOUNT_TYPE,
               type = CommandType.INTEGER,
               description = "List users by account type. Valid types include admin, domain-admin, read-only-admin, or user.")
    private Integer accountType;

    @Parameter(name = ApiConstants.ID, type = CommandType.UUID, entityType = UserResponse.class, description = "List user by ID.")
    private Long id;

    @Parameter(name = ApiConstants.STATE, type = CommandType.STRING, description = "List users by state of the user account.")
    private String state;

    @Parameter(name = ApiConstants.USERNAME, type = CommandType.STRING, description = "List user by the username")
    private String username;

    @Parameter(name = ApiConstants.SHOW_RESOURCE_ICON, type = CommandType.BOOLEAN,
            description = "flag to display the resource icon for users")
    private Boolean showIcon;

    /////////////////////////////////////////////////////
    /////////////////// Accessors ///////////////////////
    /////////////////////////////////////////////////////

    public Account.Type getAccountType() {
        return Account.Type.getFromValue(accountType);
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

    public Boolean getShowIcon() {
        return showIcon != null ? showIcon : false;
    }

    /////////////////////////////////////////////////////
    /////////////// API Implementation///////////////////
    /////////////////////////////////////////////////////

    @Override
    public String getCommandName() {
        return s_name;
    }

    @Override
    public void execute() {
        ListResponse<UserResponse> response = _queryService.searchForUsers(this);
        response.setResponseName(getCommandName());
        this.setResponseObject(response);
        if (response != null && response.getCount() > 0 && getShowIcon()) {
            updateUserResponse(response.getResponses());
        }
    }

    private void updateUserResponse(List<UserResponse> response) {
        for (UserResponse userResponse : response) {
            ResourceIcon resourceIcon = resourceIconManager.getByResourceTypeAndUuid(ResourceTag.ResourceObjectType.User, userResponse.getObjectId());
            if (resourceIcon == null) {
                resourceIcon = resourceIconManager.getByResourceTypeAndUuid(ResourceTag.ResourceObjectType.Account, userResponse.getAccountId());
                if (resourceIcon == null) {
                    continue;
                }
            }
            ResourceIconResponse iconResponse = _responseGenerator.createResourceIconResponse(resourceIcon);
            userResponse.setResourceIconResponse(iconResponse);
        }
    }
}
