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

import com.cloud.agent.api.Answer;
import com.cloud.agent.api.Command;
import com.cloud.agent.api.SecStorageFirewallCfgCommand;
import com.cloud.agent.api.to.DataStoreTO;
import com.cloud.agent.api.to.DataTO;
import com.cloud.agent.transport.ArrayTypeAdaptor;
import com.cloud.agent.transport.InterfaceTypeAdaptor;
import com.cloud.agent.transport.LoggingExclusionStrategy;
import com.cloud.agent.transport.Request;
import com.cloud.agent.transport.StoragePoolTypeAdaptor;
import com.cloud.hypervisor.Hypervisor;
import com.cloud.storage.Storage;
import com.cloud.utils.Pair;
import com.cloud.utils.StringUtils;
import com.cloud.utils.exception.CloudRuntimeException;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonSerializationContext;
import com.google.gson.JsonSerializer;
import com.google.gson.reflect.TypeToken;
import org.apache.cloudstack.transport.HypervisorTypeAdaptor;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.LogManager;

import java.lang.reflect.Type;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * JSON serializer adapter for transport classes (com.cloud.agent.api.to.*) that ensures backward compatibility
 * with older Agent versions due to rename of the fields
 * (see https://github.com/shapeblue/cloudstack-apple/pull/532/changes)
 */

public class AbstractTOAdaptor<T> implements JsonSerializer<T> {
    private static final Logger LOGGER = LogManager.getLogger(AbstractTOAdaptor.class);
    private static final Gson gson;

    static {
        GsonBuilder gsonBuilder = new GsonBuilder();
        gson = setDefaultGsonConfig(gsonBuilder);
        GsonBuilder loggerBuilder = new GsonBuilder();
        loggerBuilder.disableHtmlEscaping();
        loggerBuilder.setExclusionStrategies(new LoggingExclusionStrategy(LOGGER));
    }

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

    private static Gson setDefaultGsonConfig(GsonBuilder builder) {
        builder.setVersion(1.5);
        InterfaceTypeAdaptor<DataStoreTO> dsAdaptor = new InterfaceTypeAdaptor<DataStoreTO>();
        builder.registerTypeAdapter(DataStoreTO.class, dsAdaptor);
        InterfaceTypeAdaptor<DataTO> dtAdaptor = new InterfaceTypeAdaptor<DataTO>();
        builder.registerTypeAdapter(DataTO.class, dtAdaptor);
        ArrayTypeAdaptor<Command> cmdAdaptor = new ArrayTypeAdaptor<Command>();
        builder.registerTypeAdapter(Command[].class, cmdAdaptor);
        ArrayTypeAdaptor<Answer> ansAdaptor = new ArrayTypeAdaptor<Answer>();
        builder.registerTypeAdapter(Answer[].class, ansAdaptor);
        builder.registerTypeAdapter(new TypeToken<List<SecStorageFirewallCfgCommand.PortConfig>>() {
        }.getType(), new Request.PortConfigListTypeAdaptor());
        builder.registerTypeAdapter(new TypeToken<Pair<Long, Long>>() {
        }.getType(), new Request.NwGroupsCommandTypeAdaptor());
        builder.registerTypeAdapter(Storage.StoragePoolType.class, new StoragePoolTypeAdaptor());
        builder.registerTypeAdapter(Hypervisor.HypervisorType.class, new HypervisorTypeAdaptor());

        Gson gson = builder.create();
        dsAdaptor.initGson(gson);
        dtAdaptor.initGson(gson);
        cmdAdaptor.initGson(gson);
        ansAdaptor.initGson(gson);
        return gson;
    }

    @Override
    public JsonElement serialize(T src, Type typeOfSrc, JsonSerializationContext context) {
        if (src == null) {
            return null;
        }
        JsonObject obj = gson.toJsonTree(src).getAsJsonObject();
        if (obj != null) {
            for (Map.Entry<String, String> field : fieldMappings.entrySet()) {
                String sourceField = field.getKey();
                String destinationField = field.getValue();
                if (obj.has(sourceField)) {
                    obj.add(destinationField, obj.get(sourceField));
                }
            }
        }
        return obj;
    }
}
