//
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
//

package com.cloud.network.nicira;

import java.lang.reflect.Type;

import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;

public class RoutingConfigAdapter implements JsonDeserializer<RoutingConfig> {

    private static final String ROUTING_TABLE_ROUTING_CONFIG = "RoutingTableRoutingConfig";
    private static final String SINGLE_DEFAULT_ROUTE_IMPLICIT_ROUTING_CONFIG = "SingleDefaultRouteImplicitRoutingConfig";

    @Override
    public RoutingConfig deserialize(final JsonElement jsonElement, final Type type, final JsonDeserializationContext context) throws JsonParseException {
        final JsonObject jsonObject = jsonElement.getAsJsonObject();

        if (!jsonObject.has("type")) {
            throw new JsonParseException("Deserializing as a RoutingConfig, but no type present in the json object");
        }

        final String routingConfigType = jsonObject.get("type").getAsString();
        if (SINGLE_DEFAULT_ROUTE_IMPLICIT_ROUTING_CONFIG.equals(routingConfigType)) {
            return context.deserialize(jsonElement, SingleDefaultRouteImplicitRoutingConfig.class);
        } else if (ROUTING_TABLE_ROUTING_CONFIG.equals(routingConfigType)) {
            return context.deserialize(jsonElement, RoutingTableRoutingConfig.class);
        }

        throw new JsonParseException("Failed to deserialize type \"" + routingConfigType + "\"");
    }
}
