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
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.cloudstack.api.Identity;
import org.apache.cloudstack.api.InternalIdentity;
import org.apache.cloudstack.api.response.ExceptionResponse;
import org.apache.cloudstack.config.ApiServiceConfiguration;
import org.apache.commons.collections.MapUtils;
import org.apache.commons.lang3.ObjectUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.cloud.utils.PropertiesUtil;
import com.cloud.utils.Ternary;
import com.cloud.utils.exception.CloudRuntimeException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

public class ResponseMessageResolver {
    private static final Logger LOG =
            LogManager.getLogger(ResponseMessageResolver.class);

    protected static final String MESSAGES_DIRNAME = "messages";
    protected static final String ERROR_MESSAGES_FILENAME = MESSAGES_DIRNAME + "/error-messages.json";
    protected static final String PLUGIN_ERROR_MESSAGES_PREFIX = "error-messages-";
    protected static final String PLUGIN_ERROR_MESSAGES_SUFFIX = ".json";
    protected static final String ERROR_KEY_ADMIN_SUFFIX = ".admin";

    private static final Pattern VARIABLE_PATTERN = Pattern.compile("\\{\\{\\s*([A-Za-z0-9_]+)\\s*}}");
    private static final List<String> RESOURCE_NAME_GETTERS =
            Arrays.asList("getDisplayText", "getDisplayName", "getName");

    private static final ObjectMapper MAPPER = new ObjectMapper();

    // volatile for safe publication
    private static volatile Map<String, String> templates =
            Collections.emptyMap();

    /**
     * Snapshot of every source file that fed into {@link #templates} (the main file plus any
     * plugin-specific {@code error-messages-*.json} files found alongside it), keyed by filename
     * with the value being that file's last-modified time. Reloading is skipped unless this
     * snapshot differs from the last one taken, so both content edits AND files being added or
     * removed are detected.
     */
    private static volatile Map<String, Long> loadedFileSnapshot = Collections.emptyMap();

    private ResponseMessageResolver() {
    }

    /**
     * Clears the cached templates and last modified timestamp.
     * Useful for testing to ensure cache isolation between tests.
     */
    protected static synchronized void clearCache() {
        templates = Collections.emptyMap();
        loadedFileSnapshot = Collections.emptyMap();
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

    protected static Map<String, Object> getCombinedMetadataFromErrorTemplate(String template,
                                                                              Map<String, Object> metadata) {
        return getCombinedMetadataFromErrorTemplate(template, metadata, false);
    }

    protected static Map<String, Object> getCombinedMetadataFromErrorTemplate(String template,
                                                                              Map<String, Object> metadata,
                                                                              boolean onlyForTemplateVariables) {
        List<String> variableNames = getVariableNamesInErrorKey(template);
        if (variableNames.isEmpty()) {
            return onlyForTemplateVariables ? Collections.emptyMap() : metadata;
        }
        Map<String, Object> contextMetadata = CallContext.current().getErrorContextParameters();
        Map<String, Object> combinedMetadata = new LinkedHashMap<>();
        if (MapUtils.isNotEmpty(contextMetadata)) {
            for (String varName : variableNames) {
                if (contextMetadata.containsKey(varName)) {
                    combinedMetadata.put(varName, contextMetadata.get(varName));
                }
            }
        }
        if (MapUtils.isNotEmpty(metadata)) {
            combinedMetadata.putAll(metadata);
        }
        if (onlyForTemplateVariables) {
            combinedMetadata.entrySet().removeIf(x -> !variableNames.contains(x.getKey()));
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

    protected static boolean useResourceToStringInMetadata() {
        return ApiServiceConfiguration.ErrorMessageMetadataPreferToString.value();
    }

    protected static Map<String, String> getStringMap(Map<String, Object> metadata) {
        boolean isAdmin = CallContext.current().isCallingAccountRootAdmin();
        return getStringMap(metadata, isAdmin);
    }

    protected static Map<String, String> getStringMap(Map<String, Object> metadata, boolean isAdmin) {
        Map<String, String> stringMap = new LinkedHashMap<>();
        if (MapUtils.isEmpty(metadata)) {
            return stringMap;
        }
        for (Map.Entry<String, Object> entry : metadata.entrySet()) {
            Object value = entry.getValue();
            stringMap.put(entry.getKey(),
                    useResourceToStringInMetadata() ?
                            getMetadataObjectStringValuePreferringToString(value, isAdmin) :
                            getMetadataObjectStringValue(value, isAdmin));
        }
        return stringMap;
    }

    public static Map<String, Object> convertToStringMap(Map<String, Object> metadata) {
        Map<String, String> stringMap = getStringMap(metadata, false);
        return new LinkedHashMap<>(stringMap);
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
     *       The ID is included only if the {@code ApiServiceConfiguration.ErrorMessageMetadataIncludeIdForAdmins}
     *       global setting is {@code true} and {@code obj} implements {@link InternalIdentity}. The UUID is
     *       included when {@code obj} implements {@link org.apache.cloudstack.api.Identity}.</li>
     *   <li>If no display name is available, returns the UUID (if {@code obj} implements
     *       {@code Identity}); otherwise returns {@code obj.toString()}.</li>
     * </ul>
     *
     * <p>Reflection is used to call getters; invocation failures are silently ignored and treated as
     * absence of the corresponding value.
     *
     * @param obj metadata object
     * @param isAdmin true when the caller is a root admin and admin-only details may be included
     * @return formatted metadata string suitable for inclusion in error messages, or {@code null}
     *         if {@code obj} is {@code null}
     */
    protected static String getMetadataObjectStringValue(Object obj, boolean isAdmin) {
        if (obj == null) {
            return null;
        }
        String uuid = null;
        if (obj instanceof Identity) {
            uuid = ((Identity) obj).getUuid();
        }
        String name = null;
        for (String getter : RESOURCE_NAME_GETTERS) {
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
        sb.append(name);

        Long id = null;
        if (obj instanceof InternalIdentity && isAdmin && ApiServiceConfiguration.ErrorMessageMetadataIncludeIdForAdmins.value()) {
            id = ((InternalIdentity) obj).getId();
        }

        if (ObjectUtils.allNull(id, uuid)) {
            return sb.toString();
        }
        sb.append(" (ID: ");
        if (id != null) {
            sb.append(id);
            if (uuid != null) {
                sb.append(", UUID: ");
            }
        }
        if (uuid != null) {
            sb.append(uuid);
        }
        sb.append(")");

        return sb.toString();
    }

    /**
     * Converts a metadata object to a human-readable string, prioritizing toString().
     *
     * <p>Behavior:
     * <ul>
     *   <li>If {@code obj} is {@code null}, returns {@code null}.</li>
     *   <li>First attempts to use {@code obj.toString()}. If the result is non-empty, returns it.</li>
     *   <li>For non-root admins, removes any "id: DBID" patterns from the toString() result.</li>
     *   <li>If {@code obj.toString()} is empty or null (after filtering), falls back to {@link #getMetadataObjectStringValue(Object, boolean)},
     *       which uses reflection to find display names and format metadata with UUID/ID info.</li>
     * </ul>
     *
     * @param obj metadata object
     * @param isAdmin true when the caller is a root admin and admin-only details may be included
     * @return formatted metadata string suitable for inclusion in error messages, or {@code null}
     *         if {@code obj} is {@code null}
     */
    protected static String getMetadataObjectStringValuePreferringToString(Object obj, boolean isAdmin) {
        if (obj == null) {
            return null;
        }
        String result = obj.toString();
        if (StringUtils.isNotEmpty(result)) {
            if (!isAdmin) {
                result = result.replaceAll("\\bid:\\s*\\d+", "").trim();
            }
            if (StringUtils.isNotEmpty(result)) {
                return result;
            }
        }
        return getMetadataObjectStringValue(obj, isAdmin);
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

    /**
     * Lists any plugin-contributed {@code error-messages-*.json} files sitting alongside the main
     * error-messages file, sorted alphabetically so merge order (and therefore which file wins on
     * a key collision between two plugin files) is deterministic. These are never bundled with any
     * package; an operator or plugin author drops them into the same directory as the main file at
     * runtime, conventionally named after the contributing artifact, e.g. {@code
     * error-messages-cloud-plugin-mom-webhook.json}. The resolver does not otherwise attach any
     * meaning to the suffix.
     */
    private static List<File> listPluginErrorMessageFiles(File configDir) {
        File[] files = configDir.listFiles((dir, name) ->
                name.startsWith(PLUGIN_ERROR_MESSAGES_PREFIX) && name.endsWith(PLUGIN_ERROR_MESSAGES_SUFFIX));
        if (files == null || files.length == 0) {
            return Collections.emptyList();
        }
        List<File> pluginFiles = new ArrayList<>(Arrays.asList(files));
        pluginFiles.sort(Comparator.comparing(File::getName));
        return pluginFiles;
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
                    loadedFileSnapshot = Collections.emptyMap();
                }
                return;
            }

            List<File> pluginFiles = listPluginErrorMessageFiles(errorMessagesFile.getParentFile());

            Map<String, Long> currentSnapshot = new LinkedHashMap<>();
            currentSnapshot.put(errorMessagesFile.getName(), Files.getLastModifiedTime(errorMessagesFile.toPath()).toMillis());
            for (File pluginFile : pluginFiles) {
                currentSnapshot.put(pluginFile.getName(), pluginFile.lastModified());
            }

            if (currentSnapshot.equals(loadedFileSnapshot)) {
                return;
            }

            Map<String, String> mainTemplates;
            try (InputStream is = Files.newInputStream(errorMessagesFile.toPath())) {
                mainTemplates = MAPPER.readValue(is, new TypeReference<>() {
                });
            }
            Map<String, String> merged = new LinkedHashMap<>(mainTemplates);

            // keys contributed so far by plugin files specifically, to warn on plugin-vs-plugin
            // collisions without also warning every time a plugin (as designed) overrides the main file
            Set<String> pluginContributedKeys = new HashSet<>();
            for (File pluginFile : pluginFiles) {
                try (InputStream is = Files.newInputStream(pluginFile.toPath())) {
                    Map<String, String> pluginTemplates = MAPPER.readValue(is, new TypeReference<>() {
                    });
                    for (String key : pluginTemplates.keySet()) {
                        if (pluginContributedKeys.contains(key)) {
                            LOG.warn("Error message key '{}' from {} was already defined by another plugin " +
                                    "error-messages file; the later file (alphabetically) takes precedence",
                                    key, pluginFile.getName());
                        }
                    }
                    merged.putAll(pluginTemplates);
                    pluginContributedKeys.addAll(pluginTemplates.keySet());
                } catch (Exception e) {
                    LOG.warn("Failed to load plugin error messages from {}, skipping this file", pluginFile, e);
                }
            }

            templates = merged;
            loadedFileSnapshot = currentSnapshot;

            LOG.info("Reloaded {} error message templates from {} (+{} plugin file(s): {})",
                    templates.size(), errorMessagesFile.toPath(), pluginFiles.size(), pluginFiles);

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

    public static Ternary<String, String, Map<String, Object>> resolve(String errorKey, Map<String, Object> metadata) {
        String template = getTemplateForKey(errorKey);
        if (template == null) {
            return new Ternary<>(errorKey, errorKey, metadata);
        }
        Map<String, Object> combinedMetadata = getCombinedMetadataFromErrorTemplate(template, metadata);
        return new Ternary<>(expand(template, getStringMap(combinedMetadata)), errorKey, combinedMetadata);
    }

    public static void updateExceptionResponse(ExceptionResponse response, CloudRuntimeException cre,
                                               long accountId, long userId) {
        CallContext.register(userId, accountId);
        try {
            updateExceptionResponse(response, cre);
        } finally {
            CallContext.unregister();
        }
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
        Map<String, Object> combinedMetadata = getCombinedMetadataFromErrorTemplate(template, map,
                true);
        Map<String, String> stringMap = getStringMap(combinedMetadata);
        response.setErrorText(expand(template,  stringMap));
        response.setErrorMetadata(stringMap);
    }
}
