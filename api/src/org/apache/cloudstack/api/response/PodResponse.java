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

import java.util.List;

import org.apache.cloudstack.api.ApiConstants;
import org.apache.cloudstack.api.BaseResponse;
import org.apache.cloudstack.api.EntityReference;

import com.cloud.dc.Pod;
import com.cloud.serializer.Param;
import com.google.gson.annotations.SerializedName;

@EntityReference(value = Pod.class)
public class PodResponse extends BaseResponse {
    @SerializedName("id") @Param(description="the ID of the Pod")
    private String id;

    @SerializedName("name") @Param(description="the name of the Pod")
    private String name;

    @SerializedName("zoneid") @Param(description="the Zone ID of the Pod")
    private String zoneId;

    @SerializedName(ApiConstants.ZONE_NAME) @Param(description="the Zone name of the Pod")
    private String zoneName;

    @SerializedName("gateway") @Param(description="the gateway of the Pod")
    private String gateway;

    @SerializedName("netmask") @Param(description="the netmask of the Pod")
    private String netmask;

    @SerializedName("startip") @Param(description="the starting IP for the Pod")
    private String startIp;

    @SerializedName("endip") @Param(description="the ending IP for the Pod")
    private String endIp;

    @SerializedName("allocationstate") @Param(description="the allocation state of the Pod")
    private String allocationState;

    @SerializedName("capacity")  @Param(description="the capacity of the Pod", responseObject = CapacityResponse.class)
    private List<CapacityResponse> capacitites;

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

    public String getZoneId() {
        return zoneId;
    }

    public void setZoneId(String zoneId) {
        this.zoneId = zoneId;
    }

    public String getZoneName() {
        return zoneName;
    }

    public void setZoneName(String zoneName) {
        this.zoneName = zoneName;
    }

    public String getGateway() {
        return gateway;
    }

    public void setGateway(String gateway) {
        this.gateway = gateway;
    }

    public String getNetmask() {
        return netmask;
    }

    public void setNetmask(String netmask) {
        this.netmask = netmask;
    }

    public String getStartIp() {
        return startIp;
    }

    public void setStartIp(String startIp) {
        this.startIp = startIp;
    }

    public String getEndIp() {
        return endIp;
    }

    public void setEndIp(String endIp) {
        this.endIp = endIp;
    }

    public String getAllocationState() {
        return allocationState;
    }

    public void setAllocationState(String allocationState) {
        this.allocationState = allocationState;
    }

    public List<CapacityResponse> getCapacitites() {
        return capacitites;
    }

    public void setCapacitites(List<CapacityResponse> capacitites) {
        this.capacitites = capacitites;
    }
}
