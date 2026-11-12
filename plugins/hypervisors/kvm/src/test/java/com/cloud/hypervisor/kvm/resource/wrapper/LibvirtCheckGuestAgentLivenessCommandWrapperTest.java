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
//

package com.cloud.hypervisor.kvm.resource.wrapper;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
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
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import com.cloud.agent.api.CheckGuestAgentLivenessAnswer;
import com.cloud.agent.api.CheckGuestAgentLivenessCommand;
import com.cloud.hypervisor.kvm.resource.LibvirtComputingResource;

@RunWith(MockitoJUnitRunner.class)
public class LibvirtCheckGuestAgentLivenessCommandWrapperTest {

    private static final String VM_NAME = "i-2-3-VM";

    @Mock
    private LibvirtComputingResource libvirtComputingResource;
    @Mock
    private LibvirtUtilitiesHelper libvirtUtilitiesHelper;
    @Mock
    private Connect conn;
    @Mock
    private Domain domain;

    private LibvirtCheckGuestAgentLivenessCommandWrapper wrapper;
    private CheckGuestAgentLivenessCommand command;

    @Before
    public void setUp() throws LibvirtException {
        wrapper = new LibvirtCheckGuestAgentLivenessCommandWrapper();
        command = new CheckGuestAgentLivenessCommand(VM_NAME);

        when(libvirtComputingResource.getLibvirtUtilitiesHelper()).thenReturn(libvirtUtilitiesHelper);
        when(libvirtUtilitiesHelper.getConnection()).thenReturn(conn);
    }

    private void mockRunningDomain() throws LibvirtException {
        when(libvirtComputingResource.getDomain(conn, VM_NAME)).thenReturn(domain);
        DomainInfo domainInfo = new DomainInfo();
        domainInfo.state = DomainState.VIR_DOMAIN_RUNNING;
        when(domain.getInfo()).thenReturn(domainInfo);
    }

    @Test
    public void domainNotFoundIsNotAlive() throws LibvirtException {
        when(libvirtComputingResource.getDomain(conn, VM_NAME)).thenReturn(null);

        CheckGuestAgentLivenessAnswer answer = (CheckGuestAgentLivenessAnswer) wrapper.execute(command, libvirtComputingResource);

        assertFalse(answer.getResult());
        assertTrue(answer.getDetails().contains("was not found"));
    }

    @Test
    public void domainNotRunningIsNotAlive() throws LibvirtException {
        when(libvirtComputingResource.getDomain(conn, VM_NAME)).thenReturn(domain);
        DomainInfo domainInfo = new DomainInfo();
        domainInfo.state = DomainState.VIR_DOMAIN_PAUSED;
        when(domain.getInfo()).thenReturn(domainInfo);

        CheckGuestAgentLivenessAnswer answer = (CheckGuestAgentLivenessAnswer) wrapper.execute(command, libvirtComputingResource);

        assertFalse(answer.getResult());
        assertTrue(answer.getDetails().contains("VIR_DOMAIN_PAUSED"));
        verify(domain, org.mockito.Mockito.never()).qemuAgentCommand(anyString(), anyInt(), anyInt());
    }

    @Test
    public void libvirtExceptionOnConnectionIsNotAlive() throws LibvirtException {
        LibvirtException libvirtException = mock(LibvirtException.class);
        when(libvirtException.getMessage()).thenReturn("connection refused");
        when(libvirtUtilitiesHelper.getConnection()).thenThrow(libvirtException);

        CheckGuestAgentLivenessAnswer answer = (CheckGuestAgentLivenessAnswer) wrapper.execute(command, libvirtComputingResource);

        assertFalse(answer.getResult());
        assertTrue(answer.getDetails().contains("connection refused"));
    }

    @Test
    public void positiveJsonReturnIsAlive() throws LibvirtException {
        mockRunningDomain();
        when(domain.qemuAgentCommand(anyString(), anyInt(), anyInt())).thenReturn("{\"return\": {}}");

        CheckGuestAgentLivenessAnswer answer = (CheckGuestAgentLivenessAnswer) wrapper.execute(command, libvirtComputingResource);

        assertTrue(answer.getResult());
    }

    @Test
    public void errorFieldInJsonReturnIsNotAlive() throws LibvirtException {
        mockRunningDomain();
        when(domain.qemuAgentCommand(anyString(), anyInt(), anyInt())).thenReturn("{\"return\": {}, \"error\": {\"desc\": \"timeout\"}}");

        CheckGuestAgentLivenessAnswer answer = (CheckGuestAgentLivenessAnswer) wrapper.execute(command, libvirtComputingResource);

        assertFalse(answer.getResult());
    }

    @Test
    public void nonJsonObjectReturnIsNotAlive() throws LibvirtException {
        mockRunningDomain();
        when(domain.qemuAgentCommand(anyString(), anyInt(), anyInt())).thenReturn("[]");

        CheckGuestAgentLivenessAnswer answer = (CheckGuestAgentLivenessAnswer) wrapper.execute(command, libvirtComputingResource);

        assertFalse(answer.getResult());
    }

    @Test
    public void missingReturnFieldIsNotAlive() throws LibvirtException {
        mockRunningDomain();
        when(domain.qemuAgentCommand(anyString(), anyInt(), anyInt())).thenReturn("{\"foo\": \"bar\"}");

        CheckGuestAgentLivenessAnswer answer = (CheckGuestAgentLivenessAnswer) wrapper.execute(command, libvirtComputingResource);

        assertFalse(answer.getResult());
    }

    @Test
    public void invalidJsonIsNotAliveAndMessageIsAbbreviated() throws LibvirtException {
        mockRunningDomain();
        String garbage = "{\"return\": \"" + "x".repeat(300);
        when(domain.qemuAgentCommand(anyString(), anyInt(), anyInt())).thenReturn(garbage);

        CheckGuestAgentLivenessAnswer answer = (CheckGuestAgentLivenessAnswer) wrapper.execute(command, libvirtComputingResource);

        assertFalse(answer.getResult());
        assertTrue(answer.getDetails().contains("invalid JSON"));
        assertTrue(answer.getDetails().endsWith("..."));
        assertTrue(answer.getDetails().length() < garbage.length());
    }

    @Test
    public void emptyResponseIsNotAlive() throws LibvirtException {
        mockRunningDomain();
        when(domain.qemuAgentCommand(anyString(), anyInt(), anyInt())).thenReturn("   ");

        CheckGuestAgentLivenessAnswer answer = (CheckGuestAgentLivenessAnswer) wrapper.execute(command, libvirtComputingResource);

        assertFalse(answer.getResult());
        assertTrue(answer.getDetails().contains("empty response"));
    }

    @Test
    public void nullResponseIsNotAlive() throws LibvirtException {
        mockRunningDomain();
        when(domain.qemuAgentCommand(anyString(), anyInt(), anyInt())).thenReturn(null);

        CheckGuestAgentLivenessAnswer answer = (CheckGuestAgentLivenessAnswer) wrapper.execute(command, libvirtComputingResource);

        assertFalse(answer.getResult());
    }

    @Test
    public void zeroWaitIsFlooredToMinimumTimeoutSeconds() throws LibvirtException {
        mockRunningDomain();
        command.setWait(0);
        when(domain.qemuAgentCommand(anyString(), anyInt(), anyInt())).thenReturn("{\"return\": {}}");

        wrapper.execute(command, libvirtComputingResource);

        ArgumentCaptor<Integer> timeoutCaptor = ArgumentCaptor.forClass(Integer.class);
        verify(domain).qemuAgentCommand(anyString(), timeoutCaptor.capture(), anyInt());
        assertEquals(Integer.valueOf(1), timeoutCaptor.getValue());
    }

    @Test
    public void positiveWaitIsUsedAsTimeoutSeconds() throws LibvirtException {
        mockRunningDomain();
        command.setWait(10);
        when(domain.qemuAgentCommand(anyString(), anyInt(), anyInt())).thenReturn("{\"return\": {}}");

        wrapper.execute(command, libvirtComputingResource);

        ArgumentCaptor<Integer> timeoutCaptor = ArgumentCaptor.forClass(Integer.class);
        verify(domain).qemuAgentCommand(anyString(), timeoutCaptor.capture(), anyInt());
        assertEquals(Integer.valueOf(10), timeoutCaptor.getValue());
    }

    @Test
    public void domainIsFreedAfterExecution() throws LibvirtException {
        mockRunningDomain();
        when(domain.qemuAgentCommand(anyString(), anyInt(), anyInt())).thenReturn("{\"return\": {}}");

        wrapper.execute(command, libvirtComputingResource);

        verify(domain).free();
    }

    @Test
    public void domainFreeExceptionIsSwallowed() throws LibvirtException {
        mockRunningDomain();
        when(domain.qemuAgentCommand(anyString(), anyInt(), anyInt())).thenReturn("{\"return\": {}}");
        org.mockito.Mockito.doThrow(mock(LibvirtException.class)).when(domain).free();

        CheckGuestAgentLivenessAnswer answer = (CheckGuestAgentLivenessAnswer) wrapper.execute(command, libvirtComputingResource);

        assertTrue(answer.getResult());
    }
}
