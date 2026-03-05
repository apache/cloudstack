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
 * Command to restore a VM from a saved memory file.
 *
 * <p>This command restores a previously saved VM memory state from a file,
 * effectively resuming the VM from the exact point where it was saved.
 * This is used during VM snapshot revert operations for storage plugins
 * that use file-based memory snapshots (e.g., ONTAP).</p>
 *
 * <p>The workflow is:</p>
 * <ol>
 *   <li>Ensure the VM is stopped/undefined</li>
 *   <li>Restore memory from file using virsh restore or Connect.domainRestore()</li>
 *   <li>VM resumes running from the saved state</li>
 * </ol>
 */
public class RestoreVMFromMemoryFileCommand extends Command {

    private String vmName;
    private String vmUuid;
    private String memoryFilePath;
    private boolean deleteMemoryFileAfterRestore;

    public RestoreVMFromMemoryFileCommand() {
        // Default constructor for serialization
    }

    /**
     * Creates a command to restore VM from a memory file.
     *
     * @param vmName the name of the VM (libvirt domain name)
     * @param vmUuid the UUID of the VM
     * @param memoryFilePath the absolute path to the saved memory file
     * @param deleteMemoryFileAfterRestore if true, delete the memory file after successful restore
     */
    public RestoreVMFromMemoryFileCommand(String vmName, String vmUuid, String memoryFilePath,
                                           boolean deleteMemoryFileAfterRestore) {
        this.vmName = vmName;
        this.vmUuid = vmUuid;
        this.memoryFilePath = memoryFilePath;
        this.deleteMemoryFileAfterRestore = deleteMemoryFileAfterRestore;
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

    public boolean isDeleteMemoryFileAfterRestore() {
        return deleteMemoryFileAfterRestore;
    }

    public void setDeleteMemoryFileAfterRestore(boolean deleteMemoryFileAfterRestore) {
        this.deleteMemoryFileAfterRestore = deleteMemoryFileAfterRestore;
    }

    @Override
    public boolean executeInSequence() {
        return false;
    }
}
