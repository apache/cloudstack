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
package org.apache.cloudstack.storage.fileshare;

import com.cloud.utils.fsm.StateMachine2;
import com.cloud.utils.fsm.StateObject;

public interface FileShare extends StateObject<FileShare.State> {

    String FileShareVmNamePrefix = "fs";

    enum FileSystemType {
        EXT3, EXT4, XFS
    }

    enum Protocol {
        NFS, SMB
    }

    enum State {
        Allocated(false, "The file share is allocated in db but hasn't been created or initialized yet."),
        Creating(true, "The file share is being created."),
        Created(false, "The file share is created but not initialized yet."),
        Initializing(true, "The file share is being initialzed."),
        Ready(false, "The file share is initialized and ready to use."),
        Stopping(true, "The file share is being stopped"),
        Stopped(false, "The file share is in stopped state. It can not be used but the data is still there."),
        Starting(false, "The file share is being started."),
        Destroyed(true, "The file share is destroyed."),
        Expunging(false, "The file share is being expunged.");

        boolean _transitional;
        String _description;

        /**
         * File Share State
         *
         * @param transitional true for transition/non-final state, otherwise false
         * @param description  description of the state
         */
        State(boolean transitional, String description) {
            _transitional = transitional;
            _description = description;
        }

        public boolean isTransitional() {
            return _transitional;
        }

        public String getDescription() {
            return _description;
        }

        private final static StateMachine2<State, Event, FileShare> s_fsm = new StateMachine2<State, Event, FileShare>();

        static {
            s_fsm.addTransition(new StateMachine2.Transition<State, Event>(Allocated, Event.CreateRequested, Creating, null));
            s_fsm.addTransition(new StateMachine2.Transition<State, Event>(Allocated, Event.DestroyRequested, Destroyed, null));
            s_fsm.addTransition(new StateMachine2.Transition<State, Event>(Creating, Event.OperationSucceeded, Destroyed, null));
            s_fsm.addTransition(new StateMachine2.Transition<State, Event>(Creating, Event.OperationFailed, Allocated, null));
            s_fsm.addTransition(new StateMachine2.Transition<State, Event>(Created, Event.InitializationRequested, Initializing, null));
            s_fsm.addTransition(new StateMachine2.Transition<State, Event>(Created, Event.DestroyRequested, Destroyed, null));
            s_fsm.addTransition(new StateMachine2.Transition<State, Event>(Initializing, Event.OperationSucceeded, Ready, null));
            s_fsm.addTransition(new StateMachine2.Transition<State, Event>(Initializing, Event.OperationFailed, Created, null));
            s_fsm.addTransition(new StateMachine2.Transition<State, Event>(Ready, Event.StopRequested, Stopping, null));
            s_fsm.addTransition(new StateMachine2.Transition<State, Event>(Stopping, Event.OperationSucceeded, Stopped, null));
            s_fsm.addTransition(new StateMachine2.Transition<State, Event>(Stopping, Event.OperationFailed, Ready, null));
            s_fsm.addTransition(new StateMachine2.Transition<State, Event>(Stopped, Event.InitializationRequested, Initializing, null));
            s_fsm.addTransition(new StateMachine2.Transition<State, Event>(Stopped, Event.StartRequested, Starting, null));
            s_fsm.addTransition(new StateMachine2.Transition<State, Event>(Stopped, Event.DestroyRequested, Destroyed, null));
            s_fsm.addTransition(new StateMachine2.Transition<State, Event>(Starting, Event.OperationSucceeded, Ready, null));
            s_fsm.addTransition(new StateMachine2.Transition<State, Event>(Starting, Event.OperationFailed, Stopped, null));
            s_fsm.addTransition(new StateMachine2.Transition<State, Event>(Destroyed, Event.RecoveryRequested, Stopped, null));
            s_fsm.addTransition(new StateMachine2.Transition<State, Event>(Destroyed, Event.ExpungeOperation, Expunging, null));
        }
    }

    enum Event {
        CreateRequested,
        InitializationRequested,
        StopRequested,
        StartRequested,
        DestroyRequested,
        OperationSucceeded,
        OperationFailed,
        ExpungeOperation,
        RecoveryRequested,
    }

    Long getId();

    String getName();

    String getUuid();

    String getDescription();

    Long getDomainId();

    Long getAccountId();

    Long getProjectId();

    Long getDataCenterId();

    State getState();

    String getEndpointIp();

    void setEndpointIp(String endpointIp);

    String getEndpointPath();

    void setEndpointPath(String endpointPath);

    String getFsProviderName();

    Long getSize();

    Protocol getProtocol();

    Long getVolumeId();

    void setVolumeId(Long volumeId);

    Long getStorageVmId();

    void setStorageVmId(Long vmId);

    String getMountOptions();

    FileSystemType getFsType();

    Long getDiskOfferingId();

    Long getServiceOfferingId();
}
