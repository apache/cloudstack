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

package org.apache.cloudstack.dns.powerdns;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;

import org.apache.cloudstack.dns.exception.DnsOperationException;
import org.apache.cloudstack.dns.exception.DnsAuthenticationException;
import org.apache.cloudstack.dns.exception.DnsConflictException;
import org.apache.cloudstack.dns.exception.DnsNotFoundException;
import org.apache.http.StatusLine;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.junit.MockitoJUnitRunner;
import org.mockito.stubbing.Answer;
import org.springframework.test.util.ReflectionTestUtils;

import com.fasterxml.jackson.databind.JsonNode;

@RunWith(MockitoJUnitRunner.class)
public class PowerDnsClientTest {

    PowerDnsClient client;
    CloseableHttpClient httpClientMock;

    @Before
    public void setUp() {
        client = new PowerDnsClient();
        httpClientMock = mock(CloseableHttpClient.class);
        ReflectionTestUtils.setField(client, "httpClient", httpClientMock);
    }

    private CloseableHttpResponse createResponse(int statusCode, String jsonBody) {
        CloseableHttpResponse responseMock = mock(CloseableHttpResponse.class);
        StatusLine statusLineMock = mock(StatusLine.class);
        when(responseMock.getStatusLine()).thenReturn(statusLineMock);
        when(statusLineMock.getStatusCode()).thenReturn(statusCode);

        if (jsonBody != null) {
            when(responseMock.getEntity()).thenReturn(new StringEntity(jsonBody, StandardCharsets.UTF_8));
        }

        return responseMock;
    }

    private void mockHttpResponse(int statusCode, String jsonBody) throws IOException {
        CloseableHttpResponse response = createResponse(statusCode, jsonBody);
        when(httpClientMock.execute(any(HttpUriRequest.class))).thenReturn(response);
    }

    @Test
    public void testNormalizeApexRecord() {
        String result = client.normalizeRecordName("@", "example.com");
        assertEquals("example.com.", result);

        result = client.normalizeRecordName("", "example.com");
        assertEquals("example.com.", result);
    }

    @Test
    public void testNormalizeRelativeRecord() {
        String result = client.normalizeRecordName("www", "example.com");
        assertEquals("www.example.com.", result);

        result = client.normalizeRecordName("WWW", "example.com"); // test case-insensitive
        assertEquals("www.example.com.", result);
    }

    @Test
    public void testNormalizeAbsoluteRecordWithinZone() {
        String result = client.normalizeRecordName("www.example.com.", "example.com");
        assertEquals("www.example.com.", result);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testNormalizeAbsoluteRecordOutsideZoneThrows() {
        client.normalizeRecordName("other.com.", "example.com");
    }

    @Test
    public void testNormalizeDottedNameWithoutTrailingDot() {
        String result = client.normalizeRecordName("api.test.com", "example.com");
        assertEquals("api.test.com.", result);
    }

    @Test
    public void testNormalizeRelativeSubdomain() {
        String result = client.normalizeRecordName("mail", "example.com");
        assertEquals("mail.example.com.", result);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testNormalizeNullRecordNameThrows() {
        client.normalizeRecordName(null, "example.com");
    }

    @Test
    public void testNormalizeZoneNormalization() {
        String result = client.normalizeRecordName("www", "Example.Com");
        assertEquals("www.example.com.", result);

        result = client.normalizeRecordName("www", "example.com.");
        assertEquals("www.example.com.", result);
    }

    @Test
    public void testDiscoverAuthoritativeServerIdSuccess() throws Exception {
        mockHttpResponse(200, "[{\"id\":\"localhost\", \"daemon_type\":\"authoritative\"}]");
        String result = client.discoverAuthoritativeServerId("http://pdns:8081", null, "apikey");
        assertEquals("localhost", result);
    }

    @Test
    public void testDiscoverAuthoritativeServerIdFallback() throws Exception {
        mockHttpResponse(200, "[{\"id\":\"server1\", \"daemon_type\":\"recursor\"}, {\"id\":\"server2\", \"daemon_type\":\"authoritative\"}]");
        String result = client.discoverAuthoritativeServerId("http://pdns", 8081, "apikey");
        assertEquals("server2", result);
    }

    @Test(expected = DnsOperationException.class)
    public void testDiscoverAuthoritativeServerIdEmpty() throws Exception {
        mockHttpResponse(200, "[]");
        client.discoverAuthoritativeServerId("http://pdns", 8081, "apikey");
    }

    @Test(expected = DnsOperationException.class)
    public void testDiscoverAuthoritativeServerIdNoAuthoritative() throws Exception {
        mockHttpResponse(200, "[{\"id\":\"server1\", \"daemon_type\":\"recursor\"}]");
        client.discoverAuthoritativeServerId("http://pdns", 8081, "apikey");
    }

    @Test
    public void testValidateServerIdSuccess() throws Exception {
        mockHttpResponse(200, "{\"id\":\"abc\", \"daemon_type\":\"authoritative\"}");
        String result = client.validateServerId("http://pdns", 8081, "apikey", "abc");
        assertEquals("abc", result);
    }

    @Test(expected = DnsOperationException.class)
    public void testValidateServerIdNotAuthoritative() throws Exception {
        mockHttpResponse(200, "{\"id\":\"abc\", \"daemon_type\":\"recursor\"}");
        client.validateServerId("http://pdns", 8081, "apikey", "abc");
    }

    @Test
    public void testResolveServerIdWithExternalId() throws Exception {
        mockHttpResponse(200, "{\"id\":\"abc\", \"daemon_type\":\"authoritative\"}");
        String result = client.resolveServerId("http://pdns", 8081, "apikey", "abc");
        assertEquals("abc", result);
    }

    @Test
    public void testResolveServerIdWithoutExternalId() throws Exception {
        mockHttpResponse(200, "[{\"id\":\"localhost\", \"daemon_type\":\"authoritative\"}]");
        String result = client.resolveServerId("http://pdns", 8081, "apikey", null);
        assertEquals("localhost", result);
    }

    @Test
    public void testCreateZone() throws Exception {
        when(httpClientMock.execute(any(HttpUriRequest.class))).thenAnswer(new Answer<CloseableHttpResponse>() {
            @Override
            public CloseableHttpResponse answer(InvocationOnMock invocation) {
                HttpUriRequest request = invocation.getArgument(0);
                if (request.getMethod().equals("GET")) {
                    return createResponse(200, "{\"id\":\"abc\", \"daemon_type\":\"authoritative\"}");
                }
                if (request.getMethod().equals("POST")) {
                    return createResponse(201, "{\"id\":\"example.com.\"}");
                }
                return createResponse(500, null);
            }
        });

        String result = client.createZone("http://pdns", 8081, "apikey", "abc", "example.com", "Native", false, Arrays.asList("ns1.com"));
        assertEquals("example.com.", result);
    }

    @Test
    public void testUpdateZone() throws Exception {
        when(httpClientMock.execute(any(HttpUriRequest.class))).thenAnswer(new Answer<CloseableHttpResponse>() {
            @Override
            public CloseableHttpResponse answer(InvocationOnMock invocation) {
                HttpUriRequest request = invocation.getArgument(0);
                if (request.getMethod().equals("GET")) {
                    return createResponse(200, "{\"id\":\"abc\", \"daemon_type\":\"authoritative\"}");
                }
                if (request.getMethod().equals("PUT")) {
                    return createResponse(204, null);
                }
                return createResponse(500, null);
            }
        });

        client.updateZone("http://pdns", 8081, "apikey", "abc", "example.com", "Native", true, Arrays.asList("ns1.com"));
        // No exception means success
    }

    @Test
    public void testDeleteZone() throws Exception {
        when(httpClientMock.execute(any(HttpUriRequest.class))).thenAnswer(new Answer<CloseableHttpResponse>() {
            @Override
            public CloseableHttpResponse answer(InvocationOnMock invocation) {
                HttpUriRequest request = invocation.getArgument(0);
                if (request.getMethod().equals("GET")) {
                    return createResponse(200, "{\"id\":\"abc\", \"daemon_type\":\"authoritative\"}");
                }
                if (request.getMethod().equals("DELETE")) {
                    return createResponse(204, null);
                }
                return createResponse(500, null);
            }
        });

        client.deleteZone("http://pdns", 8081, "apikey", "abc", "example.com");
    }

    @Test
    public void testModifyRecord() throws Exception {
        when(httpClientMock.execute(any(HttpUriRequest.class))).thenAnswer(new Answer<CloseableHttpResponse>() {
            @Override
            public CloseableHttpResponse answer(InvocationOnMock invocation) {
                HttpUriRequest request = invocation.getArgument(0);
                if (request.getMethod().equals("GET")) {
                    return createResponse(200, "{\"id\":\"abc\", \"daemon_type\":\"authoritative\"}");
                }
                if (request.getMethod().equals("PATCH")) {
                    return createResponse(204, null);
                }
                return createResponse(500, null);
            }
        });

        String result = client.modifyRecord("http://pdns", 8081, "apikey", "abc", "example.com", "www", "A", 300, Arrays.asList("1.2.3.4"), "REPLACE");
        assertEquals("www.example.com", result);
    }

    @Test
    public void testModifyRecordApex() throws Exception {
        when(httpClientMock.execute(any(HttpUriRequest.class))).thenAnswer(new Answer<CloseableHttpResponse>() {
            @Override
            public CloseableHttpResponse answer(InvocationOnMock invocation) {
                HttpUriRequest request = invocation.getArgument(0);
                if (request.getMethod().equals("GET")) {
                    return createResponse(200, "{\"id\":\"abc\", \"daemon_type\":\"authoritative\"}");
                }
                if (request.getMethod().equals("PATCH")) {
                    return createResponse(204, null);
                }
                return createResponse(500, null);
            }
        });

        String result = client.modifyRecord("http://pdns", 8081, "apikey", "abc", "example.com", "@", "A", 300, Arrays.asList("1.2.3.4"), "REPLACE");
        assertEquals("example.com", result);
    }

    @Test
    public void testListRecords() throws Exception {
        when(httpClientMock.execute(any(HttpUriRequest.class))).thenAnswer(new Answer<CloseableHttpResponse>() {
            @Override
            public CloseableHttpResponse answer(InvocationOnMock invocation) {
                HttpUriRequest request = invocation.getArgument(0);
                // validateServerId uses /servers/abc
                // listRecords uses /servers/abc/zones/example.com.
                if (request.getURI().getPath().endsWith("abc")) {
                    return createResponse(200, "{\"id\":\"abc\", \"daemon_type\":\"authoritative\"}");
                }
                if (request.getURI().getPath().endsWith("example.com.")) {
                    return createResponse(200, "{\"rrsets\":[{\"name\":\"www.example.com.\",\"type\":\"A\"}]}");
                }
                return createResponse(500, null);
            }
        });

        Iterable<JsonNode> records = client.listRecords("http://pdns", 8081, "apikey", "abc", "example.com");
        assertNotNull(records);
        assertTrue(records.iterator().hasNext());
        assertEquals("www.example.com.", records.iterator().next().path("name").asText());
    }

    @Test
    public void testListRecordsEmpty() throws Exception {
        when(httpClientMock.execute(any(HttpUriRequest.class))).thenAnswer(new Answer<CloseableHttpResponse>() {
            @Override
            public CloseableHttpResponse answer(InvocationOnMock invocation) {
                HttpUriRequest request = invocation.getArgument(0);
                if (request.getURI().getPath().endsWith("abc")) {
                    return createResponse(200, "{\"id\":\"abc\", \"daemon_type\":\"authoritative\"}");
                }
                if (request.getURI().getPath().endsWith("example.com.")) {
                    return createResponse(200, "{}");
                }
                return createResponse(500, null);
            }
        });

        Iterable<JsonNode> records = client.listRecords("http://pdns", 8081, "apikey", "abc", "example.com");
        assertNotNull(records);
        assertTrue(!records.iterator().hasNext());
    }

    @Test(expected = DnsNotFoundException.class)
    public void testExecuteThrowsNotFound() throws Exception {
        mockHttpResponse(404, "Not Found");
        client.validateServerId("http://pdns", 8081, "apikey", "abc");
    }

    @Test(expected = DnsAuthenticationException.class)
    public void testExecuteThrowsAuthError() throws Exception {
        mockHttpResponse(401, "Unauthorized");
        client.validateServerId("http://pdns", 8081, "apikey", "abc");
    }

    @Test(expected = DnsConflictException.class)
    public void testExecuteThrowsConflictError() throws Exception {
        mockHttpResponse(409, "Conflict");
        client.validateServerId("http://pdns", 8081, "apikey", "abc");
    }

    @Test(expected = DnsOperationException.class)
    public void testExecuteThrowsUnexpectedStatus() throws Exception {
        mockHttpResponse(500, "Server Error");
        client.validateServerId("http://pdns", 8081, "apikey", "abc");
    }
    // Route helper: GET /servers/abc → validate; GET /zones/... → zone response
    private void mockDnsRecordExists(String zoneJson) throws IOException {
        when(httpClientMock.execute(any(HttpUriRequest.class))).thenAnswer(new Answer<CloseableHttpResponse>() {
            @Override
            public CloseableHttpResponse answer(InvocationOnMock invocation) {
                HttpUriRequest request = invocation.getArgument(0);
                String path = request.getURI().getPath();
                if (path.endsWith("/abc")) {
                    return createResponse(200, "{\"id\":\"abc\", \"daemon_type\":\"authoritative\"}");
                }
                // zone query (contains /zones/)
                if (zoneJson == null) {
                    return createResponse(200, null);     // empty body → execute() returns null
                }
                return createResponse(200, zoneJson);
            }
        });
    }

    @Test
    public void testDnsRecordExistsZoneNodeNull() throws Exception {
        // execute() returns null → zoneNode == null → false
        mockDnsRecordExists(null);
        boolean result = client.dnsRecordExists("http://pdns", 8081, "apikey", "abc", "example.com", "www", "A");
        assertEquals(false, result);
    }

    @Test
    public void testDnsRecordExistsMissingRrSetsField() throws Exception {
        // response has no "rrsets" key → !zoneNode.has(RR_SETS) → false
        mockDnsRecordExists("{}");
        boolean result = client.dnsRecordExists("http://pdns", 8081, "apikey", "abc", "example.com", "www", "A");
        assertEquals(false, result);
    }

    @Test
    public void testDnsRecordExistsRrSetsNotArray() throws Exception {
        // rrsets is a scalar string, not an ArrayNode → isArray() == false → false
        mockDnsRecordExists("{\"rrsets\":\"not-an-array\"}");
        boolean result = client.dnsRecordExists("http://pdns", 8081, "apikey", "abc", "example.com", "www", "A");
        assertEquals(false, result);
    }

    @Test
    public void testDnsRecordExistsEmptyRrSetsArray() throws Exception {
        // rrsets is an empty array → isArray() == true && isEmpty() == true → false
        mockDnsRecordExists("{\"rrsets\":[]}");
        boolean result = client.dnsRecordExists("http://pdns", 8081, "apikey", "abc", "example.com", "www", "A");
        assertEquals(false, result);
    }

    @Test
    public void testDnsRecordExistsNonEmptyRrSetsArray() throws Exception {
        // rrsets is a non-empty array → isArray() == true && !isEmpty() → true
        mockDnsRecordExists("{\"rrsets\":[{\"name\":\"www.example.com.\",\"type\":\"A\"}]}");
        boolean result = client.dnsRecordExists("http://pdns", 8081, "apikey", "abc", "example.com", "www", "A");
        assertEquals(true, result);
    }

    @Test
    public void testCloseSucceeds() throws Exception {
        // httpClient.close() completes normally → no exception propagated
        CloseableHttpClient mockClient = mock(CloseableHttpClient.class);
        ReflectionTestUtils.setField(client, "httpClient", mockClient);
        client.close();
        org.mockito.Mockito.verify(mockClient).close();
    }

    @Test
    public void testCloseSwallowsIOException() throws Exception {
        // httpClient.close() throws IOException → caught and logged (warn), no rethrow
        CloseableHttpClient mockClient = mock(CloseableHttpClient.class);
        org.mockito.Mockito.doThrow(new IOException("connection reset")).when(mockClient).close();
        ReflectionTestUtils.setField(client, "httpClient", mockClient);
        client.close(); // must NOT throw
    }
}
