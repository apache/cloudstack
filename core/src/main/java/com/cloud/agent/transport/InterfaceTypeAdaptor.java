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

package com.cloud.agent.transport;

import java.lang.reflect.Type;
import java.util.Map;

import com.google.gson.Gson;
import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.JsonSerializationContext;
import com.google.gson.JsonSerializer;

import com.cloud.utils.exception.CloudRuntimeException;

public class InterfaceTypeAdaptor<T> implements JsonDeserializer<T>, JsonSerializer<T> {

    protected Gson _gson = null;

    public InterfaceTypeAdaptor() {
    }

    public void initGson(Gson gson) {
        _gson = gson;
    }

    @Override
    public JsonElement serialize(T src, Type typeOfSrc, JsonSerializationContext context) {
        JsonObject obj = new JsonObject();
        obj.add(src.getClass().getName(), _gson.toJsonTree(src));
        return obj;
    }

    @Override
    @SuppressWarnings("unchecked")
    public T deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context) throws JsonParseException {
        JsonObject element = (JsonObject)json;
        Map.Entry<String, JsonElement> entry = element.entrySet().iterator().next();
        String name = entry.getKey();
        Class<?> clazz;
        try {
            clazz = Class.forName(name);
        } catch (ClassNotFoundException e) {
            throw new CloudRuntimeException("can't find " + name);
        }
        return (T)_gson.fromJson(entry.getValue(), clazz);
    }
}
