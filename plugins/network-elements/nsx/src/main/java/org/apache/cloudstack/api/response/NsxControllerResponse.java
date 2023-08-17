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

import com.cloud.network.NsxProvider;
import com.cloud.serializer.Param;
import com.google.gson.annotations.SerializedName;
import org.apache.cloudstack.api.ApiConstants;
import org.apache.cloudstack.api.BaseResponse;
import org.apache.cloudstack.api.EntityReference;

@EntityReference(value = {NsxProvider.class})
public class NsxControllerResponse extends BaseResponse {
    @SerializedName(ApiConstants.NAME)
    @Param(description = "NSX controller name")
    private String name;

    @SerializedName(ApiConstants.ZONE_ID)
    @Param(description = "Zone ID to which the NSX controller is associated with")
    private long zoneId;

    @SerializedName(ApiConstants.ZONE_NAME)
    @Param(description = "Zone name to which the NSX controller is associated with")
    private String zoneName;

    @SerializedName(ApiConstants.HOST_NAME)
    @Param(description = "NSX controller hostname or IP address")
    private String hostname;

    // TODO: Should Password be returned?

    @SerializedName(ApiConstants.TIER0_GATEWAY)
    @Param(description = "The tier-0 gateway network. Tier-0 gateway is responsible for handling" +
            " traffic between logical and physical networks"
    )
    private String tier0Gateway;

    @SerializedName(ApiConstants.EDGE_CLUSTER)
    @Param(description = "The name of the edge cluster. An edge cluster is a logical grouping of edge nodes in NSX")
    private String edgeCluster;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public long getZoneId() {
        return zoneId;
    }

    public void setZoneId(long zoneId) {
        this.zoneId = zoneId;
    }

    public String getZoneName() {
        return zoneName;
    }

    public void setZoneName(String zoneName) {
        this.zoneName = zoneName;
    }

    public String getHostname() {
        return hostname;
    }

    public void setHostname(String hostname) {
        this.hostname = hostname;
    }

    public String getTier0Gateway() {
        return tier0Gateway;
    }

    public void setTier0Gateway(String tier0Gateway) {
        this.tier0Gateway = tier0Gateway;
    }

    public String getEdgeCluster() {
        return edgeCluster;
    }

    public void setEdgeCluster(String edgeCluster) {
        this.edgeCluster = edgeCluster;
    }
}
