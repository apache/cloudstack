/*
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
import com.cloud.utils.exception.CloudRuntimeException;
import org.apache.cloudstack.utils.bytescale.ByteScaleUtils;
import org.libvirt.Connect;
import org.libvirt.Domain;
import org.libvirt.LibvirtException;

@ResourceWrapper(handles = ScaleVmCommand.class)
public class LibvirtScaleVmCommandWrapper extends CommandWrapper<ScaleVmCommand, Answer, LibvirtComputingResource> {

    @Override
    public Answer execute(ScaleVmCommand command, LibvirtComputingResource libvirtComputingResource) {
        VirtualMachineTO vmSpec = command.getVirtualMachine();
        String vmName = vmSpec.getName();
        Connect conn = null;

        long newMemory = ByteScaleUtils.bytesToKibibytes(vmSpec.getMaxRam());
        int newVcpus = vmSpec.getCpus();
        int newCpuShares = libvirtComputingResource.calculateCpuShares(vmSpec);
        String vmDefinition = vmSpec.toString();
        String scalingDetails = String.format("%s memory to [%s KiB], CPU cores to [%s] and cpu_shares to [%s]", vmDefinition, newMemory, newVcpus, newCpuShares);

        try {
            LibvirtUtilitiesHelper libvirtUtilitiesHelper = libvirtComputingResource.getLibvirtUtilitiesHelper();

            conn = libvirtUtilitiesHelper.getConnectionByVmName(vmName);
            Domain dm = conn.domainLookupByName(vmName);

            logger.debug(String.format("Scaling %s.", scalingDetails));
            scaleMemory(dm, newMemory, vmDefinition);
            scaleVcpus(dm, newVcpus, vmDefinition);
            updateCpuShares(dm, newCpuShares);

            return new ScaleVmAnswer(command, true, String.format("Successfully scaled %s.", scalingDetails));
        } catch (LibvirtException | CloudRuntimeException e) {
            String message = String.format("Unable to scale %s due to [%s].", scalingDetails, e.getMessage());
            logger.error(message, e);
            return new ScaleVmAnswer(command, false, message);
        }
    }

    /**
     * Sets the cpu_shares (priority) of the running VM. This is necessary because the priority is only calculated when deploying the VM.
     * To prevent the cpu_shares to be manually updated by using the command virsh schedinfo or restarting the VM. This method updates the cpu_shares of a running VM on the fly.
     * @param dm domain of the VM.
     * @param newCpuShares new priority of the running VM.
     * @throws org.libvirt.LibvirtException
     **/
    protected void updateCpuShares(Domain dm, int newCpuShares) throws LibvirtException {
        int oldCpuShares = LibvirtComputingResource.getCpuShares(dm);

        if (oldCpuShares < newCpuShares) {
            LibvirtComputingResource.setCpuShares(dm, newCpuShares);
            logger.info(String.format("Successfully increased cpu_shares of VM [%s] from [%s] to [%s].", dm.getName(), oldCpuShares, newCpuShares));
        }
    }

    protected void scaleVcpus(Domain dm, int newVcpus, String vmDefinition) throws LibvirtException {
        long runningVcpus = LibvirtComputingResource.countDomainRunningVcpus(dm);

        if (runningVcpus < newVcpus) {
            dm.setVcpus(newVcpus);
            return;
        }

        logger.info(String.format("Not scaling the CPU cores. To scale the CPU cores of the %s, the new CPU count [%s] must be higher than the current CPU count [%s].",
            vmDefinition, newVcpus, runningVcpus));
    }

    protected void scaleMemory(Domain dm, long newMemory, String vmDefinition) throws LibvirtException, CloudRuntimeException {
        long currentMemory = LibvirtComputingResource.getDomainMemory(dm);
        long memoryToAttach = newMemory - currentMemory;

        if (memoryToAttach <= 0) {
            logger.info(String.format("Not scaling the memory. To scale the memory of the %s, the new memory [%s] must be higher than the current memory [%s]. The current "
              + "difference is [%s].", vmDefinition, newMemory, currentMemory, memoryToAttach));
            return;
        }

        if (!dm.getXMLDesc(0).contains("<maxMemory slots='16' unit='KiB'>")) {
            throw new CloudRuntimeException(String.format("The %s is not prepared for dynamic scaling. To be prepared, the VM must be deployed with a dynamic service offering,"
              + " VM dynamic scale enabled and global setting \"enable.dynamic.scale.vm\" as \"true\". If you changed one of these settings after deploying the VM,"
              + " consider stopping and starting it again to prepared it to dynamic scaling.", vmDefinition));
        }

        String memoryDevice = new LibvirtVmMemoryDeviceDef(memoryToAttach).toString();
        logger.debug(String.format("Attaching memory device [%s] to %s.", memoryDevice, vmDefinition));
        dm.attachDevice(memoryDevice);
    }
}
