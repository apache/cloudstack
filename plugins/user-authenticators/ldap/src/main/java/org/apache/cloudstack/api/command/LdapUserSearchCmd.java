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

import java.util.ArrayList;
import java.util.List;

import javax.inject.Inject;

import org.apache.log4j.Logger;

import org.apache.cloudstack.api.APICommand;
import org.apache.cloudstack.api.BaseListCmd;
import org.apache.cloudstack.api.Parameter;
import org.apache.cloudstack.api.response.LdapUserResponse;
import org.apache.cloudstack.api.response.ListResponse;
import org.apache.cloudstack.ldap.LdapManager;
import org.apache.cloudstack.ldap.LdapUser;
import org.apache.cloudstack.ldap.NoLdapUserMatchingQueryException;

import com.cloud.user.Account;

@APICommand(name = "searchLdap", responseObject = LdapUserResponse.class, description = "Searches LDAP based on the username attribute", since = "4.2.0",
        requestHasSensitiveInfo = false, responseHasSensitiveInfo = false)
public class LdapUserSearchCmd extends BaseListCmd {

    public static final Logger s_logger = Logger.getLogger(LdapUserSearchCmd.class.getName());
    private static final String s_name = "ldapuserresponse";
    @Inject
    private LdapManager _ldapManager;

    @Parameter(name = "query", type = CommandType.STRING, entityType = LdapUserResponse.class, required = true, description = "query to search using")
    private String query;

    public LdapUserSearchCmd() {
        super();
    }

    public LdapUserSearchCmd(final LdapManager ldapManager) {
        super();
        _ldapManager = ldapManager;
    }

    private List<LdapUserResponse> createLdapUserResponse(final List<LdapUser> users) {
        final List<LdapUserResponse> ldapUserResponses = new ArrayList<LdapUserResponse>();
        if (users != null) {
            for (final LdapUser user : users) {
                final LdapUserResponse ldapUserResponse = _ldapManager.createLdapUserResponse(user);
                ldapUserResponse.setObjectName("LdapUser");
                ldapUserResponses.add(ldapUserResponse);
            }
        }
        return ldapUserResponses;
    }

    @Override
    public void execute() {
        final ListResponse<LdapUserResponse> response = new ListResponse<LdapUserResponse>();
        List<LdapUser> users = null;

        try {
            users = _ldapManager.searchUsers(query);
        } catch (final NoLdapUserMatchingQueryException e) {
            s_logger.debug(e.getMessage());
        }

        final List<LdapUserResponse> ldapUserResponses = createLdapUserResponse(users);

        response.setResponses(ldapUserResponses);
        response.setResponseName(getCommandName());
        setResponseObject(response);
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
