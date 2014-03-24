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
package com.cloud.bridge.util;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;

/**
 *
 * For more complex cases (JsonArrays or other) it can be rewrite to matcher pattern
 */
public final class JsonElementUtil {

    private JsonElementUtil() {}

    public static JsonElement getAsJsonElement(final JsonElement jsonElement, final String... path) {
        JsonElement currentElement = jsonElement;
        for (final String propertyName : path) {
            if (currentElement == null) {
                return null;
            }
            if (jsonElement.isJsonObject()) {
                currentElement = ((JsonObject) currentElement).get(propertyName);
            } else {
                return null;
            }
        }
        return currentElement;
    }

    public static Integer getAsInt(final JsonElement jsonElement, final String... path) {
        final JsonElement targetElement = getAsJsonElement(jsonElement, path);
        if (targetElement == null || !targetElement.isJsonPrimitive()) {
            return null;
        }
        final JsonPrimitive asPrimitive = (JsonPrimitive) targetElement;
        return asPrimitive.getAsInt();
    }

    public static String getAsString(final JsonElement jsonElement, final String... path) {
        final JsonElement targetElement = getAsJsonElement(jsonElement, path);
        return targetElement == null ? null : targetElement.getAsString();
    }

}
