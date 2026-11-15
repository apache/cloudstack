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
package org.apache.cloudstack.gui.theme.json.config.validator.attributes;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Specific validator for the "userCard" object within the GUI theme JSON configuration.
 *
 * <p>
 * This component is defined as a bean in the Spring XML configuration and is automatically injected into
 * the {@link org.apache.cloudstack.gui.theme.json.config.validator.JsonConfigValidator} attribute list.
 * </p>
 */
public class UserCardAttribute extends AttributeBase {
    private static final List<String> ALLOWED_USER_CARD_LINKS_PROPERTIES = List.of("title", "text", "link", "icon");
    private static final String LINKS = "links";

    @Override
    protected String getAttributeName() {
        return "userCard";
    }

    @Override
    protected List<String> getAllowedProperties() {
        return List.of("title", "icon", "links");
    }

    @Override
    public void validate(Map.Entry<String, JsonElement> entry, JsonObject jsonObject) {
        if (!getAttributeName().equals(entry.getKey())) {
            return;
        }

        Set<Map.Entry<String, JsonElement>> entries = entry.getValue().getAsJsonObject().entrySet();
        JsonObject objectToBeAdded = new JsonObject();
        for (Map.Entry<String, JsonElement> recursiveEntry : entries) {
            String entryKey = recursiveEntry.getKey();

            if (!getAllowedProperties().contains(entryKey)) {
                warnOfInvalidJsonAttribute(entryKey);
                continue;
            }

            if (LINKS.equals(entryKey)) {
                createLinkJsonObject(recursiveEntry, jsonObject);
            }

            objectToBeAdded.add(entryKey, recursiveEntry.getValue());
        }
    }

    private void createLinkJsonObject(Map.Entry<String, JsonElement> entry, JsonObject jsonObject) {
        JsonArray jsonArrayResult = new JsonArray();
        JsonArray sourceJsonArray = entry.getValue().getAsJsonArray();
        for (JsonElement jsonElement : sourceJsonArray) {
            Set<Map.Entry<String, JsonElement>> linkEntries = jsonElement.getAsJsonObject().entrySet();
            JsonObject linkObjectToBeAdded = createJsonObject(linkEntries, ALLOWED_USER_CARD_LINKS_PROPERTIES);

            if (linkObjectToBeAdded.entrySet().isEmpty()) {
                return;
            }

            jsonArrayResult.add(linkObjectToBeAdded);
        }
        jsonObject.add(entry.getKey(), jsonArrayResult);
    }
}
