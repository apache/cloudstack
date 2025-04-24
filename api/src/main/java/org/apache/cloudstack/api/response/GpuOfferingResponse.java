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
import org.apache.cloudstack.api.BaseResponseWithAnnotations;
import org.apache.cloudstack.api.EntityReference;
import org.apache.cloudstack.gpu.GpuOffering;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

@EntityReference(value = GpuOffering.class)
public class GpuOfferingResponse extends BaseResponseWithAnnotations {
    @SerializedName(ApiConstants.ID)
    @Param(description = "the ID of the GPU offering")
    private String id;

    @SerializedName(ApiConstants.NAME)
    @Param(description = "the name of the GPU offering")
    private String name;

    @SerializedName(ApiConstants.DESCRIPTION)
    @Param(description = "the description of the GPU offering")
    private String description;

    @SerializedName(ApiConstants.CREATED)
    @Param(description = "the date this GPU offering was created")
    private Date created;

    @SerializedName("state")
    @Param(description = "indicates if the GPU offering is enabled")
    private GpuOffering.State state;

    @SerializedName("vgpuprofiles")
    @Param(description = "the list of vGPU profiles included in this offering", responseObject = VgpuProfileResponse.class)
    private List<VgpuProfileResponse> vgpuProfiles;

    public GpuOfferingResponse() {
    }

    public GpuOfferingResponse(GpuOffering gpuOffering) {
        setObjectName("gpuoffering");
        this.id = gpuOffering.getUuid();
        this.name = gpuOffering.getName();
        this.description = gpuOffering.getDescription();
        this.created = gpuOffering.getCreated();
        this.state = gpuOffering.getState();
        this.vgpuProfiles = new ArrayList<>();
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
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

    public void setDescription(String description) {
        this.description = description;
    }

    public Date getCreated() {
        return created;
    }

    public void setCreated(Date created) {
        this.created = created;
    }

    public GpuOffering.State getState() {
        return state;
    }

    public void setState(GpuOffering.State state) {
        this.state = state;
    }

    public List<VgpuProfileResponse> getVgpuProfiles() {
        return vgpuProfiles;
    }

    public void setVgpuProfiles(List<VgpuProfileResponse> vgpuProfiles) {
        this.vgpuProfiles = vgpuProfiles;
    }
}
