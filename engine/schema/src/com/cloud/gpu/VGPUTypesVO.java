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
package com.cloud.gpu;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Table;

import org.apache.cloudstack.api.InternalIdentity;

@Entity
@Table(name="vgpu_types")
public class VGPUTypesVO implements InternalIdentity {
    @Id
    @GeneratedValue(strategy=GenerationType.IDENTITY)
    @Column(name="id")
    private long id;

    @Column(name="vgpu_type")
    private String vgpuType;

    @Column(name="gpu_group_id")
    private long gpuGroupId;

    @Column(name="remaining_vm_capacity")
    private long remainingCapacity;

    protected VGPUTypesVO() {
    }

    public VGPUTypesVO(String vgpuType, long gpuGroupId, long remainingCapacity) {
        this.vgpuType = vgpuType;
        this.gpuGroupId = gpuGroupId;
        this.remainingCapacity = remainingCapacity;
    }

    public String getVgpuType() {
        return vgpuType;
    }

    public void setVgpuType(String vgpuType) {
        this.vgpuType = vgpuType;
    }

    public long getGpuGroupId() {
        return gpuGroupId;
    }

    public void setGpuGroupId(long gpuGroupId) {
        this.gpuGroupId = gpuGroupId;
    }

    public long getRemainingCapacity() {
        return remainingCapacity;
    }

    public void setRemainingCapacity(long remainingCapacity) {
        this.remainingCapacity = remainingCapacity;
    }

    @Override
    public long getId() {
        return id;
    }
}
