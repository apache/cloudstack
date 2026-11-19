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
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import org.apache.cloudstack.framework.config.ConfigKey;
import org.junit.jupiter.api.Test;

/**
 * The plugin's settings (SPEC.md section 9).
 *
 * Scope is the thing worth asserting: a setting at the wrong scope either
 * cannot be varied where it must be, or can be varied where it must not.
 */
class AiServicesConfigTest {

    @Test
    void everyKeyIsExported() {
        assertEquals(10, AiServicesConfig.all().length);
    }

    @Test
    void keyNamesAreNamespaced() {
        for (ConfigKey<?> key : AiServicesConfig.all()) {
            assertTrue(key.key().startsWith("ai.services."),
                    key.key() + " should be under the ai.services namespace");
        }
    }

    @Test
    void keyNamesAreUnique() {
        Set<String> names = new HashSet<>();
        for (ConfigKey<?> key : AiServicesConfig.all()) {
            assertTrue(names.add(key.key()), "duplicate config key " + key.key());
        }
    }

    @Test
    void enablementIsPerZone() {
        // A provider may run GPU capacity in one zone only.
        assertEquals(
                java.util.Collections.singletonList(ConfigKey.Scope.Zone),
                AiServicesConfig.Enabled.getScopes());
    }

    @Test
    void hubApprovalIsPerDomain() {
        // Section 5 lets one reseller mirror directly while others request it.
        assertEquals(
                java.util.Collections.singletonList(ConfigKey.Scope.Domain),
                AiServicesConfig.HfRequireApproval.getScopes());
    }

    @Test
    void engineSettingsAreGlobal() {
        for (ConfigKey<?> key : Arrays.asList(
                AiServicesConfig.EngineUrls,
                AiServicesConfig.EngineTimeout,
                AiServicesConfig.EngineClientCert,
                AiServicesConfig.EngineClientKey,
                AiServicesConfig.EngineTrustStore)) {
            assertEquals(
                    java.util.Collections.singletonList(ConfigKey.Scope.Global),
                    key.getScopes(), key.key());
        }
    }

    @Test
    void theDefaultIsDisabled() {
        // Enabling a zone should be a decision, not what happens on upgrade.
        assertFalse(Boolean.parseBoolean(AiServicesConfig.Enabled.defaultValue()));
    }

    @Test
    void hubApprovalDefaultsToRequired() {
        assertTrue(Boolean.parseBoolean(AiServicesConfig.HfRequireApproval.defaultValue()));
    }

    @Test
    void thereIsNoDefaultEngineUrl() {
        // A default would point the plugin somewhere nobody chose.
        assertEquals("", AiServicesConfig.EngineUrls.defaultValue());
    }

    @Test
    void theDrainTimeoutMatchesTheSpecDefault() {
        // SPEC.md section 7: 15 minutes.
        assertEquals("900", AiServicesConfig.MaintenanceDrainTimeout.defaultValue());
    }

    @Test
    void everyKeyIsDynamic() {
        // None of these should need a management server restart.
        for (ConfigKey<?> key : AiServicesConfig.all()) {
            assertTrue(key.isDynamic(), key.key() + " should be dynamic");
        }
    }

    @Test
    void everyKeyDocumentsItself() {
        for (ConfigKey<?> key : AiServicesConfig.all()) {
            assertTrue(key.description() != null && key.description().length() > 20,
                    key.key() + " needs a usable description");
        }
    }

    /**
     * A password must sit in a category CloudStack encrypts.
     *
     * <p>{@code ConfigurationVO.isEncrypted()} returns true only for the
     * categories "Hidden" and "Secure" — verified against the bytecode
     * of cloud-framework-config, where it is a literal
     * {@code StringUtils.equalsAny(getCategory(), "Hidden", "Secure")}.
     * Anything else is stored in the database, and returned by
     * {@code listConfigurations}, in cleartext.
     *
     * <p>The trust store's password is deliberately not here: a trust
     * store holds public certificates, so its password protects
     * nothing and Java's own convention is the well-known "changeit".
     */
    @Test
    void theKeyStorePasswordSitsInAnEncryptedCategory() {
        assertEquals("Secure", AiServicesConfig.EngineClientKey.category());
    }

    @Test
    void theOtherKeysStayWhereAnOperatorLooksForThem() {
        assertEquals("Advanced", AiServicesConfig.EngineClientCert.category());
        assertEquals("Advanced", AiServicesConfig.EngineTrustStore.category());
        assertEquals("Advanced", AiServicesConfig.EngineUrls.category());
    }
}
