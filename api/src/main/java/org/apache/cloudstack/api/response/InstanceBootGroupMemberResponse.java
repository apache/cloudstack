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
import java.util.List;

import com.google.gson.annotations.SerializedName;

import org.apache.cloudstack.api.ApiConstants;
import org.apache.cloudstack.api.BaseResponse;
import org.apache.cloudstack.api.EntityReference;
import org.apache.cloudstack.vm.bootgroup.InstanceBootGroupMember;

import com.cloud.serializer.Param;

@SuppressWarnings("unused")
@EntityReference(value = InstanceBootGroupMember.class)
public class InstanceBootGroupMemberResponse extends BaseResponse {

    @SerializedName(ApiConstants.ID)
    @Param(description = "The UUID of this member entry")
    private String id;

    @SerializedName(ApiConstants.BOOT_GROUP_ID)
    @Param(description = "The ID of the boot group this member belongs to")
    private String bootGroupId;

    @SerializedName(ApiConstants.MEMBER_TYPE)
    @Param(description = "The type of the member: VirtualMachine or InstanceGroup")
    private String memberType;

    @SerializedName(ApiConstants.MEMBER_ID)
    @Param(description = "The ID of the VM or InstanceGroup")
    private String memberId;

    @SerializedName(ApiConstants.MEMBER_NAME)
    @Param(description = "The name of the VM or InstanceGroup")
    private String memberName;

    @SerializedName(ApiConstants.MEMBER_STATE)
    @Param(description = "The state of the Instance")
    private String memberState;

    @SerializedName(ApiConstants.BOOT_ORDER)
    @Param(description = "The boot order value for this member")
    private int order;

    @SerializedName(ApiConstants.CREATED)
    @Param(description = "The date the member was added to the boot group")
    private Date created;

    @SerializedName(ApiConstants.READINESS_MODE)
    @Param(description = "None, CHILD_DEPENDENT or RULE_BASED, computed from whether readiness rules are attached")
    private String readinessMode;

    @SerializedName(ApiConstants.READINESS_STATUS)
    @Param(description = "The last cached readiness evaluation")
    private String readinessStatus;

    @SerializedName(ApiConstants.READINESS_MESSAGE)
    @Param(description = "Why readinessstatus is what it is: the failing rule(s)' cached message, and/or a count of not-ready member VMs for InstanceGroup members")
    private String readinessMessage;

    @SerializedName(ApiConstants.CHILDREN)
    @Param(description = "For InstanceGroup members, the VMs within the group (only present when requested via details=children)", responseObject = InstanceBootGroupMemberChildResponse.class)
    private List<InstanceBootGroupMemberChildResponse> children;

    public void setId(String id) {
        this.id = id;
    }

    public void setBootGroupId(String bootGroupId) {
        this.bootGroupId = bootGroupId;
    }

    public void setMemberType(String memberType) {
        this.memberType = memberType;
    }

    public void setMemberId(String memberId) {
        this.memberId = memberId;
    }

    public void setMemberName(String memberName) {
        this.memberName = memberName;
    }

    public void setMemberState(String memberState) {
        this.memberState = memberState;
    }

    public void setOrder(int order) {
        this.order = order;
    }

    public void setCreated(Date created) {
        this.created = created;
    }

    public void setReadinessMode(String readinessMode) {
        this.readinessMode = readinessMode;
    }

    public void setReadinessStatus(String readinessStatus) {
        this.readinessStatus = readinessStatus;
    }

    public void setReadinessMessage(String readinessMessage) {
        this.readinessMessage = readinessMessage;
    }

    public void setChildren(List<InstanceBootGroupMemberChildResponse> children) {
        this.children = children;
    }
}
