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

import org.apache.cloudstack.api.ApiConstants;
import org.apache.cloudstack.api.BaseResponse;

import com.cloud.serializer.Param;
import com.google.gson.annotations.SerializedName;

public class RouterHealthCheckResultsListResponse extends BaseResponse {
    @SerializedName(ApiConstants.ROUTER_ID)
    @Param(description = "the id of the router")
    private String routerId;

    @SerializedName(ApiConstants.ROUTER_HEALTH_CHECKS)
    @Param(description = "the id of the router")
    private List<RouterHealthCheckResultResponse> healthChecks;

    public String getRouterId() {
        return routerId;
    }

    public List<RouterHealthCheckResultResponse> getHealthChecks() {
        return healthChecks;
    }

    public void setRouterId(String routerId) {
        this.routerId = routerId;
    }

    public void setHealthChecks(List<RouterHealthCheckResultResponse> healthChecks) {
        this.healthChecks = healthChecks;
    }
}
