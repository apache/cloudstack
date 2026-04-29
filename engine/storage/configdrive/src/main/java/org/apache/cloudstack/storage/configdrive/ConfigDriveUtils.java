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

package org.apache.cloudstack.storage.configdrive;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

public class ConfigDriveUtils {

    static void mergeJsonArraysAndUpdateObject(JsonObject finalObject, JsonObject newObj, String memberName, String... keys) {
        JsonArray existingMembers = finalObject.has(memberName) ? finalObject.get(memberName).getAsJsonArray() : new JsonArray();
        JsonArray newMembers = newObj.has(memberName) ? newObj.get(memberName).getAsJsonArray() : new JsonArray();

        if (existingMembers.size() > 0 || newMembers.size() > 0) {
            JsonArray finalMembers = new JsonArray();
            Set<String> idSet = new HashSet<>();
            for (JsonElement element : existingMembers.getAsJsonArray()) {
                JsonObject elementObject = element.getAsJsonObject();
                String key = Arrays.stream(keys).map(elementObject::get).map(JsonElement::getAsString).reduce((a, b) -> a + "-" + b).orElse("");
                idSet.add(key);
                finalMembers.add(element);
            }
            for (JsonElement element : newMembers.getAsJsonArray()) {
                JsonObject elementObject = element.getAsJsonObject();
                String key = Arrays.stream(keys).map(elementObject::get).map(JsonElement::getAsString).reduce((a, b) -> a + "-" + b).orElse("");
                if (!idSet.contains(key)) {
                    finalMembers.add(element);
                }
            }
            finalObject.add(memberName, finalMembers);
        }
    }

}
