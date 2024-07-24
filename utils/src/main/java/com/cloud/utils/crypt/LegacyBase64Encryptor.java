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

import org.jasypt.encryption.pbe.StandardPBEStringEncryptor;
import org.jasypt.encryption.pbe.config.SimpleStringPBEConfig;

public class LegacyBase64Encryptor implements Base64Encryptor {
    StandardPBEStringEncryptor encryptor;

    public LegacyBase64Encryptor(String password) {
        try {
            encryptor = new StandardPBEStringEncryptor();
            encryptor.setAlgorithm("PBEWithMD5AndDES");
            encryptor.setPassword(password);
            SimpleStringPBEConfig stringConfig = new SimpleStringPBEConfig();
            encryptor.setConfig(stringConfig);
        } catch (Exception e) {
            throw new EncryptionException("Failed to initialize LegacyBase64Encryptor");
        }
    }

    @Override
    public String encrypt(String plain) {
        try {
            return encryptor.encrypt(plain);
        } catch (Exception ex) {
            throw new EncryptionException("Failed to encrypt " + plain + ". Error: " + ex.getMessage());
        }
    }

    @Override
    public String decrypt(String encrypted) {
        try {
            return encryptor.decrypt(encrypted);
        } catch (Exception ex) {
            throw new EncryptionException("Failed to decrypt " + CloudStackEncryptor.hideValueWithAsterisks(encrypted) + ". Error: " + ex.getMessage());
        }
    }

}
