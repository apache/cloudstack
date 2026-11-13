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
package org.apache.cloudstack.utils.rbd;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;

import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.cloud.utils.script.Script;

/**
 * Looks after the guests which the RBD method of nasbackup.sh freezes or pauses while it takes its
 * snapshots. The script records that in a state file per VM. If the script is killed, or the agent which
 * ran it restarts, nothing else undoes it: SIGKILL runs no trap. This releases the guests the state
 * files say are still held.
 */
public class RbdBackupGuestManager {
    protected static final Logger LOGGER = LogManager.getLogger(RbdBackupGuestManager.class);

    // Keep in step with RBD_STATE_DIR of nasbackup.sh.
    public static final String STATE_DIRECTORY = "/run/csbackup";
    public static final String STATE_FILE_SUFFIX = ".guest";

    private final File stateDirectory;

    public RbdBackupGuestManager() {
        this(new File(STATE_DIRECTORY));
    }

    public RbdBackupGuestManager(File stateDirectory) {
        this.stateDirectory = stateDirectory;
    }

    /**
     * The file nasbackup.sh keeps while its RBD method holds the guest frozen or paused.
     */
    public File getStateFile(String vmName) {
        return new File(stateDirectory, vmName + STATE_FILE_SUFFIX);
    }

    /**
     * Releases the guest of every state file left in the directory, which the agent does when it
     * starts: the scripts of the agent which went down are gone with it.
     */
    public void releaseAllGuests() {
        File[] stateFiles = stateDirectory.listFiles((dir, name) -> name.endsWith(STATE_FILE_SUFFIX));
        if (stateFiles == null) {
            return;
        }
        for (File stateFile : stateFiles) {
            String name = stateFile.getName();
            String vmName = name.substring(0, name.length() - STATE_FILE_SUFFIX.length());
            if (!vmName.isEmpty()) {
                releaseGuest(vmName);
            }
        }
    }

    /**
     * Releases the guest, but only if the state file says the script held it, so a VM which an
     * operator paused is left alone. The state file is removed once the guest is released.
     */
    public void releaseGuest(String vmName) {
        File stateFile = getStateFile(vmName);
        if (!stateFile.isFile()) {
            return;
        }
        String state;
        try {
            state = new String(Files.readAllBytes(stateFile.toPath()), StandardCharsets.UTF_8);
        } catch (IOException e) {
            LOGGER.warn("Unable to read the state file [{}] of the backup of VM {}", stateFile, vmName, e);
            return;
        }
        LOGGER.warn("The RBD backup of VM {} ended while holding the guest ({}), releasing it", vmName, state.trim().replace('\n', ' '));
        String virsh = Script.getExecutableAbsolutePath("virsh");
        boolean released = true;
        if (state.contains("suspended")) {
            String domState = Script.executeCommand(virsh, "-c", "qemu:///system", "domstate", vmName);
            if (domState != null && StringUtils.equals(domState.trim(), "paused")
                    && Script.executeCommandForExitValue(virsh, "-c", "qemu:///system", "resume", vmName) != 0) {
                LOGGER.warn("Failed to resume VM {} after its backup", vmName);
                released = false;
            }
        }
        if (state.contains("frozen")) {
            // Thawing a guest which is not frozen only makes the agent complain.
            if (Script.executeCommandForExitValue(virsh, "-c", "qemu:///system", "qemu-agent-command", vmName,
                    "{\"execute\":\"guest-fsfreeze-thaw\"}") != 0) {
                LOGGER.warn("Failed to thaw the filesystems of VM {} after its backup", vmName);
            }
        }
        if (released && !stateFile.delete()) {
            LOGGER.warn("Failed to delete the state file [{}]", stateFile);
        }
    }
}
