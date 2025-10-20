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

import com.google.gson.annotations.SerializedName;

import org.apache.cloudstack.api.ApiConstants;
import org.apache.cloudstack.api.BaseResponse;
import org.apache.cloudstack.api.EntityReference;

import com.cloud.serializer.Param;
import com.cloud.storage.GuestOsCategory;

@EntityReference(value = GuestOsCategory.class)
public class GuestOSCategoryResponse extends BaseResponse implements SetResourceIconResponse {
    @SerializedName(ApiConstants.ID)
    @Param(description = "the ID of the OS category")
    private String id;

    @SerializedName(ApiConstants.NAME)
    @Param(description = "the name of the OS category")
    private String name;

    @SerializedName(ApiConstants.IS_FEATURED)
    @Param(description = "Whether the OS category is featured", since = "4.21.0")
    private Boolean featured;

    @SerializedName(ApiConstants.RESOURCE_ICON)
    @Param(description = "Base64 string representation of the resource icon", since = "4.21.0")
    private ResourceIconResponse resourceIconResponse;

    @SerializedName(ApiConstants.CREATED)
    @Param(description = "Date when the OS category was created." )
    private Date created;

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

    public void setFeatured(Boolean featured) {
        this.featured = featured;
    }

    @Override
    public void setResourceIconResponse(ResourceIconResponse resourceIconResponse) {
        this.resourceIconResponse = resourceIconResponse;
    }

    public void setCreated(Date created) {
        this.created = created;
    }
}
