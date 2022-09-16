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
package org.apache.cloudstack.network.tungsten.api.response;

import com.cloud.dc.DataCenter;
import com.cloud.serializer.Param;
import com.google.gson.annotations.SerializedName;
import org.apache.cloudstack.api.ApiConstants;
import org.apache.cloudstack.api.BaseResponse;

import java.util.List;

public class TungstenFabricRoutingPolicyFromTermResponse extends BaseResponse {

    @SerializedName(ApiConstants.TUNGSTEN_ROUTING_POLICY_COMMUNITIES)
    @Param(description = "Tungsten-Fabric routing policy communities")
    private List<String> communities;

    @SerializedName(ApiConstants.TUNGSTEN_ROUTING_POLICY_MATCH_ALL)
    @Param(description = "Tungsten-Fabric routing policy match all communities")
    private boolean matchAll;

    @SerializedName(ApiConstants.TUNGSTEN_ROUTING_POLICY_PROTOCOL)
    @Param(description = "Tungsten-Fabric routing policy protocol")
    private List<String> protocol;

    @SerializedName(ApiConstants.TUNGSTEN_ROUTING_POLICY_PREFIX_LIST)
    @Param(description = "Tungsten-Fabric routing policy prefix list")
    private List<String> prefixList;

    @SerializedName(ApiConstants.ZONE_ID)
    @Param(description = "Tungsten-Fabric provider zone id")
    private long zoneId;

    @SerializedName(ApiConstants.ZONE_NAME)
    @Param(description = "Tungsten-Fabric provider zone name")
    private String zoneName;

    public TungstenFabricRoutingPolicyFromTermResponse(List<String> communities, boolean matchAll, List<String> protocol,
                                                       List<String> prefixList, DataCenter zone) {
        this.communities = communities;
        this.matchAll = matchAll;
        this.protocol = protocol;
        this.prefixList = prefixList;
        this.zoneId = zone.getId();
        this.zoneName = zone.getName();
    }

    public List<String> getCommunities() {
        return communities;
    }

    public void setCommunities(List<String> communities) {
        this.communities = communities;
    }

    public boolean isMatchAll() {
        return matchAll;
    }

    public void setMatchAll(boolean matchAll) {
        this.matchAll = matchAll;
    }

    public List<String> getProtocol() {
        return protocol;
    }

    public void setProtocol(List<String> protocol) {
        this.protocol = protocol;
    }

    public List<String> getPrefixList() {
        return prefixList;
    }

    public void setPrefixList(List<String> prefixList) {
        this.prefixList = prefixList;
    }

    public long getZoneId() {
        return zoneId;
    }

    public void setZoneId(final long zoneId) {
        this.zoneId = zoneId;
    }

    public String getZoneName() {
        return zoneName;
    }

    public void setZoneName(final String zoneName) {
        this.zoneName = zoneName;
    }
}
