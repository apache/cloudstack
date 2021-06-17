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

import com.cloud.serializer.Param;
import com.cloud.server.ResourceTag;
import com.google.gson.annotations.SerializedName;
import org.apache.cloudstack.api.ApiConstants;
import org.apache.cloudstack.api.BaseResponse;

public class ResourceIconResponse extends BaseResponse {
    @SerializedName(ApiConstants.RESOURCE_TYPE)
    @Param(description = "resource type")
    private ResourceTag.ResourceObjectType resourceType;

    @SerializedName(ApiConstants.RESOURCE_ID)
    @Param(description = "id of the resource")
    private String resourceId;

    @SerializedName(ApiConstants.BASE64_IMAGE)
    @Param(description = "base64 representation of resource icon")
    private String image;

    public ResourceTag.ResourceObjectType getResourceType() {
        return resourceType;
    }

    public void setResourceType(ResourceTag.ResourceObjectType resourceType) {
        this.resourceType = resourceType;
    }

    public String getResourceId() {
        return resourceId;
    }

    public void setResourceId(String resourceId) {
        this.resourceId = resourceId;
    }

    public String getImage() {
        return image;
    }

    public void setImage(String image) {
        this.image = image;
    }
}
