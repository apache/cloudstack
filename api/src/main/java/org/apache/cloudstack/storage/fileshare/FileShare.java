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

import org.apache.cloudstack.acl.ControlledEntity;
import org.apache.cloudstack.api.Identity;
import org.apache.cloudstack.api.InternalIdentity;
import org.apache.cloudstack.framework.config.ConfigKey;

import java.util.Date;

public interface FileShare extends ControlledEntity, Identity, InternalIdentity, StateObject<FileShare.State> {

    static final ConfigKey<Boolean> FileShareFeatureEnabled = new ConfigKey<Boolean>("Advanced", Boolean.class,
            "file.share.feature.enabled",
            "true",
            " Indicates whether the File Share feature is enabled or not. Management server restart needed on change",
            false);

    ConfigKey<Integer> FileShareCleanupInterval = new ConfigKey<>(Integer.class,
            "fileshare.cleanup.interval",
            "Advanced",
            "60",
            "The interval (in seconds) to wait before running the fileshare cleanup thread.",
            false,
            ConfigKey.Scope.Global,
            null,
            FileShareFeatureEnabled.key());

    ConfigKey<Integer> FileShareCleanupDelay = new ConfigKey<>(Integer.class,
            "fileshare.cleanup.delay",
            "Advanced",
            "60",
            "Determines how long (in seconds) to wait before actually expunging destroyed file shares. The default value = the default value of fileshare.cleanup.interval.",
            false,
            ConfigKey.Scope.Global,
            null,
            FileShareFeatureEnabled.key());

    String FileShareVmNamePrefix = "fsvm";
    String FileSharePath = "/mnt/fs/share";

    enum FileSystemType {
        EXT4, XFS
    }

    enum Protocol {
        NFS, SMB
    }

    enum State {
        Allocated(false, "The file share is allocated in db but hasn't been created or started yet."),
        Ready(false, "The file share is ready to use."),
        Stopping(true, "The file share is being stopped"),
        Stopped(false, "The file share is in stopped state. It can not be used but the data is still there."),
        Starting(true, "The file share is being started."),
        Detached(false, "The file share Data is not attached to any VM."),
        Destroyed(false, "The file share is destroyed."),
        Expunging(false, "The file share is being expunged."),
        Error(false, "The file share is in error state.");

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
            s_fsm.addTransition(new StateMachine2.Transition<State, Event>(Allocated, Event.OperationFailed, Error, null));
            s_fsm.addTransition(new StateMachine2.Transition<State, Event>(Allocated, Event.OperationSucceeded, Stopped, null));
            s_fsm.addTransition(new StateMachine2.Transition<State, Event>(Error, Event.DestroyRequested, Destroyed, null));
            s_fsm.addTransition(new StateMachine2.Transition<State, Event>(Stopped, Event.StartRequested, Starting, null));
            s_fsm.addTransition(new StateMachine2.Transition<State, Event>(Starting, Event.OperationSucceeded, Ready, null));
            s_fsm.addTransition(new StateMachine2.Transition<State, Event>(Starting, Event.OperationFailed, Stopped, null));
            s_fsm.addTransition(new StateMachine2.Transition<State, Event>(Detached, Event.OperationSucceeded, Stopped, null));
            s_fsm.addTransition(new StateMachine2.Transition<State, Event>(Detached, Event.OperationFailed, Detached, null));
            s_fsm.addTransition(new StateMachine2.Transition<State, Event>(Ready, Event.StopRequested, Stopping, null));
            s_fsm.addTransition(new StateMachine2.Transition<State, Event>(Stopping, Event.OperationSucceeded, Stopped, null));
            s_fsm.addTransition(new StateMachine2.Transition<State, Event>(Stopping, Event.OperationFailed, Ready, null));
            s_fsm.addTransition(new StateMachine2.Transition<State, Event>(Stopped, Event.Detach, Detached, null));
            s_fsm.addTransition(new StateMachine2.Transition<State, Event>(Stopped, Event.DestroyRequested, Destroyed, null));
            s_fsm.addTransition(new StateMachine2.Transition<State, Event>(Destroyed, Event.RecoveryRequested, Stopped, null));
            s_fsm.addTransition(new StateMachine2.Transition<State, Event>(Destroyed, Event.ExpungeOperation, Expunging, null));
            s_fsm.addTransition(new StateMachine2.Transition<State, Event>(Expunging, Event.ExpungeOperation, Expunging, null));
        }
    }

    enum Event {
        DeployRequested,
        InitializationRequested,
        StopRequested,
        StartRequested,
        Detach,
        DestroyRequested,
        OperationSucceeded,
        OperationFailed,
        ExpungeOperation,
        RecoveryRequested,
    }

    static String getFileSharePathFromNameAndUuid(String name, String uuid) {
        return FileSharePath;
    }

    long getId();

    String getName();

    void setName(String name);

    String getUuid();

    String getDescription();

    void setDescription(String description);

    Long getDataCenterId();

    State getState();

    String getFsProviderName();

    Protocol getProtocol();

    Long getVolumeId();

    void setVolumeId(Long volumeId);

    Long getVmId();

    void setVmId(Long vmId);

    FileSystemType getFsType();

    Long getServiceOfferingId();

    void setServiceOfferingId(Long serviceOfferingId);

    Date getUpdated();

    public long getUpdatedCount();

    public void incrUpdatedCount();
}
