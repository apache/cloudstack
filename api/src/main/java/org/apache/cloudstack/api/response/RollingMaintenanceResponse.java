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
import com.google.gson.annotations.SerializedName;
import org.apache.cloudstack.api.BaseResponse;

import java.util.List;

public class RollingMaintenanceResponse extends BaseResponse {

    @SerializedName("success")
    @Param(description = "indicates if the rolling maintenance operation was successful")
    private Boolean success;

    @SerializedName("details")
    @Param(description = "in case of failure, details are displayed")
    private String details;

    @SerializedName("hostsupdated")
    @Param(description = "the hosts updated", responseObject = RollingMaintenanceHostUpdatedResponse.class)
    private List<RollingMaintenanceHostUpdatedResponse> updatedHosts;

    @SerializedName("hostsskipped")
    @Param(description = "the hosts skipped", responseObject = RollingMaintenanceHostSkippedResponse.class)
    private List<RollingMaintenanceHostSkippedResponse> skippedHosts;

    public RollingMaintenanceResponse(Boolean success, String details) {
        this.success = success;
        this.details = details;
    }

    public Boolean getSuccess() {
        return success;
    }

    public void setSuccess(Boolean success) {
        this.success = success;
    }

    public String getDetails() {
        return details;
    }

    public void setDetails(String details) {
        this.details = details;
    }

    public List<RollingMaintenanceHostUpdatedResponse> getUpdatedHosts() {
        return updatedHosts;
    }

    public void setUpdatedHosts(List<RollingMaintenanceHostUpdatedResponse> updatedHosts) {
        this.updatedHosts = updatedHosts;
    }

    public List<RollingMaintenanceHostSkippedResponse> getSkippedHosts() {
        return skippedHosts;
    }

    public void setSkippedHosts(List<RollingMaintenanceHostSkippedResponse> skippedHosts) {
        this.skippedHosts = skippedHosts;
    }
}
