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
import net.juniper.tungsten.api.types.FirewallRule;
import net.juniper.tungsten.api.types.FirewallRuleEndpointType;
import org.apache.cloudstack.api.ApiConstants;
import org.apache.cloudstack.api.BaseResponse;
import org.apache.commons.lang.StringUtils;

import java.util.List;

public class TungstenFabricFirewallRuleResponse extends BaseResponse {
    @SerializedName(ApiConstants.UUID)
    @Param(description = "Tungsten-Fabric firewall rule uuid")
    private String uuid;

    @SerializedName(ApiConstants.NAME)
    @Param(description = "Tungsten-Fabric firewall rule name")
    private String name;

    @SerializedName(ApiConstants.ACTION)
    @Param(description = "Tungsten-Fabric firewall rule action")
    private String action;

    @SerializedName(ApiConstants.SERVVICE_GROUP)
    @Param(description = "Tungsten-Fabric firewall rule service group")
    private String serviceGroup;

    @SerializedName(ApiConstants.SRC_TAG)
    @Param(description = "Tungsten-Fabric firewall rule source tag")
    private String srcTag;

    @SerializedName(ApiConstants.SRC_ADDRESS_GROUP)
    @Param(description = "Tungsten-Fabric firewall rule source address group")
    private String srcAddressGroup;

    @SerializedName(ApiConstants.DIRECTION)
    @Param(description = "Tungsten-Fabric firewall rule direction")
    private String direction;

    @SerializedName(ApiConstants.DEST_TAG)
    @Param(description = "Tungsten-Fabric firewall rule destination tag")
    private String destTag;

    @SerializedName(ApiConstants.DEST_ADDRESS_GROUP)
    @Param(description = "Tungsten-Fabric firewall rule destination address group")
    private String destAddressGroup;

    @SerializedName(ApiConstants.TAG_TYPE)
    @Param(description = "Tungsten-Fabric firewall rule tag type")
    private String tagType;

    public TungstenFabricFirewallRuleResponse(String uuid, String name) {
        this.uuid = uuid;
        this.name = name;
        this.setObjectName("firewallrule");
    }

    public TungstenFabricFirewallRuleResponse(FirewallRule firewallRule) {
        this.uuid = firewallRule.getUuid();
        this.name = firewallRule.getName();
        this.action = firewallRule.getActionList().getSimpleAction();
        this.direction = firewallRule.getDirection();
        List<ObjectReference<ApiPropertyBase>> serviceGroupList = firewallRule.getServiceGroup();
        if (serviceGroupList != null) {
            List<String> serviceGroupNameList = serviceGroupList.get(0).getReferredName();
            this.serviceGroup = serviceGroupNameList.get(serviceGroupNameList.size() - 1);
        }

        if (firewallRule.getEndpoint1() != null) {
            FirewallRuleEndpointType srcEndpoint = firewallRule.getEndpoint1();
            if (srcEndpoint.getTags() != null) {
                List<String> srcTagList = srcEndpoint.getTags();
                this.srcTag = srcTagList.get(0);
            } else {
                String addressGroup = srcEndpoint.getAddressGroup();
                String[] addressGroupList = StringUtils.split(addressGroup, ":");
                this.srcAddressGroup = addressGroupList[addressGroupList.length - 1];
            }
        }

        if (firewallRule.getEndpoint2() != null) {
            FirewallRuleEndpointType destEndpoint = firewallRule.getEndpoint2();
            if (destEndpoint.getTags() != null) {
                List<String> destTagList = destEndpoint.getTags();
                this.destTag = destTagList.get(0);
            } else {
                String addressGroup = destEndpoint.getAddressGroup();
                String[] addressGroupList = StringUtils.split(addressGroup, ":");
                this.destAddressGroup = addressGroupList[addressGroupList.length - 1];
            }
        }

        if (firewallRule.getMatchTags() != null && firewallRule.getMatchTags().getTagList().size() > 0) {
            this.tagType = firewallRule.getMatchTags().getTagList().get(0);
        }

        this.setObjectName("firewallrule");
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

    public String getAction() {
        return action;
    }

    public void setAction(final String action) {
        this.action = action;
    }

    public String getDirection() {
        return direction;
    }

    public void setDirection(final String direction) {
        this.direction = direction;
    }

    public String getServiceGroup() {
        return serviceGroup;
    }

    public void setServiceGroup(final String serviceGroup) {
        this.serviceGroup = serviceGroup;
    }

    public String getSrcAddressGroup() {
        return srcAddressGroup;
    }

    public void setSrcAddressGroup(final String srcAddressGroup) {
        this.srcAddressGroup = srcAddressGroup;
    }

    public String getDestAddressGroup() {
        return destAddressGroup;
    }

    public void setDestAddressGroup(final String destAddressGroup) {
        this.destAddressGroup = destAddressGroup;
    }

    public String getTagType() {
        return tagType;
    }

    public void setTagType(final String tagType) {
        this.tagType = tagType;
    }

    public String getSrcTag() {
        return srcTag;
    }

    public void setSrcTag(final String srcTag) {
        this.srcTag = srcTag;
    }

    public String getDestTag() {
        return destTag;
    }

    public void setDestTag(final String destTag) {
        this.destTag = destTag;
    }
}
