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
import org.apache.cloudstack.api.EntityReference;
import org.apache.cloudstack.region.Region;

import com.cloud.serializer.Param;

@EntityReference(value = Region.class)
public class RegionResponse extends BaseResponse {
    @SerializedName(ApiConstants.ID)
    @Param(description = "the ID of the region")
    private Integer id;

    @SerializedName(ApiConstants.NAME)
    @Param(description = "the name of the region")
    private String name;

    @SerializedName(ApiConstants.END_POINT)
    @Param(description = "the end point of the region")
    private String endPoint;

    @SerializedName("gslbserviceenabled")
    @Param(description = "true if GSLB service is enabled in the region, false otherwise")
    private boolean gslbServiceEnabled;

    @SerializedName("portableipserviceenabled")
    @Param(description = "true if security groups support is enabled, false otherwise")
    private boolean portableipServiceEnabled;

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getEndPoint() {
        return endPoint;
    }

    public void setEndPoint(String endPoint) {
        this.endPoint = endPoint;
    }

    public void setGslbServiceEnabled(boolean gslbServiceEnabled) {
        this.gslbServiceEnabled = gslbServiceEnabled;
    }

    public void setPortableipServiceEnabled(boolean portableipServiceEnabled) {
        this.portableipServiceEnabled = portableipServiceEnabled;
    }
}
