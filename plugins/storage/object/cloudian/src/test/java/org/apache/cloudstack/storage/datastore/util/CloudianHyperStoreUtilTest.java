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
// SPDX-License-Identifier: Apache-2.0
package org.apache.cloudstack.storage.datastore.util;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.post;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyString;

import org.apache.cloudstack.api.ApiErrorCode;
import org.apache.cloudstack.api.ServerApiException;
import org.apache.cloudstack.cloudian.client.CloudianClient;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;

import com.cloud.utils.exception.CloudRuntimeException;
import com.github.tomakehurst.wiremock.junit.WireMockRule;

public class CloudianHyperStoreUtilTest {
    private final int port = 18081;

    @Rule
    public WireMockRule wireMockRule = new WireMockRule(port);

    private AutoCloseable closeable;

    @Before
    public void setUp() {
        closeable = MockitoAnnotations.openMocks(this);
    }

    @After
    public void tearDown() throws Exception {
        closeable.close();
    }

    @Test
    public void testGetCloudianClientBadUrl() {
        String url = "bad://bad-url";
        CloudRuntimeException thrown = assertThrows(CloudRuntimeException.class, () -> CloudianHyperStoreUtil.getCloudianClient(url, "", "", false));
        assertNotNull(thrown);
        assertTrue(thrown.getMessage().contains("unknown protocol"));
    }

    @Test
    public void testGetCloudianClient() {
        String url = "https://localhost:98765";
        CloudianClient cc = CloudianHyperStoreUtil.getCloudianClient(url, "", "", false);
        assertNotNull(cc);
    }

    @Test
    public void testGetCloudianClientNoPort() {
        String url = "https://localhost";
        CloudianClient cc = CloudianHyperStoreUtil.getCloudianClient(url, "", "", false);
        assertNotNull(cc);
    }

    @Test
    public void testGetCloudianClientToGetServerVersion() {
        final String expect = "8.1 Compiled: 2023-11-11 16:30";
        wireMockRule.stubFor(get(urlEqualTo("/system/version"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withBody(expect)));

        // Get a connection and try using it
        String url = String.format("http://localhost:%d", port);
        CloudianClient cc = CloudianHyperStoreUtil.getCloudianClient(url, "u", "p", false);
        String version = cc.getServerVersion();
        assertEquals(expect, version);
    }

    @Test
    public void testGetCloudianClientShortenedTimeout() {
        // Response delayed 3 seconds. We should never get it.
        final String expect = "8.1 Compiled: 2023-11-11 16:30";
        wireMockRule.stubFor(get(urlEqualTo("/system/version"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withFixedDelay(3000)  // 3 second delay.
                        .withBody(expect)));

        try (MockedStatic<CloudianHyperStoreUtil> mockStatic = Mockito.mockStatic(CloudianHyperStoreUtil.class)) {
            // Force a shorter 1 second timeout for testing so as not to hold up unit tests.
            mockStatic.when(() -> CloudianHyperStoreUtil.getAdminTimeoutSeconds()).thenReturn(1);
            mockStatic.when(() -> CloudianHyperStoreUtil.getCloudianClient(anyString(), anyString(), anyString(), anyBoolean())).thenCallRealMethod();

            // Get a connection and try using it but it should timeout
            String url = String.format("http://localhost:%d", port);
            CloudianClient cc = CloudianHyperStoreUtil.getCloudianClient(url, "u", "p", false);
            long before = System.currentTimeMillis();
            ServerApiException thrown = assertThrows(ServerApiException.class, () -> cc.getServerVersion());
            long after = System.currentTimeMillis();
            assertNotNull(thrown);
            assertEquals(ApiErrorCode.RESOURCE_UNAVAILABLE_ERROR, thrown.getErrorCode());
            assertTrue((after - before) >= 1000); // should timeout after 1 second.
        }
    }

    @Test
    public void testValidateS3UrlGood() {
        // Mock an AWS S3 invalid access key response.
        StringBuilder ERR_XML = new StringBuilder("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n");
        ERR_XML.append("<Error>\n");
        ERR_XML.append("  <Code>InvalidAccessKeyId</Code>\n");
        ERR_XML.append("  <Message>The AWS Access Key Id you provided does not exist in our records.</Message>\n");
        ERR_XML.append("  <AWSAccessKeyId>unknown</AWSAccessKeyId>\n");
        ERR_XML.append("  <HostId>12345=</HostId>\n");
        ERR_XML.append("</Error>\n");

        wireMockRule.stubFor(get(urlEqualTo("/"))
                .willReturn(aResponse()
                        .withStatus(403)
                        .withHeader("content-type", "application/xml")
                        .withBody(ERR_XML.toString())));

        // Test: validates the AmazonS3 client returned by CloudianHyperStoreUtil.getS3Client()
        // which is called indirectly via the validateS3Url() method can connect to the
        // remote port and handles the access key error as the expected s3 response.
        String url = String.format("http://localhost:%d", port);
        CloudianHyperStoreUtil.validateS3Url(url);
    }

    @Test
    public void testValidateS3UrlBadRequest() {
        wireMockRule.stubFor(get(urlEqualTo("/"))
                .willReturn(aResponse()
                        .withStatus(400)
                        .withHeader("content-type", "text/html")
                        .withBody("<html><body>400 Bad Request</body></html>")));

        String url = String.format("http://localhost:%d", port);
        CloudRuntimeException thrown = assertThrows(CloudRuntimeException.class, () -> CloudianHyperStoreUtil.validateS3Url(url));
        assertNotNull(thrown);
    }

    @Test
    public void testValidateIAMUrlGoodInvalidClientTokenId() {
        // Mock an AWS IAM invalid access key response.
        StringBuilder ERR_XML = new StringBuilder();
        ERR_XML.append("<ErrorResponse xmlns=\"https://iam.amazonaws.com/doc/2010-05-08/\">\n");
        ERR_XML.append("  <Error>\n");
        ERR_XML.append("    <Type>Sender</Type>\n");
        ERR_XML.append("    <Code>InvalidClientTokenId</Code>\n");
        ERR_XML.append("    <Message>The security token included in the request is invalid.</Message>\n");
        ERR_XML.append("  </Error>\n");
        ERR_XML.append("  <RequestId>a2c47f7e-0196-4b45-af18-a9e99e4d9ed5</RequestId>\n");
        ERR_XML.append("</ErrorResponse>\n");

        wireMockRule.stubFor(post(urlEqualTo("/"))
                .willReturn(aResponse()
                        .withStatus(403)
                        .withHeader("content-type", "text/xml")
                        .withBody(ERR_XML.toString())));

        // Test: validates the AmazonIdentityManagement client returned by CloudianHyperStoreUtil.getIAMClient()
        // which is called indirectly via the validateIAMUrl() method can connect to the
        // remote port and handles the access key error as the expected s3 response.
        String url = String.format("http://localhost:%d", port);
        CloudianHyperStoreUtil.validateIAMUrl(url);
    }

    @Test
    public void testValidateIAMUrlGoodInvalidAccessKeyId() {
        // Mock HyperStore IAM invalid access key current response.
        StringBuilder ERR_XML = new StringBuilder("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n");
        ERR_XML.append("<ErrorResponse xmlns=\"https://iam.amazonaws.com/doc/2010-05-08/\">\n");
        ERR_XML.append("  <Error>\n");
        ERR_XML.append("    <Code>InvalidAccessKeyId</Code>\n");
        ERR_XML.append("    <Message>The Access Key Id you provided does not exist in our records.</Message>\n");
        ERR_XML.append("  </Error>\n");
        ERR_XML.append("  <RequestId>a2c47f7e-0196-4b45-af18-a9e99e4d9ed5</RequestId>\n");
        ERR_XML.append("</ErrorResponse>\n");

        wireMockRule.stubFor(post(urlEqualTo("/"))
                .willReturn(aResponse()
                        .withStatus(403)
                        .withHeader("content-type", "application/xml;charset=UTF-8")
                        .withBody(ERR_XML.toString())));

        // Test: validates the AmazonIdentityManagement client returned by CloudianHyperStoreUtil.getIAMClient()
        // which is called indirectly via the validateIAMUrl() method can connect to the
        // remote port and handles the access key error as the expected s3 response.
        String url = String.format("http://localhost:%d", port);
        CloudianHyperStoreUtil.validateIAMUrl(url);
    }

    @Test
    public void testValidateIAMUrlBadRequest() {
        wireMockRule.stubFor(post(urlEqualTo("/"))
                .willReturn(aResponse()
                        .withStatus(400)
                        .withHeader("content-type", "text/html")
                        .withBody("<html><body>400 Bad Request</body></html>")));

        String url = String.format("http://localhost:%d", port);
        CloudRuntimeException thrown = assertThrows(CloudRuntimeException.class, () -> CloudianHyperStoreUtil.validateIAMUrl(url));
        assertNotNull(thrown);
    }
}
