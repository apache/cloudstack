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

package org.apache.cloudstack.logsws;

import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.util.Base64;

import javax.crypto.Cipher;
import javax.crypto.spec.SecretKeySpec;

import com.cloud.serializer.GsonHelper;

public class LogsWebSessionTokenCryptoUtil {
    private static final String ALGORITHM = "AES";
    private static final String TRANSFORMATION = "AES";
    private static final String KEY_PREFIX = "Logger";

    private static String getDesiredLengthKey(String key) {
        if (key.length() >= 16) {
            return key.substring(0, 16);
        }
        return  KEY_PREFIX.substring(0, 16 - key.length()) + key;
    }

    public static String encrypt(LogsWebSessionTokenPayload payload, String key) throws GeneralSecurityException {
        String json = GsonHelper.getGson().toJson(payload);
        SecretKeySpec secretKey = new SecretKeySpec(getDesiredLengthKey(key).getBytes(StandardCharsets.UTF_8), ALGORITHM);
        Cipher cipher = Cipher.getInstance(TRANSFORMATION);
        cipher.init(Cipher.ENCRYPT_MODE, secretKey);
        byte[] encrypted = cipher.doFinal(json.getBytes(StandardCharsets.UTF_8));
        // URL-safe Base64 (no '/' or '+') and remove padding
        return Base64.getUrlEncoder().withoutPadding().encodeToString(encrypted);
    }

    public static LogsWebSessionTokenPayload decrypt(String token, String key) throws GeneralSecurityException {
        byte[] decoded = Base64.getUrlDecoder().decode(token);
        SecretKeySpec secretKey = new SecretKeySpec(getDesiredLengthKey(key).getBytes(StandardCharsets.UTF_8), ALGORITHM);
        Cipher cipher = Cipher.getInstance(TRANSFORMATION);
        cipher.init(Cipher.DECRYPT_MODE, secretKey);
        byte[] decrypted = cipher.doFinal(decoded);
        String json = new String(decrypted, StandardCharsets.UTF_8);
        return GsonHelper.getGson().fromJson(json, LogsWebSessionTokenPayload.class);
    }
}
