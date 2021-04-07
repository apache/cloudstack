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
import com.cloud.hypervisor.kvm.resource.LibvirtVmMemoryDeviceDef;
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

        long newMemory = ByteScaleUtils.bytesToKib(vmSpec.getMaxRam());
        int newVcpus = vmSpec.getCpus();
        String vmDefinition = vmSpec.toString();
        String scalingDetails = String.format("%s memory to [%s KiB] and cpu cores to [%s]", vmDefinition, newMemory, newVcpus);

        try {
            LibvirtUtilitiesHelper libvirtUtilitiesHelper = libvirtComputingResource.getLibvirtUtilitiesHelper();

            conn = libvirtUtilitiesHelper.getConnectionByVmName(vmName);
            Domain dm = conn.domainLookupByName(vmName);

            long currentMemory = LibvirtComputingResource.getDomainMemory(dm);
            long runningVcpus = libvirtComputingResource.countDomainRunningVcpus(dm);
            long memoryToAttach = newMemory - currentMemory;

            logger.debug(String.format("Scaling %s.", scalingDetails));

            if (memoryToAttach > 0) {
                String memoryDevice = new LibvirtVmMemoryDeviceDef(newMemory - LibvirtComputingResource.getDomainMemory(dm)).toString();
                logger.debug(String.format("Attaching memory device [%s] to %s.", memoryDevice, vmDefinition));
                dm.attachDevice(memoryDevice);
            } else {
                logger.info(String.format("Not scaling the memory. To scale the memory of the %s, the new memory [%s] must be higher than the current memory [%s]. The current difference is [%s].", vmDefinition, newMemory, currentMemory, memoryToAttach));
            }

            if (runningVcpus < newVcpus) {
                dm.setVcpus(newVcpus);
            } else {
                logger.info(String.format("Not scaling the cpu cores. To scale the cpu cores of the %s, the new cpu count [%s] must be higher than the current cpu count [%s].", vmDefinition, newVcpus, runningVcpus));
            }

            return new ScaleVmAnswer(command, true, String.format("Successfully scaled %s.", scalingDetails));
        } catch (LibvirtException e) {
            String message = String.format("Unable to scale %s due to [%s].", scalingDetails, e.getMessage());
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