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

package com.cloud.hypervisor.kvm.resource.wrapper;

import java.io.File;
import java.net.URISyntaxException;

import com.cloud.agent.resource.virtualnetwork.VRScripts;
import com.cloud.utils.FileUtil;
import org.libvirt.Connect;
import org.libvirt.DomainInfo.DomainState;
import org.libvirt.LibvirtException;

import com.cloud.agent.api.Answer;
import com.cloud.agent.api.StartAnswer;
import com.cloud.agent.api.StartCommand;
import com.cloud.agent.api.to.NicTO;
import com.cloud.agent.api.to.VirtualMachineTO;
import com.cloud.agent.resource.virtualnetwork.VirtualRoutingResource;
import com.cloud.exception.InternalErrorException;
import com.cloud.hypervisor.kvm.resource.LibvirtComputingResource;
import com.cloud.hypervisor.kvm.resource.LibvirtKvmAgentHook;
import com.cloud.hypervisor.kvm.resource.LibvirtVMDef;
import com.cloud.hypervisor.kvm.storage.KVMStoragePoolManager;
import com.cloud.network.Networks.TrafficType;
import com.cloud.resource.CommandWrapper;
import com.cloud.resource.ResourceWrapper;
import com.cloud.vm.UserVmManager;
import com.cloud.vm.VirtualMachine;

@ResourceWrapper(handles =  StartCommand.class)
public final class LibvirtStartCommandWrapper extends CommandWrapper<StartCommand, Answer, LibvirtComputingResource> {


    @Override
    public Answer execute(final StartCommand command, final LibvirtComputingResource libvirtComputingResource) {
        final VirtualMachineTO vmSpec = command.getVirtualMachine();
        vmSpec.setVncAddr(command.getHostIp());
        final String vmName = vmSpec.getName();
        LibvirtVMDef vm = null;

        DomainState  state = DomainState.VIR_DOMAIN_SHUTOFF;
        final KVMStoragePoolManager storagePoolMgr = libvirtComputingResource.getStoragePoolMgr();
        final LibvirtUtilitiesHelper libvirtUtilitiesHelper = libvirtComputingResource.getLibvirtUtilitiesHelper();
        Connect conn = null;
        try {

            vm = libvirtComputingResource.createVMFromSpec(vmSpec);
            conn = libvirtUtilitiesHelper.getConnectionByType(vm.getHvsType());

            final NicTO[] nics = vmSpec.getNics();

            for (final NicTO nic : nics) {
                if (vmSpec.getType() != VirtualMachine.Type.User) {
                    nic.setPxeDisable(true);
                }
            }

            libvirtComputingResource.createVbd(conn, vmSpec, vmName, vm);

            if (!storagePoolMgr.connectPhysicalDisksViaVmSpec(vmSpec)) {
                return new StartAnswer(command, "Failed to connect physical disks to host");
            }

            libvirtComputingResource.createVifs(vmSpec, vm);

            logger.debug("starting " + vmName + ": " + vm.toString());
            String vmInitialSpecification = vm.toString();
            String vmFinalSpecification = performXmlTransformHook(vmInitialSpecification, libvirtComputingResource);
            libvirtComputingResource.startVM(conn, vmName, vmFinalSpecification);
            performAgentStartHook(vmName, libvirtComputingResource);

            libvirtComputingResource.applyDefaultNetworkRules(conn, vmSpec, false);

            // pass cmdline info to system vms
            if (vmSpec.getType() != VirtualMachine.Type.User || (vmSpec.getBootArgs() != null && (vmSpec.getBootArgs().contains(UserVmManager.CKS_NODE) || vmSpec.getBootArgs().contains(UserVmManager.SHAREDFSVM)))) {
                // try to patch and SSH into the systemvm for up to 5 minutes
                for (int count = 0; count < 10; count++) {
                    // wait and try passCmdLine for 30 seconds at most for CLOUDSTACK-2823
                    if (libvirtComputingResource.passCmdLine(vmName, vmSpec.getBootArgs())) {
                        break;
                    }
                }

                if (vmSpec.getType() != VirtualMachine.Type.User) {
                    String controlIp = null;
                    for (final NicTO nic : vmSpec.getNics()) {
                        if (nic.getType() == TrafficType.Control) {
                            controlIp = nic.getIp();
                            break;
                        }
                    }

                    final VirtualRoutingResource virtRouterResource = libvirtComputingResource.getVirtRouterResource();
                    // check if the router is up?
                    for (int count = 0; count < 60; count++) {
                        final boolean result = virtRouterResource.connect(controlIp, 1, 5000);
                        if (result) {
                            break;
                        }
                    }

                    try {
                        File pemFile = new File(LibvirtComputingResource.SSHPRVKEYPATH);
                        FileUtil.scpPatchFiles(controlIp, VRScripts.CONFIG_CACHE_LOCATION, Integer.parseInt(LibvirtComputingResource.DEFAULTDOMRSSHPORT), pemFile, LibvirtComputingResource.systemVmPatchFiles, LibvirtComputingResource.BASEPATH);
                        if (!virtRouterResource.isSystemVMSetup(vmName, controlIp)) {
                            String errMsg = "Failed to patch systemVM";
                            logger.error(errMsg);
                            return new StartAnswer(command, errMsg);
                        }
                    } catch (Exception e) {
                        String errMsg = "Failed to scp files to system VM. Patching of systemVM failed";
                        logger.error(errMsg, e);
                        return new StartAnswer(command, String.format("%s due to: %s", errMsg, e.getMessage()));
                    }
                }
            }

            state = DomainState.VIR_DOMAIN_RUNNING;
            return new StartAnswer(command);
        } catch (final LibvirtException e) {
            logger.warn("LibvirtException ", e);
            if (conn != null) {
                libvirtComputingResource.handleVmStartFailure(conn, vmName, vm);
            }
            return new StartAnswer(command, e.getMessage());
        } catch (final InternalErrorException e) {
            logger.warn("InternalErrorException ", e);
            if (conn != null) {
                libvirtComputingResource.handleVmStartFailure(conn, vmName, vm);
            }
            return new StartAnswer(command, e.getMessage());
        } catch (final URISyntaxException e) {
            logger.warn("URISyntaxException ", e);
            if (conn != null) {
                libvirtComputingResource.handleVmStartFailure(conn, vmName, vm);
            }
            return new StartAnswer(command, e.getMessage());
        } finally {
            if (state != DomainState.VIR_DOMAIN_RUNNING) {
                storagePoolMgr.disconnectPhysicalDisksViaVmSpec(vmSpec);
            }
        }
    }

    private void performAgentStartHook(String vmName, LibvirtComputingResource libvirtComputingResource) {
        try {
            LibvirtKvmAgentHook onStartHook = libvirtComputingResource.getStartHook();
            onStartHook.handle(vmName);
        } catch (Exception e) {
            logger.warn("Exception occurred when handling LibVirt VM onStart hook: {}", e);
        }
    }

    private String performXmlTransformHook(String vmInitialSpecification, final LibvirtComputingResource libvirtComputingResource) {
        String vmFinalSpecification;
        try {
            // if transformer fails, everything must go as it's just skipped.
            LibvirtKvmAgentHook t = libvirtComputingResource.getTransformer();
            vmFinalSpecification = (String) t.handle(vmInitialSpecification);
            if (null == vmFinalSpecification) {
                logger.warn("Libvirt XML transformer returned NULL, will use XML specification unchanged.");
                vmFinalSpecification = vmInitialSpecification;
            }
        } catch(Exception e) {
            logger.warn("Exception occurred when handling LibVirt XML transformer hook: {}", e);
            vmFinalSpecification = vmInitialSpecification;
        }
        return vmFinalSpecification;
    }
}
