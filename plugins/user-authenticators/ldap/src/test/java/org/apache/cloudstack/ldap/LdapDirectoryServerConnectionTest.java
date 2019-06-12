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
import org.apache.directory.api.ldap.model.exception.LdapException;
import org.apache.directory.api.ldap.model.ldif.LdifReader;
import org.apache.directory.server.core.api.DirectoryService;
import org.apache.directory.server.core.api.changelog.ChangeLog;
import org.apache.directory.server.ldap.LdapServer;
import org.apache.directory.server.xdbm.IndexNotFoundException;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import software.apacheds.embedded.EmbeddedLdapServer;

import javax.naming.ldap.LdapContext;
import java.io.File;
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
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.initMocks;

@RunWith(MockitoJUnitRunner.class)
public class LdapDirectoryServerConnectionTest {

    EmbeddedLdapServer embeddedLdapServer;

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

    @Before
    public void setup() throws Exception {
        this.embeddedLdapServer = new EmbeddedLdapServer();
//        embeddedLdapServer.addSchemaFromPath(new File("src/test/resources/"), "minimal.ldif");
        embeddedLdapServer.init();

        LdapConfigurationVO configurationVO = new LdapConfigurationVO("localhost",10389,null);
        when(configurationDao.find("localhost",10389,null)).thenReturn(configurationVO);
        ldapTestConfigTool.overrideConfigValue(configuration, "ldapBaseDn", "ou=system");
        ldapTestConfigTool.overrideConfigValue(configuration, "ldapBindPassword", "secret");
        ldapTestConfigTool.overrideConfigValue(configuration, "ldapBindPrincipal", "uid=admin,ou=system");
        when(userManagerFactory.getInstance(LdapUserManager.Provider.OPENLDAP)).thenReturn(new OpenLdapUserManagerImpl(configuration));
        // construct an ellaborate structure around a single object
        Pair<List<LdapConfigurationVO>, Integer> vos = new Pair<List<LdapConfigurationVO>, Integer>( Collections.singletonList(configurationVO),1);
        when(configurationDao.searchConfigurations(null, 0, 1L)).thenReturn(vos);

        contextFactory = new LdapContextFactory(configuration);
        ldapManager = new LdapManagerImpl(configurationDao, contextFactory, userManagerFactory, configuration);
    }

    @After
    public void cleanup() throws Exception {
        embeddedLdapServer.destroy();
    }

    @Test
    public void testEmbeddedLdapServerInitialization() throws IndexNotFoundException {
//        expect:
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
//        assertEquals(["uid"].sort(), embeddedLdapServer.getUserIndexMap().keySet().sort());
    }

    @Test
    public void testEmbeddedLdifLoading() {
        try {
            embeddedLdapServer.addSchemaFromClasspath("minimal.ldif");
            List<LdapUser> usahs = ldapManager.getUsers(1L);
            assertFalse("should find some users", usahs.isEmpty());
            ldapManager.getUser("dahn", LdapManager.LinkType.GROUP.toString(), "admins", 1L);
        } catch (LdapException e) {
            fail(e.getLocalizedMessage());
        } catch (IOException e) {
            fail(e.getLocalizedMessage());
        } catch (NoLdapUserMatchingQueryException e) {
            fail(e.getLocalizedMessage());
        }
    }
}
