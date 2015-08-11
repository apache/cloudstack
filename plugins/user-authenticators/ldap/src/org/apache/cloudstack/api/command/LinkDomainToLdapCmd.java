/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.cloudstack.api.command;

import javax.inject.Inject;

import com.cloud.exception.InvalidParameterValueException;
import org.apache.cloudstack.api.APICommand;
import org.apache.cloudstack.api.ApiConstants;
import org.apache.cloudstack.api.ApiErrorCode;
import org.apache.cloudstack.api.BaseCmd;
import org.apache.cloudstack.api.Parameter;
import org.apache.cloudstack.api.ServerApiException;
import org.apache.cloudstack.api.response.DomainResponse;
import org.apache.cloudstack.api.response.LinkDomainToLdapResponse;
import org.apache.cloudstack.ldap.LdapManager;
import org.apache.log4j.Logger;

import com.cloud.user.Account;

@APICommand(name = "linkDomainToLdap", description = "link an existing cloudstack domain to group or OU in ldap", responseObject = LinkDomainToLdapResponse.class, since = "4.6.0",
    requestHasSensitiveInfo = false, responseHasSensitiveInfo = false)
public class LinkDomainToLdapCmd extends BaseCmd {
    public static final Logger s_logger = Logger.getLogger(LinkDomainToLdapCmd.class.getName());
    private static final String s_name = "linkdomaintoldapresponse";

    @Parameter(name = ApiConstants.DOMAIN_ID, type = CommandType.UUID, entityType = DomainResponse.class, description = "The id of the domain which has to be linked to LDAP.")
    private Long domainId;

    @Parameter(name = ApiConstants.TYPE, type = CommandType.STRING, required = true, description = "type of the ldap name. GROUP or OU")
    private String type;

    @Parameter(name = ApiConstants.NAME, type = CommandType.STRING, required = true, description = "name of the group or OU in LDAP")
    private String name;

    @Parameter(name = ApiConstants.ADMIN, type = CommandType.STRING, required = false, description = "domain admin username in LDAP ")
    private String admin;

    @Parameter(name = ApiConstants.ACCOUNT_TYPE, type = CommandType.SHORT, required = true, description = "Type of the account to auto import. Specify 0 for user, 1 for root " +
        "admin, and 2 for domain admin")
    private short accountType;

    @Inject
    private LdapManager _ldapManager;

    @Override
    public void execute() throws ServerApiException {
        try {
            LinkDomainToLdapResponse response = _ldapManager.linkDomainToLdap(domainId, type, name, accountType);
            response.setObjectName("LinkDomainToLdap");
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
