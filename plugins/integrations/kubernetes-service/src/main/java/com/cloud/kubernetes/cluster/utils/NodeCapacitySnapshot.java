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
package com.cloud.kubernetes.cluster.utils;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import org.apache.commons.lang3.StringUtils;

/** The parts of a Kubernetes Node object that a live resize reads. */
public final class NodeCapacitySnapshot {
    /** Set while a node has resize work that has not been verified yet; the value is the cluster UUID. */
    public static final String PENDING_ANNOTATION = "cloudstack.apache.org/cks-live-resize-pending";
    /** Set when CKS, not an operator, cordoned the node for a resize; the value is the cluster UUID. */
    public static final String CORDON_ANNOTATION = "cloudstack.apache.org/cks-live-resize";

    private final String uid;
    private final String resourceVersion;
    private final boolean unschedulable;
    private final Map<String, String> annotations;
    private final boolean ready;
    private final NodeResources capacity;
    private final NodeResources allocatable;

    public NodeCapacitySnapshot(String uid,
                                String resourceVersion,
                                boolean unschedulable,
                                Map<String, String> annotations,
                                boolean ready,
                                NodeResources capacity,
                                NodeResources allocatable) {
        this.uid = uid;
        this.resourceVersion = resourceVersion;
        this.unschedulable = unschedulable;
        this.annotations = Collections.unmodifiableMap(new HashMap<>(annotations));
        this.ready = ready;
        this.capacity = capacity;
        this.allocatable = allocatable;
    }

    public String getUid() {
        return uid;
    }

    public String getResourceVersion() {
        return resourceVersion;
    }

    public boolean isUnschedulable() {
        return unschedulable;
    }

    public boolean isReady() {
        return ready;
    }

    public NodeResources getCapacity() {
        return capacity;
    }

    public NodeResources getAllocatable() {
        return allocatable;
    }

    public boolean isPendingFor(String clusterUuid) {
        return clusterUuid.equals(annotations.get(PENDING_ANNOTATION));
    }

    /** Whether CKS cordoned the node: it is unschedulable and carries this cluster's ownership marker. */
    public boolean isCordonOwnedBy(String clusterUuid) {
        return unschedulable && hasOwnershipMarker(clusterUuid);
    }

    /**
     * Whether this cluster's ownership marker is set. kubectl uncordon keeps annotations, so the marker can
     * be left on a node somebody has uncordoned since; it then no longer means that CKS owns a cordon.
     */
    public boolean hasOwnershipMarker(String clusterUuid) {
        return clusterUuid.equals(annotations.get(CORDON_ANNOTATION));
    }

    /** Whether a resize marker carries another cluster's UUID; such markers are never changed. */
    public boolean hasForeignMarker(String clusterUuid) {
        return isForeign(annotations.get(PENDING_ANNOTATION), clusterUuid)
                || isForeign(annotations.get(CORDON_ANNOTATION), clusterUuid);
    }

    private static boolean isForeign(String owner, String clusterUuid) {
        return StringUtils.isNotEmpty(owner) && !owner.equals(clusterUuid);
    }
}
