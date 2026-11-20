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

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import javax.inject.Inject;
import javax.naming.ConfigurationException;

import org.apache.cloudstack.api.command.admin.nic.DisablePacketCaptureCmd;
import org.apache.cloudstack.api.command.admin.nic.EnablePacketCaptureCmd;
import org.apache.cloudstack.api.command.admin.nic.GetPacketCaptureStatusCmd;
import org.apache.cloudstack.api.response.PacketCaptureResponse;

import com.cloud.agent.AgentManager;
import com.cloud.agent.api.Answer;
import com.cloud.event.ActionEvent;
import com.cloud.event.EventTypes;
import com.cloud.exception.InvalidParameterValueException;
import com.cloud.hypervisor.Hypervisor.HypervisorType;
import com.cloud.network.dao.NetworkDao;
import com.cloud.network.dao.NetworkVO;
import com.cloud.utils.component.ManagerBase;
import com.cloud.utils.component.PluggableService;
import com.cloud.utils.exception.CloudRuntimeException;
import com.cloud.utils.fsm.StateListener;
import com.cloud.utils.fsm.StateMachine2;
import com.cloud.vm.NicDetailVO;
import com.cloud.vm.NicVO;
import com.cloud.vm.VMInstanceVO;
import com.cloud.vm.VirtualMachine;
import com.cloud.vm.VirtualMachine.Event;
import com.cloud.vm.VirtualMachine.State;
import com.cloud.vm.dao.NicDao;
import com.cloud.vm.dao.NicDetailsDao;
import com.cloud.vm.dao.VMInstanceDao;

public class PacketCaptureServiceImpl extends ManagerBase implements PacketCaptureService, PluggableService,
        StateListener<State, Event, VirtualMachine> {

    @Inject
    private NicDao nicDao;
    @Inject
    private NicDetailsDao nicDetailsDao;
    @Inject
    private VMInstanceDao vmInstanceDao;
    @Inject
    private NetworkDao networkDao;
    @Inject
    private AgentManager agentManager;

    @Override
    public boolean configure(String name, Map<String, Object> params) throws ConfigurationException {
        VirtualMachine.State.getStateMachine().registerListener(this);
        return true;
    }

    @Override
    @ActionEvent(eventType = EventTypes.EVENT_NIC_PACKET_CAPTURE_ENABLE, eventDescription = "enabling packet capture", async = true)
    public void enablePacketCapture(long nicId) {
        NicVO nic = validateAndGetNic(nicId);
        VMInstanceVO vm = validateAndGetVm(nic);
        if (isVmRunningOnHost(vm)) {
            sendCommand(PacketCaptureCommand.Action.START, vm, nic);
        }
        nicDetailsDao.removeDetail(nic.getId(), PACKET_CAPTURE_NIC_DETAIL);
        nicDetailsDao.addDetail(nic.getId(), PACKET_CAPTURE_NIC_DETAIL, Boolean.TRUE.toString(), true);
        logger.info("Enabled packet capture on NIC {} of VM {}", nic, vm);
    }

    @Override
    @ActionEvent(eventType = EventTypes.EVENT_NIC_PACKET_CAPTURE_DISABLE, eventDescription = "disabling packet capture", async = true)
    public void disablePacketCapture(long nicId) {
        NicVO nic = validateAndGetNic(nicId);
        VMInstanceVO vm = validateAndGetVm(nic);
        if (isVmRunningOnHost(vm)) {
            sendCommand(PacketCaptureCommand.Action.STOP, vm, nic);
        }
        nicDetailsDao.removeDetail(nic.getId(), PACKET_CAPTURE_NIC_DETAIL);
        logger.info("Disabled packet capture on NIC {} of VM {}", nic, vm);
    }

    @Override
    public PacketCaptureResponse getPacketCaptureStatus(long nicId) {
        NicVO nic = validateAndGetNic(nicId);
        VMInstanceVO vm = validateAndGetVm(nic);

        boolean running = false;
        if (isPacketCaptureEnabled(nic.getId()) && isVmRunningOnHost(vm)) {
            PacketCaptureAnswer answer = sendCommand(PacketCaptureCommand.Action.STATUS, vm, nic);
            running = answer.isRunning();
        }

        PacketCaptureResponse response = new PacketCaptureResponse();
        response.setNicId(nic.getUuid());
        response.setVirtualMachineId(vm.getUuid());
        response.setVirtualMachineName(vm.getInstanceName());
        response.setMacAddress(nic.getMacAddress());
        response.setEnabled(isPacketCaptureEnabled(nic.getId()));
        response.setRunning(running);
        return response;
    }

    @Override
    public boolean preStateTransitionEvent(State oldState, Event event, State newState, VirtualMachine vo, boolean status, Object opaque) {
        return true;
    }

    @Override
    public boolean postStateTransitionEvent(StateMachine2.Transition<State, Event> transition, VirtualMachine vm, boolean status, Object opaque) {
        if (!status) {
            return true;
        }
        State oldState = transition.getCurrentState();
        State newState = transition.getToState();
        Event event = transition.getEvent();
        if (State.isVmStarted(oldState, event, newState) || State.isVmMigrated(oldState, event, newState)) {
            startEnabledCapturesForVm(vm);
        }
        return true;
    }

    /**
     * Starts the capture on the current host of the VM for every NIC that has
     * packet capture enabled. Called after a VM started or migrated; failures
     * are logged and do not fail the VM operation.
     */
    private void startEnabledCapturesForVm(VirtualMachine vm) {
        if (vm.getHypervisorType() != HypervisorType.KVM || vm.getHostId() == null) {
            return;
        }
        for (NicVO nic : nicDao.listByVmId(vm.getId())) {
            if (!isPacketCaptureEnabled(nic.getId())) {
                continue;
            }
            try {
                VMInstanceVO vmVo = vmInstanceDao.findById(vm.getId());
                sendCommand(PacketCaptureCommand.Action.START, vmVo, nic);
                logger.info("Started packet capture on NIC {} of VM {} on host {}", nic, vm, vm.getHostId());
            } catch (Exception e) {
                logger.warn("Failed to start packet capture on NIC {} of VM {} on host {}", nic, vm, vm.getHostId(), e);
            }
        }
    }

    private boolean isPacketCaptureEnabled(long nicId) {
        NicDetailVO detail = nicDetailsDao.findDetail(nicId, PACKET_CAPTURE_NIC_DETAIL);
        return detail != null && Boolean.parseBoolean(detail.getValue());
    }

    private NicVO validateAndGetNic(long nicId) {
        NicVO nic = nicDao.findById(nicId);
        if (nic == null || nic.getRemoved() != null) {
            throw new InvalidParameterValueException("Unable to find a NIC with the specified id");
        }
        return nic;
    }

    private VMInstanceVO validateAndGetVm(NicVO nic) {
        Long vmId = nic.getInstanceId();
        VMInstanceVO vm = vmId == null ? null : vmInstanceDao.findById(vmId);
        if (vm == null) {
            throw new InvalidParameterValueException(String.format("NIC %s is not attached to an Instance", nic.getUuid()));
        }
        if (vm.getHypervisorType() != null && vm.getHypervisorType() != HypervisorType.KVM) {
            throw new InvalidParameterValueException("Packet capture is only supported on KVM");
        }
        return vm;
    }

    private boolean isVmRunningOnHost(VMInstanceVO vm) {
        return vm.getState() == State.Running && vm.getHostId() != null;
    }

    private PacketCaptureAnswer sendCommand(PacketCaptureCommand.Action action, VMInstanceVO vm, NicVO nic) {
        NetworkVO network = networkDao.findById(nic.getNetworkId());
        PacketCaptureCommand command = new PacketCaptureCommand(action, vm.getInstanceName(), vm.getUuid(),
                nic.getUuid(), nic.getMacAddress(), nic.getIPv4Address(), nic.getIPv6Address(),
                network == null ? null : network.getUuid());
        Answer answer = agentManager.easySend(vm.getHostId(), command);
        if (answer == null || !answer.getResult()) {
            throw new CloudRuntimeException(String.format("Failed to %s packet capture for NIC %s of VM %s on host %d: %s",
                    action.name().toLowerCase(), nic.getUuid(), vm.getInstanceName(), vm.getHostId(),
                    answer == null ? "no answer from host" : answer.getDetails()));
        }
        return (PacketCaptureAnswer) answer;
    }

    @Override
    public List<Class<?>> getCommands() {
        List<Class<?>> cmdList = new ArrayList<>();
        cmdList.add(EnablePacketCaptureCmd.class);
        cmdList.add(DisablePacketCaptureCmd.class);
        cmdList.add(GetPacketCaptureStatusCmd.class);
        return cmdList;
    }
}
