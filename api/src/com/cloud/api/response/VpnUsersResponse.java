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

@SuppressWarnings("unused")
public class VpnUsersResponse extends BaseResponse implements ControlledEntityResponse{
    @SerializedName(ApiConstants.ID) @Param(description="the vpn userID")
    private IdentityProxy id = new IdentityProxy("vpn_users");

    @SerializedName(ApiConstants.USERNAME) @Param(description="the username of the vpn user")
    private String userName;

    @SerializedName(ApiConstants.ACCOUNT) @Param(description="the account of the remote access vpn")
    private String accountName;

    @SerializedName(ApiConstants.DOMAIN_ID) @Param(description="the domain id of the account of the remote access vpn")
	private IdentityProxy domainId = new IdentityProxy("domain");

    @SerializedName(ApiConstants.DOMAIN) @Param(description="the domain name of the account of the remote access vpn")
	private String domainName;
    
    @SerializedName(ApiConstants.PROJECT_ID) @Param(description="the project id of the vpn")
    private IdentityProxy projectId = new IdentityProxy("projects");
    
    @SerializedName(ApiConstants.PROJECT) @Param(description="the project name of the vpn")
    private String projectName;
    

	public void setId(Long id) {
		this.id.setValue(id);
	}
	
	public void setUserName(String name) {
		this.userName = name;
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
	
    @Override
    public void setProjectId(Long projectId) {
        this.projectId.setValue(projectId);
    }

    @Override
    public void setProjectName(String projectName) {
        this.projectName = projectName;
    }	

}
