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

import com.cloud.serializer.Param;
import com.google.gson.annotations.SerializedName;
import org.apache.cloudstack.api.ApiConstants;
import org.apache.cloudstack.api.EntityReference;
import org.apache.cloudstack.gpu.GpuCard;
import org.apache.cloudstack.gpu.VgpuProfile;

@EntityReference(value = VgpuProfile.class)
public class VgpuProfileResponse extends GpuCardResponse {

    @SerializedName(ApiConstants.DESCRIPTION)
    @Param(description = "the description of the vGPU profile")
    private String description;

    @SerializedName(ApiConstants.GPU_CARD_ID)
    @Param(description = "the ID of the GPU card associated with this vGPU profile")
    private String gpuCardId;

    @SerializedName(ApiConstants.GPU_CARD_NAME)
    @Param(description = "the name of the vGPU profile")
    private String gpuCardName;

    @SerializedName(ApiConstants.MAX_VGPU_PER_PHYSICAL_GPU)
    @Param(description = "the maximum number of vGPUs per physical GPU")
    private Long maxVgpuPerPgpu;

    @SerializedName(ApiConstants.VIDEORAM)
    @Param(description = "the video RAM size in MB")
    private Long videoRam;

    @SerializedName(ApiConstants.MAXHEADS)
    @Param(description = "the maximum number of display heads")
    private Long maxHeads;

    @SerializedName(ApiConstants.MAXRESOLUTIONX)
    @Param(description = "the maximum X resolution")
    private Long maxResolutionX;

    @SerializedName(ApiConstants.MAXRESOLUTIONY)
    @Param(description = "the maximum Y resolution")
    private Long maxResolutionY;

    public VgpuProfileResponse(VgpuProfile vgpuProfile, GpuCard gpuCard) {
        super(gpuCard);
        id = vgpuProfile.getUuid();
        name = vgpuProfile.getName();
        description = vgpuProfile.getDescription();
        gpuCardId = gpuCard.getUuid();
        gpuCardName = gpuCard.getName();
        maxVgpuPerPgpu = vgpuProfile.getMaxVgpuPerPgpu();
        videoRam = vgpuProfile.getVideoRam();
        maxHeads = vgpuProfile.getMaxHeads();
        maxResolutionX = vgpuProfile.getMaxResolutionX();
        maxResolutionY = vgpuProfile.getMaxResolutionY();
        setObjectName("vgpuprofile");
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public String getGpuCardId() {
        return gpuCardId;
    }

    public String getGpuCardName() {
        return gpuCardName;
    }

    public Long getMaxVgpuPerPgpu() {
        return maxVgpuPerPgpu;
    }

    public void setDescription(String description) {
        this.description = description;
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
}
