//Licensed to the Apache Software Foundation (ASF) under one
//or more contributor license agreements.  See the NOTICE file
//distributed with this work for additional information
//regarding copyright ownership.  The ASF licenses this file
//to you under the Apache License, Version 2.0 (the
//"License"); you may not use this file except in compliance
//with the License.  You may obtain a copy of the License at
//
//http://www.apache.org/licenses/LICENSE-2.0
//
//Unless required by applicable law or agreed to in writing,
//software distributed under the License is distributed on an
//"AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
//KIND, either express or implied.  See the License for the
//specific language governing permissions and limitations
//under the License.
package org.apache.cloudstack.api.response;

import java.math.BigDecimal;

import com.google.gson.annotations.SerializedName;

import org.apache.cloudstack.api.ApiConstants;
import org.apache.cloudstack.api.BaseResponse;

import com.cloud.serializer.Param;

public class QuotaStatementItemResourceResponse extends BaseResponse {

    @SerializedName("quotaconsumed")
    @Param(description = "Quota consumed.")
    private BigDecimal quotaUsed;

    @SerializedName(ApiConstants.RESOURCE_ID)
    @Param(description = "Resources's id.")
    private String resourceId;

    @SerializedName(ApiConstants.DISPLAY_NAME)
    @Param(description = "Resource's display name.")
    private String displayName;

    @SerializedName(ApiConstants.REMOVED)
    @Param(description = "Indicates if the resource is removed or active.")
    private boolean removed;

    public void setQuotaUsed(BigDecimal quotaUsed) {
        this.quotaUsed = quotaUsed;
    }

    public void setResourceId(String resourceId) {
        this.resourceId = resourceId;
    }

    public void setDisplayName(String displayName) {
        this.displayName = displayName;
    }

    public void setRemoved(boolean removed) {
        this.removed = removed;
    }
}
