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
import net.juniper.tungsten.api.types.RouteType;
import org.apache.cloudstack.api.ApiConstants;
import org.apache.cloudstack.api.BaseResponse;

public class TungstenFabricNetworkStaticRouteResponse extends BaseResponse {
    @SerializedName(ApiConstants.ROUTE_PREFIX)
    @Param(description = "Tungsten-Fabric network static route prefix")
    private String routePrefix;

    @SerializedName(ApiConstants.ROUTE_NEXT_HOP)
    @Param(description = "Tungsten-Fabric network static route next hop")
    private String routeNextHop;

    @SerializedName(ApiConstants.ROUTE_NEXT_HOP_TYPE)
    @Param(description = "Tungsten-Fabric network static route next hop type")
    private String routeNextHopType;

    @SerializedName(ApiConstants.COMMUNITIES)
    @Param(description = "Tungsten-Fabric network static route communities")
    private String communities;

    @SerializedName(ApiConstants.ZONE_ID)
    @Param(description = "Tungsten-Fabric provider zone id")
    private long zoneId;

    @SerializedName(ApiConstants.ZONE_NAME)
    @Param(description = "Tungsten-Fabric provider zone name")
    private String zoneName;

    public TungstenFabricNetworkStaticRouteResponse(String routePrefix, String routeNextHop, String routeNextHopType, DataCenter zone) {
        this.routePrefix = routePrefix;
        this.routeNextHop = routeNextHop;
        this.routeNextHopType = routeNextHopType;
        this.zoneId = zone.getId();
        this.zoneName = zone.getName();
        this.setObjectName("networkstaticroute");
    }

    public TungstenFabricNetworkStaticRouteResponse(RouteType routeType, DataCenter zone) {
        this.routePrefix = routeType.getPrefix();
        this.routeNextHop = routeType.getNextHop();
        this.routeNextHopType = routeType.getNextHopType();
        this.zoneId = zone.getId();
        this.zoneName = zone.getName();
        this.setObjectName("networkstaticroute");
        if (routeType.getCommunityAttributes() != null &&
                !routeType.getCommunityAttributes().getCommunityAttribute().isEmpty() &&
                routeType.getCommunityAttributes().getCommunityAttribute() != null) {
            StringBuilder sb = new StringBuilder();
            for (String item : routeType.getCommunityAttributes().getCommunityAttribute()) {
                sb.append(item);
                sb.append(",");
            }
            sb.setLength(sb.length() - 1);
            this.communities = sb.toString();
        }
    }

    public String getRoutePrefix() {
        return routePrefix;
    }

    public void setRoutePrefix(String routePrefix) {
        this.routePrefix = routePrefix;
    }

    public String getRouteNextHop() {
        return routeNextHop;
    }

    public void setRouteNextHop(String routeNextHop) {
        this.routeNextHop = routeNextHop;
    }

    public String getRouteNextHopType() {
        return routeNextHopType;
    }

    public void setRouteNextHopType(String routeNextHopType) {
        this.routeNextHopType = routeNextHopType;
    }

    public String getCommunities() {
        return communities;
    }

    public void setCommunities(final String communities) {
        this.communities = communities;
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
