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
package com.cloud.network.dao;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Table;

import org.apache.cloudstack.api.InternalIdentity;

@Entity
@Table(name = ("load_balancer_vm_map"))
public class LoadBalancerVMMapVO implements InternalIdentity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private long id;

    @Column(name = "load_balancer_id")
    private long loadBalancerId;

    @Column(name = "instance_id")
    private long instanceId;

    @Column(name = "instance_ip")
    private String instanceIp;

    @Column(name = "revoke")
    private boolean revoke = false;

    @Column(name = "state")
    private String state;

    public LoadBalancerVMMapVO() {
    }

    public LoadBalancerVMMapVO(long loadBalancerId, long instanceId) {
        this.loadBalancerId = loadBalancerId;
        this.instanceId = instanceId;
    }

    public LoadBalancerVMMapVO(long loadBalancerId, long instanceId, boolean revoke) {
        this.loadBalancerId = loadBalancerId;
        this.instanceId = instanceId;
        this.revoke = revoke;
    }

    public LoadBalancerVMMapVO(long loadBalancerId, long instanceId, String vmIp, boolean revoke) {
        this.loadBalancerId = loadBalancerId;
        this.instanceId = instanceId;
        this.instanceIp = vmIp;
        this.revoke = revoke;
    }

    @Override
    public long getId() {
        return id;
    }

    public long getLoadBalancerId() {
        return loadBalancerId;
    }

    public long getInstanceId() {
        return instanceId;
    }

    public boolean isRevoke() {
        return revoke;
    }

    public void setRevoke(boolean revoke) {
        this.revoke = revoke;
    }

    public String getState() {
        return state;
    }

    public void setState(String state) {
        this.state = state;
    }

    public String getInstanceIp() {
        return instanceIp;
    }

    public void setInstanceIp(String instanceIp) {
        this.instanceIp = instanceIp;
    }

}
