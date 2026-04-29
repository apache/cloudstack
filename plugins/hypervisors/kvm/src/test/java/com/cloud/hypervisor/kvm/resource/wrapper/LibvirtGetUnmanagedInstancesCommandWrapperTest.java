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
package com.cloud.hypervisor.kvm.resource.wrapper;

import com.cloud.hypervisor.kvm.resource.LibvirtVMDef;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.mockito.Spy;
import org.mockito.junit.MockitoJUnitRunner;

import java.util.UUID;

@RunWith(MockitoJUnitRunner.class)
public class LibvirtGetUnmanagedInstancesCommandWrapperTest {

    @Spy
    private LibvirtGetUnmanagedInstancesCommandWrapper wrapper = new LibvirtGetUnmanagedInstancesCommandWrapper();

    @Test
    public void testGetDiskRelativePathNullDisk() {
        Assert.assertNull(wrapper.getDiskRelativePath(null));
    }

    @Test
    public void testGetDiskRelativePathBlockType() {
        LibvirtVMDef.DiskDef diskDef = Mockito.mock(LibvirtVMDef.DiskDef.class);
        Mockito.when(diskDef.getDiskType()).thenReturn(LibvirtVMDef.DiskDef.DiskType.BLOCK);
        Assert.assertNull(wrapper.getDiskRelativePath(diskDef));
    }

    @Test
    public void testGetDiskRelativePathNullPath() {
        LibvirtVMDef.DiskDef diskDef = Mockito.mock(LibvirtVMDef.DiskDef.class);
        Mockito.when(diskDef.getDiskType()).thenReturn(LibvirtVMDef.DiskDef.DiskType.FILE);
        Mockito.when(diskDef.getSourcePath()).thenReturn(null);
        Assert.assertNull(wrapper.getDiskRelativePath(diskDef));
    }

    @Test
    public void testGetDiskRelativePathWithoutSlashes() {
        LibvirtVMDef.DiskDef diskDef = Mockito.mock(LibvirtVMDef.DiskDef.class);
        Mockito.when(diskDef.getDiskType()).thenReturn(LibvirtVMDef.DiskDef.DiskType.FILE);
        String imagePath = UUID.randomUUID().toString();
        Mockito.when(diskDef.getSourcePath()).thenReturn(imagePath);
        Assert.assertEquals(imagePath, wrapper.getDiskRelativePath(diskDef));
    }

    @Test
    public void testGetDiskRelativePathFullPath() {
        LibvirtVMDef.DiskDef diskDef = Mockito.mock(LibvirtVMDef.DiskDef.class);
        Mockito.when(diskDef.getDiskType()).thenReturn(LibvirtVMDef.DiskDef.DiskType.FILE);
        String relativePath = "ea4b2296-d349-4968-ab72-c8eb523b556e";
        String imagePath = String.format("/mnt/97e4c9ed-e3bc-3e26-b103-7967fc9feae1/%s", relativePath);
        Mockito.when(diskDef.getSourcePath()).thenReturn(imagePath);
        Assert.assertEquals(relativePath, wrapper.getDiskRelativePath(diskDef));
    }
}
