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

package com.cloud.utils.ssh;

import java.io.ByteArrayOutputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

import org.apache.commons.codec.binary.Base64;

import com.jcraft.jsch.JSch;
import com.jcraft.jsch.JSchException;
import com.jcraft.jsch.KeyPair;

public class SSHKeysHelper {

    private KeyPair keyPair;
    private static final char[] hexChars = {'0', '1', '2', '3', '4', '5', '6', '7', '8', '9', 'a', 'b', 'c', 'd', 'e', 'f'};

    private static String toHexString(byte[] b) {
        StringBuffer sb = new StringBuffer();
        for (int i = 0; i < b.length; i++) {
            sb.append(hexChars[(b[i] >> 4) & 0x0f]);
            sb.append(hexChars[(b[i]) & 0x0f]);
        }
        return sb.toString();
    }

    public SSHKeysHelper(Integer keyLength) {
        try {
            keyPair = KeyPair.genKeyPair(new JSch(), KeyPair.RSA, keyLength);
        } catch (JSchException e) {
            e.printStackTrace();
        }
    }

    public String getPublicKeyFingerPrint() {
        return getPublicKeyFingerprint(getPublicKey());
    }

    public static String getPublicKeyFingerprint(String publicKey) {
        String key[] = publicKey.split(" ");
        if (key.length < 2) {
            throw new RuntimeException("Incorrect public key is passed in");
        }
        byte[] keyBytes = Base64.decodeBase64(key[1]);

        MessageDigest md5 = null;
        try {
            md5 = MessageDigest.getInstance("MD5");
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        }

        String rString = "";
        String sumString = "";
        if (md5 != null) {
            sumString = toHexString(md5.digest(keyBytes));
        }

        for (int i = 2; i <= sumString.length(); i += 2) {
            rString += sumString.substring(i - 2, i);
            if (i != sumString.length())
                rString += ":";
        }

        return rString;
    }

    public static String getPublicKeyFromKeyMaterial(String keyMaterial) {
        if (!keyMaterial.contains(" "))
            keyMaterial = new String(Base64.decodeBase64(keyMaterial.getBytes()));

        if ((!keyMaterial.startsWith("ssh-rsa")
             && !keyMaterial.startsWith("ssh-dss")
             && !keyMaterial.startsWith("ecdsa-sha2-nistp256")
             && !keyMaterial.startsWith("ecdsa-sha2-nistp384")
             && !keyMaterial.startsWith("ecdsa-sha2-nistp521")
             && !keyMaterial.startsWith("ssh-ed25519"))
             || !keyMaterial.contains(" "))
            return null;

        String[] key = keyMaterial.split(" ");
        if (key.length < 2)
            return null;

        return key[0].concat(" ").concat(key[1]);
    }

    public String getPublicKey() {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        keyPair.writePublicKey(baos, "");

        return baos.toString();
    }

    public String getPrivateKey() {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        keyPair.writePrivateKey(baos);

        return baos.toString();
    }

}
