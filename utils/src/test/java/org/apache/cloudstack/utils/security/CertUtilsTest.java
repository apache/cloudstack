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

package org.apache.cloudstack.utils.security;

import java.security.KeyPair;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.cert.X509Certificate;
import java.security.interfaces.RSAPublicKey;
import java.util.Arrays;
import java.util.List;

import org.bouncycastle.asn1.x509.GeneralName;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

public class CertUtilsTest {
    KeyPair caKeyPair;
    X509Certificate caCertificate;

    @Before
    public void setUp() throws Exception {
        caKeyPair = CertUtils.generateRandomKeyPair(1024);
        caCertificate = CertUtils.generateV3Certificate(null, caKeyPair, caKeyPair.getPublic(), "CN=test", "SHA256WithRSAEncryption", 365, null, null);
    }

    @Test
    public void testGenerateRandomKeyPair() throws Exception {
        final int size = 2048;
        final KeyPair kp = CertUtils.generateRandomKeyPair(size);
        Assert.assertEquals(((RSAPublicKey)kp.getPublic()).getModulus().bitLength(), size);
    }

    @Test
    public void testCertificateConversionMethods() throws Exception {
        final X509Certificate in = caCertificate;
        final String pem = CertUtils.x509CertificateToPem(in);
        final X509Certificate out = CertUtils.pemToX509Certificate(pem);
        Assert.assertTrue(pem.startsWith("-----BEGIN CERTIFICATE-----\n"));
        Assert.assertTrue(pem.endsWith("-----END CERTIFICATE-----\n"));
        Assert.assertEquals(in.getSerialNumber(), out.getSerialNumber());
        Assert.assertArrayEquals(in.getSignature(), out.getSignature());
        Assert.assertEquals(in.getSigAlgName(), out.getSigAlgName());
        Assert.assertEquals(in.getPublicKey(), out.getPublicKey());
        Assert.assertEquals(in.getNotBefore(), out.getNotBefore());
        Assert.assertEquals(in.getNotAfter(), out.getNotAfter());
        Assert.assertEquals(in.getIssuerDN().toString(), out.getIssuerDN().toString());
    }

    @Test
    public void testKeysConversionMethods() throws Exception {
        final KeyPair kp = CertUtils.generateRandomKeyPair(2048);

        final PrivateKey inPrivateKey = kp.getPrivate();
        final PrivateKey outPrivateKey = CertUtils.pemToPrivateKey(CertUtils.privateKeyToPem(inPrivateKey));
        Assert.assertEquals(inPrivateKey.getAlgorithm(), outPrivateKey.getAlgorithm());
        Assert.assertEquals(inPrivateKey.getFormat(), outPrivateKey.getFormat());
        Assert.assertArrayEquals(inPrivateKey.getEncoded(), outPrivateKey.getEncoded());

        final PublicKey inPublicKey = kp.getPublic();
        final PublicKey outPublicKey = CertUtils.pemToPublicKey(CertUtils.publicKeyToPem(inPublicKey));
        Assert.assertEquals(inPublicKey.getAlgorithm(), outPublicKey.getAlgorithm());
        Assert.assertEquals(inPublicKey.getFormat(), inPublicKey.getFormat());
        Assert.assertArrayEquals(inPublicKey.getEncoded(), outPublicKey.getEncoded());
    }

    @Test
    public void testGenerateRandomBigInt() throws Exception {
        Assert.assertNotEquals(CertUtils.generateRandomBigInt(), CertUtils.generateRandomBigInt());
    }

    @Test
    public void testGenerateCertificate() throws Exception {
        final KeyPair clientKeyPair = CertUtils.generateRandomKeyPair(1024);
        final List<String> domainNames = Arrays.asList("domain1.com", "www.2.domain2.com", "3.domain3.com");
        final List<String> addressList = Arrays.asList("1.2.3.4", "192.168.1.1", "2a02:120b:2c16:f6d0:d9df:8ebc:e44a:f181");

        final X509Certificate clientCert = CertUtils.generateV3Certificate(caCertificate, caKeyPair, clientKeyPair.getPublic(),
                "CN=domain.example", "SHA256WithRSAEncryption", 10, domainNames, addressList);

        clientCert.verify(caKeyPair.getPublic());
        Assert.assertEquals(clientCert.getIssuerDN(), caCertificate.getIssuerDN());
        Assert.assertEquals(clientCert.getSigAlgName(), "SHA256WITHRSA");
        Assert.assertArrayEquals(clientCert.getPublicKey().getEncoded(), clientKeyPair.getPublic().getEncoded());
        Assert.assertNotNull(clientCert.getSubjectAlternativeNames());

        for (final List<?> altNames : clientCert.getSubjectAlternativeNames()) {
            Assert.assertTrue(altNames.size() == 2);
            final Object first = altNames.get(0);
            final Object second = altNames.get(1);
            if (first instanceof Integer && ((Integer) first) == GeneralName.iPAddress) {
                Assert.assertTrue(addressList.contains((String) second));
            }
            if (first instanceof Integer && ((Integer) first) == GeneralName.dNSName) {
                Assert.assertTrue(domainNames.contains((String) second));
            }
        }
    }

}
