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
package com.cloud.api.commands;

import org.apache.log4j.Logger;

import com.cloud.api.ApiConstants;
import com.cloud.api.BaseAsyncCmd;
import com.cloud.api.BaseCmd;
import com.cloud.api.Implementation;
import com.cloud.api.Parameter;
import com.cloud.api.ServerApiException;
import com.cloud.api.response.Site2SiteCustomerGatewayResponse;
import com.cloud.event.EventTypes;
import com.cloud.network.Site2SiteCustomerGateway;
import com.cloud.user.Account;

@Implementation(description="Creates site to site vpn customer gateway", responseObject=Site2SiteCustomerGatewayResponse.class)
public class CreateVpnCustomerGatewayCmd extends BaseAsyncCmd {
    public static final Logger s_logger = Logger.getLogger(CreateVpnCustomerGatewayCmd.class.getName());

    private static final String s_name = "createcustomergatewayresponse";

    /////////////////////////////////////////////////////
    //////////////// API parameters /////////////////////
    /////////////////////////////////////////////////////
    @Parameter(name=ApiConstants.GATEWAY, type=CommandType.STRING, required=true, description="public ip address id of the customer gateway")
    private String gatewayIp;

    @Parameter(name=ApiConstants.CIDR_LIST, type=CommandType.STRING, required=true, description="guest cidr list of the customer gateway")
    private String guestCidrList;

    @Parameter(name=ApiConstants.IPSEC_PSK, type=CommandType.STRING, required=true, description="IPsec Preshared-Key of the customer gateway")
    private String ipsecPsk;

    @Parameter(name=ApiConstants.IKE_POLICY, type=CommandType.STRING, required=true, description="IKE policy of the customer gateway")
    private String ikePolicy;

    @Parameter(name=ApiConstants.ESP_POLICY, type=CommandType.STRING, required=true, description="ESP policy of the customer gateway")
    private String espPolicy;

    @Parameter(name=ApiConstants.LIFETIME, type=CommandType.STRING, required=false, description="Lifetime of vpn connection to the customer gateway, in seconds")
    private Long lifetime;

    /////////////////////////////////////////////////////
    /////////////////// Accessors ///////////////////////
    /////////////////////////////////////////////////////

    public String getEntityTable() {
    	return "s2s_customer_gateway";
    }
    
    public String getIpsecPsk() {
        return ipsecPsk;
    }

    public String getGuestCidrList() {
        return guestCidrList;
    }

    public String getGatewayIp() {
        return gatewayIp;
    }

    public String getIkePolicy() {
        return ikePolicy;
    }

    public String getEspPolicy() {
        return espPolicy;
    }

    public Long getLifetime() {
        return lifetime;
    }

    /////////////////////////////////////////////////////
    /////////////// API Implementation///////////////////
    /////////////////////////////////////////////////////


    @Override
    public String getCommandName() {
        return s_name;
    }

	@Override
	public long getEntityOwnerId() {
        return Account.ACCOUNT_ID_SYSTEM;
    }

	@Override
	public String getEventDescription() {
		return "Create site-to-site VPN customer gateway";
	}

	@Override
	public String getEventType() {
		return EventTypes.EVENT_S2S_CUSTOMER_GATEWAY_CREATE;
	}
	
    @Override
    public void execute(){
        Site2SiteCustomerGateway result = _s2sVpnService.createCustomerGateway(this);
        if (result != null) {
            Site2SiteCustomerGatewayResponse response = _responseGenerator.createSite2SiteCustomerGatewayResponse(result);
            response.setResponseName(getCommandName());
            this.setResponseObject(response);
        } else {
            throw new ServerApiException(BaseCmd.INTERNAL_ERROR, "Failed to create customer VPN gateway");
        }
    }
}
