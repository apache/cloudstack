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

package com.cloud.agent.api;

/**
 * Command to save VM memory state to a file on shared storage.
 *
 * <p>This command pauses the VM, saves its memory state to the specified file path,
 * and then resumes the VM. This is used by storage plugins (e.g., ONTAP) that need
 * to capture memory state as a file that can be included in a storage-level snapshot,
 * rather than using libvirt's internal qcow2 snapshot mechanism.</p>
 *
 * <p>The workflow is:</p>
 * <ol>
 *   <li>Pause the VM (to ensure consistent memory state)</li>
 *   <li>Save memory to file using virsh save or Domain.save()</li>
 *   <li>Resume the VM (so the VM continues running after snapshot)</li>
 * </ol>
 *
 * <p>Note: This is different from libvirt's managed-save which stops the VM.
 * We want the VM to continue running after the memory is saved.</p>
 */
public class SaveVMMemoryToFileCommand extends Command {

    private String vmName;
    private String vmUuid;
    private String memoryFilePath;
    private boolean resumeAfterSave;

    public SaveVMMemoryToFileCommand() {
        // Default constructor for serialization
    }

    /**
     * Creates a command to save VM memory to a file.
     *
     * @param vmName the name of the VM (libvirt domain name)
     * @param vmUuid the UUID of the VM
     * @param memoryFilePath the absolute path where memory should be saved
     *                       (must be on shared/accessible storage)
     * @param resumeAfterSave if true, resume the VM after saving memory;
     *                        if false, leave the VM paused (caller will resume later)
     */
    public SaveVMMemoryToFileCommand(String vmName, String vmUuid, String memoryFilePath, boolean resumeAfterSave) {
        this.vmName = vmName;
        this.vmUuid = vmUuid;
        this.memoryFilePath = memoryFilePath;
        this.resumeAfterSave = resumeAfterSave;
    }

    public String getVmName() {
        return vmName;
    }

    public void setVmName(String vmName) {
        this.vmName = vmName;
    }

    public String getVmUuid() {
        return vmUuid;
    }

    public void setVmUuid(String vmUuid) {
        this.vmUuid = vmUuid;
    }

    public String getMemoryFilePath() {
        return memoryFilePath;
    }

    public void setMemoryFilePath(String memoryFilePath) {
        this.memoryFilePath = memoryFilePath;
    }

    public boolean isResumeAfterSave() {
        return resumeAfterSave;
    }

    public void setResumeAfterSave(boolean resumeAfterSave) {
        this.resumeAfterSave = resumeAfterSave;
    }

    @Override
    public boolean executeInSequence() {
        return false;
    }
}
