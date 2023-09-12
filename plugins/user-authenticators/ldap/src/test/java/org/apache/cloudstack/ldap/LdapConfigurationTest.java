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

import org.apache.cloudstack.ldap.dao.LdapConfigurationDao;
import org.apache.cloudstack.ldap.dao.LdapConfigurationDaoImpl;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.runners.MockitoJUnitRunner;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

@RunWith(MockitoJUnitRunner.class)
public class LdapConfigurationTest {

    private final LdapTestConfigTool ldapTestConfigTool = new LdapTestConfigTool();
    LdapConfigurationDao ldapConfigurationDao;
    LdapConfiguration ldapConfiguration;

    private void overrideConfigValue(LdapConfiguration ldapConfiguration, final String configKeyName, final Object o) throws IllegalAccessException, NoSuchFieldException
    {
        ldapTestConfigTool.overrideConfigValue(ldapConfiguration, configKeyName, o);
    }

    @Before public void init() throws Exception {
        ldapConfigurationDao = new LdapConfigurationDaoImpl();
        ldapConfiguration = new LdapConfiguration(ldapConfigurationDao);
        ;
    }

    @Test public void getAuthenticationReturnsSimple() throws Exception {
        ldapTestConfigTool.overrideConfigValue(ldapConfiguration, "ldapBindPrincipal", "cn=bla");
        ldapTestConfigTool.overrideConfigValue(ldapConfiguration, "ldapBindPassword", "pw");
        String authentication = ldapConfiguration.getAuthentication(null);
        assertEquals("authentication should be set to simple", "simple", authentication);
    }

    @Test public void getBaseDnReturnsABaseDn() throws Exception {
        ldapTestConfigTool.overrideConfigValue(ldapConfiguration, "ldapBaseDn", "dc=cloudstack,dc=org");
        String baseDn = ldapConfiguration.getBaseDn(null);
        assertEquals("The set baseDn should be returned", "dc=cloudstack,dc=org", baseDn);
    }

    @Test public void getGroupUniqueMemberAttribute() throws Exception {
        String[] groupNames = {"bla", "uniquemember", "memberuid", "", null};
        for (String groupObject : groupNames) {
            ldapTestConfigTool.overrideConfigValue(ldapConfiguration, "ldapGroupUniqueMemberAttribute", groupObject);
            String expectedResult = null;
            if (groupObject == null) {
                expectedResult = "uniquemember";
            } else {
                expectedResult = groupObject;
            }
            ;
            String result = ldapConfiguration.getGroupUniqueMemberAttribute(null);
            assertEquals("testing for " + groupObject, expectedResult, result);
        }
    }

    @Test public void getSSLStatusCanBeTrue() throws Exception {
//        given: "We have a ConfigDao with values for truststore and truststore password set"
        ldapTestConfigTool.overrideConfigValue(ldapConfiguration, "ldapTrustStore", "/tmp/ldap.ts");
        ldapTestConfigTool.overrideConfigValue(ldapConfiguration, "ldapTrustStorePassword", "password");

        assertTrue("A request is made to get the status of SSL should result in true", ldapConfiguration.getSSLStatus(null));
    }

    @Test public void getSearchGroupPrincipleReturnsSuccessfully() throws Exception {
        // We have a ConfigDao with a value for ldap.search.group.principle and an LdapConfiguration
        ldapTestConfigTool.overrideConfigValue(ldapConfiguration, "ldapSearchGroupPrinciple", "cn=cloudstack,cn=users,dc=cloudstack,dc=org");
        String result = ldapConfiguration.getSearchGroupPrinciple(null);

        assertEquals("The result holds the same value configDao did", "cn=cloudstack,cn=users,dc=cloudstack,dc=org", result);
    }

    @Test public void getTrustStorePasswordResopnds() throws Exception {
        // We have a ConfigDao with a value for truststore password
        ldapTestConfigTool.overrideConfigValue(ldapConfiguration, "ldapTrustStorePassword", "password");

        String result = ldapConfiguration.getTrustStorePassword(null);

        assertEquals("The result is password", "password", result);
    }

    @Test public void getGroupObject() throws Exception {
        String[] groupNames = {"bla", "groupOfUniqueNames", "groupOfNames", "", null};
        for (String groupObject : groupNames) {
            ldapTestConfigTool.overrideConfigValue(ldapConfiguration, "ldapGroupObject", groupObject);
            String expectedResult = null;
            if (groupObject == null) {
                expectedResult = "groupOfUniqueNames";
            } else {
                expectedResult = groupObject;
            }
            ;
            String result = ldapConfiguration.getGroupObject(null);
            assertEquals("testing for " + groupObject, expectedResult, result);
        }
    }

    @Test public void getNullLdapProvider() {
        assertEquals(LdapUserManager.Provider.OPENLDAP, ldapConfiguration.getLdapProvider(null));
    }
}
