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

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

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

import com.cloud.agent.api.Answer;
import com.cloud.hypervisor.kvm.resource.LibvirtComputingResource;
import com.cloud.storage.Storage;
import com.cloud.utils.Pair;
import com.cloud.utils.script.Script;
import com.cloud.vm.VirtualMachine;

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
        when(command.getRestoreVolumeSizes()).thenReturn(Arrays.asList(1024L));
        when(command.getWait()).thenReturn(60);
        PrimaryDataStoreTO primaryDataStore = Mockito.mock(PrimaryDataStoreTO.class);
        when(primaryDataStore.getPoolType()).thenReturn(Storage.StoragePoolType.NetworkFilesystem);
        when(command.getRestoreVolumePools()).thenReturn(Arrays.asList(primaryDataStore));
        when(command.getRestoreVolumePaths()).thenReturn(Arrays.asList("/var/lib/libvirt/images/volume-123"));
        when(command.getBackupFiles()).thenReturn(Arrays.asList("volume-123"));
        when(command.getVmState()).thenReturn(VirtualMachine.State.Running);
        when(command.getMountTimeout()).thenReturn(30);

        try (MockedStatic<Files> filesMock = mockStatic(Files.class)) {
            Path tempPath = Mockito.mock(Path.class);
            when(tempPath.toString()).thenReturn("/tmp/csbackup.abc123");
            filesMock.when(() -> Files.createTempDirectory(anyString())).thenReturn(tempPath);

            try (MockedStatic<Script> scriptMock = mockStatic(Script.class)) {
                scriptMock.when(() -> Script.getExecutableAbsolutePath(anyString()))
                        .thenAnswer(invocation -> invocation.getArgument(0));
                scriptMock.when(() -> Script.executeCommand(any(String[].class)))
                        .thenReturn(null);
                scriptMock.when(() -> Script.executeCommandForExitValue(any(String[].class)))
                        .thenReturn(0);
                scriptMock.when(() -> Script.runSimpleBashScriptForExitValue(anyString()))
                        .thenReturn(0); // Other commands success
                scriptMock.when(() -> Script.executePipedCommands(anyList(), anyLong()))
                        .thenReturn(new Pair<>(0, "vda"));

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
        when(primaryDataStore.getPoolType()).thenReturn(Storage.StoragePoolType.NetworkFilesystem);
        when(command.getRestoreVolumePools()).thenReturn(Arrays.asList(primaryDataStore));
        when(command.getRestoreVolumePaths()).thenReturn(Arrays.asList("/var/lib/libvirt/images/volume-123"));
        when(command.getBackupVolumesUUIDs()).thenReturn(Arrays.asList("volume-123"));
        when(command.getBackupFiles()).thenReturn(Arrays.asList("volume-123"));
        when(command.getMountTimeout()).thenReturn(30);

        try (MockedStatic<Files> filesMock = mockStatic(Files.class)) {
            Path tempPath = Mockito.mock(Path.class);
            when(tempPath.toString()).thenReturn("/tmp/csbackup.abc123");
            filesMock.when(() -> Files.createTempDirectory(anyString())).thenReturn(tempPath);

            try (MockedStatic<Script> scriptMock = mockStatic(Script.class)) {
                scriptMock.when(() -> Script.executeCommand(any(String[].class)))
                        .thenReturn(null);
                scriptMock.when(() -> Script.executeCommandForExitValue(any(String[].class)))
                        .thenReturn(0);
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
        when(primaryDataStore.getPoolType()).thenReturn(Storage.StoragePoolType.NetworkFilesystem);
        when(command.getRestoreVolumePools()).thenReturn(Arrays.asList(primaryDataStore));
        when(command.getRestoreVolumePaths()).thenReturn(Arrays.asList("/var/lib/libvirt/images/volume-123"));
        when(command.getBackupFiles()).thenReturn(Arrays.asList("volume-123"));
        when(command.getMountTimeout()).thenReturn(30);

        try (MockedStatic<Files> filesMock = mockStatic(Files.class)) {
            Path tempPath = Mockito.mock(Path.class);
            when(tempPath.toString()).thenReturn("/tmp/csbackup.abc123");
            filesMock.when(() -> Files.createTempDirectory(anyString())).thenReturn(tempPath);

            try (MockedStatic<Script> scriptMock = mockStatic(Script.class)) {
                scriptMock.when(() -> Script.executeCommand(any(String[].class)))
                        .thenReturn(null);
                scriptMock.when(() -> Script.executeCommandForExitValue(any(String[].class)))
                        .thenReturn(0);
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
        when(command.getRestoreVolumeSizes()).thenReturn(Arrays.asList(1024L));
        when(command.getWait()).thenReturn(60);
        PrimaryDataStoreTO primaryDataStore = Mockito.mock(PrimaryDataStoreTO.class);
        when(primaryDataStore.getPoolType()).thenReturn(Storage.StoragePoolType.NetworkFilesystem);
        when(command.getRestoreVolumePools()).thenReturn(Arrays.asList(primaryDataStore));
        when(command.getRestoreVolumePaths()).thenReturn(Arrays.asList("/var/lib/libvirt/images/volume-123"));
        when(command.getBackupFiles()).thenReturn(Arrays.asList("volume-123"));
        when(command.getVmState()).thenReturn(VirtualMachine.State.Running);
        when(command.getMountTimeout()).thenReturn(30);

        try (MockedStatic<Files> filesMock = mockStatic(Files.class)) {
            Path tempPath = Mockito.mock(Path.class);
            when(tempPath.toString()).thenReturn("/tmp/csbackup.abc123");
            filesMock.when(() -> Files.createTempDirectory(anyString())).thenReturn(tempPath);

            try (MockedStatic<Script> scriptMock = mockStatic(Script.class)) {
                scriptMock.when(() -> Script.getExecutableAbsolutePath(Mockito.anyString()))
                        .thenAnswer(invocation -> invocation.getArgument(0));
                scriptMock.when(() ->
                                Script.executePipedCommands(anyList(), anyLong()))
                        .thenReturn(new Pair<>(0, "vda")); // Current device

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
        lenient().when(command.getRestoreVolumeSizes()).thenReturn(Arrays.asList(1024L));
        lenient().when(command.getWait()).thenReturn(60);
        PrimaryDataStoreTO primaryDataStore = Mockito.mock(PrimaryDataStoreTO.class);
        lenient().when(primaryDataStore.getPoolType()).thenReturn(Storage.StoragePoolType.NetworkFilesystem);
        when(command.getRestoreVolumePools()).thenReturn(Arrays.asList(primaryDataStore));
        lenient().when(command.getRestoreVolumePaths()).thenReturn(Arrays.asList("/var/lib/libvirt/images/volume-123"));
        when(command.getBackupFiles()).thenReturn(Arrays.asList("volume-123"));
        lenient().when(command.getVmState()).thenReturn(VirtualMachine.State.Running);
        lenient().when(command.getMountTimeout()).thenReturn(30);

        try (MockedStatic<Files> filesMock = mockStatic(Files.class)) {
            Path tempPath = Mockito.mock(Path.class);
            when(tempPath.toString()).thenReturn("/tmp/csbackup.abc123");
            filesMock.when(() -> Files.createTempDirectory(anyString())).thenReturn(tempPath);

            try (MockedStatic<Script> scriptMock = mockStatic(Script.class)) {
                scriptMock.when(() -> Script.executeCommand(any(String[].class)))
                        .thenThrow(new RuntimeException("failure")); // Mount failure

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
        when(command.getRestoreVolumeSizes()).thenReturn(Arrays.asList(1024L));
        when(command.getWait()).thenReturn(60);
        PrimaryDataStoreTO primaryDataStore = Mockito.mock(PrimaryDataStoreTO.class);
        when(primaryDataStore.getPoolType()).thenReturn(Storage.StoragePoolType.NetworkFilesystem);
        when(command.getRestoreVolumePools()).thenReturn(Arrays.asList(primaryDataStore));
        when(command.getRestoreVolumePaths()).thenReturn(Arrays.asList("/var/lib/libvirt/images/volume-123"));
        when(command.getBackupFiles()).thenReturn(Arrays.asList("volume-123"));
        when(command.getVmState()).thenReturn(VirtualMachine.State.Running);
        when(command.getMountTimeout()).thenReturn(30);

        try (MockedStatic<Files> filesMock = mockStatic(Files.class)) {
            Path tempPath = Mockito.mock(Path.class);
            when(tempPath.toString()).thenReturn("/tmp/csbackup.abc123");
            filesMock.when(() -> Files.createTempDirectory(anyString())).thenReturn(tempPath);

            try (MockedStatic<Script> scriptMock = mockStatic(Script.class)) {
                scriptMock.when(() -> Script.executeCommand(any(String[].class)))
                        .thenReturn(null);
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
        when(command.getRestoreVolumeSizes()).thenReturn(Arrays.asList(1024L));
        when(command.getWait()).thenReturn(60);
        PrimaryDataStoreTO primaryDataStore = Mockito.mock(PrimaryDataStoreTO.class);
        when(primaryDataStore.getPoolType()).thenReturn(Storage.StoragePoolType.NetworkFilesystem);
        when(command.getRestoreVolumePools()).thenReturn(Arrays.asList(primaryDataStore));
        when(command.getRestoreVolumePaths()).thenReturn(Arrays.asList("/var/lib/libvirt/images/volume-123"));
        when(command.getBackupFiles()).thenReturn(Arrays.asList("volume-123"));
        when(command.getVmState()).thenReturn(VirtualMachine.State.Running);
        when(command.getMountTimeout()).thenReturn(30);

        try (MockedStatic<Files> filesMock = mockStatic(Files.class)) {
            Path tempPath = Mockito.mock(Path.class);
            when(tempPath.toString()).thenReturn("/tmp/csbackup.abc123");
            filesMock.when(() -> Files.createTempDirectory(anyString())).thenReturn(tempPath);

            try (MockedStatic<Script> scriptMock = mockStatic(Script.class)) {
                scriptMock.when(() -> Script.executeCommand(any(String[].class)))
                        .thenReturn(null);
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
        when(command.getRestoreVolumeSizes()).thenReturn(Arrays.asList(1024L));
        when(command.getWait()).thenReturn(60);
        PrimaryDataStoreTO primaryDataStore = Mockito.mock(PrimaryDataStoreTO.class);
        when(primaryDataStore.getPoolType()).thenReturn(Storage.StoragePoolType.NetworkFilesystem);
        when(command.getRestoreVolumePools()).thenReturn(Arrays.asList(primaryDataStore));
        when(command.getRestoreVolumePaths()).thenReturn(Arrays.asList("/var/lib/libvirt/images/volume-123"));
        when(command.getBackupFiles()).thenReturn(Arrays.asList("volume-123"));
        when(command.getVmState()).thenReturn(VirtualMachine.State.Running);
        when(command.getMountTimeout()).thenReturn(30);

        try (MockedStatic<Files> filesMock = mockStatic(Files.class)) {
            Path tempPath = Mockito.mock(Path.class);
            when(tempPath.toString()).thenReturn("/tmp/csbackup.abc123");
            filesMock.when(() -> Files.createTempDirectory(anyString())).thenReturn(tempPath);

            try (MockedStatic<Script> scriptMock = mockStatic(Script.class)) {
                scriptMock.when(() -> Script.getExecutableAbsolutePath(anyString()))
                        .thenAnswer(invocation -> invocation.getArgument(0));
                scriptMock.when(() -> Script.executeCommand(any(String[].class)))
                        .thenReturn(null);
                scriptMock.when(() -> Script.executeCommandForExitValue(any(String[].class)))
                        .thenAnswer(invocation -> {
                            if (Arrays.stream(invocation.getArguments()).map(String::valueOf).anyMatch("rsync"::equals)) {
                                return 1; // Rsync failure
                            }
                            return 0;
                        });
                scriptMock.when(() -> Script.runSimpleBashScriptForExitValue(anyString()))
                        .thenAnswer(invocation -> {
                            String command = invocation.getArgument(0);
                            if (command.contains("ls ")) {
                                return 0; // File exists
                            } else if (command.contains("qemu-img check")) {
                                return 0; // File is valid
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
        when(command.getRestoreVolumeSizes()).thenReturn(Arrays.asList(1024L));
        when(command.getWait()).thenReturn(60);
        PrimaryDataStoreTO primaryDataStore = Mockito.mock(PrimaryDataStoreTO.class);
        when(primaryDataStore.getPoolType()).thenReturn(Storage.StoragePoolType.NetworkFilesystem);
        when(command.getRestoreVolumePools()).thenReturn(Arrays.asList(primaryDataStore));
        when(command.getRestoreVolumePaths()).thenReturn(Arrays.asList("/var/lib/libvirt/images/volume-123"));
        when(command.getBackupFiles()).thenReturn(Arrays.asList("volume-123"));
        when(command.getVmState()).thenReturn(VirtualMachine.State.Running);
        when(command.getMountTimeout()).thenReturn(30);

        try (MockedStatic<Files> filesMock = mockStatic(Files.class)) {
            Path tempPath = Mockito.mock(Path.class);
            when(tempPath.toString()).thenReturn("/tmp/csbackup.abc123");
            filesMock.when(() -> Files.createTempDirectory(anyString())).thenReturn(tempPath);

            try (MockedStatic<Script> scriptMock = mockStatic(Script.class)) {
                scriptMock.when(() -> Script.getExecutableAbsolutePath(anyString()))
                        .thenAnswer(invocation -> invocation.getArgument(0));
                scriptMock.when(() -> Script.executeCommand(any(String[].class)))
                        .thenReturn(null);
                scriptMock.when(() -> Script.executeCommandForExitValue(any(String[].class)))
                        .thenAnswer(invocation -> {
                            if (Arrays.stream(invocation.getArguments()).map(String::valueOf).anyMatch("attach-disk"::equals)) {
                                return 1; // Attach failure
                            }
                            return 0;
                        });
                scriptMock.when(() -> Script.runSimpleBashScriptForExitValue(anyString()))
                        .thenAnswer(invocation -> {
                            String command = invocation.getArgument(0);
                            if (command.contains("ls ")) {
                                return 0; // File exists
                            } else if (command.contains("qemu-img check")) {
                                return 0; // File is valid
                            }
                            return 0; // Other commands success
                        });
                scriptMock.when(() -> Script.executePipedCommands(anyList(), anyLong()))
                        .thenReturn(new Pair<>(0, "vda"));

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
        lenient().when(command.getRestoreVolumeSizes()).thenReturn(Arrays.asList(1024L));
        lenient().when(command.getWait()).thenReturn(60);
        PrimaryDataStoreTO primaryDataStore = Mockito.mock(PrimaryDataStoreTO.class);
        lenient().when(primaryDataStore.getPoolType()).thenReturn(Storage.StoragePoolType.NetworkFilesystem);
        when(command.getRestoreVolumePools()).thenReturn(Arrays.asList(primaryDataStore));
        lenient().when(command.getRestoreVolumePaths()).thenReturn(Arrays.asList("/var/lib/libvirt/images/volume-123"));
        when(command.getBackupFiles()).thenReturn(Arrays.asList("volume-123"));
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
        when(primaryDataStore1.getPoolType()).thenReturn(Storage.StoragePoolType.NetworkFilesystem);
        when(primaryDataStore2.getPoolType()).thenReturn(Storage.StoragePoolType.NetworkFilesystem);
        when(command.getRestoreVolumePools()).thenReturn(Arrays.asList(
                primaryDataStore1,
                primaryDataStore2
        ));
        when(command.getRestoreVolumePaths()).thenReturn(Arrays.asList(
                "/var/lib/libvirt/images/volume-123",
                "/var/lib/libvirt/images/volume-456"
        ));
        when(command.getBackupVolumesUUIDs()).thenReturn(Arrays.asList("volume-123", "volume-456"));
        when(command.getBackupFiles()).thenReturn(Arrays.asList("volume-123", "volume-456"));
        when(command.getMountTimeout()).thenReturn(30);

        try (MockedStatic<Files> filesMock = mockStatic(Files.class)) {
            Path tempPath = Mockito.mock(Path.class);
            when(tempPath.toString()).thenReturn("/tmp/csbackup.abc123");
            filesMock.when(() -> Files.createTempDirectory(anyString())).thenReturn(tempPath);

            try (MockedStatic<Script> scriptMock = mockStatic(Script.class)) {
                scriptMock.when(() -> Script.runSimpleBashScriptForExitValue(anyString()))
                        .thenReturn(0); // All commands success
                scriptMock.when(() -> Script.executeCommand(any(String[].class)))
                        .thenReturn(null);
                scriptMock.when(() -> Script.executeCommandForExitValue(any(String[].class)))
                        .thenReturn(0); // All commands success

                filesMock.when(() -> Files.deleteIfExists(any(Path.class))).thenReturn(true);

                Answer result = wrapper.execute(command, libvirtComputingResource);

                Assert.assertNotNull(result);
                Assert.assertTrue(result instanceof BackupAnswer);
                BackupAnswer backupAnswer = (BackupAnswer) result;
                Assert.assertTrue(backupAnswer.getResult());
            }
        }
    }

    /** Mockito hands varargs to the answer as individual arguments; rebuild the command line from {@code from}. */
    private static String joinArgs(Object[] args, int from) {
        StringBuilder sb = new StringBuilder();
        for (int i = from; i < args.length; i++) {
            if (args[i] instanceof String[]) {
                sb.append(String.join(" ", (String[]) args[i]));
            } else {
                sb.append(args[i]);
            }
            if (i < args.length - 1) {
                sb.append(' ');
            }
        }
        return sb.toString();
    }

    private void stubEncryptedNfsRestore(String passphrase) {
        when(command.getVmName()).thenReturn("test-vm");
        when(command.getBackupPath()).thenReturn("backup/path");
        when(command.getBackupRepoAddress()).thenReturn("192.168.1.100:/backup");
        when(command.getBackupRepoType()).thenReturn("nfs");
        when(command.getMountOptions()).thenReturn("rw");
        when(command.isVmExists()).thenReturn(false);
        when(command.getWait()).thenReturn(60);
        when(command.getEncryptionPassphrase()).thenReturn(passphrase);
        PrimaryDataStoreTO primaryDataStore = Mockito.mock(PrimaryDataStoreTO.class);
        when(primaryDataStore.getPoolType()).thenReturn(Storage.StoragePoolType.NetworkFilesystem);
        when(command.getRestoreVolumePools()).thenReturn(Arrays.asList(primaryDataStore));
        when(command.getRestoreVolumePaths()).thenReturn(Arrays.asList("/var/lib/libvirt/images/volume-123"));
        when(command.getBackupFiles()).thenReturn(Arrays.asList("volume-123"));
        when(command.getMountTimeout()).thenReturn(30);
    }

    @Test
    public void testEncryptedBackupIsCheckedAndDecryptedWithTheSecret() throws Exception {
        stubEncryptedNfsRestore("s3cret");
        List<String> qemuImgCalls = new ArrayList<>();

        try (MockedStatic<Files> filesMock = mockStatic(Files.class)) {
            Path tempPath = Mockito.mock(Path.class);
            when(tempPath.toString()).thenReturn("/tmp/csbackup.abc123");
            filesMock.when(() -> Files.createTempDirectory(anyString())).thenReturn(tempPath);
            filesMock.when(() -> Files.deleteIfExists(any(Path.class))).thenReturn(true);

            try (MockedStatic<Script> scriptMock = mockStatic(Script.class)) {
                scriptMock.when(() -> Script.runSimpleBashScriptForExitValue(anyString(), anyInt(), any(Boolean.class))).thenReturn(0);
                scriptMock.when(() -> Script.runSimpleBashScriptForExitValue(anyString())).thenReturn(0);
                scriptMock.when(() -> Script.executeCommand(any(String[].class))).thenReturn("{\"encrypted\": true}");
                scriptMock.when(() -> Script.executeCommandForExitValue(any(String[].class)))
                        .thenAnswer(inv -> { qemuImgCalls.add(joinArgs(inv.getArguments(), 0)); return 0; });
                scriptMock.when(() -> Script.executeCommandForExitValue(anyLong(), any(String[].class)))
                        .thenAnswer(inv -> { qemuImgCalls.add(joinArgs(inv.getArguments(), 1)); return 0; });

                BackupAnswer answer = (BackupAnswer) wrapper.execute(command, libvirtComputingResource);

                Assert.assertTrue(answer.getDetails(), answer.getResult());
                Assert.assertEquals(2, qemuImgCalls.size());
                Assert.assertTrue(qemuImgCalls.get(0), qemuImgCalls.get(0).startsWith("qemu-img check --object secret,id=sec0,file="));
                Assert.assertTrue(qemuImgCalls.get(0), qemuImgCalls.get(0).contains("encrypt.key-secret=sec0"));
                Assert.assertTrue(qemuImgCalls.get(1), qemuImgCalls.get(1).startsWith("qemu-img convert -O qcow2 --object secret,id=sec0,file="));
                Assert.assertTrue(qemuImgCalls.get(1), qemuImgCalls.get(1).endsWith(" /var/lib/libvirt/images/volume-123"));
                // the rsync used for plain backups must not run for an encrypted one
                scriptMock.verify(() -> Script.runSimpleBashScriptForExitValue(Mockito.startsWith("rsync"), anyInt(), any(Boolean.class)), Mockito.never());
            }
        }
    }

    @Test
    public void testEncryptedBackupWithoutPassphraseFailsClearly() throws Exception {
        stubEncryptedNfsRestore(null);

        try (MockedStatic<Files> filesMock = mockStatic(Files.class)) {
            Path tempPath = Mockito.mock(Path.class);
            when(tempPath.toString()).thenReturn("/tmp/csbackup.abc123");
            filesMock.when(() -> Files.createTempDirectory(anyString())).thenReturn(tempPath);
            filesMock.when(() -> Files.deleteIfExists(any(Path.class))).thenReturn(true);

            try (MockedStatic<Script> scriptMock = mockStatic(Script.class)) {
                scriptMock.when(() -> Script.runSimpleBashScriptForExitValue(anyString(), anyInt(), any(Boolean.class))).thenReturn(0);
                scriptMock.when(() -> Script.runSimpleBashScriptForExitValue(anyString())).thenReturn(0);
                scriptMock.when(() -> Script.executeCommand(any(String[].class))).thenReturn("{\"encrypted\": true}");

                BackupAnswer answer = (BackupAnswer) wrapper.execute(command, libvirtComputingResource);

                Assert.assertFalse(answer.getResult());
                Assert.assertTrue(answer.getDetails(), answer.getDetails().contains("LUKS-encrypted but no passphrase is configured"));
                scriptMock.verify(() -> Script.executeCommandForExitValue(anyLong(), any(String[].class)), Mockito.never());
            }
        }
    }
}
