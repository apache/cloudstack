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

import java.util.Date;
import java.util.Map;

import com.google.gson.annotations.SerializedName;

import org.apache.cloudstack.api.ApiConstants;
import org.apache.cloudstack.api.BaseResponse;
import org.apache.cloudstack.api.EntityReference;
import org.apache.cloudstack.vm.bootgroup.readiness.InstanceBootGroupReadinessRule;

import com.cloud.serializer.Param;

@SuppressWarnings("unused")
@EntityReference(value = InstanceBootGroupReadinessRule.class)
public class InstanceBootGroupReadinessRuleResponse extends BaseResponse {

    @SerializedName(ApiConstants.ID)
    @Param(description = "The ID of the readiness rule")
    private String id;

    @SerializedName(ApiConstants.NAME)
    @Param(description = "The name of the readiness rule")
    private String name;

    @SerializedName(ApiConstants.BOOT_GROUP_ID)
    @Param(description = "The ID of the boot group this rule belongs to")
    private String bootGroupId;

    @SerializedName(ApiConstants.MEMBER_TYPE)
    @Param(description = "The item type this rule applies to: VirtualMachine or InstanceGroup")
    private String itemType;

    @SerializedName(ApiConstants.MEMBER_ID)
    @Param(description = "The ID of the item (VM or instance group) this rule applies to")
    private String itemId;

    @SerializedName(ApiConstants.MEMBER_NAME)
    @Param(description = "The name of the item (VM or instance group) this rule applies to")
    private String itemName;

    @SerializedName(ApiConstants.RULE_TYPE)
    @Param(description = "The readiness rule type")
    private String ruleType;

    @SerializedName(ApiConstants.ENABLED)
    @Param(description = "Whether the rule is enabled")
    private boolean enabled;

    @SerializedName(ApiConstants.INHERITED)
    @Param(description = "True if this rule is not attached to the queried item directly, but inherited from its owning InstanceGroup "
            + "(only possible when listing by virtualmachineid; the rule's itemtype/itemid still refer to the InstanceGroup it's actually attached to)")
    private boolean inherited;

    @SerializedName(ApiConstants.DETAILS)
    @Param(description = "Rule-type-specific configuration")
    private Map<String, String> details;

    @SerializedName(ApiConstants.CREATED)
    @Param(description = "The date the rule was created")
    private Date created;

    @SerializedName(ApiConstants.READINESS_STATUS)
    @Param(description = "The last cached evaluation status of this rule: READY, NOT_READY, ERROR or UNKNOWN")
    private String status;

    @SerializedName("statusmessage")
    @Param(description = "The message from the last evaluation of this rule")
    private String statusMessage;

    @SerializedName("checkedon")
    @Param(description = "When this rule was last evaluated")
    private Date checkedOn;

    public void setId(String id) {
        this.id = id;
    }

    public void setName(String name) {
        this.name = name;
    }

    public void setBootGroupId(String bootGroupId) {
        this.bootGroupId = bootGroupId;
    }

    public void setItemType(String itemType) {
        this.itemType = itemType;
    }

    public void setItemId(String itemId) {
        this.itemId = itemId;
    }

    public void setItemName(String itemName) {
        this.itemName = itemName;
    }

    public void setRuleType(String ruleType) {
        this.ruleType = ruleType;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public void setInherited(boolean inherited) {
        this.inherited = inherited;
    }

    public void setDetails(Map<String, String> details) {
        this.details = details;
    }

    public void setCreated(Date created) {
        this.created = created;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public void setStatusMessage(String statusMessage) {
        this.statusMessage = statusMessage;
    }

    public void setCheckedOn(Date checkedOn) {
        this.checkedOn = checkedOn;
    }
}
