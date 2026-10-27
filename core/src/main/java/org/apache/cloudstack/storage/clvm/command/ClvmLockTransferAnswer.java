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

package org.apache.cloudstack.storage.clvm.command;

import com.cloud.agent.api.Answer;

/**
 * Answer for ClvmLockTransferCommand, containing lock state information.
 * This answer includes the current lock holder information when querying lock state.
 */
public class ClvmLockTransferAnswer extends Answer {

    private String currentLockHostname;
    private boolean isActive;
    private boolean isOpen;
    private String lvAttributes;

    public ClvmLockTransferAnswer(ClvmLockTransferCommand cmd, boolean result, String details) {
        super(cmd, result, details);
    }

    public ClvmLockTransferAnswer(ClvmLockTransferCommand cmd, boolean result, String details,
                                  String currentLockHostname, boolean isActive, boolean isOpen,
                                  String lvAttributes) {
        super(cmd, result, details);
        this.currentLockHostname = currentLockHostname;
        this.isActive = isActive;
        this.isOpen = isOpen;
        this.lvAttributes = lvAttributes;
    }

    /**
     * Get the hostname from lv_host. Retained for diagnostics only —
     * do NOT use this to determine lock holder identity.
     */
    public String getCurrentLockHostname() {
        return currentLockHostname;
    }

    public void setCurrentLockHostname(String currentLockHostname) {
        this.currentLockHostname = currentLockHostname;
    }

    /**
     * Whether the LV is locally active on the queried host (lv_attr[4]=='a').
     * This is the authoritative signal for lock holder discovery via fan-out.
     */
    public boolean isActive() {
        return isActive;
    }

    public void setActive(boolean active) {
        isActive = active;
    }

    /**
     * Whether a process has the device file open on the queried host (lv_attr[5]=='o').
     * true means a VM is actively doing I/O on this host right now — do NOT deactivate.
     */
    public boolean isOpen() {
        return isOpen;
    }

    public void setOpen(boolean open) {
        isOpen = open;
    }

    public String getLvAttributes() {
        return lvAttributes;
    }

    public void setLvAttributes(String lvAttributes) {
        this.lvAttributes = lvAttributes;
    }
}
