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
package com.cloud.dc;

import java.util.UUID;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Table;

import org.apache.cloudstack.api.Identity;
import org.apache.cloudstack.api.InternalIdentity;

@Entity
@Table(name = "cluster_physical_network_traffic_info")
public class ClusterPhysicalNetworkTrafficInfoVO implements InternalIdentity, Identity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private long id;

    @Column(name = "uuid")
    private String uuid;

    @Column(name = "cluster_id")
    private long clusterId;

    @Column(name = "physical_network_traffic_id")
    private long physicalNetworkTrafficId;

    @Column(name = "vmware_network_label")
    private String vmwareNetworkLabel;

    public ClusterPhysicalNetworkTrafficInfoVO() {
    }

    public ClusterPhysicalNetworkTrafficInfoVO(long clusterId, long physicalNetworkTrafficId, String vmwareLabel) {
        this.clusterId = clusterId;
        this.physicalNetworkTrafficId = physicalNetworkTrafficId;
        this.vmwareNetworkLabel = vmwareLabel;
        this.uuid = UUID.randomUUID().toString();
    }

    @Override
    public long getId() {
        return id;
    }

    public long getClusterId() {
        return clusterId;
    }

    public long getPhysicalNetworkTrafficId() {
        return physicalNetworkTrafficId;
    }

    public void setVmwareNetworkLabel(String vmwareNetworkLabel) {
        this.vmwareNetworkLabel = vmwareNetworkLabel;
    }

    public String getVmwareNetworkLabel() {
        return vmwareNetworkLabel;
    }

    @Override
    public String getUuid() {
        return this.uuid;
    }

    public void setUuid(String uuid) {
        this.uuid = uuid;
    }
}