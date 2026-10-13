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

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.libvirt.Connect;
import org.libvirt.Domain;
import org.libvirt.LibvirtException;
import org.mockito.junit.MockitoJUnitRunner;

import com.cloud.agent.api.Answer;
import com.cloud.agent.api.UpdateNicVlanMembershipCommand;
import com.cloud.agent.api.to.NicTO;
import com.cloud.exception.InternalErrorException;
import com.cloud.hypervisor.kvm.resource.LibvirtComputingResource;
import com.cloud.hypervisor.kvm.resource.LibvirtVMDef.InterfaceDef;
import com.cloud.hypervisor.kvm.resource.VifDriver;
import com.cloud.network.Networks;
import com.cloud.utils.exception.CloudRuntimeException;

@RunWith(MockitoJUnitRunner.class)
public class LibvirtUpdateNicVlanMembershipCommandWrapperTest {

    private static final String VM_NAME = "i-2-3-VM";

    private LibvirtComputingResource libvirtComputingResource;
    private LibvirtUtilitiesHelper libvirtUtilitiesHelper;
    private Connect conn;
    private Domain domain;
    private InterfaceDef iface;
    private VifDriver vifDriver;
    private NicTO nic;
    private UpdateNicVlanMembershipCommand command;
    private LibvirtUpdateNicVlanMembershipCommandWrapper wrapper;

    @Before
    public void setUp() throws Exception {
        libvirtComputingResource = mock(LibvirtComputingResource.class);
        libvirtUtilitiesHelper = mock(LibvirtUtilitiesHelper.class);
        conn = mock(Connect.class);
        domain = mock(Domain.class);
        iface = mock(InterfaceDef.class);
        vifDriver = mock(VifDriver.class);

        nic = new NicTO();
        nic.setMac("02:00:00:00:00:01");
        nic.setType(Networks.TrafficType.Guest);
        nic.setName("cloudbr1");

        command = new UpdateNicVlanMembershipCommand(nic, VM_NAME);
        wrapper = new LibvirtUpdateNicVlanMembershipCommandWrapper();

        when(libvirtComputingResource.getLibvirtUtilitiesHelper()).thenReturn(libvirtUtilitiesHelper);
        when(libvirtUtilitiesHelper.getConnectionByVmName(VM_NAME)).thenReturn(conn);
        when(libvirtComputingResource.getDomain(conn, VM_NAME)).thenReturn(domain);
        when(libvirtComputingResource.getInterface(conn, VM_NAME, nic.getMac())).thenReturn(iface);
        when(libvirtComputingResource.getVifDriver(nic.getType(), nic.getName())).thenReturn(vifDriver);
    }

    @Test
    public void executeReturnsSuccessAndUpdatesMembershipOnSuccess() throws Exception {
        Answer answer = wrapper.execute(command, libvirtComputingResource);

        verify(vifDriver).updateVlanTrunkMembership(domain, iface, nic);
        assertTrue(answer.getResult());
        verify(domain).free();
    }

    @Test
    public void executeReturnsFailureAnswerOnLibvirtException() throws Exception {
        LibvirtException libvirtException = mock(LibvirtException.class);
        when(libvirtException.toString()).thenReturn("libvirt failure");
        doThrow(libvirtException).when(vifDriver).updateVlanTrunkMembership(any(), any(), any());

        Answer answer = wrapper.execute(command, libvirtComputingResource);

        assertFalse(answer.getResult());
        assertTrue(answer.getDetails().contains("libvirt failure"));
        verify(domain).free();
    }

    @Test
    public void executeReturnsFailureAnswerOnInternalErrorException() throws Exception {
        doThrow(new InternalErrorException("bad vlan state")).when(vifDriver).updateVlanTrunkMembership(any(), any(), any());

        Answer answer = wrapper.execute(command, libvirtComputingResource);

        assertFalse(answer.getResult());
        assertTrue(answer.getDetails().contains("bad vlan state"));
        verify(domain).free();
    }

    @Test
    public void executeReturnsFailureAnswerOnCloudRuntimeException() throws Exception {
        doThrow(new CloudRuntimeException("unexpected failure")).when(vifDriver).updateVlanTrunkMembership(any(), any(), any());

        Answer answer = wrapper.execute(command, libvirtComputingResource);

        assertFalse(answer.getResult());
        assertTrue(answer.getDetails().contains("unexpected failure"));
        verify(domain).free();
    }
}
