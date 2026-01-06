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
package com.cloud.agent.transport;

import com.cloud.storage.Storage.StoragePoolType;
import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonNull;
import com.google.gson.JsonParseException;
import com.google.gson.JsonPrimitive;
import com.google.gson.JsonSerializationContext;
import com.google.gson.JsonSerializer;

import java.lang.reflect.Type;

/**
 * {@link StoragePoolType} acts as extendable set of singleton objects and should return same result when used "=="
 * or {@link Object#equals(Object)}.
 * To support that, need to return existing object for a given name instead of creating new.
 */
public class StoragePoolTypeAdaptor implements JsonDeserializer<StoragePoolType>, JsonSerializer<StoragePoolType> {
    @Override
    public StoragePoolType deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context) throws JsonParseException {
        if (json instanceof JsonPrimitive && ((JsonPrimitive) json).isString()) {
            return StoragePoolType.valueOf(json.getAsString());
        }
        return null;
    }

    @Override
    public JsonElement serialize(StoragePoolType src, Type typeOfSrc, JsonSerializationContext context) {
        String name = src.name();
        if (name == null) {
            return new JsonNull();
        }
        return new JsonPrimitive(name);
    }
}
