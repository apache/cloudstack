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
import org.apache.cloudstack.api.EntityReference;
import org.apache.cloudstack.gpu.GpuCard;

@EntityReference(value = GpuCard.class)
public class GpuCardResponse extends BaseResponse {
    @SerializedName(ApiConstants.ID)
    @Param(description = "the ID of the GPU card")
    protected String id;

    @SerializedName("deviceid")
    @Param(description = "the device ID of the GPU card")
    protected String deviceId;

    @SerializedName("devicename")
    @Param(description = "the device name of the GPU card")
    protected String deviceName;

    @SerializedName("name")
    @Param(description = "the display name of the GPU card")
    protected String name;

    @SerializedName("vendorname")
    @Param(description = "the vendor name of the GPU card")
    protected String vendorName;

    @SerializedName("vendorid")
    @Param(description = "the vendor ID of the GPU card")
    protected String vendorId;

    public GpuCardResponse(GpuCard gpuCard) {
        super("gpucard");
        id = gpuCard.getUuid();
        deviceId = gpuCard.getDeviceId();
        deviceName = gpuCard.getDeviceName();
        name = gpuCard.getName();
        vendorName = gpuCard.getVendorName();
        vendorId = gpuCard.getVendorId();
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getDeviceId() {
        return deviceId;
    }

    public void setDeviceId(String deviceId) {
        this.deviceId = deviceId;
    }

    public String getDeviceName() {
        return deviceName;
    }

    public void setDeviceName(String deviceName) {
        this.deviceName = deviceName;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getVendorName() {
        return vendorName;
    }

    public void setVendorName(String vendorName) {
        this.vendorName = vendorName;
    }

    public String getVendorId() {
        return vendorId;
    }

    public void setVendorId(String vendorId) {
        this.vendorId = vendorId;
    }
}
