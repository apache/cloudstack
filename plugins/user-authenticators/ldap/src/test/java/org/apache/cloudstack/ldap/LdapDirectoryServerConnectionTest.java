/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.  The
 * ASF licenses this file to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance with the
 * License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.  See the
 * License for the specific language governing permissions and limitations
 * under the License.
 */
package org.apache.cloudstack.ldap;

import com.cloud.utils.Pair;
import org.apache.cloudstack.ldap.dao.LdapConfigurationDao;
import org.apache.directory.api.ldap.model.entry.DefaultEntry;
import org.apache.directory.api.ldap.model.entry.Entry;
import org.apache.directory.api.ldap.model.exception.LdapException;
import org.apache.directory.api.ldap.model.message.AddRequest;
import org.apache.directory.api.ldap.model.message.AddRequestImpl;
import org.apache.directory.api.ldap.model.message.AddResponse;
import org.apache.directory.ldap.client.api.LdapConnection;
import org.apache.directory.ldap.client.api.LdapNetworkConnection;
import org.apache.directory.server.core.api.DirectoryService;
import org.apache.directory.server.core.api.changelog.ChangeLog;
import org.apache.directory.server.ldap.LdapServer;
import org.apache.directory.server.xdbm.IndexNotFoundException;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.mockito.Mockito.lenient;

@RunWith(MockitoJUnitRunner.class)
public class LdapDirectoryServerConnectionTest {

    static EmbeddedLdapServer embeddedLdapServer;

    @Mock
    LdapConfigurationDao configurationDao;

    LdapContextFactory contextFactory;

    @Mock
    LdapUserManagerFactory userManagerFactory;

    @InjectMocks
    LdapConfiguration configuration;

    @InjectMocks
    private LdapManagerImpl ldapManager;

    private final LdapTestConfigTool ldapTestConfigTool = new LdapTestConfigTool();

    @BeforeClass
    public static void start() throws Exception {
        embeddedLdapServer = new EmbeddedLdapServer();
        embeddedLdapServer.init();
    }
    @Before
    public void setup() throws Exception {
        LdapConfigurationVO configurationVO = new LdapConfigurationVO("localhost",10389,null);
        lenient().when(configurationDao.find("localhost",10389,null)).thenReturn(configurationVO);
        ldapTestConfigTool.overrideConfigValue(configuration, "ldapBaseDn", "ou=system");
        ldapTestConfigTool.overrideConfigValue(configuration, "ldapBindPassword", "secret");
        ldapTestConfigTool.overrideConfigValue(configuration, "ldapBindPrincipal", "uid=admin,ou=system");
        ldapTestConfigTool.overrideConfigValue(configuration, "ldapMemberOfAttribute", "memberOf");
        lenient().when(userManagerFactory.getInstance(LdapUserManager.Provider.OPENLDAP)).thenReturn(new OpenLdapUserManagerImpl(configuration));
        // construct an elaborate structure around a single object
        Pair<List<LdapConfigurationVO>, Integer> vos = new Pair<List<LdapConfigurationVO>, Integer>( Collections.singletonList(configurationVO),1);
        lenient().when(configurationDao.searchConfigurations(null, 0, 1L)).thenReturn(vos);

        contextFactory = new LdapContextFactory(configuration);
        ldapManager = new LdapManagerImpl(configurationDao, contextFactory, userManagerFactory, configuration);
    }

    @After
    public void cleanup() throws Exception {
        contextFactory = null;
        ldapManager = null;
    }

    @AfterClass
    public static void stop() throws Exception {
        embeddedLdapServer.destroy();
    }

    @Test
    public void testEmbeddedLdapServerInitialization() throws IndexNotFoundException {
        LdapServer ldapServer = embeddedLdapServer.getLdapServer();
        assertNotNull(ldapServer);

        DirectoryService directoryService = embeddedLdapServer.getDirectoryService();
        assertNotNull(directoryService);
        assertNotNull(directoryService.getSchemaPartition());
        assertNotNull(directoryService.getSystemPartition());
        assertNotNull(directoryService.getSchemaManager());
        assertNotNull(directoryService.getDnFactory());

        assertNotNull(directoryService.isDenormalizeOpAttrsEnabled());

        ChangeLog changeLog = directoryService.getChangeLog();

        assertNotNull(changeLog);
        assertFalse(changeLog.isEnabled());

        assertNotNull(directoryService.isStarted());
        assertNotNull(ldapServer.isStarted());

        List userList = new ArrayList(embeddedLdapServer.getUserIndexMap().keySet());
        java.util.Collections.sort(userList);
        List checkList = Arrays.asList("uid");
        assertEquals(userList, checkList);
    }

//    @Test
    public void testEmbeddedLdapAvailable() {
        try {
            List<LdapUser> usahs = ldapManager.getUsers(1L);
            assertFalse("should find at least the admin user", usahs.isEmpty());
        } catch (NoLdapUserMatchingQueryException e) {
            fail(e.getLocalizedMessage());
        }
    }

    @Test
    public void testSchemaLoading() {
        try {
            assertTrue("standard not loaded", embeddedLdapServer.addSchemaFromClasspath("other"));
// we need member of in ACS nowadays (backwards compatibility broken):
// assertTrue("memberOf schema not loaded", embeddedLdapServer.addSchemaFromPath(new File("src/test/resources/memberOf"), "microsoft"));
        } catch (LdapException | IOException e) {
            fail(e.getLocalizedMessage());
        }
    }

//    @Test
    public void testUserCreation() {
        LdapConnection connection = new LdapNetworkConnection( "localhost", 10389 );
        try {
            connection.bind( "uid=admin,ou=system", "secret" );

            connection.add(new DefaultEntry(
                    "ou=acsadmins,ou=users,ou=system",
            "objectClass: organizationalUnit",
// might also need to be           objectClass: top
            "ou: acsadmins"
            ));
            connection.add(new DefaultEntry(
                    "uid=dahn,ou=acsadmins,ou=users,ou=system",
                    "objectClass: inetOrgPerson",
                    "objectClass: top",
                    "cn: dahn",
                    "sn: Hoogland",
                    "givenName: Daan",
                    "mail: d@b.c",
                    "uid: dahn"
            ));

            connection.add(
                    new DefaultEntry(
                            "cn=JuniorAdmins,ou=groups,ou=system", // The Dn
                            "objectClass: groupOfUniqueNames",
                            "ObjectClass: top",
                            "cn: JuniorAdmins",
                            "uniqueMember: uid=dahn,ou=acsadmins,ou=system,ou=users") );

            assertTrue( connection.exists( "cn=JuniorAdmins,ou=groups,ou=system" ) );
            assertTrue( connection.exists( "uid=dahn,ou=acsadmins,ou=users,ou=system" ) );

            Entry ourUser = connection.lookup("uid=dahn,ou=acsadmins,ou=users,ou=system");
            ourUser.add("memberOf", "cn=JuniorAdmins,ou=groups,ou=system");
            AddRequest addRequest = new AddRequestImpl();
            addRequest.setEntry( ourUser );
            AddResponse response = connection.add( addRequest );
            assertNotNull( response );
            // We would need to either
//            assertEquals( ResultCodeEnum.SUCCESS, response.getLdapResult().getResultCode() );
            // or have the automatic virtual attribute

            List<LdapUser> usahs = ldapManager.getUsers(1L);
            assertEquals("now an admin and a normal user should be present",2, usahs.size());

        } catch (LdapException | NoLdapUserMatchingQueryException e) {
            fail(e.getLocalizedMessage());
        }
    }
}
