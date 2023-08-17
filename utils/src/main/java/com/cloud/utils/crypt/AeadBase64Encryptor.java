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

import com.google.crypto.tink.Aead;
import com.google.crypto.tink.aead.AeadConfig;
import com.google.crypto.tink.subtle.AesGcmJce;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Base64;

public class AeadBase64Encryptor implements Base64Encryptor {
    Aead aead = null;
    private byte[] aad = new byte[]{};

    private void initEncryptor(byte[] key) {
        try {
            AeadConfig.register();
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(key);
            this.aead = new AesGcmJce(hash);
        } catch (Exception e) {
            throw new EncryptionException("Failed to initialize AeadBase64Encryptor");
        }
    }

    public AeadBase64Encryptor(byte[] key) {
        initEncryptor(key);
    }

    public AeadBase64Encryptor(byte[] key, byte[] aad) {
        initEncryptor(key);
        this.aad = aad;
    }

    @Override
    public String encrypt(String plain) {
        try {
            return Base64.getEncoder().encodeToString(aead.encrypt(plain.getBytes(StandardCharsets.UTF_8), aad));
        } catch (Exception ex) {
            throw new EncryptionException("Failed to encrypt " + plain + ". Error: " + ex.getMessage());
        }
    }

    @Override
    public String decrypt(String encrypted) {
        try {
            return new String(aead.decrypt(Base64.getDecoder().decode(encrypted), aad));
        } catch (Exception ex) {
            throw new EncryptionException("Failed to decrypt " + CloudStackEncryptor.hideValueWithAsterisks(encrypted) + ". Error: " + ex.getMessage());
        }
    }

}
