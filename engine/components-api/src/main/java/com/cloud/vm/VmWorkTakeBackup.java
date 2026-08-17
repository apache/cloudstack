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

public class VmWorkTakeBackup extends VmWork {

    private long backupId;

    private boolean quiesceVm;

    private boolean isolated;

    public VmWorkTakeBackup(long userId, long accountId, long vmId, long backupId, String handlerName, boolean quiesceVm, boolean isolated) {
        super(userId, accountId, vmId, handlerName);
        this.quiesceVm = quiesceVm;
        this.backupId = backupId;
        this.isolated = isolated;
    }

    public boolean isQuiesceVm() {
        return quiesceVm;
    }

    public long getBackupId() {
        return backupId;
    }

    public boolean isIsolated() {
        return isolated;
    }

    @Override
    public String toString() {
        return super.toStringAfterRemoveParams(null, null);
    }
}
