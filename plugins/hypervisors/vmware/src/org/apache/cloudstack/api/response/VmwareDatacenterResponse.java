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
import org.apache.cloudstack.api.EntityReference;

import com.cloud.hypervisor.vmware.VmwareDatacenter;
import com.cloud.serializer.Param;

@EntityReference(value = VmwareDatacenter.class)
public class VmwareDatacenterResponse extends BaseResponse {
    @SerializedName(ApiConstants.ID) @Param(description="The VMware Datacenter ID")
    private String id;

    @SerializedName(ApiConstants.ZONE_ID)
    @Param(description = "the Zone ID associated with this VMware Datacenter")
    private Long zoneId;

    @SerializedName(ApiConstants.NAME) @Param(description="The VMware Datacenter name")
    private String name;

    @SerializedName(ApiConstants.VCENTER)
    @Param(description = "The VMware vCenter name/ip")
    private String vCenter;

    public String getName() {
        return name;
    }

    public String getVcenter() {
        return vCenter;
    }

    public String getId() {
        return id;
    }

    public Long getZoneId() {
        return zoneId;
    }

    public void setZoneId(Long zoneId) {
        this.zoneId = zoneId;
    }

    public void setName(String name) {
        this.name = name;
    }

    public void setVcenter(String vCenter) {
        this.vCenter = vCenter;
    }

    public void setId(String id) {
        this.id = id;
    }
}
