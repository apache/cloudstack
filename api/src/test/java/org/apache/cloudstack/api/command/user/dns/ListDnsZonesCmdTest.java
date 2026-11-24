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
import static org.mockito.Mockito.when;

import org.apache.cloudstack.api.response.DnsZoneResponse;
import org.apache.cloudstack.api.response.ListResponse;
import org.junit.Test;

public class ListDnsZonesCmdTest extends BaseDnsCmdTest {

    private ListDnsZonesCmd createCmd() throws Exception {
        ListDnsZonesCmd cmd = new ListDnsZonesCmd();
        setField(cmd, "dnsProviderManager", dnsProviderManager);
        setField(cmd, "id", ENTITY_ID);
        setField(cmd, "dnsServerId", 200L);
        setField(cmd, "name", "example.com");
        return cmd;
    }

    @Test
    public void testAccessors() throws Exception {
        ListDnsZonesCmd cmd = createCmd();
        assertEquals(Long.valueOf(ENTITY_ID), cmd.getId());
        assertEquals(Long.valueOf(200L), cmd.getDnsServerId());
        assertEquals("example.com", cmd.getName());
    }

    @Test
    public void testExecute() throws Exception {
        ListDnsZonesCmd cmd = createCmd();

        ListResponse<DnsZoneResponse> mockListResponse = new ListResponse<>();
        when(dnsProviderManager.listDnsZones(cmd)).thenReturn(mockListResponse);

        cmd.execute();

        @SuppressWarnings("unchecked")
        ListResponse<DnsZoneResponse> response = (ListResponse<DnsZoneResponse>) cmd.getResponseObject();
        assertNotNull(response);
        assertEquals("listdnszonesresponse", response.getResponseName());
    }
}
