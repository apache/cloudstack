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

package org.apache.cloudstack.ca.provider;

import java.math.BigInteger;
import java.security.KeyPair;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.List;

import org.apache.cloudstack.utils.security.CertUtils;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.MockitoJUnitRunner;

import com.cloud.certificate.CrlVO;
import com.cloud.certificate.dao.CrlDao;

@RunWith(MockitoJUnitRunner.class)
public class RootCACustomTrustManagerTest {

    @Mock
    private CrlDao crlDao;
    private KeyPair caKeypair;
    private KeyPair clientKeypair;
    private X509Certificate caCertificate;
    private X509Certificate expiredClientCertificate;
    private X509Certificate validClientCertificate;
    private X509Certificate foreignClientCertificate;
    private String clientIp = "1.2.3.4";
    private Map<String, X509Certificate> certMap = new HashMap<>();

    @Before
    public void setUp() throws Exception {
        certMap.clear();
        caKeypair = CertUtils.generateRandomKeyPair(1024);
        clientKeypair = CertUtils.generateRandomKeyPair(1024);
        caCertificate = CertUtils.generateV3Certificate(null, caKeypair, caKeypair.getPublic(), "CN=ca", "SHA256withRSA", 365, null, null);
        expiredClientCertificate = CertUtils.generateV3Certificate(caCertificate, caKeypair, clientKeypair.getPublic(),
                "CN=cloudstack.apache.org", "SHA256withRSA", 0, Collections.singletonList("cloudstack.apache.org"), Collections.singletonList(clientIp));
        validClientCertificate = CertUtils.generateV3Certificate(caCertificate, caKeypair, clientKeypair.getPublic(),
                "CN=cloudstack.apache.org", "SHA256withRSA", 365, Collections.singletonList("cloudstack.apache.org"), Collections.singletonList(clientIp));

        // A certificate signed by a rogue CA but crafted with a SAN matching the client IP and a serial absent
        // from the CRL. It passes every check except the CA signature verification.
        final KeyPair rogueCaKeypair = CertUtils.generateRandomKeyPair(1024);
        final KeyPair rogueClientKeypair = CertUtils.generateRandomKeyPair(1024);
        final X509Certificate rogueCaCertificate = CertUtils.generateV3Certificate(null, rogueCaKeypair, rogueCaKeypair.getPublic(),
                "CN=rogue-ca", "SHA256withRSA", 365, null, null);
        foreignClientCertificate = CertUtils.generateV3Certificate(rogueCaCertificate, rogueCaKeypair, rogueClientKeypair.getPublic(),
                "CN=cloudstack.apache.org", "SHA256withRSA", 365, Collections.singletonList("cloudstack.apache.org"), Collections.singletonList(clientIp));
    }

    @Test
    public void testAuthNotStrictWithInvalidCert() throws Exception {
        final RootCACustomTrustManager trustManager = new RootCACustomTrustManager(clientIp, false, true, false, certMap, Collections.singletonList(caCertificate), crlDao);
        trustManager.checkClientTrusted(null, null);
    }

    @Test
    public void testAuthNotStrictWithRevokedCert() throws Exception {
        Mockito.when(crlDao.findBySerial(Mockito.any(BigInteger.class))).thenReturn(new CrlVO());
        final RootCACustomTrustManager trustManager = new RootCACustomTrustManager(clientIp, false, true, false, certMap, Collections.singletonList(caCertificate), crlDao);
        trustManager.checkClientTrusted(new X509Certificate[]{caCertificate}, "RSA");
        Assert.assertTrue(certMap.containsKey(clientIp));
        Assert.assertEquals(certMap.get(clientIp), caCertificate);
    }

    @Test
    public void testAuthNotStrictWithInvalidCertOwnership() throws Exception {
        Mockito.when(crlDao.findBySerial(Mockito.any(BigInteger.class))).thenReturn(null);
        final RootCACustomTrustManager trustManager = new RootCACustomTrustManager(clientIp, false, true, false, certMap, Collections.singletonList(caCertificate), crlDao);
        trustManager.checkClientTrusted(new X509Certificate[]{caCertificate}, "RSA");
        Assert.assertTrue(certMap.containsKey(clientIp));
        Assert.assertEquals(certMap.get(clientIp), caCertificate);
    }

    @Test(expected = CertificateException.class)
    public void testAuthNotStrictWithDenyExpiredCertAndOwnership() throws Exception {
        Mockito.when(crlDao.findBySerial(Mockito.any(BigInteger.class))).thenReturn(null);
        final RootCACustomTrustManager trustManager = new RootCACustomTrustManager(clientIp, false, false, false, certMap, Collections.singletonList(caCertificate), crlDao);
        trustManager.checkClientTrusted(new X509Certificate[]{expiredClientCertificate}, "RSA");
    }

    @Test
    public void testAuthNotStrictWithAllowExpiredCertAndOwnership() throws Exception {
        Mockito.when(crlDao.findBySerial(Mockito.any(BigInteger.class))).thenReturn(null);
        final RootCACustomTrustManager trustManager = new RootCACustomTrustManager(clientIp, false, true, false, certMap, Collections.singletonList(caCertificate), crlDao);
        trustManager.checkClientTrusted(new X509Certificate[]{expiredClientCertificate}, "RSA");
        Assert.assertTrue(certMap.containsKey(clientIp));
        Assert.assertEquals(certMap.get(clientIp), expiredClientCertificate);
    }

    @Test(expected = CertificateException.class)
    public void testAuthStrictWithInvalidCert() throws Exception {
        final RootCACustomTrustManager trustManager = new RootCACustomTrustManager(clientIp, true, true, false, certMap, Collections.singletonList(caCertificate), crlDao);
        trustManager.checkClientTrusted(null, null);
    }

    @Test(expected = CertificateException.class)
    public void testAuthStrictWithRevokedCert() throws Exception {
        Mockito.when(crlDao.findBySerial(Mockito.any(BigInteger.class))).thenReturn(new CrlVO());
        final RootCACustomTrustManager trustManager = new RootCACustomTrustManager(clientIp, true, true, false, certMap, Collections.singletonList(caCertificate), crlDao);
        trustManager.checkClientTrusted(new X509Certificate[]{caCertificate}, "RSA");
    }

    @Test(expected = CertificateException.class)
    public void testAuthStrictWithInvalidCertOwnership() throws Exception {
        Mockito.when(crlDao.findBySerial(Mockito.any(BigInteger.class))).thenReturn(null);
        final RootCACustomTrustManager trustManager = new RootCACustomTrustManager(clientIp, true, true, false, certMap, Collections.singletonList(caCertificate), crlDao);
        trustManager.checkClientTrusted(new X509Certificate[]{caCertificate}, "RSA");
    }

    @Test(expected = CertificateException.class)
    public void testAuthStrictWithDenyExpiredCertAndOwnership() throws Exception {
        Mockito.when(crlDao.findBySerial(Mockito.any(BigInteger.class))).thenReturn(null);
        final RootCACustomTrustManager trustManager = new RootCACustomTrustManager(clientIp, true, false, false, certMap, Collections.singletonList(caCertificate), crlDao);
        trustManager.checkClientTrusted(new X509Certificate[]{expiredClientCertificate}, "RSA");
    }

    @Test
    public void testGetAcceptedIssuersWithChain() throws Exception {
        final KeyPair rootKeyPair = CertUtils.generateRandomKeyPair(1024);
        final X509Certificate rootCert = CertUtils.generateV3Certificate(null, rootKeyPair, rootKeyPair.getPublic(),
                "CN=root", "SHA256withRSA", 365, null, null);
        final List<X509Certificate> chain = Arrays.asList(caCertificate, rootCert);
        final RootCACustomTrustManager trustManager = new RootCACustomTrustManager(
                clientIp, false, true, false, certMap, chain, crlDao);

        final X509Certificate[] issuers = trustManager.getAcceptedIssuers();
        Assert.assertEquals(2, issuers.length);
        Assert.assertEquals(caCertificate, issuers[0]);
        Assert.assertEquals(rootCert, issuers[1]);
    }

    @Test
    public void testAuthStrictWithAllowExpiredCertAndOwnership() throws Exception {
        Mockito.when(crlDao.findBySerial(Mockito.any(BigInteger.class))).thenReturn(null);
        final RootCACustomTrustManager trustManager = new RootCACustomTrustManager(clientIp, true, true, false, certMap, Collections.singletonList(caCertificate), crlDao);
        Assert.assertTrue(trustManager.getAcceptedIssuers() != null);
        Assert.assertTrue(trustManager.getAcceptedIssuers().length == 1);
        Assert.assertEquals(trustManager.getAcceptedIssuers()[0], caCertificate);
        trustManager.checkClientTrusted(new X509Certificate[]{expiredClientCertificate}, "RSA");
        Assert.assertTrue(certMap.containsKey(clientIp));
        Assert.assertEquals(certMap.get(clientIp), expiredClientCertificate);
    }

    @Test
    public void testSignatureVerificationEnabledWithValidCert() throws Exception {
        Mockito.when(crlDao.findBySerial(Mockito.any(BigInteger.class))).thenReturn(null);
        final RootCACustomTrustManager trustManager = new RootCACustomTrustManager(clientIp, true, false, true, certMap, Collections.singletonList(caCertificate), crlDao);
        trustManager.checkClientTrusted(new X509Certificate[]{validClientCertificate}, "RSA");
        Assert.assertTrue(certMap.containsKey(clientIp));
        Assert.assertEquals(certMap.get(clientIp), validClientCertificate);
    }

    @Test
    public void testSignatureVerificationEnabledWithCertSignedByRotatedCA() throws Exception {
        // The trusted CA list contains a decoy plus the actual signer; verification must
        // succeed as long as any entry in the list signed the certificate.
        final KeyPair otherCaKeyPair = CertUtils.generateRandomKeyPair(1024);
        final X509Certificate otherCaCertificate = CertUtils.generateV3Certificate(null, otherCaKeyPair, otherCaKeyPair.getPublic(),
                "CN=other-ca", "SHA256withRSA", 365, null, null);
        Mockito.when(crlDao.findBySerial(Mockito.any(BigInteger.class))).thenReturn(null);
        final List<X509Certificate> caCertificates = Arrays.asList(otherCaCertificate, caCertificate);
        final RootCACustomTrustManager trustManager = new RootCACustomTrustManager(clientIp, true, false, true, certMap, caCertificates, crlDao);
        trustManager.checkClientTrusted(new X509Certificate[]{validClientCertificate}, "RSA");
        Assert.assertTrue(certMap.containsKey(clientIp));
        Assert.assertEquals(certMap.get(clientIp), validClientCertificate);
    }

    @Test(expected = CertificateException.class)
    public void testSignatureVerificationEnabledStrictWithForeignCert() throws Exception {
        Mockito.when(crlDao.findBySerial(Mockito.any(BigInteger.class))).thenReturn(null);
        final RootCACustomTrustManager trustManager = new RootCACustomTrustManager(clientIp, true, true, true, certMap, Collections.singletonList(caCertificate), crlDao);
        trustManager.checkClientTrusted(new X509Certificate[]{foreignClientCertificate}, "RSA");
    }

    @Test
    public void testSignatureVerificationDisabledStrictWithForeignCert() throws Exception {
        Mockito.when(crlDao.findBySerial(Mockito.any(BigInteger.class))).thenReturn(null);
        final RootCACustomTrustManager trustManager = new RootCACustomTrustManager(clientIp, true, true, false, certMap, Collections.singletonList(caCertificate), crlDao);
        trustManager.checkClientTrusted(new X509Certificate[]{foreignClientCertificate}, "RSA");
        Assert.assertTrue(certMap.containsKey(clientIp));
        Assert.assertEquals(certMap.get(clientIp), foreignClientCertificate);
    }

    @Test
    public void testSignatureVerificationEnabledNotStrictWithForeignCert() throws Exception {
        Mockito.when(crlDao.findBySerial(Mockito.any(BigInteger.class))).thenReturn(null);
        final RootCACustomTrustManager trustManager = new RootCACustomTrustManager(clientIp, false, true, true, certMap, Collections.singletonList(caCertificate), crlDao);
        trustManager.checkClientTrusted(new X509Certificate[]{foreignClientCertificate}, "RSA");
        Assert.assertTrue(certMap.containsKey(clientIp));
        Assert.assertEquals(certMap.get(clientIp), foreignClientCertificate);
    }

    @Test(expected = CertificateException.class)
    public void testSignatureVerificationEnabledStrictWithNoCaCertificates() throws Exception {
        final RootCACustomTrustManager trustManager = new RootCACustomTrustManager(clientIp, true, true, true, certMap, new ArrayList<>(), crlDao);
        trustManager.checkClientTrusted(new X509Certificate[]{validClientCertificate}, "RSA");
    }

    @Test
    public void testSignatureVerificationEnabledNotStrictWithNoCaCertificates() throws Exception {
        Mockito.when(crlDao.findBySerial(Mockito.any(BigInteger.class))).thenReturn(null);
        final RootCACustomTrustManager trustManager = new RootCACustomTrustManager(clientIp, false, true, true, certMap, new ArrayList<>(), crlDao);
        trustManager.checkClientTrusted(new X509Certificate[]{validClientCertificate}, "RSA");
        Assert.assertTrue(certMap.containsKey(clientIp));
    }

    @Test
    public void testCheckServerTrustedVerificationDisabledIsNoop() throws Exception {
        final RootCACustomTrustManager trustManager = new RootCACustomTrustManager(clientIp, true, true, false, certMap, Collections.singletonList(caCertificate), crlDao);
        trustManager.checkServerTrusted(new X509Certificate[]{foreignClientCertificate}, "RSA");
        trustManager.checkServerTrusted(null, "RSA");
    }

    @Test
    public void testCheckServerTrustedVerificationEnabledWithCaSignedCert() throws Exception {
        final RootCACustomTrustManager trustManager = new RootCACustomTrustManager(clientIp, true, true, true, certMap, Collections.singletonList(caCertificate), crlDao);
        trustManager.checkServerTrusted(new X509Certificate[]{validClientCertificate}, "RSA");
    }

    @Test(expected = CertificateException.class)
    public void testCheckServerTrustedVerificationEnabledWithForeignCert() throws Exception {
        final RootCACustomTrustManager trustManager = new RootCACustomTrustManager(clientIp, true, true, true, certMap, Collections.singletonList(caCertificate), crlDao);
        trustManager.checkServerTrusted(new X509Certificate[]{foreignClientCertificate}, "RSA");
    }

    @Test(expected = CertificateException.class)
    public void testCheckServerTrustedVerificationEnabledWithNoCert() throws Exception {
        final RootCACustomTrustManager trustManager = new RootCACustomTrustManager(clientIp, true, true, true, certMap, Collections.singletonList(caCertificate), crlDao);
        trustManager.checkServerTrusted(null, "RSA");
    }

    @Test(expected = CertificateException.class)
    public void testCheckServerTrustedVerificationEnabledStrictWithNoCaCertificates() throws Exception {
        final RootCACustomTrustManager trustManager = new RootCACustomTrustManager(clientIp, true, true, true, certMap, new ArrayList<>(), crlDao);
        trustManager.checkServerTrusted(new X509Certificate[]{validClientCertificate}, "RSA");
    }

    @Test
    public void testCheckServerTrustedVerificationEnabledNotStrictWithNoCaCertificates() throws Exception {
        final RootCACustomTrustManager trustManager = new RootCACustomTrustManager(clientIp, false, true, true, certMap, new ArrayList<>(), crlDao);
        trustManager.checkServerTrusted(new X509Certificate[]{validClientCertificate}, "RSA");
    }

}
