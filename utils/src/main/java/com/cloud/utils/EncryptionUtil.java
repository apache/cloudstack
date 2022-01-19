/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package com.cloud.utils;

import java.io.UnsupportedEncodingException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

import org.apache.commons.codec.binary.Base64;
import org.apache.log4j.Logger;
import org.jasypt.encryption.pbe.PBEStringEncryptor;
import org.jasypt.encryption.pbe.StandardPBEStringEncryptor;

import com.cloud.utils.exception.CloudRuntimeException;

public class EncryptionUtil {
    public static final Logger s_logger = Logger.getLogger(EncryptionUtil.class.getName());
    private static PBEStringEncryptor encryptor;

    private static void initialize(String key) {
        StandardPBEStringEncryptor standardPBEStringEncryptor = new StandardPBEStringEncryptor();
        standardPBEStringEncryptor.setAlgorithm("PBEWITHSHA1ANDDESEDE");
        standardPBEStringEncryptor.setPassword(key);
        encryptor = standardPBEStringEncryptor;
    }

    public static String encodeData(String data, String key) {
        if (encryptor == null) {
            initialize(key);
        }
        return encryptor.encrypt(data);
    }

    public static String decodeData(String encodedData, String key) {
        if (encryptor == null) {
            initialize(key);
        }
        return encryptor.decrypt(encodedData);
    }

    public static String generateSignature(String data, String key) {
        try {
            final Mac mac = Mac.getInstance("HmacSHA1");
            final SecretKeySpec keySpec = new SecretKeySpec(key.getBytes("UTF-8"), "HmacSHA1");
            mac.init(keySpec);
            mac.update(data.getBytes("UTF-8"));
            final byte[] encryptedBytes = mac.doFinal();
            return Base64.encodeBase64String(encryptedBytes);
        } catch (NoSuchAlgorithmException | InvalidKeyException | UnsupportedEncodingException e) {
            s_logger.error("exception occurred which encoding the data." + e.getMessage());
            throw new CloudRuntimeException("unable to generate signature", e);
        }
    }
}
