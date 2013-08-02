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

import org.apache.cloudstack.api.APICommand;
import org.apache.cloudstack.api.BaseListCmd;
import org.apache.cloudstack.api.ServerApiException;
import org.apache.cloudstack.api.response.LdapUserResponse;
import org.apache.cloudstack.api.response.ListResponse;
import org.apache.cloudstack.ldap.LdapManager;
import org.apache.cloudstack.ldap.LdapUser;
import org.apache.cloudstack.ldap.NoLdapUserMatchingQueryException;
import org.apache.log4j.Logger;

import com.cloud.user.Account;

@APICommand(name = "listAllLdapUsers", responseObject = LdapUserResponse.class, description = "Lists all LDAP Users", since = "4.2.0")
public class LdapListAllUsersCmd extends BaseListCmd {

	public static final Logger s_logger = Logger
			.getLogger(LdapListAllUsersCmd.class.getName());
	private static final String s_name = "ldapuserresponse";
	@Inject
	private LdapManager _ldapManager;

	public LdapListAllUsersCmd() {
		super();
	}

	public LdapListAllUsersCmd(final LdapManager ldapManager) {
		super();
		_ldapManager = ldapManager;
	}

	private List<LdapUserResponse> createLdapUserResponse(List<LdapUser> users) {
		final List<LdapUserResponse> ldapResponses = new ArrayList<LdapUserResponse>();
		for (final LdapUser user : users) {
			final LdapUserResponse ldapResponse = _ldapManager
					.createLdapUserResponse(user);
			ldapResponse.setObjectName("LdapUser");
			ldapResponses.add(ldapResponse);
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
}