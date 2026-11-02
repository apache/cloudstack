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
import static org.junit.Assert.assertNull;

import java.lang.reflect.Field;

import org.apache.cloudstack.dns.DnsZone;
import org.junit.Test;

public class DnsZoneJoinVOTest {

    private void setField(Object target, String fieldName, Object value) throws Exception {
        Field field = target.getClass().getDeclaredField(fieldName);
        field.setAccessible(true);
        field.set(target, value);
    }

    @Test
    public void testDefaultConstructor() {
        DnsZoneJoinVO vo = new DnsZoneJoinVO();
        assertEquals(0L, vo.getId());
        assertNull(vo.getUuid());
        assertNull(vo.getName());
        assertNull(vo.getState());
        assertNull(vo.getDescription());
    }

    @Test
    public void testAllGetters() throws Exception {
        DnsZoneJoinVO vo = new DnsZoneJoinVO();

        setField(vo, "id", 1L);
        setField(vo, "uuid", "zone-uuid");
        setField(vo, "name", "example.com");
        setField(vo, "state", DnsZone.State.Active);
        setField(vo, "dnsServerUuid", "server-uuid");
        setField(vo, "dnsServerName", "pdns-server");
        setField(vo, "dnsServerAccountName", "server-owner");
        setField(vo, "accountName", "admin");
        setField(vo, "domainName", "ROOT");
        setField(vo, "domainUuid", "domain-uuid");
        setField(vo, "domainPath", "/ROOT");
        setField(vo, "description", "Test zone");

        assertEquals(1L, vo.getId());
        assertEquals("zone-uuid", vo.getUuid());
        assertEquals("example.com", vo.getName());
        assertEquals(DnsZone.State.Active, vo.getState());
        assertEquals("server-uuid", vo.getDnsServerUuid());
        assertEquals("pdns-server", vo.getDnsServerName());
        assertEquals("server-owner", vo.getDnsServerAccountName());
        assertEquals("admin", vo.getAccountName());
        assertEquals("ROOT", vo.getDomainName());
        assertEquals("domain-uuid", vo.getDomainUuid());
        assertEquals("/ROOT", vo.getDomainPath());
        assertEquals("Test zone", vo.getDescription());
    }

    @Test
    public void testStateInactive() throws Exception {
        DnsZoneJoinVO vo = new DnsZoneJoinVO();
        setField(vo, "state", DnsZone.State.Inactive);
        assertEquals(DnsZone.State.Inactive, vo.getState());
    }
}
