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
@Table(name = "vgpu_types")
public class VGPUTypesVO implements InternalIdentity {
    @Id
    @GeneratedValue(strategy=GenerationType.IDENTITY)
    @Column(name="id")
    private long id;

    @Column(name="gpu_group_id")
    private long gpuGroupId;

    @Column(name="vgpu_type")
    private String vgpuType;

    @Column(name="video_ram")
    private Long videoRam;

    @Column(name="max_heads")
    private Long maxHeads;

    @Column(name="max_resolution_x")
    private Long maxResolutionX;

    @Column(name="max_resolution_y")
    private Long maxResolutionY;

    @Column(name="max_vgpu_per_pgpu")
    private Long maxVgpuPerPgpu;

    @Column(name="remaining_capacity")
    private long remainingCapacity;

    @Column(name="max_capacity")
    private long maxCapacity;

    protected VGPUTypesVO() {
    }

    public VGPUTypesVO(long gpuGroupId, String vgpuType, Long videoRam, Long maxHeads, Long maxResolutionX, Long maxResolutionY, Long maxVgpuPerPgpu,
            long remainingCapacity, long maxCapacity) {
        this.gpuGroupId = gpuGroupId;
        this.vgpuType = vgpuType;
        this.videoRam = videoRam;
        this.maxHeads = maxHeads;
        this.maxResolutionX = maxResolutionX;
        this.maxResolutionY = maxResolutionY;
        this.maxVgpuPerPgpu = maxVgpuPerPgpu;
        this.remainingCapacity = remainingCapacity;
        this.maxCapacity = maxCapacity;
    }

    public long getGpuGroupId() {
        return gpuGroupId;
    }

    public void setGpuGroupId(long gpuGroupId) {
        this.gpuGroupId = gpuGroupId;
    }

    public String getVgpuType() {
        return vgpuType;
    }

    public void setVgpuType(String vgpuType) {
        this.vgpuType = vgpuType;
    }

    public Long getVideoRam() {
        return videoRam;
    }

    public void setVideoRam(Long videoRam) {
        this.videoRam = videoRam;
    }

    public Long getMaxHeads() {
        return maxHeads;
    }

    public void setMaxHeads(Long maxHeads) {
        this.maxHeads = maxHeads;
    }

    public Long getMaxResolutionX() {
        return maxResolutionX;
    }

    public void setMaxResolutionX(Long maxResolutionX) {
        this.maxResolutionX = maxResolutionX;
    }

    public Long getMaxResolutionY() {
        return maxResolutionY;
    }

    public void setMaxResolutionY(Long maxResolutionY) {
        this.maxResolutionY = maxResolutionY;
    }

    public Long getMaxVgpuPerPgpu() {
        return maxVgpuPerPgpu;
    }

    public void setMaxVgpuPerPgpu(Long maxVgpuPerPgpu) {
        this.maxVgpuPerPgpu = maxVgpuPerPgpu;
    }

    public long getRemainingCapacity() {
        return remainingCapacity;
    }

    public void setRemainingCapacity(long remainingCapacity) {
        this.remainingCapacity = remainingCapacity;
    }

    public long getMaxCapacity() {
        return maxCapacity;
    }

    public void setMaxCapacity(long maxCapacity) {
        this.maxCapacity = maxCapacity;
    }

    @Override
    public long getId() {
        return id;
    }
}
