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
package com.cloud.storage.resource;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockConstruction;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.cloud.hypervisor.vmware.mo.DatacenterMO;
import com.cloud.hypervisor.vmware.mo.DatastoreMO;
import com.cloud.hypervisor.vmware.mo.VirtualDiskManagerMO;
import com.cloud.hypervisor.vmware.mo.VmdkAdapterType;
import com.cloud.hypervisor.vmware.util.VmwareContext;
import com.cloud.storage.Storage;
import com.cloud.utils.Pair;
import com.vmware.vim25.FileBackedVirtualDiskSpec;
import com.vmware.vim25.ManagedObjectReference;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;
import org.mockito.MockedConstruction;
import org.mockito.Mockito;

public class VmwareStorageLayoutHelperTest {

    @Test
    public void syncVolumeToVmDefaultFolderMovesRootVolumeWithVirtualDiskManager() throws Exception {
        DatacenterMO datacenter = mock(DatacenterMO.class);
        DatastoreMO datastore = mock(DatastoreMO.class);
        VmwareContext context = mock(VmwareContext.class);
        ManagedObjectReference datacenterMor = mock(ManagedObjectReference.class);

        when(datastore.getName()).thenReturn("datastore");
        when(datastore.folderExists("[datastore]", "vm-name")).thenReturn(true);
        when(datastore.fileExists("[datastore] volume.vmdk")).thenReturn(true);
        when(datastore.getContext()).thenReturn(context);
        when(datacenter.getMor()).thenReturn(datacenterMor);

        try (MockedConstruction<VirtualDiskManagerMO> diskManagers = mockConstruction(VirtualDiskManagerMO.class)) {
            String path = VmwareStorageLayoutHelper.syncVolumeToVmDefaultFolder(datacenter, "vm-name", datastore, "volume");

            assertEquals("[datastore] vm-name/volume.vmdk", path);
            VirtualDiskManagerMO diskManager = diskManagers.constructed().get(0);
            verify(diskManager).moveVirtualDisk("[datastore] volume.vmdk", datacenterMor,
                    "[datastore] vm-name/volume.vmdk", datacenterMor, true);
        }
    }

    @Test
    public void syncVolumeToVmDefaultFolderCopiesRootVolumeWithTargetAdapterAndProvisioningType() throws Exception {
        DatacenterMO datacenter = mock(DatacenterMO.class);
        DatastoreMO datastore = mock(DatastoreMO.class);
        VmwareContext context = mock(VmwareContext.class);
        ManagedObjectReference datacenterMor = mock(ManagedObjectReference.class);

        when(datastore.getName()).thenReturn("datastore");
        when(datastore.folderExists("[datastore]", "vm-name")).thenReturn(true);
        when(datastore.fileExists("[datastore] volume.vmdk")).thenReturn(true);
        when(datastore.getContext()).thenReturn(context);
        when(datacenter.getMor()).thenReturn(datacenterMor);

        try (MockedConstruction<VirtualDiskManagerMO> diskManagers = mockConstruction(VirtualDiskManagerMO.class)) {
            Pair<String, Boolean> result = VmwareStorageLayoutHelper.syncVolumeToVmDefaultFolder(datacenter, "vm-name", datastore, "volume", null,
                    VmdkAdapterType.lsilogic, Storage.ProvisioningType.FAT);

            assertEquals("[datastore] vm-name/volume.vmdk", result.first());
            assertTrue(result.second());
            VirtualDiskManagerMO diskManager = diskManagers.constructed().get(0);
            ArgumentCaptor<FileBackedVirtualDiskSpec> diskSpec = ArgumentCaptor.forClass(FileBackedVirtualDiskSpec.class);
            InOrder inOrder = Mockito.inOrder(diskManager);
            inOrder.verify(diskManager).copyVirtualDisk(eq("[datastore] volume.vmdk"), eq(datacenterMor),
                    eq("[datastore] vm-name/volume.vmdk"), eq(datacenterMor), diskSpec.capture(), eq(true));
            inOrder.verify(diskManager).deleteVirtualDisk("[datastore] volume.vmdk", datacenterMor);
            assertEquals("lsiLogic", diskSpec.getValue().getAdapterType());
            assertEquals("eagerZeroedThick", diskSpec.getValue().getDiskType());
        }
    }

    @Test
    public void syncVolumeToVmDefaultFolderCopiesBaseFolderVolumeWithTargetAdapter() throws Exception {
        DatacenterMO datacenter = mock(DatacenterMO.class);
        DatastoreMO datastore = mock(DatastoreMO.class);
        VmwareContext context = mock(VmwareContext.class);
        ManagedObjectReference datacenterMor = mock(ManagedObjectReference.class);

        when(datastore.getName()).thenReturn("datastore");
        when(datastore.folderExists("[datastore]", "vm-name")).thenReturn(true);
        when(datastore.fileExists("[datastore] fcd/volume.vmdk")).thenReturn(true);
        when(datastore.getContext()).thenReturn(context);
        when(datacenter.getMor()).thenReturn(datacenterMor);

        try (MockedConstruction<VirtualDiskManagerMO> diskManagers = mockConstruction(VirtualDiskManagerMO.class)) {
            Pair<String, Boolean> result = VmwareStorageLayoutHelper.syncVolumeToVmDefaultFolder(datacenter, "vm-name", datastore, "volume", null,
                    VmdkAdapterType.lsilogic, Storage.ProvisioningType.THIN);

            assertEquals("[datastore] vm-name/volume.vmdk", result.first());
            assertTrue(result.second());
            VirtualDiskManagerMO diskManager = diskManagers.constructed().get(0);
            ArgumentCaptor<FileBackedVirtualDiskSpec> diskSpec = ArgumentCaptor.forClass(FileBackedVirtualDiskSpec.class);
            InOrder inOrder = Mockito.inOrder(diskManager);
            inOrder.verify(diskManager).copyVirtualDisk(eq("[datastore] fcd/volume.vmdk"), eq(datacenterMor),
                    eq("[datastore] vm-name/volume.vmdk"), eq(datacenterMor), diskSpec.capture(), eq(true));
            inOrder.verify(diskManager).deleteVirtualDisk("[datastore] fcd/volume.vmdk", datacenterMor);
            assertEquals("lsiLogic", diskSpec.getValue().getAdapterType());
            assertEquals("thin", diskSpec.getValue().getDiskType());
        }
    }
}
