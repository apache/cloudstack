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
import org.apache.cloudstack.api.BaseCmd;
import org.apache.cloudstack.api.response.LinkAccountToLdapResponse;
import org.apache.cloudstack.ldap.LdapManager;
import org.apache.cloudstack.ldap.LdapUser;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class LinkAccountToLdapCmdTest implements LdapConfigurationChanger {

    @Mock
    LdapManager ldapManager;
    @Mock
    AccountService accountService;

    LinkAccountToLdapCmd linkAccountToLdapCmd;

    @Before
    public void setUp() throws NoSuchFieldException, IllegalAccessException {
        linkAccountToLdapCmd = new LinkAccountToLdapCmd();
        setHiddenField(linkAccountToLdapCmd, "_ldapManager", ldapManager);
        setHiddenField(linkAccountToLdapCmd, "_accountService", accountService);
    }

    @Test
    public void execute() throws Exception {
        //      test with valid params and with admin who doesn't exist in cloudstack
        long domainId = 1;
        String type = "GROUP";
        String ldapDomain = "CN=test,DC=ccp,DC=Citrix,DC=com";
        Account.Type accountType = Account.Type.DOMAIN_ADMIN;
        String username = "admin";
        long accountId = 24;
        String accountName = "test";

        setHiddenField(linkAccountToLdapCmd, "ldapDomain", ldapDomain);
        setHiddenField(linkAccountToLdapCmd, "admin", username);
        setHiddenField(linkAccountToLdapCmd, "type", type);
        setHiddenField(linkAccountToLdapCmd, "domainId", domainId);
        setHiddenField(linkAccountToLdapCmd, "accountType", accountType.ordinal());
        setHiddenField(linkAccountToLdapCmd, "accountName", accountName);


        LinkAccountToLdapResponse response = new LinkAccountToLdapResponse(String.valueOf(domainId), type, ldapDomain, accountType.ordinal(), username, accountName);
        when(ldapManager.linkAccountToLdap(linkAccountToLdapCmd)).thenReturn(response);
        when(ldapManager.getUser(username, type, ldapDomain, 1L))
                .thenReturn(new LdapUser(username, "admin@ccp.citrix.com", "Admin", "Admin", ldapDomain, "ccp", false, null));

        when(accountService.getActiveAccountByName(username, domainId)).thenReturn(null);
        UserAccountVO userAccount =  new UserAccountVO();
        userAccount.setAccountId(24);
        when(accountService.createUserAccount(eq(username), eq(""), eq("Admin"), eq("Admin"), eq("admin@ccp.citrix.com"), isNull(String.class),
                eq(username), eq(Account.Type.DOMAIN_ADMIN), eq(RoleType.DomainAdmin.getId()), eq(domainId), isNull(String.class),
                (java.util.Map<String,String>)isNull(), anyString(), anyString(), eq(User.Source.LDAP))).thenReturn(userAccount);

        linkAccountToLdapCmd.execute();
        LinkAccountToLdapResponse result = (LinkAccountToLdapResponse)linkAccountToLdapCmd.getResponseObject();
        assertEquals("objectName", BaseCmd.getCommandNameByClass(LinkAccountToLdapCmd.class), result.getObjectName());
        assertEquals("commandName", linkAccountToLdapCmd.getCommandName(), result.getResponseName());
        assertEquals("domainId", String.valueOf(domainId), result.getDomainId());
        assertEquals("type", type, result.getType());
        assertEquals("name", ldapDomain, result.getLdapDomain());
        assertEquals("accountId", String.valueOf(accountId), result.getAdminId());
    }
}
