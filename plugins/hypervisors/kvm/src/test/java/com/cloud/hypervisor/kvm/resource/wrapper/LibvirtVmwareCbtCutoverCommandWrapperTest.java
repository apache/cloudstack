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
import com.cloud.agent.api.VmwareCbtCutoverCommand;
import com.cloud.agent.api.VmwareCbtMigrationAnswer;
import com.cloud.agent.api.to.RemoteInstanceTO;
import com.cloud.agent.api.to.VmwareCbtDiskTO;
import com.cloud.agent.api.to.VmwareCbtTargetStorageType;
import com.cloud.hypervisor.kvm.resource.LibvirtComputingResource;
import com.cloud.hypervisor.kvm.storage.KVMStoragePool;
import com.cloud.hypervisor.kvm.storage.KVMStoragePoolManager;
import com.cloud.storage.Storage;

@RunWith(MockitoJUnitRunner.class)
public class LibvirtVmwareCbtCutoverCommandWrapperTest {

    private final TestLibvirtVmwareCbtCutoverCommandWrapper wrapper = new TestLibvirtVmwareCbtCutoverCommandWrapper();

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
        Mockito.when(libvirtComputingResource.getLibguestfsBackend()).thenReturn("direct");
        Mockito.when(libvirtComputingResource.getStoragePoolMgr()).thenReturn(storagePoolManager);
        Mockito.when(storagePoolManager.getStoragePool(Storage.StoragePoolType.RBD, "rbd-pool-uuid")).thenReturn(rbdStoragePool);
        Mockito.when(rbdStoragePool.getType()).thenReturn(Storage.StoragePoolType.RBD);
        Mockito.when(rbdStoragePool.getSourceDir()).thenReturn("cloudstack");
        Mockito.when(rbdStoragePool.getSourceHost()).thenReturn("10.0.0.10,10.0.0.11");
        Mockito.when(rbdStoragePool.getSourcePort()).thenReturn(6789);
        Mockito.when(rbdStoragePool.getAuthUserName()).thenReturn("client.admin");
    }

    @Test
    public void testExecuteRunsVirtV2vInPlaceForSyncedQcow2Disks() throws IOException {
        VmwareCbtCutoverCommand command = createCommand(List.of(
                createDisk("disk-1", temporaryFolder.newFile("disk-1.qcow2").getAbsolutePath()),
                createDisk("disk-2", temporaryFolder.newFile("disk-2.qcow2").getAbsolutePath())));
        command.setWait(1);

        Answer answer = wrapper.execute(command, libvirtComputingResource);

        Assert.assertTrue(answer.getResult());
        Assert.assertTrue(answer.getDetails().contains("virt-v2v-in-place conversion completed"));
        Assert.assertTrue(wrapper.lastCommand.contains("virt-v2v-in-place --root first -i libvirtxml"));
        Assert.assertTrue(wrapper.lastCommand.contains("export LIBGUESTFS_BACKEND='direct'"));
        Assert.assertTrue(wrapper.lastSourceXml.contains("<driver name='qemu' type='qcow2'/>"));
        Assert.assertTrue(wrapper.lastSourceXml.contains("<target dev='sda' bus='scsi'/>"));
        Assert.assertTrue(wrapper.lastSourceXml.contains("<target dev='sdb' bus='scsi'/>"));
        Assert.assertEquals(2, ((VmwareCbtMigrationAnswer) answer).getDiskResults().size());
    }

    @Test
    public void testExecuteRelocatesInPlaceFinalizedDiskToStoragePoolRoot() throws IOException {
        Path poolRoot = temporaryFolder.newFolder("pool-root").toPath();
        Path migrationDir = poolRoot.resolve("cloudstack-cbt").resolve("migration-uuid");
        Files.createDirectories(migrationDir);
        Path sourceDisk = migrationDir.resolve("disk-1.qcow2");
        Files.writeString(sourceDisk, "synced disk");
        VmwareCbtCutoverCommand command = createCommand(List.of(createDisk("disk-1", sourceDisk.toString())));
        command.setWait(1);

        Answer answer = wrapper.execute(command, libvirtComputingResource);

        Assert.assertTrue(answer.getResult());
        VmwareCbtMigrationAnswer migrationAnswer = (VmwareCbtMigrationAnswer) answer;
        Path relocatedPath = Path.of(migrationAnswer.getDiskResults().get(0).getTargetPath());
        Assert.assertEquals(poolRoot, relocatedPath.getParent());
        Assert.assertTrue(Files.exists(relocatedPath));
        Assert.assertFalse(Files.exists(sourceDisk));
    }

    @Test
    public void testExecuteRunsVirtV2vCoreInPlaceOptionForSyncedQcow2Disks() throws IOException {
        wrapper.finalizationMode = LibvirtVmwareCbtCutoverCommandWrapper.VirtV2vFinalizationMode.VIRT_V2V_IN_PLACE_OPTION;
        VmwareCbtCutoverCommand command = createCommand(List.of(createDisk("disk-1",
                temporaryFolder.newFile("disk-1.qcow2").getAbsolutePath())));
        command.setWait(1);

        Answer answer = wrapper.execute(command, libvirtComputingResource);

        Assert.assertTrue(answer.getResult());
        Assert.assertTrue(answer.getDetails().contains("virt-v2v --in-place conversion completed"));
        Assert.assertTrue(wrapper.lastCommand.contains("virt-v2v --root first -i libvirtxml"));
        Assert.assertTrue(wrapper.lastCommand.contains("--in-place -v"));
        Assert.assertFalse(wrapper.lastCommand.contains("virt-v2v-in-place"));
        Assert.assertFalse(wrapper.lastCommand.contains("-O "));
        Assert.assertEquals(1, ((VmwareCbtMigrationAnswer) answer).getDiskResults().size());
    }

    @Test
    public void testExecuteRunsVirtV2vInPlaceForRawRbdDisks() {
        VmwareCbtDiskTO disk = createDisk("disk-1", "cloudstack-cbt-migration-uuid-disk-1-disk-1", "raw");
        VmwareCbtCutoverCommand command = createCommand(List.of(disk));
        command.setTargetStorageType(VmwareCbtTargetStorageType.RBD_RAW);
        command.setDestinationStoragePoolType(Storage.StoragePoolType.RBD);
        command.setDestinationStoragePoolUuid("rbd-pool-uuid");
        command.setWait(1);

        Answer answer = wrapper.execute(command, libvirtComputingResource);

        Assert.assertTrue(answer.getResult());
        Assert.assertTrue(wrapper.lastCommand.startsWith("bash '"));
        Assert.assertTrue(wrapper.lastScript.contains("qemu-nbd --fork --persistent --shared=1 --format=raw"));
        Assert.assertTrue(wrapper.lastSourceXml.contains("<disk type='network' device='disk'>"));
        Assert.assertTrue(wrapper.lastSourceXml.contains("<driver name='qemu' type='raw'/>"));
        Assert.assertTrue(wrapper.lastSourceXml.contains("<source protocol='nbd'>"));
        Assert.assertTrue(wrapper.lastSourceXml.contains("<host name='localhost' port='"));
        Assert.assertFalse(wrapper.lastSourceXml.contains("protocol='rbd'"));
        Assert.assertEquals("cloudstack-cbt-migration-uuid-disk-1-disk-1",
                ((VmwareCbtMigrationAnswer) answer).getDiskResults().get(0).getTargetPath());
    }

    @Test
    public void testExecuteRejectsRbdWhenInPlaceFinalizationIsUnavailable() {
        wrapper.finalizationMode = LibvirtVmwareCbtCutoverCommandWrapper.VirtV2vFinalizationMode.VIRT_V2V_FALLBACK;
        VmwareCbtDiskTO disk = createDisk("disk-1", "cloudstack-cbt-migration-uuid-disk-1-disk-1", "raw");
        VmwareCbtCutoverCommand command = createCommand(List.of(disk));
        command.setTargetStorageType(VmwareCbtTargetStorageType.RBD_RAW);
        command.setDestinationStoragePoolType(Storage.StoragePoolType.RBD);
        command.setDestinationStoragePoolUuid("rbd-pool-uuid");
        command.setAllowNonInPlaceFinalization(true);

        Answer answer = wrapper.execute(command, libvirtComputingResource);

        Assert.assertFalse(answer.getResult());
        Assert.assertTrue(answer.getDetails().contains("RBD target finalization requires virt-v2v in-place support"));
    }

    @Test
    public void testExecuteRejectsMissingTargetDisk() {
        VmwareCbtCutoverCommand command = createCommand(List.of(createDisk("disk-1", "/path/that/does/not/exist.qcow2")));

        Answer answer = wrapper.execute(command, libvirtComputingResource);

        Assert.assertFalse(answer.getResult());
        Assert.assertTrue(answer.getDetails().contains("does not exist"));
    }

    @Test
    public void testExecuteFallsBackToRegularVirtV2vWhenInPlaceIsMissing() throws IOException {
        wrapper.finalizationMode = LibvirtVmwareCbtCutoverCommandWrapper.VirtV2vFinalizationMode.VIRT_V2V_FALLBACK;
        Path sourceDisk = temporaryFolder.newFile("disk-1.qcow2").toPath();
        VmwareCbtCutoverCommand command = createCommand(List.of(createDisk("disk-1", sourceDisk.toString())));
        command.setAllowNonInPlaceFinalization(true);

        Answer answer = wrapper.execute(command, libvirtComputingResource);

        Assert.assertTrue(answer.getResult());
        Assert.assertTrue(answer.getDetails().contains("virt-v2v fallback conversion completed"));
        Assert.assertTrue(wrapper.lastCommand.contains("virt-v2v --root first -i libvirtxml"));
        Assert.assertTrue(wrapper.lastCommand.contains("-o local"));
        Assert.assertTrue(wrapper.lastCommand.contains("export TMPDIR="));
        Assert.assertEquals(1, ((VmwareCbtMigrationAnswer) answer).getDiskResults().size());
        Assert.assertTrue(((VmwareCbtMigrationAnswer) answer).getDiskResults().get(0).getTargetPath().contains("virt-v2v-output"));
        Assert.assertFalse(Files.exists(sourceDisk));
    }

    @Test
    public void testExecuteRelocatesFallbackFinalizedDiskToStoragePoolRoot() throws IOException {
        wrapper.finalizationMode = LibvirtVmwareCbtCutoverCommandWrapper.VirtV2vFinalizationMode.VIRT_V2V_FALLBACK;
        Path poolRoot = temporaryFolder.newFolder("pool-root-fallback").toPath();
        Path migrationDir = poolRoot.resolve("cloudstack-cbt").resolve("migration-uuid");
        Files.createDirectories(migrationDir);
        Path sourceDisk = migrationDir.resolve("disk-1.qcow2");
        Files.writeString(sourceDisk, "synced disk");
        VmwareCbtCutoverCommand command = createCommand(List.of(createDisk("disk-1", sourceDisk.toString())));
        command.setAllowNonInPlaceFinalization(true);

        Answer answer = wrapper.execute(command, libvirtComputingResource);

        Assert.assertTrue(answer.getResult());
        VmwareCbtMigrationAnswer migrationAnswer = (VmwareCbtMigrationAnswer) answer;
        Path relocatedPath = Path.of(migrationAnswer.getDiskResults().get(0).getTargetPath());
        Assert.assertEquals(poolRoot, relocatedPath.getParent());
        Assert.assertTrue(Files.exists(relocatedPath));
        Assert.assertFalse(relocatedPath.toString().contains("virt-v2v-output"));
        Assert.assertFalse(Files.exists(sourceDisk));
    }

    @Test
    public void testExecuteRejectsRegularVirtV2vFallbackWhenNotAllowed() throws IOException {
        wrapper.finalizationMode = LibvirtVmwareCbtCutoverCommandWrapper.VirtV2vFinalizationMode.VIRT_V2V_FALLBACK;
        VmwareCbtCutoverCommand command = createCommand(List.of(createDisk("disk-1",
                temporaryFolder.newFile("disk-1.qcow2").getAbsolutePath())));

        Answer answer = wrapper.execute(command, libvirtComputingResource);

        Assert.assertFalse(answer.getResult());
        Assert.assertTrue(answer.getDetails().contains("cannot finalize VMware CBT migration in-place"));
    }

    @Test
    public void testExecuteReturnsLastCommandOutputOnVirtV2vFailure() throws IOException {
        wrapper.exitValue = 1;
        wrapper.lastCommandOutput = "virt-v2v: error: internal error: assertion failed at linux_kernels.ml, line 185, char 13";
        VmwareCbtCutoverCommand command = createCommand(List.of(createDisk("disk-1",
                temporaryFolder.newFile("disk-1.qcow2").getAbsolutePath())));

        Answer answer = wrapper.execute(command, libvirtComputingResource);

        Assert.assertFalse(answer.getResult());
        Assert.assertTrue(answer.getDetails().contains("Last command output: virt-v2v: error"));
    }

    private VmwareCbtCutoverCommand createCommand(List<VmwareCbtDiskTO> disks) {
        return new VmwareCbtCutoverCommand("migration-uuid", createRemoteInstance(), disks, 2, true);
    }

    private RemoteInstanceTO createRemoteInstance() {
        return new RemoteInstanceTO("source-vm", null, "vcenter.example.com", "administrator@vsphere.local",
                "password", "Datacenter", "Cluster", "esxi.example.com", "VirtualMachine:vm-1");
    }

    private VmwareCbtDiskTO createDisk(String diskId, String targetPath) {
        return createDisk(diskId, targetPath, "qcow2");
    }

    private VmwareCbtDiskTO createDisk(String diskId, String targetPath, String targetFormat) {
        return new VmwareCbtDiskTO(diskId, 2000, String.format("[%s] vm/%s.vmdk", diskId, diskId),
                "datastore1", targetPath, targetFormat, "*", null, 8192);
    }

    private static class TestLibvirtVmwareCbtCutoverCommandWrapper extends LibvirtVmwareCbtCutoverCommandWrapper {
        private VirtV2vFinalizationMode finalizationMode = VirtV2vFinalizationMode.VIRT_V2V_IN_PLACE_BINARY;
        private String lastCommand;
        private String lastScript = "";
        private String lastSourceXml;
        private int exitValue;
        private String lastCommandOutput;

        @Override
        protected VirtV2vFinalizationMode getVirtV2vFinalizationMode(LibvirtComputingResource serverResource) {
            return finalizationMode;
        }

        @Override
        protected VmwareCbtCommandResult executeLoggedBash(String command, long timeout, String logPrefix) {
            lastCommand = command;
            lastScript = readScript(command);
            String commandToInspect = lastScript.isEmpty() ? command : lastScript;
            lastSourceXml = readSourceXml(commandToInspect);
            if (exitValue == 0 && commandToInspect.contains("virt-v2v --root first")) {
                createFallbackOutputDisk(commandToInspect);
            }
            return new VmwareCbtCommandResult(exitValue, lastCommandOutput);
        }

        private void createFallbackOutputDisk(String command) {
            String marker = "-os '";
            int start = command.indexOf(marker);
            if (start < 0) {
                return;
            }
            start += marker.length();
            int end = command.indexOf("'", start);
            if (end < 0) {
                return;
            }
            try {
                Path outputDir = Path.of(command.substring(start, end));
                Files.createDirectories(outputDir);
                Files.writeString(outputDir.resolve("migration-uuid-sda"), "converted disk");
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }

        private String readSourceXml(String command) {
            String marker = "-i libvirtxml '";
            int start = command.indexOf(marker);
            if (start < 0) {
                return "";
            }
            start += marker.length();
            int end = command.indexOf("'", start);
            if (end < 0) {
                return "";
            }
            try {
                return Files.readString(Path.of(command.substring(start, end)));
            } catch (IOException e) {
                return "";
            }
        }

        private String readScript(String command) {
            String marker = "bash '";
            int start = command.indexOf(marker);
            if (start < 0) {
                return "";
            }
            start += marker.length();
            int end = command.indexOf("'", start);
            if (end < 0) {
                return "";
            }
            try {
                return Files.readString(Path.of(command.substring(start, end)));
            } catch (IOException e) {
                return "";
            }
        }
    }
}
