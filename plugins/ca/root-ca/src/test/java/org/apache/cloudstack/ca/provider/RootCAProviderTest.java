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

import java.lang.reflect.Field;
import java.security.InvalidKeyException;
import java.security.KeyPair;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.SignatureException;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.Arrays;

import javax.net.ssl.SSLEngine;

import org.apache.cloudstack.framework.ca.Certificate;
import org.apache.cloudstack.framework.config.ConfigKey;
import org.apache.cloudstack.utils.security.CertUtils;
import org.apache.cloudstack.utils.security.SSLUtils;
import org.joda.time.DateTime;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import org.mockito.junit.MockitoJUnitRunner;
import org.mockito.Mockito;


@RunWith(MockitoJUnitRunner.class)
public class RootCAProviderTest {

    private KeyPair caKeyPair;
    private X509Certificate caCertificate;

    private RootCAProvider provider;

    private void addField(final RootCAProvider provider, final String name, final Object o) throws IllegalAccessException, NoSuchFieldException {
        Field f = RootCAProvider.class.getDeclaredField(name);
        f.setAccessible(true);
        f.set(provider, o);
    }

    @Before
    public void setUp() throws Exception {
        caKeyPair = CertUtils.generateRandomKeyPair(1024);
        caCertificate = CertUtils.generateV3Certificate(null, caKeyPair, caKeyPair.getPublic(), "CN=ca", "SHA256withRSA", 365, null, null);

        provider = new RootCAProvider();

        addField(provider, "caKeyPair", caKeyPair);
        addField(provider, "caCertificate", caCertificate);
        addField(provider, "caKeyPair", caKeyPair);
    }

    @After
    public void tearDown() throws Exception {
    }

    @Test
    public void testCanProvisionCertificates() {
        Assert.assertTrue(provider.canProvisionCertificates());
    }

    @Test
    public void testGetCaCertificate() {
        Assert.assertTrue(provider.getCaCertificate().size() == 1);
        Assert.assertEquals(provider.getCaCertificate().get(0), caCertificate);
    }

    @Test
    public void testIssueCertificateWithoutCsr() throws NoSuchProviderException, CertificateException, NoSuchAlgorithmException, InvalidKeyException, SignatureException {
        final Certificate certificate = provider.issueCertificate(Arrays.asList("domain1.com", "domain2.com"), null, 1);
        Assert.assertTrue(certificate != null);
        Assert.assertTrue(certificate.getPrivateKey() != null);
        Assert.assertEquals(certificate.getCaCertificates().get(0), caCertificate);
        Assert.assertEquals(certificate.getClientCertificate().getIssuerDN(), caCertificate.getIssuerDN());
        Assert.assertTrue(certificate.getClientCertificate().getNotAfter().before(new DateTime().plusDays(1).toDate()));
        certificate.getClientCertificate().verify(caCertificate.getPublicKey());
    }

    @Test
    public void testIssueCertificateWithCsr() throws NoSuchProviderException, CertificateException, NoSuchAlgorithmException, InvalidKeyException, SignatureException {
        final String csr = "-----BEGIN NEW CERTIFICATE REQUEST-----\n" +
                "MIICxTCCAa0CAQAwUDETMBEGA1UEBhMKY2xvdWRzdGFjazETMBEGA1UEChMKY2xvdWRzdGFjazET\n" +
                "MBEGA1UECxMKY2xvdWRzdGFjazEPMA0GA1UEAxMGdi0xLVZNMIIBIjANBgkqhkiG9w0BAQEFAAOC\n" +
                "AQ8AMIIBCgKCAQEAhi3hOrt/p0hUmoW2A+2gFAMxSINItRrHfQ6VUnHhYKZGcTN9honVFuu30tz7\n" +
                "oSLUUx1laWEWLlIozpUcPSjOuPa5a0JS8kjplMd8DLfLNeQ6gcuEWznMRJqCaKM72qn/FAK3r11l\n" +
                "2NofEfWbHU5QVQ5CsYF0JndspLcnmf0tnmreAzz6vlSEPQd4g2hTSsPb72eAqYd0eJnl2oXe7cF3\n" +
                "iemg6/lWoxlh8njVFDKJ5ibNQA/RSc5syzzaQ8fn/AkZlChR5pml47elfC3GuqetfZPAEP4rebXV\n" +
                "zEw+UVbMo5bWx4AYm1S2HxhmsWC/1J5oxluZDtC6tjMqnkKQze8HbQIDAQABoDAwLgYJKoZIhvcN\n" +
                "AQkOMSEwHzAdBgNVHQ4EFgQUdgA1C/7vW3lUcb/dnolGjZB55/AwDQYJKoZIhvcNAQELBQADggEB\n" +
                "AH6ynWbyW5o4h2yEvmcr+upmu/LZYkpfwIWIo+dfrHX9OHu0rhHDIgMgqEStWzrOfhAkcEocQo21\n" +
                "E4Q39nECO+cgTCQ1nfH5BVqaMEg++n6tqXBwLmAQJkftEmB+YUPFB9OGn5TQY9Pcnof95Y8xnvtR\n" +
                "0DvVQa9RM9IsqxgvU4wQCcaNHuEC46Wzo7lyYJ6p//GLw8UQnHxsWktt8U+vyaqXjOvz0+nJobUz\n" +
                "Jv7r7DFkOwgS6ObBczaZsv1yx2YklcKfbsI7xVsvZAXFey2RsvSJi1QPEJC5XbwDenWnCSrPfjJg\n" +
                "SLJ0p9tV70D6v07r1OOmBtvU5AH4N+vioAZA0BE=\n" +
                "-----END NEW CERTIFICATE REQUEST-----\n";
        final Certificate certificate = provider.issueCertificate(csr, Arrays.asList("v-1-VM", "domain1.com", "domain2.com"), null, 1);
        Assert.assertTrue(certificate != null);
        Assert.assertTrue(certificate.getPrivateKey() == null);
        Assert.assertEquals(certificate.getCaCertificates().get(0), caCertificate);
        Assert.assertTrue(certificate.getClientCertificate().getSubjectDN().toString().startsWith("CN=v-1-VM,"));
        certificate.getClientCertificate().verify(caCertificate.getPublicKey());
    }

    @Test
    public void testRevokeCertificate() throws Exception {
        Assert.assertTrue(provider.revokeCertificate(CertUtils.generateRandomBigInt(), "anyString"));
    }

    @Test
    public void testCreateSSLEngineWithoutAuthStrictness() throws Exception {
        provider.rootCAAuthStrictness = Mockito.mock(ConfigKey.class);
        Mockito.when(provider.rootCAAuthStrictness.value()).thenReturn(Boolean.FALSE);
        final SSLEngine e = provider.createSSLEngine(SSLUtils.getSSLContext(), "/1.2.3.4:5678", null);
        Assert.assertFalse(e.getNeedClientAuth());
    }

    @Test
    public void testCreateSSLEngineWithAuthStrictness() throws Exception {
        provider.rootCAAuthStrictness = Mockito.mock(ConfigKey.class);
        Mockito.when(provider.rootCAAuthStrictness.value()).thenReturn(Boolean.TRUE);
        final SSLEngine e = provider.createSSLEngine(SSLUtils.getSSLContext(), "/1.2.3.4:5678", null);
        Assert.assertTrue(e.getNeedClientAuth());
    }

    @Test
    public void testGetProviderName() throws Exception {
        Assert.assertEquals(provider.getProviderName(), "root");
    }

}