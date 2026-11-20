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
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.apache.cloudstack.ai.api.command.AiServicesAsyncCmd;
import org.apache.cloudstack.api.APICommand;
import org.apache.cloudstack.api.BaseAsyncCmd;
import org.apache.cloudstack.api.BaseCmd;
import org.junit.jupiter.api.Test;

/**
 * The whole v1 command surface (SPEC.md section 9).
 *
 * Section 9's table is the authoritative list. It lives as data in
 * `aisvc/api/commands.py`, where the Python tests check it against the
 * engine's routes; these assert the Java side of the same contract, so
 * a command cannot exist in one place and be missing from the other.
 */
class CommandRegistrationTest {

    /** Every v1 command name in SPEC.md section 9's table. */
    private static final List<String> SPEC_V1_COMMANDS = Arrays.asList(
            "searchAiHfModels", "checkAiHfModel", "mirrorAiModel", "approveAiModel",
            "publishAiModel", "listAiModels", "deployAiModel", "listAiModelDeployments",
            "updateAiModelDeployment", "deleteAiModelDeployment", "promoteAiRollout",
            "rollbackAiRollout", "listAiEndpoints", "createAiApiKey", "listAiApiKeys", "revokeAiApiKey",
            "listAiServingProfiles", "createAiServingProfile", "updateAiServingProfile",
            "deleteAiServingProfile", "listAiGenerationProfiles",
            "createAiGenerationProfile", "updateAiGenerationProfile",
            "deleteAiGenerationProfile", "assignAiTuningProfile",
            "getAiEffectiveParameters", "listAiUsage", "listAiBudgets", "createAiBudget",
            "updateAiBudget", "deleteAiBudget", "createAiPlaygroundSession",
            "listAiSetupChecks", "discoverAiGpuClasses", "runAiSetupSelfTest", "listAiGpuCapacity", "drainAiNode",
            "listAiReconcilerRuns");

    /** The commands SPEC.md section 9 marks async. */
    private static final Set<String> ASYNC_COMMANDS = new HashSet<>(Arrays.asList(
            "mirrorAiModel", "deployAiModel", "updateAiModelDeployment",
            "deleteAiModelDeployment", "promoteAiRollout", "rollbackAiRollout",
            "createAiServingProfile", "updateAiServingProfile", "deleteAiServingProfile",
            "assignAiTuningProfile", "runAiSetupSelfTest", "drainAiNode"));

    private final List<Class<?>> registered = new AiServicesManagerImpl().getCommands();

    private Set<String> registeredNames() {
        Set<String> names = new HashSet<>();
        for (Class<?> command : registered) {
            names.add(command.getAnnotation(APICommand.class).name());
        }
        return names;
    }

    @Test
    void everyV1CommandInTheSpecIsRegistered() {
        List<String> missing = new ArrayList<>(SPEC_V1_COMMANDS);
        missing.removeAll(registeredNames());

        assertTrue(missing.isEmpty(), "commands in SPEC.md but not registered: " + missing);
    }

    @Test
    void nothingIsRegisteredThatTheSpecDoesNotList() {
        Set<String> extra = new HashSet<>(registeredNames());
        SPEC_V1_COMMANDS.forEach(extra::remove);

        assertTrue(extra.isEmpty(), "registered but not in SPEC.md section 9: " + extra);
    }

    @Test
    void theCountMatchesTheSpecsTable() {
        assertEquals(SPEC_V1_COMMANDS.size(), registered.size());
    }

    @Test
    void everyCommandIsConcreteAndInstantiable() {
        for (Class<?> command : registered) {
            assertFalse(Modifier.isAbstract(command.getModifiers()),
                    command.getSimpleName() + " is abstract");
            assertTrue(BaseCmd.class.isAssignableFrom(command),
                    command.getSimpleName() + " does not extend BaseCmd");
        }
    }

    @Test
    void theAsyncCommandsAreTheOnesTheSpecMarksAsync() {
        for (Class<?> command : registered) {
            String name = command.getAnnotation(APICommand.class).name();
            boolean isAsync = BaseAsyncCmd.class.isAssignableFrom(command);

            assertEquals(ASYNC_COMMANDS.contains(name), isAsync,
                    name + " async mismatch: SPEC.md says "
                            + ASYNC_COMMANDS.contains(name));
        }
    }

    @Test
    void everyAsyncCommandUsesOurBridge() {
        // Extending BaseAsyncCmd directly would skip the 202 check and
        // the engine job polling.
        for (Class<?> command : registered) {
            if (BaseAsyncCmd.class.isAssignableFrom(command)) {
                assertTrue(AiServicesAsyncCmd.class.isAssignableFrom(command),
                        command.getSimpleName() + " bypasses AiServicesAsyncCmd");
            }
        }
    }

    @Test
    void everyCommandDeclaresAResponseObject() {
        for (Class<?> command : registered) {
            APICommand annotation = command.getAnnotation(APICommand.class);
            assertNotNull(annotation.responseObject(), command.getSimpleName());
            assertFalse(annotation.description().isEmpty(),
                    command.getSimpleName() + " has no description");
        }
    }

    @Test
    void onlyTheCommandsThatMintAKeyReturnSensitiveInfo() {
        // Two, and only two. A key's secret is shown once, and marking
        // more would suppress response logging that is useful.
        //
        // createAiPlaygroundSession was absent from this set, and the
        // test passed — because the response mapping was dropping the
        // key it mints, reading `session_key` where the engine sends
        // `apikey`. The declaration was accidentally true, which is a
        // bad reason for a test to be green: the command has always
        // minted a real key, and now that the key survives the mapping
        // the declaration has to say so.
        Set<String> mintsAKey = Set.of("createAiApiKey", "createAiPlaygroundSession");

        for (Class<?> command : registered) {
            APICommand annotation = command.getAnnotation(APICommand.class);

            assertEquals(mintsAKey.contains(annotation.name()),
                    annotation.responseHasSensitiveInfo(),
                    annotation.name());
        }
    }

    @Test
    void noCommandRequestCarriesSensitiveInfo() {
        // Requests carry names and ids; secrets reach the engine over
        // mTLS from configuration, never as API parameters.
        for (Class<?> command : registered) {
            assertFalse(command.getAnnotation(APICommand.class).requestHasSensitiveInfo(),
                    command.getSimpleName());
        }
    }

    @Test
    void commandNamesAreUnique() {
        assertEquals(registered.size(), registeredNames().size());
    }

    @Test
    void everyCommandCanBeConstructed() throws Exception {
        // CloudStack instantiates these reflectively; a missing no-arg
        // constructor fails at request time rather than at startup.
        for (Class<?> command : registered) {
            assertNotNull(command.getDeclaredConstructor().newInstance(),
                    command.getSimpleName());
        }
    }
}
