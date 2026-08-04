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

package org.apache.cloudstack.dns.bunny;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.apache.cloudstack.dns.exception.DnsAuthenticationException;
import org.apache.cloudstack.dns.exception.DnsConflictException;
import org.apache.cloudstack.dns.exception.DnsNotFoundException;
import org.apache.cloudstack.dns.exception.DnsOperationException;
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
public class BunnyDnsClientTest {

    BunnyDnsClient client;
    CloseableHttpClient httpClientMock;

    @Before
    public void setUp() {
        client = new BunnyDnsClient();
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
    public void testTypeCodeAndName() {
        assertEquals(0, BunnyDnsClient.typeCode("A"));
        assertEquals(4, BunnyDnsClient.typeCode("mx"));
        assertEquals("SRV", BunnyDnsClient.typeName(8));
        assertEquals(null, BunnyDnsClient.typeName(9)); // CAA - not modeled by CloudStack
    }

    @Test(expected = IllegalArgumentException.class)
    public void testTypeCodeUnsupportedThrows() {
        BunnyDnsClient.typeCode("CAA");
    }

    @Test
    public void testNormalizeDomain() {
        assertEquals("example.com", client.normalizeDomain("Example.Com."));
        assertEquals("example.com", client.normalizeDomain("  example.com  "));
    }

    @Test(expected = IllegalArgumentException.class)
    public void testNormalizeDomainBlankThrows() {
        client.normalizeDomain("  ");
    }

    @Test
    public void testToBunnyRecordNameApex() {
        assertEquals("", client.toBunnyRecordName("@", "example.com"));
        assertEquals("", client.toBunnyRecordName("", "example.com"));
        assertEquals("", client.toBunnyRecordName("example.com", "example.com"));
        assertEquals("", client.toBunnyRecordName("example.com.", "example.com"));
    }

    @Test
    public void testToBunnyRecordNameRelative() {
        assertEquals("www", client.toBunnyRecordName("www", "example.com"));
        assertEquals("www", client.toBunnyRecordName("WWW", "example.com"));
    }

    @Test
    public void testToBunnyRecordNameStripsAbsoluteSuffix() {
        assertEquals("www", client.toBunnyRecordName("www.example.com", "example.com"));
        assertEquals("www", client.toBunnyRecordName("www.example.com.", "example.com"));
    }

    @Test
    public void testToBunnyRecordNameAlreadyRelativeDotted() {
        // Not absolute (no trailing dot) and doesn't match the zone suffix -> passed through as-is.
        assertEquals("api.internal", client.toBunnyRecordName("api.internal", "example.com"));
    }

    @Test(expected = IllegalArgumentException.class)
    public void testToBunnyRecordNameNullThrows() {
        client.toBunnyRecordName(null, "example.com");
    }

    @Test
    public void testToFqdn() {
        assertEquals("example.com", client.toFqdn("", "example.com"));
        assertEquals("www.example.com", client.toFqdn("www", "example.com"));
    }

    @Test
    public void testParseContentDefault() {
        BunnyDnsClient.ContentFields fields = BunnyDnsClient.parseContent("A", "1.2.3.4");
        assertEquals("1.2.3.4", fields.value);
        assertEquals(null, fields.priority);
    }

    @Test
    public void testParseContentMx() {
        BunnyDnsClient.ContentFields fields = BunnyDnsClient.parseContent("MX", "10 mail.example.com.");
        assertEquals("mail.example.com", fields.value);
        assertEquals(Integer.valueOf(10), fields.priority);
        assertEquals(null, fields.weight);
        assertEquals(null, fields.port);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testParseContentMxInvalidThrows() {
        BunnyDnsClient.parseContent("MX", "mail.example.com.");
    }

    @Test
    public void testParseContentSrv() {
        BunnyDnsClient.ContentFields fields = BunnyDnsClient.parseContent("SRV", "10 20 5061 sip.example.com.");
        assertEquals("sip.example.com", fields.value);
        assertEquals(Integer.valueOf(10), fields.priority);
        assertEquals(Integer.valueOf(20), fields.weight);
        assertEquals(Integer.valueOf(5061), fields.port);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testParseContentSrvInvalidThrows() {
        BunnyDnsClient.parseContent("SRV", "10 20 sip.example.com.");
    }

    @Test(expected = IllegalArgumentException.class)
    public void testParseContentBlankThrows() {
        BunnyDnsClient.parseContent("A", "  ");
    }

    @Test
    public void testFormatContentMx() {
        com.fasterxml.jackson.databind.ObjectMapper mapper = new com.fasterxml.jackson.databind.ObjectMapper();
        com.fasterxml.jackson.databind.node.ObjectNode record = mapper.createObjectNode();
        record.put("Value", "mail.example.com");
        record.put("Priority", 10);
        assertEquals("10 mail.example.com.", BunnyDnsClient.formatContent("MX", record));
    }

    @Test
    public void testFormatContentSrv() {
        com.fasterxml.jackson.databind.ObjectMapper mapper = new com.fasterxml.jackson.databind.ObjectMapper();
        com.fasterxml.jackson.databind.node.ObjectNode record = mapper.createObjectNode();
        record.put("Value", "sip.example.com");
        record.put("Priority", 10);
        record.put("Weight", 20);
        record.put("Port", 5061);
        assertEquals("10 20 5061 sip.example.com.", BunnyDnsClient.formatContent("SRV", record));
    }

    @Test
    public void testFormatContentDefault() {
        com.fasterxml.jackson.databind.ObjectMapper mapper = new com.fasterxml.jackson.databind.ObjectMapper();
        com.fasterxml.jackson.databind.node.ObjectNode record = mapper.createObjectNode();
        record.put("Value", "1.2.3.4");
        assertEquals("1.2.3.4", BunnyDnsClient.formatContent("A", record));
    }

    @Test
    public void testResolveZoneIdFound() throws Exception {
        mockHttpResponse(200, "{\"Items\":[{\"Id\":42,\"Domain\":\"example.com\"}],\"HasMoreItems\":false}");
        long zoneId = client.resolveZoneId("https://api.bunny.net", null, "key", "example.com");
        assertEquals(42L, zoneId);
    }

    @Test(expected = DnsNotFoundException.class)
    public void testResolveZoneIdNotFound() throws Exception {
        mockHttpResponse(200, "{\"Items\":[{\"Id\":42,\"Domain\":\"other.com\"}],\"HasMoreItems\":false}");
        client.resolveZoneId("https://api.bunny.net", null, "key", "example.com");
    }

    @Test
    public void testResolveZoneIdPaginates() throws Exception {
        when(httpClientMock.execute(any(HttpUriRequest.class))).thenAnswer(new Answer<CloseableHttpResponse>() {
            @Override
            public CloseableHttpResponse answer(InvocationOnMock invocation) {
                HttpUriRequest request = invocation.getArgument(0);
                if (request.getURI().getQuery().contains("page=1")) {
                    return createResponse(200, "{\"Items\":[{\"Id\":1,\"Domain\":\"other.com\"}],\"HasMoreItems\":true}");
                }
                return createResponse(200, "{\"Items\":[{\"Id\":42,\"Domain\":\"example.com\"}],\"HasMoreItems\":false}");
            }
        });
        long zoneId = client.resolveZoneId("https://api.bunny.net", null, "key", "example.com");
        assertEquals(42L, zoneId);
    }

    @Test
    public void testZoneExistsTrue() throws Exception {
        mockHttpResponse(200, "{\"Items\":[{\"Id\":42,\"Domain\":\"example.com\"}],\"HasMoreItems\":false}");
        assertTrue(client.zoneExists("https://api.bunny.net", null, "key", "example.com"));
    }

    @Test
    public void testZoneExistsFalse() throws Exception {
        mockHttpResponse(200, "{\"Items\":[],\"HasMoreItems\":false}");
        assertFalse(client.zoneExists("https://api.bunny.net", null, "key", "example.com"));
    }

    @Test
    public void testCreateZoneReturnsIdFromResponse() throws Exception {
        mockHttpResponse(201, "{\"Id\":99}");
        long zoneId = client.createZone("https://api.bunny.net", null, "key", "example.com");
        assertEquals(99L, zoneId);
    }

    @Test
    public void testCreateZoneFallsBackToLookupWhenResponseHasNoId() throws Exception {
        when(httpClientMock.execute(any(HttpUriRequest.class))).thenAnswer(new Answer<CloseableHttpResponse>() {
            @Override
            public CloseableHttpResponse answer(InvocationOnMock invocation) {
                HttpUriRequest request = invocation.getArgument(0);
                if (request.getMethod().equals("POST")) {
                    return createResponse(201, null);
                }
                return createResponse(200, "{\"Items\":[{\"Id\":77,\"Domain\":\"example.com\"}],\"HasMoreItems\":false}");
            }
        });
        long zoneId = client.createZone("https://api.bunny.net", null, "key", "example.com");
        assertEquals(77L, zoneId);
    }

    @Test
    public void testDeleteZoneDeletesWhenFound() throws Exception {
        when(httpClientMock.execute(any(HttpUriRequest.class))).thenAnswer(new Answer<CloseableHttpResponse>() {
            @Override
            public CloseableHttpResponse answer(InvocationOnMock invocation) {
                HttpUriRequest request = invocation.getArgument(0);
                if (request.getMethod().equals("GET")) {
                    return createResponse(200, "{\"Items\":[{\"Id\":42,\"Domain\":\"example.com\"}],\"HasMoreItems\":false}");
                }
                return createResponse(204, null);
            }
        });
        client.deleteZone("https://api.bunny.net", null, "key", "example.com");
    }

    @Test
    public void testDeleteZoneNoOpWhenAlreadyGone() throws Exception {
        mockHttpResponse(200, "{\"Items\":[],\"HasMoreItems\":false}");
        client.deleteZone("https://api.bunny.net", null, "key", "example.com");
        // No exception means success; DELETE is never issued since resolveZoneId throws DnsNotFoundException.
    }

    @Test
    public void testAddRecord() throws Exception {
        mockHttpResponse(201, "{\"Id\":1}");
        String fqdn = client.addRecord("https://api.bunny.net", null, "key", 42L, "example.com", "www", "A", 300, "1.2.3.4");
        assertEquals("www.example.com", fqdn);
    }

    @Test
    public void testFindRecordsByNameAndType() throws Exception {
        mockHttpResponse(200, "{\"Items\":[{\"Id\":1,\"Name\":\"www\",\"Type\":0},{\"Id\":2,\"Name\":\"other\",\"Type\":0}],\"HasMoreItems\":false}");
        List<JsonNode> matches = client.findRecordsByNameAndType("https://api.bunny.net", null, "key", 42L, "example.com", "www", "A");
        assertEquals(1, matches.size());
        assertEquals(1, matches.get(0).path("Id").asInt());
    }

    @Test
    public void testDeleteRecordsByNameAndType() throws Exception {
        when(httpClientMock.execute(any(HttpUriRequest.class))).thenAnswer(new Answer<CloseableHttpResponse>() {
            @Override
            public CloseableHttpResponse answer(InvocationOnMock invocation) {
                HttpUriRequest request = invocation.getArgument(0);
                if (request.getMethod().equals("GET")) {
                    return createResponse(200, "{\"Items\":[{\"Id\":1,\"Name\":\"www\",\"Type\":0}],\"HasMoreItems\":false}");
                }
                return createResponse(204, null);
            }
        });
        String fqdn = client.deleteRecordsByNameAndType("https://api.bunny.net", null, "key", 42L, "example.com", "www", "A");
        assertEquals("www.example.com", fqdn);
    }

    @Test
    public void testReplaceRecordsKeepsMatchingDeletesStaleAddsMissing() throws Exception {
        List<String> requests = new ArrayList<>();
        when(httpClientMock.execute(any(HttpUriRequest.class))).thenAnswer(invocation -> {
            HttpUriRequest request = invocation.getArgument(0);
            if (request.getMethod().equals("GET")) {
                return createResponse(200, "{\"Items\":[{\"Id\":1,\"Name\":\"www\",\"Type\":0,\"Ttl\":300,\"Value\":\"1.2.3.4\"},"
                        + "{\"Id\":2,\"Name\":\"www\",\"Type\":0,\"Ttl\":300,\"Value\":\"9.9.9.9\"}],\"HasMoreItems\":false}");
            }
            requests.add(request.getMethod() + " " + request.getURI().getPath());
            return createResponse(request.getMethod().equals("PUT") ? 201 : 204, null);
        });
        String fqdn = client.replaceRecords("https://api.bunny.net", null, "key", 42L, "example.com", "www", "A", 300,
                Arrays.asList("1.2.3.4", "5.6.7.8"));
        assertEquals("www.example.com", fqdn);
        assertEquals(Arrays.asList("DELETE /dnszone/42/records/2", "PUT /dnszone/42/records"), requests);
    }

    @Test
    public void testHostnameContentRoundTripsTrailingDot() {
        assertEquals("target.example.com", BunnyDnsClient.parseContent("CNAME", "target.example.com.").value);
        com.fasterxml.jackson.databind.node.ObjectNode record = new com.fasterxml.jackson.databind.ObjectMapper().createObjectNode();
        record.put("Value", "target.example.com");
        assertEquals("target.example.com.", BunnyDnsClient.formatContent("CNAME", record));
    }

    @Test
    public void testListRecordsPaginates() throws Exception {
        when(httpClientMock.execute(any(HttpUriRequest.class))).thenAnswer(new Answer<CloseableHttpResponse>() {
            @Override
            public CloseableHttpResponse answer(InvocationOnMock invocation) {
                HttpUriRequest request = invocation.getArgument(0);
                if (request.getURI().getQuery().contains("page=1")) {
                    return createResponse(200, "{\"Items\":[{\"Id\":1,\"Name\":\"www\",\"Type\":0}],\"HasMoreItems\":true}");
                }
                return createResponse(200, "{\"Items\":[{\"Id\":2,\"Name\":\"mail\",\"Type\":4}],\"HasMoreItems\":false}");
            }
        });
        List<JsonNode> records = client.listRecords("https://api.bunny.net", null, "key", 42L);
        assertEquals(2, records.size());
    }

    @Test(expected = DnsNotFoundException.class)
    public void testExecuteThrowsNotFound() throws Exception {
        mockHttpResponse(404, "Not Found");
        client.resolveZoneId("https://api.bunny.net", null, "key", "example.com");
    }

    @Test(expected = DnsAuthenticationException.class)
    public void testExecuteThrowsAuthError() throws Exception {
        mockHttpResponse(401, "Unauthorized");
        client.validateAccess("https://api.bunny.net", null, "key");
    }

    @Test(expected = DnsConflictException.class)
    public void testExecuteThrowsConflictError() throws Exception {
        mockHttpResponse(409, "Conflict");
        client.validateAccess("https://api.bunny.net", null, "key");
    }

    @Test(expected = DnsOperationException.class)
    public void testExecuteThrowsUnexpectedStatus() throws Exception {
        mockHttpResponse(500, "Server Error");
        client.validateAccess("https://api.bunny.net", null, "key");
    }

    @Test
    public void testCloseSucceeds() throws Exception {
        CloseableHttpClient mockClient = mock(CloseableHttpClient.class);
        ReflectionTestUtils.setField(client, "httpClient", mockClient);
        client.close();
        org.mockito.Mockito.verify(mockClient).close();
    }

    @Test
    public void testCloseSwallowsIOException() throws Exception {
        CloseableHttpClient mockClient = mock(CloseableHttpClient.class);
        org.mockito.Mockito.doThrow(new IOException("connection reset")).when(mockClient).close();
        ReflectionTestUtils.setField(client, "httpClient", mockClient);
        client.close(); // must NOT throw
    }
}
