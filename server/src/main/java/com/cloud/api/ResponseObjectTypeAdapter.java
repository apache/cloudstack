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

import java.lang.reflect.Method;
import java.lang.reflect.Type;

import org.apache.cloudstack.api.ResponseObject;
import org.apache.cloudstack.api.response.ExceptionResponse;
import org.apache.cloudstack.api.response.SuccessResponse;
import org.apache.commons.lang3.StringUtils;
import org.apache.log4j.Logger;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonSerializationContext;
import com.google.gson.JsonSerializer;

public class ResponseObjectTypeAdapter implements JsonSerializer<ResponseObject> {
    public static final Logger s_logger = Logger.getLogger(ResponseObjectTypeAdapter.class.getName());

    @Override
    public JsonElement serialize(ResponseObject responseObj, Type typeOfResponseObj, JsonSerializationContext ctx) {
        JsonObject obj = new JsonObject();

        if (responseObj instanceof SuccessResponse) {
            obj.addProperty("success", ((SuccessResponse)responseObj).getSuccess());
            if (!StringUtils.isEmpty(((SuccessResponse) responseObj).getDisplayText())) {
                obj.addProperty("details", ((SuccessResponse)responseObj).getDisplayText());
            }
            return obj;
        } else if (responseObj instanceof ExceptionResponse) {
            obj.addProperty("errorcode", ((ExceptionResponse)responseObj).getErrorCode());
            obj.addProperty("errortext", ((ExceptionResponse)responseObj).getErrorText());
            return obj;
        } else {
            obj.add(responseObj.getObjectName(), ApiResponseGsonHelper.getBuilder().create().toJsonTree(responseObj));
            return obj;
        }
    }

    private static Method getGetMethod(Object o, String propName) {
        Method method = null;
        String methodName = getGetMethodName("get", propName);
        try {
            method = o.getClass().getMethod(methodName);
        } catch (SecurityException e1) {
            s_logger.error("Security exception in getting ResponseObject " + o.getClass().getName() + " get method for property: " + propName);
        } catch (NoSuchMethodException e1) {
            if (s_logger.isTraceEnabled()) {
                s_logger.trace("ResponseObject " + o.getClass().getName() + " does not have " + methodName + "() method for property: " + propName +
                    ", will check is-prefixed method to see if it is boolean property");
            }
        }

        if (method != null)
            return method;

        methodName = getGetMethodName("is", propName);
        try {
            method = o.getClass().getMethod(methodName);
        } catch (SecurityException e1) {
            s_logger.error("Security exception in getting ResponseObject " + o.getClass().getName() + " get method for property: " + propName);
        } catch (NoSuchMethodException e1) {
            s_logger.warn("ResponseObject " + o.getClass().getName() + " does not have " + methodName + "() method for property: " + propName);
        }
        return method;
    }

    private static String getGetMethodName(String prefix, String fieldName) {
        StringBuffer sb = new StringBuffer(prefix);

        if (fieldName.length() >= prefix.length() && fieldName.substring(0, prefix.length()).equals(prefix)) {
            return fieldName;
        } else {
            sb.append(fieldName.substring(0, 1).toUpperCase());
            sb.append(fieldName.substring(1));
        }

        return sb.toString();
    }
}
