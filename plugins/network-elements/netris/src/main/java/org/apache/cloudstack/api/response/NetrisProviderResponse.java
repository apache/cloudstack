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

import com.cloud.network.netris.NetrisProvider;
import com.cloud.serializer.Param;
import com.google.gson.annotations.SerializedName;
import org.apache.cloudstack.api.ApiConstants;
import org.apache.cloudstack.api.BaseResponse;
import org.apache.cloudstack.api.EntityReference;

@EntityReference(value = {NetrisProvider.class})
public class NetrisProviderResponse extends BaseResponse {
    @SerializedName(ApiConstants.NAME)
    @Param(description = "Netris Provider name")
    private String name;

    @SerializedName(ApiConstants.ZONE_ID)
    @Param(description = "Zone ID to which the Netris Provider is associated with")
    private String zoneId;

    @SerializedName(ApiConstants.ZONE_NAME)
    @Param(description = "Zone name to which the Netris Provider is associated with")
    private String zoneName;

    @SerializedName(ApiConstants.HOST_NAME)
    @Param(description = "Netris Provider hostname or IP address")
    private String hostname;

    @SerializedName(ApiConstants.PORT)
    @Param(description = "Netris Provider port")
    private String port;

    @SerializedName(ApiConstants.USERNAME)
    @Param(description = "Netris Provider username")
    private String username;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getZoneId() {
        return zoneId;
    }

    public void setZoneId(String zoneId) {
        this.zoneId = zoneId;
    }

    public String getZoneName() {
        return zoneName;
    }

    public void setZoneName(String zoneName) {
        this.zoneName = zoneName;
    }

    public String getHostname() {
        return hostname;
    }

    public void setHostname(String hostname) {
        this.hostname = hostname;
    }

    public String getPort() {
        return port;
    }

    public void setPort(String port) {
        this.port = port;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }
}
