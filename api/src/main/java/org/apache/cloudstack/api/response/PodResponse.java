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

import com.google.gson.annotations.SerializedName;

import org.apache.cloudstack.api.ApiConstants;
import org.apache.cloudstack.api.BaseResponseWithAnnotations;
import org.apache.cloudstack.api.EntityReference;

import com.cloud.dc.Pod;
import com.cloud.serializer.Param;

@EntityReference(value = Pod.class)
public class PodResponse extends BaseResponseWithAnnotations {
    @SerializedName(ApiConstants.ID)
    @Param(description = "the ID of the Pod")
    private String id;

    @SerializedName(ApiConstants.NAME)
    @Param(description = "the name of the Pod")
    private String name;

    @SerializedName(ApiConstants.ZONE_ID)
    @Param(description = "the Zone ID of the Pod")
    private String zoneId;

    @SerializedName(ApiConstants.ZONE_NAME)
    @Param(description = "the Zone name of the Pod")
    private String zoneName;

    @SerializedName(ApiConstants.GATEWAY)
    @Param(description = "the gateway of the Pod")
    private String gateway;

    @SerializedName(ApiConstants.NETMASK)
    @Param(description = "the netmask of the Pod")
    private String netmask;

    @SerializedName(ApiConstants.IP_RANGES)
    @Param(description = "the IP ranges for the Pod", responseObject = IpRangeResponse.class, since = "4.16.0")
    private List<IpRangeResponse> ipRanges;

    @Deprecated(since = "4.16")
    @SerializedName(ApiConstants.START_IP)
    @Param(description = "the starting IP for the Pod. This parameter is deprecated, please use 'startip' from ipranges parameter.")
    private List<String> startIp;

    @Deprecated(since = "4.16")
    @SerializedName(ApiConstants.END_IP)
    @Param(description = "the ending IP for the Pod. This parameter is deprecated, please use 'endip' from ipranges parameter.")
    private List<String> endIp;

    @Deprecated(since = "4.16")
    @SerializedName(ApiConstants.FOR_SYSTEM_VMS)
    @Param(description = "indicates if range is dedicated for CPVM and SSVM. This parameter is deprecated, please use 'forsystemvms' from ipranges parameter.")
    private List<String> forSystemVms;

    @Deprecated(since = "4.16")
    @SerializedName(ApiConstants.VLAN_ID)
    @Param(description = "indicates Vlan ID for the range. This parameter is deprecated, please use 'vlanid' from ipranges parameter.")
    private List<String> vlanId;

    @SerializedName(ApiConstants.ALLOCATION_STATE)
    @Param(description = "the allocation state of the Pod")
    private String allocationState;

    @SerializedName(ApiConstants.CAPACITY)
    @Param(description = "the capacity of the Pod", responseObject = CapacityResponse.class)
    private List<CapacityResponse> capacities;

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

    public void setIpRanges(List<IpRangeResponse> ipRanges) {
        this.ipRanges = ipRanges;
    }

    public List<String> getStartIp() {
        return startIp;
    }

    public void setStartIp(List<String> startIp) {
        this.startIp = startIp;
    }

    public List<String> getEndIp() {
        return endIp;
    }

    public void setEndIp(List<String> endIp) {
        this.endIp = endIp;
    }

    public void setForSystemVms(List<String> forSystemVms) {
        this.forSystemVms = forSystemVms;
    }

    public List<String> getForSystemVms() {
        return forSystemVms;
    }

    public List<String> getVlanId() {
        return vlanId;
    }

    public void setVlanId(List<String> vlanId) {
        this.vlanId = vlanId;
    }

    public String getAllocationState() {
        return allocationState;
    }

    public void setAllocationState(String allocationState) {
        this.allocationState = allocationState;
    }

    public List<CapacityResponse> getCapacities() {
        return capacities;
    }

    public void setCapacities(List<CapacityResponse> capacities) {
        this.capacities = capacities;
    }
}
