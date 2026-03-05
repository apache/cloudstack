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
import java.io.StringWriter;
import java.math.BigInteger;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.KeyPair;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.interfaces.RSAPublicKey;

import org.apache.cloudstack.utils.security.CertUtils;
import org.apache.commons.codec.binary.Base64;
import org.bouncycastle.util.io.pem.PemObject;
import org.bouncycastle.util.io.pem.PemWriter;

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
            keyPair = CertUtils.generateRandomKeyPair(keyLength);
        } catch (NoSuchAlgorithmException | NoSuchProviderException e) {
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
        if (keyPair == null || keyPair.getPublic() == null) {
            return null;
        }
        try {
            RSAPublicKey rsaPublicKey = (RSAPublicKey) keyPair.getPublic();

            ByteArrayOutputStream buffer = new ByteArrayOutputStream();

            writeString(buffer,"ssh-rsa");
            writeBigInt(buffer, rsaPublicKey.getPublicExponent());
            writeBigInt(buffer, rsaPublicKey.getModulus());

            String base64 = Base64.encodeBase64String(buffer.toByteArray());

            return "ssh-rsa " + base64;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    private static void writeString(ByteArrayOutputStream out, String str) throws Exception {
        byte[] data = str.getBytes(StandardCharsets.UTF_8);
        out.write(ByteBuffer.allocate(4).putInt(data.length).array());
        out.write(data);
    }

    private static void writeBigInt(ByteArrayOutputStream out, BigInteger value) throws Exception {
        byte[] data = value.toByteArray();
        out.write(ByteBuffer.allocate(4).putInt(data.length).array());
        out.write(data);
    }

    public String getPrivateKey() {
        if (keyPair == null || keyPair.getPrivate() == null) {
            return null;
        }
        try {
            final PemObject pemObject = new PemObject("RSA PRIVATE KEY", keyPair.getPrivate().getEncoded());
            final StringWriter sw = new StringWriter();
            try (final PemWriter pw = new PemWriter(sw)) {
                pw.writeObject(pemObject);
            }
            return sw.toString();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

}
