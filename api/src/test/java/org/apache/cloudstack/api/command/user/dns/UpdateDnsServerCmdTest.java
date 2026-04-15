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
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import org.apache.cloudstack.api.ServerApiException;
import org.apache.cloudstack.api.response.DnsServerResponse;
import org.apache.cloudstack.dns.DnsServer;
import org.junit.Test;

import com.cloud.user.Account;

public class UpdateDnsServerCmdTest extends BaseDnsCmdTest {

    private UpdateDnsServerCmd createCmd() throws Exception {
        UpdateDnsServerCmd cmd = new UpdateDnsServerCmd();
        setField(cmd, "dnsProviderManager", dnsProviderManager);
        setField(cmd, "_entityMgr", entityManager);
        setField(cmd, "id", ENTITY_ID);
        setField(cmd, "name", "updated-dns");
        setField(cmd, "url", "http://updated.dns.com");
        setField(cmd, "apiKey", "new-api-key");
        setField(cmd, "port", 9090);
        setField(cmd, "isPublic", true);
        setField(cmd, "publicDomainSuffix", "updated.example.com");
        setField(cmd, "nameServers", "ns1.updated.com,ns2.updated.com");
        setField(cmd, "state", "Enabled");
        return cmd;
    }

    @Test
    public void testAccessors() throws Exception {
        UpdateDnsServerCmd cmd = createCmd();

        assertEquals(Long.valueOf(ENTITY_ID), cmd.getId());
        assertEquals("updated-dns", cmd.getName());
        assertEquals("http://updated.dns.com", cmd.getUrl());
        assertEquals("new-api-key", cmd.getApiKey());
        assertEquals(Integer.valueOf(9090), cmd.getPort());
        assertTrue(cmd.isPublic());
        assertEquals("updated.example.com", cmd.getPublicDomainSuffix());
        assertEquals("ns1.updated.com,ns2.updated.com", cmd.getNameServers());
        assertEquals(DnsServer.State.Enabled, cmd.getState());
    }

    @Test
    public void testGetStateDisabled() throws Exception {
        UpdateDnsServerCmd cmd = createCmd();
        setField(cmd, "state", "Disabled");
        assertEquals(DnsServer.State.Disabled, cmd.getState());
    }

    @Test
    public void testGetStateNull() throws Exception {
        UpdateDnsServerCmd cmd = createCmd();
        setField(cmd, "state", null);
        assertNull(cmd.getState());
    }

    @Test
    public void testGetStateBlank() throws Exception {
        UpdateDnsServerCmd cmd = createCmd();
        setField(cmd, "state", "");
        assertNull(cmd.getState());
    }

    @Test(expected = IllegalArgumentException.class)
    public void testGetStateInvalid() throws Exception {
        UpdateDnsServerCmd cmd = createCmd();
        setField(cmd, "state", "InvalidState");
        cmd.getState();
    }

    @Test
    public void testGetEntityOwnerIdWithServer() throws Exception {
        UpdateDnsServerCmd cmd = createCmd();
        DnsServer mockServer = mock(DnsServer.class);
        when(mockServer.getAccountId()).thenReturn(ACCOUNT_ID);
        when(entityManager.findById(DnsServer.class, ENTITY_ID)).thenReturn(mockServer);

        assertEquals(ACCOUNT_ID, cmd.getEntityOwnerId());
    }

    @Test
    public void testGetEntityOwnerIdServerNotFound() throws Exception {
        UpdateDnsServerCmd cmd = createCmd();
        when(entityManager.findById(DnsServer.class, ENTITY_ID)).thenReturn(null);

        assertEquals(Account.ACCOUNT_ID_SYSTEM, cmd.getEntityOwnerId());
    }

    @Test
    public void testExecuteSuccess() throws Exception {
        UpdateDnsServerCmd cmd = createCmd();

        DnsServer mockServer = mock(DnsServer.class);
        DnsServerResponse mockResponse = new DnsServerResponse();
        mockResponse.setName("updated-dns");

        when(dnsProviderManager.updateDnsServer(cmd)).thenReturn(mockServer);
        when(dnsProviderManager.createDnsServerResponse(mockServer)).thenReturn(mockResponse);

        cmd.execute();

        DnsServerResponse response = (DnsServerResponse) cmd.getResponseObject();
        assertNotNull(response);
        assertEquals("updatednsserverresponse", response.getResponseName());
    }

    @Test(expected = ServerApiException.class)
    public void testExecuteReturnsNull() throws Exception {
        UpdateDnsServerCmd cmd = createCmd();
        when(dnsProviderManager.updateDnsServer(cmd)).thenReturn(null);
        cmd.execute();
    }

    @Test(expected = ServerApiException.class)
    public void testExecuteThrowsException() throws Exception {
        UpdateDnsServerCmd cmd = createCmd();
        when(dnsProviderManager.updateDnsServer(cmd)).thenThrow(new RuntimeException("Update failed"));
        cmd.execute();
    }
}
