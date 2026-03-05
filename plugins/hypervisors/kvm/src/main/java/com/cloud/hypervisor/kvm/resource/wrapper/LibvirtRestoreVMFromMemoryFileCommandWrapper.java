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

import org.libvirt.Connect;
import org.libvirt.Domain;
import org.libvirt.DomainInfo.DomainState;
import org.libvirt.LibvirtException;

import com.cloud.agent.api.Answer;
import com.cloud.agent.api.RestoreVMFromMemoryFileAnswer;
import com.cloud.agent.api.RestoreVMFromMemoryFileCommand;
import com.cloud.hypervisor.kvm.resource.LibvirtComputingResource;
import com.cloud.resource.CommandWrapper;
import com.cloud.resource.ResourceWrapper;
import com.cloud.utils.script.Script;
import com.cloud.vm.VirtualMachine;

/**
 * KVM wrapper for {@link RestoreVMFromMemoryFileCommand}.
 *
 * <p>This wrapper restores a VM from a previously saved memory file using
 * libvirt's domain restore functionality. The VM resumes running from the
 * exact point where memory was saved.</p>
 *
 * <p>This is used during VM snapshot revert operations for storage plugins
 * that use file-based memory snapshots (e.g., ONTAP). After the storage
 * volumes are reverted to the snapshot point, this command restores the
 * VM's memory state so the VM continues from where it was.</p>
 */
@ResourceWrapper(handles = RestoreVMFromMemoryFileCommand.class)
public class LibvirtRestoreVMFromMemoryFileCommandWrapper
        extends CommandWrapper<RestoreVMFromMemoryFileCommand, Answer, LibvirtComputingResource> {

    @Override
    public Answer execute(RestoreVMFromMemoryFileCommand cmd, LibvirtComputingResource serverResource) {
        String vmName = cmd.getVmName();
        String memoryFilePath = cmd.getMemoryFilePath();

        try {
            final LibvirtUtilitiesHelper libvirtUtilitiesHelper = serverResource.getLibvirtUtilitiesHelper();
            Connect conn = libvirtUtilitiesHelper.getConnection();

            // Verify memory file exists
            File memoryFile = new File(memoryFilePath);
            if (!memoryFile.exists()) {
                return new RestoreVMFromMemoryFileAnswer(cmd, false,
                        "Memory file not found: " + memoryFilePath);
            }

            // Check if VM is currently running - if so, it needs to be stopped first
            Domain existingDomain = null;
            try {
                existingDomain = serverResource.getDomain(conn, vmName);
                if (existingDomain != null) {
                    DomainState state = existingDomain.getInfo().state;
                    if (state == DomainState.VIR_DOMAIN_RUNNING ||
                        state == DomainState.VIR_DOMAIN_PAUSED) {
                        logger.info("RestoreVMFromMemoryFileCommandWrapper: VM " + vmName +
                                " is in state " + state + ", destroying before restore");
                        existingDomain.destroy();
                    }
                    // Undefine the domain if it's persistent (will be recreated by restore)
                    if (existingDomain.isPersistent() == 1) {
                        existingDomain.undefine();
                    }
                }
            } catch (LibvirtException e) {
                // Domain might not exist, which is fine
                logger.debug("Domain " + vmName + " not found or error checking state: " + e.getMessage());
            } finally {
                if (existingDomain != null) {
                    try {
                        existingDomain.free();
                    } catch (LibvirtException e) {
                        logger.trace("Ignoring error freeing existing domain", e);
                    }
                }
            }

            // Restore VM from memory file using virsh restore
            String virshPath = Script.getExecutableAbsolutePath("virsh");
            String restoreResult = Script.runSimpleBashScript(
                    virshPath + " restore " + memoryFilePath + " --bypass-cache");

            if (restoreResult != null && !restoreResult.isEmpty()) {
                logger.error("virsh restore failed for VM " + vmName + ": " + restoreResult);
                return new RestoreVMFromMemoryFileAnswer(cmd, false,
                        "Failed to restore VM from memory file: " + restoreResult);
            }

            // Verify VM is now running
            Domain restoredDomain = null;
            VirtualMachine.PowerState vmState = VirtualMachine.PowerState.PowerUnknown;
            try {
                restoredDomain = serverResource.getDomain(conn, vmName);
                if (restoredDomain != null) {
                    DomainState state = restoredDomain.getInfo().state;
                    if (state == DomainState.VIR_DOMAIN_RUNNING) {
                        vmState = VirtualMachine.PowerState.PowerOn;
                    } else if (state == DomainState.VIR_DOMAIN_PAUSED) {
                        // Resume the VM if it's paused after restore
                        restoredDomain.resume();
                        vmState = VirtualMachine.PowerState.PowerOn;
                    } else {
                        vmState = VirtualMachine.PowerState.PowerOff;
                    }
                }
            } finally {
                if (restoredDomain != null) {
                    try {
                        restoredDomain.free();
                    } catch (LibvirtException e) {
                        logger.trace("Ignoring error freeing restored domain", e);
                    }
                }
            }

            // Delete memory file if requested
            if (cmd.isDeleteMemoryFileAfterRestore() && memoryFile.exists()) {
                if (!memoryFile.delete()) {
                    logger.warn("RestoreVMFromMemoryFileCommandWrapper: Failed to delete memory file: " +
                            memoryFilePath);
                } else {
                    logger.info("RestoreVMFromMemoryFileCommandWrapper: Deleted memory file: " + memoryFilePath);
                }
            }

            logger.info("RestoreVMFromMemoryFileCommandWrapper: Successfully restored VM " + vmName +
                    " from " + memoryFilePath + ", VM state: " + vmState);

            return new RestoreVMFromMemoryFileAnswer(cmd, true,
                    "Successfully restored VM from memory file", vmState);

        } catch (LibvirtException e) {
            logger.error("RestoreVMFromMemoryFileCommandWrapper: LibvirtException for VM " + vmName, e);
            return new RestoreVMFromMemoryFileAnswer(cmd, false,
                    "Failed to restore VM from memory file due to libvirt error: " + e.getMessage());
        }
    }
}
