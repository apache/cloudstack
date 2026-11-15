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
package org.apache.cloudstack.framework.jobs;

import org.apache.cloudstack.api.APICommand;
import org.apache.cloudstack.api.BaseAsyncCmd;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.cloud.utils.StringUtils;
import com.cloud.utils.crypt.DBEncryptionUtil;
import com.cloud.utils.crypt.EncryptionSecretKeyChecker;
import com.google.gson.JsonElement;
import com.google.gson.JsonParser;

public final class AsyncJobDataHelper {
    private static final Logger LOGGER = LogManager.getLogger(AsyncJobDataHelper.class);

    private AsyncJobDataHelper() {
    }

    public static String encryptInfoIfNeeded(final String className, final String cmdInfoJson) {
        return encryptIfNeeded(isSensitiveRequest(className), cmdInfoJson);
    }

    public static String encryptResultIfNeeded(final String className, final String result) {
        return encryptIfNeeded(isSensitiveResponse(className), result);
    }

    private static String encryptIfNeeded(final boolean isSensitive, final String data) {
        if (!isSensitive || StringUtils.isBlank(data) || !EncryptionSecretKeyChecker.useEncryption()) {
            return data;
        }

        return DBEncryptionUtil.encrypt(data);
    }

    public static String decryptIfNeeded(final String className, final String cmdInfoJson) {
        if (StringUtils.isBlank(cmdInfoJson)) {
            return cmdInfoJson;
        }

        if (!isSensitiveRequest(className)) {
            return cmdInfoJson;
        }

        if (isValidJson(cmdInfoJson)) {
            return cmdInfoJson;
        }

        final String decryptedCmdInfo = tryDecryptWholePayload(cmdInfoJson);
        return isValidJson(decryptedCmdInfo) ? decryptedCmdInfo : cmdInfoJson;
    }

    public static String decryptResultIfNeeded(final String className, final String result) {
        if (StringUtils.isBlank(result)) {
            return result;
        }

        if (!isSensitiveResponse(className)) {
            return result;
        }

        if (isValidResult(result)) {
            return result;
        }

        final String decryptedCmdInfo = tryDecryptWholePayload(result);
        return isValidResult(decryptedCmdInfo) ? decryptedCmdInfo : result;
    }

    private static boolean isSensitiveRequest(final String className) {
        Class<? extends BaseAsyncCmd> cmdClass = resolveCmdClass(className);
        if (cmdClass == null) {
            return false;
        }
        final APICommand annotation = cmdClass.getAnnotation(APICommand.class);
        return annotation != null && annotation.requestHasSensitiveInfo();
    }

    private static boolean isSensitiveResponse(final String className) {
        Class<? extends BaseAsyncCmd> cmdClass = resolveCmdClass(className);
        if (cmdClass == null) {
            return false;
        }
        final APICommand annotation = cmdClass.getAnnotation(APICommand.class);
        return annotation != null && annotation.responseHasSensitiveInfo();
    }

    private static boolean isValidJson(final String value) {
        try {
            final JsonElement element = new JsonParser().parse(value);
            return element.isJsonObject() || element.isJsonArray();
        } catch (RuntimeException ex) {
            return false;
        }
    }

    private static boolean isValidResult(final String value) {
        // Serialized result format is: "ClassName/[objectName/]{...json...}"
        // Plain-text error messages (e.g. "job cancelled...") contain spaces.
        // AES/Base64-encrypted strings contain neither '{' nor spaces.
        return value != null && (value.contains("{") || value.contains(" "));
    }

    private static String tryDecryptWholePayload(final String cmdInfoJson) {
        if (!EncryptionSecretKeyChecker.useEncryption()) {
            return null;
        }

        try {
            return DBEncryptionUtil.decrypt(cmdInfoJson);
        } catch (RuntimeException ex) {
            LOGGER.debug("Failed to decrypt async job command info payload using direct decrypt fallback.", ex);
            return null;
        }
    }

    private static Class<? extends BaseAsyncCmd> resolveCmdClass(final String className) {
        if (StringUtils.isBlank(className)) {
            return null;
        }
        Class<? extends BaseAsyncCmd> cmdClass = null;
        try {
            Class<?> clazz = Class.forName(className);
            if (!BaseAsyncCmd.class.isAssignableFrom(clazz)) {
                LOGGER.trace("Class {} is not a subclass of BaseAsyncCmd, ignoring.", className);
                return null;
            }
            cmdClass = clazz.asSubclass(BaseAsyncCmd.class);
        } catch (ClassNotFoundException e) {
            LOGGER.trace("Failed to resolve command class {} for async job", className, e);
        }
        return cmdClass;
    }
}
