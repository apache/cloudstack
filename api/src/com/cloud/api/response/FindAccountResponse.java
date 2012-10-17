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
package com.cloud.api.response;

import java.util.List;
import java.util.Map;

import com.cloud.api.ApiConstants;
import com.cloud.serializer.Param;
import com.cloud.utils.IdentityProxy;
import com.google.gson.annotations.SerializedName;

@SuppressWarnings("unused")
public class FindAccountResponse extends BaseResponse {
    @SerializedName(ApiConstants.ID) @Param(description="the id of the account")
    private IdentityProxy id = new IdentityProxy("account");

    @SerializedName(ApiConstants.NAME) @Param(description="the name of the account")
    private String name;

    @SerializedName(ApiConstants.ACCOUNT_TYPE) @Param(description="account type (admin, domain-admin, user)")
    private Short accountType;

    @SerializedName(ApiConstants.DOMAIN_ID) @Param(description="id of the Domain the account belongs too")
    private IdentityProxy domainId = new IdentityProxy("domain");

    @SerializedName(ApiConstants.DEFAULT_ZONE_ID) @Param(description="the default zone of the account")
    private IdentityProxy defaultZoneId = new IdentityProxy("data_center");

    @SerializedName(ApiConstants.STATE) @Param(description="the state of the account")
    private String state;

    @SerializedName(ApiConstants.NETWORK_DOMAIN) @Param(description="the network domain")
    private String networkDomain;
    
    @SerializedName(ApiConstants.ACCOUNT_DETAILS) @Param(description="details for the account")
    private Map<String, String> details;

    @SerializedName("regionId") @Param(description="source region id of the user")
    private int regionId;
    
    public void setId(Long id) {
        this.id.setValue(id);
    }

    public void setName(String name) {
        this.name = name;
    }

    public void setAccountType(Short accountType) {
        this.accountType = accountType;
    }

    public void setDomainId(Long domainId) {
        this.domainId.setValue(domainId);
    }

    public void setState(String state) {
        this.state = state;
    }

    public void setNetworkDomain(String networkDomain) {
        this.networkDomain = networkDomain;
    }
    
    public void setDetails(Map<String, String> details) {
    	this.details = details;
    }

    public void setDefaultZone(Long defaultZoneId) {
    	this.defaultZoneId.setValue(defaultZoneId);
    }
    
	public int getRegionId() {
		return regionId;
	}

	public void setRegionId(int regionId) {
		this.regionId = regionId;
	}
}
