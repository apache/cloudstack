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

package org.apache.cloudstack.cloudian.api;

import javax.inject.Inject;

import org.apache.cloudstack.acl.RoleType;
import org.apache.cloudstack.api.APICommand;
import org.apache.cloudstack.api.ApiErrorCode;
import org.apache.cloudstack.api.BaseCmd;
import org.apache.cloudstack.api.ServerApiException;
import org.apache.cloudstack.cloudian.CloudianConnector;
import org.apache.cloudstack.cloudian.response.CloudianSsoLoginResponse;

import com.cloud.user.Account;
import org.apache.commons.lang3.StringUtils;

@APICommand(name = "cloudianSsoLogin", description = "Generates single-sign-on login url for logged-in CloudStack user to access the Cloudian Management Console",
        responseObject = CloudianSsoLoginResponse.class,
        since = "4.11.0",
        authorized = {RoleType.Admin, RoleType.ResourceAdmin, RoleType.DomainAdmin, RoleType.User})
public class CloudianSsoLoginCmd extends BaseCmd {

    @Inject
    private CloudianConnector connector;

    /////////////////////////////////////////////////////
    /////////////// API Implementation///////////////////
    /////////////////////////////////////////////////////

    @Override
    public long getEntityOwnerId() {
        return Account.ACCOUNT_ID_SYSTEM;
    }


    @Override
    public void execute() {
        final String ssoUrl = connector.generateSsoUrl();
        if (StringUtils.isEmpty(ssoUrl)) {
            throw new ServerApiException(ApiErrorCode.INTERNAL_ERROR, "Failed to generate Cloudian single-sign on URL for the user");
        }
        final CloudianSsoLoginResponse response = new CloudianSsoLoginResponse();
        response.setSsoRedirectUrl(ssoUrl);
        response.setResponseName(getCommandName());
        response.setObjectName(this.getActualCommandName().toLowerCase());
        setResponseObject(response);
    }
}
