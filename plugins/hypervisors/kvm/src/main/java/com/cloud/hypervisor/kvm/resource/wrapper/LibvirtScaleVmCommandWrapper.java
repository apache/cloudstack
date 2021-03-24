/*
 * Copyright 2021 The Apache Software Foundation.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.cloud.hypervisor.kvm.resource.wrapper;

import com.cloud.agent.api.Answer;
import com.cloud.agent.api.ScaleVmAnswer;
import com.cloud.agent.api.ScaleVmCommand;
import com.cloud.agent.api.to.VirtualMachineTO;
import com.cloud.hypervisor.kvm.resource.LibvirtComputingResource;
import com.cloud.resource.CommandWrapper;
import com.cloud.resource.ResourceWrapper;
import org.apache.cloudstack.utils.bytescale.ByteScaleUtils;
import org.libvirt.Connect;
import org.libvirt.Domain;
import org.libvirt.LibvirtException;

@ResourceWrapper(handles = ScaleVmCommand.class)
public final class LibvirtScaleVmCommandWrapper extends CommandWrapper<ScaleVmCommand, Answer, LibvirtComputingResource> {

    @Override
    public Answer execute(ScaleVmCommand command, LibvirtComputingResource libvirtComputingResource) {
        VirtualMachineTO vmSpec = command.getVirtualMachine();
        String vmName = vmSpec.getName();
        Connect conn = null;

        long memory = ByteScaleUtils.bytesToKib(vmSpec.getMaxRam());
        int vcpus = vmSpec.getCpus();
        String scallingDetails = String.format("%s memory to [%s KiB] and cpu cores to [%s]", vmSpec.toString(), memory, vcpus);

        try {
            LibvirtUtilitiesHelper libvirtUtilitiesHelper = libvirtComputingResource.getLibvirtUtilitiesHelper();

            conn = libvirtUtilitiesHelper.getConnectionByVmName(vmName);
            Domain dm = conn.domainLookupByName(vmName);

            logger.debug(String.format("Scalling %s.", scallingDetails));
            dm.setMemory(memory);
            dm.setVcpus(vcpus);

            return new ScaleVmAnswer(command, true, String.format("Successfully scalled %s.", scallingDetails));
        } catch (LibvirtException e) {
            String message = String.format("Unable to scale %s due to [%s].", scallingDetails, e.getMessage());
            logger.warn(message, e);
            return new ScaleVmAnswer(command, false, message);
        } finally {
            if (conn != null) {
                try {
                    conn.close();
                } catch (LibvirtException ex) {
                    logger.warn(String.format("Error trying to close libvirt connection [%s]", ex.getMessage()), ex);
                }
            }
        }
    }

}