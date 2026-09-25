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
package org.apache.cloudstack.dns.vo;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.apache.cloudstack.dns.DnsProviderType;
import org.apache.cloudstack.dns.DnsServer;
import org.junit.Test;

public class DnsServerVOTest {

    @Test
    public void testDefaultConstructor() {
        DnsServerVO vo = new DnsServerVO();
        assertNotNull(vo.getUuid());
        assertNotNull(vo.getCreated());
    }

    @Test
    public void testParameterizedConstructor() {
        List<String> nameServers = Arrays.asList("ns1.example.com", "ns2.example.com");
        DnsServerVO vo = new DnsServerVO("test-server", "http://pdns:8081", 8081, DnsProviderType.PowerDNS, "admin", "api-key-123",
                true, "public.example.com", nameServers, 10L, 20L);

        assertEquals("test-server", vo.getName());
        assertEquals("http://pdns:8081", vo.getUrl());
        assertEquals(Integer.valueOf(8081), vo.getPort());
//        assertEquals("localhost", vo.getDetail());
        assertEquals(DnsProviderType.PowerDNS, vo.getProviderType());
        assertEquals("api-key-123", vo.getDnsApiKey());
        assertTrue(vo.getPublicServer());
        assertEquals("public.example.com", vo.getPublicDomainSuffix());
        assertEquals(nameServers, vo.getNameServers());
        assertEquals(10L, vo.getAccountId());
        assertEquals(20L, vo.getDomainId());
        assertEquals(DnsServer.State.Enabled, vo.getState());
        assertNotNull(vo.getUuid());
        assertNotNull(vo.getCreated());
        assertNull(vo.getRemoved());
    }

    @Test
    public void testGetEntityType() {
        DnsServerVO vo = new DnsServerVO();
        assertEquals(DnsServer.class, vo.getEntityType());
    }

    @Test
    public void testSettersAndGetters() {
        DnsServerVO vo = new DnsServerVO();

        vo.setName("updated");
        assertEquals("updated", vo.getName());

        vo.setUrl("http://new-url:8081");
        assertEquals("http://new-url:8081", vo.getUrl());

        vo.setPort(9090);
        assertEquals(Integer.valueOf(9090), vo.getPort());

        vo.setDnsApiKey("new-key");
        assertEquals("new-key", vo.getDnsApiKey());

        vo.setPublicServer(true);
        assertTrue(vo.getPublicServer());

        vo.setPublicDomainSuffix("new.suffix.com");
        assertEquals("new.suffix.com", vo.getPublicDomainSuffix());

        vo.setState(DnsServer.State.Disabled);
        assertEquals(DnsServer.State.Disabled, vo.getState());

        vo.setNameServers("ns1.test.com,ns2.test.com");
        assertEquals(Arrays.asList("ns1.test.com", "ns2.test.com"), vo.getNameServers());
    }

    @Test
    public void testGetNameServersEmpty() {
        DnsServerVO vo = new DnsServerVO();
        assertEquals(Collections.emptyList(), vo.getNameServers());
    }

    @Test
    public void testGetNameServersBlank() {
        DnsServerVO vo = new DnsServerVO();
        vo.setNameServers("");
        assertEquals(Collections.emptyList(), vo.getNameServers());
    }

    @Test
    public void testGetNameServersSingle() {
        DnsServerVO vo = new DnsServerVO();
        vo.setNameServers("ns1.example.com");
        assertEquals(Collections.singletonList("ns1.example.com"), vo.getNameServers());
    }

    @Test
    public void testToString() {
        List<String> nameServers = Collections.singletonList("ns1.example.com");
        DnsServerVO vo = new DnsServerVO("test-server", "http://pdns:8081", 8081, DnsProviderType.PowerDNS, null, "secret-key",
                false, null, nameServers, 1L, 10L);

        String result = vo.toString();
        assertTrue(result.contains("test-server"));
        assertTrue(result.contains("http://pdns:8081"));
        // API key should be masked
        assertTrue(result.contains("*****"));
        assertFalse(result.contains("secret-key"));
    }

    @Test
    public void testConstructorNotPublicServer() {
        List<String> nameServers = Collections.singletonList("ns1.example.com");
        DnsServerVO vo = new DnsServerVO("srv", "http://url", null,
                DnsProviderType.PowerDNS, null, "key",
                false, null, nameServers, 1L, 1L);

        assertFalse(vo.getPublicServer());
        assertNull(vo.getPort());
    }
}
