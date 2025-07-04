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
import java.util.Map;

import org.apache.cloudstack.api.ApiConstants;
import org.apache.cloudstack.api.BaseResponse;
import org.apache.cloudstack.api.EntityReference;
import org.apache.cloudstack.api.Parameter;
import org.apache.cloudstack.extension.Extension;

import com.cloud.serializer.Param;
import com.google.gson.annotations.SerializedName;

@EntityReference(value = Extension.class)
public class ExtensionResponse extends BaseResponse {

    @SerializedName(ApiConstants.ID)
    @Param(description = "ID of the extension")
    private String id;

    @SerializedName(ApiConstants.NAME)
    @Param(description = "Name of the extension")
    private String name;

    @SerializedName(ApiConstants.DESCRIPTION)
    @Param(description = "Description of the extension")
    private String description;

    @SerializedName(ApiConstants.TYPE)
    @Param(description = "Type of the extension")
    private String type;

    @SerializedName(ApiConstants.PATH)
    @Param(description = "The path of the entry point fo the extension")
    private String path;

    @SerializedName(ApiConstants.PATH_READY)
    @Param(description = "True if the extension path is in ready state across management servers")
    private Boolean pathReady;

    @SerializedName(ApiConstants.IS_USER_DEFINED)
    @Param(description = "True if the extension is added by admin")
    private Boolean userDefined;

    @SerializedName(ApiConstants.ORCHESTRATOR_REQUIRES_PREPARE_VM)
    @Parameter(description = "Only honored when type is Orchestrator. Whether prepare VM is needed or not")
    private Boolean orchestratorRequiresPrepareVm;

    @SerializedName(ApiConstants.STATE)
    @Param(description = "The state of the extension")
    private String state;

    @SerializedName(ApiConstants.DETAILS)
    @Param(description = "The details of the extension")
    private Map<String, String> details;

    @SerializedName(ApiConstants.RESOURCES)
    @Param(description = "List of resources to which extension is registered to", responseObject = ExtensionResourceResponse.class)
    private List<ExtensionResourceResponse> resources;

    @SerializedName(ApiConstants.CREATED)
    @Param(description = "Creation timestamp of the extension")
    private Date created;

    @SerializedName(ApiConstants.REMOVED)
    @Param(description = "Removal timestamp of the extension, if applicable")
    private Date removed;

    public ExtensionResponse(String id, String name, String description, String type) {
        this.id = id;
        this.name = name;
        this.description = description;
        this.type = type;
    }

    public String getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getDescription() {
        return description;
    }

    public String getType() {
        return type;
    }

    public String getPath() {
        return path;
    }

    public Boolean isPathReady() {
        return pathReady;
    }

    public Boolean isUserDefined() {
        return userDefined;
    }

    public Boolean isOrchestratorRequiresPrepareVm() {
        return orchestratorRequiresPrepareVm;
    }

    public String getState() {
        return state;
    }

    public Map<String, String> getDetails() {
        return details;
    }

    public void setPath(String path) {
        this.path = path;
    }

    public void setPathReady(Boolean pathReady) {
        this.pathReady = pathReady;
    }

    public void setUserDefined(Boolean userDefined) {
        this.userDefined = userDefined;
    }

    public void setOrchestratorRequiresPrepareVm(Boolean orchestratorRequiresPrepareVm) {
        this.orchestratorRequiresPrepareVm = orchestratorRequiresPrepareVm;
    }

    public void setState(String state) {
        this.state = state;
    }

    public void setDetails(Map<String, String> details) {
        this.details = details;
    }

    public List<ExtensionResourceResponse> getResources() {
        return resources;
    }

    public void setResources(List<ExtensionResourceResponse> resources) {
        this.resources = resources;
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
