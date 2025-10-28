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

import com.cloud.exception.InvalidParameterValueException;
import com.cloud.server.ResourceIcon;
import com.cloud.server.ResourceTag;
import com.cloud.user.Account;
import com.cloud.user.User;
import org.apache.cloudstack.acl.RoleType;
import org.apache.cloudstack.api.command.user.UserCmd;
import org.apache.cloudstack.api.response.ResourceIconResponse;

import org.apache.cloudstack.api.APICommand;
import org.apache.cloudstack.api.ApiConstants;
import org.apache.cloudstack.api.BaseListAccountResourcesCmd;
import org.apache.cloudstack.api.Parameter;
import org.apache.cloudstack.api.ResponseObject.ResponseView;
import org.apache.cloudstack.api.response.ListResponse;
import org.apache.cloudstack.api.response.UserResponse;
import org.apache.commons.lang3.EnumUtils;

import java.util.List;

@APICommand(name = "listUsers", description = "Lists user accounts", responseObject = UserResponse.class,
        responseView = ResponseView.Restricted, requestHasSensitiveInfo = false, responseHasSensitiveInfo = true)
public class ListUsersCmd extends BaseListAccountResourcesCmd implements UserCmd {


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

    @Parameter(name = ApiConstants.API_KEY_ACCESS, type = CommandType.STRING, description = "List users by the Api key access value", since = "4.20.1.0", authorized = {RoleType.Admin})
    private String apiKeyAccess;

    @Parameter(name = ApiConstants.SHOW_RESOURCE_ICON, type = CommandType.BOOLEAN,
            description = "flag to display the resource icon for users")
    private Boolean showIcon;

    @Parameter(name = ApiConstants.USER_SOURCE, type = CommandType.STRING, since = "4.21.0.0",
            description = "List users by their authentication source. Valid values are: native, ldap, saml2 and saml2disabled.")
    private String userSource;

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

    public String getApiKeyAccess() {
        return apiKeyAccess;
    }

    public Boolean getShowIcon() {
        return showIcon != null ? showIcon : false;
    }

    public User.Source getUserSource() {
        if (userSource == null) {
            return null;
        }

        User.Source source = EnumUtils.getEnumIgnoreCase(User.Source.class, userSource);
        if (source == null || List.of(User.Source.OAUTH2, User.Source.UNKNOWN).contains(source)) {
            throw new InvalidParameterValueException(String.format("Invalid user source: %s. Valid values are: native, ldap, saml2 and saml2disabled.", userSource));
        }

        if (source == User.Source.NATIVE) {
            return User.Source.UNKNOWN;
        }

        return source;
    }

    /////////////////////////////////////////////////////
    /////////////// API Implementation///////////////////
    /////////////////////////////////////////////////////

    @Override
    public void execute() {
        ListResponse<UserResponse> response = _queryService.searchForUsers(getResponseView(), this);
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
