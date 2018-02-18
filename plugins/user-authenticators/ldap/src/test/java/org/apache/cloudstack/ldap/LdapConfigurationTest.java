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

import org.apache.cloudstack.framework.config.ConfigKey;
import org.apache.cloudstack.ldap.dao.LdapConfigurationDao;
import org.apache.cloudstack.ldap.dao.LdapConfigurationDaoImpl;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.runners.MockitoJUnitRunner;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

@RunWith(MockitoJUnitRunner.class)
public class LdapConfigurationTest {

    LdapConfigurationDao ldapConfigurationDao;
    LdapConfiguration ldapConfiguration;

    private void overrideConfigValue(final String configKeyName, final Object o) throws IllegalAccessException, NoSuchFieldException {
        Field configKey = LdapConfiguration.class.getDeclaredField(configKeyName);
        configKey.setAccessible(true);

        ConfigKey key = (ConfigKey)configKey.get(ldapConfiguration);

        Field modifiersField = Field.class.getDeclaredField("modifiers");
        modifiersField.setAccessible(true);
        modifiersField.setInt(configKey, configKey.getModifiers() & ~Modifier.FINAL);

        Field f = ConfigKey.class.getDeclaredField("_value");
        f.setAccessible(true);
        modifiersField.setInt(f, f.getModifiers() & ~Modifier.FINAL);
        f.set(key, o);

        Field dynamic = ConfigKey.class.getDeclaredField("_isDynamic");
        dynamic.setAccessible(true);
        modifiersField.setInt(dynamic, dynamic.getModifiers() & ~Modifier.FINAL);
        dynamic.setBoolean(key, false);
    }

    @Before
    public void init() throws Exception {
        ldapConfigurationDao =  new LdapConfigurationDaoImpl();
        ldapConfiguration = new LdapConfiguration(ldapConfigurationDao);;
    }

    @Test
    public void getAuthenticationReturnsSimple() throws Exception {
        overrideConfigValue("ldapBindPrincipal", "cn=bla");
        overrideConfigValue("ldapBindPassword", "pw");
        String authentication = ldapConfiguration.getAuthentication(null);
        assertEquals("authentication should be set to simple", "simple", authentication);
    }


    @Test
    public void getBaseDnReturnsABaseDn() throws Exception {
        overrideConfigValue("ldapBaseDn", "dc=cloudstack,dc=org");
        String baseDn = ldapConfiguration.getBaseDn(null);
        assertEquals("The set baseDn should be returned","dc=cloudstack,dc=org", baseDn);
    }

    @Test
    public void getGroupUniqueMemberAttribute() throws Exception {
        String [] groupNames = {"bla", "uniquemember", "memberuid", "", null};
        for (String groupObject: groupNames) {
            overrideConfigValue("ldapGroupUniqueMemberAttribute", groupObject);
            String expectedResult = null;
            if(groupObject == null) {
                expectedResult = "uniquemember";
            } else {
                expectedResult = groupObject;
            };
            String result = ldapConfiguration.getGroupUniqueMemberAttribute(null);
            assertEquals("testing for " + groupObject, expectedResult, result);
        }
    }

    @Test
    public void getSSLStatusCanBeTrue() throws Exception {
//        given: "We have a ConfigDao with values for truststore and truststore password set"
        overrideConfigValue("ldapTrustStore", "/tmp/ldap.ts");
        overrideConfigValue("ldapTrustStorePassword", "password");

        assertTrue("A request is made to get the status of SSL should result in true", ldapConfiguration.getSSLStatus());
    }
    @Test
    public void getSearchGroupPrincipleReturnsSuccessfully() throws Exception {
        // We have a ConfigDao with a value for ldap.search.group.principle and an LdapConfiguration
        overrideConfigValue("ldapSearchGroupPrinciple", "cn=cloudstack,cn=users,dc=cloudstack,dc=org");
        String result = ldapConfiguration.getSearchGroupPrinciple(null);

        assertEquals("The result holds the same value configDao did", "cn=cloudstack,cn=users,dc=cloudstack,dc=org",result);
    }

    @Test
    public void  getTrustStorePasswordResopnds() throws Exception {
        // We have a ConfigDao with a value for truststore password
        overrideConfigValue("ldapTrustStorePassword", "password");

        String result = ldapConfiguration.getTrustStorePassword();

        assertEquals("The result is password", "password", result);
    }


    @Test
    public void getGroupObject() throws Exception {
        String [] groupNames = {"bla", "groupOfUniqueNames", "groupOfNames", "", null};
        for (String groupObject: groupNames) {
            overrideConfigValue("ldapGroupObject", groupObject);
            String expectedResult = null;
            if(groupObject == null) {
                expectedResult = "groupOfUniqueNames";
            } else {
                expectedResult = groupObject;
            };
            String result = ldapConfiguration.getGroupObject(null);
            assertEquals("testing for " + groupObject, expectedResult, result);
        }
    }
}