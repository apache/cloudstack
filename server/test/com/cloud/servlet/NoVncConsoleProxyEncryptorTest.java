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

import java.nio.charset.Charset;
import java.util.HashMap;
import java.util.Map;

import org.junit.Assert;
import org.junit.Test;

import org.apache.commons.codec.binary.Base64;
import org.apache.commons.codec.binary.Hex;
import org.bouncycastle.crypto.engines.RijndaelEngine;
import org.bouncycastle.crypto.modes.CBCBlockCipher;
import org.bouncycastle.crypto.paddings.PaddedBufferedBlockCipher;
import org.bouncycastle.crypto.paddings.ZeroBytePadding;
import org.bouncycastle.crypto.params.KeyParameter;
import org.bouncycastle.crypto.params.ParametersWithIV;

public class NoVncConsoleProxyEncryptorTest {

    @Test
    public void testRijndael() throws Exception {
        byte[] key = "default.key.for.novnc".getBytes(Charset.forName("ASCII"));
        byte[] iv = "default.iv.for.novnc".getBytes(Charset.forName("ASCII"));
        byte[] plaintext = "text-to-encode-with-rijndael-ecb".getBytes(Charset.forName("UTF-8"));

        PaddedBufferedBlockCipher cipher = new PaddedBufferedBlockCipher(
            new RijndaelEngine(256), new ZeroBytePadding());

        final int keysize = 256;
        byte[] keyData = new byte[keysize / Byte.SIZE];
        System.arraycopy(key, 0, keyData, 0, Math.min(key.length, keyData.length));
        KeyParameter keyParameter = new KeyParameter(keyData);

        cipher.init(true, keyParameter);
        byte[] encrypted  = new byte[cipher.getOutputSize(plaintext.length)];
        int offset = cipher.processBytes(plaintext, 0, plaintext.length, encrypted, 0);
        cipher.doFinal(encrypted, offset);
        System.out.println("encrypted ECB is " + Base64.encodeBase64String(encrypted));

        cipher.reset();
        cipher.init(false, keyParameter);
        byte[] decryptedtext = new byte[cipher.getOutputSize(encrypted.length)];
        offset = cipher.processBytes(encrypted, 0, encrypted.length, decryptedtext, 0);
        offset += cipher.doFinal(decryptedtext, offset);
        byte[] result = new byte[offset];
        System.arraycopy(decryptedtext, 0, result, 0, offset);

        System.out.println("decrypted ECB is " + new String(result));
        Assert.assertEquals(new String(result), "text-to-encode-with-rijndael-ecb");

    }

    @Test
    public void testRijndaelCBC() throws Exception {
        byte[] key = "default.key.for.novnc".getBytes(Charset.forName("ASCII"));
        byte[] iv = "default.iv.for.novnc".getBytes(Charset.forName("ASCII"));
        byte[] plaintext = "text-to-encode-with-rijndael-cbc".getBytes(Charset.forName("UTF-8"));

        PaddedBufferedBlockCipher cipher = new PaddedBufferedBlockCipher(
            new CBCBlockCipher(new RijndaelEngine(256)), new ZeroBytePadding());

        final int keysize = 256;
        byte[] keyData = new byte[keysize / Byte.SIZE];
        System.arraycopy(key, 0, keyData, 0, Math.min(key.length, keyData.length));
        KeyParameter keyParameter = new KeyParameter(keyData);

        byte[] ivData = new byte[keysize / Byte.SIZE];
        System.arraycopy(iv, 0, ivData, 0, Math.min(iv.length, ivData.length));
        ParametersWithIV ivAndKey = new ParametersWithIV(keyParameter, ivData);

        cipher.init(true, ivAndKey);
        byte[] encrypted  = new byte[cipher.getOutputSize(plaintext.length)];
        int offset = cipher.processBytes(plaintext, 0, plaintext.length, encrypted, 0);
        cipher.doFinal(encrypted, offset);
        System.out.println("encrypted CBC is " + Base64.encodeBase64String(encrypted));

        cipher.reset();
        cipher.init(false, ivAndKey);
        byte[] decryptedtext = new byte[cipher.getOutputSize(encrypted.length)];
        offset = cipher.processBytes(encrypted, 0, encrypted.length, decryptedtext, 0);
        offset += cipher.doFinal(decryptedtext, offset);
        byte[] result = new byte[offset];
        System.arraycopy(decryptedtext, 0, result, 0, offset);

        System.out.println("decrypted CBC is " + new String(result));
        Assert.assertEquals(new String(result), "text-to-encode-with-rijndael-cbc");
    }

    @Test
    public void testEncrypt() throws Exception {
        NoVncConsoleProxyClientParam param = new NoVncConsoleProxyClientParam();
        param.setProxy("9.9.9.9");
        param.setVmName("testvm");
        param.setClientHostAddress("10.10.10.10");
        param.setClientHostPort(5901);
        param.setClientHostPassword("vnc_password");
        param.setClientIp("11.11.11.11");
        param.setClientTag("7f304181-3242-4867-9a26-106063139cba");

        String key = "default.key.for.novnc";
        String iv = "default.iv.for.novnc";
        String host = param.getClientHostAddress();
        int port = param.getClientHostPort();
        String password = param.getClientHostPassword();
        String clientIp = param.getClientIp();
        long timestamp = System.currentTimeMillis() / 1000;
        String proxy = param.getProxy();
        String vmName = param.getVmName();

        final StringBuilder data = new StringBuilder();
        data.append(host).append(":").append(port).append(":").append(clientIp).append(":").append(timestamp);

        String dataHash = NoVncConsoleProxyEncryptor.hash(key + data.toString());
        data.append("|").append(dataHash);

        System.out.println("data is " + data);

        // Rijndael ECB
        NoVncConsoleProxyEncryptor encryptor = new NoVncConsoleProxyEncryptor(key, iv);
        byte[] encrypted = encryptor.encryptText(data.toString());
        String encryptedText = Hex.encodeHexString(encrypted);
        System.out.println("encrypted text is " + encryptedText);
        String decrypted = encryptor.decryptText(encrypted);
        System.out.println("decrypted is " + decrypted);
        Assert.assertEquals(decrypted, data.toString());

        // Rijndael + CBC
        byte[] encryptedCBC = encryptor.encryptTextCBC(data.toString());
        String encryptedTextCBC = Hex.encodeHexString(encryptedCBC);
        System.out.println("encryptedCBC text is " + encryptedTextCBC);
        String decryptedCBC = encryptor.decryptTextCBC(encryptedCBC);
        System.out.println("decryptedCBC is " + decryptedCBC);
        Assert.assertEquals(decryptedCBC, data.toString());

        Map<String, String> input = new HashMap<String, String>();
        input.put("port", "8080");
        input.put("host", proxy);
        input.put("password", password);
        input.put("path", encryptedText);
        input.put("displayName", vmName);

        System.out.println("url is " + NoVncConsoleProxyEncryptor.httpBuildQuery(input));

    }
}
