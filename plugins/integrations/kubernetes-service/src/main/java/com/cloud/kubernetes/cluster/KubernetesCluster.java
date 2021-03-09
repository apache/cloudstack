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
package com.cloud.kubernetes.cluster;

import java.util.Date;

import org.apache.cloudstack.acl.ControlledEntity;
import org.apache.cloudstack.api.Displayable;
import org.apache.cloudstack.api.Identity;
import org.apache.cloudstack.api.InternalIdentity;

import com.cloud.utils.fsm.StateMachine2;

/**
 * KubernetesCluster describes the properties of a Kubernetes cluster
 * StateMachine maintains its states.
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
        UpgradeRequested,
        OperationSucceeded,
        OperationFailed,
        CreateFailed,
        FaultsDetected;
    }

    enum State {
        Created("Initial State of Kubernetes cluster. At this state its just a logical/DB entry with no resources consumed"),
        Starting("Resources needed for Kubernetes cluster are being provisioned"),
        Running("Necessary resources are provisioned and Kubernetes cluster is in operational ready state to launch Kubernetes"),
        Stopping("Resources for the Kubernetes cluster are being destroyed"),
        Stopped("All resources for the Kubernetes cluster are destroyed, Kubernetes cluster may still have ephemeral resource like persistent volumes provisioned"),
        Scaling("Transient state in which resources are either getting scaled up/down"),
        Upgrading("Transient state in which cluster is getting upgraded"),
        Alert("State to represent Kubernetes clusters which are not in expected desired state (operationally in active control place, stopped cluster VM's etc)."),
        Recovering("State in which Kubernetes cluster is recovering from alert state"),
        Destroyed("End state of Kubernetes cluster in which all resources are destroyed, cluster will not be usable further"),
        Destroying("State in which resources for the Kubernetes cluster is getting cleaned up or yet to be cleaned up by garbage collector"),
        Error("State of the failed to create Kubernetes clusters");

        protected static final StateMachine2<State, KubernetesCluster.Event, KubernetesCluster> s_fsm = new StateMachine2<State, KubernetesCluster.Event, KubernetesCluster>();

        public static StateMachine2<State, KubernetesCluster.Event, KubernetesCluster> getStateMachine() { return s_fsm; }

        static {
            s_fsm.addTransition(State.Created, Event.StartRequested, State.Starting);

            s_fsm.addTransition(State.Starting, Event.OperationSucceeded, State.Running);
            s_fsm.addTransition(State.Starting, Event.OperationFailed, State.Alert);
            s_fsm.addTransition(State.Starting, Event.CreateFailed, State.Error);
            s_fsm.addTransition(State.Starting, Event.StopRequested, State.Stopping);

            s_fsm.addTransition(State.Running, Event.StopRequested, State.Stopping);
            s_fsm.addTransition(State.Alert, Event.StopRequested, State.Stopping);
            s_fsm.addTransition(State.Stopping, Event.OperationSucceeded, State.Stopped);
            s_fsm.addTransition(State.Stopping, Event.OperationFailed, State.Alert);

            s_fsm.addTransition(State.Stopped, Event.StartRequested, State.Starting);

            s_fsm.addTransition(State.Running, Event.FaultsDetected, State.Alert);

            s_fsm.addTransition(State.Running, Event.ScaleUpRequested, State.Scaling);
            s_fsm.addTransition(State.Running, Event.ScaleDownRequested, State.Scaling);
            s_fsm.addTransition(State.Scaling, Event.OperationSucceeded, State.Running);
            s_fsm.addTransition(State.Scaling, Event.OperationFailed, State.Alert);

            s_fsm.addTransition(State.Running, Event.UpgradeRequested, State.Upgrading);
            s_fsm.addTransition(State.Upgrading, Event.OperationSucceeded, State.Running);
            s_fsm.addTransition(State.Upgrading, Event.OperationFailed, State.Alert);

            s_fsm.addTransition(State.Alert, Event.RecoveryRequested, State.Recovering);
            s_fsm.addTransition(State.Recovering, Event.OperationSucceeded, State.Running);
            s_fsm.addTransition(State.Recovering, Event.OperationFailed, State.Alert);

            s_fsm.addTransition(State.Running, Event.DestroyRequested, State.Destroying);
            s_fsm.addTransition(State.Stopped, Event.DestroyRequested, State.Destroying);
            s_fsm.addTransition(State.Alert, Event.DestroyRequested, State.Destroying);
            s_fsm.addTransition(State.Error, Event.DestroyRequested, State.Destroying);

            s_fsm.addTransition(State.Destroying, Event.OperationSucceeded, State.Destroyed);

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
    long getControlNodeCount();
    long getNodeCount();
    long getTotalNodeCount();
    String getKeyPair();
    long getCores();
    long getMemory();
    long getNodeRootDiskSize();
    String getEndpoint();
    boolean isCheckForGc();
    @Override
    State getState();
    Date getCreated();
}
