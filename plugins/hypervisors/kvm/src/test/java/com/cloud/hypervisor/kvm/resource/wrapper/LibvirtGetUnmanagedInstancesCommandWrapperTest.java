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

import com.cloud.hypervisor.kvm.resource.LibvirtComputingResource;
import com.cloud.hypervisor.kvm.resource.LibvirtVMDef;
import com.cloud.utils.Pair;
import org.apache.cloudstack.vm.UnmanagedInstanceTO;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.libvirt.Connect;
import org.libvirt.Domain;
import org.libvirt.DomainBlockInfo;
import org.mockito.Mockito;
import org.mockito.Spy;
import org.mockito.junit.MockitoJUnitRunner;

import java.util.List;
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

    @Test
    public void testGlusterDiskStillUsesMountLookup() throws Exception {
        LibvirtComputingResource libvirtComputingResource = Mockito.mock(LibvirtComputingResource.class);
        Connect conn = Mockito.mock(Connect.class);
        Domain domain = Mockito.mock(Domain.class);
        String domainName = "i-2-40-VM";
        Mockito.when(conn.domainLookupByName(domainName)).thenReturn(domain);
        Mockito.when(domain.blockInfo(Mockito.anyString())).thenReturn(Mockito.mock(DomainBlockInfo.class));
        Mockito.when(libvirtComputingResource.getSourceHostPath("vol1"))
                .thenReturn(new Pair<>("10.0.0.5", "/vol1"));

        LibvirtVMDef.DiskDef glusterDisk = new LibvirtVMDef.DiskDef();
        glusterDisk.defNetworkBasedDisk("vol1/img.qcow2", "10.0.0.5", 24007, null, null, 0,
                LibvirtVMDef.DiskDef.DiskBus.VIRTIO, LibvirtVMDef.DiskDef.DiskProtocol.GLUSTER,
                LibvirtVMDef.DiskDef.DiskFmtType.QCOW2);

        List<UnmanagedInstanceTO.Disk> disks = wrapper.getUnmanagedInstanceDisks(List.of(glusterDisk),
                libvirtComputingResource, conn, domainName);

        Assert.assertEquals(1, disks.size());
        Assert.assertEquals("10.0.0.5", disks.get(0).getDatastoreHost());
        Assert.assertEquals("/vol1", disks.get(0).getDatastorePath());
        Mockito.verify(libvirtComputingResource).getSourceHostPath("vol1");
    }

    @Test
    public void testRbdDiskIsNotResolvedThroughMountLookup() throws Exception {
        LibvirtComputingResource libvirtComputingResource = Mockito.mock(LibvirtComputingResource.class);
        Connect conn = Mockito.mock(Connect.class);
        Domain domain = Mockito.mock(Domain.class);
        String domainName = "i-2-35-VM";
        Mockito.when(conn.domainLookupByName(domainName)).thenReturn(domain);
        Mockito.when(domain.blockInfo(Mockito.anyString())).thenReturn(Mockito.mock(DomainBlockInfo.class));

        LibvirtVMDef.DiskDef rbdDisk = new LibvirtVMDef.DiskDef();
        String imageName = "230e1224-f7b7-4dfc-97a7-3a0752636a31";
        rbdDisk.defNetworkBasedDisk("cloudstack/" + imageName, "192.168.55.201", 6789, null, null, 0,
                LibvirtVMDef.DiskDef.DiskBus.VIRTIO, LibvirtVMDef.DiskDef.DiskProtocol.RBD,
                LibvirtVMDef.DiskDef.DiskFmtType.RAW);

        List<UnmanagedInstanceTO.Disk> disks = wrapper.getUnmanagedInstanceDisks(List.of(rbdDisk),
                libvirtComputingResource, conn, domainName);

        Assert.assertEquals(1, disks.size());
        UnmanagedInstanceTO.Disk disk = disks.get(0);
        Assert.assertEquals("192.168.55.201", disk.getDatastoreHost());
        Assert.assertEquals("cloudstack", disk.getDatastorePath());
        Assert.assertEquals("cloudstack", disk.getDatastoreName());
        Assert.assertEquals("network", disk.getDatastoreType());
        Assert.assertEquals("cloudstack/" + imageName, disk.getImagePath());
        Assert.assertEquals(imageName, disk.getFileBaseName());
        Mockito.verify(libvirtComputingResource, Mockito.never()).getSourceHostPath(Mockito.anyString());
    }
}
