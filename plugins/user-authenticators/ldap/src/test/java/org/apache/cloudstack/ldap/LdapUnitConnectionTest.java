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
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.runners.MockitoJUnitRunner;

import com.btmatthews.ldapunit.DirectoryTester;
import com.btmatthews.ldapunit.DirectoryServerConfiguration;
import com.btmatthews.ldapunit.DirectoryServerRule;

@RunWith(MockitoJUnitRunner.class)
@DirectoryServerConfiguration(ldifFiles = {LdapUnitConnectionTest.LDIF_FILE_NAME},
        baseDN = LdapUnitConnectionTest.DOMAIN_DSN,
        port = LdapUnitConnectionTest.PORT,
        authDN = LdapUnitConnectionTest.BIND_DN,
authPassword = LdapUnitConnectionTest.SECRET)
public class LdapUnitConnectionTest {
    static final String LDIF_FILE_NAME = "ldapunit.ldif";
    static final String DOMAIN_DSN = "dc=am,dc=echt,dc=net";
    static final String BIND_DN = "uid=admin,ou=cloudstack";
    static final String SECRET = "secretzz";
    static final int PORT =11389;

    @Rule
    public DirectoryServerRule directoryServerRule = new DirectoryServerRule();

    private DirectoryTester directoryTester;

    @Before
    public void setUp() {
        directoryTester = new DirectoryTester("localhost", PORT, BIND_DN, SECRET);
    }

    @After
    public void tearDown() {
        directoryTester.disconnect();
    }

    @Test
    public void testLdapInteface() throws Exception {
        directoryTester.assertDNExists("dc=am,dc=echt,dc=net");
    }
}
