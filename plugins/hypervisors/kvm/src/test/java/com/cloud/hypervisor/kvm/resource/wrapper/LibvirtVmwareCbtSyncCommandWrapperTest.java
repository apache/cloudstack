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

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;
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
import com.cloud.agent.api.VmwareCbtMigrationAnswer;
import com.cloud.agent.api.VmwareCbtSyncCommand;
import com.cloud.agent.api.to.RemoteInstanceTO;
import com.cloud.agent.api.to.VmwareCbtChangedBlockRangeTO;
import com.cloud.agent.api.to.VmwareCbtDiskTO;
import com.cloud.agent.api.to.VmwareCbtTargetStorageType;
import com.cloud.hypervisor.kvm.resource.LibvirtComputingResource;
import com.cloud.hypervisor.kvm.storage.KVMStoragePool;
import com.cloud.hypervisor.kvm.storage.KVMStoragePoolManager;
import com.cloud.storage.Storage;

@RunWith(MockitoJUnitRunner.class)
public class LibvirtVmwareCbtSyncCommandWrapperTest {

    private final TestLibvirtVmwareCbtSyncCommandWrapper wrapper = new TestLibvirtVmwareCbtSyncCommandWrapper();

    @Rule
    public TemporaryFolder temporaryFolder = new TemporaryFolder();

    @Mock
    private LibvirtComputingResource libvirtComputingResource;
    @Mock
    private KVMStoragePoolManager storagePoolManager;
    @Mock
    private KVMStoragePool rbdStoragePool;

    @Before
    public void setUp() {
        Mockito.when(libvirtComputingResource.hostSupportsVddkBlockCopy(Mockito.nullable(String.class))).thenReturn(true);
        Mockito.when(libvirtComputingResource.getStoragePoolMgr()).thenReturn(storagePoolManager);
        Mockito.when(storagePoolManager.getStoragePool(Storage.StoragePoolType.RBD, "rbd-pool-uuid")).thenReturn(rbdStoragePool);
        Mockito.when(rbdStoragePool.getType()).thenReturn(Storage.StoragePoolType.RBD);
        Mockito.when(rbdStoragePool.getSourceDir()).thenReturn("cloudstack");
        Mockito.when(rbdStoragePool.getSourceHost()).thenReturn("10.0.0.10");
        Mockito.when(rbdStoragePool.getSourcePort()).thenReturn(6789);
    }

    @Test
    public void testExecuteNoChangedBlocksReturnsReadyForCutover() {
        VmwareCbtSyncCommand command = createCommand(Collections.emptyList(), Collections.emptyList());

        Answer answer = wrapper.execute(command, libvirtComputingResource);

        Assert.assertTrue(answer.getResult());
        Assert.assertTrue(((VmwareCbtMigrationAnswer) answer).getReadyForCutover());
    }

    @Test
    public void testExecuteRejectsChangedBlocksWithoutTargetPath() {
        VmwareCbtDiskTO disk = createDisk("disk-1", null, 8192);
        VmwareCbtSyncCommand command = createCommand(List.of(disk),
                List.of(new VmwareCbtChangedBlockRangeTO("disk-1", 0, 1024)));

        Answer answer = wrapper.execute(command, libvirtComputingResource);

        Assert.assertFalse(answer.getResult());
        Assert.assertTrue(answer.getDetails().contains("no target path"));
    }

    @Test
    public void testExecuteCopiesValidatedChangedBlocks() throws IOException {
        VmwareCbtDiskTO disk = createDisk("disk-1", temporaryFolder.newFile("disk-1.qcow2").getAbsolutePath(), 8192);
        VmwareCbtSyncCommand command = createCommand(List.of(disk),
                List.of(new VmwareCbtChangedBlockRangeTO("disk-1", 0, 1024)));
        command.setVddkLibDir("/opt/vmware-vddk");
        command.setVddkThumbprint("AA:BB:CC");

        Answer answer = wrapper.execute(command, libvirtComputingResource);

        Assert.assertTrue(answer.getResult());
        Assert.assertEquals(1024, ((VmwareCbtMigrationAnswer) answer).getChangedBytes());
        Assert.assertTrue(answer.getDetails().contains("copied 1 changed block range"));
        Assert.assertTrue(wrapper.lastCommand.contains("nbdkit -r -U - vddk"));
        Assert.assertTrue(wrapper.lastCommand.contains("export uri; bash "));
        Assert.assertTrue(wrapper.lastDiskSyncScript.contains("qemu-img convert --image-opts -O raw"));
        Assert.assertTrue(wrapper.lastDiskSyncScript.contains("file.driver=nbd,file.server.type=unix,file.server.path=$nbd_socket_path"));
        Assert.assertFalse(wrapper.lastDiskSyncScript.contains("qemu-img dd"));
        Assert.assertFalse(wrapper.lastDiskSyncScript.contains("dd if=\"$source_nbd\""));
    }

    @Test
    public void testExecuteWritesChangedBlocksToRawRbdTarget() {
        VmwareCbtDiskTO disk = createDisk("disk-1", "cloudstack-cbt-migration-uuid-disk-1-disk-1", 8192, "raw");
        VmwareCbtSyncCommand command = createCommand(List.of(disk),
                List.of(new VmwareCbtChangedBlockRangeTO("disk-1", 0, 1024)));
        command.setTargetStorageType(VmwareCbtTargetStorageType.RBD_RAW);
        command.setDestinationStoragePoolType(Storage.StoragePoolType.RBD);
        command.setDestinationStoragePoolUuid("rbd-pool-uuid");
        command.setVddkLibDir("/opt/vmware-vddk");
        command.setVddkThumbprint("AA:BB:CC");

        Answer answer = wrapper.execute(command, libvirtComputingResource);

        Assert.assertTrue(answer.getResult());
        Assert.assertTrue(wrapper.lastDiskSyncScript.contains("target_format='raw'"));
        Assert.assertTrue(wrapper.lastDiskSyncScript.contains("rbd:cloudstack/cloudstack-cbt-migration-uuid-disk-1-disk-1"));
        Assert.assertTrue(wrapper.lastDiskSyncScript.contains("qemu-img convert --image-opts -O raw"));
        Assert.assertTrue(wrapper.lastDiskSyncScript.contains("qemu-io -f \"$target_format\""));
    }

    @Test
    public void testExecuteWritesChangedBlocksToRawBlockDeviceTarget() {
        KVMStoragePool linstorStoragePool = Mockito.mock(KVMStoragePool.class);
        com.cloud.hypervisor.kvm.storage.KVMPhysicalDisk targetDisk = Mockito.mock(com.cloud.hypervisor.kvm.storage.KVMPhysicalDisk.class);
        Mockito.when(storagePoolManager.getStoragePool(Storage.StoragePoolType.Linstor, "linstor-pool-uuid")).thenReturn(linstorStoragePool);
        Mockito.when(linstorStoragePool.getType()).thenReturn(Storage.StoragePoolType.Linstor);
        Mockito.when(targetDisk.getPath()).thenReturn("/dev/drbd/by-res/cs-cbt-abc12345-2000/0");
        Mockito.when(linstorStoragePool.getPhysicalDisk("cbt-abc12345-2000")).thenReturn(targetDisk);

        VmwareCbtDiskTO disk = createDisk("disk-1", "cbt-abc12345-2000", 8192, "raw");
        VmwareCbtSyncCommand command = createCommand(List.of(disk),
                List.of(new VmwareCbtChangedBlockRangeTO("disk-1", 0, 1024)));
        command.setTargetStorageType(VmwareCbtTargetStorageType.RAW_BLOCK_DEVICE);
        command.setDestinationStoragePoolType(Storage.StoragePoolType.Linstor);
        command.setDestinationStoragePoolUuid("linstor-pool-uuid");
        command.setVddkLibDir("/opt/vmware-vddk");
        command.setVddkThumbprint("AA:BB:CC");

        Answer answer = wrapper.execute(command, libvirtComputingResource);

        Assert.assertTrue(answer.getDetails(), answer.getResult());
        Assert.assertTrue(wrapper.lastDiskSyncScript.contains("target_format='raw'"));
        Assert.assertTrue(wrapper.lastDiskSyncScript.contains("target_path='/dev/drbd/by-res/cs-cbt-abc12345-2000/0'"));
        Assert.assertTrue(wrapper.lastDiskSyncScript.contains("qemu-io -f \"$target_format\""));
    }

    @Test
    public void testExecuteUsesCommandVddkLibDirOverrideForSupportCheck() throws IOException {
        VmwareCbtDiskTO disk = createDisk("disk-1", temporaryFolder.newFile("disk-1.qcow2").getAbsolutePath(), 8192);
        VmwareCbtSyncCommand command = createCommand(List.of(disk),
                List.of(new VmwareCbtChangedBlockRangeTO("disk-1", 0, 1024)));
        command.setVddkLibDir("/opt/vmware-vddk/override");
        command.setVddkThumbprint("AA:BB:CC");
        Mockito.when(libvirtComputingResource.hostSupportsVddkBlockCopy("/opt/vmware-vddk/override"))
                .thenReturn(true);

        Answer answer = wrapper.execute(command, libvirtComputingResource);

        Assert.assertTrue(answer.getResult());
        Assert.assertTrue(answer.getDetails().contains("copied 1 changed block range"));
        Mockito.verify(libvirtComputingResource).hostSupportsVddkBlockCopy("/opt/vmware-vddk/override");
    }

    @Test
    public void testExecuteReturnsLastCommandOutputOnDeltaFailure() throws IOException {
        wrapper.exitValue = 1;
        wrapper.lastCommandOutput = "qemu-nbd: Failed to blk_new_open 'nbd+unix://?socket=/tmp/nbdkit/socket': Could not open image: Permission denied";
        VmwareCbtDiskTO disk = createDisk("disk-1", temporaryFolder.newFile("disk-1.qcow2").getAbsolutePath(), 8192);
        VmwareCbtSyncCommand command = createCommand(List.of(disk),
                List.of(new VmwareCbtChangedBlockRangeTO("disk-1", 0, 1024)));
        command.setVddkLibDir("/opt/vmware-vddk");
        command.setVddkThumbprint("AA:BB:CC");

        Answer answer = wrapper.execute(command, libvirtComputingResource);

        Assert.assertFalse(answer.getResult());
        Assert.assertTrue(answer.getDetails(), answer.getDetails().contains("Last command output: qemu-nbd"));
        Assert.assertTrue(answer.getDetails(), answer.getDetails().contains("Permission denied"));
    }

    private VmwareCbtSyncCommand createCommand(List<VmwareCbtDiskTO> disks, List<VmwareCbtChangedBlockRangeTO> changedBlocks) {
        return new VmwareCbtSyncCommand("migration-uuid", createRemoteInstance(), disks, changedBlocks, 1,
                "VirtualMachineSnapshot:snapshot-1", false);
    }

    private RemoteInstanceTO createRemoteInstance() {
        return new RemoteInstanceTO("source-vm", null, "vcenter.example.com", "administrator@vsphere.local",
                "password", "Datacenter", "Cluster", "esxi.example.com", "VirtualMachine:vm-1");
    }

    private VmwareCbtDiskTO createDisk(String diskId, String targetPath, long capacityBytes) {
        return createDisk(diskId, targetPath, capacityBytes, "qcow2");
    }

    private VmwareCbtDiskTO createDisk(String diskId, String targetPath, long capacityBytes, String targetFormat) {
        return new VmwareCbtDiskTO(diskId, 2000, String.format("[%s] vm/%s.vmdk", diskId, diskId),
                "datastore1", targetPath, targetFormat, "*", null, capacityBytes);
    }

    private static class TestLibvirtVmwareCbtSyncCommandWrapper extends LibvirtVmwareCbtSyncCommandWrapper {
        private String lastCommand;
        private String lastDiskSyncScript;
        private int exitValue;
        private String lastCommandOutput;

        @Override
        protected VmwareCbtCommandResult executeLoggedBash(String command, long timeout, String logPrefix) {
            lastCommand = command;
            return new VmwareCbtCommandResult(exitValue, lastCommandOutput);
        }

        @Override
        protected String writeDiskSyncScript(VmwareCbtSyncCommand cmd, VmwareCbtSyncPlan.DiskPlan diskPlan,
                                             KVMStoragePool targetPool) throws Exception {
            String scriptPath = super.writeDiskSyncScript(cmd, diskPlan, targetPool);
            lastDiskSyncScript = Files.readString(Path.of(scriptPath));
            return scriptPath;
        }

        @Override
        protected String getVcenterThumbprint(String vcenterHost, long timeout, String sourceVmName) {
            return "AA:BB:CC";
        }

    }
}
