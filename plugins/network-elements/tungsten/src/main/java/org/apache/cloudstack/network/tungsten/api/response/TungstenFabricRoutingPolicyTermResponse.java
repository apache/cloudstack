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
import net.juniper.tungsten.api.types.PolicyTermType;
import net.juniper.tungsten.api.types.PrefixMatchType;
import org.apache.cloudstack.api.ApiConstants;
import org.apache.cloudstack.api.BaseResponse;

import java.util.ArrayList;
import java.util.List;

public class TungstenFabricRoutingPolicyTermResponse extends BaseResponse {

    @SerializedName(ApiConstants.TUNGSTEN_ROUTING_POLICY_TERM_NAME)
    @Param(description = "Tungsten-Fabric routing policy term name")
    private String termName;

    @SerializedName(ApiConstants.TUNGSTEN_ROUTING_POLICY_FROM_TERM)
    @Param(description = "Tungsten-Fabric routing from term", responseObject = TungstenFabricRoutingPolicyFromTermResponse.class)
    private TungstenFabricRoutingPolicyFromTermResponse fromTermResponse;

    @SerializedName(ApiConstants.TUNGSTEN_ROUTING_POLICY_THEN_TERM)
    @Param(description = "Tungsten-Fabric routing then term", responseObject = TungstenFabricRoutingPolicyThenTermResponse.class)
    private TungstenFabricRoutingPolicyThenTermResponse thenTermResponse;

    @SerializedName(ApiConstants.ZONE_ID)
    @Param(description = "Tungsten-Fabric provider zone id")
    private long zoneId;

    @SerializedName(ApiConstants.ZONE_NAME)
    @Param(description = "Tungsten-Fabric provider zone name")
    private String zoneName;

    public TungstenFabricRoutingPolicyTermResponse(PolicyTermType policyTermType, DataCenter zone) {
        List<String> prefixResponseList = new ArrayList<>();
        if(policyTermType.getTermMatchCondition().getPrefix() != null) {
            for (PrefixMatchType item : policyTermType.getTermMatchCondition().getPrefix()) {
                prefixResponseList.add(item.getPrefix() + " " + item.getPrefixType());
            }
        }
        TungstenFabricRoutingPolicyFromTermResponse routingPolicyFromTermResponse = new TungstenFabricRoutingPolicyFromTermResponse(
                policyTermType.getTermMatchCondition().getCommunityList(),
                policyTermType.getTermMatchCondition().getCommunityMatchAll(),
                policyTermType.getTermMatchCondition().getProtocol(),
                prefixResponseList, zone);
        TungstenFabricRoutingPolicyThenTermResponse routingPolicyThenTermResponse = new TungstenFabricRoutingPolicyThenTermResponse(
                policyTermType.getTermActionList().getUpdate(), policyTermType.getTermActionList().getAction(), zone);
        this.fromTermResponse = routingPolicyFromTermResponse;
        this.thenTermResponse = routingPolicyThenTermResponse;
        this.termName = getTermName(routingPolicyFromTermResponse, routingPolicyThenTermResponse);
        this.zoneId = zone.getId();
        this.zoneName = zone.getName();
        this.setObjectName("routingPolicyTerm");
    }

    public TungstenFabricRoutingPolicyFromTermResponse getFromTermResponse() {
        return fromTermResponse;
    }

    public void setFromTermResponse(TungstenFabricRoutingPolicyFromTermResponse fromTermResponse) {
        this.fromTermResponse = fromTermResponse;
    }

    public TungstenFabricRoutingPolicyThenTermResponse getThenTermResponse() {
        return thenTermResponse;
    }

    public void setThenTermResponse(TungstenFabricRoutingPolicyThenTermResponse thenTermResponse) {
        this.thenTermResponse = thenTermResponse;
    }

    public String getTermName() {
        return termName;
    }

    public void setTermName(String termName) {
        this.termName = termName;
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

    private String getTermName(TungstenFabricRoutingPolicyFromTermResponse routingPolicyFromTermResponse,
                               TungstenFabricRoutingPolicyThenTermResponse routingPolicyThenTermResponse) {
        StringBuilder sb = new StringBuilder();
        sb.append("from");
        if(routingPolicyFromTermResponse.getCommunities() != null && !routingPolicyFromTermResponse.getCommunities().isEmpty()) {
            sb.append(" community ");
            sb.append(routingPolicyFromTermResponse.getCommunities().toString());
        }
        if(routingPolicyFromTermResponse.getPrefixList() != null && !routingPolicyFromTermResponse.getPrefixList().isEmpty()) {
            sb.append(" prefix ");
            sb.append(routingPolicyFromTermResponse.getPrefixList().toString());
        }
        if(routingPolicyFromTermResponse.getProtocol() != null && !routingPolicyFromTermResponse.getProtocol().isEmpty()) {
            sb.append(" protocol ");
            sb.append(routingPolicyFromTermResponse.getProtocol());
        }
        if(sb.toString().equals("from")) {
            sb.append(" any");
        }
        sb.append( " then ");
        if(routingPolicyThenTermResponse.getAddCommunity() != null) {
            sb.append("add communities ");
            sb.append(routingPolicyThenTermResponse.getAddCommunity());
        }
        if(routingPolicyThenTermResponse.getSetCommunity() != null) {
            sb.append(" set communities ");
            sb.append(routingPolicyThenTermResponse.getSetCommunity());
        }
        if(routingPolicyThenTermResponse.getRemoveCommunity() != null) {
            sb.append(" remove communities ");
            sb.append(routingPolicyThenTermResponse.getRemoveCommunity());
        }
        if(routingPolicyThenTermResponse.getLocalPreference() != null) {
            sb.append(" local-preference ");
            sb.append(routingPolicyThenTermResponse.getLocalPreference());
        }
        if(routingPolicyThenTermResponse.getMed() != null) {
            sb.append(" med ");
            sb.append(routingPolicyThenTermResponse.getMed());
        }
        if(routingPolicyThenTermResponse.getAsPath() != null) {
            sb.append(" as-path ");
            sb.append(routingPolicyThenTermResponse.getAsPath());
        }
        if(routingPolicyThenTermResponse.getAction() != null) {
            sb.append(" action ");
            sb.append(routingPolicyThenTermResponse.getAction());
        } else {
            sb.append(" action ");
            sb.append("default");
        }
        return sb.toString();
    }
}
