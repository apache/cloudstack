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

import org.apache.cloudstack.api.response.DnsServerResponse;
import org.apache.cloudstack.api.response.ListResponse;
import org.apache.cloudstack.dns.DnsProviderType;
import org.junit.Test;

public class ListDnsServersCmdTest extends BaseDnsCmdTest {

    private ListDnsServersCmd createCmd() throws Exception {
        ListDnsServersCmd cmd = new ListDnsServersCmd();
        setField(cmd, "dnsProviderManager", dnsProviderManager);
        setField(cmd, "id", ENTITY_ID);
        setField(cmd, "providerType", "PowerDNS");
        return cmd;
    }

    @Test
    public void testAccessors() throws Exception {
        ListDnsServersCmd cmd = createCmd();
        assertEquals(Long.valueOf(ENTITY_ID), cmd.getId());
        assertEquals(DnsProviderType.PowerDNS, cmd.getProviderType());
    }

    @Test
    public void testGetProviderTypeNull() throws Exception {
        ListDnsServersCmd cmd = createCmd();
        setField(cmd, "providerType", null);
        assertEquals(DnsProviderType.PowerDNS, cmd.getProviderType());
    }

    @Test
    public void testExecute() throws Exception {
        ListDnsServersCmd cmd = createCmd();

        ListResponse<DnsServerResponse> mockListResponse = new ListResponse<>();
        when(dnsProviderManager.listDnsServers(cmd)).thenReturn(mockListResponse);

        cmd.execute();

        @SuppressWarnings("unchecked")
        ListResponse<DnsServerResponse> response = (ListResponse<DnsServerResponse>) cmd.getResponseObject();
        assertNotNull(response);
        assertEquals("listdnsserversresponse", response.getResponseName());
        assertEquals("dnsserver", response.getObjectName());
    }
}
