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
import org.apache.cloudstack.api.Identity;
import org.apache.cloudstack.api.InternalIdentity;

import java.util.Date;

public interface FileShare extends StateObject<FileShare.State>, Identity, InternalIdentity {

    String FileShareVmNamePrefix = "fsvm";
    String FileSharePathPrefix = "/export/fileshare/";

    enum FileSystemType {
        EXT3, EXT4, XFS
    }

    enum Protocol {
        NFS, SMB
    }

    enum State {
        Allocated(false, "The file share is allocated in db but hasn't been created or initialized yet."),
        Deploying(true, "The file share is being created."),
        Deployed(false, "The file share is deployed but not initialized yet."),
        Initializing(true, "The file share is being initialized."),
        Ready(false, "The file share is ready to use."),
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

        public static StateMachine2<FileShare.State, FileShare.Event, FileShare> getStateMachine() {
            return s_fsm;
        }

        static {
            s_fsm.addTransition(new StateMachine2.Transition<State, Event>(Allocated, Event.DeployRequested, Deploying, null));
            s_fsm.addTransition(new StateMachine2.Transition<State, Event>(Allocated, Event.DestroyRequested, Destroyed, null));
            s_fsm.addTransition(new StateMachine2.Transition<State, Event>(Deploying, Event.OperationSucceeded, Deployed, null));
            s_fsm.addTransition(new StateMachine2.Transition<State, Event>(Deploying, Event.OperationFailed, Allocated, null));
            s_fsm.addTransition(new StateMachine2.Transition<State, Event>(Deployed, Event.StartRequested, Initializing, null));
            s_fsm.addTransition(new StateMachine2.Transition<State, Event>(Deployed, Event.DestroyRequested, Destroyed, null));
            s_fsm.addTransition(new StateMachine2.Transition<State, Event>(Initializing, Event.OperationSucceeded, Ready, null));
            s_fsm.addTransition(new StateMachine2.Transition<State, Event>(Initializing, Event.OperationFailed, Deployed, null));
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
            s_fsm.addTransition(new StateMachine2.Transition<State, Event>(Expunging, Event.OperationFailed, Destroyed, null));
        }
    }

    enum Event {
        DeployRequested,
        InitializationRequested,
        StopRequested,
        StartRequested,
        DestroyRequested,
        OperationSucceeded,
        OperationFailed,
        ExpungeOperation,
        RecoveryRequested,
    }

    static String getFileSharePathFromNameAndUuid(String name, String uuid) {
        return FileSharePathPrefix + name + "-" + uuid;
    }

    long getId();

    String getName();

    void setName(String name);

    String getUuid();

    String getDescription();

    void setDescription(String description);

    Long getDomainId();

    Long getAccountId();

    Long getProjectId();

    Long getDataCenterId();

    State getState();

    String getFsProviderName();

    Protocol getProtocol();

    Long getVolumeId();

    void setVolumeId(Long volumeId);

    Long getVmId();

    void setVmId(Long vmId);

    String getMountOptions();

    FileSystemType getFsType();

    Long getServiceOfferingId();

    void setServiceOfferingId(Long serviceOfferingId);

    Date getUpdated();

    public long getUpdatedCount();

    public void incrUpdatedCount();
}
