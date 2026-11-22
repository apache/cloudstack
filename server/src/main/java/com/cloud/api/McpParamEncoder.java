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

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

/**
 * Converts the JSON arguments of an MCP tool call into CloudStack API parameters, using the same encoding as the query string of the API:
 * lists are comma separated values and maps are <code>name[index].field=value</code> entries.
 */
public final class McpParamEncoder {
    private static final Set<String> RESERVED_PARAMETERS = Set.of("command", "response", "apikey", "signature", "signatureversion", "expires",
            "sessionkey", "httpmethod");

    private McpParamEncoder() {
    }

    /**
     * @throws IllegalArgumentException if a parameter is reserved or has a structure that the API cannot represent.
     */
    public static Map<String, String> encode(JsonObject parameters) {
        Map<String, String> encoded = new LinkedHashMap<>();
        for (Map.Entry<String, JsonElement> parameter : parameters.entrySet()) {
            String name = parameter.getKey();
            JsonElement value = parameter.getValue();
            if (RESERVED_PARAMETERS.contains(name.toLowerCase())) {
                throw new IllegalArgumentException(String.format("Parameter [%s] is reserved and cannot be informed.", name));
            }

            if (value.isJsonNull()) {
                continue;
            }
            if (value.isJsonPrimitive()) {
                encoded.put(name, value.getAsString());
            } else if (value.isJsonObject()) {
                encodeMapEntry(encoded, name, 0, value);
            } else {
                encodeArray(encoded, name, value.getAsJsonArray());
            }
        }
        return encoded;
    }

    private static void encodeArray(Map<String, String> encoded, String name, JsonArray array) {
        if (array.size() == 0) {
            return;
        }
        if (array.get(0).isJsonObject()) {
            for (int index = 0; index < array.size(); index++) {
                encodeMapEntry(encoded, name, index, array.get(index));
            }
            return;
        }

        List<String> values = new ArrayList<>();
        for (JsonElement element : array) {
            values.add(getPrimitive(name, element));
        }
        encoded.put(name, String.join(",", values));
    }

    private static void encodeMapEntry(Map<String, String> encoded, String name, int index, JsonElement entry) {
        if (!entry.isJsonObject()) {
            throw new IllegalArgumentException(String.format("Parameter [%s] mixes objects and plain values, which is not supported.", name));
        }
        for (Map.Entry<String, JsonElement> field : entry.getAsJsonObject().entrySet()) {
            if (!field.getValue().isJsonNull()) {
                encoded.put(String.format("%s[%d].%s", name, index, field.getKey()), getPrimitive(name, field.getValue()));
            }
        }
    }

    private static String getPrimitive(String name, JsonElement value) {
        if (!value.isJsonPrimitive()) {
            throw new IllegalArgumentException(String.format("Parameter [%s] is nested deeper than the API supports: use a plain value, " +
                    "a list of plain values, an object of plain values or a list of such objects.", name));
        }
        return value.getAsString();
    }
}
