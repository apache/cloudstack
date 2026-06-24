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
package org.apache.cloudstack.veeam.api.dto;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.apache.cloudstack.veeam.utils.Mapper;
import org.junit.Test;

public class BaseDtoTest {
    @Test
    public void zeroUuid_ConstantValue() {
        assertEquals("00000000-0000-0000-0000-000000000000", BaseDto.ZERO_UUID);
    }

    @Test
    public void getActionLink_BuildsRelAndHref() {
        Link link = BaseDto.getActionLink("start", "/ovirt-engine/api/vms/1");
        assertEquals("start", link.getRel());
        assertEquals("/ovirt-engine/api/vms/1/start", link.getHref());
    }

    @Test
    public void ref_of_SetsHrefAndId() {
        Ref ref = Ref.of("https://host/api/templates/abc", "abc");
        assertEquals("https://host/api/templates/abc", ref.getHref());
        assertEquals("abc", ref.getId());
    }

    @Test
    public void link_of_SetsRelAndHref() {
        Link link = Link.of("edit", "/api/vms/1");
        assertEquals("edit", link.getRel());
        assertEquals("/api/vms/1", link.getHref());
    }

    @Test
    public void ref_JsonOmitsNullId() throws Exception {
        Mapper mapper = new Mapper();
        Ref ref = new Ref();
        ref.setHref("/api/vms/1");
        String json = mapper.toJson(ref);
        assertTrue(json.contains("\"href\":\"/api/vms/1\""));
        assertFalse(json.contains("\"id\""));
    }

    @Test
    public void link_JsonContainsRelAndHref() throws Exception {
        Mapper mapper = new Mapper();
        Link link = Link.of("delete", "/api/disks/7");
        String json = mapper.toJson(link);
        assertTrue(json.contains("\"rel\":\"delete\""));
        assertTrue(json.contains("\"href\":\"/api/disks/7\""));
    }

    @Test
    public void baseDto_GettersSetters() {
        Ref ref = new Ref();
        assertNull(ref.getHref());
        assertNull(ref.getId());
        ref.setHref("/test");
        ref.setId("id-1");
        assertEquals("/test", ref.getHref());
        assertEquals("id-1", ref.getId());
    }

    @Test
    public void vmOf_SetsHrefAndId() {
        Vm vm = Vm.of("/ovirt-engine/api/vms/1", "1");
        assertNotNull(vm);
        assertEquals("/ovirt-engine/api/vms/1", vm.getHref());
        assertEquals("1", vm.getId());
    }

    @Test
    public void hostOf_SetsHrefAndId() {
        Host host = Host.of("/ovirt-engine/api/hosts/h1", "h1");
        assertNotNull(host);
        assertEquals("/ovirt-engine/api/hosts/h1", host.getHref());
        assertEquals("h1", host.getId());
    }
}
