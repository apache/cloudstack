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

import org.apache.cloudstack.acl.RoleType;
import org.apache.cloudstack.api.ApiConstants;
import org.apache.cloudstack.api.BaseResponse;

import com.cloud.serializer.Param;
import com.google.gson.annotations.SerializedName;

public class ApiParameterResponse extends BaseResponse {
    @SerializedName(ApiConstants.NAME)
    @Param(description = "the name of the api parameter")
    private String name;

    @SerializedName(ApiConstants.DESCRIPTION)
    @Param(description = "description of the api parameter")
    private String description;

    @SerializedName(ApiConstants.TYPE)
    @Param(description = "parameter type")
    private String type;

    @SerializedName(ApiConstants.LENGTH)
    @Param(description = "length of the parameter")
    private int length;

    @SerializedName(ApiConstants.REQUIRED)
    @Param(description = "true if this parameter is required for the api request")
    private Boolean required;

    @SerializedName(ApiConstants.SINCE)
    @Param(description = "version of CloudStack the api was introduced in")
    private String since;

    @SerializedName("related")
    @Param(description = "comma separated related apis to get the parameter")
    private String related;

    private transient List<RoleType> authorizedRoleTypes = null;

    public ApiParameterResponse() {
    }

    public void setName(String name) {
        this.name = name;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public void setType(String type) {
        this.type = type;
    }

    public void setLength(int length) {
        this.length = length;
    }

    public void setRequired(Boolean required) {
        this.required = required;
    }

    public void setSince(String since) {
        this.since = since;
    }

    public String getRelated() {
        return related;
    }

    public void setRelated(String related) {
        this.related = related;
    }

    public void setAuthorizedRoleTypes(List<RoleType> authorizedRoleTypes) {
        this.authorizedRoleTypes = authorizedRoleTypes;
    }

    public List<RoleType> getAuthorizedRoleTypes() {
        return authorizedRoleTypes;
    }
}
