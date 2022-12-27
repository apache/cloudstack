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
import org.apache.cloudstack.api.BaseCmd;
import org.apache.cloudstack.cloudian.CloudianConnector;
import org.apache.cloudstack.cloudian.response.CloudianEnabledResponse;

import com.cloud.user.Account;

@APICommand(name = "cloudianIsEnabled", description = "Checks if the Cloudian Connector is enabled",
        responseObject = CloudianEnabledResponse.class,
        requestHasSensitiveInfo = false, responseHasSensitiveInfo = false,
        since = "4.11.0",
        authorized = {RoleType.Admin, RoleType.ResourceAdmin, RoleType.DomainAdmin, RoleType.User})
public class CloudianIsEnabledCmd extends BaseCmd {

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
        final CloudianEnabledResponse response = new CloudianEnabledResponse();
        response.setEnabled(connector.isEnabled());
        response.setCmcUrl(connector.getCmcUrl());
        response.setObjectName(getActualCommandName().toLowerCase());
        response.setResponseName(getCommandName());
        setResponseObject(response);
    }
}
