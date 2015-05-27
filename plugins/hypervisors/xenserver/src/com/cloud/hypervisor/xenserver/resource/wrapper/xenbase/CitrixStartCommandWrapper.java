//
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

package com.cloud.hypervisor.xenserver.resource.wrapper.xenbase;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.log4j.Logger;

import com.cloud.agent.api.Answer;
import com.cloud.agent.api.OvsSetTagAndFlowAnswer;
import com.cloud.agent.api.OvsSetTagAndFlowCommand;
import com.cloud.agent.api.StartAnswer;
import com.cloud.agent.api.StartCommand;
import com.cloud.agent.api.to.DiskTO;
import com.cloud.agent.api.to.GPUDeviceTO;
import com.cloud.agent.api.to.NicTO;
import com.cloud.agent.api.to.VirtualMachineTO;
import com.cloud.hypervisor.xenserver.resource.CitrixResourceBase;
import com.cloud.network.Networks;
import com.cloud.network.Networks.BroadcastDomainType;
import com.cloud.network.Networks.IsolationType;
import com.cloud.resource.CommandWrapper;
import com.cloud.resource.ResourceWrapper;
import com.cloud.vm.VirtualMachine;
import com.xensource.xenapi.Connection;
import com.xensource.xenapi.Host;
import com.xensource.xenapi.Types.VmPowerState;
import com.xensource.xenapi.VDI;
import com.xensource.xenapi.VM;

@ResourceWrapper(handles =  StartCommand.class)
public final class CitrixStartCommandWrapper extends CommandWrapper<StartCommand, Answer, CitrixResourceBase> {

    private static final Logger s_logger = Logger.getLogger(CitrixStartCommandWrapper.class);

    @Override
    public Answer execute(final StartCommand command, final CitrixResourceBase citrixResourceBase) {
        final Connection conn = citrixResourceBase.getConnection();
        final VirtualMachineTO vmSpec = command.getVirtualMachine();
        final String vmName = vmSpec.getName();
        VmPowerState state = VmPowerState.HALTED;
        VM vm = null;
        // if a VDI is created, record its UUID to send back to the CS MS
        final Map<String, String> iqnToPath = new HashMap<String, String>();
        try {
            final Set<VM> vms = VM.getByNameLabel(conn, vmName);
            if (vms != null) {
                for (final VM v : vms) {
                    final VM.Record vRec = v.getRecord(conn);
                    if (vRec.powerState == VmPowerState.HALTED) {
                        v.destroy(conn);
                    } else if (vRec.powerState == VmPowerState.RUNNING) {
                        final String host = vRec.residentOn.getUuid(conn);
                        final String msg = "VM " + vmName + " is runing on host " + host;
                        s_logger.debug(msg);
                        return new StartAnswer(command, msg, host);
                    } else {
                        final String msg = "There is already a VM having the same name " + vmName + " vm record " + vRec.toString();
                        s_logger.warn(msg);
                        return new StartAnswer(command, msg);
                    }
                }
            }
            s_logger.debug("1. The VM " + vmName + " is in Starting state.");

            final Host host = Host.getByUuid(conn, citrixResourceBase.getHost().getUuid());
            vm = citrixResourceBase.createVmFromTemplate(conn, vmSpec, host);

            final GPUDeviceTO gpuDevice = vmSpec.getGpuDevice();
            if (gpuDevice != null) {
                s_logger.debug("Creating VGPU for of VGPU type: " + gpuDevice.getVgpuType() + " in GPU group " + gpuDevice.getGpuGroup() + " for VM " + vmName);
                citrixResourceBase.createVGPU(conn, command, vm, gpuDevice);
            }

            for (final DiskTO disk : vmSpec.getDisks()) {
                final VDI newVdi = citrixResourceBase.prepareManagedDisk(conn, disk, vmName);

                if (newVdi != null) {
                    final String path = newVdi.getUuid(conn);

                    iqnToPath.put(disk.getDetails().get(DiskTO.IQN), path);
                }

                citrixResourceBase.createVbd(conn, disk, vmName, vm, vmSpec.getBootloader(), newVdi);
            }

            if (vmSpec.getType() != VirtualMachine.Type.User) {
                citrixResourceBase.createPatchVbd(conn, vmName, vm);
            }

            for (final NicTO nic : vmSpec.getNics()) {
                citrixResourceBase.createVif(conn, vmName, vm, vmSpec, nic);
            }

            citrixResourceBase.startVM(conn, host, vm, vmName);

            if (citrixResourceBase.isOvs()) {
                // TODO(Salvatore-orlando): This code should go
                for (final NicTO nic : vmSpec.getNics()) {
                    if (nic.getBroadcastType() == Networks.BroadcastDomainType.Vswitch) {
                        final HashMap<String, String> args = citrixResourceBase.parseDefaultOvsRuleComamnd(BroadcastDomainType.getValue(nic.getBroadcastUri()));
                        final OvsSetTagAndFlowCommand flowCmd = new OvsSetTagAndFlowCommand(args.get("vmName"), args.get("tag"), args.get("vlans"), args.get("seqno"),
                                Long.parseLong(args.get("vmId")));

                        final CitrixRequestWrapper citrixRequestWrapper = CitrixRequestWrapper.getInstance();

                        final OvsSetTagAndFlowAnswer r = (OvsSetTagAndFlowAnswer) citrixRequestWrapper.execute(flowCmd, citrixResourceBase);

                        if (!r.getResult()) {
                            s_logger.warn("Failed to set flow for VM " + r.getVmId());
                        } else {
                            s_logger.info("Success to set flow for VM " + r.getVmId());
                        }
                    }
                }
            }

            if (citrixResourceBase.canBridgeFirewall()) {
                String result = null;
                if (vmSpec.getType() != VirtualMachine.Type.User) {
                    final NicTO[] nics = vmSpec.getNics();
                    boolean secGrpEnabled = false;
                    for (final NicTO nic : nics) {
                        if (nic.isSecurityGroupEnabled() || nic.getIsolationUri() != null && nic.getIsolationUri().getScheme().equalsIgnoreCase(IsolationType.Ec2.toString())) {
                            secGrpEnabled = true;
                            break;
                        }
                    }
                    if (secGrpEnabled) {
                        result = citrixResourceBase.callHostPlugin(conn, "vmops", "default_network_rules_systemvm", "vmName", vmName);
                        if (result == null || result.isEmpty() || !Boolean.parseBoolean(result)) {
                            s_logger.warn("Failed to program default network rules for " + vmName);
                        } else {
                            s_logger.info("Programmed default network rules for " + vmName);
                        }
                    }

                } else {
                    // For user vm, program the rules for each nic if the
                    // isolation uri scheme is ec2
                    final NicTO[] nics = vmSpec.getNics();
                    for (final NicTO nic : nics) {
                        if (nic.isSecurityGroupEnabled() || nic.getIsolationUri() != null && nic.getIsolationUri().getScheme().equalsIgnoreCase(IsolationType.Ec2.toString())) {
                            final List<String> nicSecIps = nic.getNicSecIps();
                            String secIpsStr;
                            final StringBuilder sb = new StringBuilder();
                            if (nicSecIps != null) {
                                for (final String ip : nicSecIps) {
                                    sb.append(ip).append(":");
                                }
                                secIpsStr = sb.toString();
                            } else {
                                secIpsStr = "0:";
                            }
                            result = citrixResourceBase.callHostPlugin(conn, "vmops", "default_network_rules", "vmName", vmName, "vmIP", nic.getIp(), "vmMAC", nic.getMac(),
                                    "vmID", Long.toString(vmSpec.getId()), "secIps", secIpsStr);

                            if (result == null || result.isEmpty() || !Boolean.parseBoolean(result)) {
                                s_logger.warn("Failed to program default network rules for " + vmName + " on nic with ip:" + nic.getIp() + " mac:" + nic.getMac());
                            } else {
                                s_logger.info("Programmed default network rules for " + vmName + " on nic with ip:" + nic.getIp() + " mac:" + nic.getMac());
                            }
                        }
                    }
                }
            }

            state = VmPowerState.RUNNING;

            final StartAnswer startAnswer = new StartAnswer(command);

            startAnswer.setIqnToPath(iqnToPath);

            return startAnswer;
        } catch (final Exception e) {
            s_logger.warn("Catch Exception: " + e.getClass().toString() + " due to " + e.toString(), e);
            final String msg = citrixResourceBase.handleVmStartFailure(conn, vmName, vm, "", e);

            final StartAnswer startAnswer = new StartAnswer(command, msg);

            startAnswer.setIqnToPath(iqnToPath);

            return startAnswer;
        } finally {
            if (state != VmPowerState.HALTED) {
                s_logger.debug("2. The VM " + vmName + " is in " + state + " state.");
            } else {
                s_logger.debug("The VM is in stopped state, detected problem during startup : " + vmName);
            }
        }
    }
}