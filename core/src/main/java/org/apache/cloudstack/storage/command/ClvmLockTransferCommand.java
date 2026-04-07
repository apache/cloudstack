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

import com.cloud.agent.api.Command;

/**
 * Command to transfer CLVM (Clustered LVM) exclusive lock between hosts.
 * This enables lightweight volume migration for CLVM storage pools where volumes
 * reside in the same Volume Group (VG) but need to be accessed from different hosts.
 *
 * <p>Instead of copying volume data (traditional migration), this command simply
 * deactivates the LV on the source host and activates it exclusively on the destination host.
 *
 * <p>This is significantly faster (10-100x) than traditional migration and uses no network bandwidth.
 */
public class ClvmLockTransferCommand extends Command {

    /**
     * Operation to perform on the CLVM volume.
     * Maps to lvchange flags for LVM operations.
     */
    public enum Operation {
        /** Deactivate the volume on this host (-an) */
        DEACTIVATE("-an", "deactivate"),

        /** Activate the volume exclusively on this host (-aey) */
        ACTIVATE_EXCLUSIVE("-aey", "activate exclusively"),

        /** Activate the volume in shared mode on this host (-asy) */
        ACTIVATE_SHARED("-asy", "activate in shared mode"),

        /** Query the current lock state (lvs -o lv_attr,lv_host) */
        QUERY_LOCK_STATE("query", "query lock state");

        private final String lvchangeFlag;
        private final String description;

        Operation(String lvchangeFlag, String description) {
            this.lvchangeFlag = lvchangeFlag;
            this.description = description;
        }

        public String getLvchangeFlag() {
            return lvchangeFlag;
        }

        public String getDescription() {
            return description;
        }
    }

    private String lvPath;
    private Operation operation;
    private String volumeUuid;

    public ClvmLockTransferCommand() {
        // For serialization
    }

    public ClvmLockTransferCommand(Operation operation, String lvPath, String volumeUuid) {
        this.operation = operation;
        this.lvPath = lvPath;
        this.volumeUuid = volumeUuid;
        // Execute in sequence to ensure lock safety
        setWait(30);
    }

    public String getLvPath() {
        return lvPath;
    }

    public Operation getOperation() {
        return operation;
    }

    public String getVolumeUuid() {
        return volumeUuid;
    }

    @Override
    public boolean executeInSequence() {
        return true;
    }
}
