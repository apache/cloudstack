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
import org.apache.cloudstack.acl.RoleService;
import org.apache.cloudstack.api.ServerApiException;
import org.apache.cloudstack.ldap.LdapManager;
import org.apache.cloudstack.ldap.LdapUser;
import org.apache.cloudstack.ldap.NoLdapUserMatchingQueryException;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import static org.junit.Assert.fail;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.ArgumentMatchers.nullable;
import static org.powermock.api.mockito.PowerMockito.spy;
import static org.powermock.api.mockito.PowerMockito.when;

@RunWith(MockitoJUnitRunner.class)
public class LdapCreateAccountCmdTest implements LdapConfigurationChanger {
    @Mock
    LdapManager ldapManager;
    @Mock
    AccountService accountService;
    @Mock
    RoleService roleService;

    LdapCreateAccountCmd ldapCreateAccountCmd;

    @Before
    public void setUp() throws NoSuchFieldException, IllegalAccessException {
        ldapCreateAccountCmd = spy(new LdapCreateAccountCmd(ldapManager, accountService));
        ldapCreateAccountCmd.roleService = roleService;
        setHiddenField(ldapCreateAccountCmd,"accountType", Account.Type.DOMAIN_ADMIN.ordinal());
    }

    @Test(expected = ServerApiException.class)
    public void failureToRetrieveLdapUser() throws Exception {
        // We have an LdapManager, AccountService and LdapCreateAccountCmd and LDAP user that doesn't exist
        when(ldapManager.getUser(nullable(String.class), isNull())).thenThrow(NoLdapUserMatchingQueryException.class);
        ldapCreateAccountCmd.execute();
        fail("An exception should have been thrown: " + ServerApiException.class);
    }

    @Test(expected = ServerApiException.class)
    public void failedCreationDueToANullResponseFromCloudstackAccountCreator() throws Exception {
        // We have an LdapManager, AccountService and LdapCreateAccountCmd
        LdapUser mrMurphy = new LdapUser("rmurphy", "rmurphy@cloudstack.org", "Ryan", "Murphy", "cn=rmurphy,ou=engineering,dc=cloudstack,dc=org", "engineering", false, null);
        when(ldapManager.getUser(nullable(String.class), isNull())).thenReturn(mrMurphy).thenReturn(mrMurphy);
        ldapCreateAccountCmd.execute();
        fail("An exception should have been thrown: " + ServerApiException.class);
    }
}
