/**
 * Copyright (C) 2011 Citrix Systems, Inc.  All rights reserved
 * 
 * This software is licensed under the GNU General Public License v3 or later.
 * 
 * It is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or any later version.
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 * 
 */

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
