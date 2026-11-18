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
package com.cloud.vm;

import org.apache.cloudstack.backup.Backup;

public class VmWorkRestoreVolumeBackupAndAttach extends VmWork {

    private long backupId;

    private Backup.VolumeInfo backupVolumeInfo;

    private String hostIp;

    private boolean quickRestore;

    public VmWorkRestoreVolumeBackupAndAttach(long userId, long accountId, long vmId, String handlerName, long backupId, Backup.VolumeInfo backupVolumeInfo,
            String hostIp, boolean quickRestore) {
        super(userId, accountId, vmId, handlerName);
        this.backupId = backupId;
        this.backupVolumeInfo = backupVolumeInfo;
        this.hostIp = hostIp;
        this.quickRestore = quickRestore;
    }

    public long getBackupId() {
        return backupId;
    }

    public Backup.VolumeInfo getBackupVolumeInfo() {
        return backupVolumeInfo;
    }

    public String getHostIp() {
        return hostIp;
    }

    public boolean isQuickRestore() {
        return quickRestore;
    }
}
