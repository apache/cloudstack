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
package com.cloud.storage;

import java.util.List;

import com.cloud.user.Account;

public class CreateSnapshotPayload {
    private Long snapshotPolicyId;
    private Long snapshotId;
    private Account account;
    private boolean quiescevm;
    private Snapshot.LocationType locationType;
    private boolean asyncBackup;
    private List<Long> zoneIds;

    public Long getSnapshotPolicyId() {
        return snapshotPolicyId;
    }

    public void setSnapshotPolicyId(Long snapshotPolicyId) {
        this.snapshotPolicyId = snapshotPolicyId;
    }

    public Long getSnapshotId() {
        return snapshotId;
    }

    public void setSnapshotId(Long snapshotId) {
        this.snapshotId = snapshotId;
    }

    public Account getAccount() {
        return account;
    }

    public void setAccount(Account account) {
        this.account = account;
    }

    public void setQuiescevm(boolean quiescevm) { this.quiescevm = quiescevm; }

    public boolean getQuiescevm() {
        return this.quiescevm;
    }

    public Snapshot.LocationType getLocationType() { return this.locationType; }

    public void setLocationType(Snapshot.LocationType locationType) { this.locationType = locationType; }

    public void setAsyncBackup(boolean asyncBackup) {
        this.asyncBackup = asyncBackup;
    }

    public boolean getAsyncBackup() {
        return this.asyncBackup;
    }

    public List<Long> getZoneIds() {
        return zoneIds;
    }

    public void setZoneIds(List<Long> zoneIds) {
        this.zoneIds = zoneIds;
    }
}
