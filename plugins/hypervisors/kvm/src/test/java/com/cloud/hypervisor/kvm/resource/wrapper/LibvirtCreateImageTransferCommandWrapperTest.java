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

import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.eq;

import org.apache.cloudstack.backup.CreateImageTransferAnswer;
import org.apache.cloudstack.backup.CreateImageTransferCommand;
import org.apache.cloudstack.backup.ImageTransfer;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.MockedConstruction;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.mockito.junit.MockitoJUnitRunner;

import com.cloud.agent.api.Answer;
import com.cloud.hypervisor.kvm.resource.ImageServerControlSocket;
import com.cloud.hypervisor.kvm.resource.LibvirtComputingResource;
import com.cloud.utils.script.Script;

@RunWith(MockitoJUnitRunner.class)
public class LibvirtCreateImageTransferCommandWrapperTest {

    private LibvirtCreateImageTransferCommandWrapper wrapper;
    private CreateImageTransferCommand command;
    private LibvirtComputingResource resource;

    @Before
    public void setUp() {
        wrapper = new LibvirtCreateImageTransferCommandWrapper();
        command = Mockito.mock(CreateImageTransferCommand.class);
        resource = Mockito.mock(LibvirtComputingResource.class);
    }

    @Test
    public void testExecuteBlankTransferIdReturnsFailure() {
        Mockito.when(command.getTransferId()).thenReturn("");
        Mockito.when(command.getBackend()).thenReturn(ImageTransfer.Backend.nbd);

        Answer answer = wrapper.execute(command, resource);

        Assert.assertFalse(answer.getResult());
        Assert.assertEquals("transferId is empty.", answer.getDetails());
    }

    @Test
    public void testExecuteNbdBackendSuccessReturnsUrl() {
        Mockito.when(command.getTransferId()).thenReturn("tr-1");
        Mockito.when(command.getBackend()).thenReturn(ImageTransfer.Backend.nbd);
        Mockito.when(command.getIdleTimeoutSeconds()).thenReturn(120);
        Mockito.when(command.getSocket()).thenReturn("sock-1");
        Mockito.when(command.getExportName()).thenReturn("vol-1");
        Mockito.when(command.getCheckpointId()).thenReturn("cp-1");

        Mockito.when(resource.getImageServerPath()).thenReturn("/opt/cloudstack/image/server.py");
        Mockito.when(resource.getImageServerListenAddress()).thenReturn("");
        Mockito.when(resource.getPrivateIp()).thenReturn("10.0.0.10");
        Mockito.when(resource.isImageServerTlsEnabled()).thenReturn(false);

        try (MockedConstruction<Script> ignored = Mockito.mockConstruction(Script.class,
                (mock, context) -> Mockito.when(mock.execute()).thenReturn(null));
             MockedStatic<ImageServerControlSocket> imageServerControlMock = Mockito.mockStatic(ImageServerControlSocket.class)) {
            imageServerControlMock.when(ImageServerControlSocket::isReady).thenReturn(true);
            imageServerControlMock.when(() -> ImageServerControlSocket.registerTransfer(eq("tr-1"), anyMap())).thenReturn(true);

            Answer answer = wrapper.execute(command, resource);

            Assert.assertTrue(answer.getResult());
            Assert.assertTrue(answer instanceof CreateImageTransferAnswer);
            CreateImageTransferAnswer transferAnswer = (CreateImageTransferAnswer) answer;
            Assert.assertEquals("tr-1", transferAnswer.getImageTransferId());
            Assert.assertEquals(
                    "http://10.0.0.10:" + LibvirtComputingResource.IMAGE_SERVER_DEFAULT_PORT + "/images/tr-1",
                    transferAnswer.getTransferUrl());
        }
    }

    @Test
    public void testExecuteRegisterTransferFailureReturnsFailure() {
        Mockito.when(command.getTransferId()).thenReturn("tr-2");
        Mockito.when(command.getBackend()).thenReturn(ImageTransfer.Backend.nbd);
        Mockito.when(command.getIdleTimeoutSeconds()).thenReturn(120);
        Mockito.when(command.getSocket()).thenReturn("sock-2");
        Mockito.when(command.getExportName()).thenReturn("vol-2");
        Mockito.when(command.getCheckpointId()).thenReturn(null);

        Mockito.when(resource.getImageServerPath()).thenReturn("/opt/cloudstack/image/server.py");
        Mockito.when(resource.getImageServerListenAddress()).thenReturn("192.168.10.10");
        Mockito.when(resource.isImageServerTlsEnabled()).thenReturn(true);

        try (MockedConstruction<Script> ignored = Mockito.mockConstruction(Script.class,
                (mock, context) -> Mockito.when(mock.execute()).thenReturn(null));
             MockedStatic<ImageServerControlSocket> imageServerControlMock = Mockito.mockStatic(ImageServerControlSocket.class)) {
            imageServerControlMock.when(ImageServerControlSocket::isReady).thenReturn(true);
            imageServerControlMock.when(() -> ImageServerControlSocket.registerTransfer(eq("tr-2"), anyMap())).thenReturn(false);

            Answer answer = wrapper.execute(command, resource);

            Assert.assertFalse(answer.getResult());
            Assert.assertEquals("Failed to register transfer with image server.", answer.getDetails());
        }
    }
}
