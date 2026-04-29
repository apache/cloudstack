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

package com.cloud.utils.json;


import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

public class JsonMergeUtil {

    private static final JsonParser parser = new JsonParser();

    public static String mergeJsonPatch(String originalJsonStr, String patchJsonStr) {
        JsonObject originalJson = parser.parse(originalJsonStr).getAsJsonObject();
        JsonObject patchJson = parser.parse(patchJsonStr).getAsJsonObject();
        mergeJson(originalJson, patchJson);
        return originalJson.toString();
    }

    private static void mergeJson(JsonObject target, JsonObject patch) {
        Set<Map.Entry<String, JsonElement>> entries = patch.entrySet();
        for (Map.Entry<String, JsonElement> entry : entries) {
            String key = entry.getKey();
            JsonElement patchValue = entry.getValue();

            if (!target.has(key) || patchValue.isJsonNull()) {
                target.add(key, patchValue);
                continue;
            }

            JsonElement targetValue = target.get(key);

            if (targetValue.isJsonObject() && patchValue.isJsonObject()) {
                mergeJson(targetValue.getAsJsonObject(), patchValue.getAsJsonObject());
            } else if (targetValue.isJsonArray() && patchValue.isJsonArray()) {
                JsonArray merged = tryMergeArrayByUuid(targetValue.getAsJsonArray(), patchValue.getAsJsonArray());
                target.add(key, merged);
            } else {
                target.add(key, patchValue); // primitive or incompatible
            }
        }
    }

    private static JsonArray tryMergeArrayByUuid(JsonArray original, JsonArray patch) {
        if (anyElementNotHaveUuid(original) || anyElementNotHaveUuid(patch)) {
            return patch;
        }

        Map<String, JsonObject> originalMap = new HashMap<>();
        for (int i = 0; i < original.size(); i++) {
            JsonObject obj = original.get(i).getAsJsonObject();
            String uuid = obj.get("uuid").getAsString();
            originalMap.put(uuid, cloneJsonObject(obj));
        }

        for (int i = 0; i < patch.size(); i++) {
            JsonObject patchObj = patch.get(i).getAsJsonObject();
            String uuid = patchObj.get("uuid").getAsString();
            if (originalMap.containsKey(uuid)) {
                mergeJson(originalMap.get(uuid), patchObj);
            } else {
                originalMap.put(uuid, cloneJsonObject(patchObj));
            }
        }

        JsonArray result = new JsonArray();
        for (JsonObject obj : originalMap.values()) {
            result.add(obj);
        }
        return result;
    }

    private static boolean anyElementNotHaveUuid(JsonArray array) {
        for (int i = 0; i < array.size(); i++) {
            JsonElement e = array.get(i);
            if (!e.isJsonObject()) return true;
            JsonObject obj = e.getAsJsonObject();
            if (!obj.has("uuid")) return true;
        }
        return false;
    }

    private static JsonObject cloneJsonObject(JsonObject original) {
        // Gson 1.7.2 does not have deepCopy; use serialize+deserialize
        return parser.parse(original.toString()).getAsJsonObject();
    }
}
