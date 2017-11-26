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
package com.cloud.agent.api;
public class VgpuTypesInfo {

    private String groupName;
    private String modelName;
    private Long videoRam;
    private Long maxHeads;
    private Long maxResolutionX;
    private Long maxResolutionY;
    private Long maxVgpuPerGpu;
    private Long remainingCapacity;
    private Long maxCapacity;

    public VgpuTypesInfo(String groupName, String modelName, Long videoRam, Long maxHeads, Long maxResolutionX, Long maxResolutionY, Long maxVgpuPerGpu, Long remainingCapacity, Long maxCapacity) {
        this.groupName = groupName;
        this.modelName = modelName;
        this.videoRam = videoRam;
        this.maxHeads = maxHeads;
        this.maxResolutionX = maxResolutionX;
        this.maxResolutionY = maxResolutionY;
        this.maxVgpuPerGpu = maxVgpuPerGpu;
        this.remainingCapacity = remainingCapacity;
        this.maxCapacity = maxCapacity;
    }

    public VgpuTypesInfo() {
        this.groupName = null;
        this.modelName = null;
        this.videoRam = null;
        this.maxHeads = null;
        this.maxResolutionX = null;
        this.maxResolutionY = null;
        this.maxVgpuPerGpu = null;
        this.remainingCapacity = null;
        this.maxCapacity = null;
    }

    public String getGroupName() {
        return groupName;
    }

    public void setGroupName(final String groupName) {
        this.groupName = groupName;
    }

    public String getModelName() {
        return modelName;
    }

    public Long getVideoRam() {
        return videoRam;
    }

    public Long getMaxHeads() {
        return maxHeads;
    }

    public Long getMaxResolutionX() {
        return maxResolutionX;
    }

    public Long getMaxResolutionY() {
        return maxResolutionY;
    }

    public Long getMaxVgpuPerGpu() {
        return maxVgpuPerGpu;
    }

    public Long getRemainingCapacity() {
        return remainingCapacity;
    }

    public void setRemainingCapacity(final Long remainingCapacity) {
        this.remainingCapacity = remainingCapacity;
    }

    public Long getMaxCapacity() {
        return maxCapacity;
    }

    public void setMaxCapacity(final Long maxCapacity) {
        this.maxCapacity = maxCapacity;
    }
}
