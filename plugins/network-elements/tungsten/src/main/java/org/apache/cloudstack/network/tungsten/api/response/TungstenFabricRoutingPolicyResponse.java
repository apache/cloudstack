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
import net.juniper.tungsten.api.ApiPropertyBase;
import net.juniper.tungsten.api.ObjectReference;
import net.juniper.tungsten.api.types.PolicyTermType;
import net.juniper.tungsten.api.types.RoutingPolicy;
import org.apache.cloudstack.api.ApiConstants;
import org.apache.cloudstack.api.BaseResponse;

import java.util.ArrayList;
import java.util.List;

public class TungstenFabricRoutingPolicyResponse extends BaseResponse {

    @SerializedName(ApiConstants.UUID)
    @Param(description = "Tungsten-Fabric routing policy uuid")
    private String uuid;

    @SerializedName(ApiConstants.NAME)
    @Param(description = "Tungsten-Fabric routing policy name")
    private String name;

    @SerializedName(ApiConstants.NETWORK)
    @Param(description = "list Tungsten-Fabric policy network name")
    private List<TungstenFabricNetworkResponse> networks;

    @SerializedName(ApiConstants.TUNGSTEN_ROUTING_POLICY_TERM)
    @Param(description = "Tungsten-Fabric routing policy terms", responseObject = TungstenFabricRoutingPolicyTermResponse.class)
    private List<TungstenFabricRoutingPolicyTermResponse> terms;

    @SerializedName(ApiConstants.ZONE_ID)
    @Param(description = "Tungsten-Fabric provider zone id")
    private long zoneId;

    @SerializedName(ApiConstants.ZONE_NAME)
    @Param(description = "Tungsten-Fabric provider zone name")
    private String zoneName;

    public TungstenFabricRoutingPolicyResponse(RoutingPolicy routingPolicy, DataCenter zone) {
        List<TungstenFabricRoutingPolicyTermResponse> terms = new ArrayList<>();
        if(routingPolicy.getEntries() != null && routingPolicy.getEntries().getTerm() != null) {
            for (PolicyTermType item : routingPolicy.getEntries().getTerm()) {
                TungstenFabricRoutingPolicyTermResponse routingPolicyTermResponse = new TungstenFabricRoutingPolicyTermResponse(item,zone);
                terms.add(routingPolicyTermResponse);
            }
        }
        this.name = routingPolicy.getName();
        this.uuid = routingPolicy.getUuid();
        this.terms = terms;
        List<TungstenFabricNetworkResponse> networks = new ArrayList<>();
        List<ObjectReference<ApiPropertyBase>> objectReferenceList = routingPolicy.getVirtualNetworkBackRefs();
        if (objectReferenceList != null) {
            for (ObjectReference<ApiPropertyBase> objectReference : objectReferenceList) {
                TungstenFabricNetworkResponse tungstenFabricNetworkResponse = new TungstenFabricNetworkResponse(
                    objectReference.getUuid(),
                    objectReference.getReferredName().get(objectReference.getReferredName().size() - 1)
                );
                networks.add(tungstenFabricNetworkResponse);
            }
        }
        this.networks = networks;
        this.zoneId = zone.getId();
        this.zoneName = zone.getName();
        this.setObjectName("routingpolicy");
    }

    public String getUuid() {
        return uuid;
    }

    public void setUuid(String uuid) {
        this.uuid = uuid;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public List<TungstenFabricRoutingPolicyTermResponse> getTerms() {
        return terms;
    }

    public void setTerms(List<TungstenFabricRoutingPolicyTermResponse> terms) {
        this.terms = terms;
    }

    public List<TungstenFabricNetworkResponse> getNetworks() {
        return networks;
    }

    public void setNetworks(final List<TungstenFabricNetworkResponse> networks) {
        this.networks = networks;
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
