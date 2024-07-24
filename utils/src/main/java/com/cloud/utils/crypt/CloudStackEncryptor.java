//
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
//

package com.cloud.utils.crypt;

import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.stream.Collectors;

import org.apache.commons.lang3.StringUtils;
import org.apache.log4j.Logger;

import com.cloud.utils.exception.CloudRuntimeException;

public class CloudStackEncryptor {
    public static final Logger s_logger = Logger.getLogger(CloudStackEncryptor.class);
    private Base64Encryptor encryptor = null;
    private LegacyBase64Encryptor encryptorV1;
    private AeadBase64Encryptor encryptorV2;
    String password;
    EncryptorVersion version;
    Class callerClass;

    enum EncryptorVersion {
        V1, V2;

        public static EncryptorVersion fromString(String version) {
            if (version != null && version.equalsIgnoreCase("v1")) {
                return V1;
            }
            if (version != null && version.equalsIgnoreCase("v2")) {
                return V2;
            }
            if (StringUtils.isNotEmpty(version)) {
                throw new CloudRuntimeException(String.format("Invalid encryptor version: %s, valid options are: %s", version,
                        Arrays.stream(EncryptorVersion.values()).map(EncryptorVersion::name).collect(Collectors.joining(","))));
            }
            return null;
        }

        public static EncryptorVersion defaultVersion() {
            return V2;
        }
    }

    public CloudStackEncryptor(String password, String version, Class callerClass) {
        this.password = password;
        this.version = EncryptorVersion.fromString(version);
        this.callerClass = callerClass;
        initialize();
    }

    public String encrypt(String plain) {
        if (StringUtils.isEmpty(plain)) {
            return plain;
        }
        try {
            if (encryptor == null) {
                encryptor = encryptorV2;
                s_logger.debug(String.format("CloudStack will encrypt and decrypt values using default encryptor : %s for class %s",
                        encryptor.getClass().getSimpleName(), callerClass.getSimpleName()));
            }
            return encryptor.encrypt(plain);
        } catch (EncryptionException e) {
            throw new CloudRuntimeException("Error encrypting value: ", e);
        }
    }

    public String decrypt(String encrypted) {
        if (StringUtils.isEmpty(encrypted)) {
            return encrypted;
        }
        if (encryptor != null) {
            try {
                return encryptor.decrypt(encrypted);
            } catch (EncryptionException e) {
                throw new CloudRuntimeException("Error decrypting value: " + hideValueWithAsterisks(encrypted), e);
            }
        } else {
            String result = decrypt(encryptorV2, encrypted);
            if (result != null) {
                return result;
            }
            result = decrypt(encryptorV1, encrypted);
            if (result != null) {
                return result;
            }
            throw new CloudRuntimeException("Failed to decrypt value: " + hideValueWithAsterisks(encrypted));
        }
    }

    private String decrypt(Base64Encryptor encryptorToUse, String encrypted) {
        try {
            String result = encryptorToUse.decrypt(encrypted);
            s_logger.debug(String.format("CloudStack will encrypt and decrypt values using encryptor : %s for class %s",
                    encryptorToUse.getClass().getSimpleName(), callerClass.getSimpleName()));
            encryptor = encryptorToUse;
            return result;
        } catch (EncryptionException ex) {
            s_logger.warn(String.format("Failed to decrypt value using %s: %s", encryptorToUse.getClass().getSimpleName(), hideValueWithAsterisks(encrypted)));
        }
        return null;
    }

    protected static String hideValueWithAsterisks(String value) {
        if (StringUtils.isEmpty(value)) {
            return value;
        }
        int numChars = value.length() >= 10 ? 5: 1;
        int numAsterisks = 10 - numChars;
        return value.substring(0, numChars) + "*".repeat(numAsterisks);
    }

    protected void initialize() {
        s_logger.debug("Calling to initialize for class " + callerClass.getName());
        encryptor = null;
        if (EncryptorVersion.V1.equals(version)) {
            encryptorV1 = new LegacyBase64Encryptor(password);
            encryptor = encryptorV1;
            s_logger.debug("Initialized with encryptor : " + encryptorV1.getClass().getSimpleName());
        } else if (EncryptorVersion.V2.equals(version)) {
            encryptorV2 = new AeadBase64Encryptor(password.getBytes(StandardCharsets.UTF_8));
            encryptor = encryptorV2;
            s_logger.debug("Initialized with encryptor : " + encryptorV2.getClass().getSimpleName());
        } else {
            encryptorV1 = new LegacyBase64Encryptor(password);
            encryptorV2 = new AeadBase64Encryptor(password.getBytes(StandardCharsets.UTF_8));
            s_logger.debug("Initialized with all possible encryptors");
        }
    }
}
