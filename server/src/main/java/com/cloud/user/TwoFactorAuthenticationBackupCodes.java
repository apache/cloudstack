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
package com.cloud.user;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.lang3.StringUtils;

/**
 * Generation, storage-encoding, and verification of one-time two-factor backup (recovery)
 * codes.
 *
 * Backup codes are stored HASHED (SHA-256), never in plaintext or reversibly encrypted:
 * they are only ever verified, never re-displayed, so a one-way hash is the correct
 * posture (as with passwords). The plaintext codes are returned to the caller exactly
 * once, at generation time. Verification is constant-time and consumes (removes) the
 * matched code so each code works only once.
 *
 * The stored form is a comma-separated list of hex SHA-256 hashes, sized to fit the
 * {@code user_details.value} column.
 */
public class TwoFactorAuthenticationBackupCodes {

    public static final int DEFAULT_CODE_COUNT = 10;
    private static final int CODE_LENGTH_CHARS = 10;
    // Unambiguous alphabet (no 0/O/1/I/L) for codes a user may transcribe by hand.
    private static final String CODE_ALPHABET = "ABCDEFGHJKMNPQRSTUVWXYZ23456789";
    private static final String SEPARATOR = ",";

    private static final SecureRandom RANDOM = new SecureRandom();

    /**
     * The result of generating a fresh set: the plaintext codes to show the user once, and
     * the encoded (hashed) blob to persist.
     */
    public static class GeneratedCodes {
        private final List<String> plaintextCodes;
        private final String storedValue;

        public GeneratedCodes(List<String> plaintextCodes, String storedValue) {
            this.plaintextCodes = plaintextCodes;
            this.storedValue = storedValue;
        }

        public List<String> getPlaintextCodes() {
            return plaintextCodes;
        }

        public String getStoredValue() {
            return storedValue;
        }
    }

    private TwoFactorAuthenticationBackupCodes() {
    }

    /**
     * Generates {@code count} random codes, returning both the plaintext (to show the user
     * once) and the hashed blob to persist.
     */
    public static GeneratedCodes generate(int count) {
        List<String> plaintext = new ArrayList<>(count);
        List<String> hashes = new ArrayList<>(count);
        for (int i = 0; i < count; i++) {
            String code = randomCode();
            plaintext.add(code);
            hashes.add(hash(code));
        }
        return new GeneratedCodes(plaintext, String.join(SEPARATOR, hashes));
    }

    /**
     * Verifies {@code code} against the stored hashed blob. If it matches an unused code,
     * returns the new stored blob with that code removed (consumed); returns {@code null}
     * if there is no match. The comparison is constant-time per stored hash.
     */
    public static String consume(String code, String storedValue) {
        if (StringUtils.isAnyBlank(code, storedValue)) {
            return null;
        }
        String candidate = hash(normalize(code));
        List<String> remaining = new ArrayList<>();
        boolean matched = false;
        for (String stored : storedValue.split(SEPARATOR)) {
            if (StringUtils.isBlank(stored)) {
                continue;
            }
            if (!matched && constantTimeEquals(stored, candidate)) {
                matched = true;
                continue;
            }
            remaining.add(stored);
        }
        if (!matched) {
            return null;
        }
        return String.join(SEPARATOR, remaining);
    }

    /**
     * Number of unused codes left in the stored blob.
     */
    public static int remainingCount(String storedValue) {
        if (StringUtils.isBlank(storedValue)) {
            return 0;
        }
        int count = 0;
        for (String stored : storedValue.split(SEPARATOR)) {
            if (StringUtils.isNotBlank(stored)) {
                count++;
            }
        }
        return count;
    }

    private static String randomCode() {
        StringBuilder sb = new StringBuilder(CODE_LENGTH_CHARS);
        for (int i = 0; i < CODE_LENGTH_CHARS; i++) {
            sb.append(CODE_ALPHABET.charAt(RANDOM.nextInt(CODE_ALPHABET.length())));
        }
        return sb.toString();
    }

    private static String hash(String code) {
        return DigestUtils.sha256Hex(code.getBytes(StandardCharsets.UTF_8));
    }

    /**
     * Normalizes user input so formatting differences (spaces, hyphens, case) do not cause
     * a spurious mismatch.
     */
    private static String normalize(String code) {
        return code.replaceAll("[\\s-]", "").toUpperCase();
    }

    private static boolean constantTimeEquals(String a, String b) {
        return MessageDigest.isEqual(a.getBytes(StandardCharsets.UTF_8), b.getBytes(StandardCharsets.UTF_8));
    }
}
