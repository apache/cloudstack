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

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import org.apache.cloudstack.gui.theme.json.config.validator.JsonConfigAttributeValidator;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.List;
import java.util.Map;
import java.util.Set;

public abstract class AttributeBase implements JsonConfigAttributeValidator {
    protected Logger logger = LogManager.getLogger(getClass());

    protected abstract String getAttributeName();
    protected abstract List<String> getAllowedProperties();

    @Override
    public void validate(Map.Entry<String, JsonElement> entry, JsonObject jsonObject) {
        if (!getAttributeName().equals(entry.getKey())) {
            return;
        }

        Set<Map.Entry<String, JsonElement>> entries = entry.getValue().getAsJsonObject().entrySet();
        JsonObject objectToBeAdded = createJsonObject(entries, getAllowedProperties());

        if (!objectToBeAdded.entrySet().isEmpty()) {
            jsonObject.add(entry.getKey(), objectToBeAdded);
        }
    }

    /**
     * Creates a {@link JsonObject} with only the valid options for the attribute properties specified in the allowedProperties parameter.
     */
    public JsonObject createJsonObject(Set<Map.Entry<String, JsonElement>> entries, List<String> allowedProperties) {
        JsonObject objectToBeAdded = new JsonObject();

        for (Map.Entry<String, JsonElement> recursiveEntry : entries) {
            String entryKey = recursiveEntry.getKey();

            if (!allowedProperties.contains(entryKey)) {
                warnOfInvalidJsonAttribute(entryKey);
                continue;
            }
            objectToBeAdded.add(entryKey, recursiveEntry.getValue());
        }

        logger.trace("JSON object with valid options: {}.", objectToBeAdded);
        return objectToBeAdded;
    }

    protected void warnOfInvalidJsonAttribute(String entryKey) {
        logger.warn("The JSON attribute [{}] is not a valid option, therefore, it will be ignored.", entryKey);
    }
}
