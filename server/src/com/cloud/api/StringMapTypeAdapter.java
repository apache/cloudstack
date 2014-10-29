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
package com.cloud.api;

import java.lang.reflect.Type;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;

@SuppressWarnings("rawtypes")
public class StringMapTypeAdapter implements JsonDeserializer<Map> {
    @Override
    public Map deserialize(JsonElement src, Type srcType, JsonDeserializationContext context) throws JsonParseException {

        Map<String, String> obj = new HashMap<String, String>();
        JsonObject json = src.getAsJsonObject();

        for (Entry<String, JsonElement> entry : json.entrySet()) {
            obj.put(entry.getKey(), entry.getValue().getAsString());
        }

        return obj;
    }
}
