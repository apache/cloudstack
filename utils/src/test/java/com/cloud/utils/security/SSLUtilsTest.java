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

import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.util.ArrayList;
import java.util.Arrays;

import org.apache.cloudstack.utils.security.SSLUtils;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Spy;
import org.mockito.junit.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class SSLUtilsTest {

    @Spy
    private SSLUtils spySSLUtils;

    @Test
    public void getRecommendedProtocolsTest() {
        ArrayList<String> protocolsList = new ArrayList<>(Arrays.asList(spySSLUtils.getRecommendedProtocols()));
        verifyProtocols(protocolsList);
    }

    @Test
    public void getRecommendedCiphers() {
        String[] expectedCiphers = { "TLS_ECDHE_RSA_WITH_AES_128_CBC_SHA", "TLS_DHE_RSA_WITH_AES_128_GCM_SHA256", "TLS_DHE_RSA_WITH_AES_128_CBC_SHA256",
                "TLS_RSA_WITH_AES_128_GCM_SHA256", "TLS_RSA_WITH_AES_128_CBC_SHA256", "TLS_ECDHE_RSA_WITH_AES_128_CBC_SHA256", "TLS_ECDHE_RSA_WITH_AES_128_GCM_SHA256",
                "TLS_DHE_RSA_WITH_AES_256_CBC_SHA", "TLS_ECDHE_RSA_WITH_AES_256_CBC_SHA", "TLS_DHE_RSA_WITH_AES_256_GCM_SHA384", "TLS_DHE_RSA_WITH_AES_256_CBC_SHA256",
                "TLS_RSA_WITH_AES_256_GCM_SHA384", "TLS_RSA_WITH_AES_256_CBC_SHA256", "TLS_ECDHE_RSA_WITH_AES_256_GCM_SHA384", "TLS_ECDHE_RSA_WITH_AES_256_CBC_SHA384" };
        Assert.assertArrayEquals(expectedCiphers, spySSLUtils.getRecommendedCiphers());
    }

    @Test
    public void getSSLContextTest() throws NoSuchAlgorithmException {
        Assert.assertEquals("TLSv1.2", spySSLUtils.getSSLContext().getProtocol());
    }

    @Test
    public void getSSLContextTestStringAsParameter() throws NoSuchAlgorithmException, NoSuchProviderException {
        Assert.assertEquals("TLSv1.2", spySSLUtils.getSSLContext("SunJSSE").getProtocol());
    }

    @Test
    public void getSupportedProtocolsTest() {
        ArrayList<String> protocolsList = new ArrayList<>(Arrays.asList(spySSLUtils.getSupportedProtocols(new String[] { "TLSv1", "TLSv1.1", "TLSv1.2", "SSLv3", "SSLv2Hello" })));
        verifyProtocols(protocolsList);
    }

    private void verifyProtocols(ArrayList<String> protocolsList) {
        Assert.assertTrue(protocolsList.contains("TLSv1"));
        Assert.assertTrue(protocolsList.contains("TLSv1.1"));
        Assert.assertTrue(protocolsList.contains("TLSv1.2"));
        Assert.assertFalse(protocolsList.contains("SSLv3"));
        Assert.assertFalse(protocolsList.contains("SSLv2Hello"));
    }

}
