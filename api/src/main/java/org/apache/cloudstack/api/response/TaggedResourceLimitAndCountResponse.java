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

import com.cloud.configuration.Resource;
import com.cloud.serializer.Param;
import com.google.gson.annotations.SerializedName;

public class TaggedResourceLimitAndCountResponse extends BaseResponse {

    @SerializedName(ApiConstants.RESOURCE_TYPE)
    @Param(description = "Numerical value for the type of the resource. See the ResourceType for more information on these values.")
    private Integer resourceType;

    @SerializedName(ApiConstants.RESOURCE_TYPE_NAME)
    @Param(description = "Name for the type of the resource")
    private String resourceTypeName;

    @SerializedName(ApiConstants.TAG)
    @Param(description = "The tag for the resource type")
    private String tag;

    @SerializedName(ApiConstants.LIMIT)
    @Param(description = "The limit for the resource count for the type and tag for the owner")
    private Long limit;

    @SerializedName(ApiConstants.TOTAL)
    @Param(description = "The total amount of the resource for the type and tag that is used by the owner")
    private Long total;

    @SerializedName(ApiConstants.AVAILABLE)
    @Param(description = "The available amount of the resource for the type and tag that is available for the owner")
    private Long available;


    public void setResourceType(Resource.ResourceType resourceType) {
        this.resourceType = resourceType.getOrdinal();
        this.resourceTypeName = resourceType.getName();
    }

    public void setTag(String tag) {
        this.tag = tag;
    }

    public void setLimit(Long limit) {
        this.limit = limit;
    }

    public void setTotal(Long total) {
        this.total = total;
    }

    public void setAvailable(Long available) {
        this.available = available;
    }

    public Long getLimit() {
        return limit;
    }

    public Long getTotal() {
        return total;
    }

    public Long getAvailable() {
        return available;
    }
}
