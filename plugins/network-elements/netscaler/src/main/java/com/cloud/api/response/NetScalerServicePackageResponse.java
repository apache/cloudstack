// Licensed to the Apache Software Foundation (ASF) under one or more
// contributor license agreements.  See the NOTICE file distributed with
// this work for additional information regarding copyright ownership.
// The ASF licenses this file to You under the Apache License, Version 2.0
// (the "License"); you may not use this file except in compliance with
// the License.  You may obtain a copy of the License at
//
//   http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.

package com.cloud.api.response;

import com.google.gson.annotations.SerializedName;

import org.apache.cloudstack.api.ApiConstants;
import org.apache.cloudstack.api.BaseResponse;

import com.cloud.network.NetScalerServicePackageVO;
import com.cloud.serializer.Param;

public class NetScalerServicePackageResponse extends BaseResponse {

    @SerializedName(ApiConstants.ID)
    @Param(description = "Service Package UUID")
    private String id;

    @SerializedName(ApiConstants.NAME)
    @Param(description = "Service Package Name")
    private String name;

    @SerializedName(ApiConstants.DESCRIPTION)
    @Param(description = "Description of Service Package")
    private String description;

    public NetScalerServicePackageResponse() {
    }

    public NetScalerServicePackageResponse(NetScalerServicePackageVO servicePackage) {
        this.id = servicePackage.getUuid();
        this.name = servicePackage.getName();
        this.description = servicePackage.getDescription();
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

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

}
