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

public class UpdateDnsZoneCmdTest extends BaseDnsCmdTest {

    private UpdateDnsZoneCmd createCmd() throws Exception {
        UpdateDnsZoneCmd cmd = new UpdateDnsZoneCmd();
        setField(cmd, "dnsProviderManager", dnsProviderManager);
        setField(cmd, "id", ENTITY_ID);
        setField(cmd, "description", "Updated description");
        return cmd;
    }

    @Test
    public void testAccessors() throws Exception {
        UpdateDnsZoneCmd cmd = createCmd();
        assertEquals(Long.valueOf(ENTITY_ID), cmd.getId());
        assertEquals("Updated description", cmd.getDescription());
    }

    @Test
    public void testGetEntityOwnerId() throws Exception {
        UpdateDnsZoneCmd cmd = createCmd();
        assertEquals(ACCOUNT_ID, cmd.getEntityOwnerId());
    }

    @Test
    public void testExecuteSuccess() throws Exception {
        UpdateDnsZoneCmd cmd = createCmd();

        DnsZone mockZone = mock(DnsZone.class);
        DnsZoneResponse mockResponse = new DnsZoneResponse();
        mockResponse.setName("example.com");

        when(dnsProviderManager.updateDnsZone(cmd)).thenReturn(mockZone);
        when(dnsProviderManager.createDnsZoneResponse(mockZone)).thenReturn(mockResponse);

        cmd.execute();

        DnsZoneResponse response = (DnsZoneResponse) cmd.getResponseObject();
        assertNotNull(response);
        assertEquals("updatednszoneresponse", response.getResponseName());
    }

    @Test(expected = ServerApiException.class)
    public void testExecuteReturnsNull() throws Exception {
        UpdateDnsZoneCmd cmd = createCmd();
        when(dnsProviderManager.updateDnsZone(cmd)).thenReturn(null);
        cmd.execute();
    }

    @Test(expected = ServerApiException.class)
    public void testExecuteThrowsException() throws Exception {
        UpdateDnsZoneCmd cmd = createCmd();
        when(dnsProviderManager.updateDnsZone(cmd)).thenThrow(new RuntimeException("Update failed"));
        cmd.execute();
    }
}
