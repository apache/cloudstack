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

import java.util.List;

import com.google.gson.annotations.SerializedName;

import org.apache.cloudstack.api.ApiConstants;
import org.apache.cloudstack.api.BaseResponse;

import com.cloud.serializer.Param;

public class ConfigurationGroupResponse extends BaseResponse {
    @SerializedName(ApiConstants.NAME)
    @Param(description = "the name of the configuration group")
    private String groupName;

    @SerializedName(ApiConstants.SUBGROUP)
    @Param(description = "the subgroups of the configuration group", responseObject = ConfigurationSubGroupResponse.class)
    private List<ConfigurationSubGroupResponse> subGroups;

    @SerializedName(ApiConstants.DESCRIPTION)
    @Param(description = "the description of the configuration group")
    private String description;

    @SerializedName(ApiConstants.PRECEDENCE)
    @Param(description = "the precedence of the configuration group")
    private Long precedence;

    public String getGroupName() {
        return groupName;
    }

    public void setGroupName(String groupName) {
        this.groupName = groupName;
    }

    public List<ConfigurationSubGroupResponse> getSubGroups() {
        return subGroups;
    }

    public void setSubGroups(List<ConfigurationSubGroupResponse> subGroups) {
        this.subGroups = subGroups;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public Long getPrecedence() {
        return precedence;
    }

    public void setPrecedence(Long precedence) {
        this.precedence = precedence;
    }
}
