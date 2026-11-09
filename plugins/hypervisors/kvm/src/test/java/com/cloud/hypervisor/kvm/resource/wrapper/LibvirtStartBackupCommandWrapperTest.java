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

import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.apache.cloudstack.backup.StartBackupAnswer;
import org.apache.cloudstack.backup.StartBackupCommand;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.MockedConstruction;
import org.mockito.Mockito;
import org.mockito.junit.MockitoJUnitRunner;
import org.libvirt.Connect;
import org.libvirt.Domain;
import org.libvirt.LibvirtException;

import com.cloud.agent.api.Answer;
import com.cloud.hypervisor.kvm.resource.LibvirtComputingResource;
import com.cloud.utils.script.Script;

@RunWith(MockitoJUnitRunner.class)
public class LibvirtStartBackupCommandWrapperTest {

    private LibvirtStartBackupCommandWrapper wrapper;
    private StartBackupCommand command;
    private LibvirtComputingResource resource;

    private Domain domain;

    private static final String NAMED_NODES = "{\"return\": ["
            + "{\"node-name\": \"libvirt-2-format\", \"file\": \"/path/disk1.qcow2\", \"drv\": \"qcow2\","
            + "  \"image\": {\"filename\": \"/path/disk1.qcow2\"}, \"dirty-bitmaps\": [{\"name\": \"cp-running-0\"}]},"
            + "{\"node-name\": \"libvirt-1-format\", \"file\": \"/path/disk2.qcow2\", \"drv\": \"qcow2\","
            + "  \"image\": {\"filename\": \"/path/disk2.qcow2\"}, \"dirty-bitmaps\": []}]}";

    @Before
    public void setUp() throws Exception {
        wrapper = new LibvirtStartBackupCommandWrapper();
        command = Mockito.mock(StartBackupCommand.class);
        resource = Mockito.mock(LibvirtComputingResource.class);
        domain = Mockito.mock(Domain.class);
        LibvirtUtilitiesHelper helper = Mockito.mock(LibvirtUtilitiesHelper.class);
        Connect conn = Mockito.mock(Connect.class);
        Mockito.lenient().when(resource.getLibvirtUtilitiesHelper()).thenReturn(helper);
        Mockito.lenient().when(helper.getConnection()).thenReturn(conn);
        Mockito.lenient().when(resource.getDomain(conn, "i-2-3-VM")).thenReturn(domain);
        Mockito.lenient().when(domain.qemuMonitorCommand(Mockito.contains("query-block"), Mockito.eq(0))).thenReturn("{\"return\": []}");
    }

    private void runningVmFromForgottenCheckpoint() {
        Mockito.when(command.isStoppedVM()).thenReturn(false);
        Mockito.when(command.getVmName()).thenReturn("i-2-3-VM");
        Mockito.when(command.getToCheckpointId()).thenReturn("cp-running-1");
        Mockito.when(command.getFromCheckpointId()).thenReturn("cp-running-0");
        Mockito.when(command.getFromCheckpointCreateTime()).thenReturn(1700000000L);
        Mockito.when(command.getSocket()).thenReturn("sock-1");
        Mockito.when(command.getDiskPathUuidMap()).thenReturn(Map.of("/path/disk1.qcow2", "vol-1", "/path/disk2.qcow2", "vol-2"));
        Mockito.when(resource.getDiskPathLabelMap("i-2-3-VM")).thenReturn(Map.of("/path/disk1.qcow2", "vda", "/path/disk2.qcow2", "vdb"));
    }

    private static String commandOf(Script script) {
        return Mockito.mockingDetails(script).getInvocations().stream()
                .filter(invocation -> invocation.getMethod().getName().equals("add"))
                .flatMap(invocation -> java.util.Arrays.stream(invocation.getArguments()).flatMap(argument ->
                        argument instanceof String[] ? java.util.Arrays.stream((String[]) argument) : java.util.stream.Stream.of((String) argument)))
                .filter(argument -> !"-c".equals(argument))
                .collect(java.util.stream.Collectors.joining(" "));
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
    public void testExecuteRunningVmForgottenFromCheckpointIsRedefinedWithTheDisksThatHaveItsBitmap() throws Exception {
        runningVmFromForgottenCheckpoint();
        Mockito.when(domain.qemuMonitorCommand(Mockito.contains("query-named-block-nodes"), Mockito.eq(0))).thenReturn(NAMED_NODES);
        List<String> redefineXml = new ArrayList<>();

        try (MockedConstruction<Script> scripts = Mockito.mockConstruction(Script.class, (mock, context) -> {
            if (context.getCount() == 1) {
                Mockito.when(mock.execute()).thenReturn("checkpoint missing");
            } else if (context.getCount() == 2) {
                Mockito.when(mock.execute()).thenAnswer(invocation -> {
                    String[] args = commandOf(mock).split(" ");
                    redefineXml.add(new String(Files.readAllBytes(Paths.get(args[java.util.Arrays.asList(args).indexOf("--xmlfile") + 1]))));
                    return null;
                });
            } else {
                Mockito.when(mock.execute()).thenReturn(null);
            }
        })) {
            Answer answer = wrapper.execute(command, resource);

            Assert.assertTrue(answer.getDetails(), answer.getResult());
            Assert.assertEquals(3, scripts.constructed().size());
            Assert.assertTrue(commandOf(scripts.constructed().get(0)).startsWith("virsh checkpoint-dumpxml --domain i-2-3-VM --checkpointname cp-running-0"));
            Assert.assertTrue(commandOf(scripts.constructed().get(1)).startsWith("checkpoint-create --domain i-2-3-VM --xmlfile"));
            Assert.assertTrue(commandOf(scripts.constructed().get(1)).endsWith("--redefine"));
            Assert.assertTrue(redefineXml.get(0), redefineXml.get(0).contains("<name>cp-running-0</name>"));
            Assert.assertTrue(redefineXml.get(0), redefineXml.get(0).contains("<creationTime>1700000000</creationTime>"));
            Assert.assertTrue(redefineXml.get(0), redefineXml.get(0).contains("<disk name='vda' checkpoint='bitmap'/>"));
            Assert.assertTrue(redefineXml.get(0), redefineXml.get(0).contains("<disk name='vdb' checkpoint='no'/>"));
            Assert.assertTrue(commandOf(scripts.constructed().get(2)).startsWith("virsh backup-begin i-2-3-VM"));
        }
    }

    @Test
    public void testExecuteRunningVmForgottenFromCheckpointWithoutAnyBitmapIsNotRedefined() throws Exception {
        runningVmFromForgottenCheckpoint();
        Mockito.when(domain.qemuMonitorCommand(Mockito.contains("query-named-block-nodes"), Mockito.eq(0)))
                .thenReturn(NAMED_NODES.replace("cp-running-0", "cp-other"));

        try (MockedConstruction<Script> scripts = Mockito.mockConstruction(Script.class, (mock, context) ->
                Mockito.when(mock.execute()).thenReturn(context.getCount() == 1 ? "checkpoint missing" : null))) {
            Answer answer = wrapper.execute(command, resource);

            Assert.assertTrue(answer.getDetails(), answer.getResult());
            Assert.assertEquals(2, scripts.constructed().size());
            Assert.assertTrue(commandOf(scripts.constructed().get(1)).startsWith("virsh backup-begin i-2-3-VM"));
        }
    }

    @Test
    public void testExecuteRunningVmBitmapLookupFailureReturnsFailure() throws Exception {
        runningVmFromForgottenCheckpoint();
        Mockito.when(domain.qemuMonitorCommand(Mockito.contains("query-named-block-nodes"), Mockito.eq(0))).thenThrow(LibvirtException.class);

        try (MockedConstruction<Script> scripts = Mockito.mockConstruction(Script.class, (mock, context) ->
                Mockito.when(mock.execute()).thenReturn("checkpoint missing"))) {
            Answer answer = wrapper.execute(command, resource);

            Assert.assertFalse(answer.getResult());
            Assert.assertTrue(answer.getDetails(), answer.getDetails().startsWith("Failed to look up the bitmap of from-checkpoint cp-running-0"));
            Assert.assertEquals(1, scripts.constructed().size());
        }
    }

    @Test
    public void testExecuteRunningVmCheckpointRedefineFailure() throws Exception {
        runningVmFromForgottenCheckpoint();
        Mockito.when(domain.qemuMonitorCommand(Mockito.contains("query-named-block-nodes"), Mockito.eq(0))).thenReturn(NAMED_NODES);

        try (MockedConstruction<Script> ignored = Mockito.mockConstruction(Script.class, (mock, context) -> {
            if (context.getCount() == 1) {
                Mockito.when(mock.execute()).thenReturn("checkpoint missing");
            } else if (context.getCount() == 2) {
                Mockito.when(mock.execute()).thenReturn("checkpoint redefine failed");
            } else {
                Mockito.when(mock.execute()).thenReturn(null);
            }
        })) {
            Answer answer = wrapper.execute(command, resource);

            Assert.assertFalse(answer.getResult());
            Assert.assertTrue(answer.getDetails().contains("Failed to redefine from-checkpoint cp-running-0"));
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
        Mockito.when(command.getDiskPathUuidMap()).thenReturn(Map.of("/path/disk1.qcow2", "vol-1"));
        Mockito.when(resource.getDiskPathLabelMap("i-2-3-VM")).thenThrow(new RuntimeException("disk labels unavailable"));

        Answer answer = wrapper.execute(command, resource);

        Assert.assertFalse(answer.getResult());
        Assert.assertTrue(answer.getDetails().contains("Error starting backup: disk labels unavailable"));
    }
}
