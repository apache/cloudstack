//
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
//
package com.cloud.hypervisor.kvm.resource.wrapper;

import com.cloud.agent.api.to.NfsTO;
import com.cloud.hypervisor.Hypervisor;
import com.cloud.hypervisor.kvm.resource.LibvirtComputingResource;
import com.cloud.hypervisor.kvm.resource.LibvirtDomainXMLParser;
import com.cloud.hypervisor.kvm.resource.LibvirtVMDef;
import com.cloud.hypervisor.kvm.storage.KVMPhysicalDisk;
import com.cloud.hypervisor.kvm.storage.KVMStoragePool;
import com.cloud.hypervisor.kvm.storage.KVMStoragePoolManager;
import com.cloud.utils.script.Script;
import org.apache.cloudstack.storage.to.PrimaryDataStoreTO;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.mockito.Spy;
import org.mockito.junit.MockitoJUnitRunner;

import java.util.Arrays;
import java.util.List;
import java.util.UUID;

@RunWith(MockitoJUnitRunner.class)
public class LibvirtConvertInstanceCommandWrapperTest {

    @Spy
    private LibvirtConvertInstanceCommandWrapper convertInstanceCommandWrapper = Mockito.spy(LibvirtConvertInstanceCommandWrapper.class);

    @Mock
    private LibvirtComputingResource libvirtComputingResourceMock;

    @Mock
    private KVMStoragePool temporaryPool;
    @Mock
    private PrimaryDataStoreTO primaryDataStore;
    @Mock
    private NfsTO secondaryDataStore;
    @Mock
    private KVMStoragePoolManager storagePoolManager;

    private static final String secondaryPoolUrl = "nfs://192.168.1.1/secondary";
    @Before
    public void setUp() {
        Mockito.when(secondaryDataStore.getUrl()).thenReturn(secondaryPoolUrl);
        Mockito.when(libvirtComputingResourceMock.getStoragePoolMgr()).thenReturn(storagePoolManager);
        Mockito.when(storagePoolManager.getStoragePoolByURI(secondaryPoolUrl)).thenReturn(temporaryPool);
        KVMPhysicalDisk physicalDisk1 = Mockito.mock(KVMPhysicalDisk.class);
        KVMPhysicalDisk physicalDisk2 = Mockito.mock(KVMPhysicalDisk.class);
        Mockito.when(temporaryPool.listPhysicalDisks()).thenReturn(Arrays.asList(physicalDisk1, physicalDisk2));
    }

    @Test
    public void testIsInstanceConversionSupportedOnHost() {
        try (MockedStatic<Script> ignored = Mockito.mockStatic(Script.class)) {
            Mockito.when(Script.runSimpleBashScriptForExitValue(LibvirtConvertInstanceCommandWrapper.checkIfConversionIsSupportedCommand)).thenReturn(0);
            boolean supported = convertInstanceCommandWrapper.isInstanceConversionSupportedOnHost();
            Assert.assertTrue(supported);
        }
    }

    @Test
    public void testAreSourceAndDestinationHypervisorsSupported() {
        boolean supported = convertInstanceCommandWrapper.areSourceAndDestinationHypervisorsSupported(Hypervisor.HypervisorType.VMware, Hypervisor.HypervisorType.KVM);
        Assert.assertTrue(supported);
    }

    @Test
    public void testAreSourceAndDestinationHypervisorsSupportedUnsupportedSource() {
        boolean supported = convertInstanceCommandWrapper.areSourceAndDestinationHypervisorsSupported(Hypervisor.HypervisorType.XenServer, Hypervisor.HypervisorType.KVM);
        Assert.assertFalse(supported);
    }

    @Test
    public void testAreSourceAndDestinationHypervisorsSupportedUnsupportedDestination() {
        boolean supported = convertInstanceCommandWrapper.areSourceAndDestinationHypervisorsSupported(Hypervisor.HypervisorType.VMware, Hypervisor.HypervisorType.VMware);
        Assert.assertFalse(supported);
    }

    @Test
    public void testGetTemporaryStoragePool() {
        KVMStoragePool temporaryStoragePool = convertInstanceCommandWrapper.getTemporaryStoragePool(secondaryDataStore, libvirtComputingResourceMock.getStoragePoolMgr());
        Assert.assertNotNull(temporaryStoragePool);
    }

    @Test
    public void testGetTemporaryDisksWithPrefixFromTemporaryPool() {
        String convertPath = "/xyz";
        String convertPrefix = UUID.randomUUID().toString();
        KVMPhysicalDisk physicalDisk1 = Mockito.mock(KVMPhysicalDisk.class);
        Mockito.when(physicalDisk1.getName()).thenReturn("disk1");
        KVMPhysicalDisk physicalDisk2 = Mockito.mock(KVMPhysicalDisk.class);
        Mockito.when(physicalDisk2.getName()).thenReturn("disk2");

        KVMPhysicalDisk convertedDisk1 = Mockito.mock(KVMPhysicalDisk.class);
        Mockito.when(convertedDisk1.getName()).thenReturn(String.format("%s-sda", convertPrefix));
        KVMPhysicalDisk convertedDisk2 = Mockito.mock(KVMPhysicalDisk.class);
        Mockito.when(convertedDisk2.getName()).thenReturn(String.format("%s-sdb", convertPrefix));
        KVMPhysicalDisk convertedXml = Mockito.mock(KVMPhysicalDisk.class);
        Mockito.when(convertedXml.getName()).thenReturn(String.format("%s.xml", convertPrefix));
        Mockito.when(temporaryPool.listPhysicalDisks()).thenReturn(Arrays.asList(physicalDisk1, physicalDisk2,
                convertedDisk1, convertedDisk2, convertedXml));

        List<KVMPhysicalDisk> convertedDisks = convertInstanceCommandWrapper.getTemporaryDisksWithPrefixFromTemporaryPool(temporaryPool, convertPath, convertPrefix);
        Assert.assertEquals(2, convertedDisks.size());
    }

    @Test
    public void testGetTemporaryDisksFromParsedXml() {
        String relativePath = UUID.randomUUID().toString();
        String fullPath = String.format("/mnt/xyz/%s", relativePath);

        LibvirtVMDef.DiskDef diskDef = new LibvirtVMDef.DiskDef();
        LibvirtVMDef.DiskDef.DiskBus bus = LibvirtVMDef.DiskDef.DiskBus.VIRTIO;
        LibvirtVMDef.DiskDef.DiskFmtType type = LibvirtVMDef.DiskDef.DiskFmtType.QCOW2;
        diskDef.defFileBasedDisk(fullPath, "test", bus, type);

        LibvirtDomainXMLParser parser = Mockito.mock(LibvirtDomainXMLParser.class);
        Mockito.when(parser.getDisks()).thenReturn(List.of(diskDef));

        KVMPhysicalDisk convertedDisk1 = Mockito.mock(KVMPhysicalDisk.class);
        Mockito.when(convertedDisk1.getName()).thenReturn("disk1");
        Mockito.when(temporaryPool.getPhysicalDisk(relativePath)).thenReturn(convertedDisk1);

        List<KVMPhysicalDisk> disks = convertInstanceCommandWrapper.getTemporaryDisksFromParsedXml(temporaryPool, parser, "");
        Mockito.verify(convertInstanceCommandWrapper).sanitizeDisksPath(List.of(diskDef));
        Assert.assertEquals(1, disks.size());
        Assert.assertEquals("disk1", disks.get(0).getName());
    }

    @Test
    public void testSanitizeDisksPath() {
        String relativePath = UUID.randomUUID().toString();
        String fullPath = String.format("/mnt/xyz/%s", relativePath);
        LibvirtVMDef.DiskDef diskDef = new LibvirtVMDef.DiskDef();
        LibvirtVMDef.DiskDef.DiskBus bus = LibvirtVMDef.DiskDef.DiskBus.VIRTIO;
        LibvirtVMDef.DiskDef.DiskFmtType type = LibvirtVMDef.DiskDef.DiskFmtType.QCOW2;
        diskDef.defFileBasedDisk(fullPath, "test", bus, type);

        convertInstanceCommandWrapper.sanitizeDisksPath(List.of(diskDef));
        Assert.assertEquals(relativePath, diskDef.getDiskPath());
    }
}
