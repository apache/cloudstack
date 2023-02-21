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

package com.cloud.hypervisor.vmware.util;

import static org.junit.Assert.assertEquals;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.MockitoJUnitRunner;

import com.cloud.hypervisor.vmware.mo.VirtualMachineMO;
import com.vmware.vim25.VirtualDisk;

@RunWith(MockitoJUnitRunner.class)
public class VmwareHelperTest {
    @Mock
    private VirtualMachineMO virtualMachineMO;

    @Test
    public void prepareDiskDeviceTestNotLimitingIOPS() throws Exception {
        Mockito.when(virtualMachineMO.getIDEDeviceControllerKey()).thenReturn(1);
        VirtualDisk virtualDisk = (VirtualDisk) VmwareHelper.prepareDiskDevice(virtualMachineMO, null, -1, new String[1], null, 0, 0, null);
        assertEquals(null, virtualDisk.getStorageIOAllocation());
    }

    @Test
    public void prepareDiskDeviceTestLimitingIOPS() throws Exception {
        Mockito.when(virtualMachineMO.getIDEDeviceControllerKey()).thenReturn(1);
        VirtualDisk virtualDisk = (VirtualDisk) VmwareHelper.prepareDiskDevice(virtualMachineMO, null, -1, new String[1], null, 0, 0, Long.valueOf(1000));
        assertEquals(Long.valueOf(1000), virtualDisk.getStorageIOAllocation().getLimit());
    }

    @Test
    public void prepareDiskDeviceTestLimitingIOPSToZero() throws Exception {
        Mockito.when(virtualMachineMO.getIDEDeviceControllerKey()).thenReturn(1);
        VirtualDisk virtualDisk = (VirtualDisk) VmwareHelper.prepareDiskDevice(virtualMachineMO, null, -1, new String[1], null, 0, 0, Long.valueOf(0));
        assertEquals(null, virtualDisk.getStorageIOAllocation());
    }
}
