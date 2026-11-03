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

import org.apache.cloudstack.backup.FinalizeImageTransferCommand;
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
public class LibvirtFinalizeImageTransferCommandWrapperTest {

    private LibvirtFinalizeImageTransferCommandWrapper wrapper;
    private FinalizeImageTransferCommand command;
    private LibvirtComputingResource resource;

    @Before
    public void setUp() {
        wrapper = new LibvirtFinalizeImageTransferCommandWrapper();
        command = Mockito.mock(FinalizeImageTransferCommand.class);
        resource = Mockito.mock(LibvirtComputingResource.class);
    }

    @Test
    public void testExecuteBlankTransferIdReturnsFailure() {
        Mockito.when(command.getTransferId()).thenReturn("");

        Answer answer = wrapper.execute(command, resource);

        Assert.assertFalse(answer.getResult());
        Assert.assertEquals("transferId is empty.", answer.getDetails());
    }

    @Test
    public void testExecuteWithActiveTransfersReturnsSuccessWithoutStoppingServer() {
        Mockito.when(command.getTransferId()).thenReturn("tr-1");

        try (MockedStatic<ImageServerControlSocket> imageServerControlMock = Mockito.mockStatic(ImageServerControlSocket.class)) {
            imageServerControlMock.when(() -> ImageServerControlSocket.unregisterTransfer("tr-1")).thenReturn(2);

            Answer answer = wrapper.execute(command, resource);

            Assert.assertTrue(answer.getResult());
            Assert.assertEquals("Image transfer finalized.", answer.getDetails());
        }
    }

    @Test
    public void testExecuteServerUnreachableReturnsSuccessAndForcesStop() {
        Mockito.when(command.getTransferId()).thenReturn("tr-2");

        try (MockedConstruction<Script> ignored = Mockito.mockConstruction(Script.class,
                (mock, context) -> Mockito.when(mock.execute()).thenReturn("inactive"));
             MockedStatic<ImageServerControlSocket> imageServerControlMock = Mockito.mockStatic(ImageServerControlSocket.class)) {
            imageServerControlMock.when(() -> ImageServerControlSocket.unregisterTransfer("tr-2")).thenReturn(-1);

            Answer answer = wrapper.execute(command, resource);

            Assert.assertTrue(answer.getResult());
            Assert.assertEquals("Image transfer finalized (server unreachable, forced stop).", answer.getDetails());
        }
    }
}
