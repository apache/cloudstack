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
import static org.junit.Assert.assertNotNull;

import java.util.Collections;

import org.apache.cloudstack.veeam.utils.Mapper;
import org.junit.Test;

public class VnicProfileReportedDeviceTest {
    // ---- VnicProfile -------------------------------------------------------
    @Test
    public void vnicProfile_GettersSetters() {
        VnicProfile profile = new VnicProfile();
        profile.setName("default");
        profile.setDescription("Default vNIC profile");
        Ref network = Ref.of("/api/networks/net1", "net1");
        Ref dc = Ref.of("/api/datacenters/dc1", "dc1");
        profile.setNetwork(network);
        profile.setDataCenter(dc);
        assertEquals("default", profile.getName());
        assertEquals("Default vNIC profile", profile.getDescription());
        assertEquals("net1", profile.getNetwork().getId());
        assertEquals("dc1", profile.getDataCenter().getId());
    }

    @Test
    public void vnicProfileJson_ContainsName() throws Exception {
        Mapper mapper = new Mapper();
        VnicProfile profile = new VnicProfile();
        profile.setName("my-profile");
        String json = mapper.toJson(profile);
        assertTrue(json.contains("\"name\":\"my-profile\""));
    }

    // ---- ReportedDevice ----------------------------------------------------
    @Test
    public void reportedDevice_GettersSetters() {
        ReportedDevice device = new ReportedDevice();
        device.setName("eth0");
        device.setType("bridge");
        device.setDescription("Primary NIC");
        Mac mac = new Mac();
        mac.setAddress("aa:bb:cc:dd:ee:01");
        device.setMac(mac);
        NamedList<Ip> ips = NamedList.of("ip", Collections.singletonList(new Ip()));
        device.setIps(ips);
        assertEquals("eth0", device.getName());
        assertEquals("bridge", device.getType());
        assertEquals("Primary NIC", device.getDescription());
        assertEquals("aa:bb:cc:dd:ee:01", device.getMac().getAddress());
        assertNotNull(device.getIps());
    }

    @Test
    public void reportedDeviceJson_ContainsNameAndType() throws Exception {
        Mapper mapper = new Mapper();
        ReportedDevice device = new ReportedDevice();
        device.setName("ens3");
        device.setType("ethernet");
        String json = mapper.toJson(device);
        assertTrue(json.contains("\"name\":\"ens3\""));
        assertTrue(json.contains("\"type\":\"ethernet\""));
    }
}
