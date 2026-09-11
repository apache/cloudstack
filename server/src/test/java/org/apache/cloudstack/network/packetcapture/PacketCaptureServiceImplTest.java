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
package org.apache.cloudstack.network.packetcapture;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Collections;

import org.apache.cloudstack.api.response.PacketCaptureResponse;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.MockitoJUnitRunner;

import com.cloud.agent.AgentManager;
import com.cloud.exception.InvalidParameterValueException;
import com.cloud.hypervisor.Hypervisor.HypervisorType;
import com.cloud.network.dao.NetworkDao;
import com.cloud.utils.exception.CloudRuntimeException;
import com.cloud.utils.fsm.StateMachine2;
import com.cloud.vm.NicDetailVO;
import com.cloud.vm.NicVO;
import com.cloud.vm.VMInstanceVO;
import com.cloud.vm.VirtualMachine;
import com.cloud.vm.dao.NicDao;
import com.cloud.vm.dao.NicDetailsDao;
import com.cloud.vm.dao.VMInstanceDao;

@RunWith(MockitoJUnitRunner.class)
public class PacketCaptureServiceImplTest {

    private static final long NIC_ID = 10L;
    private static final long VM_ID = 20L;
    private static final long HOST_ID = 30L;

    @Mock
    private NicDao nicDao;
    @Mock
    private NicDetailsDao nicDetailsDao;
    @Mock
    private VMInstanceDao vmInstanceDao;
    @Mock
    private NetworkDao networkDao;
    @Mock
    private AgentManager agentManager;

    @Mock
    private NicVO nic;
    @Mock
    private VMInstanceVO vm;

    @InjectMocks
    private PacketCaptureServiceImpl service = new PacketCaptureServiceImpl();

    @Before
    public void setUp() {
        when(nicDao.findById(NIC_ID)).thenReturn(nic);
        when(nic.getId()).thenReturn(NIC_ID);
        when(nic.getInstanceId()).thenReturn(VM_ID);
        when(vmInstanceDao.findById(VM_ID)).thenReturn(vm);
        when(vm.getHypervisorType()).thenReturn(HypervisorType.KVM);
    }

    private void mockRunningVm() {
        when(vm.getState()).thenReturn(VirtualMachine.State.Running);
        when(vm.getHostId()).thenReturn(HOST_ID);
    }

    private void mockAnswer(boolean success, boolean running) {
        PacketCaptureAnswer answer = Mockito.mock(PacketCaptureAnswer.class);
        when(answer.getResult()).thenReturn(success);
        if (success) {
            Mockito.lenient().when(answer.isRunning()).thenReturn(running);
        } else {
            Mockito.lenient().when(answer.getDetails()).thenReturn("something failed");
        }
        when(agentManager.easySend(eq(HOST_ID), any(PacketCaptureCommand.class))).thenReturn(answer);
    }

    private void mockEnabledDetail() {
        NicDetailVO detail = new NicDetailVO(NIC_ID, PacketCaptureService.PACKET_CAPTURE_NIC_DETAIL, "true", true);
        when(nicDetailsDao.findDetail(NIC_ID, PacketCaptureService.PACKET_CAPTURE_NIC_DETAIL)).thenReturn(detail);
    }

    @Test
    public void testEnableOnRunningVmSendsStartAndAddsDetail() {
        mockRunningVm();
        mockAnswer(true, true);

        service.enablePacketCapture(NIC_ID);

        verify(agentManager).easySend(eq(HOST_ID), any(PacketCaptureCommand.class));
        verify(nicDetailsDao).addDetail(NIC_ID, PacketCaptureService.PACKET_CAPTURE_NIC_DETAIL, "true", true);
    }

    @Test
    public void testEnableOnStoppedVmOnlyAddsDetail() {
        when(vm.getState()).thenReturn(VirtualMachine.State.Stopped);

        service.enablePacketCapture(NIC_ID);

        verify(agentManager, never()).easySend(anyLong(), any());
        verify(nicDetailsDao).addDetail(NIC_ID, PacketCaptureService.PACKET_CAPTURE_NIC_DETAIL, "true", true);
    }

    @Test
    public void testEnableFailsWithoutAddingDetailWhenHostFails() {
        mockRunningVm();
        mockAnswer(false, false);

        assertThrows(CloudRuntimeException.class, () -> service.enablePacketCapture(NIC_ID));

        verify(nicDetailsDao, never()).addDetail(anyLong(), any(), any(), Mockito.anyBoolean());
    }

    @Test
    public void testEnableRejectsNonKvm() {
        when(vm.getHypervisorType()).thenReturn(HypervisorType.VMware);

        assertThrows(InvalidParameterValueException.class, () -> service.enablePacketCapture(NIC_ID));
    }

    @Test
    public void testDisableOnRunningVmSendsStopAndRemovesDetail() {
        mockRunningVm();
        mockAnswer(true, false);

        service.disablePacketCapture(NIC_ID);

        verify(agentManager).easySend(eq(HOST_ID), any(PacketCaptureCommand.class));
        verify(nicDetailsDao).removeDetail(NIC_ID, PacketCaptureService.PACKET_CAPTURE_NIC_DETAIL);
    }

    @Test
    public void testStatusReportsEnabledAndRunning() {
        mockRunningVm();
        mockAnswer(true, true);
        mockEnabledDetail();
        when(nic.getUuid()).thenReturn("nic-uuid");
        when(vm.getUuid()).thenReturn("vm-uuid");

        PacketCaptureResponse response = service.getPacketCaptureStatus(NIC_ID);

        assertEquals(Boolean.TRUE, response.getEnabled());
        assertEquals(Boolean.TRUE, response.getRunning());
    }

    @Test
    public void testStatusDoesNotQueryHostWhenDisabled() {
        mockRunningVm();

        PacketCaptureResponse response = service.getPacketCaptureStatus(NIC_ID);

        verify(agentManager, never()).easySend(anyLong(), any());
        assertEquals(Boolean.FALSE, response.getEnabled());
        assertEquals(Boolean.FALSE, response.getRunning());
    }

    @Test
    public void testVmStartTransitionStartsEnabledCaptures() {
        mockRunningVm();
        mockAnswer(true, true);
        mockEnabledDetail();
        when(vm.getId()).thenReturn(VM_ID);
        when(nicDao.listByVmId(VM_ID)).thenReturn(Collections.singletonList(nic));

        StateMachine2.Transition<VirtualMachine.State, VirtualMachine.Event> transition = new StateMachine2.Transition<>(
                VirtualMachine.State.Starting, VirtualMachine.Event.OperationSucceeded, VirtualMachine.State.Running, null);
        service.postStateTransitionEvent(transition, vm, true, null);

        verify(agentManager).easySend(eq(HOST_ID), any(PacketCaptureCommand.class));
    }
}
