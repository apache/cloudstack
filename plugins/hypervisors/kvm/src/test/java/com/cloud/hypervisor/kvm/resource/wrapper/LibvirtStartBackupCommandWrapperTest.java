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
        wrapper = new LibvirtStartBackupCommandWrapper();
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
        Mockito.when(command.getDiskPathUuidMap()).thenReturn(Map.of("/path/disk1.qcow2", "vol-1"));
        Mockito.when(resource.getDiskPathLabelMap("i-2-3-VM")).thenThrow(new RuntimeException("disk labels unavailable"));

        Answer answer = wrapper.execute(command, resource);

        Assert.assertFalse(answer.getResult());
        Assert.assertTrue(answer.getDetails().contains("Error starting backup: disk labels unavailable"));
    }
}
