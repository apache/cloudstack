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
package org.apache.cloudstack.storage.sharedfs;

import com.cloud.utils.fsm.StateMachine2;
import com.cloud.utils.fsm.StateObject;

import org.apache.cloudstack.acl.ControlledEntity;
import org.apache.cloudstack.api.Identity;
import org.apache.cloudstack.api.InternalIdentity;
import org.apache.cloudstack.framework.config.ConfigKey;

import java.util.Date;

public interface SharedFS extends ControlledEntity, Identity, InternalIdentity, StateObject<SharedFS.State> {

    static final ConfigKey<Boolean> SharedFSFeatureEnabled = new ConfigKey<Boolean>("Advanced", Boolean.class,
            "sharedfs.feature.enabled",
            "true",
            " Indicates whether the Shared FileSystem feature is enabled or not. Management server restart needed on change",
            false);

    ConfigKey<Integer> SharedFSCleanupInterval = new ConfigKey<>(Integer.class,
            "sharedfs.cleanup.interval",
            "Advanced",
            "14400",
            "The interval (in seconds) to wait before running the shared filesystem cleanup thread.",
            false,
            ConfigKey.Scope.Global,
            null,
            SharedFSFeatureEnabled.key());

    ConfigKey<Integer> SharedFSCleanupDelay = new ConfigKey<>(Integer.class,
            "sharedfs.cleanup.delay",
            "Advanced",
            "86400",
            "Determines how long (in seconds) to wait before actually expunging destroyed shared filesystem.",
            false,
            ConfigKey.Scope.Global,
            null,
            SharedFSFeatureEnabled.key());

    ConfigKey<Integer> SharedFSExpungeWorkers = new ConfigKey<>(Integer.class,
            "sharedfs.expunge.workers",
            "Advanced",
            "2",
            "Determines how many threads are created to do the work of expunging destroyed shared filesystem.",
            false,
            ConfigKey.Scope.Global,
            null,
            SharedFSFeatureEnabled.key());

    String SharedFSVmNamePrefix = "sharedfs";
    String SharedFSPath = "/export";

    enum FileSystemType {
        EXT4, XFS
    }

    enum Protocol {
        NFS
    }

    enum State {
        Allocated(false, "The shared filesystem is allocated in db but hasn't been created or started yet."),
        Ready(false, "The shared filesystem is ready to use."),
        Stopping(true, "The shared filesystem is being stopped"),
        Stopped(false, "The shared filesystem is in stopped state. It can not be used but the data is still there."),
        Starting(true, "The shared filesystem is being started."),
        Destroyed(false, "The shared filesystem is destroyed."),
        Expunging(true, "The shared filesystem is being expunged."),
        Expunged(false, "The shared filesystem has been expunged."),
        Error(false, "The shared filesystem is in error state.");

        boolean _transitional;
        String _description;

        /**
         * SharedFS State
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

        private final static StateMachine2<State, Event, SharedFS> s_fsm = new StateMachine2<State, Event, SharedFS>();

        public static StateMachine2<SharedFS.State, SharedFS.Event, SharedFS> getStateMachine() {
            return s_fsm;
        }

        static {
            s_fsm.addTransition(new StateMachine2.Transition<State, Event>(Allocated, Event.OperationFailed, Error, null));
            s_fsm.addTransition(new StateMachine2.Transition<State, Event>(Allocated, Event.OperationSucceeded, Ready, null));
            s_fsm.addTransition(new StateMachine2.Transition<State, Event>(Error, Event.DestroyRequested, Destroyed, null));
            s_fsm.addTransition(new StateMachine2.Transition<State, Event>(Stopped, Event.StartRequested, Starting, null));
            s_fsm.addTransition(new StateMachine2.Transition<State, Event>(Starting, Event.OperationSucceeded, Ready, null));
            s_fsm.addTransition(new StateMachine2.Transition<State, Event>(Starting, Event.OperationFailed, Stopped, null));
            s_fsm.addTransition(new StateMachine2.Transition<State, Event>(Ready, Event.StopRequested, Stopping, null));
            s_fsm.addTransition(new StateMachine2.Transition<State, Event>(Stopping, Event.OperationSucceeded, Stopped, null));
            s_fsm.addTransition(new StateMachine2.Transition<State, Event>(Stopping, Event.OperationFailed, Ready, null));
            s_fsm.addTransition(new StateMachine2.Transition<State, Event>(Stopped, Event.DestroyRequested, Destroyed, null));
            s_fsm.addTransition(new StateMachine2.Transition<State, Event>(Destroyed, Event.RecoveryRequested, Stopped, null));
            s_fsm.addTransition(new StateMachine2.Transition<State, Event>(Destroyed, Event.ExpungeOperation, Expunging, null));
            s_fsm.addTransition(new StateMachine2.Transition<State, Event>(Error, Event.ExpungeOperation, Expunging, null));
            s_fsm.addTransition(new StateMachine2.Transition<State, Event>(Expunging, Event.ExpungeOperation, Expunging, null));
            s_fsm.addTransition(new StateMachine2.Transition<State, Event>(Expunging, Event.OperationSucceeded, Expunged, null));
        }
    }

    enum Event {
        StopRequested,
        StartRequested,
        DestroyRequested,
        OperationSucceeded,
        OperationFailed,
        ExpungeOperation,
        RecoveryRequested,
    }

    static String getSharedFSPath() {
        return SharedFSPath;
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
