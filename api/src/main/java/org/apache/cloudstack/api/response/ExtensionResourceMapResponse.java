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

import com.cloud.extension.ExtensionResourceMap;
import com.cloud.serializer.Param;
import com.google.gson.annotations.SerializedName;
import org.apache.cloudstack.api.ApiConstants;
import org.apache.cloudstack.api.BaseResponse;
import org.apache.cloudstack.api.EntityReference;

import java.util.Date;
import java.util.Map;

@EntityReference(value = ExtensionResourceMap.class)
public class ExtensionResourceMapResponse extends BaseResponse {
    @SerializedName(ApiConstants.EXTENSION_ID)
    @Param(description = "ID of the extension associated with this mapping")
    private String extensionId;

    @SerializedName(ApiConstants.RESOURCE_ID)
    @Param(description = "ID of the resource associated with this mapping")
    private String resourceId;

    @SerializedName(ApiConstants.RESOURCE_ID)
    @Param(description = "Name of the resource associated with this mapping")
    private String resourceName;

    @SerializedName(ApiConstants.RESOURCE_TYPE)
    @Param(description = "Type of the resource")
    private String resourceType;

    @SerializedName(ApiConstants.SCRIPT)
    @Param(description = "the path of the script")
    private String script;

    @SerializedName(ApiConstants.DETAILS)
    @Param(description = "the details of the resource map")
    private Map<String, String> details;

    @SerializedName(ApiConstants.CREATED)
    @Param(description = "Creation timestamp of the mapping")
    private Date created;

    @SerializedName(ApiConstants.REMOVED)
    @Param(description = "Removal timestamp of the mapping, if applicable")
    private Date removed;

    public ExtensionResourceMapResponse() {
    }

    public ExtensionResourceMapResponse(String extensionId, String resourceId, String resourceType) {
        this.extensionId = extensionId;
        this.resourceId = resourceId;
        this.resourceType = resourceType;
    }

    public String getExtensionId() {
        return extensionId;
    }

    public void setExtensionId(String extensionId) {
        this.extensionId = extensionId;
    }

    public String getResourceId() {
        return resourceId;
    }

    public void setResourceId(String resourceId) {
        this.resourceId = resourceId;
    }

    public void setResourceName(String resourceName) {
        this.resourceName = resourceName;
    }

    public String getResourceType() {
        return resourceType;
    }

    public void setResourceType(String resourceType) {
        this.resourceType = resourceType;
    }

    public String getScriptPath() {
        return script;
    }

    public void setScriptPath(String script) {
        this.script = script;
    }

    public Map<String, String> getDetails() {
        return details;
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

    public Date getRemoved() {
        return removed;
    }

    public void setRemoved(Date removed) {
        this.removed = removed;
    }
}
