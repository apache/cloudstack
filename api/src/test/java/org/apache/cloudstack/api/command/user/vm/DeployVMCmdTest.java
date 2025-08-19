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
package org.apache.cloudstack.api.command.user.vm;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.cloudstack.api.ApiConstants;
import org.apache.cloudstack.api.ApiConstants.BootMode;
import org.apache.cloudstack.api.ApiConstants.BootType;
import org.apache.cloudstack.api.ApiConstants.IoDriverPolicy;
import org.apache.cloudstack.vm.lease.VMLeaseManager;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Spy;
import org.mockito.junit.MockitoJUnitRunner;
import org.springframework.test.util.ReflectionTestUtils;

import com.cloud.exception.InvalidParameterValueException;
import com.cloud.network.NetworkService;
import com.cloud.utils.db.EntityManager;
import com.cloud.vm.VmDetailConstants;
import com.cloud.network.Network;
import com.cloud.template.VirtualMachineTemplate;
import com.cloud.offering.DiskOffering;
import com.cloud.network.Network.IpAddresses;
import com.cloud.vm.VmDiskInfo;

@RunWith(MockitoJUnitRunner.class)
public class DeployVMCmdTest {

    @Spy
    private DeployVMCmd cmd = new DeployVMCmd();

    @Test
    public void testGetBootType_ValidUEFI() {
        ReflectionTestUtils.setField(cmd, "bootType", "UEFI");

        BootType result = cmd.getBootType();

        assertEquals(BootType.UEFI, result);
    }

    @Test
    public void testGetBootTypeValidBIOS() {
        ReflectionTestUtils.setField(cmd, "bootType", "BIOS");

        BootType result = cmd.getBootType();

        assertEquals(BootType.BIOS, result);
    }

    @Test
    public void testGetBootTypeInvalidValue() {
        ReflectionTestUtils.setField(cmd, "bootType", "INVALID");

        InvalidParameterValueException thrownException = assertThrows(InvalidParameterValueException.class, () -> {
            cmd.getBootType();
        });
        assertTrue(thrownException.getMessage().contains("Invalid bootType INVALID"));
    }

    @Test
    public void testGetBootTypeNullValue() {
        ReflectionTestUtils.setField(cmd, "bootType", null);

        BootType result = cmd.getBootType();

        assertNull(result);
    }

    @Test
    public void testGetBootModeValidSecure() {
        ReflectionTestUtils.setField(cmd, "bootMode", "SECURE");
        ReflectionTestUtils.setField(cmd, "bootType", "UEFI");

        BootMode result = cmd.getBootMode();

        assertEquals(BootMode.SECURE, result);
    }

    @Test
    public void testGetBootModeValidLegacy() {
        ReflectionTestUtils.setField(cmd, "bootMode", "LEGACY");
        ReflectionTestUtils.setField(cmd, "bootType", "UEFI");

        BootMode result = cmd.getBootMode();

        assertEquals(BootMode.LEGACY, result);
    }

    @Test
    public void testGetBootModeInvalidValue() {
        ReflectionTestUtils.setField(cmd, "bootMode", "INVALID");
        ReflectionTestUtils.setField(cmd, "bootType", "UEFI");

        InvalidParameterValueException thrownException = assertThrows(InvalidParameterValueException.class, () -> {
            cmd.getBootMode();
        });
        assertTrue(thrownException.getMessage().contains("Invalid bootmode: INVALID specified for VM: null. Valid values are: [LEGACY, SECURE]"));
    }

    @Test
    public void testGetBootModeUEFIWithoutBootMode() {
        ReflectionTestUtils.setField(cmd, "bootMode", null);
        ReflectionTestUtils.setField(cmd, "bootType", "UEFI");

        InvalidParameterValueException thrownException = assertThrows(InvalidParameterValueException.class, () -> {
            cmd.getBootMode();
        });
        assertTrue(thrownException.getMessage().contains("bootmode must be specified for the VM with boot type: UEFI. Valid values are: [LEGACY, SECURE]"));
    }

    @Test
    public void testGetDetails() {
        ReflectionTestUtils.setField(cmd, "bootType", "UEFI");
        ReflectionTestUtils.setField(cmd, "bootMode", "SECURE");
        ReflectionTestUtils.setField(cmd, "rootdisksize", 100L);
        ReflectionTestUtils.setField(cmd, "ioDriverPolicy", "native");
        ReflectionTestUtils.setField(cmd, "iothreadsEnabled", true);
        ReflectionTestUtils.setField(cmd, "nicMultiqueueNumber", null);
        ReflectionTestUtils.setField(cmd, "nicPackedVirtQueues", null);
        ReflectionTestUtils.setField(cmd, "details", new HashMap<>());

        Map<String, String> result = cmd.getDetails();

        assertEquals("SECURE", result.get("UEFI"));
        assertEquals("100", result.get(VmDetailConstants.ROOT_DISK_SIZE));
        assertEquals("native", result.get(VmDetailConstants.IO_POLICY));
        assertEquals("true", result.get(VmDetailConstants.IOTHREADS));
    }

    @Test
    public void testGetLeaseExpiryActionValidStop() {
        ReflectionTestUtils.setField(cmd, "leaseExpiryAction", "STOP");

        VMLeaseManager.ExpiryAction result = cmd.getLeaseExpiryAction();

        assertEquals(VMLeaseManager.ExpiryAction.STOP, result);
    }

    @Test
    public void testGetLeaseExpiryActionValidDestroy() {
        ReflectionTestUtils.setField(cmd, "leaseExpiryAction", "DESTROY");

        VMLeaseManager.ExpiryAction result = cmd.getLeaseExpiryAction();

        assertEquals(VMLeaseManager.ExpiryAction.DESTROY, result);
    }

    @Test
    public void testGetLeaseExpiryActionInvalidValue() {
        ReflectionTestUtils.setField(cmd, "leaseExpiryAction", "INVALID");

        InvalidParameterValueException thrownException = assertThrows(InvalidParameterValueException.class, () -> {
            cmd.getLeaseExpiryAction();
        });
        assertTrue(thrownException.getMessage().contains("Invalid value configured for leaseexpiryaction"));
    }

    @Test
    public void testGetLeaseExpiryActionNullValue() {
        ReflectionTestUtils.setField(cmd, "leaseExpiryAction", null);

        VMLeaseManager.ExpiryAction result = cmd.getLeaseExpiryAction();

        assertNull(result);
    }

    @Test
    public void testGetIoDriverPolicyValidThrottle() {
        ReflectionTestUtils.setField(cmd, "ioDriverPolicy", "native");

        IoDriverPolicy result = cmd.getIoDriverPolicy();

        assertEquals(IoDriverPolicy.valueOf("NATIVE"), result);
    }

    @Test
    public void testGetIoDriverPolicyInvalidValue() {
        ReflectionTestUtils.setField(cmd, "ioDriverPolicy", "INVALID");

        InvalidParameterValueException thrownException = assertThrows(InvalidParameterValueException.class, () -> {
            cmd.getIoDriverPolicy();
        });
        assertTrue(thrownException.getMessage().contains("Invalid io policy INVALID"));
    }

    @Test
    public void testGetNetworkIds() {
        List<Long> networkIds = Arrays.asList(1L, 2L, 3L);
        ReflectionTestUtils.setField(cmd, "networkIds", networkIds);
        ReflectionTestUtils.setField(cmd, "vAppNetworks", null);
        ReflectionTestUtils.setField(cmd, "ipToNetworkList", null);

        List<Long> result = cmd.getNetworkIds();

        assertEquals(networkIds, result);
    }

    @Test
    public void testGetNetworkIdsVAppNetworks() {
        Map<String, Object> vAppNetworks = new HashMap<>();
        vAppNetworks.put("network1", new HashMap<String, String>());
        ReflectionTestUtils.setField(cmd, "vAppNetworks", vAppNetworks);
        ReflectionTestUtils.setField(cmd, "networkIds", null);
        ReflectionTestUtils.setField(cmd, "ipToNetworkList", null);
        ReflectionTestUtils.setField(cmd, "ipAddress", null);
        ReflectionTestUtils.setField(cmd, "ip6Address", null);

        List<Long> result = cmd.getNetworkIds();

        assertTrue(result.isEmpty());
    }

    @Test
    public void testGetNetworkIdsVAppNetworksAndNetworkIds() {
        Map<String, Object> vAppNetworks = new HashMap<>();
        vAppNetworks.put("network1", new HashMap<String, String>());
        ReflectionTestUtils.setField(cmd, "vAppNetworks", vAppNetworks);
        ReflectionTestUtils.setField(cmd, "networkIds", Arrays.asList(1L, 2L));

        InvalidParameterValueException thrownException = assertThrows(InvalidParameterValueException.class, () -> {
            cmd.getNetworkIds();
        });
        assertTrue(thrownException.getMessage().contains("nicnetworklist can't be specified along with networkids"));
    }

    @Test
    public void testGetNetworkIdsIpToNetworkListAndNetworkIds() {
        Map<String, Object> ipToNetworkList = new HashMap<>();
        ipToNetworkList.put("0", new HashMap<String, String>());
        ReflectionTestUtils.setField(cmd, "ipToNetworkList", ipToNetworkList);
        ReflectionTestUtils.setField(cmd, "networkIds", Arrays.asList(1L, 2L));

        InvalidParameterValueException thrownException = assertThrows(InvalidParameterValueException.class, () -> {
            cmd.getNetworkIds();
        });
        assertTrue(thrownException.getMessage().contains("ipToNetworkMap can't be specified along with networkIds or ipAddress"));
    }

    @Test
    public void testGetIpToNetworkMap_WithNetworkIds() {
        ReflectionTestUtils.setField(cmd, "networkIds", Arrays.asList(1L, 2L));
        ReflectionTestUtils.setField(cmd, "ipToNetworkList", new HashMap<>());

        InvalidParameterValueException thrownException = assertThrows(InvalidParameterValueException.class, () -> {
            cmd.getIpToNetworkMap();
        });
        assertTrue(thrownException.getMessage().contains("NetworkIds and ipAddress can't be specified along with ipToNetworkMap parameter"));
    }

    @Test
    public void testGetIpToNetworkMap_WithIpAddress() {
        ReflectionTestUtils.setField(cmd, "ipAddress", "192.168.1.1");
        ReflectionTestUtils.setField(cmd, "ipToNetworkList", new HashMap<>());

        InvalidParameterValueException thrownException = assertThrows(InvalidParameterValueException.class, () -> {
            cmd.getIpToNetworkMap();
        });
        assertTrue(thrownException.getMessage().contains("NetworkIds and ipAddress can't be specified along with ipToNetworkMap parameter"));
    }

    @Test
    public void testGetIpToNetworkMap_WithEmptyIpToNetworkList() {
        ReflectionTestUtils.setField(cmd, "networkIds", null);
        ReflectionTestUtils.setField(cmd, "ipAddress", null);
        ReflectionTestUtils.setField(cmd, "ipToNetworkList", new HashMap<>());

        Map<Long, IpAddresses> result = cmd.getIpToNetworkMap();

        assertNull(result);
    }

    @Test
    public void testGetIpToNetworkMap_WithNullIpToNetworkList() {
        ReflectionTestUtils.setField(cmd, "networkIds", null);
        ReflectionTestUtils.setField(cmd, "ipAddress", null);
        ReflectionTestUtils.setField(cmd, "ipToNetworkList", null);

        Map<Long, IpAddresses> result = cmd.getIpToNetworkMap();

        assertNull(result);
    }

    @Test
    public void testGetDataDiskInfoList() {
        Map<String, Object> dataDisksDetails = new HashMap<>();
        Map<String, String> dataDisk = new HashMap<>();
        dataDisk.put(ApiConstants.DISK_OFFERING_ID, "offering-uuid");
        dataDisk.put(ApiConstants.DEVICE_ID, "0");
        dataDisk.put(ApiConstants.MIN_IOPS, "1000");
        dataDisk.put(ApiConstants.MAX_IOPS, "2000");
        dataDisksDetails.put("0", dataDisk);

        ReflectionTestUtils.setField(cmd, "dataDisksDetails", dataDisksDetails);

        EntityManager entityMgr = mock(EntityManager.class);
        ReflectionTestUtils.setField(cmd, "_entityMgr", entityMgr);
        DiskOffering diskOffering = mock(DiskOffering.class);
        when(diskOffering.getDiskSize()).thenReturn(1024 * 1024 * 1024L);
        when(diskOffering.isCustomizedIops()).thenReturn(true);
        when(entityMgr.findByUuid(DiskOffering.class, "offering-uuid")).thenReturn(diskOffering);

        List<VmDiskInfo> result = cmd.getDataDiskInfoList();

        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals(diskOffering, result.get(0).getDiskOffering());
        assertEquals(1L, result.get(0).getSize().longValue());
        assertEquals(1000L, result.get(0).getMinIops().longValue());
        assertEquals(2000L, result.get(0).getMaxIops().longValue());
    }

    @Test
    public void testGetIpAddressesFromIpMap() {
        Map<String, Object> ipToNetworkList = new HashMap<>();
        Map<String, String> ipMap = new HashMap<>();
        ipMap.put("ip", "192.168.1.100");
        ipMap.put("mac", "00:11:22:33:44:55");
        ipMap.put("networkid", "1");
        ipToNetworkList.put("0", ipMap);

        ReflectionTestUtils.setField(cmd, "ipToNetworkList", ipToNetworkList);
        ReflectionTestUtils.setField(cmd, "networkIds", null);
        ReflectionTestUtils.setField(cmd, "ipAddress", null);

        Network mockNetwork = mock(Network.class);
        NetworkService networkServiceMock = mock(NetworkService.class);
        ReflectionTestUtils.setField(cmd, "_networkService", networkServiceMock);

        Map<Long, IpAddresses> result = cmd.getIpToNetworkMap();

        assertNotNull(result);
        assertTrue(result.containsKey(1L));
        assertEquals(result.get(1L).getIp4Address(), "192.168.1.100");
        assertEquals(result.get(1L).getMacAddress(), "00:11:22:33:44:55");
    }

    @Test
    public void testGetIpAddressesFromIpMapInvalidMac() {
        Map<String, Object> ipToNetworkList = new HashMap<>();
        Map<String, String> ipMap = new HashMap<>();
        ipMap.put("ip", "192.168.1.100");
        ipMap.put("mac", "invalid-mac");
        ipMap.put("networkid", "1");
        ipToNetworkList.put("0", ipMap);

        ReflectionTestUtils.setField(cmd, "ipToNetworkList", ipToNetworkList);
        ReflectionTestUtils.setField(cmd, "networkIds", null);
        ReflectionTestUtils.setField(cmd, "ipAddress", null);

        Network mockNetwork = mock(Network.class);
        NetworkService networkServiceMock = mock(NetworkService.class);
        ReflectionTestUtils.setField(cmd, "_networkService", networkServiceMock);

        InvalidParameterValueException thrownException = assertThrows(InvalidParameterValueException.class, () -> {
            cmd.getIpToNetworkMap();
        });
        assertTrue(thrownException.getMessage().contains("Mac address is not valid"));
    }

    @Test
    public void testGetDhcpOptionsMap() {
        Map<String, Object> dhcpOptionsNetworkList = new HashMap<>();
        Map<String, String> dhcpOptions = new HashMap<>();
        dhcpOptions.put("networkid", "network-1");
        dhcpOptions.put("dhcp:114", "url-value");
        dhcpOptions.put("dhcp:66", "www.test.com");
        dhcpOptionsNetworkList.put("0", dhcpOptions);

        ReflectionTestUtils.setField(cmd, "dhcpOptionsNetworkList", dhcpOptionsNetworkList);

        Map<String, Map<Integer, String>> result = cmd.getDhcpOptionsMap();

        assertNotNull(result);
        assertTrue(result.containsKey("network-1"));
        Map<Integer, String> networkOptions = result.get("network-1");
        assertEquals("url-value", networkOptions.get(114));
        assertEquals("www.test.com", networkOptions.get(66));
    }

    @Test
    public void testGetDhcpOptionsMap_WithMissingNetworkId() {
        Map<String, Object> dhcpOptionsNetworkList = new HashMap<>();
        Map<String, String> dhcpOptions = new HashMap<>();
        dhcpOptions.put("dhcp:114", "url-value");
        dhcpOptionsNetworkList.put("0", dhcpOptions);

        ReflectionTestUtils.setField(cmd, "dhcpOptionsNetworkList", dhcpOptionsNetworkList);

        IllegalArgumentException thrownException = assertThrows(IllegalArgumentException.class, () -> {
            cmd.getDhcpOptionsMap();
        });
        assertTrue(thrownException.getMessage().contains("No networkid specified when providing extra dhcp options"));
    }

    @Test
    public void testGetDataDiskTemplateToDiskOfferingMap() {
        ReflectionTestUtils.setField(cmd, "diskOfferingId", null);

        Map<String, Object> dataDiskTemplateToDiskOfferingList = new HashMap<>();
        Map<String, String> dataDiskTemplate = new HashMap<>();
        dataDiskTemplate.put("datadisktemplateid", "template-uuid");
        dataDiskTemplate.put("diskofferingid", "offering-uuid");
        dataDiskTemplateToDiskOfferingList.put("0", dataDiskTemplate);

        ReflectionTestUtils.setField(cmd, "dataDiskTemplateToDiskOfferingList", dataDiskTemplateToDiskOfferingList);

        VirtualMachineTemplate mockTemplate = mock(VirtualMachineTemplate.class);
        when(mockTemplate.getId()).thenReturn(1L);

        DiskOffering mockOffering = mock(DiskOffering.class);

        EntityManager entityMgr = mock(EntityManager.class);
        ReflectionTestUtils.setField(cmd, "_entityMgr", entityMgr);
        when(entityMgr.findByUuid(VirtualMachineTemplate.class, "template-uuid")).thenReturn(mockTemplate);
        when(entityMgr.findByUuid(DiskOffering.class, "offering-uuid")).thenReturn(mockOffering);

        Map<Long, DiskOffering> result = cmd.getDataDiskTemplateToDiskOfferingMap();

        assertNotNull(result);
        assertEquals(mockOffering, result.get(1L));
    }

    @Test
    public void testGetDataDiskTemplateToDiskOfferingMapWithDiskOfferingId() {
        ReflectionTestUtils.setField(cmd, "diskOfferingId", 1L);
        ReflectionTestUtils.setField(cmd, "dataDiskTemplateToDiskOfferingList", new HashMap<>());

        InvalidParameterValueException thrownException = assertThrows(InvalidParameterValueException.class, () -> {
            cmd.getDataDiskTemplateToDiskOfferingMap();
        });
        assertTrue(thrownException.getMessage().contains("diskofferingid parameter can't be specified along with datadisktemplatetodiskofferinglist parameter"));
    }

    @Test
    public void testGetDataDiskTemplateToDiskOfferingMapInvalidTemplateId() {
        ReflectionTestUtils.setField(cmd, "diskOfferingId", null);

        Map<String, Object> dataDiskTemplateToDiskOfferingList = new HashMap<>();
        Map<String, String> dataDiskTemplate = new HashMap<>();
        dataDiskTemplate.put("datadisktemplateid", "invalid-template");
        dataDiskTemplate.put("diskofferingid", "offering-uuid");
        dataDiskTemplateToDiskOfferingList.put("0", dataDiskTemplate);

        ReflectionTestUtils.setField(cmd, "dataDiskTemplateToDiskOfferingList", dataDiskTemplateToDiskOfferingList);

        EntityManager entityMgr = mock(EntityManager.class);
        ReflectionTestUtils.setField(cmd, "_entityMgr", entityMgr);
        when(entityMgr.findByUuid(VirtualMachineTemplate.class, "invalid-template")).thenReturn(null);
        when(entityMgr.findById(VirtualMachineTemplate.class, "invalid-template")).thenReturn(null);

        InvalidParameterValueException thrownException = assertThrows(InvalidParameterValueException.class, () -> {
            cmd.getDataDiskTemplateToDiskOfferingMap();
        });
        assertTrue(thrownException.getMessage().contains("Unable to translate and find entity with datadisktemplateid"));
    }
}
