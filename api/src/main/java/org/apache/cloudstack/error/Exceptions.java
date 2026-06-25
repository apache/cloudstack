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

package org.apache.cloudstack.error;

import java.util.Collections;
import java.util.Map;

import org.apache.cloudstack.api.ApiErrorCode;
import org.apache.cloudstack.api.ServerApiException;
import org.apache.cloudstack.context.ResponseMessageResolver;

import com.cloud.exception.InvalidParameterValueException;
import com.cloud.exception.PermissionDeniedException;
import com.cloud.utils.exception.CloudRuntimeException;

public class Exceptions {

    public static InvalidParameterValueException invalidParameterValueException(String errorKey, Map<String, Object> metadata) {
        return new InvalidParameterValueException(errorKey, metadata);
    }

    public static InvalidParameterValueException invalidParameterValueException(String errorKey) {
        return invalidParameterValueException(errorKey, Collections.emptyMap());
    }

    public static PermissionDeniedException permissionDeniedException(String errorKey, Map<String, Object> metadata) {
        return new PermissionDeniedException(errorKey, metadata);
    }

    public static PermissionDeniedException permissionDeniedException(String errorKey) {
        return permissionDeniedException(errorKey, Collections.emptyMap());
    }

    public static ServerApiException serverApiException(ApiErrorCode errorCode, String errorKey, Map<String, Object> metadata) {
        return new ServerApiException(errorCode, errorKey, metadata);
    }

    public static ServerApiException serverApiException(ApiErrorCode errorCode, String errorKey) {
        return serverApiException(errorCode, errorKey, Collections.emptyMap());
    }

    public static CloudRuntimeException cloudRuntimeException(String errorKey, Map<String, Object> metadata) {
        return new CloudRuntimeException(ResponseMessageResolver.resolve(errorKey, metadata));
    }

    public static CloudRuntimeException cloudRuntimeException(String errorKey) {
        return new CloudRuntimeException(ResponseMessageResolver.resolve(errorKey, Collections.emptyMap()));
    }

    public static CloudRuntimeException cloudRuntimeException(String errorKey, Map<String, Object> metadata, Throwable th) {
        return new CloudRuntimeException(ResponseMessageResolver.resolve(errorKey, metadata), th);
    }
}
