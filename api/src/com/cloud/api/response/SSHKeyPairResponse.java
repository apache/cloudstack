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
import com.cloud.serializer.Param;
import com.google.gson.annotations.SerializedName;

public class SSHKeyPairResponse extends BaseResponse {
	
    @SerializedName(ApiConstants.NAME) @Param(description="Name of the keypair")
    private String name;

    @SerializedName("fingerprint") @Param(description="Fingerprint of the public key")
    private String fingerprint;
    
    @SerializedName("privatekey") @Param(description="Private key")
    private String privateKey;
    
    @SerializedName(ApiConstants.ACCOUNT) @Param(description="the account associated with the ssh key")
    private String accountName;

    @SerializedName(ApiConstants.DOMAIN_ID) @Param(description="the ID of the domain in which ssh key exists")
    private Long domainId;

    public SSHKeyPairResponse() {}
    
    public SSHKeyPairResponse(String name, String fingerprint) {
    	this(name, fingerprint, null);
    }
    
    public SSHKeyPairResponse(String name, String fingerprint, String privateKey) {
    	this.name = name;
    	this.fingerprint = fingerprint;
    	this.privateKey = privateKey;
    }
    

	public void setName(String name) {
		this.name = name;
	}

	public void setFingerprint(String fingerprint) {
		this.fingerprint = fingerprint;
	}

	public void setPrivateKey(String privateKey) {
		this.privateKey = privateKey;
	}

    public void setAccountName(String accountName) {
        this.accountName = accountName;
    }

    public void setDomainId(Long domainId) {
        this.domainId = domainId;
    }
    
}
