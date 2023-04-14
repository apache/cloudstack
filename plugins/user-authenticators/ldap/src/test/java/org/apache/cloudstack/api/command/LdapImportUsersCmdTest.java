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
import com.cloud.domain.DomainVO;
import com.cloud.user.Account;
import com.cloud.user.AccountService;
import com.cloud.user.DomainService;
import org.apache.cloudstack.acl.RoleService;
import org.apache.cloudstack.api.response.ListResponse;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.apache.cloudstack.api.response.LdapUserResponse;
import org.apache.cloudstack.ldap.LdapManager;
import org.apache.cloudstack.ldap.LdapUser;
import org.mockito.junit.MockitoJUnitRunner;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static junit.framework.TestCase.assertEquals;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
import static org.powermock.api.mockito.PowerMockito.spy;
import static org.powermock.api.mockito.PowerMockito.when;

@RunWith(MockitoJUnitRunner.class)
public class LdapImportUsersCmdTest implements LdapConfigurationChanger {
    @Mock
    LdapManager ldapManager;
    @Mock
    AccountService accountService;
    @Mock
    DomainService domainService;
    @Mock
    RoleService roleService;

    LdapImportUsersCmd ldapImportUsersCmd;

    @Before
    public void setUp() throws NoSuchFieldException, IllegalAccessException {
        ldapImportUsersCmd = spy(new LdapImportUsersCmd(ldapManager, domainService, accountService));
        ldapImportUsersCmd.roleService = roleService;
        setHiddenField(ldapImportUsersCmd, "accountType", Account.Type.DOMAIN_ADMIN.ordinal());
    }

    @Test
    public void successfulResponseFromExecute() throws Exception {
        List<LdapUser> users = new ArrayList();
        users.add(new LdapUser("rmurphy", "rmurphy@test.com", "Ryan", "Murphy", "cn=rmurphy,ou=engineering,dc=cloudstack,dc=org", "engineering", false, null));
        users.add(new LdapUser("bob", "bob@test.com", "Robert", "Young", "cn=bob,ou=engineering,dc=cloudstack,dc=org", "engineering", false, null));
        when(ldapManager.getUsers(null)).thenReturn(users);
        LdapUserResponse response1 = new LdapUserResponse("rmurphy", "rmurphy@test.com", "Ryan", "Murphy", "cn=rmurphy,ou=engineering,dc=cloudstack,dc=org", "engineering");
        LdapUserResponse response2 = new LdapUserResponse("bob", "bob@test.com", "Robert", "Young", "cn=bob,ou=engineering,dc=cloudstack,dc=org", "engineering");
        when(ldapManager.createLdapUserResponse(any(LdapUser.class))).thenReturn(response1).thenReturn(response2);


        Domain domain = new DomainVO("engineering", 1L, 1L, "engineering", UUID.randomUUID().toString());
        when(domainService.getDomainByName("engineering", 1L)).thenReturn(null, domain);
        when(domainService.createDomain(eq("engineering"), eq(1L), eq("engineering"), anyString())).thenReturn(domain);

        ldapImportUsersCmd.execute();
        ListResponse<LdapUserResponse> resp = (ListResponse<LdapUserResponse>)ldapImportUsersCmd.getResponseObject();
        assertEquals(" when LdapListUsersCmd is executed, a list of size 2 should be returned", 2, resp.getResponses().size());
    }
}
