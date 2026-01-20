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

import com.cloud.agent.api.Answer;
import com.cloud.hypervisor.kvm.resource.LibvirtComputingResource;
import com.cloud.utils.script.Script;
import com.cloud.vm.VirtualMachine;
import org.apache.cloudstack.backup.BackupAnswer;
import org.apache.cloudstack.backup.RestoreBackupCommand;
import org.apache.cloudstack.storage.to.PrimaryDataStoreTO;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.mockito.junit.MockitoJUnitRunner;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class LibvirtRestoreBackupCommandWrapperTest {

    private LibvirtRestoreBackupCommandWrapper wrapper;
    private LibvirtComputingResource libvirtComputingResource;
    private RestoreBackupCommand command;

    @Before
    public void setUp() {
        wrapper = new LibvirtRestoreBackupCommandWrapper();
        libvirtComputingResource = Mockito.mock(LibvirtComputingResource.class);
        command = Mockito.mock(RestoreBackupCommand.class);
    }

    @Test
    public void testExecuteWithVmExistsNull() throws Exception {
        when(command.getVmName()).thenReturn("test-vm");
        when(command.getBackupPath()).thenReturn("backup/path");
        when(command.getBackupRepoAddress()).thenReturn("192.168.1.100:/backup");
        when(command.getBackupRepoType()).thenReturn("nfs");
        when(command.getMountOptions()).thenReturn("rw");
        when(command.isVmExists()).thenReturn(null);
        when(command.getDiskType()).thenReturn("root");
        PrimaryDataStoreTO primaryDataStore = Mockito.mock(PrimaryDataStoreTO.class);
        when(command.getRestoreVolumePools()).thenReturn(Arrays.asList(primaryDataStore));
        when(command.getRestoreVolumePaths()).thenReturn(Arrays.asList("/var/lib/libvirt/images/volume-123"));
        when(command.getRestoreVolumeUUID()).thenReturn("volume-123");
        when(command.getVmState()).thenReturn(VirtualMachine.State.Running);
        when(command.getMountTimeout()).thenReturn(30);

        try (MockedStatic<Files> filesMock = mockStatic(Files.class)) {
            Path tempPath = Mockito.mock(Path.class);
            when(tempPath.toString()).thenReturn("/tmp/csbackup.abc123");
            filesMock.when(() -> Files.createTempDirectory(anyString())).thenReturn(tempPath);

            try (MockedStatic<Script> scriptMock = mockStatic(Script.class)) {
                scriptMock.when(() -> Script.runSimpleBashScriptForExitValue(anyString(), anyInt(), any(Boolean.class)))
                        .thenReturn(0); // Mount success
                scriptMock.when(() -> Script.runSimpleBashScriptForExitValue(anyString()))
                        .thenReturn(0); // Other commands success
                scriptMock.when(() -> Script.runSimpleBashScript(anyString()))
                        .thenReturn("vda"); // Current device

                filesMock.when(() -> Files.deleteIfExists(any(Path.class))).thenReturn(true);

                Answer result = wrapper.execute(command, libvirtComputingResource);

                Assert.assertNotNull(result);
                Assert.assertTrue(result instanceof BackupAnswer);
                BackupAnswer backupAnswer = (BackupAnswer) result;
                Assert.assertTrue(backupAnswer.getResult());
                Assert.assertEquals("volume-123", backupAnswer.getDetails());
            }
        }
    }

    @Test
    public void testExecuteWithVmExistsTrue() throws Exception {
        when(command.getVmName()).thenReturn("test-vm");
        when(command.getBackupPath()).thenReturn("backup/path");
        when(command.getBackupRepoAddress()).thenReturn("192.168.1.100:/backup");
        when(command.getBackupRepoType()).thenReturn("nfs");
        when(command.getMountOptions()).thenReturn("rw");
        when(command.isVmExists()).thenReturn(true);
        when(command.getDiskType()).thenReturn("root");
        PrimaryDataStoreTO primaryDataStore = Mockito.mock(PrimaryDataStoreTO.class);
        when(command.getRestoreVolumePools()).thenReturn(Arrays.asList(primaryDataStore));
        when(command.getRestoreVolumePaths()).thenReturn(Arrays.asList("/var/lib/libvirt/images/volume-123"));
        when(command.getBackupVolumesUUIDs()).thenReturn(Arrays.asList("volume-123"));
        when(command.getMountTimeout()).thenReturn(30);

        try (MockedStatic<Files> filesMock = mockStatic(Files.class)) {
            Path tempPath = Mockito.mock(Path.class);
            when(tempPath.toString()).thenReturn("/tmp/csbackup.abc123");
            filesMock.when(() -> Files.createTempDirectory(anyString())).thenReturn(tempPath);

            try (MockedStatic<Script> scriptMock = mockStatic(Script.class)) {
                scriptMock.when(() -> Script.runSimpleBashScriptForExitValue(anyString(), anyInt(), any(Boolean.class)))
                        .thenReturn(0); // Mount success
                scriptMock.when(() -> Script.runSimpleBashScriptForExitValue(anyString()))
                        .thenReturn(0); // Other commands success

                filesMock.when(() -> Files.deleteIfExists(any(Path.class))).thenReturn(true);

                Answer result = wrapper.execute(command, libvirtComputingResource);

                Assert.assertNotNull(result);
                Assert.assertTrue(result instanceof BackupAnswer);
                BackupAnswer backupAnswer = (BackupAnswer) result;
                Assert.assertTrue(backupAnswer.getResult());
            }
        }
    }

    @Test
    public void testExecuteWithVmExistsFalse() throws Exception {
        when(command.getVmName()).thenReturn("test-vm");
        when(command.getBackupPath()).thenReturn("backup/path");
        when(command.getBackupRepoAddress()).thenReturn("192.168.1.100:/backup");
        when(command.getBackupRepoType()).thenReturn("nfs");
        when(command.getMountOptions()).thenReturn("rw");
        when(command.isVmExists()).thenReturn(false);
        when(command.getDiskType()).thenReturn("root");
        PrimaryDataStoreTO primaryDataStore = Mockito.mock(PrimaryDataStoreTO.class);
        when(command.getRestoreVolumePools()).thenReturn(Arrays.asList(primaryDataStore));
        when(command.getRestoreVolumePaths()).thenReturn(Arrays.asList("/var/lib/libvirt/images/volume-123"));
        when(command.getMountTimeout()).thenReturn(30);

        try (MockedStatic<Files> filesMock = mockStatic(Files.class)) {
            Path tempPath = Mockito.mock(Path.class);
            when(tempPath.toString()).thenReturn("/tmp/csbackup.abc123");
            filesMock.when(() -> Files.createTempDirectory(anyString())).thenReturn(tempPath);

            try (MockedStatic<Script> scriptMock = mockStatic(Script.class)) {
                scriptMock.when(() -> Script.runSimpleBashScriptForExitValue(anyString(), anyInt(), any(Boolean.class)))
                        .thenReturn(0); // Mount success
                scriptMock.when(() -> Script.runSimpleBashScriptForExitValue(anyString()))
                        .thenReturn(0); // Other commands success

                filesMock.when(() -> Files.deleteIfExists(any(Path.class))).thenReturn(true);

                Answer result = wrapper.execute(command, libvirtComputingResource);

                Assert.assertNotNull(result);
                Assert.assertTrue(result instanceof BackupAnswer);
                BackupAnswer backupAnswer = (BackupAnswer) result;
                Assert.assertTrue(backupAnswer.getResult());
            }
        }
    }

    @Test
    public void testExecuteWithCifsMountType() throws Exception {
        when(command.getVmName()).thenReturn("test-vm");
        when(command.getBackupPath()).thenReturn("backup/path");
        when(command.getBackupRepoAddress()).thenReturn("//192.168.1.100/backup");
        when(command.getBackupRepoType()).thenReturn("cifs");
        when(command.getMountOptions()).thenReturn("username=user,password=pass");
        when(command.isVmExists()).thenReturn(null);
        when(command.getDiskType()).thenReturn("root");
        PrimaryDataStoreTO primaryDataStore = Mockito.mock(PrimaryDataStoreTO.class);
        when(command.getRestoreVolumePools()).thenReturn(Arrays.asList(primaryDataStore));
        when(command.getRestoreVolumePaths()).thenReturn(Arrays.asList("/var/lib/libvirt/images/volume-123"));
        when(command.getRestoreVolumeUUID()).thenReturn("volume-123");
        when(command.getVmState()).thenReturn(VirtualMachine.State.Running);
        when(command.getMountTimeout()).thenReturn(30);

        try (MockedStatic<Files> filesMock = mockStatic(Files.class)) {
            Path tempPath = Mockito.mock(Path.class);
            when(tempPath.toString()).thenReturn("/tmp/csbackup.abc123");
            filesMock.when(() -> Files.createTempDirectory(anyString())).thenReturn(tempPath);

            try (MockedStatic<Script> scriptMock = mockStatic(Script.class)) {
                scriptMock.when(() -> Script.runSimpleBashScriptForExitValue(anyString(), anyInt(), any(Boolean.class)))
                        .thenReturn(0); // Mount success
                scriptMock.when(() -> Script.runSimpleBashScriptForExitValue(anyString()))
                        .thenReturn(0); // Other commands success
                scriptMock.when(() -> Script.runSimpleBashScript(anyString()))
                        .thenReturn("vda"); // Current device

                filesMock.when(() -> Files.deleteIfExists(any(Path.class))).thenReturn(true);

                Answer result = wrapper.execute(command, libvirtComputingResource);

                Assert.assertNotNull(result);
                Assert.assertTrue(result instanceof BackupAnswer);
                BackupAnswer backupAnswer = (BackupAnswer) result;
                Assert.assertTrue(backupAnswer.getResult());
            }
        }
    }

    @Test
    public void testExecuteWithMountFailure() throws Exception {
        lenient().when(command.getVmName()).thenReturn("test-vm");
        lenient().when(command.getBackupPath()).thenReturn("backup/path");
        lenient().when(command.getBackupRepoAddress()).thenReturn("192.168.1.100:/backup");
        lenient().when(command.getBackupRepoType()).thenReturn("nfs");
        lenient().when(command.getMountOptions()).thenReturn("rw");
        lenient().when(command.isVmExists()).thenReturn(null);
        lenient().when(command.getDiskType()).thenReturn("root");
        PrimaryDataStoreTO primaryDataStore = Mockito.mock(PrimaryDataStoreTO.class);
        when(command.getRestoreVolumePools()).thenReturn(Arrays.asList(primaryDataStore));
        lenient().when(command.getRestoreVolumePaths()).thenReturn(Arrays.asList("/var/lib/libvirt/images/volume-123"));
        lenient().when(command.getRestoreVolumeUUID()).thenReturn("volume-123");
        lenient().when(command.getVmState()).thenReturn(VirtualMachine.State.Running);
        lenient().when(command.getMountTimeout()).thenReturn(30);

        try (MockedStatic<Files> filesMock = mockStatic(Files.class)) {
            Path tempPath = Mockito.mock(Path.class);
            when(tempPath.toString()).thenReturn("/tmp/csbackup.abc123");
            filesMock.when(() -> Files.createTempDirectory(anyString())).thenReturn(tempPath);

            try (MockedStatic<Script> scriptMock = mockStatic(Script.class)) {
                scriptMock.when(() -> Script.runSimpleBashScriptForExitValue(anyString(), anyInt(), any(Boolean.class)))
                        .thenReturn(1); // Mount failure

                Answer result = wrapper.execute(command, libvirtComputingResource);

                Assert.assertNotNull(result);
                Assert.assertTrue(result instanceof BackupAnswer);
                BackupAnswer backupAnswer = (BackupAnswer) result;
                Assert.assertFalse(backupAnswer.getResult());
                Assert.assertTrue(backupAnswer.getDetails().contains("Failed to mount the backup repository"));
            }
        }
    }

    @Test
    public void testExecuteWithBackupFileNotFound() throws Exception {
        when(command.getVmName()).thenReturn("test-vm");
        when(command.getBackupPath()).thenReturn("backup/path");
        when(command.getBackupRepoAddress()).thenReturn("192.168.1.100:/backup");
        when(command.getBackupRepoType()).thenReturn("nfs");
        when(command.getMountOptions()).thenReturn("rw");
        when(command.isVmExists()).thenReturn(null);
        when(command.getDiskType()).thenReturn("root");
        PrimaryDataStoreTO primaryDataStore = Mockito.mock(PrimaryDataStoreTO.class);
        when(command.getRestoreVolumePools()).thenReturn(Arrays.asList(primaryDataStore));
        when(command.getRestoreVolumePaths()).thenReturn(Arrays.asList("/var/lib/libvirt/images/volume-123"));
        when(command.getRestoreVolumeUUID()).thenReturn("volume-123");
        when(command.getVmState()).thenReturn(VirtualMachine.State.Running);
        when(command.getMountTimeout()).thenReturn(30);

        try (MockedStatic<Files> filesMock = mockStatic(Files.class)) {
            Path tempPath = Mockito.mock(Path.class);
            when(tempPath.toString()).thenReturn("/tmp/csbackup.abc123");
            filesMock.when(() -> Files.createTempDirectory(anyString())).thenReturn(tempPath);

            try (MockedStatic<Script> scriptMock = mockStatic(Script.class)) {
                scriptMock.when(() -> Script.runSimpleBashScriptForExitValue(anyString(), anyInt(), any(Boolean.class)))
                        .thenReturn(0); // Mount success
                scriptMock.when(() -> Script.runSimpleBashScriptForExitValue(anyString()))
                        .thenAnswer(invocation -> {
                            String command = invocation.getArgument(0);
                            if (command.contains("ls ")) {
                                return 1; // File not found
                            }
                            return 0; // Other commands success
                        });

                filesMock.when(() -> Files.deleteIfExists(any(Path.class))).thenReturn(true);

                Answer result = wrapper.execute(command, libvirtComputingResource);

                Assert.assertNotNull(result);
                Assert.assertTrue(result instanceof BackupAnswer);
                BackupAnswer backupAnswer = (BackupAnswer) result;
                Assert.assertFalse(backupAnswer.getResult());
                Assert.assertTrue(backupAnswer.getDetails().contains("Backup file for the volume [volume-123] does not exist"));
            }
        }
    }

    @Test
    public void testExecuteWithCorruptBackupFile() throws Exception {
        when(command.getVmName()).thenReturn("test-vm");
        when(command.getBackupPath()).thenReturn("backup/path");
        when(command.getBackupRepoAddress()).thenReturn("192.168.1.100:/backup");
        when(command.getBackupRepoType()).thenReturn("nfs");
        when(command.getMountOptions()).thenReturn("rw");
        when(command.isVmExists()).thenReturn(null);
        when(command.getDiskType()).thenReturn("root");
        PrimaryDataStoreTO primaryDataStore = Mockito.mock(PrimaryDataStoreTO.class);
        when(command.getRestoreVolumePools()).thenReturn(Arrays.asList(primaryDataStore));
        when(command.getRestoreVolumePaths()).thenReturn(Arrays.asList("/var/lib/libvirt/images/volume-123"));
        when(command.getRestoreVolumeUUID()).thenReturn("volume-123");
        when(command.getVmState()).thenReturn(VirtualMachine.State.Running);
        when(command.getMountTimeout()).thenReturn(30);

        try (MockedStatic<Files> filesMock = mockStatic(Files.class)) {
            Path tempPath = Mockito.mock(Path.class);
            when(tempPath.toString()).thenReturn("/tmp/csbackup.abc123");
            filesMock.when(() -> Files.createTempDirectory(anyString())).thenReturn(tempPath);

            try (MockedStatic<Script> scriptMock = mockStatic(Script.class)) {
                scriptMock.when(() -> Script.runSimpleBashScriptForExitValue(anyString(), anyInt(), any(Boolean.class)))
                        .thenReturn(0); // Mount success
                scriptMock.when(() -> Script.runSimpleBashScriptForExitValue(anyString()))
                        .thenAnswer(invocation -> {
                            String command = invocation.getArgument(0);
                            if (command.contains("ls ")) {
                                return 0; // File exists
                            } else if (command.contains("qemu-img check")) {
                                return 1; // Corrupt file
                            }
                            return 0; // Other commands success
                        });

                filesMock.when(() -> Files.deleteIfExists(any(Path.class))).thenReturn(true);

                Answer result = wrapper.execute(command, libvirtComputingResource);

                Assert.assertNotNull(result);
                Assert.assertTrue(result instanceof BackupAnswer);
                BackupAnswer backupAnswer = (BackupAnswer) result;
                Assert.assertFalse(backupAnswer.getResult());
                Assert.assertTrue(backupAnswer.getDetails().contains("Backup qcow2 file for the volume [volume-123] is corrupt"));
            }
        }
    }

    @Test
    public void testExecuteWithRsyncFailure() throws Exception {
        when(command.getVmName()).thenReturn("test-vm");
        when(command.getBackupPath()).thenReturn("backup/path");
        when(command.getBackupRepoAddress()).thenReturn("192.168.1.100:/backup");
        when(command.getBackupRepoType()).thenReturn("nfs");
        when(command.getMountOptions()).thenReturn("rw");
        when(command.isVmExists()).thenReturn(null);
        when(command.getDiskType()).thenReturn("root");
        PrimaryDataStoreTO primaryDataStore = Mockito.mock(PrimaryDataStoreTO.class);
        when(command.getRestoreVolumePools()).thenReturn(Arrays.asList(primaryDataStore));
        when(command.getRestoreVolumePaths()).thenReturn(Arrays.asList("/var/lib/libvirt/images/volume-123"));
        when(command.getRestoreVolumeUUID()).thenReturn("volume-123");
        when(command.getVmState()).thenReturn(VirtualMachine.State.Running);
        when(command.getMountTimeout()).thenReturn(30);

        try (MockedStatic<Files> filesMock = mockStatic(Files.class)) {
            Path tempPath = Mockito.mock(Path.class);
            when(tempPath.toString()).thenReturn("/tmp/csbackup.abc123");
            filesMock.when(() -> Files.createTempDirectory(anyString())).thenReturn(tempPath);

            try (MockedStatic<Script> scriptMock = mockStatic(Script.class)) {
                scriptMock.when(() -> Script.runSimpleBashScriptForExitValue(anyString(), anyInt(), any(Boolean.class)))
                        .thenReturn(0); // Mount success
                scriptMock.when(() -> Script.runSimpleBashScriptForExitValue(anyString()))
                        .thenAnswer(invocation -> {
                            String command = invocation.getArgument(0);
                            if (command.contains("ls ")) {
                                return 0; // File exists
                            } else if (command.contains("qemu-img check")) {
                                return 0; // File is valid
                            } else if (command.contains("rsync")) {
                                return 1; // Rsync failure
                            }
                            return 0; // Other commands success
                        });

                filesMock.when(() -> Files.deleteIfExists(any(Path.class))).thenReturn(true);

                Answer result = wrapper.execute(command, libvirtComputingResource);

                Assert.assertNotNull(result);
                Assert.assertTrue(result instanceof BackupAnswer);
                BackupAnswer backupAnswer = (BackupAnswer) result;
                Assert.assertFalse(backupAnswer.getResult());
                Assert.assertTrue(backupAnswer.getDetails().contains("Unable to restore contents from the backup volume [volume-123]"));
            }
        }
    }

    @Test
    public void testExecuteWithAttachVolumeFailure() throws Exception {
        when(command.getVmName()).thenReturn("test-vm");
        when(command.getBackupPath()).thenReturn("backup/path");
        when(command.getBackupRepoAddress()).thenReturn("192.168.1.100:/backup");
        when(command.getBackupRepoType()).thenReturn("nfs");
        when(command.getMountOptions()).thenReturn("rw");
        when(command.isVmExists()).thenReturn(null);
        when(command.getDiskType()).thenReturn("root");
        PrimaryDataStoreTO primaryDataStore = Mockito.mock(PrimaryDataStoreTO.class);
        when(command.getRestoreVolumePools()).thenReturn(Arrays.asList(primaryDataStore));
        when(command.getRestoreVolumePaths()).thenReturn(Arrays.asList("/var/lib/libvirt/images/volume-123"));
        when(command.getRestoreVolumeUUID()).thenReturn("volume-123");
        when(command.getVmState()).thenReturn(VirtualMachine.State.Running);
        when(command.getMountTimeout()).thenReturn(30);

        try (MockedStatic<Files> filesMock = mockStatic(Files.class)) {
            Path tempPath = Mockito.mock(Path.class);
            when(tempPath.toString()).thenReturn("/tmp/csbackup.abc123");
            filesMock.when(() -> Files.createTempDirectory(anyString())).thenReturn(tempPath);

            try (MockedStatic<Script> scriptMock = mockStatic(Script.class)) {
                scriptMock.when(() -> Script.runSimpleBashScriptForExitValue(anyString(), anyInt(), any(Boolean.class)))
                        .thenReturn(0); // Mount success
                scriptMock.when(() -> Script.runSimpleBashScriptForExitValue(anyString()))
                        .thenAnswer(invocation -> {
                            String command = invocation.getArgument(0);
                            if (command.contains("ls ")) {
                                return 0; // File exists
                            } else if (command.contains("qemu-img check")) {
                                return 0; // File is valid
                            } else if (command.contains("rsync")) {
                                return 0; // Rsync success
                            } else if (command.contains("virsh attach-disk")) {
                                return 1; // Attach failure
                            }
                            return 0; // Other commands success
                        });
                scriptMock.when(() -> Script.runSimpleBashScript(anyString()))
                        .thenReturn("vda"); // Current device

                filesMock.when(() -> Files.deleteIfExists(any(Path.class))).thenReturn(true);

                Answer result = wrapper.execute(command, libvirtComputingResource);

                Assert.assertNotNull(result);
                Assert.assertTrue(result instanceof BackupAnswer);
                BackupAnswer backupAnswer = (BackupAnswer) result;
                Assert.assertFalse(backupAnswer.getResult());
                Assert.assertTrue(backupAnswer.getDetails().contains("Failed to attach volume to VM: test-vm"));
            }
        }
    }

    @Test
    public void testExecuteWithTempDirectoryCreationFailure() throws Exception {
        lenient().when(command.getVmName()).thenReturn("test-vm");
        lenient().when(command.getBackupPath()).thenReturn("backup/path");
        lenient().when(command.getBackupRepoAddress()).thenReturn("192.168.1.100:/backup");
        lenient().when(command.getBackupRepoType()).thenReturn("nfs");
        lenient().when(command.getMountOptions()).thenReturn("rw");
        lenient().when(command.isVmExists()).thenReturn(null);
        lenient().when(command.getDiskType()).thenReturn("root");
        PrimaryDataStoreTO primaryDataStore = Mockito.mock(PrimaryDataStoreTO.class);
        when(command.getRestoreVolumePools()).thenReturn(Arrays.asList(primaryDataStore));
        lenient().when(command.getRestoreVolumePaths()).thenReturn(Arrays.asList("/var/lib/libvirt/images/volume-123"));
        lenient().when(command.getRestoreVolumeUUID()).thenReturn("volume-123");
        lenient().when(command.getVmState()).thenReturn(VirtualMachine.State.Running);
        lenient().when(command.getMountTimeout()).thenReturn(30);

        try (MockedStatic<Files> filesMock = mockStatic(Files.class)) {
            filesMock.when(() -> Files.createTempDirectory(anyString()))
                    .thenThrow(new IOException("Failed to create temp directory"));

            Answer result = wrapper.execute(command, libvirtComputingResource);

            Assert.assertNotNull(result);
            Assert.assertTrue(result instanceof BackupAnswer);
            BackupAnswer backupAnswer = (BackupAnswer) result;
            Assert.assertFalse(backupAnswer.getResult());
            Assert.assertTrue(backupAnswer.getDetails().contains("Failed to create the tmp mount directory for restore"));
        }
    }

    @Test
    public void testExecuteWithMultipleVolumes() throws Exception {
        when(command.getVmName()).thenReturn("test-vm");
        when(command.getBackupPath()).thenReturn("backup/path");
        when(command.getBackupRepoAddress()).thenReturn("192.168.1.100:/backup");
        when(command.getBackupRepoType()).thenReturn("nfs");
        when(command.getMountOptions()).thenReturn("rw");
        when(command.isVmExists()).thenReturn(true);
        when(command.getDiskType()).thenReturn("root");
        PrimaryDataStoreTO primaryDataStore1 = Mockito.mock(PrimaryDataStoreTO.class);
        PrimaryDataStoreTO primaryDataStore2 = Mockito.mock(PrimaryDataStoreTO.class);
        when(command.getRestoreVolumePools()).thenReturn(Arrays.asList(
                primaryDataStore1,
                primaryDataStore2
        ));
        when(command.getRestoreVolumePaths()).thenReturn(Arrays.asList(
                "/var/lib/libvirt/images/volume-123",
                "/var/lib/libvirt/images/volume-456"
        ));
        when(command.getBackupVolumesUUIDs()).thenReturn(Arrays.asList("volume-123", "volume-456"));
        when(command.getMountTimeout()).thenReturn(30);

        try (MockedStatic<Files> filesMock = mockStatic(Files.class)) {
            Path tempPath = Mockito.mock(Path.class);
            when(tempPath.toString()).thenReturn("/tmp/csbackup.abc123");
            filesMock.when(() -> Files.createTempDirectory(anyString())).thenReturn(tempPath);

            try (MockedStatic<Script> scriptMock = mockStatic(Script.class)) {
                scriptMock.when(() -> Script.runSimpleBashScriptForExitValue(anyString(), anyInt(), any(Boolean.class)))
                        .thenReturn(0); // Mount success
                scriptMock.when(() -> Script.runSimpleBashScriptForExitValue(anyString()))
                        .thenReturn(0); // All other commands success

                filesMock.when(() -> Files.deleteIfExists(any(Path.class))).thenReturn(true);

                Answer result = wrapper.execute(command, libvirtComputingResource);

                Assert.assertNotNull(result);
                Assert.assertTrue(result instanceof BackupAnswer);
                BackupAnswer backupAnswer = (BackupAnswer) result;
                Assert.assertTrue(backupAnswer.getResult());
            }
        }
    }
}
