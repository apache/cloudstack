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
import java.util.function.Function;

import org.apache.cloudstack.api.ApiErrorCode;
import org.apache.cloudstack.api.ServerApiException;
import org.apache.cloudstack.context.ResponseMessageResolver;
import org.apache.commons.collections.MapUtils;

import com.cloud.exception.InvalidParameterValueException;
import com.cloud.exception.PermissionDeniedException;
import com.cloud.utils.StringUtils;
import com.cloud.utils.Ternary;
import com.cloud.utils.exception.CloudRuntimeException;

public final class Exceptions {

    private Exceptions() {
    }

    public static InvalidParameterValueException invalidParameterValueException(final String errorKey) {
        return invalidParameterValueException(errorKey, Collections.emptyMap());
    }

    public static InvalidParameterValueException invalidParameterValueException(final String errorKey,
                                                                                final Map<String, Object> metadata) {
        return build(errorKey, metadata, InvalidParameterValueException::new);
    }

    public static PermissionDeniedException permissionDeniedException(final String errorKey) {
        return permissionDeniedException(errorKey, Collections.emptyMap());
    }

    public static PermissionDeniedException permissionDeniedException(final String errorKey,
                                                                      final Map<String, Object> metadata) {
        return build(errorKey, metadata, PermissionDeniedException::new);
    }

    public static ServerApiException serverApiException(final ApiErrorCode errorCode,
                                                        final String errorKey) {
        return serverApiException(errorCode, errorKey, Collections.emptyMap());
    }

    public static ServerApiException serverApiException(final ApiErrorCode errorCode,
                                                        final String errorKey,
                                                        final Map<String, Object> metadata) {
        Ternary<String, String, Map<String, Object>> data =
                ResponseMessageResolver.resolve(errorKey, metadata);

        ServerApiException ex = new ServerApiException(errorCode, data.first());
        enrich(ex, data);
        return ex;
    }

    public static ServerApiException serverApiException(final ApiErrorCode errorCode,
                                                        final String message,
                                                        final String errorKey,
                                                        final Map<String, Object> metadata) {
        if (StringUtils.isNotBlank(errorKey)) {
            return serverApiException(errorCode, errorKey, metadata);
        }

        return new ServerApiException(errorCode, message);
    }

    public static CloudRuntimeException cloudRuntimeException(final String errorKey) {
        return cloudRuntimeException(errorKey, Collections.emptyMap());
    }

    public static CloudRuntimeException cloudRuntimeException(final String errorKey,
                                                              final Map<String, Object> metadata) {
        return build(errorKey, metadata, CloudRuntimeException::new);
    }

    public static CloudRuntimeException cloudRuntimeException(final String errorKey,
                                                              final Map<String, Object> metadata,
                                                              final Throwable cause) {
        Ternary<String, String, Map<String, Object>> data =
                ResponseMessageResolver.resolve(errorKey, metadata);

        CloudRuntimeException ex = new CloudRuntimeException(data.first(), cause);
        enrich(ex, data);
        return ex;
    }

    private static <T extends CloudRuntimeException> T build(
            final String errorKey,
            final Map<String, Object> metadata,
            final Function<String, T> exceptionSupplier) {

        Ternary<String, String, Map<String, Object>> data =
                ResponseMessageResolver.resolve(errorKey, metadata);

        T ex = exceptionSupplier.apply(data.first());
        enrich(ex, data);
        return ex;
    }

    private static void enrich(final CloudRuntimeException ex,
                               final Ternary<String, String, Map<String, Object>> data) {
        if (data == null) {
            return;
        }
        ex.setMessageKey(data.second());
        ex.setMetadata(data.third());
    }

    public static void normalizeMetadata(CloudRuntimeException cre) {
        if (MapUtils.isEmpty(cre.getMetadata())) {
            return;
        }
        cre.setMetadata(ResponseMessageResolver.convertToStringMap(cre.getMetadata()));
    }
}
