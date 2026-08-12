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

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.mockito.junit.MockitoJUnitRunner;

import javax.naming.directory.SearchControls;
import javax.naming.ldap.LdapContext;

import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class ADLdapUserManagerImplTest {

    ADLdapUserManagerImpl adLdapUserManager;

    MockedStatic<LdapConfiguration> LdapConfiguration;
    @Mock
    LdapConfiguration ldapConfigurationMock;

    @Before
    public void init() {
        LdapConfiguration = Mockito.mockStatic(LdapConfiguration.class,Mockito.CALLS_REAL_METHODS);
        adLdapUserManager = new ADLdapUserManagerImpl();
        adLdapUserManager._ldapConfiguration = ldapConfigurationMock;
    }

    @After
    public void afterEach() {
        LdapConfiguration.close();
    }
    @Test
    public void testGenerateADSearchFilterWithNestedGroupsEnabled() {
        when(adLdapUserManager._ldapConfiguration.getUserObject(anyLong())).thenReturn("user");
        when(adLdapUserManager._ldapConfiguration.getCommonNameAttribute()).thenReturn("CN");
        when(adLdapUserManager._ldapConfiguration.getBaseDn(any())).thenReturn("DC=cloud,DC=citrix,DC=com");
        when(adLdapUserManager._ldapConfiguration.isNestedGroupsEnabled(anyLong())).thenReturn(true);

        String [] groups = {"dev", "dev-hyd"};
        for (String group: groups) {
            String result = adLdapUserManager.generateADGroupSearchFilter(group, 1L);
            assertEquals(("(&(&(objectCategory=person)(objectClass=user))(memberOf:1.2.840.113556.1.4.1941:=CN=" + group + ",DC=cloud,DC=citrix,DC=com))"), result);
        }
    }

    @Test
    public void testGenerateADSearchFilterWithNestedGroupsDisabled() {
        when(adLdapUserManager._ldapConfiguration.getUserObject(anyLong())).thenReturn("user");
        when(adLdapUserManager._ldapConfiguration.getCommonNameAttribute()).thenReturn("CN");
        when(adLdapUserManager._ldapConfiguration.getBaseDn(anyLong())).thenReturn("DC=cloud,DC=citrix,DC=com");
        when(adLdapUserManager._ldapConfiguration.isNestedGroupsEnabled(anyLong())).thenReturn(false);

        String [] groups = {"dev", "dev-hyd"};
        for (String group: groups) {
            String result = adLdapUserManager.generateADGroupSearchFilter(group, 1L);
            assertEquals(("(&(&(objectCategory=person)(objectClass=user))(memberOf=CN=" + group + ",DC=cloud,DC=citrix,DC=com))"), result);
        }
    }

    @Mock
    LdapContext ldapContext;

    @Test(expected = IllegalArgumentException.class)
    public void testGetUsersInGroupUsingNullGroup() throws Exception {
        String[] returnAttributes = {"username", "firstname", "lastname", "email"};
        lenient().when(adLdapUserManager._ldapConfiguration.getScope()).thenReturn(SearchControls.SUBTREE_SCOPE);
        lenient().when(adLdapUserManager._ldapConfiguration.getReturnAttributes(null)).thenReturn(returnAttributes);
        lenient().when(adLdapUserManager._ldapConfiguration.getBaseDn(any())).thenReturn(null).thenReturn(null).thenReturn("DC=cloud,DC=citrix,DC=com");

        LdapContext context = ldapContext;
        String [] groups = {null, "group", null};
        for (String group: groups) {
            adLdapUserManager.getUsersInGroup(group, context,null);
        }
    }
}
