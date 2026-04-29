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

@SuppressWarnings("unused")
public class VnfNicResponse {
    @SerializedName(ApiConstants.DEVICE_ID)
    @Param(description = "Device id of the NIC")
    private long deviceId;

    @SerializedName(ApiConstants.NAME)
    @Param(description = "Name of the NIC")
    private String name;

    @SerializedName(ApiConstants.REQUIRED)
    @Param(description = "True if the NIC is required. False if optional")
    private Boolean required;

    @SerializedName(ApiConstants.MANAGEMENT)
    @Param(description = "True if the NIC is a management interface. False otherwise")
    private Boolean management;

    @SerializedName(ApiConstants.DESCRIPTION)
    @Param(description = "Description of the NIC")
    private String description;

    @SerializedName(ApiConstants.NETWORK_ID)
    @Param(description = "Network id of the NIC")
    private String networkId;

    @SerializedName(ApiConstants.NETWORK_NAME)
    @Param(description = "Network name of the NIC")
    private String networkName;

    public void setDeviceId(long deviceId) {
        this.deviceId = deviceId;
    }

    public void setName(String name) {
        this.name = name;
    }

    public void setRequired(Boolean required) {
        this.required = required;
    }

    public void setManagement(Boolean management) {
        this.management = management;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public void setNetworkId(String networkId) {
        this.networkId = networkId;
    }

    public void setNetworkName(String networkName) {
        this.networkName = networkName;
    }

    public VnfNicResponse() {
    }

    public VnfNicResponse(long deviceId, String name, Boolean required, Boolean management, String description) {
        this.deviceId = deviceId;
        this.name = name;
        this.required = required;
        this.management = management;
        this.description = description;
    }

    public long getDeviceId() {
        return deviceId;
    }

    public String getName() {
        return name;
    }

    public Boolean isRequired() {
        return required;
    }

    public Boolean isManagement() {
        return management;
    }

    public String getDescription() {
        return description;
    }

    public String getNetworkId() {
        return networkId;
    }

    public String getNetworkName() {
        return networkName;
    }
}
