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

import org.apache.cloudstack.api.ApiConstants;
import org.apache.cloudstack.api.BaseResponse;

import com.cloud.serializer.Param;
import com.google.gson.annotations.SerializedName;

@SuppressWarnings("unused")
public class ResourceDetailResponse extends BaseResponse{
    @SerializedName(ApiConstants.RESOURCE_ID)
    @Param(description = "ID of the resource")
    private String resourceId;

    @SerializedName(ApiConstants.RESOURCE_TYPE)
    @Param(description = "ID of the resource")
    private String resourceType;

    @SerializedName(ApiConstants.KEY)
    @Param(description = "key of the resource detail")
    private String name;


    @SerializedName(ApiConstants.VALUE)
    @Param(description = "value of the resource detail")
    private String value;

    
    @SerializedName(ApiConstants.FOR_DISPLAY)
    @Param(description = "if detail is returned to the regular user", since="4.3")
    private boolean forDisplay;
    
    public String getResourceId() {
        return resourceId;
    }

    public void setResourceId(String resourceId) {
        this.resourceId = resourceId;
    }

    public String getResourceType() {
        return resourceType;
    }

    public void setResourceType(String resourceType) {
        this.resourceType = resourceType;
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

    public void setForDisplay(boolean forDisplay) {
        this.forDisplay = forDisplay;
    }
}
