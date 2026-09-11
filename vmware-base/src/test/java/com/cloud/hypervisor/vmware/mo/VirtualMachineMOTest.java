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
import com.cloud.utils.exception.CloudRuntimeException;
import com.vmware.vim25.ManagedObjectReference;
import com.vmware.vim25.VimPortType;
import com.vmware.vim25.VirtualDevice;
import com.vmware.vim25.VirtualLsiLogicController;
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
import org.mockito.MockitoAnnotations;
import org.mockito.junit.MockitoJUnitRunner;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.fail;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class VirtualMachineMOTest {

    @Mock
    VmwareContext context;
    @Mock
    VmwareClient client;
    @Mock
    ManagedObjectReference mor;

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
    public void TestEnsureLsiLogicDeviceControllers() {
        try {
            when(client.getDynamicProperty(any(ManagedObjectReference.class), any(String.class))).thenReturn(getVirtualScSiDeviceList(VirtualLsiLogicController.class));
            vmMo.ensureLsiLogicDeviceControllers(1, 0);
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
    public void testAttachDiskSucceedsWhenAdapterTypeUpdateFails() throws Exception {
        VirtualMachineMO spyVmMo = spy(vmMo);
        ManagedObjectReference morDs = mock(ManagedObjectReference.class);
        ManagedObjectReference morTask = mock(ManagedObjectReference.class);
        VimPortType service = mock(VimPortType.class);

        doReturn(1).when(spyVmMo).getScsiDiskControllerKey(anyString());
        doReturn(200).when(spyVmMo).getIDEDeviceControllerKey();
        doReturn(0).when(spyVmMo).getNextDeviceNumber(anyInt());
        doThrow(new IOException("HTTP 500 from vCenter datastore browser")).when(spyVmMo).updateVmdkAdapter(anyString(), anyString());

        when(mor.getValue()).thenReturn("vm-1");
        when(context.getService()).thenReturn(service);
        when(service.reconfigVMTask(eq(mor), any())).thenReturn(morTask);
        when(client.waitForTask(morTask)).thenReturn(true);

        spyVmMo.attachDisk(new String[]{"[ds] i-2-3-VM/data.vmdk"}, morDs, "pvscsi", null, null);

        verify(spyVmMo).updateVmdkAdapter(eq("[ds] i-2-3-VM/data.vmdk"), eq("pvscsi"));
        verify(service).reconfigVMTask(eq(mor), any());
        verify(client).waitForTask(morTask);
    }

    @Test(expected = Exception.class)
    public void testAttachDiskFailsWhenAdapterTypeIsInvalid() throws Exception {
        VirtualMachineMO spyVmMo = spy(vmMo);
        ManagedObjectReference morDs = mock(ManagedObjectReference.class);
        ManagedObjectReference morTask = mock(ManagedObjectReference.class);
        VimPortType service = mock(VimPortType.class);

        doReturn(1).when(spyVmMo).getScsiDiskControllerKey(anyString());
        doReturn(200).when(spyVmMo).getIDEDeviceControllerKey();
        doReturn(0).when(spyVmMo).getNextDeviceNumber(anyInt());
        doThrow(new Exception("Failed to attach disk due to invalid vmdk adapter type")).when(spyVmMo).updateVmdkAdapter(anyString(), anyString());

        when(mor.getValue()).thenReturn("vm-1");
        // Lenient: these mirror the success path and are only reached if the
        // invalid-adapter-type exception is (incorrectly) swallowed by attachDisk.
        lenient().when(context.getService()).thenReturn(service);
        lenient().when(service.reconfigVMTask(eq(mor), any())).thenReturn(morTask);
        lenient().when(client.waitForTask(morTask)).thenReturn(true);

        spyVmMo.attachDisk(new String[]{"[ds] i-2-3-VM/data.vmdk"}, morDs, "pvscsi", null, null);
    }
}
