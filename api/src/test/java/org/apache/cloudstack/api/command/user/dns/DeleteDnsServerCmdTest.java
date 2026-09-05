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

import org.apache.cloudstack.api.ServerApiException;
import org.apache.cloudstack.api.response.SuccessResponse;
import org.apache.cloudstack.dns.DnsServer;
import org.junit.Test;

import com.cloud.event.EventTypes;
import com.cloud.user.Account;

public class DeleteDnsServerCmdTest extends BaseDnsCmdTest {

    private DeleteDnsServerCmd createCmd() throws Exception {
        DeleteDnsServerCmd cmd = new DeleteDnsServerCmd();
        setField(cmd, "dnsProviderManager", dnsProviderManager);
        setField(cmd, "_entityMgr", entityManager);
        setField(cmd, "id", ENTITY_ID);
        return cmd;
    }

    @Test
    public void testGetId() throws Exception {
        DeleteDnsServerCmd cmd = createCmd();
        assertEquals(Long.valueOf(ENTITY_ID), cmd.getId());
    }

    @Test
    public void testGetCleanupDefault() throws Exception {
        DeleteDnsServerCmd cmd = createCmd();
        assertTrue(cmd.getCleanup());
    }

    @Test
    public void testGetCleanupFalse() throws Exception {
        DeleteDnsServerCmd cmd = createCmd();
        setField(cmd, "cleanup", false);
        assertFalse(cmd.getCleanup());
    }

    @Test
    public void testGetEntityOwnerIdWithServer() throws Exception {
        DeleteDnsServerCmd cmd = createCmd();
        DnsServer mockServer = mock(DnsServer.class);
        when(mockServer.getAccountId()).thenReturn(ACCOUNT_ID);
        when(entityManager.findById(DnsServer.class, ENTITY_ID)).thenReturn(mockServer);

        assertEquals(ACCOUNT_ID, cmd.getEntityOwnerId());
    }

    @Test
    public void testGetEntityOwnerIdServerNotFound() throws Exception {
        DeleteDnsServerCmd cmd = createCmd();
        when(entityManager.findById(DnsServer.class, ENTITY_ID)).thenReturn(null);

        assertEquals(Account.ACCOUNT_ID_SYSTEM, cmd.getEntityOwnerId());
    }

    @Test
    public void testEventType() throws Exception {
        DeleteDnsServerCmd cmd = createCmd();
        assertEquals(EventTypes.EVENT_DNS_SERVER_DELETE, cmd.getEventType());
    }

    @Test
    public void testEventDescription() throws Exception {
        DeleteDnsServerCmd cmd = createCmd();
        assertEquals("Deleting DNS server ID: " + ENTITY_ID, cmd.getEventDescription());
    }

    @Test
    public void testExecuteSuccess() throws Exception {
        DeleteDnsServerCmd cmd = createCmd();
        when(dnsProviderManager.deleteDnsServer(cmd)).thenReturn(true);

        cmd.execute();

        SuccessResponse response = (SuccessResponse) cmd.getResponseObject();
        assertNotNull(response);
        verify(dnsProviderManager).deleteDnsServer(cmd);
    }

    @Test(expected = ServerApiException.class)
    public void testExecuteReturnsFalse() throws Exception {
        DeleteDnsServerCmd cmd = createCmd();
        when(dnsProviderManager.deleteDnsServer(cmd)).thenReturn(false);
        cmd.execute();
    }

    @Test(expected = ServerApiException.class)
    public void testExecuteThrowsException() throws Exception {
        DeleteDnsServerCmd cmd = createCmd();
        when(dnsProviderManager.deleteDnsServer(cmd)).thenThrow(new RuntimeException("Error"));
        cmd.execute();
    }
}
