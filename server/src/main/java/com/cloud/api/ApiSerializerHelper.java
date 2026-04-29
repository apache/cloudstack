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

import java.util.HashMap;
import java.util.Map;

import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.LogManager;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.gson.Gson;

import org.apache.cloudstack.api.ResponseObject;

public class ApiSerializerHelper {
    protected static Logger LOGGER = LogManager.getLogger(ApiSerializerHelper.class);
    private static String token = "/";

    public static String toSerializedString(Object result) {
        if (result != null) {
            Class<?> clz = result.getClass();
            Gson gson = ApiGsonHelper.getBuilder().create();

            if (result instanceof ResponseObject) {
                return clz.getName() + token + ((ResponseObject)result).getObjectName() + token + gson.toJson(result);
            } else {
                return clz.getName() + token + gson.toJson(result);
            }
        }
        return null;
    }

    public static Object fromSerializedString(String result) {
        try {
            if (result != null && !result.isEmpty()) {
                String[] serializedParts = result.split(token);

                if (serializedParts.length < 2) {
                    return null;
                }
                String clzName = serializedParts[0];
                String nameField = null;
                String content = null;
                if (serializedParts.length == 2) {
                    content = serializedParts[1];
                } else {
                    nameField = serializedParts[1];
                    int index = result.indexOf(token + nameField + token);
                    content = result.substring(index + nameField.length() + 2);
                }

                Class<?> clz;
                try {
                    clz = Class.forName(clzName);
                } catch (ClassNotFoundException e) {
                    return null;
                }

                Gson gson = ApiGsonHelper.getBuilder().create();
                Object obj = gson.fromJson(content, clz);
                if (nameField != null) {
                    ((ResponseObject)obj).setObjectName(nameField);
                }
                return obj;
            }
            return null;
        } catch (RuntimeException e) {
            LOGGER.error("Caught runtime exception when doing GSON deserialization on: " + result);
            throw e;
        }
    }

    public static Map<String, Object> fromSerializedStringToMap(String result) {
        Map<String,Object> objParams = null;
        try {
            Object obj = fromSerializedString(result);
            if (obj != null) {
                Gson gson = ApiGsonHelper.getBuilder().create();
                String objJson = gson.toJson(obj);
                objParams = new ObjectMapper().readValue(objJson, HashMap.class);
                objParams.put("class", obj.getClass().getName());

                String nameField = ((ResponseObject)obj).getObjectName();
                if (nameField != null) {
                    objParams.put("object", nameField);
                }
            }
        } catch (RuntimeException | JsonProcessingException e) {
            LOGGER.error("Caught runtime exception when doing GSON deserialization to map on: " + result, e);
        }

        return objParams;
    }
}
