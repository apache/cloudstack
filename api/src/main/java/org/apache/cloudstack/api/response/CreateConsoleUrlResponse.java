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

public class CreateConsoleUrlResponse extends BaseResponse {

    @SerializedName(ApiConstants.RESULT)
    @Param(description = "true if the console endpoint is generated properly")
    private Boolean result;

    @SerializedName(ApiConstants.DETAILS)
    @Param(description = "details in case of an error")
    private String details;

    @SerializedName(ApiConstants.IP_ADDRESS)
    @Param(description = "the console ip address")
    private String ipAddress;

    @SerializedName(ApiConstants.PORT)
    @Param(description = "the console port")
    private String port;

    @SerializedName(ApiConstants.TOKEN)
    @Param(description = "the console token")
    private String token;

    @SerializedName(ApiConstants.URL)
    @Param(description = "the console url")
    private String url;

    public Boolean getResult() {
        return result;
    }

    public void setResult(Boolean result) {
        this.result = result;
    }

    public String getDetails() {
        return details;
    }

    public void setDetails(String details) {
        this.details = details;
    }

    public String getIpAddress() {
        return ipAddress;
    }

    public void setIpAddress(String ip) {
        this.ipAddress = ip;
    }

    public String getPort() {
        return port;
    }

    public void setPort(String port) {
        this.port = port;
    }

    public String getToken() {
        return token;
    }

    public void setToken(String token) {
        this.token = token;
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }
}
