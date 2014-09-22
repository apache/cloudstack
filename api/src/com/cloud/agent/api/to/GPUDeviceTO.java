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
package com.cloud.agent.api.to;

import java.util.HashMap;

import com.cloud.agent.api.VgpuTypesInfo;

public class GPUDeviceTO {

    private String gpuGroup;
    private String vgpuType;
    private HashMap<String, HashMap<String, VgpuTypesInfo>> groupDetails = new HashMap<String, HashMap<String, VgpuTypesInfo>>();

    public GPUDeviceTO( String gpuGroup, String vgpuType, HashMap<String, HashMap<String, VgpuTypesInfo>> groupDetails) {
        this.gpuGroup = gpuGroup;
        this.vgpuType = vgpuType;
        this.groupDetails = groupDetails;
    }

    public String getGpuGroup() {
        return gpuGroup;
    }

    public String getVgpuType() {
        return vgpuType;
    }

    public void setGpuGroup(String gpuGroup) {
        this.gpuGroup = gpuGroup;
    }

    public void setVgpuType(String vgpuType) {
        this.vgpuType = vgpuType;
    }

    public HashMap<String, HashMap<String, VgpuTypesInfo>> getGroupDetails() {
        return groupDetails;
    }

    public void setGroupDetails(HashMap<String, HashMap<String, VgpuTypesInfo>> groupDetails) {
        this.groupDetails = groupDetails;
    }

}
