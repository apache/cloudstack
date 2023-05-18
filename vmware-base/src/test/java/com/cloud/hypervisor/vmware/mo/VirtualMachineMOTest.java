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
import org.mockito.runners.MockitoJUnitRunner;

import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.fail;
import static org.mockito.Matchers.any;
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

    private List<VirtualDevice> getVirtualScSiDeviceList(Class<?> cls) {

        List<VirtualDevice> deviceList = new ArrayList<>();
        try {

            VirtualSCSIController scsiController = (VirtualSCSIController)cls.newInstance();
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
        MockitoAnnotations.initMocks(this);
        vmMo = new VirtualMachineMO(context, mor);
        when(context.getVimClient()).thenReturn(client);
    }

    @BeforeClass
    public static void setUpBeforeClass() throws Exception {
    }

    @AfterClass
    public static void tearDownAfterClass() throws Exception {
    }

    @Before
    public void setUp() throws Exception {
    }

    @After
    public void tearDown() throws Exception {
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
}
