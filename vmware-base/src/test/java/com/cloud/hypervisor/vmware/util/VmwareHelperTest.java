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
import static org.junit.Assert.assertNull;

import com.vmware.vim25.DatastoreInfo;
import com.vmware.vim25.Description;
import com.vmware.vim25.ManagedObjectReference;
import com.vmware.vim25.VirtualDiskFlatVer2BackingInfo;
import org.apache.cloudstack.vm.UnmanagedInstanceTO;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.MockitoJUnitRunner;

import com.cloud.hypervisor.vmware.mo.VirtualMachineMO;
import com.vmware.vim25.VirtualDisk;

import java.util.List;

@RunWith(MockitoJUnitRunner.class)
public class VmwareHelperTest {
    @Mock
    private VirtualMachineMO virtualMachineMO;

    private static final String diskLabel = "disk1";
    private static final String diskFileBaseName = "xyz.vmdk";
    private static final String dataStoreName = "Datastore";
    private static final String vmName = "VM1";

    @Before
    public void setUp() throws Exception {
        VirtualDiskFlatVer2BackingInfo backingInfo = Mockito.mock(VirtualDiskFlatVer2BackingInfo.class);
        Mockito.when(backingInfo.getFileName()).thenReturn("abc");
        Mockito.when(backingInfo.getDatastore()).thenReturn(Mockito.mock(ManagedObjectReference.class));
        VirtualDisk disk = Mockito.mock(VirtualDisk.class);
        VirtualDisk[] disks = new VirtualDisk[1];
        disks[0] = disk;
        Description description = Mockito.mock(Description.class);
        Mockito.when(description.getLabel()).thenReturn(diskLabel);
        Mockito.when(description.getSummary()).thenReturn("");
        Mockito.when(disk.getBacking()).thenReturn(backingInfo);
        Mockito.when(disk.getDeviceInfo()).thenReturn(description);
        Mockito.when(virtualMachineMO.getAllDiskDevice()).thenReturn(disks);
        Mockito.when(virtualMachineMO.getVmdkFileBaseName(disk)).thenReturn(diskFileBaseName);

        DatastoreInfo datastoreInfo = Mockito.mock(DatastoreInfo.class);
        Mockito.when(datastoreInfo.getName()).thenReturn(dataStoreName);
        VmwareClient client = Mockito.mock(VmwareClient.class);
        Mockito.when(client.getDynamicProperty(Mockito.any(ManagedObjectReference.class), Mockito.anyString()))
                .thenReturn(datastoreInfo);
        VmwareContext context = Mockito.mock(VmwareContext.class);
        Mockito.when(context.getVimClient()).thenReturn(client);
        Mockito.when(virtualMachineMO.getContext()).thenReturn(context);
        Mockito.when(virtualMachineMO.getName()).thenReturn(vmName);
    }

    @Test
    public void prepareDiskDeviceTestNotLimitingIOPS() throws Exception {
        Mockito.when(virtualMachineMO.getIDEDeviceControllerKey()).thenReturn(1);
        VirtualDisk virtualDisk = (VirtualDisk) VmwareHelper.prepareDiskDevice(virtualMachineMO, null, -1, new String[1], null, 0, 0, null);
        assertNull(virtualDisk.getStorageIOAllocation());
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
        assertNull(virtualDisk.getStorageIOAllocation());
    }

    @Test
    public void testGetUnmanageInstanceDisks() {
        List<UnmanagedInstanceTO.Disk> disks = VmwareHelper.getUnmanageInstanceDisks(virtualMachineMO);
        Assert.assertEquals(1, disks.size());
        UnmanagedInstanceTO.Disk disk = disks.get(0);
        Assert.assertEquals(diskLabel, disk.getLabel());
        Assert.assertEquals(diskFileBaseName, disk.getFileBaseName());
        Assert.assertEquals(dataStoreName, disk.getDatastoreName());
    }
}
