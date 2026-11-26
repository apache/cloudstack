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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.lang.reflect.Field;

import org.apache.cloudstack.api.ApiConstants;
import org.apache.cloudstack.api.Parameter;
import org.apache.cloudstack.api.response.NetworkResponse;
import org.junit.jupiter.api.Test;

import com.cloud.exception.InvalidParameterValueException;
import com.cloud.network.Network;
import com.cloud.utils.db.EntityManager;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

/**
 * The network a private endpoint attaches to (SPEC.md section 8).
 *
 * <p>The parameter was a plain {@code STRING}, so CloudStack never
 * checked that the caller could see the network it named: no entity
 * type meant no ACL check and no resolution. Declaring it a
 * {@code UUID} with {@code entityType} gets both.
 *
 * <p>That changes what the command holds. CloudStack resolves a UUID
 * parameter to the **internal numeric id**, and the engine stores the
 * network id it is given and compares it against the UUIDs CloudStack
 * returns from {@code listNetworks}. So the id has to be translated
 * back before it is sent on, or a private endpoint would be recorded
 * against a number that matches nothing.
 */
class PrivateEndpointNetworkTest {

    private static void set(Object target, String field, Object value) throws Exception {
        Field f = target.getClass().getDeclaredField(field);
        f.setAccessible(true);
        f.set(target, value);
    }

    private static Parameter parameterOn(Class<?> cls, String field) throws Exception {
        return cls.getDeclaredField(field).getAnnotation(Parameter.class);
    }

    @Test
    void theNetworkIsDeclaredAsAnEntitySoCloudStackChecksAccess() throws Exception {
        Parameter annotation = parameterOn(CreateAiPrivateEndpointCmd.class, "networkId");

        assertEquals(NetworkResponse.class, annotation.entityType()[0]);
    }

    @Test
    void itIsAUuidParameterRatherThanAString() throws Exception {
        Parameter annotation = parameterOn(CreateAiPrivateEndpointCmd.class, "networkId");

        assertEquals("uuid", annotation.type().name().toLowerCase());
    }

    @Test
    void theBodyCarriesTheNetworksUuidNotItsInternalId() throws Exception {
        // The engine compares this against listNetworks output.
        Network network = mock(Network.class);
        when(network.getUuid()).thenReturn("8f1c-net-uuid");
        EntityManager entities = mock(EntityManager.class);
        when(entities.findById(eq(Network.class), eq(7L))).thenReturn(network);

        CreateAiPrivateEndpointCmd command = new CreateAiPrivateEndpointCmd();
        command._entityMgr = entities;
        set(command, "deployment", "llama-prod");
        set(command, "networkId", 7L);

        JsonObject body = JsonParser.parseString(command.engineBody()).getAsJsonObject();

        assertEquals("8f1c-net-uuid", body.get("network_id").getAsString());
    }

    @Test
    void aNetworkThatCannotBeResolvedIsRefused() throws Exception {
        EntityManager entities = mock(EntityManager.class);
        when(entities.findById(eq(Network.class), eq(7L))).thenReturn(null);

        CreateAiPrivateEndpointCmd command = new CreateAiPrivateEndpointCmd();
        command._entityMgr = entities;
        set(command, "deployment", "llama-prod");
        set(command, "networkId", 7L);

        assertThrows(InvalidParameterValueException.class, command::engineBody);
    }

    @Test
    void anUnsetNetworkIsOmittedRatherThanResolved() throws Exception {
        // A public-only endpoint names no network, and resolving null
        // would refuse a legitimate request.
        CreateAiPrivateEndpointCmd command = new CreateAiPrivateEndpointCmd();
        set(command, "deployment", "llama-prod");

        JsonObject body = JsonParser.parseString(command.engineBody()).getAsJsonObject();

        assertFalse(body.has("network_id"));
    }

    @Test
    void theParameterIsStillCalledNetworkid() throws Exception {
        // CloudMonkey, Terraform and the UI all name it; renaming it
        // while typing it would be a breaking change in disguise.
        Parameter annotation = parameterOn(CreateAiPrivateEndpointCmd.class, "networkId");

        assertEquals(ApiConstants.NETWORK_ID, annotation.name());
    }
}
