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

package com.cloud.hypervisor.external.provisioner.api;

import com.cloud.hypervisor.external.provisioner.vo.Extension;
import com.cloud.serializer.Param;
import com.google.gson.annotations.SerializedName;
import org.apache.cloudstack.api.ApiConstants;
import org.apache.cloudstack.api.BaseResponse;
import org.apache.cloudstack.api.EntityReference;

import java.util.Date;
import java.util.Map;

@EntityReference(value = Extension.class)
public class ExtensionResponse extends BaseResponse {

    @SerializedName(ApiConstants.ID)
    @Param(description = "ID of the extension")
    private String id;

    @SerializedName(ApiConstants.UUID)
    @Param(description = "UUID of the extension")
    private String uuid;

    @SerializedName(ApiConstants.NAME)
    @Param(description = "Name of the extension")
    private String name;

    @SerializedName(ApiConstants.TYPE)
    @Param(description = "Type of the extension")
    private String type;

    @SerializedName(ApiConstants.POD_ID)
    @Param(description = "Pod ID associated with the extension")
    private long podId;

    @SerializedName(ApiConstants.DETAILS)
    @Param(description = "the details of the network")
    private Map<String, String> details;

    @SerializedName(ApiConstants.SCRIPT)
    @Param(description = "the path of the script")
    private String script;

    @SerializedName(ApiConstants.CREATED)
    @Param(description = "Creation timestamp of the extension")
    private Date created;

    @SerializedName(ApiConstants.REMOVED)
    @Param(description = "Removal timestamp of the extension, if applicable")
    private Date removed;

    public ExtensionResponse(String name, String type, long podId, String uuid, Map<String, String> details) {
        this.name = name;
        this.type = type;
        this.podId = podId;
        this.uuid = uuid;
        this.details = details;
    }

    public String getUuid() {
        return uuid;
    }

    public void setUuid(String uuid) {
        this.uuid = uuid;
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

    public long getPodId() {
        return podId;
    }

    public void setPodId(long podId) {
        this.podId = podId;
    }

    public void setDetails(Map<String, String> details) {
        this.details = details;
    }

    public String getScriptPath() {
        return script;
    }

    public void setScriptPath(String script) {
        this.script = script;
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
