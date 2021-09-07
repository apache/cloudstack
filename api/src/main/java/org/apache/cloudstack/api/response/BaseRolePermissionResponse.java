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

import org.apache.cloudstack.acl.RolePermissionEntity.Permission;
import org.apache.cloudstack.acl.Rule;
import org.apache.cloudstack.api.ApiConstants;
import org.apache.cloudstack.api.BaseResponse;

import com.cloud.serializer.Param;
import com.google.gson.annotations.SerializedName;

public class BaseRolePermissionResponse extends BaseResponse {
    @SerializedName(ApiConstants.RULE)
    @Param(description = "the api name or wildcard rule")
    private String rule;

    @SerializedName(ApiConstants.PERMISSION)
    @Param(description = "the permission type of the api name or wildcard rule, allow/deny")
    private String rulePermission;

    @SerializedName(ApiConstants.DESCRIPTION)
    @Param(description = "the description of the role permission")
    private String ruleDescription;

    public String getRule() {
        return rule;
    }

    public void setRule(Rule rule) {
        if (rule != null) {
            this.rule = rule.getRuleString();
        }
    }

    public String getRulePermission() {
        return rulePermission;
    }

    public void setRulePermission(Permission rulePermission) {
        if (rulePermission != null) {
            this.rulePermission = rulePermission.name().toLowerCase();
        }
    }

    public void setDescription(String description) {
        this.ruleDescription = description;
    }
}
