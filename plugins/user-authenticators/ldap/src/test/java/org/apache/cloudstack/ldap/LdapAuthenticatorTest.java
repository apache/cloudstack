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
package org.apache.cloudstack.ldap;


import com.cloud.user.AccountManager;
import com.cloud.user.AccountVO;
import com.cloud.user.Account;
import com.cloud.user.User;
import com.cloud.user.UserAccount;
import com.cloud.user.UserAccountVO;
import com.cloud.user.dao.UserAccountDao;
import com.cloud.utils.Pair;
import org.apache.cloudstack.auth.UserAuthenticator;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.anyLong;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.when;


@RunWith(MockitoJUnitRunner.class)
public class LdapAuthenticatorTest {

    @Mock
    LdapManager ldapManager;
    @Mock
    UserAccountDao userAccountDao;
    @Mock
    AccountManager accountManager;
    @Mock
    UserAccount user = new UserAccountVO();

    @InjectMocks
    LdapAuthenticator ldapAuthenticator = new LdapAuthenticator();
    private String username  = "bbanner";
    private String principal = "cd=bbanner";
    private String hardcoded = "password";
    private Long domainId = 1L;

    @Before
    public void setUp() throws Exception {
    }

    @Test
    public void authenticateAsNativeUser() throws Exception {
        final UserAccountVO user = new UserAccountVO();
        user.setSource(User.Source.NATIVE);

        lenient().when(userAccountDao.getUserAccount(username, domainId)).thenReturn(user);
        Pair<Boolean, UserAuthenticator.ActionOnFailedAuthentication> rc;
        rc = ldapAuthenticator.authenticate(username, "password", domainId, (Map<String, Object[]>)null);
        assertFalse("authentication succeeded when it should have failed", rc.first());
        assertEquals("We should not have tried to authenticate", null,rc.second());
    }

    @Test
    public void authenticateWithoutAccount() throws Exception {
        LdapUser ldapUser = new LdapUser(username,"a@b","b","banner",principal,"",false,null);
        Pair<Boolean, UserAuthenticator.ActionOnFailedAuthentication> rc;
        when(ldapManager.getUser(username, domainId)).thenReturn(ldapUser);
        rc = ldapAuthenticator.authenticate(username, "password", domainId, user);
        assertFalse("authentication succeeded when it should have failed", rc.first());
        assertEquals("", UserAuthenticator.ActionOnFailedAuthentication.INCREMENT_INCORRECT_LOGIN_ATTEMPT_COUNT,rc.second());
    }

    @Test
    public void authenticateFailingOnSyncedAccount() throws Exception {
        Pair<Boolean, UserAuthenticator.ActionOnFailedAuthentication> rc;

        List<String> memberships = new ArrayList<>();
        memberships.add("g1");
        List<String> mappedGroups = new ArrayList<>();
        mappedGroups.add("g1");
        mappedGroups.add("g2");

        LdapUser ldapUser = new LdapUser(username,"a@b","b","banner",principal,"",false,null);
        LdapUser userSpy = spy(ldapUser);
        when(userSpy.getMemberships()).thenReturn(memberships);

        List<LdapTrustMapVO> maps = new ArrayList<>();
        LdapAuthenticator auth = spy(ldapAuthenticator);
        when(auth.getMappedGroups(maps)).thenReturn(mappedGroups);

        LdapTrustMapVO trustMap = new LdapTrustMapVO(domainId, LdapManager.LinkType.GROUP, "cn=name", Account.Type.DOMAIN_ADMIN, 1l);

        AccountVO account = new AccountVO("accountName" , domainId, "domain.net", Account.Type.DOMAIN_ADMIN, "final String uuid");
        when(accountManager.getAccount(anyLong())).thenReturn(account);
        when(ldapManager.getUser(username, domainId)).thenReturn(userSpy);
        when(ldapManager.getLinkedLdapGroup(domainId, "g1")).thenReturn(trustMap);
        rc = auth.authenticate(username, "password", domainId, user, maps);
        assertFalse("authentication succeeded when it should have failed", rc.first());
        assertEquals("", UserAuthenticator.ActionOnFailedAuthentication.INCREMENT_INCORRECT_LOGIN_ATTEMPT_COUNT,rc.second());
    }

    @Test
    public void authenticate() throws Exception {
        LdapUser ldapUser = new LdapUser(username, "a@b", "b", "banner", principal, "", false, null);
        when(ldapManager.getUser(username, domainId)).thenReturn(ldapUser);
        when(ldapManager.canAuthenticate(principal, hardcoded, domainId)).thenReturn(true);
        Pair<Boolean, UserAuthenticator.ActionOnFailedAuthentication> rc = ldapAuthenticator.authenticate(username, hardcoded, domainId, user);
        assertTrue("authentication failed when it should have succeeded", rc.first());
        assertNull(rc.second());
    }
}
