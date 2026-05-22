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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.libvirt.Connect;
import org.libvirt.Domain;
import org.libvirt.DomainInfo;
import org.libvirt.DomainInfo.DomainState;
import org.libvirt.LibvirtException;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import com.cloud.agent.api.CheckVirtualMachineAnswer;
import com.cloud.agent.api.CheckVirtualMachineCommand;
import com.cloud.hypervisor.kvm.resource.LibvirtComputingResource;
import com.cloud.vm.VirtualMachine.PowerState;

@RunWith(MockitoJUnitRunner.class)
public class LibvirtCheckVirtualMachineCommandWrapperTest {

    private static final String VM_NAME = "i-2-3-VM";

    @Mock
    private LibvirtComputingResource libvirtComputingResource;
    @Mock
    private LibvirtUtilitiesHelper libvirtUtilitiesHelper;
    @Mock
    private Connect conn;
    @Mock
    private Domain domain;

    private LibvirtCheckVirtualMachineCommandWrapper wrapper;
    private CheckVirtualMachineCommand command;

    @Before
    public void setUp() throws LibvirtException {
        wrapper = new LibvirtCheckVirtualMachineCommandWrapper();
        command = new CheckVirtualMachineCommand(VM_NAME);

        when(libvirtComputingResource.getLibvirtUtilitiesHelper()).thenReturn(libvirtUtilitiesHelper);
        when(libvirtUtilitiesHelper.getConnectionByVmName(VM_NAME)).thenReturn(conn);
    }

    @Test
    public void testExecuteVmPoweredOnReturnsStateAndVncPort() throws LibvirtException {
        DomainInfo domainInfo = new DomainInfo();
        domainInfo.state = DomainState.VIR_DOMAIN_RUNNING;

        when(libvirtComputingResource.getVmState(conn, VM_NAME)).thenReturn(PowerState.PowerOn);
        when(libvirtComputingResource.getVncPort(conn, VM_NAME)).thenReturn(5900);
        when(conn.domainLookupByName(VM_NAME)).thenReturn(domain);
        when(domain.getInfo()).thenReturn(domainInfo);

        CheckVirtualMachineAnswer answer = (CheckVirtualMachineAnswer) wrapper.execute(command, libvirtComputingResource);

        assertTrue(answer.getResult());
        assertEquals(PowerState.PowerOn, answer.getState());
        assertEquals(Integer.valueOf(5900), answer.getVncPort());
    }

    @Test
    public void testExecuteVmPausedReturnsPowerUnknown() throws LibvirtException {
        DomainInfo domainInfo = new DomainInfo();
        domainInfo.state = DomainState.VIR_DOMAIN_PAUSED;

        when(libvirtComputingResource.getVmState(conn, VM_NAME)).thenReturn(PowerState.PowerOn);
        when(libvirtComputingResource.getVncPort(conn, VM_NAME)).thenReturn(5901);
        when(conn.domainLookupByName(VM_NAME)).thenReturn(domain);
        when(domain.getInfo()).thenReturn(domainInfo);

        CheckVirtualMachineAnswer answer = (CheckVirtualMachineAnswer) wrapper.execute(command, libvirtComputingResource);

        assertTrue(answer.getResult());
        assertEquals(PowerState.PowerUnknown, answer.getState());
        assertEquals(Integer.valueOf(5901), answer.getVncPort());
    }

    @Test
    public void testExecuteVmPoweredOffReturnsStateWithNullVncPort() throws LibvirtException {
        when(libvirtComputingResource.getVmState(conn, VM_NAME)).thenReturn(PowerState.PowerOff);

        CheckVirtualMachineAnswer answer = (CheckVirtualMachineAnswer) wrapper.execute(command, libvirtComputingResource);

        assertTrue(answer.getResult());
        assertEquals(PowerState.PowerOff, answer.getState());
        assertNull(answer.getVncPort());
    }

    @Test
    public void testExecuteVmStateUnknownReturnsStateWithNullVncPort() throws LibvirtException {
        when(libvirtComputingResource.getVmState(conn, VM_NAME)).thenReturn(PowerState.PowerUnknown);

        CheckVirtualMachineAnswer answer = (CheckVirtualMachineAnswer) wrapper.execute(command, libvirtComputingResource);

        assertTrue(answer.getResult());
        assertEquals(PowerState.PowerUnknown, answer.getState());
        assertNull(answer.getVncPort());
    }

    @Test
    public void testExecuteVmPoweredOnWithNullVncPort() throws LibvirtException {
        DomainInfo domainInfo = new DomainInfo();
        domainInfo.state = DomainState.VIR_DOMAIN_RUNNING;

        when(libvirtComputingResource.getVmState(conn, VM_NAME)).thenReturn(PowerState.PowerOn);
        when(libvirtComputingResource.getVncPort(conn, VM_NAME)).thenReturn(null);
        when(conn.domainLookupByName(VM_NAME)).thenReturn(domain);
        when(domain.getInfo()).thenReturn(domainInfo);

        CheckVirtualMachineAnswer answer = (CheckVirtualMachineAnswer) wrapper.execute(command, libvirtComputingResource);

        assertTrue(answer.getResult());
        assertEquals(PowerState.PowerOn, answer.getState());
        assertNull(answer.getVncPort());
    }

    @Test
    public void testExecuteLibvirtExceptionOnGetConnectionReturnsFailure() throws LibvirtException {
        LibvirtException libvirtException = mock(LibvirtException.class);
        when(libvirtException.getMessage()).thenReturn("Connection refused");
        when(libvirtUtilitiesHelper.getConnectionByVmName(VM_NAME)).thenThrow(libvirtException);

        CheckVirtualMachineAnswer answer = (CheckVirtualMachineAnswer) wrapper.execute(command, libvirtComputingResource);

        assertFalse(answer.getResult());
        assertEquals("Connection refused", answer.getDetails());
    }

    @Test
    public void testExecuteLibvirtExceptionOnGetVncPortReturnsFailure() throws LibvirtException {
        LibvirtException libvirtException = mock(LibvirtException.class);
        when(libvirtException.getMessage()).thenReturn("VNC port error");
        when(libvirtComputingResource.getVmState(conn, VM_NAME)).thenReturn(PowerState.PowerOn);
        when(libvirtComputingResource.getVncPort(conn, VM_NAME)).thenThrow(libvirtException);

        CheckVirtualMachineAnswer answer = (CheckVirtualMachineAnswer) wrapper.execute(command, libvirtComputingResource);

        assertFalse(answer.getResult());
        assertEquals("VNC port error", answer.getDetails());
    }

    @Test
    public void testExecuteLibvirtExceptionOnDomainLookupReturnsFailure() throws LibvirtException {
        LibvirtException libvirtException = mock(LibvirtException.class);
        when(libvirtException.getMessage()).thenReturn("Domain not found");
        when(libvirtComputingResource.getVmState(conn, VM_NAME)).thenReturn(PowerState.PowerOn);
        when(libvirtComputingResource.getVncPort(conn, VM_NAME)).thenReturn(5900);
        when(conn.domainLookupByName(VM_NAME)).thenThrow(libvirtException);

        CheckVirtualMachineAnswer answer = (CheckVirtualMachineAnswer) wrapper.execute(command, libvirtComputingResource);

        assertFalse(answer.getResult());
        assertEquals("Domain not found", answer.getDetails());
    }

    @Test
    public void testExecuteCallsGetLibvirtUtilitiesHelper() throws LibvirtException {
        when(libvirtComputingResource.getVmState(conn, VM_NAME)).thenReturn(PowerState.PowerOff);

        wrapper.execute(command, libvirtComputingResource);

        verify(libvirtComputingResource).getLibvirtUtilitiesHelper();
        verify(libvirtUtilitiesHelper).getConnectionByVmName(VM_NAME);
    }
}
