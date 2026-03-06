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

public class PluginsAttribute extends AttributeBase {

    @Override
    protected String getAttributeName() {
        return "plugins";
    }

    @Override
    protected List<String> getAllowedProperties() {
        return List.of("name", "path", "icon", "isExternalLink");
    }

    @Override
    public void validate(Map.Entry<String, JsonElement> entry, JsonObject jsonObject) {
        if (!getAttributeName().equals(entry.getKey())) {
            return;
        }

        JsonArray jsonArrayResult = new JsonArray();
        JsonArray sourceJsonArray = entry.getValue().getAsJsonArray();
        for (JsonElement jsonElement : sourceJsonArray) {
            Set<Map.Entry<String, JsonElement>> pluginEntries = jsonElement.getAsJsonObject().entrySet();
            JsonObject pluginObjectToBeAdded = createJsonObject(pluginEntries, getAllowedProperties());

            if (pluginObjectToBeAdded.entrySet().isEmpty()) {
                return;
            }

            jsonArrayResult.add(pluginObjectToBeAdded);
        }

        jsonObject.add(entry.getKey(), jsonArrayResult);
    }
}
