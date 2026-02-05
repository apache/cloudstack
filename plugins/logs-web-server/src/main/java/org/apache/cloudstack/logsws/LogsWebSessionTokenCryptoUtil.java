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

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.Base64;

import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;

import com.cloud.serializer.GsonHelper;

public class LogsWebSessionTokenCryptoUtil {
    private static final String ALGORITHM = "AES";
    private static final String TRANSFORMATION = "AES/GCM/NoPadding";

    private static final int IV_LENGTH_BYTES = 12;
    private static final int GCM_TAG_LENGTH_BITS = 128;
    private static final byte TOKEN_VERSION = 1;

    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    private static SecretKey deriveKey(String keyMaterial) throws GeneralSecurityException {
        MessageDigest digest = MessageDigest.getInstance("SHA-256");
        byte[] hash = digest.digest(keyMaterial.getBytes(StandardCharsets.UTF_8));
        return new SecretKeySpec(hash, 0, 32, ALGORITHM);
    }

    public static String encrypt(LogsWebSessionTokenPayload payload, String keyMaterial)
            throws GeneralSecurityException {

        String json = GsonHelper.getGson().toJson(payload);
        byte[] plaintext = json.getBytes(StandardCharsets.UTF_8);

        SecretKey key = deriveKey(keyMaterial);

        byte[] iv = new byte[IV_LENGTH_BYTES];
        SECURE_RANDOM.nextBytes(iv);

        Cipher cipher = Cipher.getInstance(TRANSFORMATION);
        GCMParameterSpec spec = new GCMParameterSpec(GCM_TAG_LENGTH_BITS, iv);
        cipher.init(Cipher.ENCRYPT_MODE, key, spec);

        byte[] ciphertextWithTag = cipher.doFinal(plaintext);
        ByteBuffer buffer = ByteBuffer.allocate(1 + IV_LENGTH_BYTES + ciphertextWithTag.length);
        buffer.put(TOKEN_VERSION);
        buffer.put(iv);
        buffer.put(ciphertextWithTag);

        return Base64.getUrlEncoder().withoutPadding().encodeToString(buffer.array());
    }

    public static LogsWebSessionTokenPayload decrypt(String token, String keyMaterial)
            throws GeneralSecurityException {

        byte[] allBytes = Base64.getUrlDecoder().decode(token);
        ByteBuffer buffer = ByteBuffer.wrap(allBytes);

        if (buffer.remaining() < 1 + IV_LENGTH_BYTES + 1) {
            throw new GeneralSecurityException("Invalid token format");
        }

        byte version = buffer.get();
        if (version != TOKEN_VERSION) {
            throw new GeneralSecurityException("Unsupported token version: " + version);
        }

        byte[] iv = new byte[IV_LENGTH_BYTES];
        buffer.get(iv);

        byte[] ciphertextWithTag = new byte[buffer.remaining()];
        buffer.get(ciphertextWithTag);

        SecretKey key = deriveKey(keyMaterial);

        Cipher cipher = Cipher.getInstance(TRANSFORMATION);
        GCMParameterSpec spec = new GCMParameterSpec(GCM_TAG_LENGTH_BITS, iv);
        cipher.init(Cipher.DECRYPT_MODE, key, spec);

        byte[] plaintext = cipher.doFinal(ciphertextWithTag);
        String json = new String(plaintext, StandardCharsets.UTF_8);

        return GsonHelper.getGson().fromJson(json, LogsWebSessionTokenPayload.class);
    }
}
