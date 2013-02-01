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

import java.util.Date;

import com.cloud.hypervisor.Hypervisor.HypervisorType;
import com.cloud.utils.fsm.StateMachine2;
import com.cloud.utils.fsm.StateObject;
import org.apache.cloudstack.acl.ControlledEntity;
import org.apache.cloudstack.api.Identity;
import org.apache.cloudstack.api.InternalIdentity;

public interface Snapshot extends ControlledEntity, Identity, InternalIdentity, StateObject<Snapshot.State> {
    public enum Type {
        MANUAL,
        RECURRING,
        TEMPLATE,
        HOURLY,
        DAILY,
        WEEKLY,
        MONTHLY;
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
        Creating,
        CreatedOnPrimary,
        BackingUp,
        BackedUp,
        Error;

        private final static StateMachine2<State, Event, Snapshot> s_fsm = new StateMachine2<State, Event, Snapshot>();

        public static StateMachine2<State, Event, Snapshot> getStateMachine() {
            return s_fsm;
        }

        static {
            s_fsm.addTransition(null, Event.CreateRequested, Creating);
            s_fsm.addTransition(Creating, Event.OperationSucceeded, CreatedOnPrimary);
            s_fsm.addTransition(Creating, Event.OperationNotPerformed, BackedUp);
            s_fsm.addTransition(Creating, Event.OperationFailed, Error);
            s_fsm.addTransition(CreatedOnPrimary, Event.BackupToSecondary, BackingUp);
            s_fsm.addTransition(BackingUp, Event.OperationSucceeded, BackedUp);
            s_fsm.addTransition(BackingUp, Event.OperationFailed, Error);
        }

        public String toString() {
            return this.name();
        }

        public boolean equals(String status) {
            return this.toString().equalsIgnoreCase(status);
        }
    }

    enum Event {
        CreateRequested,
        OperationNotPerformed,
        BackupToSecondary,
        BackedupToSecondary,
        OperationSucceeded,
        OperationFailed
    }

    public static final long MANUAL_POLICY_ID = 0L;

    long getAccountId();

    long getVolumeId();

    String getPath();

    String getName();

    Date getCreated();

    Type getType();

    State getState();

    HypervisorType getHypervisorType();

    boolean isRecursive();

    short getsnapshotType();

}
