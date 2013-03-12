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

public class IpForwardingRuleResponse extends BaseResponse {
    @SerializedName(ApiConstants.ID) @Param(description="the ID of the port forwarding rule")
    private String id;

    @SerializedName(ApiConstants.PROTOCOL) @Param(description="the protocol of the port forwarding rule")
    private String protocol;

    @SerializedName(ApiConstants.VIRTUAL_MACHINE_ID) @Param(description="the VM ID for the port forwarding rule")
    private String virtualMachineId;

    @SerializedName("virtualmachinename") @Param(description="the VM name for the port forwarding rule")
    private String virtualMachineName;

    @SerializedName("virtualmachinedisplayname") @Param(description="the VM display name for the port forwarding rule")
    private String virtualMachineDisplayName;

    @SerializedName(ApiConstants.IP_ADDRESS_ID) @Param(description="the public ip address id for the port forwarding rule")
    private Long publicIpAddressId;

    @SerializedName(ApiConstants.IP_ADDRESS) @Param(description="the public ip address for the port forwarding rule")
    private String publicIpAddress;

    @SerializedName(ApiConstants.START_PORT) @Param(description="the start port of the rule")
    private Integer startPort;

    @SerializedName(ApiConstants.END_PORT) @Param(description="the end port of the rule")
    private Integer endPort;

    @SerializedName(ApiConstants.STATE) @Param(description="state of the ip forwarding rule")
    private String state;

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getProtocol() {
        return protocol;
    }

    public void setProtocol(String protocol) {
        this.protocol = protocol;
    }

    public String getVirtualMachineId() {
        return virtualMachineId;
    }

    public void setVirtualMachineId(String virtualMachineId) {
        this.virtualMachineId = virtualMachineId;
    }

    public String getVirtualMachineName() {
        return virtualMachineName;
    }

    public void setVirtualMachineName(String virtualMachineName) {
        this.virtualMachineName = virtualMachineName;
    }

    public String getVirtualMachineDisplayName() {
        return virtualMachineDisplayName;
    }

    public void setVirtualMachineDisplayName(String virtualMachineDisplayName) {
        this.virtualMachineDisplayName = virtualMachineDisplayName;
    }

    public String getPublicIpAddress() {
        return publicIpAddress;
    }

    public void setPublicIpAddress(String publicIpAddress) {
        this.publicIpAddress = publicIpAddress;
    }

    public String getState() {
        return state;
    }

    public void setState(String state) {
        this.state = state;
    }

    public Integer getStartPort() {
        return startPort;
    }

    public void setStartPort(Integer startPort) {
        this.startPort = startPort;
    }

    public Integer getEndPort() {
        return endPort;
    }

    public void setEndPort(Integer endPort) {
        this.endPort = endPort;
    }

    public Long getPublicIpAddressId() {
        return publicIpAddressId;
    }

    public void setPublicIpAddressId(Long publicIpAddressId) {
        this.publicIpAddressId = publicIpAddressId;
    }
}
