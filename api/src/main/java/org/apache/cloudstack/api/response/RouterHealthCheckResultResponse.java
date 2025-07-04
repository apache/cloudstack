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

import org.apache.cloudstack.api.ApiConstants;
import org.apache.cloudstack.api.BaseResponse;

import com.cloud.serializer.Param;
import com.google.gson.annotations.SerializedName;

public class RouterHealthCheckResultResponse extends BaseResponse {
    @SerializedName(ApiConstants.ROUTER_CHECK_NAME)
    @Param(description = "the name of the health check on the router")
    private String checkName;

    @SerializedName(ApiConstants.ROUTER_CHECK_TYPE)
    @Param(description = "the type of the health check - basic or advanced")
    private String checkType;

    @SerializedName(ApiConstants.SUCCESS)
    @Param(description = "result of the health check")
    private boolean result;

    @SerializedName(ApiConstants.LAST_UPDATED)
    @Param(description = "the date this VPC was created")
    private Date lastUpdated;

    @SerializedName(ApiConstants.DETAILS)
    @Param(description = "detailed response generated on running health check")
    private String details;

    public String getCheckName() {
        return checkName;
    }

    public String getCheckType() {
        return checkType;
    }

    public boolean getResult() {
        return result;
    }

    public Date getLastUpdated() {
        return lastUpdated;
    }

    public String getDetails() {
        return details;
    }

    public void setCheckName(String checkName) {
        this.checkName = checkName;
    }

    public void setCheckType(String checkType) {
        this.checkType = checkType;
    }

    public void setResult(boolean result) {
        this.result = result;
    }

    public void setLastUpdated(Date lastUpdated) {
        this.lastUpdated = lastUpdated;
    }

    public void setDetails(String details) {
        this.details = details;
    }
}
