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
package org.apache.cloudstack.gui.theme.json.config.validator;

import com.cloud.utils.exception.CloudRuntimeException;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.JsonSyntaxException;
import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.inject.Inject;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class JsonConfigValidator {
    protected Logger logger = LogManager.getLogger(getClass());

    private static final List<String> ALLOWED_PRIMITIVE_PROPERTIES = List.of("appTitle", "footer", "loginFooter", "logo", "minilogo", "banner", "docBase", "apidocs");
    private static final List<String> ALLOWED_DYNAMIC_PROPERTIES = List.of("error", "theme", "plugins", "keyboardOptions", "userCard", "docHelpMappings");

    @Inject
    private List<JsonConfigAttributeValidator> attributes;

    public void validateJsonConfiguration(String jsonConfig) {
        if (StringUtils.isBlank(jsonConfig)) {
            return;
        }

        JsonObject jsonObject = new JsonObject();

        try {
            JsonElement jsonElement = JsonParser.parseString(jsonConfig);
            Set<Map.Entry<String, JsonElement>> entries = jsonElement.getAsJsonObject().entrySet();
            entries.forEach(entry -> validateJsonAttributes(entry, jsonObject));
        } catch (JsonSyntaxException exception) {
            logger.error("The following exception was thrown while parsing the JSON object: [{}].", exception.getMessage());
            throw new CloudRuntimeException("Specified JSON configuration is not a valid JSON object.");
        }
    }

    /**
     * Validates the informed JSON attributes considering the allowed properties by the API, any invalid option is ignored.
     * All valid options are added to a {@link JsonObject} that will be considered as the final JSON configuration used by the GUI theme.
     */
    private void validateJsonAttributes(Map.Entry<String, JsonElement> entry, JsonObject jsonObject) {
        JsonElement entryValue = entry.getValue();
        String entryKey = entry.getKey();

        if (entryValue.isJsonPrimitive() && ALLOWED_PRIMITIVE_PROPERTIES.contains(entryKey)) {
            logger.trace("The JSON attribute [{}] is a valid option.", entryKey);
            jsonObject.add(entryKey, entryValue);
        } else if (ALLOWED_DYNAMIC_PROPERTIES.contains(entryKey)) {
            attributes.forEach(attribute -> attribute.validate(entry, jsonObject));
        } else {
            logger.warn("The JSON attribute [{}] is not a valid option, therefore, it will be ignored.", entryKey);
        }
    }
}
