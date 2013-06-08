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

import org.apache.log4j.Logger;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonSerializationContext;
import com.google.gson.JsonSerializer;

import org.apache.cloudstack.api.ResponseObject;
import org.apache.cloudstack.api.response.ExceptionResponse;
import org.apache.cloudstack.api.response.SuccessResponse;

public class ResponseObjectTypeAdapter implements JsonSerializer<ResponseObject> {
    public static final Logger s_logger = Logger.getLogger(ResponseObjectTypeAdapter.class);

    @Override
    public JsonElement serialize(ResponseObject responseObj, Type typeOfResponseObj, JsonSerializationContext ctx) {
        JsonObject obj = new JsonObject();

        if (responseObj instanceof SuccessResponse) {
            obj.addProperty("success", ((SuccessResponse) responseObj).getSuccess());
            return obj;
        } else if (responseObj instanceof ExceptionResponse) {
            obj.addProperty("errorcode", ((ExceptionResponse) responseObj).getErrorCode());
            obj.addProperty("errortext", ((ExceptionResponse) responseObj).getErrorText());
            return obj;
        } else {
            obj.add(responseObj.getObjectName(), ApiResponseGsonHelper.getBuilder().create().toJsonTree(responseObj));
            return obj;
        }
    }
}
