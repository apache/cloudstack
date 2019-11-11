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

public class DomainRouterHealthCheckResultsResponse extends BaseResponse {
    @SerializedName("routerId")
    @Param(description = "the id of the router")
    private String routerId;

    @SerializedName(ApiConstants.NAME)
    @Param(description = "the name of the router")
    private String name;

    @SerializedName(ApiConstants.RESULT)
    @Param(description = "result of management server's attempt to fetch data.")
    private String result;

    @SerializedName(ApiConstants.DETAILS)
    @Param(description = "detailed data fetched from management server")
    private String details;

    public DomainRouterHealthCheckResultsResponse(String objectName) {
        super(objectName);
    }

    public String getRouterId() {
        return routerId;
    }

    public String getName() {
        return name;
    }

    public String getResult() {
        return result;
    }

    public String getDetails() {
        return details;
    }

    public void setRouterId(String id) {
        this.routerId = id;
    }

    public void setName(String name) {
        this.name = name;
    }

    public void setResult(String result) {
        this.result = result;
    }

    public void setDetails(String details) {
        this.details = details;
    }
}
