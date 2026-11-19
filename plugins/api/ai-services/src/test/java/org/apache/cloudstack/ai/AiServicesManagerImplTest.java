// Licensed to the Apache Software Foundation (ASF) under one or more
// contributor license agreements. See the NOTICE file distributed with this
// work for additional information regarding copyright ownership. The ASF
// licenses this file to you under the Apache License, Version 2.0 (the
// "License"); you may not use this file except in compliance with the
// License. You may obtain a copy of the License at
//
//   http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
// WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
// License for the specific language governing permissions and limitations
// under the License.
package org.apache.cloudstack.ai;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import org.apache.cloudstack.ai.api.command.ListAiModelsCmd;
import org.apache.cloudstack.api.APICommand;
import org.apache.cloudstack.api.BaseCmd;
import org.apache.cloudstack.framework.config.ConfigKey;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.apache.cloudstack.ai.dao.AiDeploymentDao;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.cloud.domain.DomainVO;
import com.cloud.domain.dao.DomainDao;

import com.cloud.utils.component.PluggableService;

/**
 * Command and configuration registration (SPEC.md section 9).
 *
 * CloudStack discovers what a plugin offers through
 * {@link PluggableService#getCommands()} and {@code Configurable}. A
 * command missing from that list is simply not an API, however complete
 * the class is, so the registration is worth asserting.
 */
class AiServicesManagerImplTest {

    private AiServicesManagerImpl manager;

    @BeforeEach
    void setUp() {
        manager = new AiServicesManagerImpl();
    }

    @Test
    void itIsAPluggableService() {
        assertTrue(manager instanceof PluggableService);
    }

    @Test
    void itRegistersItsCommands() {
        List<Class<?>> commands = manager.getCommands();

        assertFalse(commands.isEmpty());
        assertTrue(commands.contains(ListAiModelsCmd.class));
    }

    @Test
    void everyRegisteredCommandIsAnApiCommand() {
        for (Class<?> command : manager.getCommands()) {
            assertNotNull(command.getAnnotation(APICommand.class),
                    command.getSimpleName() + " is registered but has no @APICommand");
            assertTrue(BaseCmd.class.isAssignableFrom(command),
                    command.getSimpleName() + " does not extend BaseCmd");
        }
    }

    @Test
    void registeredCommandsAreDistinct() {
        List<Class<?>> commands = manager.getCommands();

        assertEquals(commands.size(), new java.util.HashSet<>(commands).size());
    }

    @Test
    void itNamesItselfAsTheConfigComponent() {
        assertEquals("AiServicesManagerImpl", manager.getConfigComponentName());
    }

    @Test
    void itExportsEveryConfigKey() {
        ConfigKey<?>[] keys = manager.getConfigKeys();

        assertEquals(AiServicesConfig.all().length, keys.length);
    }

    @Test
    void theDomainPathIsResolvedFromTheDomainDao() {
        // The engine decides domain-admin visibility by prefix-matching
        // this, so a partial path hides or leaks a whole domain.
        DomainDao domains = mock(DomainDao.class);
        DomainVO domain = new DomainVO("Acme", 2L, 1L, "Acme");
        domain.setPath("/Customers/Acme/");
        when(domains.findById(7L)).thenReturn(domain);
        manager.setDomainDao(domains);

        assertEquals("/Customers/Acme", manager.domainPathOf(7L));
    }

    @Test
    void theRootDomainPathStaysASingleSlash() {
        DomainDao domains = mock(DomainDao.class);
        DomainVO root = new DomainVO("ROOT", 1L, null, "ROOT");
        root.setPath("/");
        when(domains.findById(1L)).thenReturn(root);
        manager.setDomainDao(domains);

        assertEquals("/", manager.domainPathOf(1L));
    }

    @Test
    void anUnknownDomainRefusesRatherThanGuessing() {
        // Calling the engine with a made-up domain would widen or hide
        // access silently.
        DomainDao domains = mock(DomainDao.class);
        when(domains.findById(anyLong())).thenReturn(null);
        manager.setDomainDao(domains);

        IllegalStateException failure = assertThrows(
                IllegalStateException.class, () -> manager.domainPathOf(99L));

        assertTrue(failure.getMessage().contains("refusing"));
    }

    @Test
    void cleanupRemovesTheAccountsOwnershipRows() {
        AiDeploymentDao deployments = mock(AiDeploymentDao.class);
        when(deployments.removeByAccount(5L)).thenReturn(3);
        manager.setDeploymentDao(deployments);

        assertEquals(3, manager.cleanupAccount(5L));
        verify(deployments).removeByAccount(5L);
    }

    @Test
    void cleanupIsSafeBeforeTheDaoIsInjected() {
        assertEquals(0, manager.cleanupAccount(5L));
    }

    @Test
    void aTransportCanBeInjectedWithoutTouchingConfiguration() {
        // Lets a test drive the manager without an mTLS keystore; building
        // the client itself needs the config framework, so that is covered
        // by EngineClientTest rather than here.
        manager.setTransport((method, url, headers, body) -> null);
    }
}
