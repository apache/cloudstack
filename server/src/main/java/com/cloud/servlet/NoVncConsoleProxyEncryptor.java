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

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.nio.charset.Charset;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import org.apache.commons.codec.binary.Hex;
import org.apache.log4j.Logger;

import org.bouncycastle.crypto.InvalidCipherTextException;
import org.bouncycastle.crypto.engines.RijndaelEngine;
import org.bouncycastle.crypto.modes.CBCBlockCipher;
import org.bouncycastle.crypto.paddings.PaddedBufferedBlockCipher;
import org.bouncycastle.crypto.paddings.ZeroBytePadding;
import org.bouncycastle.crypto.params.KeyParameter;
import org.bouncycastle.crypto.params.ParametersWithIV;

import com.cloud.consoleproxy.ConsoleProxyManager;

public class NoVncConsoleProxyEncryptor {
    private static final Logger s_logger = Logger.getLogger(NoVncConsoleProxyEncryptor.class);

    private static String key;
    private static String iv;

    public NoVncConsoleProxyEncryptor(String key, String iv) {
        this.key = key;
        this.iv = iv;
    }

    protected String encrypt(NoVncConsoleProxyClientParam param) {
        if (param == null)
            return null;

        try {
            String host = param.getClientHostAddress();
            int port = param.getClientHostPort();
            String password = param.getClientHostPassword();
            String clientIp = param.getClientIp();
            String proxy = param.getProxy();
            String vmName = param.getVmName();
            long timestamp = System.currentTimeMillis() / 1000;

            final StringBuilder data = new StringBuilder();
            data.append(host).append(":").append(port).append(":").append(clientIp).append(":").append(timestamp);
            String dataHash = hash(key + data.toString());
            data.append("|").append(dataHash);

            String encryptedText = Hex.encodeHexString(encryptText(data.toString()));

            Map<String, String> input = new HashMap<String, String>();
            input.put("port", String.valueOf(ConsoleProxyManager.DEFAULT_NOVNC_PORT));
            input.put("host", proxy);
            input.put("password", password);
            input.put("path", encryptedText);
            input.put("displayName", vmName);

            return httpBuildQuery(input);
        } catch (NoSuchAlgorithmException e) {
            s_logger.error("Unexpected exception ", e);
            return null;
        } catch (InvalidCipherTextException e) {
            s_logger.error("Unexpected exception ", e);
            return null;
        }
    }

    protected static String httpBuildQuery(Map<String, String> input) {
        String result = "";
        Iterator it = input.entrySet().iterator();
        while (it.hasNext()){
            Map.Entry<String,String> entry = (Map.Entry) it.next();
            String key = entry.getKey();
            String value = entry.getValue();
            result += key + "=" + value + "&";
        }
        result = result.substring(0, result.length() - 1);
        try {
            result = URLEncoder.encode(result, "UTF-8");
        } catch (final UnsupportedEncodingException e) {
            return null;
        }
        result = result.replace("%3D", "=").replace("%26", "&");
        return result;
    }

    protected static final String hash(String text) throws NoSuchAlgorithmException {
        MessageDigest messageDigest = MessageDigest.getInstance("SHA-256");
        byte[] textBytes = messageDigest.digest(text.getBytes(Charset.forName("UTF-8")));
        return Hex.encodeHexString(textBytes);
    }

    protected static final byte[] encryptText(String text) throws InvalidCipherTextException {
        if (text == null || text.isEmpty())
            return null;

        byte[] keyBytes = key.getBytes(Charset.forName("ASCII"));
        final int keysize = 256;
        byte[] keyData = new byte[keysize / Byte.SIZE];
        System.arraycopy(keyBytes, 0, keyData, 0, Math.min(keyBytes.length, keyData.length));
        KeyParameter keyParameter = new KeyParameter(keyData);

        PaddedBufferedBlockCipher cipher = new PaddedBufferedBlockCipher(
            new RijndaelEngine(256), new ZeroBytePadding());
        cipher.init(true, keyParameter);

        byte[] plaintext = text.getBytes(Charset.forName("UTF-8"));
        byte[] ciphertext = new byte[cipher.getOutputSize(plaintext.length)];
        int offset = cipher.processBytes(plaintext, 0, plaintext.length, ciphertext, 0);
        offset += cipher.doFinal(ciphertext, offset);
        return ciphertext;
    }

    protected static final byte[] encryptTextCBC(String text) throws InvalidCipherTextException {
        if (text == null || text.isEmpty())
            return null;

        byte[] keyBytes = key.getBytes(Charset.forName("ASCII"));
        final int keysize = 256;
        byte[] keyData = new byte[keysize / Byte.SIZE];
        System.arraycopy(keyBytes, 0, keyData, 0, Math.min(keyBytes.length, keyData.length));
        KeyParameter keyParameter = new KeyParameter(keyData);

        byte[] ivBytes = iv.getBytes(Charset.forName("ASCII"));
        byte[] ivData = new byte[keysize / Byte.SIZE];
        System.arraycopy(ivBytes, 0, ivData, 0, Math.min(ivBytes.length, ivData.length));
        ParametersWithIV parametersWithIV = new ParametersWithIV(keyParameter, ivData);

        PaddedBufferedBlockCipher cipher = new PaddedBufferedBlockCipher(
            new CBCBlockCipher(new RijndaelEngine(256)), new ZeroBytePadding());
        cipher.init(true, parametersWithIV);

        byte[] plaintext = text.getBytes(Charset.forName("UTF-8"));
        byte[] ciphertext = new byte[cipher.getOutputSize(plaintext.length)];
        int offset = cipher.processBytes(plaintext, 0, plaintext.length, ciphertext, 0);
        offset += cipher.doFinal(ciphertext, offset);
        return ciphertext;
    }

    protected static final String decryptText(byte[] text) throws InvalidCipherTextException {
        if (text == null)
            return null;

        byte[] keyBytes = key.getBytes(Charset.forName("ASCII"));
        final int keysize = 256;
        byte[] keyData = new byte[keysize / Byte.SIZE];
        System.arraycopy(keyBytes, 0, keyData, 0, Math.min(keyBytes.length, keyData.length));
        KeyParameter keyParameter = new KeyParameter(keyData);

        PaddedBufferedBlockCipher cipher = new PaddedBufferedBlockCipher(
            new RijndaelEngine(256), new ZeroBytePadding());
        cipher.init(false, keyParameter);

        byte[] decryptedtext = new byte[cipher.getOutputSize(text.length)];
        int offset = cipher.processBytes(text, 0, text.length, decryptedtext, 0);
        offset += cipher.doFinal(decryptedtext, offset);
        byte[] result = new byte[offset];
        System.arraycopy(decryptedtext, 0, result, 0, offset);

        return new String(result);
    }

    protected static final String decryptTextCBC(byte[] text) throws InvalidCipherTextException {
        if (text == null)
            return null;

        byte[] keyBytes = key.getBytes(Charset.forName("ASCII"));
        final int keysize = 256;
        byte[] keyData = new byte[keysize / Byte.SIZE];
        System.arraycopy(keyBytes, 0, keyData, 0, Math.min(keyBytes.length, keyData.length));
        KeyParameter keyParameter = new KeyParameter(keyData);

        byte[] ivBytes = iv.getBytes(Charset.forName("ASCII"));
        byte[] ivData = new byte[keysize / Byte.SIZE];
        System.arraycopy(ivBytes, 0, ivData, 0, Math.min(ivBytes.length, ivData.length));
        ParametersWithIV parametersWithIV = new ParametersWithIV(keyParameter, ivData);

        PaddedBufferedBlockCipher cipher = new PaddedBufferedBlockCipher(
            new CBCBlockCipher(new RijndaelEngine(256)), new ZeroBytePadding());
        cipher.init(false, parametersWithIV);

        byte[] decryptedtext = new byte[cipher.getOutputSize(text.length)];
        int offset = cipher.processBytes(text, 0, text.length, decryptedtext, 0);
        offset += cipher.doFinal(decryptedtext, offset);
        byte[] result = new byte[offset];
        System.arraycopy(decryptedtext, 0, result, 0, offset);

        return new String(result);
    }

}
