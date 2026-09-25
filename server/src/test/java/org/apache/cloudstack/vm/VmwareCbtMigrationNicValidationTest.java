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
package org.apache.cloudstack.vm;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.cloudstack.api.ServerApiException;
import org.junit.Assert;
import org.junit.Test;

import com.cloud.network.Network;

public class VmwareCbtMigrationNicValidationTest {

    private final VmwareCbtMigrationManagerImpl manager = new VmwareCbtMigrationManagerImpl();

    private static VmwareCbtPreflightNicInfo nic(String id, String adapterType, Integer vlan) {
        return new VmwareCbtPreflightNicInfo(id, adapterType, "00:50:56:00:00:01", vlan);
    }

    private static Map<String, Long> networkMap(String... nicIds) {
        Map<String, Long> map = new HashMap<>();
        long networkId = 100L;
        for (String nicId : nicIds) {
            map.put(nicId, networkId++);
        }
        return map;
    }

    @Test
    public void testAcceptsFullyMappedNics() {
        List<VmwareCbtPreflightNicInfo> nics = Arrays.asList(
                nic("Network adapter 1", "Vmxnet3", null),
                nic("Network adapter 2", "Vmxnet3", null));

        manager.validateNicMappingsForStart("source-vm", nics,
                networkMap("Network adapter 1", "Network adapter 2"), null);
    }

    @Test
    public void testAcceptsNoSourceNics() {
        manager.validateNicMappingsForStart("source-vm", Collections.emptyList(), null, null);
    }

    @Test
    public void testRejectsUnmappedNicWithoutVlan() {
        List<VmwareCbtPreflightNicInfo> nics = Arrays.asList(
                nic("Network adapter 1", "Vmxnet3", null),
                nic("Network adapter 2", "Vmxnet3", null));

        try {
            manager.validateNicMappingsForStart("source-vm", nics, networkMap("Network adapter 1"), null);
            Assert.fail("Expected NIC mapping validation to fail");
        } catch (ServerApiException e) {
            Assert.assertTrue(e.getDescription().contains("Network adapter 2"));
            Assert.assertTrue(e.getDescription().contains("no network mapping"));
        }
    }

    @Test
    public void testAcceptsUnmappedNicWithVlanForAutoMatch() {
        List<VmwareCbtPreflightNicInfo> nics = Arrays.asList(
                nic("Network adapter 1", "Vmxnet3", null),
                nic("Network adapter 2", "Vmxnet3", 120));

        manager.validateNicMappingsForStart("source-vm", nics, networkMap("Network adapter 1"), null);
    }

    @Test
    public void testRejectsAllNicsUnmappedWithoutVlan() {
        List<VmwareCbtPreflightNicInfo> nics = Collections.singletonList(
                nic("Network adapter 1", "Vmxnet3", 0));

        try {
            manager.validateNicMappingsForStart("source-vm", nics, null, null);
            Assert.fail("Expected NIC mapping validation to fail");
        } catch (ServerApiException e) {
            Assert.assertTrue(e.getDescription().contains("Network adapter 1"));
        }
    }

    @Test
    public void testRejectsNetworkMappingForUnknownNic() {
        List<VmwareCbtPreflightNicInfo> nics = Collections.singletonList(
                nic("Network adapter 1", "Vmxnet3", null));

        try {
            manager.validateNicMappingsForStart("source-vm", nics,
                    networkMap("Network adapter 1", "Network adapter 9"), null);
            Assert.fail("Expected NIC mapping validation to fail");
        } catch (ServerApiException e) {
            Assert.assertTrue(e.getDescription().contains("Network adapter 9"));
            Assert.assertTrue(e.getDescription().contains("has no NIC(s)"));
        }
    }

    @Test
    public void testRejectsIpAddressMappingForUnknownNic() {
        List<VmwareCbtPreflightNicInfo> nics = Collections.singletonList(
                nic("Network adapter 1", "Vmxnet3", null));
        Map<String, Network.IpAddresses> ipMap = new HashMap<>();
        ipMap.put("Network adapter 7", new Network.IpAddresses("10.1.1.10", null));

        try {
            manager.validateNicMappingsForStart("source-vm", nics, networkMap("Network adapter 1"), ipMap);
            Assert.fail("Expected NIC mapping validation to fail");
        } catch (ServerApiException e) {
            Assert.assertTrue(e.getDescription().contains("Network adapter 7"));
        }
    }

    @Test
    public void testRejectsMixedAdapterTypes() {
        List<VmwareCbtPreflightNicInfo> nics = Arrays.asList(
                nic("Network adapter 1", "Vmxnet3", null),
                nic("Network adapter 2", "E1000", null));

        try {
            manager.validateNicMappingsForStart("source-vm", nics,
                    networkMap("Network adapter 1", "Network adapter 2"), null);
            Assert.fail("Expected NIC mapping validation to fail");
        } catch (ServerApiException e) {
            Assert.assertTrue(e.getDescription().contains("different types"));
        }
    }
}
