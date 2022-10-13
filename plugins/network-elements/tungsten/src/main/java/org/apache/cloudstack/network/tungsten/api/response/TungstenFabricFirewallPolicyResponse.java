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
import net.juniper.tungsten.api.ObjectReference;
import net.juniper.tungsten.api.types.FirewallPolicy;
import net.juniper.tungsten.api.types.FirewallSequence;
import org.apache.cloudstack.api.ApiConstants;
import org.apache.cloudstack.api.BaseResponse;

import java.util.ArrayList;
import java.util.List;

public class TungstenFabricFirewallPolicyResponse extends BaseResponse {
    @SerializedName(ApiConstants.UUID)
    @Param(description = "Tungsten-Fabric firewall policy uuid")
    private String uuid;

    @SerializedName(ApiConstants.NAME)
    @Param(description = "Tungsten-Fabric firewall policy name")
    private String name;

    @SerializedName(ApiConstants.FIREWALL_RULE)
    @Param(description = "list Tungsten-Fabric firewall rule")
    private List<TungstenFabricFirewallRuleResponse> firewallRules;

    @SerializedName(ApiConstants.ZONE_ID)
    @Param(description = "Tungsten-Fabric provider zone id")
    private long zoneId;

    @SerializedName(ApiConstants.ZONE_NAME)
    @Param(description = "Tungsten-Fabric provider zone name")
    private String zoneName;

    public TungstenFabricFirewallPolicyResponse(String uuid, String name, DataCenter zone) {
        this.uuid = uuid;
        this.name = name;
        this.zoneId = zone.getId();
        this.zoneName = zone.getName();
        this.setObjectName("firewallpolicy");
    }

    public TungstenFabricFirewallPolicyResponse(FirewallPolicy firewallPolicy, DataCenter zone) {
        this.uuid = firewallPolicy.getUuid();
        this.name = firewallPolicy.getName();
        this.setObjectName("firewallpolicy");

        List<TungstenFabricFirewallRuleResponse> responsesFirewallRules = new ArrayList<>();
        List<ObjectReference<FirewallSequence>> objectReferenceFirewallRuleList = firewallPolicy.getFirewallRule();
        if (objectReferenceFirewallRuleList != null) {
            for (ObjectReference<FirewallSequence> objectReference : objectReferenceFirewallRuleList) {
                TungstenFabricFirewallRuleResponse tungstenFabricFirewallRuleResponse = new TungstenFabricFirewallRuleResponse(
                    objectReference.getUuid(),
                    objectReference.getReferredName().get(objectReference.getReferredName().size() - 1), zone);
                responsesFirewallRules.add(tungstenFabricFirewallRuleResponse);
            }
        }
        this.firewallRules = responsesFirewallRules;
        this.zoneId = zone.getId();
        this.zoneName = zone.getName();
    }

    public List<TungstenFabricFirewallRuleResponse> getFirewallRules() {
        return firewallRules;
    }

    public void setFirewallRules(final List<TungstenFabricFirewallRuleResponse> firewallRules) {
        this.firewallRules = firewallRules;
    }

    public String getUuid() {
        return uuid;
    }

    public void setUuid(final String uuid) {
        this.uuid = uuid;
    }

    public String getName() {
        return name;
    }

    public void setName(final String name) {
        this.name = name;
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
