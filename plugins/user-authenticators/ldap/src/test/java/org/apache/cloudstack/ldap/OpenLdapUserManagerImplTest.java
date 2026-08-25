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

import com.cloud.user.Account;
import org.apache.cloudstack.ldap.dao.LdapTrustMapDao;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.mockito.junit.MockitoJUnitRunner;

import javax.naming.NamingEnumeration;
import javax.naming.NamingException;
import javax.naming.directory.SearchControls;
import javax.naming.directory.SearchResult;
import javax.naming.ldap.Control;
import javax.naming.ldap.LdapContext;
import javax.naming.ldap.PagedResultsResponseControl;

import java.util.Collections;
import java.util.List;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Regression tests: creating an ldap account must not fail just because another
 * account is already linked to an ldap group in the domain; browsing/importing
 * still honours that group scope.
 */
@RunWith(MockitoJUnitRunner.class)
public class OpenLdapUserManagerImplTest {

    private static final Long DOMAIN_ID = 1L;
    private static final String LINKED_GROUP = "cn=test admins,ou=groups,dc=my,dc=domain,dc=com";

    private OpenLdapUserManagerImpl openLdapUserManager;

    private MockedStatic<LdapConfiguration> ldapConfigurationMockedStatic;

    @Mock
    private LdapConfiguration ldapConfigurationMock;

    @Mock
    private LdapTrustMapDao ldapTrustMapDaoMock;

    @Mock
    private LdapContext ldapContextMock;

    @Before
    public void setup() throws Exception {
        // getUserMemberOfAttribute is static, unlike its LdapConfiguration siblings; mock statically.
        ldapConfigurationMockedStatic = Mockito.mockStatic(LdapConfiguration.class, Mockito.CALLS_REAL_METHODS);
        when(LdapConfiguration.getUserMemberOfAttribute(any())).thenReturn("memberOf");

        openLdapUserManager = new OpenLdapUserManagerImpl(ldapConfigurationMock);
        openLdapUserManager._ldapTrustMapDao = ldapTrustMapDaoMock;

        when(ldapConfigurationMock.getScope()).thenReturn(SearchControls.SUBTREE_SCOPE);
        when(ldapConfigurationMock.getReturnAttributes(any())).thenReturn(new String[]{"uid"});
        when(ldapConfigurationMock.getSearchGroupPrinciple(any())).thenReturn(null);
        when(ldapConfigurationMock.getBaseDn(any())).thenReturn("dc=my,dc=domain,dc=com");
        when(ldapConfigurationMock.getUsernameAttribute(any())).thenReturn("uid");
        when(ldapConfigurationMock.getUserObject(any())).thenReturn("inetOrgPerson");
        when(ldapConfigurationMock.getLdapPageSize(any())).thenReturn(1000);

        LdapTrustMapVO linkedGroup = new LdapTrustMapVO(DOMAIN_ID, LdapManager.LinkType.GROUP, LINKED_GROUP, Account.Type.NORMAL, 5L);
        when(ldapTrustMapDaoMock.searchByDomainId(anyLong())).thenReturn(Collections.singletonList(linkedGroup));

        NamingEnumeration<SearchResult> noResults = mock(NamingEnumeration.class);
        when(noResults.hasMoreElements()).thenReturn(false);
        when(ldapContextMock.search(any(String.class), any(String.class), any(SearchControls.class))).thenReturn(noResults);
        when(ldapContextMock.getResponseControls()).thenReturn(null);
    }

    @After
    public void tearDown() {
        ldapConfigurationMockedStatic.close();
    }

    @Test
    public void getUserDoesNotRestrictToAlreadyLinkedGroups() throws Exception {
        try {
            openLdapUserManager.getUser("test_user", ldapContextMock, DOMAIN_ID);
        } catch (NamingException expected) {
            // no results stubbed; only the filter sent matters here
        }

        String filter = capturedSearchFilter();
        assertFalse("a lookup for one specific username must not be scoped to already-linked groups: " + filter,
                filter.contains("memberOf=" + LINKED_GROUP));
    }

    @Test
    public void getUsersStillRestrictsToAlreadyLinkedGroups() throws Exception {
        openLdapUserManager.getUsers("test_user", ldapContextMock, DOMAIN_ID);

        String filter = capturedSearchFilter();
        assertTrue("browsing/importing users should still be scoped to already-linked groups: " + filter,
                filter.contains("memberOf=" + LINKED_GROUP));
    }

    @Test
    public void getUsersDoesNotAddGroupScopeWhenDomainHasNoLinkedGroups() throws Exception {
        when(ldapTrustMapDaoMock.searchByDomainId(anyLong())).thenReturn(Collections.emptyList());

        openLdapUserManager.getUsers("test_user", ldapContextMock, DOMAIN_ID);

        String filter = capturedSearchFilter();
        assertFalse("no linked groups in the domain means no group scoping should be applied: " + filter,
                filter.contains("memberOf="));
    }

    @Test
    public void searchStopsAfterOnePageWhenNoPagedResultsControlIsReturned() throws Exception {
        Control unrelatedControl = mock(Control.class);
        when(ldapContextMock.getResponseControls()).thenReturn(new Control[]{unrelatedControl});

        List<LdapUser> result = openLdapUserManager.searchUsers("test_user", ldapContextMock, DOMAIN_ID);

        assertTrue(result.isEmpty());
        verify(ldapContextMock, times(1)).search(any(String.class), any(String.class), any(SearchControls.class));
    }

    @Test
    public void searchFollowsCookieFromPagedResultsControlToNextPage() throws Exception {
        PagedResultsResponseControl pagedControl = mock(PagedResultsResponseControl.class);
        when(pagedControl.getCookie()).thenReturn(new byte[]{1, 2, 3});
        when(ldapContextMock.getResponseControls())
                .thenReturn(new Control[]{pagedControl})
                .thenReturn(null);

        List<LdapUser> result = openLdapUserManager.searchUsers("test_user", ldapContextMock, DOMAIN_ID);

        assertTrue(result.isEmpty());
        verify(ldapContextMock, times(2)).search(any(String.class), any(String.class), any(SearchControls.class));
    }

    private String capturedSearchFilter() throws Exception {
        ArgumentCaptor<String> filterCaptor = ArgumentCaptor.forClass(String.class);
        verify(ldapContextMock, atLeastOnce()).search(any(String.class), filterCaptor.capture(), any(SearchControls.class));
        return filterCaptor.getValue();
    }
}
