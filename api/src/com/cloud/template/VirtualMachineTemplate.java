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
package com.cloud.template;

import java.util.Date;
import java.util.Map;

import org.apache.cloudstack.acl.ControlledEntity;
import org.apache.cloudstack.api.Identity;
import org.apache.cloudstack.api.InternalIdentity;

import com.cloud.hypervisor.Hypervisor.HypervisorType;
import com.cloud.storage.Storage.ImageFormat;
import com.cloud.storage.Storage.TemplateType;
import com.cloud.storage.Volume.Event;
import com.cloud.storage.Volume.State;
import com.cloud.utils.fsm.StateMachine2;
import com.cloud.utils.fsm.StateObject;

public interface VirtualMachineTemplate extends ControlledEntity, Identity, InternalIdentity, StateObject<VirtualMachineTemplate.State> {
    enum State {
        Active,
        Inactive,
        NotUploaded,
        UploadInProgress,
        UploadError,
        UploadAbandoned;

        public static StateMachine2<State, Event, VirtualMachineTemplate> getStateMachine() {
            return s_fsm;
        }

        private final static StateMachine2<State, Event, VirtualMachineTemplate> s_fsm = new StateMachine2<State, Event, VirtualMachineTemplate>();
        static {
            s_fsm.addTransition(new StateMachine2.Transition<State, Event>(NotUploaded, Event.OperationTimeout, UploadAbandoned, null));
            s_fsm.addTransition(new StateMachine2.Transition<State, Event>(NotUploaded, Event.UploadRequested, UploadInProgress, null));
            s_fsm.addTransition(new StateMachine2.Transition<State, Event>(NotUploaded, Event.OperationSucceeded, Active, null));
            s_fsm.addTransition(new StateMachine2.Transition<State, Event>(NotUploaded, Event.OperationFailed, UploadError, null));
            s_fsm.addTransition(new StateMachine2.Transition<State, Event>(UploadInProgress, Event.OperationSucceeded, Active, null));
            s_fsm.addTransition(new StateMachine2.Transition<State, Event>(UploadInProgress, Event.OperationFailed, UploadError, null));
            s_fsm.addTransition(new StateMachine2.Transition<State, Event>(UploadInProgress, Event.OperationTimeout, UploadError, null));
        }
    }

    enum Event {
        OperationFailed,
        OperationSucceeded,
        UploadRequested,
        OperationTimeout;
    }

    public static enum BootloaderType {
        PyGrub, HVM, External, CD
    };

    public enum TemplateFilter {
        featured, // returns templates that have been marked as featured and public
        self, // returns templates that have been registered or created by the calling user
        selfexecutable, // same as self, but only returns templates that are ready to be deployed with
        shared, // including templates that have been granted to the calling user by another user
        sharedexecutable, // ready templates that have been granted to the calling user by another user
        executable, // templates that are owned by the calling user, or public templates, that can be used to deploy a
        community, // returns templates that have been marked as public but not featured
        all // all templates (only usable by admins)
    }

    @Override
    State getState();

    boolean isFeatured();

    /**
     * @return public or private template
     */
    boolean isPublicTemplate();

    boolean isExtractable();

    /**
     * @return name
     */
    String getName();

    ImageFormat getFormat();

    boolean isRequiresHvm();

    String getDisplayText();

    boolean getEnablePassword();

    boolean getEnableSshKey();

    boolean isCrossZones();

    Date getCreated();

    long getGuestOSId();

    boolean isBootable();

    TemplateType getTemplateType();

    HypervisorType getHypervisorType();

    int getBits();

    String getUniqueName();

    String getUrl();

    String getChecksum();

    Long getSourceTemplateId();

    String getTemplateTag();

    Map getDetails();

    boolean isDynamicallyScalable();

    Long getParentTemplateId();

    long getUpdatedCount();

    void incrUpdatedCount();

    Date getUpdated();
}
