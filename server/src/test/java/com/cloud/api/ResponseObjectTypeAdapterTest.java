// Licensed to the Apache Software Foundation (ASF) under one
// or more contributor license agreements.  See the NOTICE file
// distributed with this work for additional information
// regarding copyright ownership.  The ASF licenses this file
// to you under the Apache License, Version 2.0 (the
// "License"); you may not use this file except in compliance
// with the License.  You may obtain a copy of the License at
//
//   http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing,
// software distributed under the License is distributed on an
// "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
// KIND, either express or implied.  See the License for the
// specific language governing permissions and limitations
// under the License.
package com.cloud.api;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.Map;

import org.apache.cloudstack.api.response.AccountResponse;
import org.apache.cloudstack.api.response.ExceptionResponse;
import org.apache.cloudstack.api.response.SuccessResponse;
import org.junit.Test;

import com.cloud.utils.exception.ExceptionProxyObject;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

public class ResponseObjectTypeAdapterTest {

    private final ResponseObjectTypeAdapter adapter = new ResponseObjectTypeAdapter();

    @Test
    public void testSerializeSuccessResponseWithDisplayText() {
        SuccessResponse response = new SuccessResponse("someapiresponse");
        response.setSuccess(true);
        response.setDisplayText("it worked");

        JsonElement result = adapter.serialize(response, SuccessResponse.class, null);

        JsonObject obj = result.getAsJsonObject();
        assertTrue(obj.get("success").getAsBoolean());
        assertEquals("it worked", obj.get("details").getAsString());
    }

    @Test
    public void testSerializeSuccessResponseWithoutDisplayTextOmitsDetails() {
        SuccessResponse response = new SuccessResponse("someapiresponse");
        response.setSuccess(false);

        JsonElement result = adapter.serialize(response, SuccessResponse.class, null);

        JsonObject obj = result.getAsJsonObject();
        assertFalse(obj.get("success").getAsBoolean());
        assertFalse("details should be omitted when there is no display text", obj.has("details"));
    }

    @Test
    public void testSerializeExceptionResponseIncludesKeyAndMetadataAndDropsUuidList() {
        ExceptionResponse response = new ExceptionResponse();
        response.setErrorCode(431);
        response.setErrorText("Unable to find network with ID abc");
        response.setErrorTextKey("vm.deploy.network.not.found");
        response.setErrorMetadata(Map.of("id", "abc"));
        response.addProxyObject(new ExceptionProxyObject("uuid-1", "networkId"));

        JsonElement result = adapter.serialize(response, ExceptionResponse.class, null);

        JsonObject obj = result.getAsJsonObject();
        assertEquals(431, obj.get("errorcode").getAsInt());
        assertEquals("Unable to find network with ID abc", obj.get("errortext").getAsString());
        assertEquals("vm.deploy.network.not.found", obj.get("errortextkey").getAsString());
        assertTrue(obj.has("errormetadata"));
        assertEquals("abc", obj.getAsJsonObject("errormetadata").get("id").getAsString());
        // the uuidList (idList) is explicitly stripped from the wire response by the adapter
        assertFalse("uuidList must be removed from the serialized ExceptionResponse", obj.has("uuidList"));
    }

    @Test
    public void testSerializeExceptionResponseWithoutKeySerializesCleanlyWithoutIt() {
        // an exception that was never given a structured key still serializes cleanly: Gson
        // omits null fields by default, so errortextkey/errormetadata are simply absent
        // rather than breaking serialization
        ExceptionResponse response = new ExceptionResponse();
        response.setErrorCode(530);
        response.setErrorText("Internal error");

        JsonElement result = adapter.serialize(response, ExceptionResponse.class, null);

        JsonObject obj = result.getAsJsonObject();
        assertEquals("Internal error", obj.get("errortext").getAsString());
        assertFalse(obj.has("errortextkey"));
        assertFalse(obj.has("errormetadata"));
    }

    @Test
    public void testSerializeOtherResponseWrapsUnderObjectName() {
        AccountResponse response = new AccountResponse();
        response.setObjectName("account");
        response.setId("account-uuid");

        JsonElement result = adapter.serialize(response, AccountResponse.class, null);

        JsonObject obj = result.getAsJsonObject();
        assertTrue(obj.has("account"));
        assertEquals("account-uuid", obj.getAsJsonObject("account").get("id").getAsString());
    }
}
