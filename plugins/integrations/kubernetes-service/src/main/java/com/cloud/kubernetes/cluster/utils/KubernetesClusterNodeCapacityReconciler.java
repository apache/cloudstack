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

import java.io.File;

import com.cloud.kubernetes.cluster.KubernetesCluster;
import com.cloud.kubernetes.cluster.KubernetesServiceHelper.KubernetesClusterNodeType;
import com.cloud.offering.ServiceOffering;
import com.cloud.uservm.UserVm;

/** Reconciles the guest and Kubernetes resource views after a live VM resize. */
public interface KubernetesClusterNodeCapacityReconciler {
    NodeCapacitySnapshot captureBefore(KubernetesCluster cluster, UserVm vm, NodeAccess access) throws Exception;
    boolean requiresKubeletRefresh(ServiceOffering oldOffering, ServiceOffering newOffering,
            KubernetesClusterNodeType nodeType, KubernetesCluster.State originalState);
    void cordonIfNeeded(KubernetesCluster cluster, UserVm vm, NodeCapacitySnapshot before, NodeAccess access, long deadline) throws Exception;
    void verifyGuestResources(UserVm vm, ServiceOffering target, NodeCapacitySnapshot before, NodeAccess access, long deadline) throws Exception;
    boolean isKubernetesResourcesCurrent(NodeCapacitySnapshot snapshot, ServiceOffering target);
    void restartKubelet(UserVm vm, NodeAccess access, long deadline) throws Exception;
    NodeCapacitySnapshot waitForKubernetesResources(KubernetesCluster cluster, UserVm vm, ServiceOffering target,
            NodeCapacitySnapshot before, NodeAccess access, long deadline) throws Exception;
    void restoreSchedulability(KubernetesCluster cluster, UserVm vm, NodeCapacitySnapshot before, NodeAccess access, long deadline) throws Exception;

    final class NodeAccess {
        private final String controlAddress; private final int controlPort; private final String nodeAddress; private final int nodePort;
        private final String user; private final File sshKeyFile;
        public NodeAccess(String controlAddress, int controlPort, String nodeAddress, int nodePort, String user, File sshKeyFile) {
            this.controlAddress = controlAddress; this.controlPort = controlPort; this.nodeAddress = nodeAddress; this.nodePort = nodePort;
            this.user = user; this.sshKeyFile = sshKeyFile;
        }
        public String getControlAddress() { return controlAddress; } public int getControlPort() { return controlPort; }
        public String getNodeAddress() { return nodeAddress; } public int getNodePort() { return nodePort; }
        public String getUser() { return user; } public File getSshKeyFile() { return sshKeyFile; }
    }

    final class NodeCapacitySnapshot {
        private final boolean unschedulable; private final boolean cloudStackResizeCordon; private final long guestOnlineCpuCount;
        private final long guestMemoryKiB; private final long capacityCpuMillis; private final long capacityMemoryBytes;
        private final long allocatableCpuMillis; private final long allocatableMemoryBytes; private final boolean ready;
        public NodeCapacitySnapshot(boolean unschedulable, boolean cloudStackResizeCordon, long guestOnlineCpuCount, long guestMemoryKiB,
                long capacityCpuMillis, long capacityMemoryBytes, long allocatableCpuMillis, long allocatableMemoryBytes, boolean ready) {
            this.unschedulable = unschedulable; this.cloudStackResizeCordon = cloudStackResizeCordon; this.guestOnlineCpuCount = guestOnlineCpuCount;
            this.guestMemoryKiB = guestMemoryKiB; this.capacityCpuMillis = capacityCpuMillis; this.capacityMemoryBytes = capacityMemoryBytes;
            this.allocatableCpuMillis = allocatableCpuMillis; this.allocatableMemoryBytes = allocatableMemoryBytes; this.ready = ready;
        }
        public boolean isUnschedulable() { return unschedulable; } public boolean isCloudStackResizeCordon() { return cloudStackResizeCordon; }
        public long getGuestOnlineCpuCount() { return guestOnlineCpuCount; } public long getGuestMemoryKiB() { return guestMemoryKiB; }
        public long getCapacityCpuMillis() { return capacityCpuMillis; } public long getCapacityMemoryBytes() { return capacityMemoryBytes; }
        public long getAllocatableCpuMillis() { return allocatableCpuMillis; } public long getAllocatableMemoryBytes() { return allocatableMemoryBytes; }
        public boolean isReady() { return ready; }
    }
}
