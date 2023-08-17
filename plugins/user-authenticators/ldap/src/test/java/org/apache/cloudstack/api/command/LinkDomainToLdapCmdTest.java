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

import com.cloud.user.Account;
import com.cloud.user.AccountService;
import com.cloud.user.User;
import com.cloud.user.UserAccountVO;
import org.apache.cloudstack.acl.RoleType;
import org.apache.cloudstack.api.response.LinkDomainToLdapResponse;
import org.apache.cloudstack.ldap.LdapManager;
import org.apache.cloudstack.ldap.LdapUser;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import static org.junit.Assert.assertEquals;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
import static org.mockito.Matchers.isNull;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class LinkDomainToLdapCmdTest implements LdapConfigurationChanger
{
    @Mock
    LdapManager ldapManager;
    @Mock
    AccountService accountService;

    LinkDomainToLdapCmd linkDomainToLdapCmd;

    @Before
    public void setUp() throws NoSuchFieldException, IllegalAccessException {
        linkDomainToLdapCmd = new LinkDomainToLdapCmd();
        setHiddenField(linkDomainToLdapCmd, "_ldapManager", ldapManager);
        setHiddenField(linkDomainToLdapCmd, "_accountService", accountService);
    }

    @After
    public void tearDown() {
    }

    @Test
    public void execute() throws Exception {
//      test with valid params and with admin who doesn't exist in cloudstack
        Long domainId = 1L;
        String type = "GROUP";
        String ldapDomain = "CN=test,DC=ccp,DC=Citrix,DC=com";
        Account.Type accountType = Account.Type.DOMAIN_ADMIN;
        String username = "admin";
        long accountId = 24;
        setHiddenField(linkDomainToLdapCmd, "ldapDomain", ldapDomain);
        setHiddenField(linkDomainToLdapCmd, "admin", username);
        setHiddenField(linkDomainToLdapCmd, "type", type);
        setHiddenField(linkDomainToLdapCmd, "domainId", domainId);
        setHiddenField(linkDomainToLdapCmd, "accountType", accountType.ordinal());

        LinkDomainToLdapResponse response = new LinkDomainToLdapResponse(domainId.toString(), type, ldapDomain, accountType.ordinal());
        when(ldapManager.linkDomainToLdap(linkDomainToLdapCmd)).thenReturn(response);
        when(ldapManager.getUser(username, type, ldapDomain, 1L)).thenReturn(new LdapUser(username, "admin@ccp.citrix.com", "Admin", "Admin", ldapDomain, "ccp", false, null));

        when(accountService.getActiveAccountByName(username, domainId)).thenReturn(null);
        UserAccountVO userAccount =  new UserAccountVO();
        userAccount.setAccountId(24);
        when(accountService.createUserAccount(eq(username), eq(""), eq("Admin"), eq("Admin"), eq("admin@ccp.citrix.com"), isNull(String.class),
                eq(username), eq(Account.Type.DOMAIN_ADMIN), eq(RoleType.DomainAdmin.getId()), eq(domainId), isNull(String.class),
                (java.util.Map<String,String>)isNull(), anyString(), anyString(), eq(User.Source.LDAP))).thenReturn(userAccount);


        linkDomainToLdapCmd.execute();
        LinkDomainToLdapResponse result = (LinkDomainToLdapResponse)linkDomainToLdapCmd.getResponseObject();
        assertEquals("objectName", "LinkDomainToLdap", result.getObjectName());
        assertEquals("commandName", linkDomainToLdapCmd.getCommandName(), result.getResponseName());
        assertEquals("domainId", domainId.toString(), result.getDomainId());
        assertEquals("type", type, result.getType());
        assertEquals("name", ldapDomain, result.getLdapDomain());
        assertEquals("accountId", String.valueOf(accountId), result.getAdminId());
    }

}
