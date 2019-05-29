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

import org.apache.directory.server.core.api.DirectoryService;
import org.apache.directory.server.core.api.changelog.ChangeLog;
import org.apache.directory.server.ldap.LdapServer;
import org.apache.directory.server.xdbm.IndexNotFoundException;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import software.apacheds.embedded.EmbeddedLdapServer;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;

public class LdapDirectoryServerConnectionTest {

    EmbeddedLdapServer embeddedLdapServer;

    @Before
    public void setup() throws Exception {
        this.embeddedLdapServer = new EmbeddedLdapServer();
        embeddedLdapServer.init();
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
}
