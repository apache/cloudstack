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

import com.cloud.serializer.Param;
import com.google.gson.annotations.SerializedName;

public class VpnUsersResponse extends BaseResponse {
    @SerializedName("id") @Param(description="the vpn userID")
    private Long id;

    @SerializedName("username") @Param(description="the username of the vpn user")
    private String userName;

    @SerializedName("account") @Param(description="the account of the remote access vpn")
    private String accountName;

    @SerializedName("domainid") @Param(description="the domain id of the account of the remote access vpn")
	private long domainId;

    @SerializedName("domainname") @Param(description="the domain name of the account of the remote access vpn")
	private String domainName;
    
	public String getAccountName() {
		return accountName;
	}

	public Long getId() {
		return id;
	}

	public void setId(Long id) {
		this.id = id;
	}

	public String getUserName() {
		return userName;
	}

	public void setUserName(String name) {
		this.userName = name;
	}

	public void setAccountName(String accountName) {
		this.accountName = accountName;
		
	}

	public void setDomainId(long domainId) {
		this.domainId = domainId;
		
	}

	public void setDomainName(String name) {
		this.domainName = name;		
	}

	public long getDomainId() {
		return domainId;
	}

	public String getDomainName() {
		return domainName;
	}

	public String toString() {
    	return "id: " + getId() + ", username: " + getUserName() + ", account: " + getAccountName() + ", domainid: " + getDomainId();
    }
}
