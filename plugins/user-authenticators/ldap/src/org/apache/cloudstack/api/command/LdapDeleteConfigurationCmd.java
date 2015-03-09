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

import org.apache.log4j.Logger;

import org.apache.cloudstack.api.APICommand;
import org.apache.cloudstack.api.ApiErrorCode;
import org.apache.cloudstack.api.BaseCmd;
import org.apache.cloudstack.api.Parameter;
import org.apache.cloudstack.api.ServerApiException;
import org.apache.cloudstack.api.response.LdapConfigurationResponse;
import org.apache.cloudstack.ldap.LdapManager;

import com.cloud.exception.InvalidParameterValueException;
import com.cloud.user.Account;

@APICommand(name = "deleteLdapConfiguration", description = "Remove an Ldap Configuration", responseObject = LdapConfigurationResponse.class, since = "4.2.0",
        requestHasSensitiveInfo = false, responseHasSensitiveInfo = false)
public class LdapDeleteConfigurationCmd extends BaseCmd {
    public static final Logger s_logger = Logger.getLogger(LdapDeleteConfigurationCmd.class.getName());
    private static final String s_name = "ldapconfigurationresponse";

    @Inject
    private LdapManager _ldapManager;

    @Parameter(name = "hostname", type = CommandType.STRING, required = true, description = "Hostname")
    private String hostname;

    public LdapDeleteConfigurationCmd() {
        super();
    }

    public LdapDeleteConfigurationCmd(final LdapManager ldapManager) {
        super();
        _ldapManager = ldapManager;
    }

    @Override
    public void execute() throws ServerApiException {
        try {
            final LdapConfigurationResponse response = _ldapManager.deleteConfiguration(hostname);
            response.setObjectName("LdapDeleteConfiguration");
            response.setResponseName(getCommandName());
            setResponseObject(response);
        } catch (final InvalidParameterValueException e) {
            throw new ServerApiException(ApiErrorCode.INTERNAL_ERROR, e.toString());
        }

    }

    @Override
    public String getCommandName() {
        return s_name;
    }

    @Override
    public long getEntityOwnerId() {
        return Account.ACCOUNT_ID_SYSTEM;
    }
}
