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

import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.LogManager;

import com.google.gson.JsonElement;
import com.google.gson.JsonPrimitive;
import com.google.gson.JsonSerializationContext;
import com.google.gson.JsonSerializer;

import com.cloud.utils.encoding.URLEncoder;

public class EncodedStringTypeAdapter implements JsonSerializer<String> {
    protected Logger logger = LogManager.getLogger(getClass());

    @Override
    public JsonElement serialize(String src, Type typeOfResponseObj, JsonSerializationContext ctx) {
        return new JsonPrimitive(encodeString(src));

    }

    private String encodeString(String value) {
        if (!ApiServer.isEncodeApiResponse()) {
            return value;
        }
        try {
            return new URLEncoder().encode(value).replaceAll("\\+", "%20");
        } catch (Exception e) {
            logger.warn("Unable to encode: " + value, e);
        }
        return value;
    }

}
