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

import org.libvirt.Connect;
import org.libvirt.LibvirtException;

import com.cloud.agent.api.Answer;
import com.cloud.agent.api.UnmanageInstanceAnswer;
import com.cloud.agent.api.UnmanageInstanceCommand;
import com.cloud.agent.api.to.NicTO;
import com.cloud.agent.api.to.VirtualMachineTO;
import com.cloud.hypervisor.kvm.resource.LibvirtComputingResource;
import com.cloud.hypervisor.kvm.resource.LibvirtKvmAgentHook;
import com.cloud.hypervisor.kvm.resource.LibvirtVMDef;
import com.cloud.resource.CommandWrapper;
import com.cloud.resource.ResourceWrapper;
import com.cloud.vm.VirtualMachine;

@ResourceWrapper(handles =  UnmanageInstanceCommand.class)
public final class LibvirtUnmanageInstanceCommandWrapper extends CommandWrapper<UnmanageInstanceCommand, Answer, LibvirtComputingResource> {


    @Override
    public Answer execute(final UnmanageInstanceCommand command, final LibvirtComputingResource libvirtComputingResource) {
        final VirtualMachineTO vmSpec = command.getVirtualMachine();
        LibvirtVMDef vm = null;
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

            String vmInitialSpecification = vm.toString();
            String vmFinalSpecification = performXmlTransformHook(vmInitialSpecification, libvirtComputingResource);
            libvirtComputingResource.defineVMDomain(conn, vmFinalSpecification);

            return new UnmanageInstanceAnswer(command, true, "Successfully unmanaged");
        } catch (final LibvirtException ex) {
            logger.warn("LibvirtException ", ex);
            return new UnmanageInstanceAnswer(command, false, ex.getMessage());
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
