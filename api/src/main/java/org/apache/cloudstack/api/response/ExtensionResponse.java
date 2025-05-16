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

import com.cloud.extension.Extension;
import com.cloud.serializer.Param;
import com.google.gson.annotations.SerializedName;
import org.apache.cloudstack.api.ApiConstants;
import org.apache.cloudstack.api.BaseResponse;
import org.apache.cloudstack.api.EntityReference;

import java.util.Date;
import java.util.List;
import java.util.Map;

@EntityReference(value = Extension.class)
public class ExtensionResponse extends BaseResponse {

    @SerializedName(ApiConstants.ID)
    @Param(description = "ID of the extension")
    private String id;

    @SerializedName(ApiConstants.NAME)
    @Param(description = "Name of the extension")
    private String name;

    @SerializedName(ApiConstants.TYPE)
    @Param(description = "Type of the extension")
    private String type;

    @SerializedName(ApiConstants.SCRIPT)
    @Param(description = "the path of the script")
    private String script;

    @SerializedName(ApiConstants.DETAILS)
    @Param(description = "the details of the extension")
    private Map<String, String> details;

    @SerializedName(ApiConstants.EXTENSION_RESOURCE_ID)
    @Param(description = "List of resources to which extension is registered to", responseObject = ExtensionResourceMapResponse.class)
    private List<ExtensionResourceMapResponse> resources;

    @SerializedName(ApiConstants.CREATED)
    @Param(description = "Creation timestamp of the extension")
    private Date created;

    @SerializedName(ApiConstants.REMOVED)
    @Param(description = "Removal timestamp of the extension, if applicable")
    private Date removed;

    public ExtensionResponse(String name, String type, String uuid, Map<String, String> details) {
        this.name = name;
        this.type = type;
        this.id = uuid;
        this.details = details;
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

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getScriptPath() {
        return script;
    }

    public void setScriptPath(String script) {
        this.script = script;
    }

    public void setDetails(Map<String, String> details) {
        this.details = details;
    }

    public List<ExtensionResourceMapResponse> getResources() {
        return resources;
    }

    public void setResources(List<ExtensionResourceMapResponse> resources) {
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
