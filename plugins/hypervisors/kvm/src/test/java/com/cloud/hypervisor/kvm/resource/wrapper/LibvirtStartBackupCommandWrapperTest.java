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
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.apache.cloudstack.backup.StartBackupAnswer;
import org.apache.cloudstack.backup.StartBackupCommand;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.libvirt.Domain;
import org.libvirt.DomainBlockInfo;
import org.libvirt.DomainJobInfo;
import org.libvirt.LibvirtException;
import org.mockito.MockedConstruction;
import org.mockito.Mockito;
import org.mockito.junit.MockitoJUnitRunner;

import com.cloud.agent.api.Answer;
import com.cloud.hypervisor.kvm.resource.LibvirtComputingResource;
import com.cloud.utils.script.Script;

@RunWith(MockitoJUnitRunner.class)
public class LibvirtStartBackupCommandWrapperTest {

    private LibvirtStartBackupCommandWrapper wrapper;
    private StartBackupCommand command;
    private LibvirtComputingResource resource;

    @Before
    public void setUp() {
        wrapper = Mockito.spy(new LibvirtStartBackupCommandWrapper());
        Mockito.lenient().doReturn(null).when(wrapper).prepareScratchFiles(Mockito.any(), Mockito.any(), Mockito.any(LibvirtComputingResource.class));
        command = Mockito.mock(StartBackupCommand.class);
        resource = Mockito.mock(LibvirtComputingResource.class);
    }

    @Test
    public void testExecuteStoppedVmBitmapAddSuccess() {
        Mockito.when(command.isStoppedVM()).thenReturn(true);
        Mockito.when(command.getToCheckpointId()).thenReturn("cp-stopped-1");
        Mockito.when(command.getDiskPathUuidMap()).thenReturn(Map.of("/path/disk1.qcow2", "vol-1"));

        try (MockedConstruction<Script> ignored = Mockito.mockConstruction(Script.class,
                (mock, context) -> Mockito.when(mock.execute()).thenReturn(null))) {
            Answer answer = wrapper.execute(command, resource);

            Assert.assertTrue(answer.getResult());
            Assert.assertTrue(answer instanceof StartBackupAnswer);
            Assert.assertTrue(answer.getDetails().contains("checkpoint bitmap added successfully"));
        }
    }

    @Test
    public void testExecuteStoppedVmBitmapAddFailure() {
        Mockito.when(command.isStoppedVM()).thenReturn(true);
        Mockito.when(command.getToCheckpointId()).thenReturn("cp-stopped-2");
        Mockito.when(command.getDiskPathUuidMap()).thenReturn(Map.of("/path/disk1.qcow2", "vol-1"));

        try (MockedConstruction<Script> ignored = Mockito.mockConstruction(Script.class,
                (mock, context) -> Mockito.when(mock.execute()).thenReturn("bitmap add error"))) {
            Answer answer = wrapper.execute(command, resource);

            Assert.assertFalse(answer.getResult());
            Assert.assertTrue(answer.getDetails().contains("Failed to add bitmap cp-stopped-2"));
        }
    }

    @Test
    public void testExecuteRunningVmMissingFromCheckpointCreateTimeReturnsFailure() {
        Mockito.when(command.isStoppedVM()).thenReturn(false);
        Mockito.when(command.getVmName()).thenReturn("i-2-3-VM");
        Mockito.when(command.getToCheckpointId()).thenReturn("cp-running-1");
        Mockito.when(command.getFromCheckpointId()).thenReturn("cp-running-0");
        Mockito.when(command.getFromCheckpointCreateTime()).thenReturn(null);
        Mockito.when(command.getSocket()).thenReturn("sock-1");

        try (MockedConstruction<Script> ignored = Mockito.mockConstruction(Script.class,
                (mock, context) -> Mockito.when(mock.execute()).thenReturn("not found"))) {
            Answer answer = wrapper.execute(command, resource);

            Assert.assertFalse(answer.getResult());
            Assert.assertTrue(answer.getDetails().contains("From checkpoint create time is null"));
        }
    }

    @Test
    public void testExecuteRunningVmBackupBeginSuccess() {
        Mockito.when(command.isStoppedVM()).thenReturn(false);
        Mockito.when(command.getVmName()).thenReturn("i-2-3-VM");
        Mockito.when(command.getToCheckpointId()).thenReturn("cp-running-2");
        Mockito.when(command.getFromCheckpointId()).thenReturn(null);
        Mockito.when(command.getSocket()).thenReturn("sock-2");
        Mockito.when(command.getDiskPathUuidMap()).thenReturn(Map.of("/path/disk1.qcow2", "vol-1"));
        Mockito.when(resource.getDiskPathLabelMap("i-2-3-VM")).thenReturn(Map.of("/path/disk1.qcow2", "vda"));

        try (MockedConstruction<Script> ignored = Mockito.mockConstruction(Script.class,
                (mock, context) -> Mockito.when(mock.execute()).thenReturn(null))) {
            Answer answer = wrapper.execute(command, resource);

            Assert.assertTrue(answer.getResult());
            Assert.assertTrue(answer.getDetails().contains("Backup started successfully"));
        }
    }

    @Test
    public void testExecuteRunningVmCheckpointRedefineFailure() {
        Mockito.when(command.isStoppedVM()).thenReturn(false);
        Mockito.when(command.getVmName()).thenReturn("i-2-3-VM");
        Mockito.when(command.getToCheckpointId()).thenReturn("cp-running-3");
        Mockito.when(command.getFromCheckpointId()).thenReturn("cp-running-missing");
        Mockito.when(command.getFromCheckpointCreateTime()).thenReturn(12345L);
        Mockito.when(command.getSocket()).thenReturn("sock-3");

        try (MockedConstruction<Script> ignored = Mockito.mockConstruction(Script.class, (mock, context) -> {
            int constructionIndex = context.getCount();
            if (constructionIndex == 1) {
                Mockito.when(mock.execute()).thenReturn("checkpoint missing");
            } else if (constructionIndex == 2) {
                Mockito.when(mock.execute()).thenReturn("checkpoint redefine failed");
            } else {
                Mockito.when(mock.execute()).thenReturn(null);
            }
        })) {
            Answer answer = wrapper.execute(command, resource);

            Assert.assertFalse(answer.getResult());
            Assert.assertTrue(answer.getDetails().contains("Failed to redefine from-checkpoint cp-running-missing"));
        }
    }

    @Test
    public void testExecuteRunningVmBackupBeginFailure() {
        Mockito.when(command.isStoppedVM()).thenReturn(false);
        Mockito.when(command.getVmName()).thenReturn("i-2-3-VM");
        Mockito.when(command.getToCheckpointId()).thenReturn("cp-running-4");
        Mockito.when(command.getFromCheckpointId()).thenReturn(null);
        Mockito.when(command.getSocket()).thenReturn("sock-4");
        Mockito.when(command.getDiskPathUuidMap()).thenReturn(Map.of("/path/disk1.qcow2", "vol-1"));
        Mockito.when(resource.getDiskPathLabelMap("i-2-3-VM")).thenReturn(Map.of("/path/disk1.qcow2", "vda"));

        try (MockedConstruction<Script> ignored = Mockito.mockConstruction(Script.class, (mock, context) -> {
            if (context.getCount() == 1) {
                Mockito.when(mock.execute()).thenReturn("backup begin failed");
            } else {
                Mockito.when(mock.execute()).thenReturn(null);
            }
        })) {
            Answer answer = wrapper.execute(command, resource);

            Assert.assertFalse(answer.getResult());
            Assert.assertTrue(answer.getDetails().contains("Backup begin failed: backup begin failed"));
        }
    }

    @Test
    public void testExecuteRunningVmCreateBackupXmlExceptionReturnsFailure() {
        Mockito.when(command.isStoppedVM()).thenReturn(false);
        Mockito.when(command.getVmName()).thenReturn("i-2-3-VM");
        Mockito.when(command.getToCheckpointId()).thenReturn("cp-running-5");
        Mockito.when(command.getFromCheckpointId()).thenReturn(null);
        Mockito.when(command.getSocket()).thenReturn("sock-5");
        Mockito.when(resource.getDiskPathLabelMap("i-2-3-VM")).thenThrow(new RuntimeException("disk labels unavailable"));

        Answer answer = wrapper.execute(command, resource);

        Assert.assertFalse(answer.getResult());
        Assert.assertTrue(answer.getDetails().contains("Error starting backup: disk labels unavailable"));
    }

    @Test
    public void testScratchFilesGoNextToTheDisk() {
        Map<String, String> scratchFiles = LibvirtStartBackupCommandWrapper.getScratchFiles(
                Map.of("/mnt/pool-1/vol-path-1", "vol-uuid-1", "/var/lib/libvirt/images/vol-path-2", "vol-uuid-2"),
                Map.of("/mnt/pool-1/vol-path-1", "vda", "/var/lib/libvirt/images/vol-path-2", "vdb"));

        Assert.assertEquals("/mnt/pool-1/backup-scratch-vol-uuid-1.qcow2", scratchFiles.get("/mnt/pool-1/vol-path-1"));
        Assert.assertEquals("/var/lib/libvirt/images/backup-scratch-vol-uuid-2.qcow2", scratchFiles.get("/var/lib/libvirt/images/vol-path-2"));
    }

    @Test
    public void testScratchFilesOnlyForDisksInTheBackup() {
        Map<String, String> scratchFiles = LibvirtStartBackupCommandWrapper.getScratchFiles(
                Map.of("/mnt/pool-1/vol-path-1", "vol-uuid-1", "/mnt/pool-1/vol-path-2", "vol-uuid-2"),
                Map.of("/mnt/pool-1/vol-path-1", "vda"));

        Assert.assertEquals(List.of("/mnt/pool-1/vol-path-1"), List.copyOf(scratchFiles.keySet()));
    }

    private static Domain domainWithDisks(int jobType, Map<String, Long> capacities) throws LibvirtException {
        Domain dm = Mockito.mock(Domain.class);
        DomainJobInfo job = Mockito.mock(DomainJobInfo.class);
        Mockito.when(job.getType()).thenReturn(jobType);
        Mockito.when(dm.getJobInfo()).thenReturn(job);
        for (Map.Entry<String, Long> entry : capacities.entrySet()) {
            DomainBlockInfo info = Mockito.mock(DomainBlockInfo.class);
            Mockito.lenient().when(info.getCapacity()).thenReturn(entry.getValue());
            Mockito.lenient().when(dm.blockInfo(entry.getKey())).thenReturn(info);
        }
        return dm;
    }

    private static Map<String, String> scratchFilesFor(Map<String, String> diskPathUuidMap) {
        return LibvirtStartBackupCommandWrapper.getScratchFiles(diskPathUuidMap,
                diskPathUuidMap.keySet().stream().collect(Collectors.toMap(path -> path, path -> "vd")));
    }

    @Test
    public void testPrepareScratchFilesRemovesStaleFile() throws Exception {
        Path pool = Files.createTempDirectory("pool");
        try {
            String disk = pool.resolve("vol-path-1").toString();
            Map<String, String> scratchFiles = scratchFilesFor(Map.of(disk, "vol-uuid-1"));
            Path stale = Path.of(scratchFiles.get(disk));
            Files.writeString(stale, "left by a crashed backup");
            Mockito.doReturn(10L * 1024 * 1024 * 1024).when(wrapper).getUsableSpace(pool.toString());

            String error = wrapper.prepareScratchFiles(domainWithDisks(LibvirtStartBackupCommandWrapper.NO_JOB, Map.of(disk, 1024L * 1024 * 1024)), scratchFiles);

            Assert.assertNull(error);
            Assert.assertFalse(Files.exists(stale));
        } finally {
            deleteRecursively(pool);
        }
    }

    @Test
    public void testPrepareScratchFilesNeedsRoomForAllDisksInTheDirectory() throws Exception {
        Path pool = Files.createTempDirectory("pool");
        try {
            String disk1 = pool.resolve("vol-path-1").toString();
            String disk2 = pool.resolve("vol-path-2").toString();
            long tenGiB = 10L * 1024 * 1024 * 1024;
            Map<String, String> scratchFiles = scratchFilesFor(Map.of(disk1, "vol-uuid-1", disk2, "vol-uuid-2"));
            // Room for one disk's worst case, not for both.
            Mockito.doReturn(15L * 1024 * 1024 * 1024).when(wrapper).getUsableSpace(pool.toString());

            String error = wrapper.prepareScratchFiles(domainWithDisks(LibvirtStartBackupCommandWrapper.NO_JOB, Map.of(disk1, tenGiB, disk2, tenGiB)), scratchFiles);

            Assert.assertNotNull(error);
            Assert.assertTrue(error.contains("Not enough free space"));
            Assert.assertTrue(error.contains(pool.toString()));
        } finally {
            deleteRecursively(pool);
        }
    }

    @Test
    public void testPrepareScratchFilesLeavesFilesOfAnActiveJobAlone() throws Exception {
        Path pool = Files.createTempDirectory("pool");
        try {
            String disk = pool.resolve("vol-path-1").toString();
            Map<String, String> scratchFiles = scratchFilesFor(Map.of(disk, "vol-uuid-1"));
            Path inUse = Path.of(scratchFiles.get(disk));
            Files.writeString(inUse, "scratch of a running backup");
            int unboundedJob = 2;

            String error = wrapper.prepareScratchFiles(domainWithDisks(unboundedJob, Map.of(disk, 1024L)), scratchFiles);

            Assert.assertNotNull(error);
            Assert.assertTrue(Files.exists(inUse));
        } finally {
            deleteRecursively(pool);
        }
    }

    @Test
    public void testExecuteRunningVmScratchFailureDoesNotBeginBackup() {
        Mockito.when(command.isStoppedVM()).thenReturn(false);
        Mockito.when(command.getVmName()).thenReturn("i-2-3-VM");
        Mockito.when(command.getFromCheckpointId()).thenReturn(null);
        Mockito.when(command.getDiskPathUuidMap()).thenReturn(Map.of("/path/disk1.qcow2", "vol-1"));
        Mockito.when(resource.getDiskPathLabelMap("i-2-3-VM")).thenReturn(Map.of("/path/disk1.qcow2", "vda"));
        Mockito.doReturn("Not enough free space for the backup scratch files in /path").when(wrapper)
                .prepareScratchFiles(Mockito.eq("i-2-3-VM"), Mockito.any(), Mockito.any(LibvirtComputingResource.class));

        try (MockedConstruction<Script> scripts = Mockito.mockConstruction(Script.class)) {
            Answer answer = wrapper.execute(command, resource);

            Assert.assertFalse(answer.getResult());
            Assert.assertTrue(answer.getDetails().contains("Not enough free space"));
            Assert.assertTrue(scripts.constructed().isEmpty());
        }
    }

    private static void deleteRecursively(Path dir) throws IOException {
        try (Stream<Path> paths = Files.walk(dir)) {
            for (Path path : paths.sorted(Comparator.reverseOrder()).collect(Collectors.toList())) {
                Files.deleteIfExists(path);
            }
        }
    }
}
