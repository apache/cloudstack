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
package com.cloud.server.api.response;

import com.google.gson.annotations.SerializedName;

import org.apache.cloudstack.api.ApiConstants;
import org.apache.cloudstack.api.response.NetworkDeviceResponse;

import com.cloud.serializer.Param;

public class NwDeviceDhcpResponse extends NetworkDeviceResponse {
    @SerializedName(ApiConstants.ZONE_ID)
    @Param(description = "Zone where to add PXE server")
    private Long zoneId;

    @SerializedName(ApiConstants.POD_ID)
    @Param(description = "Pod where to add PXE server")
    private Long podId;

    @SerializedName(ApiConstants.URL)
    @Param(description = "Ip of PXE server")
    private String url;

    @SerializedName(ApiConstants.TYPE)
    @Param(description = "Type of add PXE server")
    private String type;

    public void setZoneId(Long zoneId) {
        this.zoneId = zoneId;
    }

    public Long getZoneId() {
        return zoneId;
    }

    public void setPodId(Long podId) {
        this.podId = podId;
    }

    public Long getPodId() {
        return podId;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public String getUrl() {
        return url;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getType() {
        return type;
    }
}
