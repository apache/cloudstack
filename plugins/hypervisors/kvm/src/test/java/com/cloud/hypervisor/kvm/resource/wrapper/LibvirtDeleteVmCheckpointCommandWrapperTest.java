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

import java.util.Collections;
import java.util.Map;

import org.apache.cloudstack.backup.DeleteVmCheckpointCommand;
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
public class LibvirtDeleteVmCheckpointCommandWrapperTest {

    private LibvirtDeleteVmCheckpointCommandWrapper wrapper;
    private DeleteVmCheckpointCommand command;

    @Before
    public void setUp() {
        wrapper = new LibvirtDeleteVmCheckpointCommandWrapper();
        command = Mockito.mock(DeleteVmCheckpointCommand.class);
    }

    @Test
    public void testExecuteStoppedVmWithoutDisksReturnsFailure() {
        Mockito.when(command.isStoppedVM()).thenReturn(true);
        Mockito.when(command.getCheckpointId()).thenReturn("cp-1");
        Mockito.when(command.getDiskPathUuidMap()).thenReturn(Collections.emptyMap());

        Answer answer = wrapper.execute(command, Mockito.mock(LibvirtComputingResource.class));

        Assert.assertFalse(answer.getResult());
        Assert.assertEquals("No disks provided for bitmap removal", answer.getDetails());
    }

    @Test
    public void testExecuteStoppedVmBitmapRemovalFailureReturnsFailure() {
        Mockito.when(command.isStoppedVM()).thenReturn(true);
        Mockito.when(command.getCheckpointId()).thenReturn("cp-2");
        Mockito.when(command.getDiskPathUuidMap()).thenReturn(Map.of("/path/disk1.qcow2", "vol-1"));

        try (MockedConstruction<Script> ignored = Mockito.mockConstruction(Script.class,
                (mock, context) -> Mockito.when(mock.execute()).thenReturn("bitmap remove error"))) {
            Answer answer = wrapper.execute(command, Mockito.mock(LibvirtComputingResource.class));

            Assert.assertFalse(answer.getResult());
            Assert.assertTrue(answer.getDetails().contains("Failed to remove bitmap cp-2"));
        }
    }

    @Test
    public void testExecuteRunningVmDeleteCheckpointSuccess() {
        Mockito.when(command.isStoppedVM()).thenReturn(false);
        Mockito.when(command.getVmName()).thenReturn("i-2-3-VM");
        Mockito.when(command.getCheckpointId()).thenReturn("cp-3");

        try (MockedConstruction<Script> ignored = Mockito.mockConstruction(Script.class,
                (mock, context) -> Mockito.when(mock.execute()).thenReturn(null))) {
            Answer answer = wrapper.execute(command, Mockito.mock(LibvirtComputingResource.class));

            Assert.assertTrue(answer.getResult());
            Assert.assertEquals("Checkpoint deleted", answer.getDetails());
        }
    }
}
