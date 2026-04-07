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

package org.apache.cloudstack.storage.command;

import com.cloud.agent.api.Answer;

/**
 * Answer for ClvmLockTransferCommand, containing lock state information.
 * This answer includes the current lock holder information when querying lock state.
 */
public class ClvmLockTransferAnswer extends Answer {

    private String currentLockHostname;
    private boolean isActive;
    private boolean isExclusive;
    private String lvAttributes;

    public ClvmLockTransferAnswer(ClvmLockTransferCommand cmd, boolean result, String details) {
        super(cmd, result, details);
    }

    public ClvmLockTransferAnswer(ClvmLockTransferCommand cmd, boolean result, String details,
                                  String currentLockHostname, boolean isActive, boolean isExclusive,
                                  String lvAttributes) {
        super(cmd, result, details);
        this.currentLockHostname = currentLockHostname;
        this.isActive = isActive;
        this.isExclusive = isExclusive;
        this.lvAttributes = lvAttributes;
    }

    /**
     * Get the hostname of the host currently holding the lock (if any).
     * This is parsed from the LVM "lv_host" field.
     *
     * @return hostname or null if no lock is held
     */
    public String getCurrentLockHostname() {
        return currentLockHostname;
    }

    public void setCurrentLockHostname(String currentLockHostname) {
        this.currentLockHostname = currentLockHostname;
    }

    /**
     * Whether the volume is currently active on any host.
     *
     * @return true if active, false otherwise
     */
    public boolean isActive() {
        return isActive;
    }

    public void setActive(boolean active) {
        isActive = active;
    }

    /**
     * Whether the lock is exclusive (as opposed to shared).
     * Only meaningful if isActive() is true.
     *
     * @return true if exclusive lock, false if shared
     */
    public boolean isExclusive() {
        return isExclusive;
    }

    public void setExclusive(boolean exclusive) {
        isExclusive = exclusive;
    }

    public String getLvAttributes() {
        return lvAttributes;
    }

    public void setLvAttributes(String lvAttributes) {
        this.lvAttributes = lvAttributes;
    }
}
