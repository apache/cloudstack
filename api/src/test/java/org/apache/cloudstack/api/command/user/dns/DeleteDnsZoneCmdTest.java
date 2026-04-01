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
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.apache.cloudstack.api.ServerApiException;
import org.apache.cloudstack.api.response.SuccessResponse;
import org.apache.cloudstack.dns.DnsZone;
import org.junit.Test;

import com.cloud.event.EventTypes;
import com.cloud.user.Account;

public class DeleteDnsZoneCmdTest extends BaseDnsCmdTest {

    private DeleteDnsZoneCmd createCmd() throws Exception {
        DeleteDnsZoneCmd cmd = new DeleteDnsZoneCmd();
        setField(cmd, "dnsProviderManager", dnsProviderManager);
        setField(cmd, "_entityMgr", entityManager);
        setField(cmd, "id", ENTITY_ID);
        return cmd;
    }

    @Test
    public void testGetId() throws Exception {
        DeleteDnsZoneCmd cmd = createCmd();
        assertEquals(Long.valueOf(ENTITY_ID), cmd.getId());
    }

    @Test
    public void testGetEntityOwnerIdWithZone() throws Exception {
        DeleteDnsZoneCmd cmd = createCmd();
        DnsZone mockZone = mock(DnsZone.class);
        when(mockZone.getAccountId()).thenReturn(ACCOUNT_ID);
        when(entityManager.findById(DnsZone.class, ENTITY_ID)).thenReturn(mockZone);

        assertEquals(ACCOUNT_ID, cmd.getEntityOwnerId());
    }

    @Test
    public void testGetEntityOwnerIdZoneNotFound() throws Exception {
        DeleteDnsZoneCmd cmd = createCmd();
        when(entityManager.findById(DnsZone.class, ENTITY_ID)).thenReturn(null);

        assertEquals(Account.ACCOUNT_ID_SYSTEM, cmd.getEntityOwnerId());
    }

    @Test
    public void testEventType() throws Exception {
        DeleteDnsZoneCmd cmd = createCmd();
        assertEquals(EventTypes.EVENT_DNS_ZONE_DELETE, cmd.getEventType());
    }

    @Test
    public void testEventDescription() throws Exception {
        DeleteDnsZoneCmd cmd = createCmd();
        assertEquals("Deleting DNS Zone ID: " + ENTITY_ID, cmd.getEventDescription());
    }

    @Test
    public void testExecuteSuccess() throws Exception {
        DeleteDnsZoneCmd cmd = createCmd();
        when(dnsProviderManager.deleteDnsZone(ENTITY_ID)).thenReturn(true);

        cmd.execute();

        SuccessResponse response = (SuccessResponse) cmd.getResponseObject();
        assertNotNull(response);
        verify(dnsProviderManager).deleteDnsZone(ENTITY_ID);
    }

    @Test(expected = ServerApiException.class)
    public void testExecuteReturnsFalse() throws Exception {
        DeleteDnsZoneCmd cmd = createCmd();
        when(dnsProviderManager.deleteDnsZone(ENTITY_ID)).thenReturn(false);
        cmd.execute();
    }

    @Test(expected = ServerApiException.class)
    public void testExecuteThrowsException() throws Exception {
        DeleteDnsZoneCmd cmd = createCmd();
        when(dnsProviderManager.deleteDnsZone(ENTITY_ID)).thenThrow(new RuntimeException("Error"));
        cmd.execute();
    }
}
