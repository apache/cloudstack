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

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import com.cloud.utils.exception.CloudRuntimeException;
import org.apache.log4j.Logger;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.gson.Gson;

import org.apache.cloudstack.api.ResponseObject;

public class ApiSerializerHelper {
    public static final Logger s_logger = Logger.getLogger(ApiSerializerHelper.class.getName());
    private static String token = "/";
    private static String[] apiPackages = {"com.cloud.agent.api", "org.apache.cloudstack.api"};

    /**
     * serialise an object's class to a {@link String}
     * @param inputObject
     * @return
     */
    public static String toSerializedString(Object inputObject) {
        if (inputObject != null) {
            Class<?> clz = inputObject.getClass();
            Gson gson = ApiGsonHelper.getBuilder().create();

            if (inputObject instanceof ResponseObject) {
                return clz.getName() + token + ((ResponseObject)inputObject).getObjectName() + token + gson.toJson(inputObject);
            } else {
                return clz.getName() + token + gson.toJson(inputObject);
            }
        }
        return null;
    }

    /**
     * deserialise an {@link Object} from a {@link String}
     *
     * @param classString the string representation of the {@link Class} instance to create
     * @return an {@link Object} of the requested {@link Class} type
     */
    public static Object fromSerializedString(String classString) {
        try {
            if (classString != null && !classString.isEmpty()) {
                String[] serializedParts = classString.split(token);

                if (serializedParts.length < 2) {
                    return null;
                }
                String clzName = serializedParts[0];
                validateClassPath(clzName, apiPackages);
                String nameField = null;
                String content = null;
                if (serializedParts.length == 2) {
                    content = serializedParts[1];
                } else {
                    nameField = serializedParts[1];
                    int index = classString.indexOf(token + nameField + token);
                    content = classString.substring(index + nameField.length() + 2);
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
            s_logger.error("Caught runtime exception when doing GSON deserialization on: " + classString);
            throw e;
        }
    }

    /**
     * validates that a class is allowed to be deserialised.
     *
     * TODO move to a more globally accesible util class
     * TODO make generic for allow lists
     * TODO extend to return the desired class
     *
     * @param clzName
     * @param basePaths
     */
    private static void validateClassPath(String clzName, String[] basePaths) {
        if (clzName != null) {
            for (String basePath : basePaths) {
                if (clzName.startsWith(basePath)) {
                    return;
                }
            }
        }
        String packages = Arrays.toString(basePaths);
        throw new CloudRuntimeException(String.format("illegal to load \"%s\" forName, only classes in packages \"%s\" are allowed", clzName, packages));
    }

    /**
     * deserialise a map
     *
     * @param mapRepresentation the {@link String} representation of the {@link Map<>} type
     * @return the {@link Map<>} object
     */
    public static Map<String, Object> fromSerializedStringToMap(String mapRepresentation) {
        Map<String,Object> objParams = null;
        try {
            Object obj = fromSerializedString(mapRepresentation);
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
            s_logger.error("Caught runtime exception when doing GSON deserialization to map on: " + mapRepresentation, e);
        }

        return objParams;
    }
}
