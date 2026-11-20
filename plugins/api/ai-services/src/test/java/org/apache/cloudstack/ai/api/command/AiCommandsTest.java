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
package org.apache.cloudstack.ai.api.command;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.reflect.Field;

import org.apache.cloudstack.ai.AiServicesEvents;
import org.apache.cloudstack.ai.api.response.AiDeploymentResponse;
import org.apache.cloudstack.ai.engine.EngineException;
import org.apache.cloudstack.api.APICommand;
import org.apache.cloudstack.api.ApiErrorCode;
import org.apache.cloudstack.api.BaseAsyncCmd;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import com.cloud.dc.DataCenter;
import com.cloud.exception.InvalidParameterValueException;
import com.cloud.utils.db.EntityManager;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

/** The async commands and the engine-error mapping (SPEC.md section 9). */
class AiCommandsTest {

    private static void set(Object target, String field, Object value) throws Exception {
        Field declared = target.getClass().getDeclaredField(field);
        declared.setAccessible(true);
        declared.set(target, value);
    }

    @Test
    void deployIsAnAsyncCommand() {
        assertTrue(BaseAsyncCmd.class.isAssignableFrom(DeployAiModelCmd.class));
        assertEquals("deployAiModel",
                DeployAiModelCmd.class.getAnnotation(APICommand.class).name());
    }

    @Test
    void deployEmitsTheSpecifiedEvent() {
        assertEquals(AiServicesEvents.MODEL_DEPLOY, new DeployAiModelCmd().getEventType());
    }

    @Test
    void deployPostsToTheDeploymentsEndpoint() {
        DeployAiModelCmd command = new DeployAiModelCmd();

        assertEquals("/v1/deployments", command.enginePath());
        assertEquals("POST", command.engineMethod());
    }

    /** A command whose zone id resolves to a CloudStack zone by that name. */
    private static DeployAiModelCmd deployCommandFor(long zoneId, String zoneName) throws Exception {
        DeployAiModelCmd command = new DeployAiModelCmd();
        set(command, "zoneId", zoneId);
        DataCenter zone = mock(DataCenter.class);
        when(zone.getName()).thenReturn(zoneName);
        EntityManager entities = mock(EntityManager.class);
        when(entities.findById(DataCenter.class, zoneId)).thenReturn(zone);
        command._entityMgr = entities;
        return command;
    }

    @Test
    void deployCarriesTheModelAndTheZonesNameInItsBody() throws Exception {
        // The *name*, not the id. `zoneid` is a CommandType.UUID, which
        // CloudStack hands back as its internal numeric id, and the
        // engine knows zones by the name platform.yaml records in
        // `cloudstack_zone`. Sending the raw Long was answered with a
        // 422 by a live engine, and this test pinned it in place by
        // asserting the number.
        DeployAiModelCmd command = deployCommandFor(4L, "East-1");
        set(command, "modelName", "llama-3.3-70b-instruct");

        JsonObject body = JsonParser.parseString(command.engineBody()).getAsJsonObject();

        assertEquals("llama-3.3-70b-instruct", body.get("model").getAsString());
        assertEquals("East-1", body.get("zone_id").getAsString());
    }

    @Test
    void deployRefusesAZoneCloudStackCannotFind() throws Exception {
        DeployAiModelCmd command = new DeployAiModelCmd();
        set(command, "modelName", "m");
        set(command, "zoneId", 9L);
        command._entityMgr = mock(EntityManager.class);

        assertThrows(InvalidParameterValueException.class, command::engineBody);
    }

    @Test
    void anUnsetReplicaCountIsOmittedRatherThanSentAsZero() throws Exception {
        // Zero would scale the deployment to nothing; absent lets the
        // engine use the model's declared minimum.
        DeployAiModelCmd command = deployCommandFor(1L, "East-1");
        set(command, "modelName", "m");

        JsonObject body = JsonParser.parseString(command.engineBody()).getAsJsonObject();

        assertFalse(body.has("replicas"));
    }

    @Test
    void anExplicitReplicaCountIsSent() throws Exception {
        DeployAiModelCmd command = deployCommandFor(1L, "East-1");
        set(command, "modelName", "m");
        set(command, "replicas", 3);

        JsonObject body = JsonParser.parseString(command.engineBody()).getAsJsonObject();

        assertEquals(3, body.get("replicas").getAsInt());
    }

    @Test
    void deployDescribesWhatItIsDoing() throws Exception {
        DeployAiModelCmd command = new DeployAiModelCmd();
        set(command, "modelName", "llama");

        assertTrue(command.getEventDescription().contains("llama"));
    }

    @Test
    void deleteUsesTheDeleteVerbAgainstTheDeployment() throws Exception {
        DeleteAiModelDeploymentCmd command = new DeleteAiModelDeploymentCmd();
        set(command, "deploymentId", "d-1");

        assertEquals("/v1/deployments/d-1", command.enginePath());
        assertEquals("DELETE", command.engineMethod());
        assertNull(command.engineBody());
    }

    @Test
    void deleteEmitsTheSpecifiedEvent() {
        assertEquals(AiServicesEvents.MODEL_DELETE,
                new DeleteAiModelDeploymentCmd().getEventType());
    }

    @ParameterizedTest
    @CsvSource({
        "400, PARAM_ERROR",
        "422, PARAM_ERROR",
        "404, PARAM_ERROR",
        "403, ACCOUNT_ERROR",
        "503, RESOURCE_UNAVAILABLE_ERROR",
        "500, INTERNAL_ERROR",
        "0, INTERNAL_ERROR",
    })
    void engineErrorsMapOntoCloudStackCodes(int status, String expected) {
        // A 403 is a permission decision; reporting it as an internal
        // error would hide from the tenant why their call failed.
        ApiErrorCode mapped =
                AiServicesAsyncCmd.errorCodeFor(new EngineException("x", status, null));

        assertEquals(expected, mapped.name());
    }

    @Test
    void aDeploymentResponseIsBuiltFromAJobResult() {
        JsonObject result = JsonParser.parseString(
                "{\"deployment\": {\"id\": \"d-1\", \"model\": \"llama\","
                        + " \"colour\": \"blue\", \"replicas_desired\": 2,"
                        + " \"replicas_ready\": 1, \"state\": \"Running\"}}")
                .getAsJsonObject();

        AiDeploymentResponse response = AiDeploymentResponse.from(result);

        assertEquals("d-1", response.getId());
        assertEquals("llama", response.getModel());
        assertEquals("blue", response.getColour());
        assertEquals(2, response.getReplicasDesired());
        assertEquals(1, response.getReplicasReady());
        assertEquals("Running", response.getState());
    }

    @Test
    void aFlatDeploymentObjectWorksToo() {
        JsonObject result = JsonParser.parseString(
                "{\"id\": \"d-2\", \"model\": \"m\"}").getAsJsonObject();

        assertEquals("d-2", AiDeploymentResponse.from(result).getId());
    }

    @Test
    void anEmptyJobResultStillYieldsAResponse() {
        AiDeploymentResponse response = AiDeploymentResponse.from(null);

        assertNotNull(response);
        assertEquals("aideployment", response.getObjectName());
        assertNull(response.getId());
    }

    @Test
    void missingFieldsAreNullRatherThanAnError() {
        AiDeploymentResponse response = AiDeploymentResponse.from(
                JsonParser.parseString("{\"id\": \"d-3\"}").getAsJsonObject());

        assertNull(response.getReplicasReady());
        assertNull(response.getState());
    }
}
