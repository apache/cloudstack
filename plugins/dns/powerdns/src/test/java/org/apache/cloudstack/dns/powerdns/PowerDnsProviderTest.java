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

import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyList;
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

import org.apache.cloudstack.dns.DnsRecord;
import org.apache.cloudstack.dns.DnsRecord.RecordType;
import org.apache.cloudstack.dns.DnsServer;
import org.apache.cloudstack.dns.DnsZone;
import org.apache.cloudstack.dns.DnsProviderType;
import org.apache.cloudstack.dns.exception.DnsProviderException;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.junit.MockitoJUnitRunner;
import org.springframework.test.util.ReflectionTestUtils;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

@RunWith(MockitoJUnitRunner.class)
public class PowerDnsProviderTest {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    private PowerDnsProvider provider;
    private PowerDnsClient clientMock;
    private DnsServer serverMock;
    private DnsZone zoneMock;

    @Before
    public void setUp() {
        provider = new PowerDnsProvider();
        clientMock = mock(PowerDnsClient.class);
        serverMock = mock(DnsServer.class);
        zoneMock = mock(DnsZone.class);
        ReflectionTestUtils.setField(provider, "client", clientMock);

        when(serverMock.getUrl()).thenReturn("http://pdns:8081");
        when(serverMock.getDnsApiKey()).thenReturn("secret");
        when(serverMock.getPort()).thenReturn(8081);
        when(serverMock.getDetail(PowerDnsProvider.PDNS_SERVER_ID)).thenReturn("localhost");
        when(serverMock.getNameServers()).thenReturn(Arrays.asList("ns1.example.com"));

        when(zoneMock.getName()).thenReturn("example.com");
    }

    @Test
    public void testGetProviderType() {
        assertEquals(DnsProviderType.PowerDNS, provider.getProviderType());
    }

    @Test
    public void testConfigureCreatesClientWhenNull() {
        PowerDnsProvider freshProvider = new PowerDnsProvider();
        boolean result = freshProvider.configure("test", new HashMap<>());
        assertTrue(result);
        assertNotNull(ReflectionTestUtils.getField(freshProvider, "client"));
    }

    @Test
    public void testConfigureDoesNotReplaceExistingClient() {
        PowerDnsClient existingClient = mock(PowerDnsClient.class);
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
    public void testValidateServerFieldsBlankUrl() {
        when(serverMock.getUrl()).thenReturn("  ");
        provider.validateRequiredServerFields(serverMock);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testValidateServerFieldsNullApiKey() {
        when(serverMock.getDnsApiKey()).thenReturn(null);
        provider.validateRequiredServerFields(serverMock);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testValidateServerFieldsBlankApiKey() {
        when(serverMock.getDnsApiKey()).thenReturn("");
        provider.validateRequiredServerFields(serverMock);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testValidateServerAndZoneFieldsBlankZoneName() {
        when(zoneMock.getName()).thenReturn("   ");
        provider.validateRequiredServerAndZoneFields(serverMock, zoneMock);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testValidateServerAndZoneFieldsNullZoneName() {
        when(zoneMock.getName()).thenReturn(null);
        provider.validateRequiredServerAndZoneFields(serverMock, zoneMock);
    }

    @Test
    public void testValidateDelegatesToClient() throws DnsProviderException {
        when(clientMock.validateServerId(anyString(), anyInt(), anyString(), anyString())).thenReturn("localhost");
        provider.validate(serverMock);
        verify(clientMock).validateServerId("http://pdns:8081", 8081, "secret", "localhost");
    }

    @Test(expected = IllegalArgumentException.class)
    public void testValidateThrowsWhenServerUrlBlank() throws DnsProviderException {
        when(serverMock.getUrl()).thenReturn("");
        provider.validate(serverMock);
    }

    @Test
    public void testValidateAndResolveServer() throws Exception {
        when(clientMock.resolveServerId(anyString(), anyInt(), anyString(), anyString())).thenReturn("localhost");
        String result = provider.validateAndResolveServer(serverMock);
        assertEquals("localhost", result);
        verify(clientMock).resolveServerId("http://pdns:8081", 8081, "secret", "localhost");
    }

    @Test(expected = IllegalArgumentException.class)
    public void testValidateAndResolveServerThrowsWhenUrlBlank() throws Exception {
        when(serverMock.getUrl()).thenReturn(null);
        provider.validateAndResolveServer(serverMock);
    }

    @Test
    public void testProvisionZoneDelegatesToClient() throws DnsProviderException {
        when(clientMock.createZone(anyString(), anyInt(), anyString(), anyString(), anyString(), anyString(), eq(false), anyList())).thenReturn("example.com.");
        String zoneId = provider.provisionZone(serverMock, zoneMock);
        assertEquals("example.com.", zoneId);
        verify(clientMock).createZone("http://pdns:8081", 8081, "secret", "localhost", "example.com",
                "Native", false, Arrays.asList("ns1.example.com"));
    }

    @Test(expected = IllegalArgumentException.class)
    public void testProvisionZoneThrowsWhenZoneNameBlank() throws DnsProviderException {
        when(zoneMock.getName()).thenReturn(null);
        provider.provisionZone(serverMock, zoneMock);
    }

    @Test
    public void testDeleteZoneDelegatesToClient() throws DnsProviderException {
        provider.deleteZone(serverMock, zoneMock);
        verify(clientMock).deleteZone("http://pdns:8081", 8081, "secret", "localhost", "example.com");
    }

    @Test(expected = IllegalArgumentException.class)
    public void testDeleteZoneThrowsWhenZoneNameBlank() throws DnsProviderException {
        when(zoneMock.getName()).thenReturn("");
        provider.deleteZone(serverMock, zoneMock);
    }

    @Test
    public void testUpdateZoneDelegatesToClient() throws DnsProviderException {
        provider.updateZone(serverMock, zoneMock);
        verify(clientMock).updateZone("http://pdns:8081", 8081, "secret", "localhost", "example.com",
                "Native", false, Arrays.asList("ns1.example.com"));
    }

    @Test
    public void testAddRecordDelegatesToClient() throws DnsProviderException {
        DnsRecord record = new DnsRecord("www", RecordType.A, Arrays.asList("1.2.3.4"), 300);
        when(clientMock.modifyRecord(anyString(), anyInt(), anyString(), anyString(), anyString(),
                anyString(), anyString(), anyLong(), anyList(), anyString())).thenReturn("www.example.com");

        String result = provider.addRecord(serverMock, zoneMock, record);

        assertEquals("www.example.com", result);
        verify(clientMock).modifyRecord("http://pdns:8081", 8081, "secret", "localhost", "example.com",
                "www", "A", 300L, Arrays.asList("1.2.3.4"), "REPLACE");
    }

    @Test(expected = IllegalArgumentException.class)
    public void testAddRecordThrowsWhenServerUrlBlank() throws DnsProviderException {
        when(serverMock.getUrl()).thenReturn("");
        DnsRecord record = new DnsRecord("www", RecordType.A, Arrays.asList("1.2.3.4"), 300);
        provider.addRecord(serverMock, zoneMock, record);
    }

    @Test
    public void testUpdateRecordDelegatesToAddRecord() throws DnsProviderException {
        DnsRecord record = new DnsRecord("mail", RecordType.MX, Arrays.asList("10 mail.example.com"), 300);
        when(clientMock.modifyRecord(anyString(), anyInt(), anyString(), anyString(), anyString(),
                anyString(), anyString(), anyLong(), anyList(), anyString())).thenReturn("mail.example.com");

        String result = provider.updateRecord(serverMock, zoneMock, record);

        assertEquals("mail.example.com", result);
        verify(clientMock).modifyRecord(anyString(), anyInt(), anyString(), anyString(), anyString(),
                eq("mail"), eq("MX"), eq(300L), eq(Arrays.asList("10 mail.example.com")), eq("REPLACE"));
    }

    @Test
    public void testDeleteRecordDelegatesToClientWithDeleteChangeType() throws DnsProviderException {
        DnsRecord record = new DnsRecord("old", RecordType.CNAME, Arrays.asList("target.com"), 600);
        when(clientMock.modifyRecord(anyString(), anyInt(), anyString(), anyString(), anyString(),
                anyString(), anyString(), anyLong(), anyList(), anyString())).thenReturn("old.example.com");

        String result = provider.deleteRecord(serverMock, zoneMock, record);

        assertEquals("old.example.com", result);
        verify(clientMock).modifyRecord(anyString(), anyInt(), anyString(), anyString(), anyString(),
                eq("old"), eq("CNAME"), eq(600L), eq(Arrays.asList("target.com")), eq("DELETE"));
    }

    @Test
    public void testApplyRecordPassesChangeTypeToClient() throws DnsProviderException {
        DnsRecord record = new DnsRecord("txt", RecordType.TXT, Arrays.asList("v=spf1 include:example.com ~all"), 3600);
        when(clientMock.modifyRecord(anyString(), anyInt(), anyString(), anyString(), anyString(),
                anyString(), anyString(), anyLong(), anyList(), anyString())).thenReturn("txt.example.com");

        provider.applyRecord("http://pdns:8081", 8081, "secret", "localhost", "example.com",
                record, PowerDnsProvider.ChangeType.REPLACE);

        verify(clientMock).modifyRecord("http://pdns:8081", 8081, "secret", "localhost", "example.com",
                "txt", "TXT", 3600L, Arrays.asList("v=spf1 include:example.com ~all"), "REPLACE");
    }

    @Test
    public void testListRecordsParsesRrsets() throws DnsProviderException {
        ObjectNode aRecord = MAPPER.createObjectNode();
        aRecord.put("name", "www.example.com.");
        aRecord.put("type", "A");
        aRecord.put("ttl", 300);
        ArrayNode records = aRecord.putArray("records");
        records.addObject().put("content", "1.2.3.4");

        ObjectNode mxRecord = MAPPER.createObjectNode();
        mxRecord.put("name", "example.com.");
        mxRecord.put("type", "MX");
        mxRecord.put("ttl", 600);
        ArrayNode mxRecords = mxRecord.putArray("records");
        mxRecords.addObject().put("content", "10 mail.example.com");

        when(clientMock.listRecords(anyString(), anyInt(), anyString(), anyString(), anyString()))
                .thenReturn(Arrays.asList(aRecord, mxRecord));

        List<DnsRecord> result = provider.listRecords(serverMock, zoneMock);

        assertEquals(2, result.size());

        DnsRecord first = result.get(0);
        assertEquals("www.example.com.", first.getName());
        assertEquals(RecordType.A, first.getType());
        assertEquals(300, first.getTtl());
        assertEquals(Arrays.asList("1.2.3.4"), first.getContents());

        DnsRecord second = result.get(1);
        assertEquals("example.com.", second.getName());
        assertEquals(RecordType.MX, second.getType());
        assertEquals(600, second.getTtl());
        assertEquals(Arrays.asList("10 mail.example.com"), second.getContents());
    }

    @Test
    public void testListRecordsSkipsSoaRecords() throws DnsProviderException {
        ObjectNode soaRecord = MAPPER.createObjectNode();
        soaRecord.put("name", "example.com.");
        soaRecord.put("type", "SOA");
        soaRecord.put("ttl", 3600);
        soaRecord.putArray("records").addObject().put("content", "ns1.example.com. admin.example.com. ...");

        when(clientMock.listRecords(anyString(), anyInt(), anyString(), anyString(), anyString()))
                .thenReturn(Collections.singletonList(soaRecord));

        List<DnsRecord> result = provider.listRecords(serverMock, zoneMock);
        assertTrue(result.isEmpty());
    }

    @Test
    public void testListRecordsSkipsUnknownRecordTypes() throws DnsProviderException {
        ObjectNode unknownRecord = MAPPER.createObjectNode();
        unknownRecord.put("name", "test.example.com.");
        unknownRecord.put("type", "UNKNOWNTYPE");
        unknownRecord.put("ttl", 300);
        unknownRecord.putArray("records").addObject().put("content", "some-data");

        when(clientMock.listRecords(anyString(), anyInt(), anyString(), anyString(), anyString()))
                .thenReturn(Collections.singletonList(unknownRecord));

        List<DnsRecord> result = provider.listRecords(serverMock, zoneMock);
        assertTrue(result.isEmpty());
    }

    @Test
    public void testListRecordsIgnoresEmptyContentEntries() throws DnsProviderException {
        ObjectNode aRecord = MAPPER.createObjectNode();
        aRecord.put("name", "host.example.com.");
        aRecord.put("type", "A");
        aRecord.put("ttl", 300);
        ArrayNode records = aRecord.putArray("records");
        records.addObject().put("content", "");
        records.addObject().put("content", "5.6.7.8");

        when(clientMock.listRecords(anyString(), anyInt(), anyString(), anyString(), anyString()))
                .thenReturn(Collections.singletonList(aRecord));

        List<DnsRecord> result = provider.listRecords(serverMock, zoneMock);
        assertEquals(1, result.size());
        assertEquals(Collections.singletonList("5.6.7.8"), result.get(0).getContents());
    }

    @Test
    public void testListRecordsReturnsEmptyListWhenClientReturnsEmpty() throws DnsProviderException {
        when(clientMock.listRecords(anyString(), anyInt(), anyString(), anyString(), anyString()))
                .thenReturn(Collections.emptyList());

        List<DnsRecord> result = provider.listRecords(serverMock, zoneMock);
        assertNotNull(result);
        assertTrue(result.isEmpty());
    }

    @Test(expected = DnsProviderException.class)
    public void testListRecordsPropagatesClientException() throws DnsProviderException {
        when(clientMock.listRecords(anyString(), anyInt(), anyString(), anyString(), anyString()))
                .thenThrow(mock(DnsProviderException.class));

        provider.listRecords(serverMock, zoneMock);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testListRecordsThrowsWhenZoneNameBlank() throws DnsProviderException {
        when(zoneMock.getName()).thenReturn("");
        provider.listRecords(serverMock, zoneMock);
    }

    @Test
    public void testChangeTypeValues() {
        assertEquals("REPLACE", PowerDnsProvider.ChangeType.REPLACE.name());
        assertEquals("DELETE", PowerDnsProvider.ChangeType.DELETE.name());
    }
}
