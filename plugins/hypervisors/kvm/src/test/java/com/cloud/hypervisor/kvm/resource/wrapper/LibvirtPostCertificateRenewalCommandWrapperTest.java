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

import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import org.apache.cloudstack.ca.PostCertificateRenewalCommand;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.libvirt.Connect;
import org.libvirt.Domain;
import org.libvirt.LibvirtException;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockedConstruction;
import org.mockito.junit.MockitoJUnitRunner;

import com.cloud.agent.api.Answer;
import com.cloud.hypervisor.kvm.resource.LibvirtComputingResource;
import com.cloud.utils.script.Script;

@RunWith(MockitoJUnitRunner.class)
public class LibvirtPostCertificateRenewalCommandWrapperTest {

    private static final long SUPPORTED_QEMU_VERSION = 6000000L;

    @Mock
    private LibvirtComputingResource libvirtComputingResource;
    @Mock
    private LibvirtUtilitiesHelper libvirtUtilitiesHelper;
    @Mock
    private Connect connect;

    private final LibvirtPostCertificateRenewalCommandWrapper wrapper = new LibvirtPostCertificateRenewalCommandWrapper();

    @Before
    public void setUp() {
        when(libvirtComputingResource.getHypervisorQemuVersion()).thenReturn(SUPPORTED_QEMU_VERSION);
    }

    private Answer executeWithScriptMocked() {
        try (MockedConstruction<Script> ignored = Mockito.mockConstruction(Script.class)) {
            return wrapper.execute(new PostCertificateRenewalCommand(), libvirtComputingResource);
        }
    }

    @Test
    public void testExecuteReloadsVncTlsCertificateForEachRunningVm() throws Exception {
        final Domain vm1 = mock(Domain.class);
        when(vm1.getName()).thenReturn("i-2-3-VM");
        final Domain vm2 = mock(Domain.class);
        when(vm2.getName()).thenReturn("i-4-5-VM");

        when(libvirtComputingResource.getLibvirtUtilitiesHelper()).thenReturn(libvirtUtilitiesHelper);
        when(libvirtUtilitiesHelper.getConnection()).thenReturn(connect);
        when(connect.listDomains()).thenReturn(new int[]{1, 2});
        when(connect.domainLookupByID(1)).thenReturn(vm1);
        when(connect.domainLookupByID(2)).thenReturn(vm2);

        final Answer answer = executeWithScriptMocked();

        assertTrue(answer.getResult());
        final ArgumentCaptor<String> monitorCommandCaptor = ArgumentCaptor.forClass(String.class);
        verify(vm1, times(1)).qemuMonitorCommand(monitorCommandCaptor.capture(), Mockito.eq(0));
        verify(vm2, times(1)).qemuMonitorCommand(Mockito.anyString(), Mockito.eq(0));
        final String capturedCommand = monitorCommandCaptor.getValue();
        assertTrue(capturedCommand.contains("display-reload"));
        assertTrue(capturedCommand.contains("\"type\":\"vnc\""));
        assertTrue(capturedCommand.contains("\"tls-certs\":true"));
        verify(vm1, times(1)).free();
        verify(vm2, times(1)).free();
    }

    @Test
    public void testExecuteContinuesWithOtherVmsWhenOneReloadFails() throws Exception {
        final Domain failingVm = mock(Domain.class);
        when(failingVm.getName()).thenReturn("i-2-3-VM");
        when(failingVm.qemuMonitorCommand(Mockito.anyString(), Mockito.eq(0))).thenThrow(mock(LibvirtException.class));
        final Domain workingVm = mock(Domain.class);
        when(workingVm.getName()).thenReturn("i-4-5-VM");

        when(libvirtComputingResource.getLibvirtUtilitiesHelper()).thenReturn(libvirtUtilitiesHelper);
        when(libvirtUtilitiesHelper.getConnection()).thenReturn(connect);
        when(connect.listDomains()).thenReturn(new int[]{1, 2});
        when(connect.domainLookupByID(1)).thenReturn(failingVm);
        when(connect.domainLookupByID(2)).thenReturn(workingVm);

        final Answer answer = executeWithScriptMocked();

        assertTrue(answer.getResult());
        verify(workingVm, times(1)).qemuMonitorCommand(Mockito.anyString(), Mockito.eq(0));
        verify(failingVm, times(1)).free();
        verify(workingVm, times(1)).free();
    }

    @Test
    public void testExecuteContinuesWhenNoRunningVms() throws Exception {
        when(libvirtComputingResource.getLibvirtUtilitiesHelper()).thenReturn(libvirtUtilitiesHelper);
        when(libvirtUtilitiesHelper.getConnection()).thenReturn(connect);
        when(connect.listDomains()).thenReturn(new int[]{});

        final Answer answer = executeWithScriptMocked();

        assertTrue(answer.getResult());
    }

    @Test
    public void testExecuteHandlesUnableToListRunningVms() throws Exception {
        when(libvirtComputingResource.getLibvirtUtilitiesHelper()).thenReturn(libvirtUtilitiesHelper);
        when(libvirtUtilitiesHelper.getConnection()).thenThrow(mock(LibvirtException.class));

        final Answer answer = executeWithScriptMocked();

        assertTrue(answer.getResult());
    }

    @Test
    public void testExecuteSkipsVncCertReloadWhenQemuVersionTooOld() throws Exception {
        when(libvirtComputingResource.getHypervisorQemuVersion()).thenReturn(5002000L);

        final Answer answer = executeWithScriptMocked();

        assertTrue(answer.getResult());
        verifyNoInteractions(libvirtUtilitiesHelper);
    }
}
