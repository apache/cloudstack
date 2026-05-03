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

package org.apache.cloudstack.framework.extensions.api.response;

import org.apache.cloudstack.api.ApiConstants;
import org.apache.cloudstack.api.response.ExtractResponse;

import com.cloud.serializer.Param;
import com.google.gson.annotations.SerializedName;

public class DownloadExtensionResponse extends ExtractResponse {

    @SerializedName(ApiConstants.MANAGEMENT_SERVER_ID)
    @Param(description = "the management server ID of the host")
    private String managementServerId;

    @SerializedName(ApiConstants.MANAGEMENT_SERVER_NAME)
    @Param(description = "the management server name of the host")
    private String managementServerName;

    public String getManagementServerId() {
        return managementServerId;
    }

    public void setManagementServerId(String managementServerId) {
        this.managementServerId = managementServerId;
    }

    public String getManagementServerName() {
        return managementServerName;
    }

    public void setManagementServerName(String managementServerName) {
        this.managementServerName = managementServerName;
    }
}
