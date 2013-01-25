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

import java.util.Map;

import org.apache.cloudstack.api.ApiConstants;
import org.apache.cloudstack.api.BaseResponse;
import org.apache.cloudstack.api.EntityReference;

import com.cloud.serializer.Param;
import com.cloud.user.Account;
import com.google.gson.annotations.SerializedName;

@SuppressWarnings("unused")
@EntityReference(value = Account.class)
public class FindAccountResponse extends BaseResponse {
    @SerializedName(ApiConstants.ID) @Param(description="the id of the account")
    private String id;

    @SerializedName(ApiConstants.NAME) @Param(description="the name of the account")
    private String name;

    @SerializedName(ApiConstants.ACCOUNT_TYPE) @Param(description="account type (admin, domain-admin, user)")
    private Short accountType;

    @SerializedName(ApiConstants.DOMAIN_ID) @Param(description="id of the Domain the account belongs too")
    private String domainId;

    @SerializedName(ApiConstants.DEFAULT_ZONE_ID) @Param(description="the default zone of the account")
    private String defaultZoneId;

    @SerializedName(ApiConstants.STATE) @Param(description="the state of the account")
    private String state;

    @SerializedName(ApiConstants.NETWORK_DOMAIN) @Param(description="the network domain")
    private String networkDomain;
    
    @SerializedName(ApiConstants.ACCOUNT_DETAILS) @Param(description="details for the account")
    private Map<String, String> details;

    @SerializedName("regionId") @Param(description="source region id of the user")
    private int regionId;
    
    public void setName(String name) {
        this.name = name;
    }

    public void setAccountType(Short accountType) {
        this.accountType = accountType;
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

	public void setRegionId(int regionId) {
		this.regionId = regionId;
	}

	public String getId() {
		return id;
	}

	public void setId(String id) {
		this.id = id;
	}

	public String getDomainId() {
		return domainId;
	}

	public void setDomainId(String domainId) {
		this.domainId = domainId;
	}

	public String getDefaultZoneId() {
		return defaultZoneId;
	}

	public void setDefaultZoneId(String defaultZoneId) {
		this.defaultZoneId = defaultZoneId;
	}
}
