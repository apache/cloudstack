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
package com.cloud.vm;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.Id;
import javax.persistence.Table;

import com.cloud.utils.time.InaccurateClock;
import com.cloud.vm.VirtualMachine.State;

@Entity
@Table(name = "op_it_work")
public class ItWorkVO {
    enum ResourceType {
        Volume, Nic, Host
    }

    enum Step {
        Prepare, Starting, Started, Release, Done, Migrating, Reconfiguring
    }

    @Id
    @Column(name = "id")
    String id;

    @Column(name = "created_at")
    long createdAt;

    @Column(name = "mgmt_server_id")
    long managementServerId;

    @Column(name = "type")
    State type;

    @Column(name = "thread")
    String threadName;

    @Column(name = "step")
    Step step;

    @Column(name = "updated_at")
    long updatedAt;

    @Column(name = "instance_id")
    long instanceId;

    public long getInstanceId() {
        return instanceId;
    }

    @Column(name = "resource_id")
    long resourceId;

    @Column(name = "resource_type")
    ResourceType resourceType;

    @Column(name = "vm_type")
    @Enumerated(value = EnumType.STRING)
    VirtualMachine.Type vmType;

    public VirtualMachine.Type getVmType() {
        return vmType;
    }

    public long getResourceId() {
        return resourceId;
    }

    public void setResourceId(long resourceId) {
        this.resourceId = resourceId;
    }

    public ResourceType getResourceType() {
        return resourceType;
    }

    public void setResourceType(ResourceType resourceType) {
        this.resourceType = resourceType;
    }

    protected ItWorkVO() {
    }

    protected ItWorkVO(String id, long managementServerId, State type, VirtualMachine.Type vmType, long instanceId) {
        this.id = id;
        this.managementServerId = managementServerId;
        this.type = type;
        this.threadName = Thread.currentThread().getName();
        this.step = Step.Prepare;
        this.instanceId = instanceId;
        this.resourceType = null;
        this.createdAt = InaccurateClock.getTimeInSeconds();
        this.updatedAt = createdAt;
        this.vmType = vmType;
    }

    public String getId() {
        return id;
    }

    public Long getCreatedAt() {
        return createdAt;
    }

    public long getManagementServerId() {
        return managementServerId;
    }

    public void setManagementServerId(long managementServerId) {
        this.managementServerId = managementServerId;
    }

    public State getType() {
        return type;
    }

    public void setType(State type) {
        this.type = type;
    }

    public String getThreadName() {
        return threadName;
    }

    public Step getStep() {
        return step;
    }

    public void setStep(Step step) {
        this.step = step;
    }

    public long getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(long updatedAt) {
        this.updatedAt = updatedAt;
    }

    public long getSecondsTaskIsInactive() {
        return InaccurateClock.getTimeInSeconds() - this.updatedAt;
    }

    public long getSecondsTaskHasBeenCreated() {
        return InaccurateClock.getTimeInSeconds() - this.createdAt;
    }

    @Override
    public String toString() {
        return new StringBuilder("ItWork[").append(id)
            .append("-")
            .append(type.toString())
            .append("-")
            .append(instanceId)
            .append("-")
            .append(step.toString())
            .append("]")
            .toString();
    }
}
