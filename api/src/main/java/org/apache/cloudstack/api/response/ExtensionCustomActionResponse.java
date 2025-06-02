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

import org.apache.cloudstack.extension.ExtensionCustomAction;
import com.cloud.serializer.Param;
import com.google.gson.annotations.SerializedName;
import org.apache.cloudstack.api.ApiConstants;
import org.apache.cloudstack.api.BaseResponse;
import org.apache.cloudstack.api.EntityReference;

import java.util.Date;
import java.util.Map;
import java.util.Set;

@EntityReference(value = ExtensionCustomAction.class)
public class ExtensionCustomActionResponse extends BaseResponse {

    @SerializedName(ApiConstants.ID)
    @Param(description = "ID of the extension custom action")
    private String id;

    @SerializedName(ApiConstants.NAME)
    @Param(description = "Name of the extension custom action")
    private String name;

    @SerializedName(ApiConstants.DESCRIPTION)
    @Param(description = "Description of the extension custom action")
    private String description;

    @SerializedName(ApiConstants.EXTENSION_ID)
    @Param(description = "ID of the extension that this extension custom action belongs to")
    private String extensionId;

    @SerializedName(ApiConstants.EXTENSION_NAME)
    @Param(description = "Name of the extension that this extension custom action belongs to")
    private String extensionName;

    @SerializedName(ApiConstants.RESOURCE_TYPE)
    @Param(description = "Resource type for which the action is available")
    private String resourceType;

    @SerializedName(ApiConstants.ROLES_LIST)
    @Param(description = "Comma separated list of roles associated with the extension custom action")
    private String rolesList;

    @SerializedName(ApiConstants.ENABLED)
    @Param(description = "Whether the extension custom action is enabled or not")
    private Boolean enabled;

    @SerializedName(ApiConstants.DETAILS)
    @Param(description = "Details of the extension custom action")
    private Map<String, String> details;

    @SerializedName(ApiConstants.PARAMETERS)
    @Param(description = "List of the parameters for the action", responseObject = ExtensionCustomActionParameterResponse.class)
    private Set<ExtensionCustomActionParameterResponse> parameters;

    @SerializedName(ApiConstants.CREATED)
    @Param(description = "Creation timestamp of the extension custom action")
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

    public void setExtensionId(String extensionId) {
        this.extensionId = extensionId;
    }

    public void setExtensionName(String extensionName) {
        this.extensionName = extensionName;
    }

    public String getResourceType() {
        return resourceType;
    }

    public void setResourceType(String resourceType) {
        this.resourceType = resourceType;
    }

    public String getRolesList() {
        return rolesList;
    }

    public void setRolesList(String rolesList) {
        this.rolesList = rolesList;
    }

    public void setEnabled(Boolean enabled) {
        this.enabled = enabled;
    }

    public void setParameters(Set<ExtensionCustomActionParameterResponse> parameters) {
        this.parameters = parameters;
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
