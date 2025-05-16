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

import com.cloud.extension.ExtensionCustomAction;
import com.cloud.serializer.Param;
import com.google.gson.annotations.SerializedName;
import org.apache.cloudstack.api.ApiConstants;
import org.apache.cloudstack.api.BaseResponse;
import org.apache.cloudstack.api.EntityReference;

import java.util.Date;
import java.util.Map;

@EntityReference(value = ExtensionCustomAction.class)
public class ExtensionCustomActionResponse extends BaseResponse {

    @SerializedName(ApiConstants.ID)
    @Param(description = "UUID of the extension custom action")
    private String id;

    @SerializedName(ApiConstants.NAME)
    @Param(description = "Name of the extension custom action")
    private String name;

    @SerializedName(ApiConstants.DESCRIPTION)
    @Param(description = "Description of the extension custom action")
    private String description;

    @SerializedName(ApiConstants.ROLES_LIST)
    @Param(description = "Comma separated list of roles associated with the extension custom action")
    private String rolesList;

    @SerializedName(ApiConstants.DETAILS)
    @Param(description = "the details of the extension")
    private Map<String, String> details;

    @SerializedName(ApiConstants.CREATED)
    @Param(description = "Creation timestamp of the extension")
    private Date created;

    public ExtensionCustomActionResponse(String id, String name, String description, String rolesList) {
        this.id = id;
        this.name = name;
        this.description = description;
        this.rolesList = rolesList;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getRolesList() {
        return rolesList;
    }

    public void setRolesList(String rolesList) {
        this.rolesList = rolesList;
    }

    public void setDetails(Map<String, String> details) {
        this.details = details;
    }

    public Date getCreated() {
        return created;
    }

    public void setCreated(Date created) {
        this.created = created;
    }
}
