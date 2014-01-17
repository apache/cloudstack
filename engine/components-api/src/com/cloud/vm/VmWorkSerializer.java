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
package com.cloud.vm;

import java.lang.reflect.Type;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;

import org.apache.cloudstack.framework.jobs.impl.JobSerializerHelper;

public class VmWorkSerializer {
    static class StringMapTypeAdapter implements JsonDeserializer<Map> {

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

    protected static Gson s_gson;
    static {
        GsonBuilder gBuilder = new GsonBuilder();
        gBuilder.setVersion(1.3);
        gBuilder.registerTypeAdapter(Map.class, new StringMapTypeAdapter());
        s_gson = gBuilder.create();
    }

    public static String serialize(VmWork work) {
        // TODO: there are way many generics, too tedious to get serialization work under GSON
        // use java binary serialization instead
        //
        return JobSerializerHelper.toObjectSerializedString(work);
        // return s_gson.toJson(work);
    }

    public static <T extends VmWork> T deserialize(Class<?> clazz, String workInJsonText) {
        // TODO: there are way many generics, too tedious to get serialization work under GSON
        // use java binary serialization instead
        //
        return (T)JobSerializerHelper.fromObjectSerializedString(workInJsonText);
        // return (T)s_gson.fromJson(workInJsonText, clazz);
    }
}
