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

package org.apache.cloudstack.context;

import java.io.File;
import java.io.InputStream;
import java.lang.reflect.InvocationTargetException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

import org.apache.cloudstack.api.Identity;
import org.apache.cloudstack.api.InternalIdentity;
import org.apache.cloudstack.api.response.ExceptionResponse;
import org.apache.commons.collections.MapUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.cloud.exception.InvalidParameterValueException;
import com.cloud.utils.PropertiesUtil;
import com.cloud.utils.exception.CloudRuntimeException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

public final class ErrorMessageResolver {

    private static final Logger LOG =
            LogManager.getLogger(ErrorMessageResolver.class);

    private static final String ERROR_MESSAGES_FILENAME = "error-messages.json";
    private static final String ERROR_KEY_ADMIN_SUFFIX = ".admin";

    private static final ObjectMapper MAPPER = new ObjectMapper();

    // volatile for safe publication
    private static volatile Map<String, String> templates =
            Collections.emptyMap();

    private static volatile long lastModified = -1;

    private ErrorMessageResolver() {
    }

    public static String getMessage(String errorKey, Map<String, Object> metadata) {
        return getMessageUsingStringMap(errorKey, getStringMap(metadata));
    }

    private static String getTemplateForKey(String errorKey) {
        if (errorKey == null) {
            return null;
        }
        reloadIfRequired();
        if (!errorKey.endsWith(ERROR_KEY_ADMIN_SUFFIX) && CallContext.current().isCallingAccountRootAdmin()) {
            String template = templates.get(errorKey + ERROR_KEY_ADMIN_SUFFIX);
            if (template != null) {
                return template;
            }
        }
        return templates.get(errorKey);
    }

    private static String getMessageUsingStringMap(String errorKey, Map<String, String> metadata) {
        String template = getTemplateForKey(errorKey);
        if (template == null) {
            return errorKey;
        }
        return expand(template, metadata);
    }

    private static Map<String, String> getStringMap(Map<String, Object> metadata) {
        Map<String, String> stringMap = new LinkedHashMap<>();
        if (MapUtils.isNotEmpty(metadata)) {
            for (Map.Entry<String, Object> entry : metadata.entrySet()) {
                Object value = entry.getValue();
                stringMap.put(entry.getKey(), getMetadataObjectStringValue(value));
            }
        }
        return stringMap;
    }

    private static String getMetadataObjectStringValue(Object obj) {
        if (obj == null) {
            return null;
        }
        // obj is of primitive type
        // String value structure should be 'NAME' (ID: id, UUID: uuid)
        // NAME is obtained from obj.getName() or obj.getDisplayText()
        // ID is obtained from obj.getId() if obj instanceof InternalIdentity and only for root admin
        // UUID is obtained from obj.getUuid() if obj instanceof Identity
        // If NAME is not available, fallback to obj.toString() then simply return UUID if available
        String uuid = null;
        if (obj instanceof Identity) {
            uuid = ((Identity) obj).getUuid();
        }
        String name = null;
        for (String getter : new String[]{"getDisplayText", "getDisplayName", "getName"}) {
            name = invokeStringGetter(obj, getter);
            if (name != null) {
                break;
            }
        }
        if (StringUtils.isEmpty(name)) {
            if (StringUtils.isNotEmpty(uuid)) {
                return uuid;
            }
            return obj.toString();
        }

        StringBuilder sb = new StringBuilder();
        sb.append("'").append(name).append("'");

        Long id = null;
        if (CallContext.current().isCallingAccountRootAdmin() && obj instanceof InternalIdentity) {
            id = ((InternalIdentity) obj).getId();
        }

        if (id == null && uuid == null) {
            return sb.toString();
        }
        sb.append(" (");
        if (id != null) {
            sb.append("ID: ").append(id);
            if (uuid != null) {
                sb.append(", ");
            }
        }
        if (uuid != null) {
            sb.append("UUID: ").append(uuid);
        }
        sb.append(")");

        return sb.toString();
    }

    private static String invokeStringGetter(Object obj, String methodName) {
        try {
            Class<?> cls = obj.getClass();
            var m = cls.getMethod(methodName);
            Object val = m.invoke(obj);
            return val == null ? null : val.toString();
        } catch (NoSuchMethodException | IllegalAccessException | InvocationTargetException e) {
            return null;
        }
    }

    public static void updateExceptionResponse(ExceptionResponse response, CloudRuntimeException cre) {
        String key = cre.getMessageKey();
        Map<String, Object> map = cre.getMetadata();

        if (key == null) {
            if (cre.getCause() instanceof InvalidParameterValueException) {
                key = ((InvalidParameterValueException) cre.getCause()).getMessageKey();
                map = ((InvalidParameterValueException) cre.getCause()).getMetadata();
            } else {
                return;
            }
        }
        response.setErrorTextKey(key);
        Map<String, String> stringMap = getStringMap(map);
        String message = getMessageUsingStringMap(key, stringMap);
        if (message != null) {
            response.setErrorText(message);
        }
        response.setErrorMetadata(stringMap);
    }

    private static synchronized void reloadIfRequired() {
        try {
            // log current directory for debugging purposes
            LOG.debug("Current working directory: {}",
                    Paths.get(".").toAbsolutePath().normalize());
            File errorMessagesFile = PropertiesUtil.findConfigFile(ERROR_MESSAGES_FILENAME);
            if (errorMessagesFile == null || !errorMessagesFile.exists()) {
                if (!templates.isEmpty()) {
                    LOG.warn("Error messages file disappeared: {}",
                            errorMessagesFile != null ? errorMessagesFile.getAbsolutePath() : ERROR_MESSAGES_FILENAME);
                    templates = Collections.emptyMap();
                }
                return;
            }

            long modified =
                    Files.getLastModifiedTime(errorMessagesFile.toPath()).toMillis();

            if (modified == lastModified) {
                return;
            }

            try (InputStream is =
                         Files.newInputStream(errorMessagesFile.toPath())) {

                templates = MAPPER.readValue(
                        is,
                        new TypeReference<>() {
                        }
                );
                lastModified = modified;

                LOG.info("Reloaded {} error message templates from {}",
                        templates.size(), errorMessagesFile.toPath());
            }

        } catch (Exception e) {
            LOG.warn("Failed to reload error messages from {}",
                    ERROR_MESSAGES_FILENAME, e);
        }
    }

    private static String expand(String template, Map<String, String> metadata) {
        if (metadata == null || metadata.isEmpty()) {
            return template;
        }
        String result = template;
        for (Map.Entry<String, String> entry : metadata.entrySet()) {
            String placeholder = "{{" + entry.getKey() + "}}";
            Object value = entry.getValue();
            if (value != null) {
                result = result.replace(placeholder, value.toString());
            }
        }
        return result;
    }
}
