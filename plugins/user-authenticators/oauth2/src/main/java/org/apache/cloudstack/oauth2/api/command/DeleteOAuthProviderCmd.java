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
package org.apache.cloudstack.oauth2.api.command;

import org.apache.cloudstack.api.ApiCommandResourceType;
import org.apache.cloudstack.oauth2.OAuth2AuthManager;
import org.apache.cloudstack.oauth2.api.response.OauthProviderResponse;
import org.apache.log4j.Logger;

import org.apache.cloudstack.api.APICommand;
import org.apache.cloudstack.api.ApiConstants;
import org.apache.cloudstack.api.ApiErrorCode;
import org.apache.cloudstack.api.BaseCmd;
import org.apache.cloudstack.api.Parameter;
import org.apache.cloudstack.api.ServerApiException;
import org.apache.cloudstack.api.response.SuccessResponse;
import org.apache.cloudstack.context.CallContext;

import javax.inject.Inject;

@APICommand(name = "deleteOauthProvider", description = "Deletes the registered OAuth provider", responseObject = SuccessResponse.class,
        requestHasSensitiveInfo = false, responseHasSensitiveInfo = false, since = "4.19.0")
public class DeleteOAuthProviderCmd extends BaseCmd {
    public static final Logger s_logger = Logger.getLogger(DeleteOAuthProviderCmd.class.getName());


    /////////////////////////////////////////////////////
    //////////////// API parameters /////////////////////
    /////////////////////////////////////////////////////
    @Parameter(name = ApiConstants.ID, type = CommandType.UUID, entityType = OauthProviderResponse.class, required = true, description = "id of the OAuth provider to be deleted")
    private Long id;

    @Inject
    OAuth2AuthManager _oauthMgr;

    /////////////////////////////////////////////////////
    /////////////////// Accessors ///////////////////////
    /////////////////////////////////////////////////////

    public Long getId() {
        return id;
    }

    /////////////////////////////////////////////////////
    /////////////// API Implementation///////////////////
    /////////////////////////////////////////////////////

    @Override
    public long getEntityOwnerId() {
        return CallContext.current().getCallingAccount().getId();
    }

    @Override
    public Long getApiResourceId() {
        return id;
    }

    @Override
    public ApiCommandResourceType getApiResourceType() {
        return ApiCommandResourceType.User;
    }

    @Override
    public void execute() {
        boolean result = _oauthMgr.deleteOauthProvider(getId());
        if (result) {
            SuccessResponse response = new SuccessResponse(getCommandName());
            this.setResponseObject(response);
        } else {
            throw new ServerApiException(ApiErrorCode.INTERNAL_ERROR, "Failed to delete the OAuth provider");
        }
    }
}
