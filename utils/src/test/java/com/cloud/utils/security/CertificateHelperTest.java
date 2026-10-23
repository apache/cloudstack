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

package com.cloud.utils.security;

import java.security.Key;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.KeyStore;
import java.security.cert.X509Certificate;
import java.security.spec.InvalidKeySpecException;

import org.apache.cloudstack.utils.security.CertUtils;
import org.apache.commons.codec.binary.Base64;
import org.junit.Assert;
import org.junit.Test;

public class CertificateHelperTest {

    private static String encodeKey(final KeyPair keyPair) {
        return Base64.encodeBase64String(keyPair.getPrivate().getEncoded());
    }

    @Test
    public void testBuildPrivateKeyRsa() throws Exception {
        final KeyPair keyPair = CertUtils.generateRandomKeyPair(2048);
        final Key key = CertificateHelper.buildPrivateKey(encodeKey(keyPair));
        Assert.assertEquals("RSA", key.getAlgorithm());
        Assert.assertArrayEquals(keyPair.getPrivate().getEncoded(), key.getEncoded());
    }

    @Test
    public void testBuildPrivateKeyEc() throws Exception {
        final KeyPairGenerator generator = KeyPairGenerator.getInstance("EC");
        generator.initialize(256);
        final KeyPair keyPair = generator.generateKeyPair();
        final Key key = CertificateHelper.buildPrivateKey(encodeKey(keyPair));
        Assert.assertEquals("EC", key.getAlgorithm());
        Assert.assertArrayEquals(keyPair.getPrivate().getEncoded(), key.getEncoded());
    }

    @Test
    public void testBuildPrivateKeyInvalid() throws Exception {
        try {
            CertificateHelper.buildPrivateKey(Base64.encodeBase64String("not a key".getBytes()));
            Assert.fail("Expected InvalidKeySpecException");
        } catch (final InvalidKeySpecException e) {
            Assert.assertEquals("Private key is not one of the supported types: RSA, EC, DSA", e.getMessage());
        }
    }

    @Test
    public void testBuildKeystoreWithEcCertificate() throws Exception {
        final KeyPairGenerator generator = KeyPairGenerator.getInstance("EC");
        generator.initialize(256);
        final KeyPair keyPair = generator.generateKeyPair();
        final X509Certificate certificate = CertUtils.generateV3Certificate(null, keyPair, keyPair.getPublic(), "CN=test", "SHA256withECDSA", 365, null, null);

        final KeyStore ks = CertificateHelper.buildKeystore("test", CertUtils.x509CertificateToPem(certificate), encodeKey(keyPair), "password");
        Assert.assertTrue(ks.isKeyEntry("test"));
        Assert.assertEquals("EC", ks.getKey("test", "password".toCharArray()).getAlgorithm());
    }
}
