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

import com.google.gson.annotations.SerializedName;

import org.apache.cloudstack.api.ApiConstants;
import org.apache.cloudstack.api.BaseResponse;

import com.cloud.serializer.Param;

public class ConfigurationResponse extends BaseResponse {
    @SerializedName(ApiConstants.CATEGORY)
    @Param(description = "the category of the configuration")
    private String category;

    @SerializedName(ApiConstants.NAME)
    @Param(description = "the name of the configuration")
    private String name;

    @SerializedName(ApiConstants.VALUE)
    @Param(description = "the value of the configuration")
    private String value;

    @SerializedName(ApiConstants.SCOPE)
    @Param(description = "scope(zone/cluster/pool/account) of the parameter that needs to be updated")
    private String scope;

    @SerializedName(ApiConstants.ID)
    @Param(description = "the value of the configuration")
    private Long id;

    @SerializedName(ApiConstants.DESCRIPTION)
    @Param(description = "the description of the configuration")
    private String description;

    @SerializedName(ApiConstants.IS_DYNAMIC)
    @Param(description = "true if the configuration is dynamic")
    private boolean isDynamic;

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getValue() {
        return value;
    }

    public void setValue(String value) {
        this.value = value;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getScope() {
        return scope;
    }

    public void setScope(String scope) {
        this.scope = scope;
    }

    public boolean isDynamic() {
        return isDynamic;
    }

    public void setIsDynamic(boolean isDynamic) {
        this.isDynamic = isDynamic;
    }

}
