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
package com.cloud.network.ovs.dao;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Table;

import org.apache.cloudstack.api.InternalIdentity;

@Entity
@Table(name = "op_vpc_distributed_router_sequence_no")
public class VpcDistributedRouterSeqNoVO implements InternalIdentity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @Column(name = "vpc_id", updatable = false, nullable = false)
    private Long vpcId;

    @Column(name = "topology_update_sequence_no")
    long topologyUpdateSequenceNo = 0;

    @Column(name = "routing_policy__update_sequence_no")
    long policyUpdateSequenceNo = 0;

    protected VpcDistributedRouterSeqNoVO() {

    }

    public VpcDistributedRouterSeqNoVO(Long vpcId) {
        super();
        this.vpcId = vpcId;
    }

    @Override
    public long getId() {
        return id;
    }

    public Long getVpcId() {
        return vpcId;
    }

    public void setVpcId(Long vpcId) {
        this.vpcId = vpcId;
    }

    public long getTopologyUpdateSequenceNo() {
        return topologyUpdateSequenceNo;
    }

    public void incrTopologyUpdateSequenceNo() {
        topologyUpdateSequenceNo++;
    }

    public long getPolicyUpdateSequenceNo() {
        return policyUpdateSequenceNo;
    }

    public void incrPolicyUpdateSequenceNo() {
        policyUpdateSequenceNo++;
    }
}
