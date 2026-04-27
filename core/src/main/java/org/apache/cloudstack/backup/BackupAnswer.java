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

package org.apache.cloudstack.backup;

import com.cloud.agent.api.Answer;
import com.cloud.agent.api.Command;

import java.util.Map;

public class BackupAnswer extends Answer {
    private Long size;
    private Long virtualSize;
    private Map<String, String> volumes;
    Boolean needsCleanup;
    // Set by the NAS backup provider after a checkpoint/bitmap was created during this backup.
    // The provider persists it in backup_details under NASBackupChainKeys.BITMAP_NAME.
    private String bitmapCreated;
    // Set when an incremental was requested but the agent had to fall back to a full
    // (e.g. VM was stopped). Provider should record this backup as type=full.
    private Boolean incrementalFallback;
    // Set when the agent had to recreate the parent bitmap before this incremental
    // (e.g. CloudStack rebuilt the domain XML on the previous VM start, losing bitmaps).
    // The first incremental after a recreate is larger than usual; subsequent
    // incrementals return to normal size. Informational — recorded in backup_details.
    private String bitmapRecreated;

    public BackupAnswer(final Command command, final boolean success, final String details) {
        super(command, success, details);
    }

    public Long getSize() {
        return size;
    }

    public void setSize(Long size) {
        this.size = size;
    }

    public Long getVirtualSize() {
        return virtualSize;
    }

    public void setVirtualSize(Long virtualSize) {
        this.virtualSize = virtualSize;
    }

    public Map<String, String> getVolumes() {
        return volumes;
    }

    public void setVolumes(Map<String, String> volumes) {
        this.volumes = volumes;
    }

    public Boolean getNeedsCleanup() {
        if (needsCleanup == null) {
            return false;
        }
        return needsCleanup;
    }

    public void setNeedsCleanup(Boolean needsCleanup) {
        this.needsCleanup = needsCleanup;
    }

    public String getBitmapCreated() {
        return bitmapCreated;
    }

    public void setBitmapCreated(String bitmapCreated) {
        this.bitmapCreated = bitmapCreated;
    }

    public Boolean getIncrementalFallback() {
        return incrementalFallback != null && incrementalFallback;
    }

    public void setIncrementalFallback(Boolean incrementalFallback) {
        this.incrementalFallback = incrementalFallback;
    }

    public String getBitmapRecreated() {
        return bitmapRecreated;
    }

    public void setBitmapRecreated(String bitmapRecreated) {
        this.bitmapRecreated = bitmapRecreated;
    }
}
