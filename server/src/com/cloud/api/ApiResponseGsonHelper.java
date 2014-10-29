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

import org.apache.cloudstack.acl.RoleType;
import org.apache.cloudstack.api.ResponseObject;
import org.apache.cloudstack.context.CallContext;

import com.cloud.serializer.Param;
import com.cloud.user.Account;
import com.google.gson.ExclusionStrategy;
import com.google.gson.FieldAttributes;
import com.google.gson.GsonBuilder;

/**
 * The ApiResonseGsonHelper is different from ApiGsonHelper - it registeres one more adapter for String type required for api response encoding
 */
public class ApiResponseGsonHelper {
    private static final GsonBuilder s_gBuilder;

    static {
        s_gBuilder = new GsonBuilder().setDateFormat("yyyy-MM-dd'T'HH:mm:ssZ");
        s_gBuilder.setVersion(1.3);
        s_gBuilder.registerTypeAdapter(ResponseObject.class, new ResponseObjectTypeAdapter());
        s_gBuilder.registerTypeAdapter(String.class, new EncodedStringTypeAdapter());
        s_gBuilder.setExclusionStrategies(new ExclStrat());
    }

    public static GsonBuilder getBuilder() {
        return s_gBuilder;
    }

    private static class ExclStrat implements ExclusionStrategy {

        public boolean shouldSkipClass(Class<?> arg0) {
            return false;
        }
        public boolean shouldSkipField(FieldAttributes f) {

            Param param = f.getAnnotation(Param.class);
            if (param != null) {
                RoleType[] allowedRoles = param.authorized();
                if (allowedRoles.length > 0) {
                    boolean permittedParameter = false;
                    Account caller = CallContext.current().getCallingAccount();
                    for (RoleType allowedRole : allowedRoles) {
                        if (allowedRole.getValue() == caller.getType()) {
                            permittedParameter = true;
                            break;
                        }
                    }
                    if (!permittedParameter) {
                        return true;
                    }
                }
            }
            return false;
        }
    }
}
