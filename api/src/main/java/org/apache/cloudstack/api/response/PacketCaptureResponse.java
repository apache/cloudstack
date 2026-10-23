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

import org.apache.cloudstack.api.ApiConstants;
import org.apache.cloudstack.api.BaseResponse;

import com.cloud.serializer.Param;
import com.google.gson.annotations.SerializedName;

public class PacketCaptureResponse extends BaseResponse {

    @SerializedName(ApiConstants.NIC_ID)
    @Param(description = "the ID of the NIC")
    private String nicId;

    @SerializedName(ApiConstants.VIRTUAL_MACHINE_ID)
    @Param(description = "the ID of the Instance the NIC belongs to")
    private String virtualMachineId;

    @SerializedName(ApiConstants.VIRTUAL_MACHINE_NAME)
    @Param(description = "the internal name of the Instance the NIC belongs to")
    private String virtualMachineName;

    @SerializedName(ApiConstants.MAC_ADDRESS)
    @Param(description = "the MAC address of the NIC")
    private String macAddress;

    @SerializedName(ApiConstants.ENABLED)
    @Param(description = "true if packet capture is enabled on the NIC")
    private Boolean enabled;

    @SerializedName("running")
    @Param(description = "true if a packet capture is currently running on the host for the NIC")
    private Boolean running;

    public void setNicId(String nicId) {
        this.nicId = nicId;
    }

    public void setVirtualMachineId(String virtualMachineId) {
        this.virtualMachineId = virtualMachineId;
    }

    public void setVirtualMachineName(String virtualMachineName) {
        this.virtualMachineName = virtualMachineName;
    }

    public void setMacAddress(String macAddress) {
        this.macAddress = macAddress;
    }

    public void setEnabled(Boolean enabled) {
        this.enabled = enabled;
    }

    public void setRunning(Boolean running) {
        this.running = running;
    }

    public Boolean getEnabled() {
        return enabled;
    }

    public Boolean getRunning() {
        return running;
    }
}
