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

import javax.persistence.Column;

import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;

@Entity
@Table(name = "kubernetes_cluster_vm_map")
public class KubernetesClusterVmMapVO implements KubernetesClusterVmMap {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    long id;

    @Column(name = "cluster_id")
    long clusterId;

    @Column(name = "vm_id")
    long vmId;

    @Column(name = "control_node")
    boolean controlNode;

    public KubernetesClusterVmMapVO() {
    }

    public KubernetesClusterVmMapVO(long clusterId, long vmId, boolean controlNode) {
        this.vmId = vmId;
        this.clusterId = clusterId;
        this.controlNode = controlNode;
    }


    @Override
    public long getId() {
        return id;
    }

    @Override
    public long getClusterId() {
        return clusterId;
    }

    public void setClusterId(long clusterId) {
        this.clusterId = clusterId;
    }

    @Override
    public long getVmId() {
        return vmId;
    }

    public void setVmId(long vmId) {
        this.vmId = vmId;
    }

    @Override
    public boolean isControlNode() {
        return controlNode;
    }

    public void setControlNode(boolean controlNode) {
        this.controlNode = controlNode;
    }
}
