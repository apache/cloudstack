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

import org.apache.cloudstack.backup.StartNBDServerAnswer;
import org.apache.cloudstack.backup.StartNBDServerCommand;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.MockedConstruction;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.mockito.junit.MockitoJUnitRunner;

import com.cloud.agent.api.Answer;
import com.cloud.hypervisor.kvm.resource.LibvirtComputingResource;
import com.cloud.utils.script.Script;

@RunWith(MockitoJUnitRunner.class)
public class LibvirtStartNBDServerCommandWrapperTest {

    private LibvirtStartNBDServerCommandWrapper wrapper;
    private StartNBDServerCommand command;
    private LibvirtComputingResource resource;

    @Before
    public void setUp() {
        wrapper = new LibvirtStartNBDServerCommandWrapper();
        command = Mockito.mock(StartNBDServerCommand.class);
        resource = Mockito.mock(LibvirtComputingResource.class);
        Mockito.when(command.getVolumePath()).thenReturn("/path/disk.qcow2");
        Mockito.when(command.getExportName()).thenReturn("vol-1");
        Mockito.when(command.getSocket()).thenReturn("sock-1");
        Mockito.when(command.getTransferId()).thenReturn("transfer-1");
        Mockito.when(command.getDirection()).thenReturn("upload");
        Mockito.when(command.getFromCheckpointId()).thenReturn(null);
    }

    @Test
    public void testExecuteMissingVolumePathReturnsFailure() {
        Mockito.when(command.getVolumePath()).thenReturn(null);

        Answer answer = wrapper.execute(command, resource);

        Assert.assertFalse(answer.getResult());
        Assert.assertEquals("Volume path is required for the nbd server", answer.getDetails());
    }

    @Test
    public void testExecuteMissingExportNameReturnsFailure() {
        Mockito.when(command.getExportName()).thenReturn(" ");

        Answer answer = wrapper.execute(command, resource);

        Assert.assertFalse(answer.getResult());
        Assert.assertEquals("Export name is required for the nbd server", answer.getDetails());
    }

    @Test
    public void testExecuteMissingSocketReturnsFailure() {
        Mockito.when(command.getSocket()).thenReturn("");

        Answer answer = wrapper.execute(command, resource);

        Assert.assertFalse(answer.getResult());
        Assert.assertEquals("Socket is required for the nbd server", answer.getDetails());
    }

    @Test
    public void testExecuteAlreadyActiveServiceReturnsFailure() {
        try (MockedConstruction<Script> ignored = Mockito.mockConstruction(Script.class, (mock, context) ->
                Mockito.when(mock.execute()).thenReturn(null))) {
            Answer answer = wrapper.execute(command, resource);

            Assert.assertFalse(answer.getResult());
            Assert.assertTrue(answer.getDetails().contains("already running"));
        }
    }

    @Test
    public void testExecuteStartScriptFailureReturnsFailure() {
        try (MockedConstruction<Script> ignored = Mockito.mockConstruction(Script.class, (mock, context) -> {
            if (context.getCount() == 1) {
                Mockito.when(mock.execute()).thenReturn("inactive");
            } else if (context.getCount() == 2) {
                Mockito.when(mock.execute()).thenReturn("start failed");
            } else {
                Mockito.when(mock.execute()).thenReturn(null);
            }
        })) {
            Answer answer = wrapper.execute(command, resource);

            Assert.assertFalse(answer.getResult());
            Assert.assertTrue(answer.getDetails().contains("Failed to start qemu-nbd service: start failed"));
        }
    }

    @Test
    public void testExecuteInterruptedWhileWaitingReturnsFailure() {
        try (MockedConstruction<Script> ignored = Mockito.mockConstruction(Script.class, (mock, context) -> {
            if (context.getCount() <= 2) {
                Mockito.when(mock.execute()).thenReturn(context.getCount() == 1 ? "inactive" : null);
            } else {
                Mockito.when(mock.execute()).thenReturn("inactive");
            }
        })) {
            Thread.currentThread().interrupt();
            Answer answer = wrapper.execute(command, resource);

            Assert.assertFalse(answer.getResult());
            Assert.assertTrue(answer.getDetails().contains("Interrupted while waiting"));
            Assert.assertTrue(Thread.currentThread().isInterrupted());
            Thread.interrupted();
        }
    }

    @Test
    public void testExecuteSuccessReturnsTransferDetails() {
        try (MockedConstruction<Script> ignored = Mockito.mockConstruction(Script.class, (mock, context) -> {
            if (context.getCount() == 1) {
                Mockito.when(mock.execute()).thenReturn("inactive");
            } else {
                Mockito.when(mock.execute()).thenReturn(null);
            }
        })) {
            Answer answer = wrapper.execute(command, resource);

            Assert.assertTrue(answer.getResult());
            Assert.assertTrue(answer instanceof StartNBDServerAnswer);
            StartNBDServerAnswer startNBDServerAnswer = (StartNBDServerAnswer) answer;
            Assert.assertEquals("transfer-1", startNBDServerAnswer.getImageTransferId());
            Assert.assertEquals("nbd+unix:///sock-1", startNBDServerAnswer.getTransferUrl());
        }
    }

    @Test
    public void testExecuteAddsBitmapOptionWhenBitmapExists() {
        Mockito.when(command.getFromCheckpointId()).thenReturn("cp-1");

        try (MockedStatic<Script> scriptStaticMock = Mockito.mockStatic(Script.class);
                MockedConstruction<Script> scriptConstruction = Mockito.mockConstruction(Script.class, (mock, context) -> {
                    if (context.getCount() == 1) {
                        Mockito.when(mock.execute()).thenReturn("inactive");
                    } else {
                        Mockito.when(mock.execute()).thenReturn(null);
                    }
                })) {
            scriptStaticMock.when(() -> Script.runBashScriptIgnoreExitValue(
                    Mockito.anyString(), Mockito.anyInt())).thenReturn(
                    "{\"format-specific\":{\"data\":{\"bitmaps\":[{\"name\":\"cp-1\"}]}}}");

            Answer answer = wrapper.execute(command, resource);

            Assert.assertTrue(answer.getResult());
            Script startScript = scriptConstruction.constructed().get(1);
            boolean bitmapOptionAdded = Mockito.mockingDetails(startScript).getInvocations().stream().anyMatch(invocation ->
                    invocation.getMethod().getName().equals("add")
                            && invocation.getArguments().length > 0
                            && invocation.getArguments()[0] instanceof String
                            && ((String) invocation.getArguments()[0]).contains("-B cp-1"));
            Assert.assertTrue(bitmapOptionAdded);
        }
    }
}
