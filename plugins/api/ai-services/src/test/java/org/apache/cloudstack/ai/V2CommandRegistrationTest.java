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
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.junit.jupiter.api.Test;

/**
 * Tier 2 commands are generated but off by default (SPEC.md section 8).
 *
 * <p>The engine marks these {@code Release.V2} in {@code commands.py},
 * and that boundary has to hold on this side too: a GA install should
 * not grow an API surface for features it does not run. The global
 * setting {@code ai.services.v2.enabled} turns them on, so an operator
 * can exercise v2 without a separate build.
 */
class V2CommandRegistrationTest {

    /** Every v2 command, as aisvc/api/commands.py names them. */
    private static final Set<String> V2_COMMANDS = Set.of(
            "listAiAdapters", "uploadAiAdapter", "deleteAiAdapter",
            "createAiBatch", "listAiBatches", "cancelAiBatch", "getAiBatchResults",
            "listAiGuardrailProfiles", "createAiGuardrailProfile",
            "updateAiGuardrailProfile", "deleteAiGuardrailProfile",
            "runAiModelEvaluation", "listAiEvaluations",
            "uploadAiCertificate", "listAiCertificates", "replaceAiCertificate",
            "deleteAiCertificate",
            "listAiPrivateEndpoints", "createAiPrivateEndpoint", "deleteAiPrivateEndpoint");

    /** The production path: no ConfigDepot in a unit test, so the key reads its default. */
    private Set<String> asShipped() {
        return names(new AiServicesManagerImpl());
    }

    /** The same manager with the setting turned on. */
    private Set<String> withV2() {
        return names(new AiServicesManagerImpl() {
            @Override
            boolean v2Enabled() {
                return true;
            }
        });
    }

    private static Set<String> names(AiServicesManagerImpl manager) {
        return manager.getCommands().stream()
                .map(Class::getSimpleName)
                .collect(Collectors.toSet());
    }

    private static String className(String apiName) {
        return Character.toUpperCase(apiName.charAt(0)) + apiName.substring(1) + "Cmd";
    }

    @Test
    void v2CommandsAreAbsentByDefault() {
        Set<String> registered = asShipped();

        for (String command : V2_COMMANDS) {
            assertFalse(registered.contains(className(command)),
                    command + " should not be registered unless v2 is enabled");
        }
    }

    @Test
    void v2CommandsAppearWhenEnabled() {
        Set<String> registered = withV2();

        for (String command : V2_COMMANDS) {
            assertTrue(registered.contains(className(command)),
                    command + " should be registered when v2 is enabled");
        }
    }

    @Test
    void enablingV2LeavesTheGaSurfaceAlone() {
        // Turning v2 on adds; it must never take away or replace.
        assertTrue(withV2().containsAll(asShipped()));
    }

    @Test
    void enablingV2AddsNothingButV2() {
        Set<String> shipped = asShipped();
        List<String> added = withV2().stream()
                .filter(name -> !shipped.contains(name))
                .sorted()
                .collect(Collectors.toList());

        assertEquals(
                V2_COMMANDS.stream().map(V2CommandRegistrationTest::className).sorted()
                        .collect(Collectors.toList()),
                added);
    }

    @Test
    void theSettingIsOffByDefault() {
        assertFalse(Boolean.parseBoolean(AiServicesConfig.V2Enabled.defaultValue()));
    }

    @Test
    void theSettingIsExported() {
        assertTrue(Arrays.asList(AiServicesConfig.all()).contains(AiServicesConfig.V2Enabled));
    }
}
