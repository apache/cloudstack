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

public class KubernetesClusterEventTypes {
    public static final String EVENT_KUBERNETES_CLUSTER_CREATE = "KUBERNETES.CLUSTER.CREATE";
    public static final String EVENT_KUBERNETES_CLUSTER_DELETE = "KUBERNETES.CLUSTER.DELETE";
    public static final String EVENT_KUBERNETES_CLUSTER_START = "KUBERNETES.CLUSTER.START";
    public static final String EVENT_KUBERNETES_CLUSTER_STOP = "KUBERNETES.CLUSTER.STOP";
    public static final String EVENT_KUBERNETES_CLUSTER_SCALE = "KUBERNETES.CLUSTER.SCALE";
    public static final String EVENT_KUBERNETES_CLUSTER_UPGRADE = "KUBERNETES.CLUSTER.UPGRADE";
    public static final String EVENT_KUBERNETES_CLUSTER_NODES_ADD = "KUBERNETES.CLUSTER.NODES.ADD";
    public static final String EVENT_KUBERNETES_CLUSTER_NODES_REMOVE = "KUBERNETES.CLUSTER.NODES.REMOVE";
}
