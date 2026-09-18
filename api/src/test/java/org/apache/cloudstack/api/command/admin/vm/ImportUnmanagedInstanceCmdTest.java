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

package org.apache.cloudstack.api.command.admin.vm;

import java.util.LinkedHashMap;
import java.util.Map;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.junit.MockitoJUnitRunner;
import org.springframework.test.util.ReflectionTestUtils;

import com.cloud.exception.InvalidParameterValueException;
import com.cloud.vm.VmDetailConstants;

@RunWith(MockitoJUnitRunner.class)
public class ImportUnmanagedInstanceCmdTest {

    private ImportUnmanagedInstanceCmd buildCmd(Map<?, ?> nicMacAddressList) {
        ImportUnmanagedInstanceCmd cmd = new ImportUnmanagedInstanceCmd();
        ReflectionTestUtils.setField(cmd, "nicMacAddressList", nicMacAddressList);
        return cmd;
    }

    private Map<String, String> entry(String nic, String mac) {
        Map<String, String> e = new LinkedHashMap<>();
        if (nic != null) {
            e.put(VmDetailConstants.NIC, nic);
        }
        if (mac != null) {
            e.put(VmDetailConstants.NIC_MAC_ADDRESS, mac);
        }
        return e;
    }

    @Test
    public void testGetNicMacAddressListNullReturnsEmpty() {
        ImportUnmanagedInstanceCmd cmd = buildCmd(null);
        Map<String, String> result = cmd.getNicMacAddressList();
        Assert.assertNotNull(result);
        Assert.assertTrue(result.isEmpty());
    }

    @Test
    public void testGetNicMacAddressListEmptyMapReturnsEmpty() {
        ImportUnmanagedInstanceCmd cmd = buildCmd(new LinkedHashMap<>());
        Assert.assertTrue(cmd.getNicMacAddressList().isEmpty());
    }

    @Test
    public void testGetNicMacAddressListValidMac() {
        Map<Object, Object> outer = new LinkedHashMap<>();
        outer.put("0", entry("nic1", "AA:BB:CC:DD:EE:FF"));
        ImportUnmanagedInstanceCmd cmd = buildCmd(outer);

        Map<String, String> result = cmd.getNicMacAddressList();

        Assert.assertEquals(1, result.size());
        // standardizeMacAddress lowercases the address
        Assert.assertEquals("aa:bb:cc:dd:ee:ff", result.get("nic1"));
    }

    @Test
    public void testGetNicMacAddressListMultipleNics() {
        Map<Object, Object> outer = new LinkedHashMap<>();
        outer.put("0", entry("nic1", "aa:bb:cc:dd:ee:01"));
        outer.put("1", entry("nic2", "aa:bb:cc:dd:ee:02"));
        ImportUnmanagedInstanceCmd cmd = buildCmd(outer);

        Map<String, String> result = cmd.getNicMacAddressList();

        Assert.assertEquals(2, result.size());
        Assert.assertEquals("aa:bb:cc:dd:ee:01", result.get("nic1"));
        Assert.assertEquals("aa:bb:cc:dd:ee:02", result.get("nic2"));
    }

    @Test(expected = InvalidParameterValueException.class)
    public void testGetNicMacAddressListInvalidMacFormat() {
        Map<Object, Object> outer = new LinkedHashMap<>();
        outer.put("0", entry("nic1", "not-a-mac"));
        buildCmd(outer).getNicMacAddressList();
    }

    @Test(expected = InvalidParameterValueException.class)
    public void testGetNicMacAddressListBroadcastMacRejected() {
        // FF:FF:FF:FF:FF:FF is a broadcast (non-unicast) address
        Map<Object, Object> outer = new LinkedHashMap<>();
        outer.put("0", entry("nic1", "FF:FF:FF:FF:FF:FF"));
        buildCmd(outer).getNicMacAddressList();
    }

    @Test(expected = InvalidParameterValueException.class)
    public void testGetNicMacAddressListMulticastMacRejected() {
        // Bit 0 of first octet set → multicast, not unicast
        Map<Object, Object> outer = new LinkedHashMap<>();
        outer.put("0", entry("nic1", "01:bb:cc:dd:ee:ff"));
        buildCmd(outer).getNicMacAddressList();
    }

    @Test(expected = InvalidParameterValueException.class)
    public void testGetNicMacAddressListEmptyNicIdRejected() {
        Map<Object, Object> outer = new LinkedHashMap<>();
        outer.put("0", entry("", "aa:bb:cc:dd:ee:ff"));
        buildCmd(outer).getNicMacAddressList();
    }

    @Test(expected = InvalidParameterValueException.class)
    public void testGetNicMacAddressListEmptyMacRejected() {
        Map<Object, Object> outer = new LinkedHashMap<>();
        outer.put("0", entry("nic1", ""));
        buildCmd(outer).getNicMacAddressList();
    }

    @Test(expected = InvalidParameterValueException.class)
    public void testGetNicMacAddressListNullMacRejected() {
        Map<Object, Object> outer = new LinkedHashMap<>();
        // entry without mac key
        Map<String, String> e = new LinkedHashMap<>();
        e.put(VmDetailConstants.NIC, "nic1");
        outer.put("0", e);
        buildCmd(outer).getNicMacAddressList();
    }
}
