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
package org.apache.cloudstack.api.command;

import javax.inject.Inject;

import org.apache.cloudstack.api.APICommand;
import org.apache.cloudstack.api.ApiConstants;
import org.apache.cloudstack.api.BaseCmd;
import org.apache.cloudstack.api.Parameter;
import org.apache.cloudstack.api.ServerApiException;
import org.apache.cloudstack.api.response.DomainResponse;
import org.apache.cloudstack.api.response.SuccessResponse;
import org.apache.cloudstack.ldap.LdapManager;

import com.cloud.user.Account;

@APICommand(name = "ldapTestConfiguration", description = "Tests connectivity to an LDAP server without saving a configuration", responseObject = SuccessResponse.class,
        since = "24.0", requestHasSensitiveInfo = false, responseHasSensitiveInfo = false)
public class LdapTestConfigurationCmd extends BaseCmd {

    @Inject
    private LdapManager ldapManager;

    @Parameter(name = ApiConstants.HOST_NAME, type = CommandType.STRING, required = true, description = "Hostname")
    private String hostname;

    @Parameter(name = ApiConstants.PORT, type = CommandType.INTEGER, description = "Port")
    private int port;

    @Parameter(name = ApiConstants.DOMAIN_ID, type = CommandType.UUID, entityType = DomainResponse.class, description = "Linked Domain")
    private Long domainId;

    public LdapTestConfigurationCmd() {
        super();
    }

    public LdapTestConfigurationCmd(final LdapManager ldapManager) {
        super();
        this.ldapManager = ldapManager;
    }

    public String getHostname() {
        return hostname;
    }

    public int getPort() {
        return port;
    }

    public Long getDomainId() {
        return domainId;
    }

    @Override
    public void execute() throws ServerApiException {
        // a failing bind throws InvalidParameterValueException, which is reported to the caller as an API error
        ldapManager.testConnection(this);
        SuccessResponse response = new SuccessResponse(getCommandName());
        response.setDisplayText("Successfully connected to the LDAP server");
        setResponseObject(response);
    }

    @Override
    public long getEntityOwnerId() {
        return Account.ACCOUNT_ID_SYSTEM;
    }
}
