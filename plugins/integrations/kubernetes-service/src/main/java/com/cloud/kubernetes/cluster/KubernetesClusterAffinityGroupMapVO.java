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
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Table;

import org.apache.cloudstack.api.InternalIdentity;

@Entity
@Table(name = "kubernetes_cluster_affinity_group_map")
public class KubernetesClusterAffinityGroupMapVO implements InternalIdentity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @Column(name = "cluster_id")
    private long clusterId;

    @Column(name = "node_type")
    private String nodeType;

    @Column(name = "affinity_group_id")
    private long affinityGroupId;

    public KubernetesClusterAffinityGroupMapVO() {
    }

    public KubernetesClusterAffinityGroupMapVO(long clusterId, String nodeType, long affinityGroupId) {
        this.clusterId = clusterId;
        this.nodeType = nodeType;
        this.affinityGroupId = affinityGroupId;
    }

    @Override
    public long getId() {
        return id;
    }

    public long getClusterId() {
        return clusterId;
    }

    public void setClusterId(long clusterId) {
        this.clusterId = clusterId;
    }

    public String getNodeType() {
        return nodeType;
    }

    public void setNodeType(String nodeType) {
        this.nodeType = nodeType;
    }

    public long getAffinityGroupId() {
        return affinityGroupId;
    }

    public void setAffinityGroupId(long affinityGroupId) {
        this.affinityGroupId = affinityGroupId;
    }
}
