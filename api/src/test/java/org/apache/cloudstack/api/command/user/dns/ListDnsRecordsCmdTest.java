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

import org.apache.cloudstack.api.response.DnsRecordResponse;
import org.apache.cloudstack.api.response.ListResponse;
import org.junit.Test;

public class ListDnsRecordsCmdTest extends BaseDnsCmdTest {

    private ListDnsRecordsCmd createCmd() throws Exception {
        ListDnsRecordsCmd cmd = new ListDnsRecordsCmd();
        setField(cmd, "dnsProviderManager", dnsProviderManager);
        setField(cmd, "dnsZoneId", ENTITY_ID);
        return cmd;
    }

    @Test
    public void testGetDnsZoneId() throws Exception {
        ListDnsRecordsCmd cmd = createCmd();
        assertEquals(Long.valueOf(ENTITY_ID), cmd.getDnsZoneId());
    }

    @Test
    public void testExecute() throws Exception {
        ListDnsRecordsCmd cmd = createCmd();

        ListResponse<DnsRecordResponse> mockListResponse = new ListResponse<>();
        when(dnsProviderManager.listDnsRecords(cmd)).thenReturn(mockListResponse);

        cmd.execute();

        @SuppressWarnings("unchecked")
        ListResponse<DnsRecordResponse> response = (ListResponse<DnsRecordResponse>) cmd.getResponseObject();
        assertNotNull(response);
        assertEquals("listdnsrecordsresponse", response.getResponseName());
    }
}
