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
import com.cloud.utils.IdentityProxy;
import com.cloud.serializer.Param;
import com.google.gson.annotations.SerializedName;

public class FirewallRuleResponse extends BaseResponse{
    @SerializedName(ApiConstants.ID) @Param(description="the ID of the port forwarding rule")
    private IdentityProxy id = new IdentityProxy("firewall_rules");

    @SerializedName(ApiConstants.PRIVATE_START_PORT) @Param(description = "the starting port of port forwarding rule's private port range")
    private String privateStartPort;

    @SerializedName(ApiConstants.PRIVATE_END_PORT) @Param(description = "the ending port of port forwarding rule's private port range")
    private String privateEndPort;

    @SerializedName(ApiConstants.PROTOCOL) @Param(description="the protocol of the port forwarding rule")
    private String protocol;

    @SerializedName(ApiConstants.PUBLIC_START_PORT) @Param(description="the starting port of port forwarding rule's public port range")
    private String publicStartPort;

    @SerializedName(ApiConstants.PUBLIC_END_PORT)  @Param(description = "the ending port of port forwarding rule's private port range")
    private String publicEndPort;

    @SerializedName(ApiConstants.VIRTUAL_MACHINE_ID) @Param(description="the VM ID for the port forwarding rule")
    private IdentityProxy virtualMachineId = new IdentityProxy("vm_instance");

    @SerializedName("virtualmachinename") @Param(description="the VM name for the port forwarding rule")
    private String virtualMachineName;

    @SerializedName("virtualmachinedisplayname") @Param(description="the VM display name for the port forwarding rule")
    private String virtualMachineDisplayName;
    
    @SerializedName(ApiConstants.IP_ADDRESS_ID) @Param(description="the public ip address id for the port forwarding rule")
    private IdentityProxy publicIpAddressId = new IdentityProxy("user_ip_address");

    @SerializedName(ApiConstants.IP_ADDRESS) @Param(description="the public ip address for the port forwarding rule")
    private String publicIpAddress;
    
    @SerializedName(ApiConstants.STATE) @Param(description="the state of the rule")
    private String state;

    @SerializedName(ApiConstants.CIDR_LIST) @Param(description="the cidr list to forward traffic from")
    private String cidrList;
    
    
    
    public Long getId() {
        return id.getValue();
    }

    public void setId(Long id) {
        this.id.setValue(id);
    }

    public String getPrivateStartPort() {
        return privateStartPort;
    }

    public String getPrivateEndPort() {
        return privateEndPort;
    }

    public void setPrivateStartPort(String privatePort) {
        this.privateStartPort = privatePort;
    }

    public void setPrivateEndPort(String privatePort) {
        this.privateEndPort = privatePort;
    }    
    
    public String getProtocol() {
        return protocol;
    }

    public void setProtocol(String protocol) {
        this.protocol = protocol;
    }

    public String getPublicStartPort() {
        return publicStartPort;
    }

    public String getPublicEndPort() {
        return publicEndPort;
    }
    
    public void setPublicStartPort(String publicPort) {
        this.publicStartPort = publicPort;
    }

    public void setPublicEndPort(String publicPort) {
        this.publicEndPort = publicPort;
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

    public Long getPublicIpAddressId() {
        return publicIpAddressId.getValue();
    }

    public void setPublicIpAddressId(Long publicIpAddressId) {
        this.publicIpAddressId.setValue(publicIpAddressId);
    }
    
    public String getCidrList() {
        return cidrList;
    }

    public void setCidrList(String cidrs) {
        this.cidrList = cidrs;
    }
    
}
