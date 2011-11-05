/**
 *  Copyright (C) 2010 Cloud.com, Inc.  All rights reserved.
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
package com.cloud.api.response;

import com.cloud.api.ApiConstants;
import com.cloud.api.IdentityProxy;
import com.cloud.serializer.Param;
import com.google.gson.annotations.SerializedName;

public class IpForwardingRuleResponse extends BaseResponse {
    @SerializedName(ApiConstants.ID) @Param(description="the ID of the port forwarding rule")
    private IdentityProxy id = new IdentityProxy("firewall_rules");

    @SerializedName(ApiConstants.PROTOCOL) @Param(description="the protocol of the port forwarding rule")
    private String protocol;

    @SerializedName(ApiConstants.VIRTUAL_MACHINE_ID) @Param(description="the VM ID for the port forwarding rule")
    private IdentityProxy virtualMachineId = new IdentityProxy("vm_instance");

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
    
    public Long getId() {
        return id.getValue();
    }

    public void setId(Long id) {
        this.id.setValue(id);
    }

    public String getProtocol() {
        return protocol;
    }

    public void setProtocol(String protocol) {
        this.protocol = protocol;
    }

    public Long getVirtualMachineId() {
        return virtualMachineId.getValue();
    }

    public void setVirtualMachineId(Long virtualMachineId) {
        this.virtualMachineId.setValue(virtualMachineId);
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
