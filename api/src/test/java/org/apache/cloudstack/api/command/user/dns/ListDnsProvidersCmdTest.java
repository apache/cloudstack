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

import java.util.Arrays;
import java.util.Collections;

import org.apache.cloudstack.api.response.DnsProviderResponse;
import org.apache.cloudstack.api.response.ListResponse;
import org.junit.Test;

public class ListDnsProvidersCmdTest extends BaseDnsCmdTest {

    private ListDnsProvidersCmd createCmd() throws Exception {
        ListDnsProvidersCmd cmd = new ListDnsProvidersCmd();
        setField(cmd, "dnsProviderManager", dnsProviderManager);
        return cmd;
    }

    @Test
    public void testExecute() throws Exception {
        ListDnsProvidersCmd cmd = createCmd();
        when(dnsProviderManager.listProviderNames()).thenReturn(Arrays.asList("PowerDNS"));

        cmd.execute();

        @SuppressWarnings("unchecked")
        ListResponse<DnsProviderResponse> response =
                (ListResponse<DnsProviderResponse>) cmd.getResponseObject();
        assertNotNull(response);
        assertEquals("listdnsprovidersresponse", response.getResponseName());
        assertNotNull(response.getResponses());
        assertEquals(1, response.getResponses().size());
    }

    @Test
    public void testExecuteMultipleProviders() throws Exception {
        ListDnsProvidersCmd cmd = createCmd();
        when(dnsProviderManager.listProviderNames())
                .thenReturn(Arrays.asList("PowerDNS", "Cloudflare"));

        cmd.execute();

        @SuppressWarnings("unchecked")
        ListResponse<DnsProviderResponse> response =
                (ListResponse<DnsProviderResponse>) cmd.getResponseObject();
        assertNotNull(response);
        assertEquals(2, response.getResponses().size());
    }

    @Test
    public void testExecuteEmptyList() throws Exception {
        ListDnsProvidersCmd cmd = createCmd();
        when(dnsProviderManager.listProviderNames())
                .thenReturn(Collections.emptyList());

        cmd.execute();

        @SuppressWarnings("unchecked")
        ListResponse<DnsProviderResponse> response =
                (ListResponse<DnsProviderResponse>) cmd.getResponseObject();
        assertNotNull(response);
        assertEquals(0, response.getResponses().size());
    }
}
