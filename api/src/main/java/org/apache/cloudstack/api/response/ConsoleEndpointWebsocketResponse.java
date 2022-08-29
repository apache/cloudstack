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
import org.apache.cloudstack.api.ApiConstants;
import org.apache.cloudstack.api.BaseResponse;

public class ConsoleEndpointWebsocketResponse extends BaseResponse {

    public ConsoleEndpointWebsocketResponse() {
    }

    @SerializedName(ApiConstants.TOKEN)
    @Param(description = "the console websocket token")
    private String token;

    @SerializedName("host")
    @Param(description = "the console websocket host")
    private String host;

    @SerializedName(ApiConstants.PORT)
    @Param(description = "the console websocket port")
    private String port;

    @SerializedName(ApiConstants.PATH)
    @Param(description = "the console websocket path")
    private String path;

    @SerializedName("extra")
    @Param(description = "the console websocket extra field for validation (if enabled)")
    private String extra;

    public String getToken() {
        return token;
    }

    public void setToken(String token) {
        this.token = token;
    }

    public String getHost() {
        return host;
    }

    public void setHost(String host) {
        this.host = host;
    }

    public String getPort() {
        return port;
    }

    public void setPort(String port) {
        this.port = port;
    }

    public String getPath() {
        return path;
    }

    public void setPath(String path) {
        this.path = path;
    }

    public String getExtra() {
        return extra;
    }

    public void setExtra(String extra) {
        this.extra = extra;
    }
}
