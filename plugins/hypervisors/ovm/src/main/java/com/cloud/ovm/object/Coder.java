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
package com.cloud.ovm.object;

import java.lang.reflect.Type;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.reflect.TypeToken;

public class Coder {
    private static Gson s_gson;
    private static Gson s_mapGson;

    public static Object[] s_emptyParams = new Object[0];

    private static class MapDecoder implements JsonDeserializer<Map<String, String>> {
        @Override
        public Map<String, String> deserialize(JsonElement arg0, Type arg1, JsonDeserializationContext arg2) throws JsonParseException {
            if (!arg0.isJsonObject()) {
                throw new JsonParseException(arg0.toString() + " is not Json Object, cannot convert to map");
            }
            JsonObject objs = arg0.getAsJsonObject();
            Map<String, String> map = new HashMap<String, String>();
            for (Entry<String, JsonElement> e : objs.entrySet()) {
                if (!e.getValue().isJsonPrimitive()) {
                    throw new JsonParseException(e.getValue().toString() + " is not a Json primitive," + arg0 + " can not convert to map");
                }
                map.put(e.getKey(), e.getValue().getAsString());
            }
            return map;
        }

    }

    static {
        GsonBuilder _builder = new GsonBuilder();
        _builder.registerTypeAdapter(Map.class, new MapDecoder());
        s_mapGson = _builder.create();
        s_gson = new Gson();
    }

    public static String toJson(Object obj) {
        return s_gson.toJson(obj);
    }

    public static <T> T fromJson(String str, Class<T> clz) {
        return s_gson.fromJson(str, clz);
    }

    @SuppressWarnings("unchecked")
    public static Map<String, String> mapFromJson(String str) {
        return s_mapGson.fromJson(str, Map.class);
    }

    public static List<String> listFromJson(String str) {
        Type listType = new TypeToken<List<String>>() {
        }.getType();
        return s_gson.fromJson(str, listType);
    }
}
