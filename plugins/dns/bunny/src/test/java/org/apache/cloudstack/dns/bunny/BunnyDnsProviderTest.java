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
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;

import org.apache.cloudstack.dns.DnsProviderType;
import org.apache.cloudstack.dns.DnsRecord;
import org.apache.cloudstack.dns.DnsRecord.RecordType;
import org.apache.cloudstack.dns.DnsServer;
import org.apache.cloudstack.dns.DnsZone;
import org.apache.cloudstack.dns.exception.DnsProviderException;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.junit.MockitoJUnitRunner;
import org.springframework.test.util.ReflectionTestUtils;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

@RunWith(MockitoJUnitRunner.class)
public class BunnyDnsProviderTest {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    private BunnyDnsProvider provider;
    private BunnyDnsClient clientMock;
    private DnsServer serverMock;
    private DnsZone zoneMock;

    @Before
    public void setUp() {
        provider = new BunnyDnsProvider();
        clientMock = mock(BunnyDnsClient.class);
        serverMock = mock(DnsServer.class);
        zoneMock = mock(DnsZone.class);
        ReflectionTestUtils.setField(provider, "client", clientMock);

        when(serverMock.getUrl()).thenReturn("https://api.bunny.net");
        when(serverMock.getDnsApiKey()).thenReturn("secret");
        when(serverMock.getPort()).thenReturn(null);

        when(zoneMock.getName()).thenReturn("example.com");
    }

    @Test
    public void testGetProviderType() {
        assertEquals(DnsProviderType.BunnyDNS, provider.getProviderType());
    }

    @Test
    public void testConfigureCreatesClientWhenNull() {
        BunnyDnsProvider freshProvider = new BunnyDnsProvider();
        boolean result = freshProvider.configure("test", new HashMap<>());
        assertTrue(result);
        assertNotNull(ReflectionTestUtils.getField(freshProvider, "client"));
    }

    @Test
    public void testConfigureDoesNotReplaceExistingClient() {
        BunnyDnsClient existingClient = mock(BunnyDnsClient.class);
        ReflectionTestUtils.setField(provider, "client", existingClient);

        boolean result = provider.configure("test", new HashMap<>());

        assertTrue(result);
        assertEquals(existingClient, ReflectionTestUtils.getField(provider, "client"));
    }

    @Test
    public void testStopClosesClient() {
        boolean result = provider.stop();
        assertTrue(result);
        verify(clientMock, times(1)).close();
    }

    @Test
    public void testStopWithNullClientSucceeds() {
        ReflectionTestUtils.setField(provider, "client", null);
        boolean result = provider.stop();
        assertTrue(result);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testValidateServerFieldsNullUrl() {
        when(serverMock.getUrl()).thenReturn(null);
        provider.validateRequiredServerFields(serverMock);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testValidateServerFieldsNullApiKey() {
        when(serverMock.getDnsApiKey()).thenReturn(null);
        provider.validateRequiredServerFields(serverMock);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testValidateServerAndZoneFieldsBlankZoneName() {
        when(zoneMock.getName()).thenReturn("   ");
        provider.validateRequiredServerAndZoneFields(serverMock, zoneMock);
    }

    @Test
    public void testValidateDelegatesToClient() throws Exception {
        provider.validate(serverMock);
        verify(clientMock).validateAccess("https://api.bunny.net", null, "secret");
    }

    @Test(expected = IllegalArgumentException.class)
    public void testValidateThrowsWhenServerUrlBlank() throws Exception {
        when(serverMock.getUrl()).thenReturn("");
        provider.validate(serverMock);
    }

    @Test
    public void testValidateAndResolveServerReturnsNull() throws Exception {
        String result = provider.validateAndResolveServer(serverMock);
        assertEquals(null, result);
        verify(clientMock).validateAccess("https://api.bunny.net", null, "secret");
    }

    @Test
    public void testProvisionZoneDelegatesToClient() throws DnsProviderException {
        when(clientMock.createZone("https://api.bunny.net", null, "secret", "example.com")).thenReturn(42L);
        String zoneId = provider.provisionZone(serverMock, zoneMock);
        assertEquals("42", zoneId);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testProvisionZoneThrowsWhenZoneNameBlank() throws DnsProviderException {
        when(zoneMock.getName()).thenReturn(null);
        provider.provisionZone(serverMock, zoneMock);
    }

    @Test
    public void testDeleteZoneDelegatesToClient() throws DnsProviderException {
        provider.deleteZone(serverMock, zoneMock);
        verify(clientMock).deleteZone("https://api.bunny.net", null, "secret", "example.com");
    }

    @Test
    public void testUpdateZoneResolvesZoneAsNoOp() throws DnsProviderException {
        when(clientMock.resolveZoneId("https://api.bunny.net", null, "secret", "example.com")).thenReturn(42L);
        provider.updateZone(serverMock, zoneMock);
        verify(clientMock).resolveZoneId("https://api.bunny.net", null, "secret", "example.com");
    }

    @Test
    public void testAddAndUpdateRecordReplaceRecords() throws DnsProviderException {
        List<String> contents = Arrays.asList("1.2.3.4", "5.6.7.8");
        DnsRecord record = new DnsRecord("www", RecordType.A, contents, 300);
        when(clientMock.resolveZoneId(anyString(), eq(null), anyString(), anyString())).thenReturn(42L);
        when(clientMock.replaceRecords("https://api.bunny.net", null, "secret", 42L, "example.com", "www", "A", 300, contents))
                .thenReturn("www.example.com");

        assertEquals("www.example.com", provider.addRecord(serverMock, zoneMock, record));
        assertEquals("www.example.com", provider.updateRecord(serverMock, zoneMock, record));
        verify(clientMock, times(2)).replaceRecords("https://api.bunny.net", null, "secret", 42L, "example.com", "www", "A", 300, contents);
    }

    @Test
    public void testDeleteRecordDelegatesToClient() throws DnsProviderException {
        DnsRecord record = new DnsRecord("old", RecordType.CNAME, Arrays.asList("target.com"), 600);
        when(clientMock.resolveZoneId(anyString(), eq(null), anyString(), anyString())).thenReturn(42L);
        when(clientMock.deleteRecordsByNameAndType(anyString(), eq(null), anyString(), anyLong(), anyString(), anyString(), anyString()))
                .thenReturn("old.example.com");

        String result = provider.deleteRecord(serverMock, zoneMock, record);

        assertEquals("old.example.com", result);
        verify(clientMock).deleteRecordsByNameAndType("https://api.bunny.net", null, "secret", 42L, "example.com", "old", "CNAME");
    }

    @Test
    public void testListRecordsAggregatesByNameAndType() throws DnsProviderException {
        ObjectNode aRecord1 = MAPPER.createObjectNode();
        aRecord1.put("Id", 1);
        aRecord1.put("Name", "www");
        aRecord1.put("Type", 0); // A
        aRecord1.put("Ttl", 300);
        aRecord1.put("Value", "1.2.3.4");

        ObjectNode aRecord2 = MAPPER.createObjectNode();
        aRecord2.put("Id", 2);
        aRecord2.put("Name", "www");
        aRecord2.put("Type", 0); // A
        aRecord2.put("Ttl", 300);
        aRecord2.put("Value", "5.6.7.8");

        when(clientMock.resolveZoneId(anyString(), eq(null), anyString(), anyString())).thenReturn(42L);
        when(clientMock.listRecords(anyString(), eq(null), anyString(), anyLong()))
                .thenReturn(Arrays.asList(aRecord1, aRecord2));
        when(clientMock.toFqdn("www", "example.com")).thenReturn("www.example.com");

        List<DnsRecord> result = provider.listRecords(serverMock, zoneMock);

        assertEquals(1, result.size());
        DnsRecord record = result.get(0);
        assertEquals("www.example.com", record.getName());
        assertEquals(RecordType.A, record.getType());
        assertEquals(300, record.getTtl());
        assertEquals(Arrays.asList("1.2.3.4", "5.6.7.8"), record.getContents());
    }

    @Test
    public void testListRecordsSkipsUnsupportedBunnyTypes() throws DnsProviderException {
        ObjectNode caaRecord = MAPPER.createObjectNode();
        caaRecord.put("Id", 1);
        caaRecord.put("Name", "www");
        caaRecord.put("Type", 9); // CAA - not modeled by DnsRecord.RecordType
        caaRecord.put("Ttl", 300);
        caaRecord.put("Value", "0 issue \"letsencrypt.org\"");

        when(clientMock.resolveZoneId(anyString(), eq(null), anyString(), anyString())).thenReturn(42L);
        when(clientMock.listRecords(anyString(), eq(null), anyString(), anyLong()))
                .thenReturn(Collections.singletonList(caaRecord));

        List<DnsRecord> result = provider.listRecords(serverMock, zoneMock);
        assertTrue(result.isEmpty());
    }

    @Test
    public void testListRecordsReturnsEmptyListWhenClientReturnsEmpty() throws DnsProviderException {
        when(clientMock.resolveZoneId(anyString(), eq(null), anyString(), anyString())).thenReturn(42L);
        when(clientMock.listRecords(anyString(), eq(null), anyString(), anyLong()))
                .thenReturn(Collections.emptyList());

        List<DnsRecord> result = provider.listRecords(serverMock, zoneMock);
        assertNotNull(result);
        assertTrue(result.isEmpty());
    }

    @Test
    public void testDnsRecordExistsTrue() throws DnsProviderException {
        ObjectNode record = MAPPER.createObjectNode();
        record.put("Id", 1);
        when(clientMock.resolveZoneId(anyString(), eq(null), anyString(), anyString())).thenReturn(42L);
        when(clientMock.findRecordsByNameAndType(anyString(), eq(null), anyString(), anyLong(), anyString(), anyString(), anyString()))
                .thenReturn(Collections.singletonList(record));

        assertTrue(provider.dnsRecordExists(serverMock, zoneMock, "www", "A"));
    }

    @Test
    public void testDnsRecordExistsFalse() throws DnsProviderException {
        when(clientMock.resolveZoneId(anyString(), eq(null), anyString(), anyString())).thenReturn(42L);
        when(clientMock.findRecordsByNameAndType(anyString(), eq(null), anyString(), anyLong(), anyString(), anyString(), anyString()))
                .thenReturn(Collections.emptyList());

        assertFalse(provider.dnsRecordExists(serverMock, zoneMock, "www", "A"));
    }

    @Test
    public void testDnsZoneExistsDelegatesToClient() {
        when(clientMock.zoneExists("https://api.bunny.net", null, "secret", "example.com")).thenReturn(true);
        assertTrue(provider.dnsZoneExists(serverMock, zoneMock));
    }
}
