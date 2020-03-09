//
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
//

package com.cloud.network.bigswitch;

import java.util.List;

import com.google.gson.annotations.SerializedName;

/**
 * NetworkData contains information expected by Big Switch Controller
 * in CreateBcfSegmentCommand
 */
public class NetworkData {
    private final Network network;

    public Network getNetwork() {
        return network;
    }

    public NetworkData() {
        network = new Network();
    }

    public class Network {
        @SerializedName("id") private String id;
        @SerializedName("name") private String name = null;
        @SerializedName("tenant_id") private String tenantId = null;
        @SerializedName("tenant_name") private String tenantName = null;
        @SerializedName("state") private String state = null;
        @SerializedName("subnets") private List<Segment> segments = null;
        @SerializedName("provider:segmentation_id") private Integer vlan = null;

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

        public String getTenantId() {
            return tenantId;
        }

        public void setTenantId(String tenantId) {
            this.tenantId = tenantId;
        }

        public String getTenantName() {
            return tenantName;
        }

        public void setTenantName(String tenantName) {
            this.tenantName = tenantName;
        }

        public String getState() {
            return state;
        }

        public void setState(String state) {
            this.state = state;
        }

        public List<Segment> getSegments() {
            return segments;
        }

        public void setSegments(List<Segment> segments) {
            this.segments = segments;
        }

        public Integer getVlan() {
            return vlan;
        }

        public void setVlan(Integer vlan) {
            this.vlan = vlan;
        }
    }

    public class Segment {
        @SerializedName("cidr") private String cidr;
        @SerializedName("gateway_ip") private String gatewayIp;
        @SerializedName("id") private String id;
        @SerializedName("name") private String name;
        @SerializedName("state") private String state;
        @SerializedName("tenant_id") private String tenantId;

        public String getCidr() {
            return cidr;
        }

        public void setCidr(String cidr) {
            this.cidr = cidr;
        }

        public String getGatewayIp() {
            return gatewayIp;
        }

        public void setGatewayIp(String gatewayIp) {
            this.gatewayIp = gatewayIp;
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

        public String getState() {
            return state;
        }

        public void setState(String state) {
            this.state = state;
        }

        public String getTenantId() {
            return tenantId;
        }

        public void setTenantId(String tenantId) {
            this.tenantId = tenantId;
        }
    }



    public class AttachmentInfo {
        @SerializedName("id") private String id = null;
        @SerializedName("mac") private String mac = null;

        public AttachmentInfo(String id, String mac){
            this.id = id;
            this.mac = mac;
        }

        public String getId() {
            return id;
        }

        public void setId(String id) {
            this.id = id;
        }

        public String getMac() {
            return mac;
        }

        public void setMac(String mac) {
            this.mac = mac;
        }
    }

    public class SegmentInfo {
        @SerializedName("network_type") private String networkType = null;
        @SerializedName("segmentation_id") private Integer segmentationId = 0;

        public SegmentInfo(String networkType, Integer segmentationId){
            this.networkType = networkType;
            this.segmentationId = segmentationId;
        }

        public String getNetworkType() {
            return networkType;
        }

        public void setNetworkType(String networkType) {
            this.networkType = networkType;
        }

        public Integer getSegmentationId() {
            return segmentationId;
        }

        public void setSegmentationId(Integer segmentationId) {
            this.segmentationId = segmentationId;
        }
    }
}
