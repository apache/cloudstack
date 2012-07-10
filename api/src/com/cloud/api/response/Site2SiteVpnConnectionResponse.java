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
public class Site2SiteVpnConnectionResponse extends BaseResponse implements ControlledEntityResponse {
    @SerializedName(ApiConstants.ID) @Param(description="the vpn gateway ID")
    private IdentityProxy id = new IdentityProxy("s2s_vpn_connection");

    @SerializedName(ApiConstants.S2S_VPN_GATEWAY_ID) @Param(description="the vpn gateway ID")
    private IdentityProxy vpnGatewayId= new IdentityProxy("s2s_vpn_gateway");
    
    @SerializedName(ApiConstants.PUBLIC_IP) @Param(description="the public IP address") //from VpnGateway
    private String ip;

    @SerializedName(ApiConstants.S2S_CUSTOMER_GATEWAY_ID) @Param(description="the customer gateway ID")
    private IdentityProxy customerGatewayId = new IdentityProxy("s2s_customer_gateway");
        
    @SerializedName(ApiConstants.GATEWAY) @Param(description="public ip address id of the customer gateway") //from CustomerGateway
    private String gatewayIp;

    @SerializedName(ApiConstants.CIDR_LIST) @Param(description="guest cidr list of the customer gateway") //from CustomerGateway
    private String guestCidrList;

    @SerializedName(ApiConstants.IPSEC_PSK) @Param(description="IPsec Preshared-Key of the customer gateway") //from CustomerGateway
    private String ipsecPsk;

    @SerializedName(ApiConstants.IKE_POLICY) @Param(description="IKE policy of the customer gateway") //from CustomerGateway
    private String ikePolicy;

    @SerializedName(ApiConstants.ESP_POLICY) @Param(description="ESP policy of the customer gateway") //from CustomerGateway
    private String espPolicy;

    @SerializedName(ApiConstants.LIFETIME) @Param(description="Lifetime of vpn connection to the customer gateway, in seconds") //from CustomerGateway
    private Long lifetime;
    
    @SerializedName(ApiConstants.STATE) @Param(description="State of vpn connection")
    private String state;
    
    @SerializedName(ApiConstants.ACCOUNT) @Param(description="the owner")
    private String accountName;
    
    @SerializedName(ApiConstants.PROJECT_ID) @Param(description="the project id")
    private IdentityProxy projectId = new IdentityProxy("projects");
    
    @SerializedName(ApiConstants.PROJECT) @Param(description="the project name")
    private String projectName;

    @SerializedName(ApiConstants.DOMAIN_ID) @Param(description="the domain id of the owner")
    private IdentityProxy domainId = new IdentityProxy("domain");
    
    @SerializedName(ApiConstants.DOMAIN) @Param(description="the domain name of the owner")
    private String domain;
    
    @SerializedName(ApiConstants.CREATED) @Param(description="the date and time the host was created")
    private Date created;

    @SerializedName(ApiConstants.REMOVED) @Param(description="the date and time the host was removed")
    private Date removed;

	public void setId(Long id) {
		this.id.setValue(id);
	}
	
    public void setVpnGatewayId(Long vpnGatewayId) {
        this.vpnGatewayId.setValue(vpnGatewayId);
    }

    public void setIp(String ip) {
		this.ip = ip;
	}
    
    public void setCustomerGatewayId(Long customerGatewayId) {
        this.customerGatewayId.setValue(customerGatewayId);
    }
    
    public void setGatewayIp(String gatewayIp) {
    	this.gatewayIp = gatewayIp;
    }
    
    public void setGuestCidrList(String guestCidrList) {
    	this.guestCidrList = guestCidrList;
    }
    
    public void setIpsecPsk(String ipsecPsk) {
    	this.ipsecPsk = ipsecPsk;
    }
    
    public void setIkePolicy(String ikePolicy) {
    	this.ikePolicy = ikePolicy;
    }
    
    public void setEspPolicy(String espPolicy) {
    	this.espPolicy = espPolicy;
    }
    
    public void setLifetime(Long lifetime) {
    	this.lifetime = lifetime;
    }     
    
    public void setState(String state) {
        this.state = state;
    }

    public void setCreated(Date created) {
        this.created = created;
    }

    public void setRemoved(Date removed) {
        this.removed = removed;
    }	

    @Override
    public void setAccountName(String accountName) {
        this.accountName = accountName;
    }

    @Override
    public void setProjectId(Long projectId) {
        this.projectId.setValue(projectId);
    }

    @Override
    public void setProjectName(String projectName) {
        this.projectName = projectName;
    }

    @Override
    public void setDomainId(Long domainId) {
        this.domainId.setValue(domainId);
    }

    @Override
    public void setDomainName(String domainName) {
        this.domain = domainName;
    }

}
