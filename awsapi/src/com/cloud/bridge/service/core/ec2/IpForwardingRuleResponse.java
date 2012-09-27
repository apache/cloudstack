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

package com.cloud.bridge.service.core.ec2;

public class IpForwardingRuleResponse /* extends BaseResponse */ {

    // @SerializedName(ApiConstants.ID) @Param(description="the ID of the port forwarding rule")
    private Long id;

    // @SerializedName(ApiConstants.PROTOCOL) @Param(description="the protocol of the port forwarding rule")
    private String protocol;

    // @SerializedName(ApiConstants.VIRTUAL_MACHINE_ID) @Param(description="the VM ID for the port forwarding rule")
    private Long virtualMachineId;

    // @SerializedName("virtualmachinename") @Param(description="the VM name for the port forwarding rule")
    private String virtualMachineName;

    // @SerializedName("virtualmachinedisplayname") @Param(description="the VM display name for the port forwarding rule")
    private String virtualMachineDisplayName;

    // @SerializedName("ipaddress") @Param(description="the public ip address for the port forwarding rule")
    private String publicIpAddress;

    // @SerializedName("state") @Param(description="state of the ip forwarding rule")
    private String state;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getProtocol() {
        return protocol;
    }

    public void setProtocol(String protocol) {
        this.protocol = protocol;
    }

    public Long getVirtualMachineId() {
        return virtualMachineId;
    }

    public void setVirtualMachineId(Long virtualMachineId) {
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
}
