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
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.cloudstack.api.Identity;
import org.apache.cloudstack.api.InternalIdentity;
import org.apache.cloudstack.api.response.ExceptionResponse;
import org.apache.commons.collections.MapUtils;
import org.apache.commons.lang3.ObjectUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.cloud.utils.PropertiesUtil;
import com.cloud.utils.exception.CloudRuntimeException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

public class ErrorMessageResolver {
    private static final Logger LOG =
            LogManager.getLogger(ErrorMessageResolver.class);

    protected static final String ERROR_MESSAGES_FILENAME = "error-messages.json";
    protected static final String ERROR_KEY_ADMIN_SUFFIX = ".admin";
    protected static final boolean INCLUDE_METADATA_ID_IN_MESSAGE = false;

    private static final Pattern VARIABLE_PATTERN = Pattern.compile("\\{\\{\\s*([A-Za-z0-9_]+)\\s*\\}\\}");

    private static final ObjectMapper MAPPER = new ObjectMapper();

    // volatile for safe publication
    private static volatile Map<String, String> templates =
            Collections.emptyMap();

    private static volatile long lastModified = -1;

    private ErrorMessageResolver() {
    }

    protected static List<String> getVariableNamesInErrorKey(String template) {
        if (template == null || template.isEmpty()) {
            return Collections.emptyList();
        }
        List<String> variables = new ArrayList<>();
        Matcher matcher = VARIABLE_PATTERN.matcher(template);
        while (matcher.find()) {
            String name = matcher.group(1);
            if (name != null && !name.isEmpty()) {
                variables.add(name);
            }
        }
        return variables;
    }

    protected static Map<String, Object> getCombinedMetadataFromErrorTemplate(String template, Map<String, Object> metadata) {
        List<String> variableNames = getVariableNamesInErrorKey(template);
        if (variableNames.isEmpty()) {
            return metadata;
        }
        Map<String, Object> contextMetadata = CallContext.current().getErrorContextParameters();
        if (MapUtils.isEmpty(contextMetadata)) {
            return metadata;
        }
        Map<String, Object> combinedMetadata = new LinkedHashMap<>();
        for (String varName : variableNames) {
            if (contextMetadata.containsKey(varName)) {
                combinedMetadata.put(varName, contextMetadata.get(varName));
            }
        }
        if (MapUtils.isNotEmpty(metadata)) {
            combinedMetadata.putAll(metadata);
        }
        return combinedMetadata;
    }

    protected static String getTemplateForKey(String errorKey) {
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

    protected static Map<String, String> getStringMap(Map<String, Object> metadata) {
        Map<String, String> stringMap = new LinkedHashMap<>();
        if (MapUtils.isNotEmpty(metadata)) {
            for (Map.Entry<String, Object> entry : metadata.entrySet()) {
                Object value = entry.getValue();
                stringMap.put(entry.getKey(), getMetadataObjectStringValue(value));
            }
        }
        return stringMap;
    }

    /**
     * Converts a metadata object to a human-readable string for error messages.
     *
     * <p>Behavior:
     * <ul>
     *   <li>If {@code obj} is {@code null}, returns {@code null}.</li>
     *   <li>Attempts to obtain a display name by invoking one of the getters
     *       {@code getDisplayText()}, {@code getDisplayName()}, or {@code getName()} via reflection.
     *       If a name is found, returns it quoted as {@code 'NAME'}.</li>
     *   <li>When the current calling account is a root admin, the returned value will include
     *       an identifier suffix in the form {@code (ID: id, UUID: uuid)} when available.
     *       The ID is included only if {@code INCLUDE_METADATA_ID_IN_MESSAGE} is {@code true}
     *       and {@code obj} implements {@link InternalIdentity}. The UUID is included when
     *       {@code obj} implements {@link org.apache.cloudstack.api.Identity}.</li>
     *   <li>If no display name is available, returns the UUID (if {@code obj} implements
     *       {@code Identity}); otherwise returns {@code obj.toString()}.</li>
     * </ul>
     *
     * <p>Reflection is used to call getters; invocation failures are silently ignored and treated as
     * absence of the corresponding value.
     *
     * @param obj metadata object
     * @return formatted metadata string suitable for inclusion in error messages, or {@code null}
     *         if {@code obj} is {@code null}
     */
    protected static String getMetadataObjectStringValue(Object obj) {
        if (obj == null) {
            return null;
        }
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

        if (!CallContext.current().isCallingAccountRootAdmin()) {
            return sb.toString();
        }

        Long id = null;
        if (INCLUDE_METADATA_ID_IN_MESSAGE && obj instanceof InternalIdentity) {
            id = ((InternalIdentity) obj).getId();
        }

        if (ObjectUtils.allNull(id, uuid)) {
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

    protected static String invokeStringGetter(Object obj, String methodName) {
        try {
            Class<?> cls = obj.getClass();
            var m = cls.getMethod(methodName);
            Object val = m.invoke(obj);
            return val == null ? null : val.toString();
        } catch (NoSuchMethodException | IllegalAccessException | InvocationTargetException e) {
            return null;
        }
    }

    protected static synchronized void reloadIfRequired() {
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

    protected static String expand(String template, Map<String, String> metadata) {
        if (MapUtils.isEmpty(metadata)) {
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

    public static String getMessage(String errorKey, Map<String, Object> metadata) {
        String template = getTemplateForKey(errorKey);
        if (template == null) {
            return errorKey;
        }
        Map<String, Object> combinedMetadata = getCombinedMetadataFromErrorTemplate(template, metadata);
        return expand(template, getStringMap(combinedMetadata));
    }

    public static void updateExceptionResponse(ExceptionResponse response, CloudRuntimeException cre) {
        String key = cre.getMessageKey();
        Map<String, Object> map = cre.getMetadata();

        if (key == null) {
            Throwable cause = cre.getCause();
            if (!(cause instanceof CloudRuntimeException)) {
                return;
            }
            CloudRuntimeException causeEx = (CloudRuntimeException) cause;
            key = causeEx.getMessageKey();
            if (key == null) {
                return;
            }
            map = causeEx.getMetadata();
        }
        response.setErrorTextKey(key);
        String template = getTemplateForKey(key);
        if (template == null) {
            response.setErrorText(key);
            response.setErrorMetadata(getStringMap(map));
            return;
        }
        Map<String, Object> combinedMetadata = getCombinedMetadataFromErrorTemplate(template, map);
        Map<String, String> stringMap = getStringMap(combinedMetadata);
        response.setErrorText(expand(template,  stringMap));
        response.setErrorMetadata(stringMap);
    }
}
