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

import com.cloud.serializer.Param;
import com.google.gson.annotations.SerializedName;
import net.juniper.tungsten.api.ApiPropertyBase;
import net.juniper.tungsten.api.ObjectReference;
import net.juniper.tungsten.api.types.ApplicationPolicySet;
import net.juniper.tungsten.api.types.FirewallSequence;
import org.apache.cloudstack.api.ApiConstants;
import org.apache.cloudstack.api.BaseResponse;

import java.util.ArrayList;
import java.util.List;

public class TungstenFabricApplicationPolicySetResponse extends BaseResponse {
    @SerializedName(ApiConstants.UUID)
    @Param(description = "Tungsten-Fabric application policy uuid")
    private String uuid;

    @SerializedName(ApiConstants.NAME)
    @Param(description = "Tungsten-Fabric policy name")
    private String name;

    @SerializedName(ApiConstants.FIREWALL_POLICY)
    @Param(description = "list Tungsten-Fabric firewall policy")
    private List<TungstenFabricFirewallPolicyResponse> firewallPolicys;

    @SerializedName(ApiConstants.TAG)
    @Param(description = "list Tungsten-Fabric tag")
    private List<TungstenFabricTagResponse> tags;

    public TungstenFabricApplicationPolicySetResponse(ApplicationPolicySet applicationPolicySet) {
        this.uuid = applicationPolicySet.getUuid();
        this.name = applicationPolicySet.getName();
        List<TungstenFabricTagResponse> tags = new ArrayList<>();
        List<ObjectReference<ApiPropertyBase>> objectReferenceTagList = applicationPolicySet.getTag();
        if (objectReferenceTagList != null) {
            for (ObjectReference<ApiPropertyBase> objectReference : objectReferenceTagList) {
                TungstenFabricTagResponse tungstenFabricTagResponse = new TungstenFabricTagResponse(
                    objectReference.getUuid(),
                    objectReference.getReferredName().get(objectReference.getReferredName().size() - 1));
                tags.add(tungstenFabricTagResponse);
            }
        }
        this.tags = tags;
        List<TungstenFabricFirewallPolicyResponse> firewallPolicys = new ArrayList<>();
        List<ObjectReference<FirewallSequence>> objectReferenceFirewallPolicyList =
            applicationPolicySet.getFirewallPolicy();
        if (objectReferenceFirewallPolicyList != null) {
            for (ObjectReference<FirewallSequence> objectReference : objectReferenceFirewallPolicyList) {
                TungstenFabricFirewallPolicyResponse tungstenFabricFirewallPolicyResponse =
                    new TungstenFabricFirewallPolicyResponse(
                    objectReference.getUuid(),
                    objectReference.getReferredName().get(objectReference.getReferredName().size() - 1));
                firewallPolicys.add(tungstenFabricFirewallPolicyResponse);
            }
        }
        this.firewallPolicys = firewallPolicys;
        this.setObjectName("applicationpolicyset");
    }

    public List<TungstenFabricFirewallPolicyResponse> getFirewallPolicys() {
        return firewallPolicys;
    }

    public void setFirewallPolicys(final List<TungstenFabricFirewallPolicyResponse> firewallPolicys) {
        this.firewallPolicys = firewallPolicys;
    }

    public List<TungstenFabricTagResponse> getTags() {
        return tags;
    }

    public void setTags(final List<TungstenFabricTagResponse> tags) {
        this.tags = tags;
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

}
