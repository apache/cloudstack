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
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.lang.reflect.Method;
import java.nio.file.Files;
import java.nio.file.Path;
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
import com.cloud.hypervisor.kvm.storage.KVMStoragePool;
import com.cloud.hypervisor.kvm.storage.KVMStoragePoolManager;
import com.cloud.storage.Storage;
import com.cloud.utils.Pair;
import com.cloud.utils.exception.CloudRuntimeException;
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

    /**
     * An RBD backup is a chain of `rbd export-diff` streams with no link between them, so the
     * agent has to recreate the image and replay every diff from the chain's full anchor up to the
     * backup being restored, in that order.
     */
    @Test
    public void testExecuteRestoresRbdVolumeByReplayingTheWholeDiffChain() throws Exception {
        when(command.getVmName()).thenReturn("test-vm");
        // The provider sets both; the chain paths are what an rbddiff restore actually reads.
        lenient().when(command.getBackupPath()).thenReturn("test-vm/inc1");
        when(command.getBackupFormat()).thenReturn("rbddiff");
        when(command.getBackupChainPaths()).thenReturn(Arrays.asList("test-vm/full", "test-vm/inc1"));
        when(command.getBackupRepoAddress()).thenReturn("192.168.1.100:/backup");
        when(command.getBackupRepoType()).thenReturn("nfs");
        when(command.getMountOptions()).thenReturn("rw");
        when(command.isVmExists()).thenReturn(true);
        when(command.getWait()).thenReturn(60);
        when(command.getMountTimeout()).thenReturn(30);
        PrimaryDataStoreTO primaryDataStore = Mockito.mock(PrimaryDataStoreTO.class);
        when(primaryDataStore.getPoolType()).thenReturn(Storage.StoragePoolType.RBD);
        when(primaryDataStore.getUuid()).thenReturn("ceph-pool-uuid");
        when(command.getRestoreVolumePools()).thenReturn(Arrays.asList(primaryDataStore));
        when(command.getRestoreVolumePaths()).thenReturn(Arrays.asList("cephpool/vol-uuid"));
        when(command.getBackupVolumesUUIDs()).thenReturn(Arrays.asList("vol-uuid"));
        when(command.getBackupFiles()).thenReturn(Arrays.asList("vol-uuid"));

        KVMStoragePoolManager storagePoolMgr = Mockito.mock(KVMStoragePoolManager.class);
        KVMStoragePool pool = Mockito.mock(KVMStoragePool.class);
        when(libvirtComputingResource.getStoragePoolMgr()).thenReturn(storagePoolMgr);
        when(storagePoolMgr.getStoragePool(any(), anyString())).thenReturn(pool);
        lenient().when(pool.getSourceHost()).thenReturn("10.0.1.2");
        lenient().when(pool.getSourcePort()).thenReturn(6789);

        List<String[]> executed = new java.util.ArrayList<>();

        try (MockedStatic<Files> filesMock = mockStatic(Files.class)) {
            Path tempPath = Mockito.mock(Path.class);
            when(tempPath.toString()).thenReturn("/tmp/csbackup.abc123");
            filesMock.when(() -> Files.createTempDirectory(anyString())).thenReturn(tempPath);
            filesMock.when(() -> Files.deleteIfExists(any(Path.class))).thenReturn(true);

            try (MockedStatic<Script> scriptMock = mockStatic(Script.class)) {
                scriptMock.when(() -> Script.getExecutableAbsolutePath(anyString()))
                        .thenAnswer(invocation -> invocation.getArgument(0));
                scriptMock.when(() -> Script.executeCommand(any(String[].class))).thenReturn(null);
                scriptMock.when(() -> Script.runSimpleBashScriptForExitValue(anyString())).thenReturn(0);
                scriptMock.when(() -> Script.executeCommandForExitValue(any(String[].class)))
                        .thenAnswer(invocation -> {
                            // executeCommandForExitValue is varargs, so Mockito hands the
                            // arguments over either already expanded or still packed.
                            Object[] raw = invocation.getArguments();
                            if (raw.length == 1 && raw[0] instanceof String[]) {
                                executed.add((String[]) raw[0]);
                            } else {
                                String[] parts = new String[raw.length];
                                for (int i = 0; i < raw.length; i++) {
                                    parts[i] = String.valueOf(raw[i]);
                                }
                                executed.add(parts);
                            }
                            return 0;
                        });
                // Two piped commands run on this path: `rbd info` for the size and `rbd snap ls`
                // for what the image carries. They are told apart by the subcommand, since the
                // info output has a "name" of its own which would otherwise read as a snapshot.
                scriptMock.when(() -> Script.executePipedCommands(anyList(), anyLong()))
                        .thenAnswer(invocation -> {
                            List<String[]> commands = invocation.getArgument(0);
                            List<String> args = Arrays.asList(commands.get(0));
                            if (args.contains("snap") && args.contains("ls")) {
                                return new Pair<>(0, "[]");
                            }
                            return new Pair<>(0, "{\"name\":\"vol-uuid\",\"size\":1073741824}");
                        });

                Answer result = wrapper.execute(command, libvirtComputingResource);
                Assert.assertTrue(((BackupAnswer) result).getResult());
            }
        }

        List<String> importedDiffs = new java.util.ArrayList<>();
        boolean created = false;
        for (String[] cmd : executed) {
            List<String> args = Arrays.asList(cmd);
            if (args.contains("import-diff")) {
                importedDiffs.add(args.get(args.indexOf("import-diff") + 1));
            } else if (args.contains("create")) {
                created = true;
                Assert.assertTrue("the image must be recreated at its own size, in bytes",
                        args.contains("1073741824B"));
                Assert.assertTrue("no diff may be replayed before the image is recreated",
                        importedDiffs.isEmpty());
            }
        }
        Assert.assertTrue("the target image must be recreated before the chain is replayed", created);
        Assert.assertEquals("the chain must be replayed full-anchor first",
                Arrays.asList("/tmp/csbackup.abc123/test-vm/full/root.vol-uuid.rbddiff",
                              "/tmp/csbackup.abc123/test-vm/inc1/root.vol-uuid.rbddiff"),
                importedDiffs);
    }

    @Test
    public void testExecuteRefusesToRestoreAnRbdVolumeCarryingAForeignSnapshot() throws Exception {
        when(command.getVmName()).thenReturn("test-vm");
        // The provider sets both; the chain paths are what an rbddiff restore actually reads.
        lenient().when(command.getBackupPath()).thenReturn("test-vm/inc1");
        when(command.getBackupFormat()).thenReturn("rbddiff");
        when(command.getBackupChainPaths()).thenReturn(Arrays.asList("test-vm/full", "test-vm/inc1"));
        when(command.getBackupRepoAddress()).thenReturn("192.168.1.100:/backup");
        when(command.getBackupRepoType()).thenReturn("nfs");
        when(command.getMountOptions()).thenReturn("rw");
        when(command.isVmExists()).thenReturn(true);
        when(command.getWait()).thenReturn(60);
        when(command.getMountTimeout()).thenReturn(30);
        PrimaryDataStoreTO primaryDataStore = Mockito.mock(PrimaryDataStoreTO.class);
        when(primaryDataStore.getPoolType()).thenReturn(Storage.StoragePoolType.RBD);
        when(primaryDataStore.getUuid()).thenReturn("ceph-pool-uuid");
        when(command.getRestoreVolumePools()).thenReturn(Arrays.asList(primaryDataStore));
        when(command.getRestoreVolumePaths()).thenReturn(Arrays.asList("cephpool/vol-uuid"));
        when(command.getBackupVolumesUUIDs()).thenReturn(Arrays.asList("vol-uuid"));
        when(command.getBackupFiles()).thenReturn(Arrays.asList("vol-uuid"));

        KVMStoragePoolManager storagePoolMgr = Mockito.mock(KVMStoragePoolManager.class);
        KVMStoragePool pool = Mockito.mock(KVMStoragePool.class);
        when(libvirtComputingResource.getStoragePoolMgr()).thenReturn(storagePoolMgr);
        when(storagePoolMgr.getStoragePool(any(), anyString())).thenReturn(pool);
        lenient().when(pool.getSourceHost()).thenReturn("10.0.1.2");
        lenient().when(pool.getSourcePort()).thenReturn(6789);

        List<String[]> executed = new java.util.ArrayList<>();

        try (MockedStatic<Files> filesMock = mockStatic(Files.class)) {
            Path tempPath = Mockito.mock(Path.class);
            when(tempPath.toString()).thenReturn("/tmp/csbackup.abc123");
            filesMock.when(() -> Files.createTempDirectory(anyString())).thenReturn(tempPath);
            filesMock.when(() -> Files.deleteIfExists(any(Path.class))).thenReturn(true);

            try (MockedStatic<Script> scriptMock = mockStatic(Script.class)) {
                scriptMock.when(() -> Script.getExecutableAbsolutePath(anyString()))
                        .thenAnswer(invocation -> invocation.getArgument(0));
                scriptMock.when(() -> Script.executeCommand(any(String[].class))).thenReturn(null);
                scriptMock.when(() -> Script.runSimpleBashScriptForExitValue(anyString())).thenReturn(0);
                scriptMock.when(() -> Script.executeCommandForExitValue(any(String[].class)))
                        .thenAnswer(invocation -> {
                            // executeCommandForExitValue is varargs, so Mockito hands the
                            // arguments over either already expanded or still packed.
                            Object[] raw = invocation.getArguments();
                            if (raw.length == 1 && raw[0] instanceof String[]) {
                                executed.add((String[]) raw[0]);
                            } else {
                                String[] parts = new String[raw.length];
                                for (int i = 0; i < raw.length; i++) {
                                    parts[i] = String.valueOf(raw[i]);
                                }
                                executed.add(parts);
                            }
                            return 0;
                        });
                // Two piped commands run on this path: `rbd info` for the size and `rbd snap ls`
                // for what the image carries. They are told apart by the subcommand, since the
                // info output has a "name" of its own which would otherwise read as a snapshot.
                scriptMock.when(() -> Script.executePipedCommands(anyList(), anyLong()))
                        .thenAnswer(invocation -> {
                            List<String[]> commands = invocation.getArgument(0);
                            List<String> args = Arrays.asList(commands.get(0));
                            if (args.contains("snap") && args.contains("ls")) {
                                return new Pair<>(0, "[{\"name\":\"csbackup-1791\"},{\"name\":\"user-snap\"}]");
                            }
                            return new Pair<>(0, "{\"name\":\"vol-uuid\",\"size\":1073741824}");
                        });

                Answer result = wrapper.execute(command, libvirtComputingResource);
                // The chain anchor is ours to drop; user-snap is not, so the restore is refused
                // before the image is touched.
                Assert.assertFalse(((BackupAnswer) result).getResult());
                Assert.assertTrue(((BackupAnswer) result).getDetails().contains("user-snap"));
                Assert.assertFalse(((BackupAnswer) result).getDetails().contains("csbackup-1791"));
            }
        }

        for (String[] cmd : executed) {
            List<String> args = Arrays.asList(cmd);
            Assert.assertFalse("the image must not be touched when a foreign snapshot is present",
                    args.contains("rm") || args.contains("purge") || args.contains("import-diff"));
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
                scriptMock.when(() -> Script.executeCommandForExitValue(anyLong(), any(String[].class)))
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
                scriptMock.when(() -> Script.executeCommandForExitValue(anyLong(), any(String[].class)))
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
                            } else if (command.contains("qemu-img info") && command.contains("backing-filename")) {
                                return 1; // No backing chain — exercise the rsync path (full backups)
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

    private String invokeGetDeviceToAttachDisk(String vmName) throws Exception {
        Method method = LibvirtRestoreBackupCommandWrapper.class.getDeclaredMethod("getDeviceToAttachDisk", String.class);
        method.setAccessible(true);
        try {
            return (String) method.invoke(wrapper, vmName);
        } catch (java.lang.reflect.InvocationTargetException e) {
            throw (Exception) e.getCause();
        }
    }

    private String[] captureAttachCommand(Storage.StoragePoolType poolType) throws Exception {
        PrimaryDataStoreTO volumePool = Mockito.mock(PrimaryDataStoreTO.class);
        lenient().when(volumePool.getPoolType()).thenReturn(poolType);
        lenient().when(volumePool.getHost()).thenReturn("10.0.0.1");
        lenient().when(volumePool.getUuid()).thenReturn("pool-uuid");
        KVMStoragePoolManager storagePoolMgr = Mockito.mock(KVMStoragePoolManager.class);
        KVMStoragePool primaryPool = Mockito.mock(KVMStoragePool.class);
        lenient().when(storagePoolMgr.getStoragePool(any(), anyString())).thenReturn(primaryPool);
        lenient().when(primaryPool.getAuthUserName()).thenReturn("cloudstack");

        Method method = LibvirtRestoreBackupCommandWrapper.class.getDeclaredMethod("attachVolumeToVm",
                KVMStoragePoolManager.class, String.class, PrimaryDataStoreTO.class, String.class);
        method.setAccessible(true);

        final String[][] captured = new String[1][];
        try (MockedStatic<Script> scriptMock = mockStatic(Script.class)) {
            scriptMock.when(() -> Script.getExecutableAbsolutePath(anyString()))
                    .thenAnswer(invocation -> invocation.getArgument(0));
            scriptMock.when(() -> Script.executePipedCommands(anyList(), anyLong()))
                    .thenReturn(new Pair<>(0, "vda" + System.lineSeparator()));
            scriptMock.when(() -> Script.executeCommandForExitValue(any(String[].class)))
                    .thenAnswer(invocation -> {
                        // Mockito expands varargs, so the command comes back as individual arguments.
                        captured[0] = Arrays.stream(invocation.getArguments()).map(String::valueOf).toArray(String[]::new);
                        return 0;
                    });
            method.invoke(wrapper, storagePoolMgr, "test-vm", volumePool, "/path/to/volume");
        }
        return captured[0];
    }

    @Test
    public void testGetDeviceToAttachDiskTrimsOutputBeforeIncrementing() throws Exception {
        try (MockedStatic<Script> scriptMock = mockStatic(Script.class)) {
            scriptMock.when(() -> Script.getExecutableAbsolutePath(anyString()))
                    .thenAnswer(invocation -> invocation.getArgument(0));
            // executePipedCommands appends a line separator to each line it reads.
            scriptMock.when(() -> Script.executePipedCommands(anyList(), anyLong()))
                    .thenReturn(new Pair<>(0, "vda" + System.lineSeparator()));

            Assert.assertEquals("vdb", invokeGetDeviceToAttachDisk("test-vm"));
        }
    }

    @Test
    public void testGetDeviceToAttachDiskPassesUnquotedAwkProgram() throws Exception {
        try (MockedStatic<Script> scriptMock = mockStatic(Script.class)) {
            scriptMock.when(() -> Script.getExecutableAbsolutePath(anyString()))
                    .thenAnswer(invocation -> invocation.getArgument(0));
            final List<String[]>[] captured = new List[1];
            scriptMock.when(() -> Script.executePipedCommands(anyList(), anyLong()))
                    .thenAnswer(invocation -> {
                        captured[0] = invocation.getArgument(0);
                        return new Pair<>(0, "vda" + System.lineSeparator());
                    });

            invokeGetDeviceToAttachDisk("test-vm");

            String[] awkCmd = captured[0].get(captured[0].size() - 1);
            // The commands are executed without a shell, so the program must carry no shell quotes.
            Assert.assertEquals("awk", awkCmd[0]);
            Assert.assertEquals("{print $1}", awkCmd[1]);
        }
    }

    @Test(expected = CloudRuntimeException.class)
    public void testGetDeviceToAttachDiskFailsWhenNoDeviceIsReturned() throws Exception {
        try (MockedStatic<Script> scriptMock = mockStatic(Script.class)) {
            scriptMock.when(() -> Script.getExecutableAbsolutePath(anyString()))
                    .thenAnswer(invocation -> invocation.getArgument(0));
            scriptMock.when(() -> Script.executePipedCommands(anyList(), anyLong()))
                    .thenReturn(new Pair<>(1, ""));

            invokeGetDeviceToAttachDisk("test-vm");
        }
    }

    @Test
    public void testAttachVolumeUsesQcow2SubdriverForFileBackedPool() throws Exception {
        String[] cmd = captureAttachCommand(Storage.StoragePoolType.NetworkFilesystem);
        List<String> args = Arrays.asList(cmd);

        Assert.assertTrue(args.contains("attach-disk"));
        Assert.assertTrue(args.contains("--driver"));
        Assert.assertTrue(args.contains("qemu"));
        Assert.assertEquals("qcow2", args.get(args.indexOf("--subdriver") + 1));
    }

    @Test
    public void testAttachVolumeOmitsQcow2SubdriverForLinstor() throws Exception {
        String[] cmd = captureAttachCommand(Storage.StoragePoolType.Linstor);
        List<String> args = Arrays.asList(cmd);

        // Linstor volumes are raw DRBD block devices, declaring qcow2 makes libvirt reject them.
        Assert.assertTrue(args.contains("attach-disk"));
        Assert.assertFalse(args.contains("--subdriver"));
    }

    @Test
    public void testAttachVolumePassesRbdXmlThroughAFile() throws Exception {
        String[] cmd = captureAttachCommand(Storage.StoragePoolType.RBD);
        List<String> args = Arrays.asList(cmd);

        Assert.assertTrue(args.contains("attach-device"));
        // The XML has to reach virsh as a file, a here-document cannot work without a shell.
        Assert.assertFalse(args.stream().anyMatch(arg -> arg.contains("EOF")));
        Assert.assertTrue(args.get(args.size() - 1).endsWith(".xml"));
    }

    @Test
    public void testMountUsesTheConfiguredTimeout() throws Exception {
        when(command.getVmName()).thenReturn("test-vm");
        when(command.getBackupPath()).thenReturn("backup/path");
        when(command.getBackupRepoAddress()).thenReturn("192.168.1.100:/backup");
        when(command.getBackupRepoType()).thenReturn("nfs");
        when(command.getMountOptions()).thenReturn("rw");
        when(command.getMountTimeout()).thenReturn(30);

        try (MockedStatic<Files> filesMock = mockStatic(Files.class)) {
            Path tempPath = Mockito.mock(Path.class);
            when(tempPath.toString()).thenReturn("/tmp/csbackup.abc123");
            filesMock.when(() -> Files.createTempDirectory(anyString())).thenReturn(tempPath);

            try (MockedStatic<Script> scriptMock = mockStatic(Script.class)) {
                scriptMock.when(() -> Script.getExecutableAbsolutePath(anyString()))
                        .thenAnswer(invocation -> invocation.getArgument(0));
                final long[] mountTimeout = new long[1];
                scriptMock.when(() -> Script.executeCommandForExitValue(anyLong(), any(String[].class)))
                        .thenAnswer(invocation -> {
                            if (Arrays.stream(invocation.getArguments()).map(String::valueOf).anyMatch("mount"::equals)) {
                                mountTimeout[0] = invocation.getArgument(0);
                                return 1; // stop the restore right after the mount
                            }
                            return 0;
                        });

                wrapper.execute(command, libvirtComputingResource);

                Assert.assertEquals(30 * 1000L, mountTimeout[0]);
            }
        }
    }

    @Test
    public void testMountFailureRemovesTheTemporaryDirectory() throws Exception {
        when(command.getVmName()).thenReturn("test-vm");
        when(command.getBackupPath()).thenReturn("backup/path");
        when(command.getBackupRepoAddress()).thenReturn("192.168.1.100:/backup");
        when(command.getBackupRepoType()).thenReturn("nfs");
        when(command.getMountOptions()).thenReturn("rw");
        when(command.getMountTimeout()).thenReturn(30);

        try (MockedStatic<Files> filesMock = mockStatic(Files.class)) {
            Path tempPath = Mockito.mock(Path.class);
            when(tempPath.toString()).thenReturn("/tmp/csbackup.abc123");
            filesMock.when(() -> Files.createTempDirectory(anyString())).thenReturn(tempPath);
            filesMock.when(() -> Files.deleteIfExists(any(Path.class))).thenReturn(true);

            try (MockedStatic<Script> scriptMock = mockStatic(Script.class)) {
                scriptMock.when(() -> Script.getExecutableAbsolutePath(anyString()))
                        .thenAnswer(invocation -> invocation.getArgument(0));
                scriptMock.when(() -> Script.executeCommandForExitValue(anyLong(), any(String[].class)))
                        .thenReturn(1); // the mount fails

                wrapper.execute(command, libvirtComputingResource);

                // the directory created for the mount must not be left behind
                filesMock.verify(() -> Files.deleteIfExists(any(Path.class)));
            }
        }
    }

    @Test
    public void testUnmountIsBoundedByTheConfiguredTimeout() throws Exception {
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
            filesMock.when(() -> Files.deleteIfExists(any(Path.class))).thenReturn(true);

            try (MockedStatic<Script> scriptMock = mockStatic(Script.class)) {
                scriptMock.when(() -> Script.getExecutableAbsolutePath(anyString()))
                        .thenAnswer(invocation -> invocation.getArgument(0));
                final long[] umountTimeout = new long[] { -1 };
                scriptMock.when(() -> Script.executeCommandForExitValue(anyLong(), any(String[].class)))
                        .thenAnswer(invocation -> {
                            if (Arrays.stream(invocation.getArguments()).map(String::valueOf).anyMatch("umount"::equals)) {
                                umountTimeout[0] = invocation.getArgument(0);
                            }
                            return 0;
                        });
                scriptMock.when(() -> Script.runSimpleBashScriptForExitValue(anyString())).thenReturn(0);

                wrapper.execute(command, libvirtComputingResource);

                // an unreachable repository blocks umount just as it blocks mount
                Assert.assertEquals(30 * 1000L, umountTimeout[0]);
            }
        }
    }
}
