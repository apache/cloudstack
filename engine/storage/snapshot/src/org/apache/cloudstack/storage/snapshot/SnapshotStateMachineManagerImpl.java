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

package org.apache.cloudstack.storage.snapshot;

import javax.inject.Inject;

import org.springframework.stereotype.Component;

import com.cloud.storage.Snapshot;
import com.cloud.storage.Snapshot.Event;
import com.cloud.storage.Snapshot.State;
import com.cloud.storage.SnapshotVO;
import com.cloud.storage.dao.SnapshotDao;
import com.cloud.storage.listener.SnapshotStateListener;
import com.cloud.utils.fsm.NoTransitionException;
import com.cloud.utils.fsm.StateMachine2;

@Component
public class SnapshotStateMachineManagerImpl implements SnapshotStateMachineManager {
    private StateMachine2<State, Event, SnapshotVO> stateMachine = new StateMachine2<State, Event, SnapshotVO>();
    @Inject
    protected SnapshotDao snapshotDao;

    public SnapshotStateMachineManagerImpl() {
        stateMachine.addTransition(Snapshot.State.Allocated, Event.CreateRequested, Snapshot.State.Creating);
        stateMachine.addTransition(Snapshot.State.Creating, Event.OperationSucceeded, Snapshot.State.CreatedOnPrimary);
        stateMachine.addTransition(Snapshot.State.Creating, Event.OperationNotPerformed, Snapshot.State.BackedUp);
        stateMachine.addTransition(Snapshot.State.Creating, Event.OperationFailed, Snapshot.State.Error);
        stateMachine.addTransition(Snapshot.State.CreatedOnPrimary, Event.BackupToSecondary, Snapshot.State.BackingUp);
        stateMachine.addTransition(State.CreatedOnPrimary, Event.OperationNotPerformed, State.BackedUp);
        stateMachine.addTransition(Snapshot.State.BackingUp, Event.OperationSucceeded, Snapshot.State.BackedUp);
        stateMachine.addTransition(Snapshot.State.BackingUp, Event.OperationFailed, Snapshot.State.CreatedOnPrimary);
        stateMachine.addTransition(Snapshot.State.BackedUp, Event.DestroyRequested, Snapshot.State.Destroying);
        stateMachine.addTransition(Snapshot.State.BackedUp, Event.CopyingRequested, Snapshot.State.Copying);
        stateMachine.addTransition(Snapshot.State.Copying, Event.OperationSucceeded, Snapshot.State.BackedUp);
        stateMachine.addTransition(Snapshot.State.Copying, Event.OperationFailed, Snapshot.State.BackedUp);
        stateMachine.addTransition(Snapshot.State.Destroying, Event.OperationSucceeded, Snapshot.State.Destroyed);
        stateMachine.addTransition(Snapshot.State.Destroying, Event.OperationFailed, State.BackedUp);

        stateMachine.registerListener(new SnapshotStateListener());
    }

    @Override
    public void processEvent(SnapshotVO snapshot, Event event) throws NoTransitionException {
        stateMachine.transitTo(snapshot, event, null, snapshotDao);
    }
}
