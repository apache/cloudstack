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

import org.apache.cloudstack.backup.StopBackupCommand;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.libvirt.Connect;
import org.libvirt.Domain;
import org.mockito.MockedConstruction;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.mockito.junit.MockitoJUnitRunner;

import com.cloud.agent.api.Answer;
import com.cloud.hypervisor.kvm.resource.LibvirtComputingResource;
import com.cloud.hypervisor.kvm.resource.LibvirtConnection;
import com.cloud.utils.script.Script;

@RunWith(MockitoJUnitRunner.class)
public class LibvirtStopBackupCommandWrapperTest {

    private LibvirtStopBackupCommandWrapper wrapper;
    private StopBackupCommand command;
    private LibvirtComputingResource resource;

    @Before
    public void setUp() {
        wrapper = new LibvirtStopBackupCommandWrapper();
        command = Mockito.mock(StopBackupCommand.class);
        resource = Mockito.mock(LibvirtComputingResource.class);
        Mockito.when(command.getVmName()).thenReturn("i-2-3-VM");
    }

    @Test
    public void testExecuteDomainNotFoundReturnsFailure() throws Exception {
        Connect connect = Mockito.mock(Connect.class);

        try (MockedStatic<LibvirtConnection> libvirtConnectionMock = Mockito.mockStatic(LibvirtConnection.class)) {
            libvirtConnectionMock.when(LibvirtConnection::getConnection).thenReturn(connect);
            Mockito.when(connect.domainLookupByName("i-2-3-VM")).thenReturn(null);

            Answer answer = wrapper.execute(command, resource);

            Assert.assertFalse(answer.getResult());
            Assert.assertEquals("Domain not found: i-2-3-VM", answer.getDetails());
        }
    }

    @Test
    public void testExecuteDomainFoundReturnsSuccess() throws Exception {
        Connect connect = Mockito.mock(Connect.class);
        Domain domain = Mockito.mock(Domain.class);

        try (MockedConstruction<Script> ignored = Mockito.mockConstruction(Script.class,
                (mock, context) -> Mockito.when(mock.execute()).thenReturn(null));
             MockedStatic<LibvirtConnection> libvirtConnectionMock = Mockito.mockStatic(LibvirtConnection.class)) {
            libvirtConnectionMock.when(LibvirtConnection::getConnection).thenReturn(connect);
            Mockito.when(connect.domainLookupByName("i-2-3-VM")).thenReturn(domain);

            Answer answer = wrapper.execute(command, resource);

            Assert.assertTrue(answer.getResult());
            Assert.assertEquals("Backup stopped successfully", answer.getDetails());
        }
    }

    @Test
    public void testExecuteLibvirtConnectionFailureReturnsFailure() {
        try (MockedStatic<LibvirtConnection> libvirtConnectionMock = Mockito.mockStatic(LibvirtConnection.class)) {
            libvirtConnectionMock.when(LibvirtConnection::getConnection).thenThrow(new RuntimeException("libvirt unavailable"));

            Answer answer = wrapper.execute(command, resource);

            Assert.assertFalse(answer.getResult());
            Assert.assertTrue(answer.getDetails().contains("Error stopping backup: libvirt unavailable"));
        }
    }
}
