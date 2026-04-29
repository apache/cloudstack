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

import com.cloud.network.bigswitch.AttachmentData.Attachment.IpAddress;
import com.cloud.network.bigswitch.NetworkData.AttachmentInfo;
import com.cloud.network.bigswitch.NetworkData.Segment;
import com.cloud.network.bigswitch.NetworkData.SegmentInfo;
import com.cloud.network.bigswitch.RouterData.Router;
import com.google.gson.annotations.SerializedName;

public class TopologyData {
    @SerializedName("networks") private final List<Network> networks;
    @SerializedName("routers") private final List<Router> routers;

    public void addNetwork(Network network) {
        networks.add(network);
    }

    public void setNetworks(List<Network> networks) {
        this.networks.clear();
        this.networks.addAll(networks);
    }

    public void clearNetworks() {
        this.networks.clear();
    }

    public void addRouter(Router router) {
        routers.add(router);
    }

    public void setRouters(List<Router> routers) {
        this.routers.clear();
        this.routers.addAll(routers);
    }

    public void clearRouters() {
        this.routers.clear();
    }

    public TopologyData() {
        networks = new ArrayList<Network>();
        routers = new ArrayList<Router>();
    }

    public class Network {
        @SerializedName("id") private String id;
        @SerializedName("name") private String name;
        @SerializedName("tenant_id") private String tenantId;
        @SerializedName("tenant_name") private String tenantName;
        @SerializedName("provider:segmentation_id") private Integer vlan = null;
        @SerializedName("state") private String state;
        @SerializedName("ports") private List<Port> ports;
        @SerializedName("subnets") private List<Segment> segments;

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
        public Integer getVlan() {
            return vlan;
        }
        public void setVlan(Integer vlan) {
            this.vlan = vlan;
        }
        public String getState() {
            return state;
        }
        public void setState(String state) {
            this.state = state;
        }
        public List<Port> getPorts() {
            return ports;
        }
        public void setPorts(List<Port> ports) {
            this.ports = ports;
        }
        public List<Segment> getSegments() {
            return segments;
        }
        public void setSegments(List<Segment> segments) {
            this.segments = segments;
        }
        @Override
        public String toString() {
            return "Network [id=" + id + ", name=" + name + ", tenantId="
                    + tenantId + ", tenantName=" + tenantName + ", vlan="
                    + vlan + ", state=" + state + ", ports=" + ports
                    + ", segments=" + segments + "]";
        }
    }

    public class Port {
        @SerializedName("attachment") private AttachmentInfo attachment;
        @SerializedName("binding:host_id") private String hostId;
        @SerializedName("bound_segment") private SegmentInfo segmentInfo;
        @SerializedName("device_owner") private String owner;
        @SerializedName("fixed_ips") private List<IpAddress> ipAddresses;
        @SerializedName("id") private String id;
        @SerializedName("mac_address") private String mac;
        @SerializedName("network") private NetworkData.Network network;
        @SerializedName("state") private String state;
        @SerializedName("tenant_id") private String tenantId;
        @SerializedName("tenant_name") private String tenantName;

        public AttachmentInfo getAttachment() {
            return attachment;
        }

        public void setAttachmentInfo(AttachmentInfo attachment) {
            this.attachment = attachment;
        }

        public String getHostId() {
            return hostId;
        }

        public void setHostId(String hostId) {
            this.hostId = hostId;
        }

        public SegmentInfo getSegmentInfo() {
            return segmentInfo;
        }

        public void setSegmentInfo(SegmentInfo segmentInfo) {
            this.segmentInfo = segmentInfo;
        }

        public String getOwner() {
            return owner;
        }

        public void setOwner(String owner) {
            this.owner = owner;
        }

        public List<IpAddress> getIpAddresses() {
            return ipAddresses;
        }

        public void setIpAddresses(List<IpAddress> ipAddresses) {
            this.ipAddresses = ipAddresses;
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

        public NetworkData.Network getNetwork() {
            return network;
        }

        public void setNetwork(NetworkData.Network network) {
            this.network = network;
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

        public String getTenantName() {
            return tenantName;
        }

        public void setTenantName(String tenantName) {
            this.tenantName = tenantName;
        }
    }

    public List<Network> getNetworks() {
        return networks;
    }

    public List<Router> getRouters() {
        return routers;
    }

    @Override
    public String toString() {
        return "TopologyData [networks=" + networks + ", routers=" + routers
                + "]";
    }
}
