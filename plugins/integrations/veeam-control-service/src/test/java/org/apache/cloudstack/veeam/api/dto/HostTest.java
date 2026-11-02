// Licensed to the Apache Software Foundation (ASF) under one
// or more contributor license agreements. See the NOTICE file
// distributed with this work for additional information
// regarding copyright ownership. The ASF licenses this file
// to you under the Apache License, Version 2.0 (the
// "License"); you may not use this file except in compliance
// with the License. You may obtain a copy of the License at
//
//   http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing,
// software distributed under the License is distributed on an
// "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
// KIND, either express or implied. See the License for the
// specific language governing permissions and limitations
// under the License.
package org.apache.cloudstack.veeam.api.dto;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import org.apache.cloudstack.veeam.utils.Mapper;
import org.junit.Test;

public class HostTest {
    @Test
    public void of_SetsHrefAndId() {
        Host host = Host.of("/api/hosts/h1", "h1");
        assertNotNull(host);
        assertEquals("/api/hosts/h1", host.getHref());
        assertEquals("h1", host.getId());
    }

    @Test
    public void gettersSetters_CoreFields() {
        Host host = new Host();
        host.setAddress("192.168.1.10");
        host.setStatus("up");
        host.setName("kvm-host-01");
        host.setPort("54321");
        assertEquals("192.168.1.10", host.getAddress());
        assertEquals("up", host.getStatus());
        assertEquals("kvm-host-01", host.getName());
        assertEquals("54321", host.getPort());
    }

    @Test
    public void hardwareInformation_GettersSetters() {
        Host.HardwareInformation hw = new Host.HardwareInformation();
        hw.setManufacturer("Dell");
        hw.setProductName("PowerEdge R740");
        hw.setSerialNumber("SN12345");
        hw.setUuid("uuid-001");
        hw.setVersion("1.0");
        assertEquals("Dell", hw.getManufacturer());
        assertEquals("PowerEdge R740", hw.getProductName());
        assertEquals("SN12345", hw.getSerialNumber());
        assertEquals("uuid-001", hw.getUuid());
        assertEquals("1.0", hw.getVersion());
    }

    @Test
    public void hostJson_ContainsAddressAndStatus() throws Exception {
        Mapper mapper = new Mapper();
        Host host = new Host();
        host.setAddress("10.0.0.1");
        host.setStatus("up");
        String json = mapper.toJson(host);
        assertTrue(json.contains("\"address\":\"10.0.0.1\""));
        assertTrue(json.contains("\"status\":\"up\""));
    }
}
