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

import org.apache.cloudstack.extension.ExtensionResourceMap;
import com.cloud.serializer.Param;
import com.google.gson.annotations.SerializedName;
import org.apache.cloudstack.api.ApiConstants;
import org.apache.cloudstack.api.BaseResponse;
import org.apache.cloudstack.api.EntityReference;

import java.util.Date;
import java.util.Map;

@EntityReference(value = ExtensionResourceMap.class)
public class ExtensionResourceResponse extends BaseResponse {

    @SerializedName(ApiConstants.ID)
    @Param(description = "ID of the resource associated with the extension")
    private String id;

    @SerializedName(ApiConstants.NAME)
    @Param(description = "Name of the resource associated with this mapping")
    private String name;

    @SerializedName(ApiConstants.TYPE)
    @Param(description = "Type of the resource")
    private String type;

    @SerializedName(ApiConstants.DETAILS)
    @Param(description = "the details of the resource map")
    private Map<String, String> details;

    @SerializedName(ApiConstants.CREATED)
    @Param(description = "Creation timestamp of the mapping")
    private Date created;

    public ExtensionResourceResponse() {
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
}
