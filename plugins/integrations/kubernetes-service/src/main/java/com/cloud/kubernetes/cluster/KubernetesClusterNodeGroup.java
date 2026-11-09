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

import org.apache.cloudstack.api.Identity;
import org.apache.cloudstack.api.InternalIdentity;

/**
 * KubernetesClusterNodeGroup describes a pool of worker nodes of a CloudManaged Kubernetes cluster.
 * All the nodes of a node group share the same service offering, template, Kubernetes node labels and taints,
 * and the size of a node group can be managed by the cluster autoscaler within its minimum and maximum size.
 * Every CloudManaged cluster has a default node group holding the worker nodes of the cluster size, which can not be
 * deleted and keeps at least one schedulable node for the system pods. External nodes do not belong to any node group.
 */
public interface KubernetesClusterNodeGroup extends InternalIdentity, Identity {

    /**
     * Label set on every node of a node group, its value is the name of the node group
     */
    String NODE_GROUP_LABEL = "cloudstack.apache.org/nodegroup";

    /**
     * Name of the default node group of a cluster
     */
    String DEFAULT_NODE_GROUP_NAME = "default";

    enum State {
        Ready("The node group is ready, its nodes can be used"),
        Scaling("Nodes are being added to or removed from the node group, or its service offering is being changed"),
        Updating("The labels, taints or autoscaling settings of the node group are being updated"),
        Deleting("The node group is being deleted"),
        Error("The last operation on the node group failed, it can be retried");

        private final String description;

        State(String description) {
            this.description = description;
        }

        public String getDescription() {
            return description;
        }

        /**
         * @return whether an operation is in progress on a node group in this state
         */
        public boolean isBusy() {
            return this == Scaling || this == Updating || this == Deleting;
        }
    }

    long getClusterId();

    String getName();

    boolean isDefault();

    State getState();

    long getServiceOfferingId();

    long getTemplateId();

    long getNodeCount();

    String getLabels();

    String getTaints();

    boolean isAutoscalingEnabled();

    Long getMinSize();

    Long getMaxSize();

    Date getCreated();

    Date getUpdated();

    Date getRemoved();
}
