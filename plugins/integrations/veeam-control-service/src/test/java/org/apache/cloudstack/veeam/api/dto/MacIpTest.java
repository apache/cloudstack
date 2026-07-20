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
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.assertFalse;

import org.apache.cloudstack.veeam.utils.Mapper;
import org.junit.Test;

public class MacIpTest {
    @Test
    public void mac_GettersSetters() {
        Mac mac = new Mac();
        mac.setAddress("02:00:00:00:00:01");
        assertEquals("02:00:00:00:00:01", mac.getAddress());
    }

    @Test
    public void mac_JsonSerializes() throws Exception {
        Mapper mapper = new Mapper();
        Mac mac = new Mac();
        mac.setAddress("aa:bb:cc:dd:ee:ff");
        String json = mapper.toJson(mac);
        assertTrue(json.contains("\"address\":\"aa:bb:cc:dd:ee:ff\""));
    }

    @Test
    public void ip_GettersSetters() {
        Ip ip = new Ip();
        ip.setAddress("192.168.1.1");
        ip.setGateway("192.168.1.254");
        ip.setNetmask("255.255.255.0");
        ip.setVersion("v4");
        assertEquals("192.168.1.1", ip.getAddress());
        assertEquals("192.168.1.254", ip.getGateway());
        assertEquals("255.255.255.0", ip.getNetmask());
        assertEquals("v4", ip.getVersion());
    }

    @Test
    public void ip_JsonOmitsNullGateway() throws Exception {
        Mapper mapper = new Mapper();
        Ip ip = new Ip();
        ip.setAddress("10.0.0.1");
        ip.setVersion("v4");
        String json = mapper.toJson(ip);
        assertTrue(json.contains("\"address\":\"10.0.0.1\""));
        assertFalse(json.contains("\"gateway\""));
        assertFalse(json.contains("\"netmask\""));
    }
}
