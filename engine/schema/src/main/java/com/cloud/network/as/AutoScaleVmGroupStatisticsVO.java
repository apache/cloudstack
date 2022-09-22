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
package com.cloud.network.as;

import java.util.Date;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Table;
import javax.persistence.Temporal;
import javax.persistence.TemporalType;

import com.cloud.network.router.VirtualRouterAutoScale;
import com.cloud.server.ResourceTag;
import org.apache.cloudstack.api.InternalIdentity;

@Entity
@Table(name = "autoscale_vmgroup_statistics")
public class AutoScaleVmGroupStatisticsVO implements InternalIdentity {

    public enum State {
        ACTIVE, INACTIVE
    }

    public static final double INVALID_VALUE = -1;
    public static final long DUMMY_ID = 0L;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private long id;

    @Column(name = "vmgroup_id")
    private long vmGroupId;

    @Column(name = "policy_id")
    private long policyId;

    @Column(name = "counter_id")
    private long counterId;

    @Column(name = "resource_id")
    private Long resourceId;

    @Column(name = "resource_type")
    @Enumerated(value = EnumType.STRING)
    private ResourceTag.ResourceObjectType resourceType;

    @Column(name = "raw_value")
    private Double rawValue = null;

    @Column(name = "value_type")
    @Enumerated(value = EnumType.STRING)
    private VirtualRouterAutoScale.AutoScaleValueType valueType;

    @Column(name = "created")
    @Temporal(value = TemporalType.TIMESTAMP)
    private Date created = null;

    @Column(name = "state")
    @Enumerated(EnumType.STRING)
    State state;

    public AutoScaleVmGroupStatisticsVO() {
    }

    public AutoScaleVmGroupStatisticsVO(long vmGroupId, long policyId, long counterId, Long resourceId, ResourceTag.ResourceObjectType resourceType,
                                        Double rawValue, VirtualRouterAutoScale.AutoScaleValueType valueType, Date created) {
        this.vmGroupId = vmGroupId;
        this.policyId = policyId;
        this.counterId = counterId;
        this.resourceId = resourceId;
        this.resourceType = resourceType;
        this.rawValue = rawValue;
        this.valueType = valueType;
        this.created = created;
        this.state = State.ACTIVE;
    }

    public AutoScaleVmGroupStatisticsVO(long vmGroupId, long policyId, long counterId, Long resourceId, ResourceTag.ResourceObjectType resourceType,
                                        VirtualRouterAutoScale.AutoScaleValueType valueType, Date created) {
        this.vmGroupId = vmGroupId;
        this.policyId = policyId;
        this.counterId = counterId;
        this.resourceId = resourceId;
        this.resourceType = resourceType;
        this.rawValue = INVALID_VALUE;
        this.valueType = valueType;
        this.created = created;
        this.state = State.INACTIVE;
    }

    public AutoScaleVmGroupStatisticsVO(long vmGroupId) {
        this.vmGroupId = vmGroupId;
        this.policyId = DUMMY_ID;
        this.counterId = DUMMY_ID;
        this.resourceId = vmGroupId;
        this.resourceType = ResourceTag.ResourceObjectType.AutoScaleVmGroup;
        this.rawValue = INVALID_VALUE;
        this.valueType = VirtualRouterAutoScale.AutoScaleValueType.INSTANT_VM_GROUP;
        this.created = new Date();
        this.state = State.INACTIVE;
    }

    @Override
    public long getId() {
        return id;
    }

    public long getVmGroupId() {
        return vmGroupId;
    }

    public long getPolicyId() {
        return policyId;
    }

    public long getCounterId() {
        return counterId;
    }

    public Long getResourceId() {
        return resourceId;
    }

    public ResourceTag.ResourceObjectType getResourceType() {
        return resourceType;
    }

    public Double getRawValue() {
        return rawValue;
    }

    public VirtualRouterAutoScale.AutoScaleValueType getValueType() {
        return valueType;
    }

    public Date getCreated() {
        return created;
    }

    public State getState() {
        return state;
    }

    public void setState(State state) {
        this.state = state;
    }
}
