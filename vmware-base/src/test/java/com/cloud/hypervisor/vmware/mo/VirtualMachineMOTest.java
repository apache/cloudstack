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
package com.cloud.hypervisor.vmware.mo;


import com.cloud.hypervisor.vmware.util.VmwareClient;
import com.cloud.hypervisor.vmware.util.VmwareContext;
import com.cloud.hypervisor.vmware.util.VmwareHelper;
import com.vmware.vim25.VirtualCdrom;
import com.vmware.vim25.VirtualDisk;
import com.vmware.vim25.VirtualE1000;
import com.vmware.vim25.VirtualIDEController;
import com.vmware.vim25.VirtualLsiLogicController;
import com.vmware.vim25.VirtualNVMEController;
import org.apache.cloudstack.storage.DiskControllerMappingVO;
import com.cloud.utils.Pair;
import com.cloud.utils.exception.CloudRuntimeException;
import com.vmware.vim25.ManagedObjectReference;
import com.vmware.vim25.VirtualController;
import com.vmware.vim25.VirtualDevice;
import com.vmware.vim25.VirtualLsiLogicSASController;
import com.vmware.vim25.VirtualSCSIController;
import com.vmware.vim25.VirtualSCSISharing;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.mockito.Spy;
import org.mockito.junit.MockitoJUnitRunner;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import static org.junit.Assert.fail;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class VirtualMachineMOTest {

    @Mock
    VmwareContext context;
    @Mock
    VmwareClient client;
    @Mock
    ManagedObjectReference mor;
    @Mock
    VirtualController virtualControllerMock;
    @Spy
    VirtualMachineMO vmMoSpy;

    VirtualMachineMO vmMo;

    AutoCloseable closeable;

    private List<VirtualDevice> getVirtualScSiDeviceList(Class<?> cls) {

        List<VirtualDevice> deviceList = new ArrayList<>();
        try {

            VirtualSCSIController scsiController = (VirtualSCSIController)cls.getDeclaredConstructor().newInstance();
            scsiController.setSharedBus(VirtualSCSISharing.NO_SHARING);
            scsiController.setBusNumber(0);
            scsiController.setKey(1);
            deviceList.add(scsiController);
        }
        catch (Exception ex) {

        }
        return deviceList;
    }


    @Before
    public void setup() {
        closeable = MockitoAnnotations.openMocks(this);
        vmMo = new VirtualMachineMO(context, mor);
        when(context.getVimClient()).thenReturn(client);

        configureSupportedDiskControllersForTests();
        vmMoSpy._context = context;
        vmMoSpy._mor = mor;
    }

    private void configureSupportedDiskControllersForTests() {
        DiskControllerMappingVO osdefaultMapping = new DiskControllerMappingVO();
        osdefaultMapping.setName("osdefault");
        osdefaultMapping.setControllerReference("osdefault");

        DiskControllerMappingVO ideMapping = new DiskControllerMappingVO();
        ideMapping.setName("ide");
        ideMapping.setControllerReference(VirtualIDEController.class.getName());
        ideMapping.setBusName("ide");
        ideMapping.setMaxDeviceCount(2);
        ideMapping.setMaxControllerCount(2);

        DiskControllerMappingVO lsilogicMapping = new DiskControllerMappingVO();
        lsilogicMapping.setName("lsilogic");
        lsilogicMapping.setControllerReference(VirtualLsiLogicController.class.getName());
        lsilogicMapping.setBusName("scsi");
        lsilogicMapping.setMaxDeviceCount(16);
        lsilogicMapping.setMaxControllerCount(4);

        DiskControllerMappingVO nvmeMapping = new DiskControllerMappingVO();
        nvmeMapping.setName("nvme");
        nvmeMapping.setControllerReference(VirtualNVMEController.class.getName());
        nvmeMapping.setBusName("nvme");
        nvmeMapping.setMaxDeviceCount(15);
        nvmeMapping.setMaxControllerCount(4);
        nvmeMapping.setMinHardwareVersion("13");

        VmwareHelper.setSupportedDiskControllers(List.of(osdefaultMapping, ideMapping, lsilogicMapping, nvmeMapping));
    }

    @BeforeClass
    public static void setUpBeforeClass() throws Exception {
    }

    @AfterClass
    public static void tearDownAfterClass() throws Exception {
    }

    @After
    public void tearDown() throws Exception {
        closeable.close();
    }

    @Test
    public void testEnsureScsiDeviceController() {
        try {
            when(client.getDynamicProperty(any(ManagedObjectReference.class), any(String.class))).thenReturn(getVirtualScSiDeviceList(VirtualLsiLogicSASController.class));
            vmMo.ensureScsiDeviceController();
        }
        catch (Exception e) {
            fail("Received exception when success expected: " + e.getMessage());
        }
    }

    @Test
    public void testGetVmxFormattedVirtualHardwareVersionOneDigit() {
        String vmxHwVersion = VirtualMachineMO.getVmxFormattedVirtualHardwareVersion(8);
        Assert.assertEquals("vmx-08", vmxHwVersion);
    }

    @Test
    public void testGetVmxFormattedVirtualHardwareVersionTwoDigits() {
        String vmxHwVersion = VirtualMachineMO.getVmxFormattedVirtualHardwareVersion(11);
        Assert.assertEquals("vmx-11", vmxHwVersion);
    }

    @Test(expected = CloudRuntimeException.class)
    public void testGetVmxFormattedVirtualHardwareVersionInvalid() {
        VirtualMachineMO.getVmxFormattedVirtualHardwareVersion(-1);
    }

    @Test
    public void getDeviceBusNameTestReturnsValue() throws Exception {
        List<VirtualDevice> virtualDevices = new ArrayList<>();
        for (int i = 0; i < 4; i++) {
            VirtualLsiLogicController controller = new VirtualLsiLogicController();
            controller.setKey(100 + i);
            controller.setBusNumber(i);
            virtualDevices.add(controller);
        }
        VirtualDisk disk = new VirtualDisk();
        disk.setControllerKey(101);
        disk.setUnitNumber(3);

        String result = vmMoSpy.getDeviceBusName(virtualDevices, disk);

        Assert.assertEquals("scsi1:3", result);
    }

    private List<VirtualDevice> configureDevicesForTests() throws Exception {
        List<VirtualDevice> devices = new ArrayList<>();
        devices.add(new VirtualE1000());
        devices.add(new VirtualCdrom());
        devices.add(new VirtualDisk());
        devices.add(new VirtualIDEController());
        devices.add(new VirtualIDEController());
        VirtualNVMEController nvmeController = new VirtualNVMEController();
        nvmeController.setKey(111);
        nvmeController.setBusNumber(3);
        devices.add(nvmeController);
        Mockito.doReturn(devices).when(client).getDynamicProperty(mor, "config.hardware.device");
        return devices;
    }

    @Test
    public void getControllersTestReturnsValue() throws Exception {
        List<VirtualDevice> devices = configureDevicesForTests();

        List<VirtualController> result = vmMoSpy.getControllers();

        for (VirtualDevice device : devices) {
            if (!(device instanceof VirtualController)) {
                continue;
            }
            Assert.assertTrue(result.contains(device));
        }
    }

    @Test
    public void getMappingsForExistingDiskControllersTestReturnsValue() throws Exception {
        configureDevicesForTests();

        Set<DiskControllerMappingVO> result = vmMoSpy.getMappingsForExistingDiskControllers();

        DiskControllerMappingVO ideMapping = VmwareHelper.getDiskControllerMapping("ide", null);
        DiskControllerMappingVO nvmeMapping = VmwareHelper.getDiskControllerMapping("nvme", null);
        Assert.assertTrue(result.contains(ideMapping));
        Assert.assertTrue(result.contains(nvmeMapping));
    }

    @Test
    public void getAnyExistingAvailableDiskControllerTestReturnsNonIdeControllerWhenNonIdeControllerIsAvailable() throws Exception {
        DiskControllerMappingVO nvmeMapping = VmwareHelper.getDiskControllerMapping("nvme", null);
        DiskControllerMappingVO ideMapping = VmwareHelper.getDiskControllerMapping("ide", null);
        Mockito.doReturn(new Pair<>(1, 2)).when(vmMoSpy).getNextAvailableControllerKeyAndDeviceNumberForType(nvmeMapping);
        Mockito.lenient().doReturn(new Pair<>(3, 4)).when(vmMoSpy).getNextAvailableControllerKeyAndDeviceNumberForType(ideMapping);
        configureDevicesForTests();

        DiskControllerMappingVO result = vmMoSpy.getAnyExistingAvailableDiskController();


        Assert.assertEquals(nvmeMapping, result);
    }

    @Test
    public void getAnyExistingAvailableDiskControllerTestReturnsIdeControllerWhenNonIdeControllerIsNotAvailable() throws Exception {
        DiskControllerMappingVO nvmeMapping = VmwareHelper.getDiskControllerMapping("nvme", null);
        DiskControllerMappingVO ideMapping = VmwareHelper.getDiskControllerMapping("ide", null);
        Mockito.doReturn(null).when(vmMoSpy).getNextAvailableControllerKeyAndDeviceNumberForType(nvmeMapping);
        Mockito.doReturn(new Pair<>(1, 2)).when(vmMoSpy).getNextAvailableControllerKeyAndDeviceNumberForType(ideMapping);
        configureDevicesForTests();

        DiskControllerMappingVO result = vmMoSpy.getAnyExistingAvailableDiskController();

        Assert.assertEquals(ideMapping, result);
    }

    @Test(expected = CloudRuntimeException.class)
    public void getAnyExistingAvailableDiskControllerTestThrowsExceptionWhenNoDiskControllerIsAvailable() throws Exception {
        DiskControllerMappingVO nvmeMapping = VmwareHelper.getDiskControllerMapping("nvme", null);
        DiskControllerMappingVO ideMapping = VmwareHelper.getDiskControllerMapping("ide", null);
        Mockito.doReturn(null).when(vmMoSpy).getNextAvailableControllerKeyAndDeviceNumberForType(nvmeMapping);
        Mockito.doReturn(null).when(vmMoSpy).getNextAvailableControllerKeyAndDeviceNumberForType(ideMapping);
        configureDevicesForTests();

        vmMoSpy.getAnyExistingAvailableDiskController();
    }

    @Test
    public void getNextAvailableControllerKeyAndDeviceNumberForTypeTestReturnsValueWhenControllerIsAvailable() throws Exception {
        Mockito.doReturn(0).when(vmMoSpy).getNextAvailableDeviceNumberForController(Mockito.any(VirtualNVMEController.class), Mockito.any());
        configureDevicesForTests();

        DiskControllerMappingVO nvmeMapping = VmwareHelper.getDiskControllerMapping("nvme", null);
        Pair<Integer, Integer> result = vmMoSpy.getNextAvailableControllerKeyAndDeviceNumberForType(nvmeMapping);

        Assert.assertEquals(111, (int) result.first());
        Assert.assertEquals(0, (int) result.second());
    }

    @Test
    public void getNextAvailableControllerKeyAndDeviceNumberForTypeTestReturnsNullWhenNoControllerIsAvailable() throws Exception {
        Mockito.doReturn(-1).when(vmMoSpy).getNextAvailableDeviceNumberForController(Mockito.any(), Mockito.any());
        configureDevicesForTests();

        DiskControllerMappingVO nvmeMapping = VmwareHelper.getDiskControllerMapping("nvme", null);
        Pair<Integer, Integer> result = vmMoSpy.getNextAvailableControllerKeyAndDeviceNumberForType(nvmeMapping);

        Assert.assertNull(result);
    }

    private Pair<VirtualController, DiskControllerMappingVO> configureScsiControllersAndDisksForGetNextAvailableDeviceNumberForControllerTests(int numberOfDevices) throws Exception {
        List<VirtualDevice> devices = new ArrayList<>();

        devices.add(new VirtualIDEController());
        devices.add(new VirtualIDEController());

        int firstScsiKey = 1;
        VirtualLsiLogicController firstScsiController = new VirtualLsiLogicController();
        firstScsiController.setKey(firstScsiKey);
        firstScsiController.setBusNumber(0);
        VirtualLsiLogicController secondScsiController = new VirtualLsiLogicController();
        secondScsiController.setKey(firstScsiKey + 1);
        secondScsiController.setBusNumber(1);

        int currentDeviceNumber = 0;
        for (int i = 0; i < numberOfDevices; i++) {
            VirtualDevice device = i == 0 ? new VirtualCdrom() : new VirtualDisk();
            device.setControllerKey(firstScsiKey);
            device.setUnitNumber(currentDeviceNumber);
            devices.add(device);

            currentDeviceNumber++;
            if (VmwareHelper.isReservedScsiDeviceNumber(currentDeviceNumber)) {
                currentDeviceNumber++;
            }
        }

        Mockito.doReturn(devices).when(client).getDynamicProperty(mor, "config.hardware.device");

        DiskControllerMappingVO lsilogicMapping = VmwareHelper.getDiskControllerMapping("lsilogic", null);
        return new Pair<>(firstScsiController, lsilogicMapping);
    }

    @Test
    public void getNextAvailableDeviceNumberForControllerTestValueWhenDeviceNumberIsAvailable() throws Exception {
        int numberOfDevices = 14;
        Pair<VirtualController, DiskControllerMappingVO> config = configureScsiControllersAndDisksForGetNextAvailableDeviceNumberForControllerTests(numberOfDevices);

        int result = vmMoSpy.getNextAvailableDeviceNumberForController(config.first(), config.second());

        Assert.assertEquals(numberOfDevices + 1, result);
    }

    @Test
    public void getNextAvailableDeviceNumberForControllerTestValueWhenNoDeviceNumberIsAvailable() throws Exception {
        Pair<VirtualController, DiskControllerMappingVO> config = configureScsiControllersAndDisksForGetNextAvailableDeviceNumberForControllerTests(15);

        int result = vmMoSpy.getNextAvailableDeviceNumberForController(config.first(), config.second());

        Assert.assertEquals(-1, result);
    }

    @Test
    public void getNthDeviceTestReturnsDeviceWhenDeviceExists() throws Exception {
        configureDevicesForTests();

        vmMoSpy.getNthDevice(VirtualIDEController.class.getName(), 1);
    }

    @Test(expected = CloudRuntimeException.class)
    public void getNthDeviceTestThrowsExceptionWhenDeviceDoesNotExist() throws Exception {
        configureDevicesForTests();

        vmMoSpy.getNthDevice(VirtualIDEController.class.getName(), 2);
    }

    @Test(expected = CloudRuntimeException.class)
    public void validateDiskControllerIsAvailableTestThrowsExceptionWhenBusNumberIsInvalid() throws Exception {
        DiskControllerMappingVO nvmeMapping = VmwareHelper.getDiskControllerMapping("nvme", null);
        Mockito.when(virtualControllerMock.getBusNumber()).thenReturn(4);

        vmMo.validateDiskControllerIsAvailable(virtualControllerMock, nvmeMapping);
    }

    @Test(expected = CloudRuntimeException.class)
    public void validateDiskControllerIsAvailableTestThrowsExceptionWhenNoAvailableDeviceNumberExists() throws Exception {
        DiskControllerMappingVO nvmeMapping = VmwareHelper.getDiskControllerMapping("nvme", null);
        Mockito.when(virtualControllerMock.getBusNumber()).thenReturn(3);
        Mockito.doReturn(-1).when(vmMoSpy).getNextAvailableDeviceNumberForController(virtualControllerMock, nvmeMapping);

        vmMoSpy.validateDiskControllerIsAvailable(virtualControllerMock, nvmeMapping);
    }

    @Test
    public void validateDiskControllerIsAvailableTestDoesNothingWhenBusNumberIsValidAndAvailableDeviceNumberExists() throws Exception {
        DiskControllerMappingVO nvmeMapping = VmwareHelper.getDiskControllerMapping("nvme", null);
        Mockito.when(virtualControllerMock.getBusNumber()).thenReturn(3);
        Mockito.doReturn(14).when(vmMoSpy).getNextAvailableDeviceNumberForController(virtualControllerMock, nvmeMapping);

        vmMoSpy.validateDiskControllerIsAvailable(virtualControllerMock, nvmeMapping);
    }
}
