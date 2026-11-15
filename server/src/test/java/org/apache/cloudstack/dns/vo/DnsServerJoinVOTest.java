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
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.Collections;

import org.apache.cloudstack.dns.DnsServer;
import org.junit.Test;

public class DnsServerJoinVOTest {

    private void setField(Object target, String fieldName, Object value) throws Exception {
        Field field = target.getClass().getDeclaredField(fieldName);
        field.setAccessible(true);
        field.set(target, value);
    }

    @Test
    public void testDefaultConstructor() {
        DnsServerJoinVO vo = new DnsServerJoinVO();
        assertEquals(0L, vo.getId());
        assertNull(vo.getUuid());
        assertNull(vo.getName());
    }

    @Test
    public void testAllGetters() throws Exception {
        DnsServerJoinVO vo = new DnsServerJoinVO();

        setField(vo, "id", 1L);
        setField(vo, "uuid", "test-uuid");
        setField(vo, "name", "test-server");
        setField(vo, "providerType", "PowerDNS");
        setField(vo, "url", "http://pdns:8081");
        setField(vo, "port", 8081);
        setField(vo, "nameServers", "ns1.example.com,ns2.example.com");
        setField(vo, "isPublic", true);
        setField(vo, "publicDomainSuffix", "pub.example.com");
        setField(vo, "state", DnsServer.State.Enabled);
        setField(vo, "accountName", "admin");
        setField(vo, "domainName", "ROOT");
        setField(vo, "domainUuid", "domain-uuid");
        setField(vo, "domainPath", "/ROOT");

        assertEquals(1L, vo.getId());
        assertEquals("test-uuid", vo.getUuid());
        assertEquals("test-server", vo.getName());
        assertEquals("PowerDNS", vo.getProviderType());
        assertEquals("http://pdns:8081", vo.getUrl());
        assertEquals(Integer.valueOf(8081), vo.getPort());
        assertEquals(Arrays.asList("ns1.example.com", "ns2.example.com"), vo.getNameServers());
        assertTrue(vo.isPublicServer());
        assertEquals("pub.example.com", vo.getPublicDomainSuffix());
        assertEquals(DnsServer.State.Enabled, vo.getState());
        assertEquals("admin", vo.getAccountName());
        assertEquals("ROOT", vo.getDomainName());
        assertEquals("domain-uuid", vo.getDomainUuid());
        assertEquals("/ROOT", vo.getDomainPath());
    }

    @Test
    public void testGetNameServersEmpty() {
        DnsServerJoinVO vo = new DnsServerJoinVO();
        assertEquals(Collections.emptyList(), vo.getNameServers());
    }

    @Test
    public void testGetNameServersBlank() throws Exception {
        DnsServerJoinVO vo = new DnsServerJoinVO();
        setField(vo, "nameServers", "");
        assertEquals(Collections.emptyList(), vo.getNameServers());
    }

    @Test
    public void testIsPublicServerDefault() {
        DnsServerJoinVO vo = new DnsServerJoinVO();
        assertFalse(vo.isPublicServer());
    }
}
