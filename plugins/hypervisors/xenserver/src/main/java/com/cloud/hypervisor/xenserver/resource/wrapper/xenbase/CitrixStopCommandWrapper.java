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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.collections.MapUtils;
import org.apache.log4j.Logger;

import com.cloud.agent.api.Answer;
import com.cloud.agent.api.StopAnswer;
import com.cloud.agent.api.StopCommand;
import com.cloud.agent.api.VgpuTypesInfo;
import com.cloud.agent.api.to.GPUDeviceTO;
import com.cloud.hypervisor.xenserver.resource.CitrixResourceBase;
import com.cloud.resource.CommandWrapper;
import com.cloud.resource.ResourceWrapper;
import com.cloud.utils.StringUtils;
import com.xensource.xenapi.Connection;
import com.xensource.xenapi.Network;
import com.xensource.xenapi.SR;
import com.xensource.xenapi.Types.VmPowerState;
import com.xensource.xenapi.Types.XenAPIException;
import com.xensource.xenapi.VGPU;
import com.xensource.xenapi.VIF;
import com.xensource.xenapi.VM;

@ResourceWrapper(handles =  StopCommand.class)
public final class CitrixStopCommandWrapper extends CommandWrapper<StopCommand, Answer, CitrixResourceBase> {

    private static final Logger s_logger = Logger.getLogger(CitrixStopCommandWrapper.class);

    @Override
    public Answer execute(final StopCommand command, final CitrixResourceBase citrixResourceBase) {
        final String vmName = command.getVmName();
        final Map<String, Boolean> vlanToPersistenceMap = command.getVlanToPersistenceMap();
        String platformstring = null;
        try {
            final Connection conn = citrixResourceBase.getConnection();
            final Set<VM> vms = VM.getByNameLabel(conn, vmName);
            // stop vm which is running on this host or is in halted state
            final Iterator<VM> iter = vms.iterator();
            while (iter.hasNext()) {
                final VM vm = iter.next();
                final VM.Record vmr = vm.getRecord(conn);
                if (vmr.powerState != VmPowerState.RUNNING) {
                    continue;
                }
                if (citrixResourceBase.isRefNull(vmr.residentOn)) {
                    continue;
                }
                if (vmr.residentOn.getUuid(conn).equals(citrixResourceBase.getHost().getUuid())) {
                    continue;
                }
                iter.remove();
            }

            if (vms.size() == 0) {
                return new StopAnswer(command, "VM does not exist", true);
            }
            for (final VM vm : vms) {
                final VM.Record vmr = vm.getRecord(conn);
                platformstring = StringUtils.mapToString(vmr.platform);
                if (vmr.isControlDomain) {
                    final String msg = "Tring to Shutdown control domain";
                    s_logger.warn(msg);
                    return new StopAnswer(command, msg, false);
                }

                if (vmr.powerState == VmPowerState.RUNNING && !citrixResourceBase.isRefNull(vmr.residentOn) && !vmr.residentOn.getUuid(conn).equals(citrixResourceBase.getHost().getUuid())) {
                    final String msg = "Stop Vm " + vmName + " failed due to this vm is not running on this host: " + citrixResourceBase.getHost().getUuid() + " but host:" + vmr.residentOn.getUuid(conn);
                    s_logger.warn(msg);
                    return new StopAnswer(command, msg, platformstring, false);
                }

                if (command.checkBeforeCleanup() && vmr.powerState == VmPowerState.RUNNING) {
                    final String msg = "Vm " + vmName + " is running on host and checkBeforeCleanup flag is set, so bailing out";
                    s_logger.debug(msg);
                    return new StopAnswer(command, msg, false);
                }

                s_logger.debug("9. The VM " + vmName + " is in Stopping state");

                try {
                    if (vmr.powerState == VmPowerState.RUNNING) {
                        /* when stop a vm, set affinity to current xenserver */
                        vm.setAffinity(conn, vm.getResidentOn(conn));

                        if (citrixResourceBase.canBridgeFirewall()) {
                            final String result = citrixResourceBase.callHostPlugin(conn, "vmops", "destroy_network_rules_for_vm", "vmName", command.getVmName());
                            if (result == null || result.isEmpty() || !Boolean.parseBoolean(result)) {
                                s_logger.warn("Failed to remove  network rules for vm " + command.getVmName());
                            } else {
                                s_logger.info("Removed  network rules for vm " + command.getVmName());
                            }
                        }
                        citrixResourceBase.shutdownVM(conn, vm, vmName, command.isForceStop());
                    }
                } catch (final Exception e) {
                    final String msg = "Catch exception " + e.getClass().getName() + " when stop VM:" + command.getVmName() + " due to " + e.toString();
                    s_logger.debug(msg);
                    return new StopAnswer(command, msg, platformstring, false);
                } finally {

                    try {
                        if (vm.getPowerState(conn) == VmPowerState.HALTED) {
                            Set<VGPU> vGPUs = null;
                            // Get updated GPU details
                            try {
                                vGPUs = vm.getVGPUs(conn);
                            } catch (final XenAPIException e2) {
                                s_logger.debug("VM " + vmName + " does not have GPU support.");
                            }
                            if (vGPUs != null && !vGPUs.isEmpty()) {
                                final HashMap<String, HashMap<String, VgpuTypesInfo>> groupDetails = citrixResourceBase.getGPUGroupDetails(conn);
                                command.setGpuDevice(new GPUDeviceTO(null, null, groupDetails));
                            }

                            final Set<VIF> vifs = vm.getVIFs(conn);
                            final List<Network> networks = new ArrayList<Network>();
                            for (final VIF vif : vifs) {
                                networks.add(vif.getNetwork(conn));
                            }
                            vm.destroy(conn);
                            final SR sr = citrixResourceBase.getISOSRbyVmName(conn, command.getVmName());
                            citrixResourceBase.removeSR(conn, sr);
                            // Disable any VLAN networks that aren't used
                            // anymore
                            for (final Network network : networks) {
                                try {
                                    if (network.getNameLabel(conn).startsWith("VLAN")) {
                                        String networkLabel = network.getNameLabel(conn);
                                        citrixResourceBase.disableVlanNetwork(conn, network, shouldDeleteVlan(networkLabel, vlanToPersistenceMap));
                                    }
                                } catch (final Exception e) {
                                    // network might be destroyed by other host
                                }
                            }
                            return new StopAnswer(command, "Stop VM " + vmName + " Succeed", platformstring, true);
                        }
                    } catch (final Exception e) {
                        final String msg = "VM destroy failed in Stop " + vmName + " Command due to " + e.getMessage();
                        s_logger.warn(msg, e);
                    } finally {
                        s_logger.debug("10. The VM " + vmName + " is in Stopped state");
                    }
                }
            }

        } catch (final Exception e) {
            final String msg = "Stop Vm " + vmName + " fail due to " + e.toString();
            s_logger.warn(msg, e);
            return new StopAnswer(command, msg, platformstring, false);
        }
        return new StopAnswer(command, "Stop VM failed", platformstring, false);
    }

    private boolean shouldDeleteVlan(String networkLabel, Map<String, Boolean> vlanToPersistenceMap) {
        String[] networkNameParts = null;
        if (networkLabel.contains("-")) {
            networkNameParts = networkLabel.split("-");
        } else {
            networkNameParts = networkLabel.split("VLAN");
        }
        String networkVlan = networkNameParts.length > 0 ? networkNameParts[networkNameParts.length - 1] : null;
        if (networkVlan != null && MapUtils.isNotEmpty(vlanToPersistenceMap) && vlanToPersistenceMap.containsKey(networkVlan)) {
            return vlanToPersistenceMap.get(networkVlan);
        }
        return true;
    }
}
