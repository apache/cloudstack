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

package org.apache.cloudstack.logsws.api.response;

import org.apache.cloudstack.api.ApiConstants;
import org.apache.cloudstack.api.BaseResponse;

import com.cloud.serializer.Param;
import com.google.gson.annotations.SerializedName;

public class LogsWebSessionWebSocketResponse extends BaseResponse {
    @SerializedName(ApiConstants.MANAGEMENT_SERVER_ID)
    @Param(description = "The ID of the management for this websocket")
    private String managementServerId;

    @SerializedName(ApiConstants.MANAGEMENT_SERVER_NAME)
    @Param(description = "The name of the management for this websocket")
    private String managementServerName;

    @SerializedName("host")
    @Param(description = "the websocket host")
    private String host;

    @SerializedName(ApiConstants.PORT)
    @Param(description = "the websocket port")
    private Integer port;

    @SerializedName(ApiConstants.PATH)
    @Param(description = "the websocket path")
    private String path;

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

    public String getHost() {
        return host;
    }

    public void setHost(String host) {
        this.host = host;
    }

    public Integer getPort() {
        return port;
    }

    public void setPort(Integer port) {
        this.port = port;
    }

    public String getPath() {
        return path;
    }

    public void setPath(String path) {
        this.path = path;
    }
}
