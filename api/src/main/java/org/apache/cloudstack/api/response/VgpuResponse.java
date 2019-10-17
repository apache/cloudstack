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
package org.apache.cloudstack.api.response;

import com.google.gson.annotations.SerializedName;

import org.apache.cloudstack.api.ApiConstants;
import org.apache.cloudstack.api.BaseResponse;

import com.cloud.serializer.Param;

public class VgpuResponse extends BaseResponse {

    @SerializedName(ApiConstants.VGPUTYPE)
    @Param(description = "Model Name of vGPU")
    private String name;

    @SerializedName(ApiConstants.VIDEORAM)
    @Param(description = "Video RAM for this vGPU type")
    private Long videoRam;

    @SerializedName(ApiConstants.MAXHEADS)
    @Param(description = "Maximum displays per user")
    private Long maxHeads;

    @SerializedName(ApiConstants.MAXRESOLUTIONX)
    @Param(description = "Maximum X resolution per display")
    private Long maxResolutionX;

    @SerializedName(ApiConstants.MAXRESOLUTIONY)
    @Param(description = "Maximum Y resolution per display")
    private Long maxResolutionY;

    @SerializedName(ApiConstants.MAXVGPUPERPGPU)
    @Param(description = "Maximum no. of vgpu per gpu card (pgpu)")
    private Long maxVgpuPerPgpu;

    @SerializedName(ApiConstants.REMAININGCAPACITY)
    @Param(description = "Remaining capacity in terms of no. of more VMs that can be deployped with this vGPU type")
    private Long remainingCapacity;

    @SerializedName(ApiConstants.MAXCAPACITY)
    @Param(description = "Maximum vgpu can be created with this vgpu type on the given gpu group")
    private Long maxCapacity;

    public void setName(String name) {
        this.name = name;
    }

    public void setVideoRam(Long videoRam) {
        this.videoRam = videoRam;
    }

    public void setMaxHeads(Long maxHeads) {
        this.maxHeads = maxHeads;
    }

    public void setMaxResolutionX(Long maxResolutionX) {
        this.maxResolutionX = maxResolutionX;
    }

    public void setMaxResolutionY(Long maxResolutionY) {
        this.maxResolutionY = maxResolutionY;
    }

    public void setMaxVgpuPerPgpu(Long maxVgpuPerPgpu) {
        this.maxVgpuPerPgpu = maxVgpuPerPgpu;
    }

    public void setRemainingCapacity(Long remainingCapacity) {
        this.remainingCapacity = remainingCapacity;
    }

    public void setmaxCapacity(Long maxCapacity) {
        this.maxCapacity = maxCapacity;
    }
}
