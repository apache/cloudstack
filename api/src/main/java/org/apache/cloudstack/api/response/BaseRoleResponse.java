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

import org.apache.cloudstack.api.ApiConstants;
import org.apache.cloudstack.api.BaseResponse;

import com.cloud.serializer.Param;
import com.google.gson.annotations.SerializedName;

public class BaseRoleResponse extends BaseResponse {
    @SerializedName(ApiConstants.ID)
    @Param(description = "the ID of the role")
    private String id;

    @SerializedName(ApiConstants.NAME)
    @Param(description = "the name of the role")
    private String roleName;

    @SerializedName(ApiConstants.DESCRIPTION)
    @Param(description = "the description of the role")
    private String roleDescription;

    @SerializedName(ApiConstants.IS_PUBLIC)
    @Param(description = "Indicates whether the role will be visible to all users (public) or only to root admins (private)." +
            " If this parameter is not specified during the creation of the role its value will be defaulted to true (public).")
    private boolean publicRole = true;

    public void setId(String id) {
        this.id = id;
    }

    public void setRoleName(String roleName) {
        this.roleName = roleName;
    }

    public void setDescription(String description) {
        this.roleDescription = description;
    }

    public void setPublicRole(boolean publicRole) {
        this.publicRole = publicRole;
    }
}
