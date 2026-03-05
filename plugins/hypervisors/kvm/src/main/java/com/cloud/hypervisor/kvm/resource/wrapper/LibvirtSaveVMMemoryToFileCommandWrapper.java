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
import com.cloud.agent.api.SaveVMMemoryToFileAnswer;
import com.cloud.agent.api.SaveVMMemoryToFileCommand;
import com.cloud.hypervisor.kvm.resource.LibvirtComputingResource;
import com.cloud.resource.CommandWrapper;
import com.cloud.resource.ResourceWrapper;
import com.cloud.utils.script.Script;

/**
 * KVM wrapper for {@link SaveVMMemoryToFileCommand}.
 *
 * <p>This wrapper saves the VM's memory state to a file on shared storage using
 * libvirt's domain save functionality. The approach is:</p>
 *
 * <ol>
 *   <li>Pause the VM to ensure consistent memory state</li>
 *   <li>Use virsh save to dump memory to the specified file</li>
 *   <li>Resume the VM (if requested) so it continues running</li>
 * </ol>
 *
 * <p>Note: Unlike libvirt's managed-save, we want the VM to continue running
 * after memory is saved. This is achieved by using --bypass-cache and --live
 * flags with virsh save, followed by virsh restore to bring the VM back.</p>
 *
 * <p>Alternative approach: Use virsh dump with --memory-only for a live memory
 * snapshot that doesn't stop the VM. However, this requires QEMU 2.1+ and
 * creates a core dump format that needs special handling.</p>
 */
@ResourceWrapper(handles = SaveVMMemoryToFileCommand.class)
public class LibvirtSaveVMMemoryToFileCommandWrapper
        extends CommandWrapper<SaveVMMemoryToFileCommand, Answer, LibvirtComputingResource> {

    @Override
    public Answer execute(SaveVMMemoryToFileCommand cmd, LibvirtComputingResource serverResource) {
        String vmName = cmd.getVmName();
        String memoryFilePath = cmd.getMemoryFilePath();
        Domain domain = null;

        try {
            final LibvirtUtilitiesHelper libvirtUtilitiesHelper = serverResource.getLibvirtUtilitiesHelper();
            Connect conn = libvirtUtilitiesHelper.getConnection();
            domain = serverResource.getDomain(conn, vmName);

            if (domain == null) {
                return new SaveVMMemoryToFileAnswer(cmd, false,
                        "Failed to save VM memory: VM " + vmName + " not found");
            }

            DomainState domainState = domain.getInfo().state;
            if (domainState != DomainState.VIR_DOMAIN_RUNNING && domainState != DomainState.VIR_DOMAIN_PAUSED) {
                return new SaveVMMemoryToFileAnswer(cmd, false,
                        "Cannot save memory for VM " + vmName + " in state " + domainState +
                        ". VM must be Running or Paused.");
            }

            // Ensure parent directory exists
            File memoryFile = new File(memoryFilePath);
            File parentDir = memoryFile.getParentFile();
            if (parentDir != null && !parentDir.exists()) {
                if (!parentDir.mkdirs()) {
                    return new SaveVMMemoryToFileAnswer(cmd, false,
                            "Failed to create directory for memory file: " + parentDir.getAbsolutePath());
                }
            }

            // Save VM memory using virsh save with --running flag
            // --running: When restored, the VM will resume running (not stay paused)
            // --bypass-cache: Avoid caching for large memory files
            // Use Script class for better error handling - it returns null on success
            Script saveScript = new Script(Script.getExecutableAbsolutePath("virsh"),
                    serverResource.getCmdsTimeout(), logger);
            saveScript.add("save");
            saveScript.add(vmName);
            saveScript.add(memoryFilePath);
            saveScript.add("--running");
            saveScript.add("--bypass-cache");

            String saveResult = saveScript.execute();
            // Script.execute() returns null on success, error message on failure
            if (saveResult != null) {
                logger.error("virsh save failed for VM " + vmName + ": " + saveResult);
                // Clean up partial file if it exists
                if (memoryFile.exists()) {
                    memoryFile.delete();
                }
                return new SaveVMMemoryToFileAnswer(cmd, false,
                        "Failed to save VM memory: " + saveResult);
            }

            logger.info("SaveVMMemoryToFileCommandWrapper: virsh save completed successfully for VM " + vmName);

            // Verify the memory file was created
            if (!memoryFile.exists()) {
                return new SaveVMMemoryToFileAnswer(cmd, false,
                        "Memory file was not created at: " + memoryFilePath);
            }

            long fileSize = memoryFile.length();
            logger.info("SaveVMMemoryToFileCommandWrapper: Successfully saved memory for VM " + vmName +
                    " to " + memoryFilePath + " (size: " + fileSize + " bytes)");

            // After virsh save, the VM is stopped. If resumeAfterSave is true,
            // we need to restore the VM immediately so it continues running.
            if (cmd.isResumeAfterSave()) {
                Script restoreScript = new Script(Script.getExecutableAbsolutePath("virsh"),
                        serverResource.getCmdsTimeout(), logger);
                restoreScript.add("restore");
                restoreScript.add(memoryFilePath);
                restoreScript.add("--bypass-cache");

                String restoreResult = restoreScript.execute();
                // Script.execute() returns null on success, error message on failure
                if (restoreResult != null) {
                    logger.error("virsh restore failed for VM " + vmName + ": " + restoreResult);
                    return new SaveVMMemoryToFileAnswer(cmd, false,
                            "Memory saved but failed to restore VM: " + restoreResult);
                }

                logger.info("SaveVMMemoryToFileCommandWrapper: VM " + vmName +
                        " restored and running after memory save");
            }

            return new SaveVMMemoryToFileAnswer(cmd, true,
                    "Successfully saved VM memory to file", memoryFilePath, fileSize);

        } catch (LibvirtException e) {
            logger.error("SaveVMMemoryToFileCommandWrapper: LibvirtException for VM " + vmName, e);
            // Clean up partial file
            File memoryFile = new File(memoryFilePath);
            if (memoryFile.exists()) {
                memoryFile.delete();
            }
            return new SaveVMMemoryToFileAnswer(cmd, false,
                    "Failed to save VM memory due to libvirt error: " + e.getMessage());
        } finally {
            if (domain != null) {
                try {
                    domain.free();
                } catch (LibvirtException e) {
                    logger.trace("Ignoring libvirt error while freeing domain", e);
                }
            }
        }
    }
}
