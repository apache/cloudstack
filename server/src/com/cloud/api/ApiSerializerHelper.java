/**
 *  Copyright (C) 2010 Cloud.com, Inc.  All rights reserved.
 * 
 * This software is licensed under the GNU General Public License v3 or later.
 * 
 * It is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or any later version.
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 * 
 */

package com.cloud.api;

import org.apache.log4j.Logger;

import com.google.gson.Gson;

public class ApiSerializerHelper {
    public static final Logger s_logger = Logger.getLogger(ApiSerializerHelper.class.getName());
    public static String token = "/";

    public static String toSerializedStringOld(Object result) {
        if (result != null) {
            Class<?> clz = result.getClass();
            Gson gson = ApiGsonHelper.getBuilder().create();

            if (result instanceof ResponseObject) {
                return clz.getName() + token + ((ResponseObject) result).getObjectName() + token + gson.toJson(result);
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
                    ((ResponseObject) obj).setObjectName(nameField);
                }
                return obj;
            }
            return null;
        } catch (RuntimeException e) {
            s_logger.error("Caught runtime exception when doing GSON deserialization on: " + result);
            throw e;
        }
    }
}
