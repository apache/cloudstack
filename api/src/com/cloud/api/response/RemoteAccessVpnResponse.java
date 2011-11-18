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

@SuppressWarnings("unused")
public class RemoteAccessVpnResponse extends BaseResponse implements ControlledEntityResponse{
    
    @SerializedName(ApiConstants.PUBLIC_IP_ID) @Param(description="the public ip address of the vpn server")
    private IdentityProxy publicIpId = new IdentityProxy("user_ip_address");
    
    @SerializedName(ApiConstants.PUBLIC_IP) @Param(description="the public ip address of the vpn server")
    private String publicIp;
    
    @SerializedName("iprange") @Param(description="the range of ips to allocate to the clients")
    private String ipRange;

    @SerializedName("presharedkey") @Param(description="the ipsec preshared key")
    private String presharedKey;

    @SerializedName(ApiConstants.ACCOUNT) @Param(description="the account of the remote access vpn")
    private String accountName;
    
    @SerializedName(ApiConstants.PROJECT_ID) @Param(description="the project id of the vpn")
    private IdentityProxy projectId = new IdentityProxy("projects");
    
    @SerializedName(ApiConstants.PROJECT) @Param(description="the project name of the vpn")
    private String projectName;

    @SerializedName(ApiConstants.DOMAIN_ID) @Param(description="the domain id of the account of the remote access vpn")
	private IdentityProxy domainId = new IdentityProxy("domain");

    @SerializedName(ApiConstants.DOMAIN) @Param(description="the domain name of the account of the remote access vpn")
	private String domainName;
    
    @SerializedName(ApiConstants.STATE) @Param(description="the state of the rule")
    private String state;

	public void setPublicIp(String publicIp) {
		this.publicIp = publicIp;
	}

	public void setIpRange(String ipRange) {
		this.ipRange = ipRange;
	}

	public void setPresharedKey(String presharedKey) {
		this.presharedKey = presharedKey;
	}

	public void setAccountName(String accountName) {
		this.accountName = accountName;
	}

	public void setDomainId(Long domainId) {
		this.domainId.setValue(domainId);
	}

	public void setDomainName(String name) {
		this.domainName = name;		
	}

    public void setState(String state) {
        this.state = state;
    }

    public void setPublicIpId(Long publicIpId) {
        this.publicIpId.setValue(publicIpId);
    }

    @Override
    public void setProjectId(Long projectId) {
        this.projectId.setValue(projectId);
    }

    @Override
    public void setProjectName(String projectName) {
        this.projectName = projectName;
    }
    
}
