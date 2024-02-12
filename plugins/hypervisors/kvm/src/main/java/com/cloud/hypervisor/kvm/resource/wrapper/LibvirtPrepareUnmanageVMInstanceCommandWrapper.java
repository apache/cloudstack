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

import com.cloud.agent.api.PrepareUnmanageVMInstanceAnswer;
import com.cloud.agent.api.PrepareUnmanageVMInstanceCommand;
import com.cloud.hypervisor.kvm.resource.LibvirtComputingResource;
import com.cloud.resource.CommandWrapper;
import com.cloud.resource.ResourceWrapper;
import org.libvirt.Connect;
import org.libvirt.Domain;

@ResourceWrapper(handles=PrepareUnmanageVMInstanceCommand.class)
public final class LibvirtPrepareUnmanageVMInstanceCommandWrapper extends CommandWrapper<PrepareUnmanageVMInstanceCommand, PrepareUnmanageVMInstanceAnswer, LibvirtComputingResource> {
    @Override
    public PrepareUnmanageVMInstanceAnswer execute(PrepareUnmanageVMInstanceCommand command, LibvirtComputingResource libvirtComputingResource) {
        final String vmName = command.getInstanceName();
        final LibvirtUtilitiesHelper libvirtUtilitiesHelper = libvirtComputingResource.getLibvirtUtilitiesHelper();
        logger.debug(String.format("Verify if KVM instance: [%s] is available before Unmanaging VM.", vmName));
        try {
            final Connect conn = libvirtUtilitiesHelper.getConnectionByVmName(vmName);
            final Domain domain = libvirtComputingResource.getDomain(conn, vmName);
            if (domain == null) {
                logger.error("Prepare Unmanage VMInstanceCommand: vm not found " + vmName);
                new PrepareUnmanageVMInstanceAnswer(command, false, String.format("Cannot find VM with name [%s] in KVM host.", vmName));
            }
        } catch (Exception e){
            logger.error("PrepareUnmanagedInstancesCommand failed due to " + e.getMessage());
            return new PrepareUnmanageVMInstanceAnswer(command, false, "Error: " + e.getMessage());
        }

        return new PrepareUnmanageVMInstanceAnswer(command, true, "OK");
    }
}
