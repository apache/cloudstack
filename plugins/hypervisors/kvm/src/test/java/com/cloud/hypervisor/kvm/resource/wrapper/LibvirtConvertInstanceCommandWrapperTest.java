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

import java.util.Arrays;
import java.util.List;
import java.util.UUID;

import org.apache.cloudstack.storage.to.PrimaryDataStoreTO;
import org.apache.cloudstack.vm.UnmanagedInstanceTO;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockedConstruction;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.mockito.Spy;
import org.mockito.junit.MockitoJUnitRunner;

import com.cloud.agent.api.Answer;
import com.cloud.agent.api.ConvertInstanceCommand;
import com.cloud.agent.api.to.NfsTO;
import com.cloud.agent.api.to.RemoteInstanceTO;
import com.cloud.hypervisor.Hypervisor;
import com.cloud.hypervisor.kvm.resource.LibvirtComputingResource;
import com.cloud.hypervisor.kvm.resource.LibvirtDomainXMLParser;
import com.cloud.hypervisor.kvm.resource.LibvirtVMDef;
import com.cloud.hypervisor.kvm.storage.KVMPhysicalDisk;
import com.cloud.hypervisor.kvm.storage.KVMStoragePool;
import com.cloud.hypervisor.kvm.storage.KVMStoragePoolManager;
import com.cloud.storage.Storage;
import com.cloud.utils.Pair;
import com.cloud.utils.script.Script;

@RunWith(MockitoJUnitRunner.class)
public class LibvirtConvertInstanceCommandWrapperTest {

    @Spy
    private LibvirtConvertInstanceCommandWrapper convertInstanceCommandWrapper = Mockito.spy(LibvirtConvertInstanceCommandWrapper.class);

    @Mock
    private LibvirtComputingResource libvirtComputingResourceMock;

    @Mock
    private KVMStoragePool temporaryPool;
    @Mock
    private KVMStoragePool destinationPool;
    @Mock
    private PrimaryDataStoreTO primaryDataStore;
    @Mock
    private NfsTO secondaryDataStore;
    @Mock
    private KVMStoragePoolManager storagePoolManager;

    private static final String secondaryPoolUrl = "nfs://192.168.1.1/secondary";
    private static final String vmName = "VmToImport";

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

    @Test
    public void testMoveTemporaryDisksToDestination() {
        KVMPhysicalDisk sourceDisk = Mockito.mock(KVMPhysicalDisk.class);
        List<KVMPhysicalDisk> disks = List.of(sourceDisk);
        String destinationPoolUuid = UUID.randomUUID().toString();
        List<String> destinationPools = List.of(destinationPoolUuid);

        KVMPhysicalDisk destDisk = Mockito.mock(KVMPhysicalDisk.class);
        Mockito.when(destDisk.getPath()).thenReturn("xyz");
        Mockito.when(storagePoolManager.getStoragePool(Storage.StoragePoolType.NetworkFilesystem, destinationPoolUuid))
                .thenReturn(destinationPool);
        Mockito.when(destinationPool.getType()).thenReturn(Storage.StoragePoolType.NetworkFilesystem);
        Mockito.when(storagePoolManager.copyPhysicalDisk(Mockito.eq(sourceDisk), Mockito.anyString(), Mockito.eq(destinationPool), Mockito.anyInt()))
                .thenReturn(destDisk);

        List<KVMPhysicalDisk> movedDisks = convertInstanceCommandWrapper.moveTemporaryDisksToDestination(disks, destinationPools, storagePoolManager);
        Assert.assertEquals(1, movedDisks.size());
        Assert.assertEquals("xyz", movedDisks.get(0).getPath());
    }

    @Test
    public void testGetUnmanagedInstanceDisks() {
        try (MockedStatic<Script> ignored = Mockito.mockStatic(Script.class)) {
            String relativePath = UUID.randomUUID().toString();
            LibvirtVMDef.DiskDef diskDef = new LibvirtVMDef.DiskDef();
            LibvirtVMDef.DiskDef.DiskBus bus = LibvirtVMDef.DiskDef.DiskBus.IDE;
            LibvirtVMDef.DiskDef.DiskFmtType type = LibvirtVMDef.DiskDef.DiskFmtType.QCOW2;
            diskDef.defFileBasedDisk(relativePath, relativePath, bus, type);

            KVMPhysicalDisk sourceDisk = Mockito.mock(KVMPhysicalDisk.class);
            Mockito.when(sourceDisk.getName()).thenReturn(UUID.randomUUID().toString());
            Mockito.when(sourceDisk.getPool()).thenReturn(destinationPool);
            Mockito.when(destinationPool.getType()).thenReturn(Storage.StoragePoolType.NetworkFilesystem);
            List<KVMPhysicalDisk> disks = List.of(sourceDisk);

            LibvirtDomainXMLParser parser = Mockito.mock(LibvirtDomainXMLParser.class);
            Mockito.when(parser.getDisks()).thenReturn(List.of(diskDef));
            Mockito.doReturn(new Pair<String, String>(null, null)).when(convertInstanceCommandWrapper).getNfsStoragePoolHostAndPath(destinationPool);

            Mockito.when(Script.executePipedCommands(Mockito.anyList(), Mockito.anyLong()))
                    .thenReturn(new Pair<>(0, null));

            List<UnmanagedInstanceTO.Disk> unmanagedInstanceDisks = convertInstanceCommandWrapper.getUnmanagedInstanceDisks(disks, parser);
            Assert.assertEquals(1, unmanagedInstanceDisks.size());
            UnmanagedInstanceTO.Disk disk = unmanagedInstanceDisks.get(0);
            Assert.assertEquals(LibvirtVMDef.DiskDef.DiskBus.IDE.toString(), disk.getController());
        }
    }

    @Test
    public void testGetNfsStoragePoolHostAndPath() {
        try (MockedStatic<Script> ignored = Mockito.mockStatic(Script.class)) {
            String localMountPoint = "/mnt/xyz";
            String host = "192.168.1.2";
            String path = "/secondary";
            String mountOutput = String.format("%s:%s on %s type nfs (...)", host, path, localMountPoint);
            Mockito.when(temporaryPool.getLocalPath()).thenReturn(localMountPoint);
            Mockito.when(Script.executePipedCommands(Mockito.anyList(), Mockito.anyLong()))
                    .thenReturn(new Pair<>(0, mountOutput));

            Pair<String, String> pair = convertInstanceCommandWrapper.getNfsStoragePoolHostAndPath(temporaryPool);
            Assert.assertEquals(host, pair.first());
            Assert.assertEquals(path, pair.second());
        }
    }

    private RemoteInstanceTO getRemoteInstanceTO(Hypervisor.HypervisorType hypervisorType) {
        RemoteInstanceTO remoteInstanceTO = Mockito.mock(RemoteInstanceTO.class);
        Mockito.when(remoteInstanceTO.getHypervisorType()).thenReturn(hypervisorType);
        Mockito.when(remoteInstanceTO.getInstanceName()).thenReturn(vmName);
        return remoteInstanceTO;
    }

    private ConvertInstanceCommand getConvertInstanceCommand(RemoteInstanceTO remoteInstanceTO, Hypervisor.HypervisorType hypervisorType, boolean checkConversionSupport) {
        ConvertInstanceCommand cmd = Mockito.mock(ConvertInstanceCommand.class);
        Mockito.when(cmd.getSourceInstance()).thenReturn(remoteInstanceTO);
        Mockito.when(cmd.getDestinationHypervisorType()).thenReturn(hypervisorType);
        Mockito.when(cmd.getWait()).thenReturn(14400);
        Mockito.when(cmd.getConversionTemporaryLocation()).thenReturn(secondaryDataStore);
        Mockito.when(cmd.getCheckConversionSupport()).thenReturn(checkConversionSupport);
        return cmd;
    }

    @Test
    public void testExecuteConvertUnsupportedOnTheHost() {
        RemoteInstanceTO remoteInstanceTO = getRemoteInstanceTO(Hypervisor.HypervisorType.VMware);
        Mockito.when(libvirtComputingResourceMock.hostSupportsInstanceConversion()).thenReturn(false);
        ConvertInstanceCommand cmd = getConvertInstanceCommand(remoteInstanceTO, Hypervisor.HypervisorType.KVM, true);
        Answer answer = convertInstanceCommandWrapper.execute(cmd, libvirtComputingResourceMock);
        Assert.assertFalse(answer.getResult());
    }

    @Test
    public void testExecuteConvertUnsupportedHypervisors() {
        RemoteInstanceTO remoteInstanceTO = getRemoteInstanceTO(Hypervisor.HypervisorType.XenServer);
        Mockito.when(libvirtComputingResourceMock.hostSupportsInstanceConversion()).thenReturn(true);
        ConvertInstanceCommand cmd = getConvertInstanceCommand(remoteInstanceTO, Hypervisor.HypervisorType.KVM, true);
        Answer answer = convertInstanceCommandWrapper.execute(cmd, libvirtComputingResourceMock);
        Assert.assertFalse(answer.getResult());
    }

    @Test
    public void testExecuteConvertFailure() {
        RemoteInstanceTO remoteInstanceTO = getRemoteInstanceTO(Hypervisor.HypervisorType.VMware);
        ConvertInstanceCommand cmd = getConvertInstanceCommand(remoteInstanceTO, Hypervisor.HypervisorType.KVM, true);
        String localMountPoint = "/mnt/xyz";
        Mockito.when(temporaryPool.getLocalPath()).thenReturn(localMountPoint);

        try (MockedStatic<Script> ignored = Mockito.mockStatic(Script.class);
             MockedConstruction<Script> ignored2 = Mockito.mockConstruction(Script.class, (mock, context) -> {
                 Mockito.when(mock.execute()).thenReturn("");
                 Mockito.when(mock.getExitValue()).thenReturn(1);
             })
        ) {
            Mockito.when(libvirtComputingResourceMock.hostSupportsInstanceConversion()).thenReturn(true);

            Answer answer = convertInstanceCommandWrapper.execute(cmd, libvirtComputingResourceMock);
            Assert.assertFalse(answer.getResult());
            Mockito.verify(convertInstanceCommandWrapper).performInstanceConversion(Mockito.anyString(),
                    Mockito.anyString(), Mockito.anyString(), Mockito.anyLong(), Mockito.anyBoolean());
        }
    }
}
