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
package org.apache.cloudstack.dedicated;

import com.cloud.dc.DedicatedResources;
import com.cloud.serializer.Param;

import com.google.gson.annotations.SerializedName;

import org.apache.cloudstack.api.BaseResponse;

public class DedicatedResourceResponse extends BaseResponse {
    @SerializedName("resourceid")
    @Param(description = "the ID of the resource")
    private String resourceId;

    @SerializedName("resourcename")
    @Param(description = "the name of the resource")
    private String resourceName;

    @SerializedName("resourcetype")
    @Param(description = "the type of the resource")
    private DedicatedResources.Type resourceType;

    public DedicatedResourceResponse(String resourceId, String resourceName, DedicatedResources.Type resourceType) {
        this.resourceId = resourceId;
        this.resourceName = resourceName;
        this.resourceType = resourceType;
    }
}
