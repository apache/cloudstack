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
package com.cloud.kubernetescluster;

import org.apache.cloudstack.acl.ControlledEntity;
import org.apache.cloudstack.api.Displayable;
import org.apache.cloudstack.api.Identity;
import org.apache.cloudstack.api.InternalIdentity;

import com.cloud.utils.fsm.StateMachine2;

/**
 * KubernetesCluster describes the properties of kubernetes cluster
 *
 */
public interface KubernetesCluster extends ControlledEntity, com.cloud.utils.fsm.StateObject<KubernetesCluster.State>, Identity, InternalIdentity, Displayable {

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
        Created("Initial State of kubernetes cluster. At this state its just a logical/DB entry with no resources consumed"),
        Starting("Resources needed for kubernetes cluster are being provisioned"),
        Running("Necessary resources are provisioned and kubernetes cluster is in operational ready state to launch kubernetess"),
        Stopping("Ephermal resources for the kubernetes cluster are being destroyed"),
        Stopped("All ephermal resources for the kubernetes cluster are destroyed, Kubernetes cluster may still have ephermal resource like persistent volumens provisioned"),
        Scaling("Transient state in which resoures are either getting scaled up/down"),
        Alert("State to represent kubernetes clusters which are not in expected desired state (operationally in active control place, stopped cluster VM's etc)."),
        Recovering("State in which kubernetes cluster is recovering from alert state"),
        Destroyed("End state of kubernetes cluster in which all resources are destroyed, cluster will not be useable further"),
        Destroying("State in which resources for the kubernetes cluster is getting cleaned up or yet to be cleaned up by garbage collector"),
        Error("State of the failed to create kubernetes clusters");

        protected static final StateMachine2<State, KubernetesCluster.Event, KubernetesCluster> s_fsm = new StateMachine2<State, KubernetesCluster.Event, KubernetesCluster>();

        public static StateMachine2<State, KubernetesCluster.Event, KubernetesCluster> getStateMachine() { return s_fsm; }

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

        State(String description) {
             _description = description;
        }
    }

    long getId();
    String getName();
    String getDescription();
    long getZoneId();
    long getKubernetesVersionId();
    long getServiceOfferingId();
    long getTemplateId();
    long getNetworkId();
    long getDomainId();
    long getAccountId();
    long getMasterNodeCount();
    long getNodeCount();
    long getTotalNodeCount();
    String getKeyPair();
    long getCores();
    long getMemory();
    long getNodeRootDiskSize();
    String getEndpoint();
    String getConsoleEndpoint();
    boolean isCheckForGc();
    @Override
    State getState();
}
