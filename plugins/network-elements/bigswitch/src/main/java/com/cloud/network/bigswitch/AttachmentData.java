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

import java.util.ArrayList;
import java.util.List;

import com.google.gson.annotations.SerializedName;

/**
 * AttachmentData contains information expected by Big Switch Controller
 * in CreateBcfAttachmentCommand
 */
public class AttachmentData {
    @SerializedName("port") final private Attachment attachment;

    public Attachment getAttachment() {
        return this.attachment;
    }

    public AttachmentData() {
        this.attachment = new Attachment();
    }

    public class Attachment {
        @SerializedName("id") private String id;
        @SerializedName("tenant_name") private String tenantName;
        @SerializedName("vlan") private Integer vlan;
        @SerializedName("fixed_ips") final private List<IpAddress> fixedIps;
        @SerializedName("mac_address") private String macAddress;
        @SerializedName("bound_segment") final private BoundSegment boundSegment;
        @SerializedName("binding:host_id") private String hostId;

        public Attachment(){
            this.boundSegment = new BoundSegment();
            this.fixedIps = new ArrayList<IpAddress>();
        }

        public class BoundSegment {
            @SerializedName("segmentation_id") private Integer segmentationId;

            public Integer getSegmentationId() {
                return segmentationId;
            }

            public void setSegmentationId(Integer segmentationId) {
                this.segmentationId = segmentationId;
            }
        }

        public class IpAddress {
            @SerializedName("ip_address") private String address;

            public IpAddress(final String ipAddr) {
                this.address = ipAddr;
            }

            public String getIpAddress(){
                return address;
            }
        }

        private String state;

        public String getTenantName() {
            return tenantName;
        }

        public void setTenantName(final String tenantName) {
            this.tenantName = tenantName;
        }

        public String getId() {
            return id;
        }

        public void setId(final String id) {
            this.id = id;
        }

        public String getHostId() {
            return hostId;
        }

        public void setHostId(final String hostId) {
            this.hostId = hostId;
        }

        public Integer getVlan() {
            return vlan;
        }

        public void setVlan(final Integer vlan) {
            this.vlan = vlan;
            this.boundSegment.setSegmentationId(vlan);
        }

        public List<IpAddress> getIpv4List() {
            return fixedIps;
        }

        public void addIpv4(final String ipv4) {
            this.fixedIps.add(new IpAddress(ipv4));
        }

        public String getMac() {
            return macAddress;
        }

        public void setMac(final String mac) {
            this.macAddress = mac;
        }

        public BoundSegment getBoundSegment() {
            return boundSegment;
        }

        public String getState() {
            return state;
        }

        public void setState(final String state) {
            this.state = state;
        }
    }
}
