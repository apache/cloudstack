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
package org.apache.cloudstack.applicationcluster;

import org.apache.cloudstack.acl.ControlledEntity;
import org.apache.cloudstack.api.Displayable;
import org.apache.cloudstack.api.Identity;
import org.apache.cloudstack.api.InternalIdentity;
import com.cloud.utils.fsm.StateMachine2;

/**
 * ApplicationCluster describes the properties of container cluster
 *
 */
public interface ApplicationCluster extends ControlledEntity, com.cloud.utils.fsm.StateObject<ApplicationCluster.State>, Identity, InternalIdentity, Displayable {

    enum Event {
        StartRequested,
        StopRequested,
        DestroyRequested,
        RecoveryRequested,
        ScaleUpRequested,
        ScaleDownRequested,
        OperationSucceeded,
        OperationFailed,
        CreateFailed,
        FaultsDetected;
    }

    enum State {
        Created("Initial State of container cluster. At this state its just a logical/DB entry with no resources consumed"),
        Starting("Resources needed for container cluster are being provisioned"),
        Running("Necessary resources are provisioned and container cluster is in operational ready state to launch containers"),
        Stopping("Ephermal resources for the container cluster are being destroyed"),
        Stopped("All ephermal resources for the container cluster are destroyed, Container cluster may still have ephermal resource like persistent volumens provisioned"),
        Scaling("Transient state in which resoures are either getting scaled up/down"),
        Alert("State to represent container clusters which are not in expected desired state (operationally in active control place, stopped cluster VM's etc)."),
        Recovering("State in which container cluster is recovering from alert state"),
        Destroyed("End state of container cluster in which all resources are destroyed, cluster will not be useable further"),
        Destroying("State in which resources for the container cluster is getting cleaned up or yet to be cleaned up by garbage collector"),
        Error("State of the failed to create container clusters");

        protected static final StateMachine2<State, ApplicationCluster.Event, ApplicationCluster> s_fsm = new StateMachine2<State, ApplicationCluster.Event, ApplicationCluster>();

        public static StateMachine2<State, ApplicationCluster.Event, ApplicationCluster> getStateMachine() { return s_fsm; }

        static {
            s_fsm.addTransition(State.Created, Event.StartRequested, State.Starting);

            s_fsm.addTransition(State.Starting, Event.OperationSucceeded, State.Running);
            s_fsm.addTransition(State.Starting, Event.OperationFailed, State.Alert);
            s_fsm.addTransition(State.Starting, Event.CreateFailed, State.Error);

            s_fsm.addTransition(State.Running, Event.StopRequested, State.Stopping);
            s_fsm.addTransition(State.Stopping, Event.OperationSucceeded, State.Stopped);
            s_fsm.addTransition(State.Stopping, Event.OperationFailed, State.Alert);

            s_fsm.addTransition(State.Stopped, Event.StartRequested, State.Starting);

            s_fsm.addTransition(State.Running, Event.FaultsDetected, State.Alert);

            s_fsm.addTransition(State.Running, Event.ScaleUpRequested, State.Scaling);
            s_fsm.addTransition(State.Running, Event.ScaleDownRequested, State.Scaling);
            s_fsm.addTransition(State.Scaling, Event.OperationSucceeded, State.Running);
            s_fsm.addTransition(State.Scaling, Event.OperationFailed, State.Alert);

            s_fsm.addTransition(State.Alert, Event.RecoveryRequested, State.Recovering);
            s_fsm.addTransition(State.Recovering, Event.OperationSucceeded, State.Running);
            s_fsm.addTransition(State.Recovering, Event.OperationFailed, State.Alert);

            s_fsm.addTransition(State.Running, Event.DestroyRequested, State.Destroying);
            s_fsm.addTransition(State.Stopped, Event.DestroyRequested, State.Destroying);
            s_fsm.addTransition(State.Alert, Event.DestroyRequested, State.Destroying);
            s_fsm.addTransition(State.Error, Event.DestroyRequested, State.Destroying);

            s_fsm.addTransition(State.Destroying, Event.DestroyRequested, State.Destroying);
            s_fsm.addTransition(State.Destroying, Event.OperationSucceeded, State.Destroyed);
            s_fsm.addTransition(State.Destroying, Event.OperationFailed, State.Destroying);

        }
        String _description;

        private State(String description) {
             _description = description;
        }
    }

    long getId();
    String getName();
    String getDescription();
    long getZoneId();
    long getServiceOfferingId();
    long getTemplateId();
    long getNetworkId();
    long getDomainId();
    long getAccountId();
    long getNodeCount();
    String getKeyPair();
    long getCores();
    long getMemory();
    String getEndpoint();
    String getConsoleEndpoint();
    State getState();
}
