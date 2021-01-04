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
import org.apache.cloudstack.api.BaseResponse;
import org.apache.cloudstack.api.EntityReference;

import com.cloud.network.PhysicalNetwork;
import com.cloud.serializer.Param;

@EntityReference(value = PhysicalNetwork.class)
@SuppressWarnings("unused")
public class PhysicalNetworkResponse extends BaseResponse {

    @SerializedName(ApiConstants.ID)
    @Param(description = "the uuid of the physical network")
    private String id;

    @SerializedName(ApiConstants.NAME)
    @Param(description = "name of the physical network")
    private String name;

    @SerializedName(ApiConstants.BROADCAST_DOMAIN_RANGE)
    @Param(description = "Broadcast domain range of the physical network")
    private String broadcastDomainRange;

    @SerializedName(ApiConstants.ZONE_ID)
    @Param(description = "zone id of the physical network")
    private String zoneId;

    @SerializedName(ApiConstants.ZONE_NAME)
    @Param(description = "zone name of the physical network")
    private String zoneName;

    @SerializedName(ApiConstants.STATE)
    @Param(description = "state of the physical network")
    private String state;

    @SerializedName(ApiConstants.VLAN)
    @Param(description = "the vlan of the physical network")
    private String vlan;

    @SerializedName(ApiConstants.DOMAIN_ID)
    @Param(description = "the domain id of the physical network owner")
    private String domainId;

    @SerializedName(ApiConstants.TAGS)
    @Param(description = "comma separated tag")
    private String tags;

    @SerializedName(ApiConstants.ISOLATION_METHODS)
    @Param(description = "isolation methods")
    private String isolationMethods;

    @SerializedName(ApiConstants.NETWORK_SPEED)
    @Param(description = "the speed of the physical network")
    private String networkSpeed;

    @Override
    public String getObjectId() {
        return this.id;

    }

    public void setId(String uuid) {
        this.id = uuid;
    }

    public void setZoneId(String zoneId) {
        this.zoneId = zoneId;
    }

    public void setZoneName(String zoneName) {
        this.zoneName = zoneName;
    }

    public void setState(String state) {
        this.state = state;
    }

    public void setDomainId(String domainId) {
        this.domainId = domainId;
    }

    public void setVlan(String vlan) {
        this.vlan = vlan;
    }

    public void setTags(List<String> tags) {
        if (tags == null || tags.size() == 0) {
            return;
        }

        StringBuilder buf = new StringBuilder();
        for (String tag : tags) {
            buf.append(tag).append(",");
        }

        this.tags = buf.delete(buf.length() - 1, buf.length()).toString();
    }

    public void setBroadcastDomainRange(String broadcastDomainRange) {
        this.broadcastDomainRange = broadcastDomainRange;
    }

    public void setNetworkSpeed(String networkSpeed) {
        this.networkSpeed = networkSpeed;
    }

    public void setIsolationMethods(List<String> isolationMethods) {
        if (isolationMethods == null || isolationMethods.size() == 0) {
            return;
        }

        StringBuilder buf = new StringBuilder();
        for (String isolationMethod : isolationMethods) {
            buf.append(isolationMethod).append(",");
        }

        this.isolationMethods = buf.delete(buf.length() - 1, buf.length()).toString();
    }

    public void setName(String name) {
        this.name = name;
    }
}
