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
import net.juniper.tungsten.api.types.ActionAsPathType;
import net.juniper.tungsten.api.types.ActionUpdateType;
import net.juniper.tungsten.api.types.CommunityListType;
import org.apache.cloudstack.api.ApiConstants;
import org.apache.cloudstack.api.BaseResponse;

public class TungstenFabricRoutingPolicyThenTermResponse extends BaseResponse {

    @SerializedName(ApiConstants.TUNGSTEN_ROUTING_POLICY_TERM_ADD_COMMUNITY)
    @Param(description = "Tungsten-Fabric routing policy term add community")
    private String addCommunity;

    @SerializedName(ApiConstants.TUNGSTEN_ROUTING_POLICY_TERM_SET_COMMUNITY)
    @Param(description = "Tungsten-Fabric routing policy term set community")
    private String setCommunity;

    @SerializedName(ApiConstants.TUNGSTEN_ROUTING_POLICY_TERM_REMOVE_COMMUNITY)
    @Param(description = "Tungsten-Fabric routing policy term remove community")
    private String removeCommunity;

    @SerializedName(ApiConstants.TUNGSTEN_ROUTING_POLICY_TERM_LOCAL_PREFERENCE)
    @Param(description = "Tungsten-Fabric routing policy term local preference")
    private String localPreference;

    @SerializedName(ApiConstants.TUNGSTEN_ROUTING_POLICY_TERM_MED)
    @Param(description = "Tungsten-Fabric routing policy term med")
    private String med;

    @SerializedName(ApiConstants.TUNGSTEN_ROUTING_POLICY_TERM_ACTION)
    @Param(description = "Tungsten-Fabric routing policy term action")
    private String action;

    @SerializedName(ApiConstants.TUNGSTEN_ROUTING_POLICY_TERM_AS_PATH)
    @Param(description = "Tungsten-Fabric routing policy term asPath")
    private String asPath;

    public TungstenFabricRoutingPolicyThenTermResponse(ActionUpdateType actionUpdateType, String action) {
        this.addCommunity = actionUpdateType.getCommunity() == null ? null : getActionCommunities(actionUpdateType.getCommunity().getAdd());
        this.setCommunity = actionUpdateType.getCommunity() == null ? null : getActionCommunities(actionUpdateType.getCommunity().getSet());
        this.removeCommunity = actionUpdateType.getCommunity() == null ? null : getActionCommunities(actionUpdateType.getCommunity().getRemove());
        this.localPreference = actionUpdateType.getLocalPref() == null ? null : actionUpdateType.getLocalPref().toString();
        this.med = actionUpdateType.getMed() == null ? null : actionUpdateType.getMed().toString();
        this.action = action;
        this.asPath = getAsnList(actionUpdateType.getAsPath());
    }

    public String getAddCommunity() {
        return addCommunity;
    }

    public void setAddCommunity(String addCommunity) {
        this.addCommunity = addCommunity;
    }

    public String getSetCommunity() {
        return setCommunity;
    }

    public void setSetCommunity(String setCommunity) {
        this.setCommunity = setCommunity;
    }

    public String getRemoveCommunity() {
        return removeCommunity;
    }

    public void setRemoveCommunity(String removeCommunity) {
        this.removeCommunity = removeCommunity;
    }

    public String getLocalPreference() {
        return localPreference;
    }

    public void setLocalPreference(String localPreference) {
        this.localPreference = localPreference;
    }

    public String getMed() {
        return med;
    }

    public void setMed(String med) {
        this.med = med;
    }

    public String getAction() {
        return action;
    }

    public void setAction(String action) {
        this.action = action;
    }

    public String getAsPath() {
        return asPath;
    }

    public void setAsPath(String asPath) {
        this.asPath = asPath;
    }

    private String getActionCommunities(CommunityListType communityListType) {
        if(communityListType == null || communityListType.getCommunity() == null) {
            return null;
        } else {
            StringBuilder sb = new StringBuilder();
            for(String item : communityListType.getCommunity()) {
                sb.append(item).append(", ");
            }
            sb.delete(sb.length() - 2, sb.length());
            return sb.toString();
        }
    }

    private String getAsnList(ActionAsPathType actionAsPathType){
        if(actionAsPathType == null || actionAsPathType.getExpand().getAsnList() == null) {
            return null;
        } else {
            StringBuilder sb = new StringBuilder();
            for(int item : actionAsPathType.getExpand().getAsnList()) {
                sb.append(item).append(", ");
            }
            sb.delete(sb.length() - 2, sb.length());
            return sb.toString();
        }
    }
}
