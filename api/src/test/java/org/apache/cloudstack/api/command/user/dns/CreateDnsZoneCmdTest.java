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
package org.apache.cloudstack.api.command.user.dns;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import org.apache.cloudstack.api.ServerApiException;
import org.apache.cloudstack.api.response.DnsZoneResponse;
import org.apache.cloudstack.dns.DnsZone;
import org.junit.Test;

import com.cloud.event.EventTypes;

public class CreateDnsZoneCmdTest extends BaseDnsCmdTest {

    private CreateDnsZoneCmd createCmd() throws Exception {
        CreateDnsZoneCmd cmd = new CreateDnsZoneCmd();
        setField(cmd, "dnsProviderManager", dnsProviderManager);
        setField(cmd, "name", "example.com");
        setField(cmd, "dnsServerId", ENTITY_ID);
        setField(cmd, "type", "Public");
        setField(cmd, "description", "Test zone");
        return cmd;
    }

    @Test
    public void testAccessors() throws Exception {
        CreateDnsZoneCmd cmd = createCmd();

        assertEquals("example.com", cmd.getName());
        assertEquals(Long.valueOf(ENTITY_ID), cmd.getDnsServerId());
        assertEquals(DnsZone.ZoneType.Public, cmd.getType());
        assertEquals("Test zone", cmd.getDescription());
    }

    @Test
    public void testGetTypePrivate() throws Exception {
        CreateDnsZoneCmd cmd = createCmd();
        setField(cmd, "type", "Private");
        assertEquals(DnsZone.ZoneType.Private, cmd.getType());
    }

    @Test
    public void testGetTypeDefaultsToPublicWhenNull() throws Exception {
        CreateDnsZoneCmd cmd = createCmd();
        setField(cmd, "type", null);
        assertEquals(DnsZone.ZoneType.Public, cmd.getType());
    }

    @Test
    public void testGetTypeDefaultsToPublicWhenBlank() throws Exception {
        CreateDnsZoneCmd cmd = createCmd();
        setField(cmd, "type", "");
        assertEquals(DnsZone.ZoneType.Public, cmd.getType());
    }

    @Test
    public void testGetEntityOwnerId() throws Exception {
        CreateDnsZoneCmd cmd = createCmd();
        assertEquals(ACCOUNT_ID, cmd.getEntityOwnerId());
    }

    @Test
    public void testEventType() throws Exception {
        CreateDnsZoneCmd cmd = createCmd();
        assertEquals(EventTypes.EVENT_DNS_ZONE_CREATE, cmd.getEventType());
    }

    @Test
    public void testEventDescription() throws Exception {
        CreateDnsZoneCmd cmd = createCmd();
        assertEquals("creating DNS zone: example.com", cmd.getEventDescription());
    }

    @Test
    public void testCreateSuccess() throws Exception {
        CreateDnsZoneCmd cmd = createCmd();

        DnsZone mockZone = mock(DnsZone.class);
        when(mockZone.getId()).thenReturn(ENTITY_ID);
        when(mockZone.getUuid()).thenReturn("uuid-123");
        when(dnsProviderManager.allocateDnsZone(cmd)).thenReturn(mockZone);

        cmd.create();

        assertEquals(Long.valueOf(ENTITY_ID), cmd.getEntityId());
        assertEquals("uuid-123", cmd.getEntityUuid());
    }

    @Test(expected = ServerApiException.class)
    public void testCreateReturnsNull() throws Exception {
        CreateDnsZoneCmd cmd = createCmd();
        when(dnsProviderManager.allocateDnsZone(cmd)).thenReturn(null);
        cmd.create();
    }

    @Test(expected = ServerApiException.class)
    public void testCreateThrowsException() throws Exception {
        CreateDnsZoneCmd cmd = createCmd();
        when(dnsProviderManager.allocateDnsZone(cmd)).thenThrow(new RuntimeException("DB error"));
        cmd.create();
    }

    @Test
    public void testExecuteSuccess() throws Exception {
        CreateDnsZoneCmd cmd = createCmd();
        cmd.setEntityId(ENTITY_ID);

        DnsZone mockZone = mock(DnsZone.class);
        DnsZoneResponse mockResponse = new DnsZoneResponse();
        mockResponse.setName("example.com");

        when(dnsProviderManager.provisionDnsZone(ENTITY_ID)).thenReturn(mockZone);
        when(dnsProviderManager.createDnsZoneResponse(mockZone)).thenReturn(mockResponse);

        cmd.execute();

        DnsZoneResponse response = (DnsZoneResponse) cmd.getResponseObject();
        assertNotNull(response);
        assertEquals("creatednszoneresponse", response.getResponseName());
    }

    @Test(expected = ServerApiException.class)
    public void testExecuteReturnsNull() throws Exception {
        CreateDnsZoneCmd cmd = createCmd();
        cmd.setEntityId(ENTITY_ID);

        when(dnsProviderManager.provisionDnsZone(ENTITY_ID)).thenReturn(null);
        cmd.execute();
    }

    @Test(expected = ServerApiException.class)
    public void testExecuteThrowsException() throws Exception {
        CreateDnsZoneCmd cmd = createCmd();
        cmd.setEntityId(ENTITY_ID);

        when(dnsProviderManager.provisionDnsZone(ENTITY_ID)).thenThrow(new RuntimeException("Provider error"));
        cmd.execute();
    }
}
