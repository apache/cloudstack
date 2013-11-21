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
import org.apache.cloudstack.api.ServerApiException;
import org.apache.cloudstack.api.command.admin.user.ListUsersCmd;
import org.apache.cloudstack.api.response.LdapUserResponse;
import org.apache.cloudstack.api.response.ListResponse;
import org.apache.cloudstack.api.response.UserResponse;
import org.apache.cloudstack.ldap.LdapManager;
import org.apache.cloudstack.ldap.LdapUser;
import org.apache.cloudstack.ldap.NoLdapUserMatchingQueryException;
import org.apache.cloudstack.query.QueryService;

import com.cloud.user.Account;

@APICommand(name = "listLdapUsers", responseObject = LdapUserResponse.class, description = "Lists all LDAP Users", since = "4.2.0")
public class LdapListUsersCmd extends BaseListCmd {

    public static final Logger s_logger = Logger.getLogger(LdapListUsersCmd.class.getName());
    private static final String s_name = "ldapuserresponse";
    @Inject
    private LdapManager _ldapManager;

    @Inject
    private QueryService _queryService;

    @Parameter(name = "listtype",
               type = CommandType.STRING,
               required = false,
               description = "Determines whether all ldap users are returned or just non-cloudstack users")
    private String listType;

    public LdapListUsersCmd() {
        super();
    }

    public LdapListUsersCmd(final LdapManager ldapManager, final QueryService queryService) {
        super();
        _ldapManager = ldapManager;
        _queryService = queryService;
    }

    private List<LdapUserResponse> createLdapUserResponse(final List<LdapUser> users) {
        final List<LdapUserResponse> ldapResponses = new ArrayList<LdapUserResponse>();
        for (final LdapUser user : users) {
            if (getListType().equals("all") || !isACloudstackUser(user)) {
                final LdapUserResponse ldapResponse = _ldapManager.createLdapUserResponse(user);
                ldapResponse.setObjectName("LdapUser");
                ldapResponses.add(ldapResponse);
            }
        }
        return ldapResponses;
    }

    @Override
    public void execute() throws ServerApiException {
        List<LdapUserResponse> ldapResponses = null;
        final ListResponse<LdapUserResponse> response = new ListResponse<LdapUserResponse>();
        try {
            final List<LdapUser> users = _ldapManager.getUsers();
            ldapResponses = createLdapUserResponse(users);
        } catch (final NoLdapUserMatchingQueryException ex) {
            ldapResponses = new ArrayList<LdapUserResponse>();
        } finally {
            response.setResponses(ldapResponses);
            response.setResponseName(getCommandName());
            setResponseObject(response);
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

    private String getListType() {
        return listType == null ? "all" : listType;
    }

    private boolean isACloudstackUser(final LdapUser ldapUser) {
        final ListResponse<UserResponse> response = _queryService.searchForUsers(new ListUsersCmd());
        final List<UserResponse> cloudstackUsers = response.getResponses();
        if (cloudstackUsers != null && cloudstackUsers.size() != 0) {
            for (final UserResponse cloudstackUser : response.getResponses()) {
                if (ldapUser.getUsername().equals(cloudstackUser.getUsername())) {
                    return true;
                }
            }
        }
        return false;
    }
}