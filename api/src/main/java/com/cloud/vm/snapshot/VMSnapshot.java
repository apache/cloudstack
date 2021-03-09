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

package com.cloud.vm.snapshot;

import java.util.Date;

import org.apache.cloudstack.acl.ControlledEntity;
import org.apache.cloudstack.api.Identity;
import org.apache.cloudstack.api.InternalIdentity;

import com.cloud.utils.fsm.StateMachine2;
import com.cloud.utils.fsm.StateObject;

public interface VMSnapshot extends ControlledEntity, Identity, InternalIdentity, StateObject<VMSnapshot.State> {

    enum State {
        Allocated("The VM snapshot is allocated but has not been created yet."), Creating("The VM snapshot is being created."), Ready(
                "The VM snapshot is ready to be used."), Reverting("The VM snapshot is being used to revert"), Expunging("The volume is being expunging"), Removed(
                "The volume is destroyed, and can't be recovered."), Error("The volume is in error state, and can't be recovered");

        String _description;

        private State(String description) {
            _description = description;
        }

        public static StateMachine2<State, Event, VMSnapshot> getStateMachine() {
            return s_fsm;
        }

        public String getDescription() {
            return _description;
        }

        private final static StateMachine2<State, Event, VMSnapshot> s_fsm = new StateMachine2<State, Event, VMSnapshot>();
        static {
            s_fsm.addTransition(Allocated, Event.CreateRequested, Creating);
            s_fsm.addTransition(Creating, Event.OperationSucceeded, Ready);
            s_fsm.addTransition(Creating, Event.OperationFailed, Error);
            s_fsm.addTransition(Ready, Event.RevertRequested, Reverting);
            s_fsm.addTransition(Reverting, Event.OperationSucceeded, Ready);
            s_fsm.addTransition(Reverting, Event.OperationFailed, Ready);
            s_fsm.addTransition(Ready, Event.ExpungeRequested, Expunging);
            s_fsm.addTransition(Error, Event.ExpungeRequested, Expunging);
            s_fsm.addTransition(Expunging, Event.ExpungeRequested, Expunging);
            s_fsm.addTransition(Expunging, Event.OperationSucceeded, Removed);
            s_fsm.addTransition(Expunging, Event.OperationFailed, Error);
        }
    }

    enum Type {
        Disk, DiskAndMemory
    }

    enum Event {
        CreateRequested, OperationFailed, OperationSucceeded, RevertRequested, ExpungeRequested,
    }

    @Override
    long getId();

    public String getName();

    public Long getVmId();

    @Override
    public State getState();

    public Date getCreated();

    public String getDescription();

    public String getDisplayName();

    public Long getParent();

    public Boolean getCurrent();

    public Type getType();

    public long getUpdatedCount();

    public void incrUpdatedCount();

    public Date getUpdated();

    public Date getRemoved();

    @Override
    public long getAccountId();

    public long getServiceOfferingId();
}
