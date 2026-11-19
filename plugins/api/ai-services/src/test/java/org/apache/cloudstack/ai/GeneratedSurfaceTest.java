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

import java.io.InputStream;
import java.io.InputStreamReader;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import org.mockito.ArgumentMatchers;
import com.cloud.utils.db.EntityManager;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

import org.apache.cloudstack.api.APICommand;
import org.apache.cloudstack.api.BaseAsyncCmd;
import org.apache.cloudstack.api.BaseResponse;
import org.apache.cloudstack.api.Parameter;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

/**
 * The generated command and response surface (SPEC.md section 9).
 *
 * Thirty-five commands and a dozen response types are generated from one
 * table, so they are tested the same way: systematically, over every
 * class, rather than one file at a time. The engine objects come from
 * `generated-engine-objects.json`, which the generator emits beside the
 * classes so the field list is never maintained twice.
 */
class GeneratedSurfaceTest {

    private static final JsonObject FIXTURE = loadFixture();

    private static JsonObject loadFixture() {
        try (InputStream in = GeneratedSurfaceTest.class.getClassLoader()
                .getResourceAsStream("generated-engine-objects.json")) {
            assertNotNull(in, "generated-engine-objects.json is missing; run the generator");
            return JsonParser.parseReader(
                    new InputStreamReader(in, StandardCharsets.UTF_8)).getAsJsonObject();
        } catch (Exception failure) {
            throw new IllegalStateException("could not read the engine fixture", failure);
        }
    }

    /**
     * Every generated command, v2 included.
     *
     * `getCommands()` leaves the v2 commands out unless the setting is on, and
     * the setting defaults to off — so iterating the manager as shipped ran
     * these invariants over the 38 v1 commands and never constructed the 20 v2
     * ones at all. They were not merely unasserted: Codecov reported
     * UploadAiAdapterCmd at 0%, and about 947 lines across those classes were
     * untouched by any test.
     *
     * The invariants here — a versioned path, no surviving placeholder, a known
     * verb, valid JSON, no body on a read — are properties of a generated
     * command, not of a release. Whether a command is registered is
     * V2CommandRegistrationTest's question, and it asks it both ways.
     */
    static Stream<Class<?>> commands() {
        return new AiServicesManagerImpl() {
            @Override
            boolean v2Enabled() {
                return true;
            }
        }.getCommands().stream();
    }

    static Stream<String> responseNames() {
        return FIXTURE.keySet().stream();
    }

    // -- responses --------------------------------------------------------

    @ParameterizedTest
    @MethodSource("responseNames")
    void everyFieldOfAnEngineObjectIsMapped(String responseName) throws Exception {
        Class<?> type = Class.forName(
                "org.apache.cloudstack.ai.api.response." + responseName);
        Method from = type.getMethod("from", JsonObject.class);

        Object mapped = from.invoke(null, FIXTURE.getAsJsonObject(responseName));

        assertNotNull(mapped);
        for (Field field : type.getDeclaredFields()) {
            if (field.isSynthetic() || java.lang.reflect.Modifier.isStatic(
                    field.getModifiers())) {
                continue;
            }
            field.setAccessible(true);
            assertNotNull(field.get(mapped),
                    responseName + "." + field.getName() + " was not mapped");
        }
    }

    @ParameterizedTest
    @MethodSource("responseNames")
    void anEmptyEngineObjectMapsWithoutFailing(String responseName) throws Exception {
        Class<?> type = Class.forName(
                "org.apache.cloudstack.ai.api.response." + responseName);
        Method from = type.getMethod("from", JsonObject.class);

        assertNotNull(from.invoke(null, new JsonObject()));
    }

    @ParameterizedTest
    @MethodSource("responseNames")
    void aNullEngineObjectStillYieldsANamedResponse(String responseName)
            throws Exception {
        Class<?> type = Class.forName(
                "org.apache.cloudstack.ai.api.response." + responseName);
        Method from = type.getMethod("from", JsonObject.class);

        BaseResponse mapped = (BaseResponse) from.invoke(null, (JsonObject) null);

        assertNotNull(mapped.getObjectName(),
                responseName + " must name itself for the serialiser");
    }

    // -- commands ---------------------------------------------------------

    /** Fill every declared @Parameter so paths and bodies are exercised. */
    private static Object populate(Class<?> command) throws Exception {
        Object instance = command.getDeclaredConstructor().newInstance();
        // A command with a typed entity parameter resolves it through
        // `_entityMgr` before building its body, so without one every
        // such command fails here with a NullPointerException that
        // says nothing about the command. Only v1 commands are listed
        // while `ai.services.v2.enabled` is off, which is why this was
        // not needed until a v1 command started resolving.
        EntityManager entities = mock(EntityManager.class);
        when(entities.findById(ArgumentMatchers.<Class<?>>any(), any()))
                .thenAnswer(found -> mock(found.getArgument(0, Class.class)));
        setEntityManager(instance, entities);

        for (Field field : command.getDeclaredFields()) {
            if (field.getAnnotation(Parameter.class) == null) {
                continue;
            }
            field.setAccessible(true);
            Class<?> type = field.getType();
            if (type == String.class) {
                field.set(instance, "sample");
            } else if (type == Integer.class) {
                field.set(instance, 7);
            } else if (type == Long.class) {
                field.set(instance, 7L);
            } else if (type == Boolean.class) {
                field.set(instance, Boolean.TRUE);
            }
        }
        return instance;
    }

    /** Set `BaseCmd._entityMgr`, wherever it sits in the hierarchy. */
    private static void setEntityManager(Object instance, EntityManager entities)
            throws Exception {
        for (Class<?> current = instance.getClass();
                current != null;
                current = current.getSuperclass()) {
            try {
                Field field = current.getDeclaredField("_entityMgr");
                field.setAccessible(true);
                field.set(instance, entities);
                return;
            } catch (NoSuchFieldException keepLooking) {
                continue;
            }
        }
    }

    private static String enginePathOf(Object command) throws Exception {
        Method path = findMethod(command.getClass(), "enginePath");
        path.setAccessible(true);
        return (String) path.invoke(command);
    }

    private static Method findMethod(Class<?> type, String name) throws Exception {
        for (Class<?> current = type; current != null; current = current.getSuperclass()) {
            try {
                return current.getDeclaredMethod(name);
            } catch (NoSuchMethodException keepLooking) {
                continue;
            }
        }
        throw new NoSuchMethodException(name + " on " + type.getName());
    }

    @ParameterizedTest
    @MethodSource("commands")
    void everyCommandBuildsAVersionedEnginePath(Class<?> command) throws Exception {
        String path = enginePathOf(populate(command));

        assertTrue(path.startsWith("/v1/"), command.getSimpleName() + " -> " + path);
    }

    @ParameterizedTest
    @MethodSource("commands")
    void noPathPlaceholderSurvivesIntoTheRequest(Class<?> command) throws Exception {
        // An unsubstituted {id} would be sent to the engine literally.
        String path = enginePathOf(populate(command));

        assertFalse(path.contains("{"), command.getSimpleName() + " -> " + path);
        assertFalse(path.contains("}"), command.getSimpleName() + " -> " + path);
    }

    @ParameterizedTest
    @MethodSource("commands")
    void everyCommandUsesAKnownVerb(Class<?> command) throws Exception {
        Method method = findMethod(command, "engineMethod");
        method.setAccessible(true);
        String verb = (String) method.invoke(populate(command));

        assertTrue(
                List.of("GET", "POST", "PATCH", "DELETE").contains(verb),
                command.getSimpleName() + " uses " + verb);
    }

    @ParameterizedTest
    @MethodSource("commands")
    void aBodyCarryingCommandProducesValidJson(Class<?> command) throws Exception {
        Method body = findMethod(command, "engineBody");
        body.setAccessible(true);
        String rendered = (String) body.invoke(populate(command));

        if (rendered != null) {
            assertTrue(JsonParser.parseString(rendered).isJsonObject(),
                    command.getSimpleName() + " body is not an object");
        }
    }

    @ParameterizedTest
    @MethodSource("commands")
    void readsCarryNoBody(Class<?> command) throws Exception {
        Method method = findMethod(command, "engineMethod");
        method.setAccessible(true);
        Object instance = populate(command);

        if ("GET".equals(method.invoke(instance)) || "DELETE".equals(method.invoke(instance))) {
            Method body = findMethod(command, "engineBody");
            body.setAccessible(true);
            assertEquals(null, body.invoke(instance),
                    command.getSimpleName() + " sends a body on a read");
        }
    }

    @Test
    void theFixtureCoversEveryGeneratedResponse() {
        // A new response type without a fixture entry would be untested.
        List<String> missing = new ArrayList<>();
        for (Map.Entry<String, com.google.gson.JsonElement> entry : FIXTURE.entrySet()) {
            if (entry.getValue().getAsJsonObject().size() == 0) {
                missing.add(entry.getKey());
            }
        }

        assertTrue(missing.isEmpty(), "fixture entries with no fields: " + missing);
    }

    @Test
    void asyncCommandsDeclareAnEventTypeAndDescription() throws Exception {
        for (Class<?> command : new AiServicesManagerImpl().getCommands()) {
            if (!BaseAsyncCmd.class.isAssignableFrom(command)) {
                continue;
            }
            BaseAsyncCmd instance = (BaseAsyncCmd) populate(command);

            assertTrue(instance.getEventType().startsWith("AI."),
                    command.getSimpleName() + " event: " + instance.getEventType());
            assertFalse(instance.getEventDescription().isEmpty(),
                    command.getSimpleName() + " has no event description");
        }
    }

    @Test
    void everyCommandNameMatchesItsClassName() {
        for (Class<?> command : new AiServicesManagerImpl().getCommands()) {
            String name = command.getAnnotation(APICommand.class).name();
            String expected = Character.toUpperCase(name.charAt(0)) + name.substring(1) + "Cmd";

            assertEquals(expected, command.getSimpleName());
        }
    }
}
