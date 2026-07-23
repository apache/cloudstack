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

import java.util.List;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.rules.TemporaryFolder;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.MockitoJUnitRunner;

import com.cloud.agent.api.Answer;
import com.cloud.agent.api.VmwareCbtPrepareCommand;
import com.cloud.agent.api.to.RemoteInstanceTO;
import com.cloud.agent.api.to.VmwareCbtDiskTO;
import com.cloud.agent.api.to.VmwareCbtTargetStorageType;
import com.cloud.hypervisor.kvm.resource.LibvirtComputingResource;
import com.cloud.hypervisor.kvm.storage.KVMStoragePool;
import com.cloud.hypervisor.kvm.storage.KVMStoragePoolManager;
import com.cloud.storage.Storage;

@RunWith(MockitoJUnitRunner.class)
public class LibvirtVmwareCbtPrepareCommandWrapperTest {

    private static final String MIGRATION_UUID = "migration-uuid";

    private final TestLibvirtVmwareCbtPrepareCommandWrapper wrapper = new TestLibvirtVmwareCbtPrepareCommandWrapper();

    @Rule
    public TemporaryFolder temporaryFolder = new TemporaryFolder();

    @Mock
    private LibvirtComputingResource libvirtComputingResource;
    @Mock
    private KVMStoragePoolManager storagePoolManager;
    @Mock
    private KVMStoragePool storagePool;
    @Mock
    private KVMStoragePool rbdStoragePool;

    @Before
    public void setUp() {
        Mockito.when(libvirtComputingResource.hostSupportsVddkBlockCopy(Mockito.nullable(String.class))).thenReturn(true);
        Mockito.when(libvirtComputingResource.getStoragePoolMgr()).thenReturn(storagePoolManager);
        Mockito.when(storagePoolManager.getStoragePool(Storage.StoragePoolType.NetworkFilesystem, "pool-uuid")).thenReturn(storagePool);
        Mockito.when(storagePool.getLocalPath()).thenReturn(temporaryFolder.getRoot().getAbsolutePath());
        Mockito.when(storagePoolManager.getStoragePool(Storage.StoragePoolType.RBD, "rbd-pool-uuid")).thenReturn(rbdStoragePool);
        Mockito.when(rbdStoragePool.getType()).thenReturn(Storage.StoragePoolType.RBD);
        Mockito.when(rbdStoragePool.getSourceDir()).thenReturn("cloudstack");
        Mockito.when(rbdStoragePool.getSourceHost()).thenReturn("10.0.0.10,10.0.0.11");
        Mockito.when(rbdStoragePool.getSourcePort()).thenReturn(6789);
    }

    @Test
    public void testExecuteReturnsLastCommandOutputOnInitialSyncFailure() {
        wrapper.exitValue = 1;
        wrapper.lastCommandOutput = "qemu-img: nbd+unix://?socket=/tmp/nbdkit/socket: error while reading at byte 1048576";
        VmwareCbtPrepareCommand command = createCommand();
        command.setVddkLibDir("/opt/vmware-vddk");
        command.setVddkThumbprint("AA:BB:CC");

        Answer answer = wrapper.execute(command, libvirtComputingResource);

        Assert.assertFalse(answer.getResult());
        Assert.assertTrue(answer.getDetails(), answer.getDetails().contains("Last command output: qemu-img"));
        Assert.assertTrue(answer.getDetails(), answer.getDetails().contains("error while reading"));
    }

    @Test
    public void testExecuteCopiesInitialSyncDirectlyToRbdRawImage() {
        VmwareCbtPrepareCommand command = createRbdCommand();
        command.setVddkLibDir("/opt/vmware-vddk");
        command.setVddkThumbprint("AA:BB:CC");

        Answer answer = wrapper.execute(command, libvirtComputingResource);

        Assert.assertTrue(answer.getResult());
        Assert.assertTrue(wrapper.lastCommand, wrapper.lastCommand.matches("(?s).*qemu-img convert -f raw -O .*raw.*\\\"\\$uri\\\".*"));
        Assert.assertTrue(wrapper.lastCommand.contains("rbd:cloudstack/cloudstack-cbt-migration-uuid-disk-1-disk-1"));
        Assert.assertFalse(wrapper.lastCommand.contains("-O qcow2"));
    }

    @Test
    public void testExecuteCopiesInitialSyncDirectlyToBlockDevice() {
        KVMStoragePool linstorStoragePool = Mockito.mock(KVMStoragePool.class);
        com.cloud.hypervisor.kvm.storage.KVMPhysicalDisk targetDisk = Mockito.mock(com.cloud.hypervisor.kvm.storage.KVMPhysicalDisk.class);
        Mockito.when(storagePoolManager.getStoragePool(Storage.StoragePoolType.Linstor, "linstor-pool-uuid")).thenReturn(linstorStoragePool);
        Mockito.when(linstorStoragePool.getType()).thenReturn(Storage.StoragePoolType.Linstor);
        Mockito.when(targetDisk.getPath()).thenReturn("/dev/drbd/by-res/cs-cbt-abc12345-2000/0");
        Mockito.when(linstorStoragePool.createPhysicalDisk(Mockito.eq("cbt-abc12345-2000"), Mockito.any(), Mockito.any(),
                Mockito.eq(8192L), Mockito.isNull())).thenReturn(targetDisk);

        VmwareCbtDiskTO disk = new VmwareCbtDiskTO("disk-1", 2000, "[datastore1] vm/disk-1.vmdk",
                "datastore1", "cbt-abc12345-2000", "raw", "*", null, 8192);
        VmwareCbtPrepareCommand command = new VmwareCbtPrepareCommand(MIGRATION_UUID, createRemoteInstance(), List.of(disk),
                Storage.StoragePoolType.Linstor, "linstor-pool-uuid", VmwareCbtTargetStorageType.RAW_BLOCK_DEVICE,
                "VirtualMachineSnapshot:snapshot-1");
        command.setVddkLibDir("/opt/vmware-vddk");
        command.setVddkThumbprint("AA:BB:CC");

        Answer answer = wrapper.execute(command, libvirtComputingResource);

        Assert.assertTrue(answer.getDetails(), answer.getResult());
        Assert.assertTrue(wrapper.lastCommand, wrapper.lastCommand.contains("qemu-img convert -n --target-is-zero -f raw -O "));
        Assert.assertTrue(wrapper.lastCommand, wrapper.lastCommand.matches("(?s).*qemu-img convert -n --target-is-zero -f raw -O .*raw.*\\\"\\$uri\\\".*"));
        Assert.assertTrue(wrapper.lastCommand, wrapper.lastCommand.contains("/dev/drbd/by-res/cs-cbt-abc12345-2000/0"));
        Mockito.verify(linstorStoragePool).createPhysicalDisk(Mockito.eq("cbt-abc12345-2000"), Mockito.any(), Mockito.any(),
                Mockito.eq(8192L), Mockito.isNull());
    }

    @Test
    public void testExecuteUsesNbdcopyForBlockDeviceWhenAvailable() {
        Mockito.when(libvirtComputingResource.hostSupportsNbdcopy()).thenReturn(true);
        KVMStoragePool linstorStoragePool = Mockito.mock(KVMStoragePool.class);
        com.cloud.hypervisor.kvm.storage.KVMPhysicalDisk targetDisk = Mockito.mock(com.cloud.hypervisor.kvm.storage.KVMPhysicalDisk.class);
        Mockito.when(storagePoolManager.getStoragePool(Storage.StoragePoolType.Linstor, "linstor-pool-uuid")).thenReturn(linstorStoragePool);
        Mockito.when(linstorStoragePool.getType()).thenReturn(Storage.StoragePoolType.Linstor);
        Mockito.when(targetDisk.getPath()).thenReturn("/dev/drbd/by-res/cs-cbt-abc12345-2000/0");
        Mockito.when(linstorStoragePool.createPhysicalDisk(Mockito.eq("cbt-abc12345-2000"), Mockito.any(), Mockito.any(),
                Mockito.eq(8192L), Mockito.isNull())).thenReturn(targetDisk);

        VmwareCbtDiskTO disk = new VmwareCbtDiskTO("disk-1", 2000, "[datastore1] vm/disk-1.vmdk",
                "datastore1", "cbt-abc12345-2000", "raw", "*", null, 8192);
        VmwareCbtPrepareCommand command = new VmwareCbtPrepareCommand(MIGRATION_UUID, createRemoteInstance(), List.of(disk),
                Storage.StoragePoolType.Linstor, "linstor-pool-uuid", VmwareCbtTargetStorageType.RAW_BLOCK_DEVICE,
                "VirtualMachineSnapshot:snapshot-1");
        command.setVddkLibDir("/opt/vmware-vddk");
        command.setVddkThumbprint("AA:BB:CC");

        Answer answer = wrapper.execute(command, libvirtComputingResource);

        Assert.assertTrue(answer.getDetails(), answer.getResult());
        Assert.assertTrue(wrapper.lastCommand, wrapper.lastCommand.matches("(?s).*nbdcopy --destination-is-zero \\\"\\$uri\\\" .*/dev/drbd/by-res/cs-cbt-abc12345-2000/0.*"));
        Assert.assertFalse(wrapper.lastCommand, wrapper.lastCommand.contains("qemu-img convert"));
    }

    private VmwareCbtPrepareCommand createCommand() {
        VmwareCbtDiskTO disk = new VmwareCbtDiskTO("disk-1", 2000, "[datastore1] vm/disk-1.vmdk",
                "datastore1", null, "qcow2", "*", null, 8192);
        return new VmwareCbtPrepareCommand(MIGRATION_UUID, createRemoteInstance(), List.of(disk),
                Storage.StoragePoolType.NetworkFilesystem, "pool-uuid", VmwareCbtTargetStorageType.QCOW2_FILE,
                "VirtualMachineSnapshot:snapshot-1");
    }

    private VmwareCbtPrepareCommand createRbdCommand() {
        VmwareCbtDiskTO disk = new VmwareCbtDiskTO("disk-1", 2000, "[datastore1] vm/disk-1.vmdk",
                "datastore1", null, "raw", "*", null, 8192);
        return new VmwareCbtPrepareCommand(MIGRATION_UUID, createRemoteInstance(), List.of(disk),
                Storage.StoragePoolType.RBD, "rbd-pool-uuid", VmwareCbtTargetStorageType.RBD_RAW,
                "VirtualMachineSnapshot:snapshot-1");
    }

    private RemoteInstanceTO createRemoteInstance() {
        return new RemoteInstanceTO("source-vm", null, "vcenter.example.com", "administrator@vsphere.local",
                "password", "Datacenter", "Cluster", "esxi.example.com", "VirtualMachine:vm-1");
    }

    private static class TestLibvirtVmwareCbtPrepareCommandWrapper extends LibvirtVmwareCbtPrepareCommandWrapper {
        private int exitValue;
        private String lastCommandOutput;
        private String lastCommand;

        @Override
        protected VmwareCbtCommandResult executeLoggedBash(String command, long timeout, String logPrefix) {
            lastCommand = command;
            return new VmwareCbtCommandResult(exitValue, lastCommandOutput);
        }
    }
}
