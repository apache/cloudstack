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
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Arrays;

import org.apache.cloudstack.api.ServerApiException;
import org.apache.cloudstack.api.response.DnsServerResponse;
import org.apache.cloudstack.dns.DnsProviderType;
import org.apache.cloudstack.dns.DnsServer;
import org.junit.Test;

public class AddDnsServerCmdTest extends BaseDnsCmdTest {

    private AddDnsServerCmd createCmd() throws Exception {
        AddDnsServerCmd cmd = new AddDnsServerCmd();
        setField(cmd, "dnsProviderManager", dnsProviderManager);
        setField(cmd, "name", "test-dns");
        setField(cmd, "url", "http://dns.example.com");
        setField(cmd, "provider", "PowerDNS");
        setField(cmd, "apiKey", "api-key-123");
        setField(cmd, "port", 8081);
        setField(cmd, "isPublic", true);
        setField(cmd, "publicDomainSuffix", "public.example.com");
        setField(cmd, "nameServers", Arrays.asList("ns1.example.com", "ns2.example.com"));
        setField(cmd, "externalServerId", "localhost");
        setField(cmd, "dnsUserName", "admin@example.com");
        return cmd;
    }

    @Test
    public void testAccessors() throws Exception {
        AddDnsServerCmd cmd = createCmd();

        assertEquals("test-dns", cmd.getName());
        assertEquals("http://dns.example.com", cmd.getUrl());
        assertEquals("api-key-123", cmd.getApiKey());
        assertEquals(Integer.valueOf(8081), cmd.getPort());
        assertTrue(cmd.isPublic());
        assertEquals("public.example.com", cmd.getPublicDomainSuffix());
        assertEquals(Arrays.asList("ns1.example.com", "ns2.example.com"), cmd.getNameServers());
        assertEquals(DnsProviderType.PowerDNS, cmd.getProvider());
        assertEquals("localhost", cmd.getExternalServerId());
        assertEquals("admin@example.com", cmd.getDnsUserName());
    }

    @Test
    public void testIsPublicFalse() throws Exception {
        AddDnsServerCmd cmd = createCmd();
        setField(cmd, "isPublic", false);
        assertFalse(cmd.isPublic());
    }

    @Test
    public void testIsPublicNull() throws Exception {
        AddDnsServerCmd cmd = createCmd();
        setField(cmd, "isPublic", null);
        assertFalse(cmd.isPublic());
    }

    @Test
    public void testGetEntityOwnerId() throws Exception {
        AddDnsServerCmd cmd = createCmd();
        assertEquals(ACCOUNT_ID, cmd.getEntityOwnerId());
    }

    @Test
    public void testGetProviderDefault() throws Exception {
        AddDnsServerCmd cmd = createCmd();
        setField(cmd, "provider", null);
        assertEquals(DnsProviderType.PowerDNS, cmd.getProvider());
    }

    @Test
    public void testGetProviderCaseInsensitive() throws Exception {
        AddDnsServerCmd cmd = createCmd();
        setField(cmd, "provider", "powerdns");
        assertEquals(DnsProviderType.PowerDNS, cmd.getProvider());
    }

    @Test
    public void testExecuteSuccess() throws Exception {
        AddDnsServerCmd cmd = createCmd();

        DnsServer mockServer = mock(DnsServer.class);
        DnsServerResponse mockResponse = new DnsServerResponse();
        mockResponse.setName("test-dns");

        when(dnsProviderManager.addDnsServer(cmd)).thenReturn(mockServer);
        when(dnsProviderManager.createDnsServerResponse(mockServer)).thenReturn(mockResponse);

        cmd.execute();

        DnsServerResponse response = (DnsServerResponse) cmd.getResponseObject();
        assertNotNull(response);
        assertEquals("adddnsserverresponse", response.getResponseName());
        verify(dnsProviderManager).addDnsServer(cmd);
        verify(dnsProviderManager).createDnsServerResponse(mockServer);
    }

    @Test(expected = ServerApiException.class)
    public void testExecuteReturnsNull() throws Exception {
        AddDnsServerCmd cmd = createCmd();
        when(dnsProviderManager.addDnsServer(cmd)).thenReturn(null);
        cmd.execute();
    }

    @Test(expected = ServerApiException.class)
    public void testExecuteThrowsException() throws Exception {
        AddDnsServerCmd cmd = createCmd();
        when(dnsProviderManager.addDnsServer(cmd)).thenThrow(new RuntimeException("Connection refused"));
        cmd.execute();
    }
}
