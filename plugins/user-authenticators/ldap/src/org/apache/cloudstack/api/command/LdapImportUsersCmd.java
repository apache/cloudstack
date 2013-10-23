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

import com.cloud.domain.Domain;
import com.cloud.exception.*;
import com.cloud.user.AccountService;
import com.cloud.user.DomainService;
import org.apache.cloudstack.api.*;
import org.apache.cloudstack.api.response.LdapUserResponse;
import org.apache.cloudstack.api.response.ListResponse;
import org.apache.cloudstack.ldap.LdapManager;
import org.apache.cloudstack.ldap.LdapUser;
import org.apache.cloudstack.ldap.NoLdapUserMatchingQueryException;
import org.apache.log4j.Logger;
import org.bouncycastle.util.encoders.Base64;

import javax.inject.Inject;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@APICommand(name = "importLdapUsers", description = "Import LDAP users", responseObject = LdapUserResponse.class, since = "4.3.0")
public class LdapImportUsersCmd extends BaseListCmd {

    public static final Logger s_logger = Logger.getLogger(LdapImportUsersCmd.class.getName());

    private static final String s_name = "ldapuserresponse";

    @Parameter(name = ApiConstants.TIMEZONE, type = CommandType.STRING,
	       description = "Specifies a timezone for this command. For more information on the timezone parameter, see Time Zone Format.")
    private String timezone;

    @Parameter(name = ApiConstants.ACCOUNT_TYPE, type = CommandType.SHORT, required = true,
	       description = "Type of the account.  Specify 0 for user, 1 for root admin, and 2 for domain admin")
    private Short accountType;

    @Parameter(name = ApiConstants.ACCOUNT_DETAILS, type = CommandType.MAP, description = "details for account used to store specific parameters")
    private Map<String, String> details;

    @Inject
    private LdapManager _ldapManager;

    public LdapImportUsersCmd() {
	super();
    }

    public LdapImportUsersCmd(final LdapManager ldapManager, final DomainService domainService, final AccountService accountService) {
	super();
	_ldapManager = ldapManager;
	_domainService = domainService;
	_accountService = accountService;
    }

    @Override
    public void execute() throws ResourceUnavailableException, InsufficientCapacityException, ServerApiException, ConcurrentOperationException, ResourceAllocationException,
	NetworkRuleConflictException {
	List<LdapUserResponse> ldapResponses = null;
	final ListResponse<LdapUserResponse> response = new ListResponse<LdapUserResponse>();
	try {
	    final List<LdapUser> users = _ldapManager.getUsers();
	    for (LdapUser user : users) {
		Domain domain = _domainService.getDomainByName(user.getDomain(), Domain.ROOT_DOMAIN);

		if (domain == null) {
		    domain = _domainService.createDomain(user.getDomain(), Domain.ROOT_DOMAIN, user.getDomain(), UUID.randomUUID().toString());
		}
		_accountService.createUserAccount(user.getUsername(), generatePassword(), user.getFirstname(), user.getLastname(), user.getEmail(), timezone, user.getUsername(),
						  accountType, domain.getId(), domain.getNetworkDomain(), details, UUID.randomUUID().toString(), UUID.randomUUID().toString());
	    }
	    ldapResponses = createLdapUserResponse(users);
	} catch (final NoLdapUserMatchingQueryException ex) {
	    ldapResponses = new ArrayList<LdapUserResponse>();
	} finally {
	    response.setResponses(ldapResponses);
	    response.setResponseName(getCommandName());
	    setResponseObject(response);
	}
    }

    private List<LdapUserResponse> createLdapUserResponse(List<LdapUser> users) {
	final List<LdapUserResponse> ldapResponses = new ArrayList<LdapUserResponse>();
	for (final LdapUser user : users) {
	    final LdapUserResponse ldapResponse = _ldapManager.createLdapUserResponse(user);
	    ldapResponse.setObjectName("LdapUser");
	    ldapResponses.add(ldapResponse);
	}
	return ldapResponses;
    }

    @Override
    public String getCommandName() {
	return s_name;
    }

    private String generatePassword() throws ServerApiException {
	try {
	    final SecureRandom randomGen = SecureRandom.getInstance("SHA1PRNG");
	    final byte bytes[] = new byte[20];
	    randomGen.nextBytes(bytes);
	    return Base64.encode(bytes).toString();
	} catch (final NoSuchAlgorithmException e) {
	    throw new ServerApiException(ApiErrorCode.INTERNAL_ERROR, "Failed to generate random password");
	}
    }
}
