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

import java.util.Date;

import com.cloud.api.ApiConstants;
import com.cloud.utils.IdentityProxy;
import com.cloud.serializer.Param;
import com.google.gson.annotations.SerializedName;

@SuppressWarnings("unused")
public class Site2SiteCustomerGatewayResponse extends BaseResponse {
    @SerializedName(ApiConstants.ID) @Param(description="the vpn gateway ID")
    private IdentityProxy id = new IdentityProxy("s2s_customer_gateway");

    @SerializedName(ApiConstants.GATEWAY) @Param(description="public ip address id of the customer gateway")
    private String gatewayIp;

    @SerializedName(ApiConstants.IP_ADDRESS) @Param(description="guest ip of the customer gateway")
    private String guestIp;

    @SerializedName(ApiConstants.CIDR_LIST) @Param(description="guest cidr list of the customer gateway")
    private String guestCidrList;

    @SerializedName(ApiConstants.IPSEC_PSK) @Param(description="IPsec preshared-key of customer gateway")
    private String ipsecPsk;

    @SerializedName(ApiConstants.REMOVED) @Param(description="the date and time the host was removed")
    private Date removed;

	public void setId(Long id) {
		this.id.setValue(id);
	}
	
    public void setGatewayIp(String gatewayIp) {
        this.gatewayIp = gatewayIp;
    }

    public void setGuestIp(String guestIp) {
        this.guestIp = guestIp;
    }

    public void setGuestCidrList(String guestCidrList) {
        this.guestCidrList = guestCidrList;
    }

    public void setIpsecPsk(String ipsecPsk) {
        this.ipsecPsk = ipsecPsk;
    }	
    
    public void setRemoved(Date removed) {
        this.removed = removed;
    }	
}
