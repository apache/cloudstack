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

import com.google.common.collect.Iterators;
import com.unboundid.ldap.sdk.LDAPConnection;
import com.unboundid.ldap.sdk.LDAPInterface;
import com.unboundid.ldap.sdk.SearchResult;
import com.unboundid.ldap.sdk.SearchScope;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.runners.MockitoJUnitRunner;
import org.zapodot.junit.ldap.EmbeddedLdapRule;
import org.zapodot.junit.ldap.EmbeddedLdapRuleBuilder;

import javax.naming.Context;
import javax.naming.NamingEnumeration;
import javax.naming.directory.DirContext;
import javax.naming.directory.SearchControls;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

@RunWith(MockitoJUnitRunner.class)
public class LdapUnboundidZapdotConnectionTest {
    private static final String DOMAIN_DSN;

    static {
        DOMAIN_DSN = "dc=cloudstack,dc=org";
    }

    @Rule
    public EmbeddedLdapRule embeddedLdapRule = EmbeddedLdapRuleBuilder
            .newInstance()
            .usingDomainDsn(DOMAIN_DSN)
            .importingLdifs("unboundid.ldif")
            .build();

    @Test
    public void testLdapInteface() throws Exception {
        // Test using the UnboundID LDAP SDK directly
        final LDAPInterface ldapConnection = embeddedLdapRule.ldapConnection();
        final SearchResult searchResult = ldapConnection.search(DOMAIN_DSN, SearchScope.SUB, "(objectClass=person)");
        assertEquals(24, searchResult.getEntryCount());
    }

    @Test
    public void testUnsharedLdapConnection() throws Exception {
        // Test using the UnboundID LDAP SDK directly by using the UnboundID LDAPConnection type
        final LDAPConnection ldapConnection = embeddedLdapRule.unsharedLdapConnection();
        final SearchResult searchResult = ldapConnection.search(DOMAIN_DSN, SearchScope.SUB, "(objectClass=person)");
        assertEquals(24, searchResult.getEntryCount());
    }

    @Test
    public void testDirContext() throws Exception {

        // Test using the good ol' JDNI-LDAP integration
        final DirContext dirContext = embeddedLdapRule.dirContext();
        final SearchControls searchControls = new SearchControls();
        searchControls.setSearchScope(SearchControls.SUBTREE_SCOPE);
        final NamingEnumeration<javax.naming.directory.SearchResult> resultNamingEnumeration =
                dirContext.search(DOMAIN_DSN, "(objectClass=person)", searchControls);
        assertEquals(24, Iterators.size(Iterators.forEnumeration(resultNamingEnumeration)));
    }
    @Test
    public void testContext() throws Exception {

        // Another test using the good ol' JDNI-LDAP integration, this time with the Context interface
        final Context context = embeddedLdapRule.context();
        final Object user = context.lookup("cn=Cammy Petri,dc=cloudstack,dc=org");
        assertNotNull(user);
    }
}
