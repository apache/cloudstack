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
package com.cloud.agent.transport.compat;

import com.cloud.utils.StringUtils;
import com.cloud.utils.exception.CloudRuntimeException;
import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonSerializationContext;
import com.google.gson.JsonSerializer;

import java.lang.reflect.Type;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * JSON serializer adapter for transport classes (com.cloud.agent.api.to.*) that ensures backward compatibility
 * with older Agent versions due to rename of the fields
 * (see https://github.com/apache/cloudstack/pull/10514)
 *
 * This class does not build its own Gson instance: doing so would silently drop whichever exclusion
 * strategy (e.g. log redaction) and sibling compat adaptors (for nested TOs) the enclosing Gson was
 * configured with. Instead, whoever registers an instance of this class into a GsonBuilder is
 * responsible for also calling {@link #initGson(Gson)} with a Gson that (a) carries that same
 * exclusion strategy and (b) has adapters registered for any nested TO types that also need field
 * renaming, but not for this adaptor's own type (to avoid infinite recursion). See
 * {@link com.cloud.serializer.GsonHelper#setDefaultGsonConfig(com.google.gson.GsonBuilder)}.
 */
public class AbstractTOAdaptor<T> implements JsonSerializer<T> {
    private Gson gson;
    private Map<String, String> fieldMappings;

    protected AbstractTOAdaptor(String... fields) {
        this.fieldMappings = new LinkedHashMap<>();
        for (int i = 0; i + 1 < fields.length; i += 2) {
            String sourceField = fields[i];
            String destinationField = fields[i + 1];
            // skip empty fields
            if (StringUtils.isBlank(sourceField) || StringUtils.isBlank(destinationField)) {
                continue;
            }
            this.fieldMappings.put(sourceField, destinationField);
        }
        if (this.fieldMappings.isEmpty()) {
            throw new CloudRuntimeException("Field mappings must not be empty");
        }
    }

    public void initGson(Gson gson) {
        this.gson = gson;
    }

    @Override
    public JsonElement serialize(T src, Type typeOfSrc, JsonSerializationContext context) {
        if (src == null) {
            return null;
        }
        JsonElement tree = gson.toJsonTree(src);
        if (tree.isJsonObject()) {
            JsonObject obj = tree.getAsJsonObject();
            for (Map.Entry<String, String> field : fieldMappings.entrySet()) {
                String sourceField = field.getKey();
                String destinationField = field.getValue();
                if (obj.has(sourceField)) {
                    obj.add(destinationField, obj.get(sourceField));
                }
            }
        }
        return tree;
    }
}
