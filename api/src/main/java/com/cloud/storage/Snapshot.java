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

import com.cloud.hypervisor.Hypervisor.HypervisorType;
import com.cloud.utils.fsm.StateObject;
import org.apache.cloudstack.acl.ControlledEntity;
import org.apache.cloudstack.api.Identity;
import org.apache.cloudstack.api.InternalIdentity;

import java.util.Date;

public interface Snapshot extends ControlledEntity, Identity, InternalIdentity, StateObject<Snapshot.State> {
    public enum Type {
        MANUAL, RECURRING, TEMPLATE, HOURLY, DAILY, WEEKLY, MONTHLY, GROUP, INTERNAL;
        // New types should be defined after INTERNAL, and change the max value
        private int max = 8;

        public void setMax(int max) {
            this.max = max;
        }

        public int getMax() {
            return max;
        }

        @Override
        public String toString() {
            return this.name();
        }

        public boolean equals(String snapshotType) {
            return this.toString().equalsIgnoreCase(snapshotType);
        }
    }

    public enum State {
        Allocated, Creating, CreatedOnPrimary, BackingUp, BackedUp, Copying, Destroying, Destroyed,
        //it's a state, user can't see the snapshot from ui, while the snapshot may still exist on the storage
        Error;

        @Override
        public String toString() {
            return this.name();
        }

        public boolean equals(String status) {
            return this.toString().equalsIgnoreCase(status);
        }
    }

    enum Event {
        CreateRequested, OperationNotPerformed, BackupToSecondary, BackedupToSecondary, DestroyRequested, CopyingRequested, OperationSucceeded, OperationFailed
    }

    enum LocationType {
        PRIMARY, SECONDARY
    }

    public static final long MANUAL_POLICY_ID = 0L;
    public static final long INTERNAL_POLICY_ID = 8L;

    @Override
    long getAccountId();

    long getVolumeId();

    String getName();

    long getSnapshotId();

    Date getCreated();

    Type getRecurringType();

    @Override
    State getState();

    HypervisorType getHypervisorType();

    boolean isRecursive();

    short getSnapshotType();

    LocationType getLocationType(); // This type is in reference to the location where the snapshot resides (ex. primary storage, archive on secondary storage, etc.)
}
