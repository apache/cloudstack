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
package com.cloud.servlet;

import org.apache.commons.codec.binary.Base64;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.LogManager;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import com.cloud.utils.crypt.AeadBase64Encryptor;
import com.cloud.utils.crypt.Base64Encryptor;

// To maintain independency of console proxy project, we duplicate this class from console proxy project
public class ConsoleProxyPasswordBasedEncryptor {
    protected Logger logger = LogManager.getLogger(getClass());

    private Gson gson;

    // key/IV will be set in 128 bit strength
    private KeyIVPair keyIvPair;

    public ConsoleProxyPasswordBasedEncryptor(String password) {
        gson = new GsonBuilder().create();
        keyIvPair = gson.fromJson(password, KeyIVPair.class);
    }

    public String encryptText(String text) {
        if (text == null || text.isEmpty())
            return text;

        Base64Encryptor encryptor = new AeadBase64Encryptor(keyIvPair.getKeyBytes(), keyIvPair.getIvBytes());
        return encryptor.encrypt(text);
    }

    public String decryptText(String encryptedText) {
        if (encryptedText == null || encryptedText.isEmpty())
            return encryptedText;

        Base64Encryptor encryptor = new AeadBase64Encryptor(keyIvPair.getKeyBytes(), keyIvPair.getIvBytes());
        return encryptor.decrypt(encryptedText);
    }

    public <T> String encryptObject(Class<?> clz, T obj) {
        if (obj == null)
            return null;

        String json = gson.toJson(obj);
        return encryptText(json);
    }

    @SuppressWarnings("unchecked")
    public <T> T decryptObject(Class<?> clz, String encrypted) {
        if (encrypted == null || encrypted.isEmpty())
            return null;

        String json = decryptText(encrypted);
        return (T)gson.fromJson(json, clz);
    }

    public static class KeyIVPair {
        String base64EncodedKeyBytes;
        String base64EncodedIvBytes;

        public KeyIVPair() {
        }

        public KeyIVPair(String base64EncodedKeyBytes, String base64EncodedIvBytes) {
            this.base64EncodedKeyBytes = base64EncodedKeyBytes;
            this.base64EncodedIvBytes = base64EncodedIvBytes;
        }

        public byte[] getKeyBytes() {
            return Base64.decodeBase64(base64EncodedKeyBytes);
        }

        public void setKeyBytes(byte[] keyBytes) {
            base64EncodedKeyBytes = Base64.encodeBase64URLSafeString(keyBytes);
        }

        public byte[] getIvBytes() {
            return Base64.decodeBase64(base64EncodedIvBytes);
        }

        public void setIvBytes(byte[] ivBytes) {
            base64EncodedIvBytes = Base64.encodeBase64URLSafeString(ivBytes);
        }
    }

}
