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

package com.cloud.api.commands;

import org.apache.log4j.Logger;

import com.cloud.api.ApiConstants;
import com.cloud.api.ApiResponseHelper;
import com.cloud.api.BaseCmd;
import com.cloud.api.Implementation;
import com.cloud.api.Parameter;
import com.cloud.api.ServerApiException;
import com.cloud.api.response.ZoneResponse;
import com.cloud.configuration.ConfigurationManager;
import com.cloud.dc.DataCenterVO;

@Implementation(method="editZone", manager=ConfigurationManager.class, description="Updates a Zone.")
public class UpdateZoneCmd extends BaseCmd {
    public static final Logger s_logger = Logger.getLogger(UpdateZoneCmd.class.getName());

    private static final String s_name = "updatezoneresponse";

    /////////////////////////////////////////////////////
    //////////////// API parameters /////////////////////
    /////////////////////////////////////////////////////

    @Parameter(name=ApiConstants.DNS1, type=CommandType.STRING, description="the first DNS for the Zone")
    private String dns1;

    @Parameter(name=ApiConstants.DNS2, type=CommandType.STRING, description="the second DNS for the Zone")
    private String dns2;

    @Parameter(name=ApiConstants.GUEST_CIDR_ADDRESS, type=CommandType.STRING, description="the guest CIDR address for the Zone")
    private String guestCidrAddress;

    @Parameter(name=ApiConstants.ID, type=CommandType.LONG, required=true, description="the ID of the Zone")
    private Long id;

    @Parameter(name=ApiConstants.INTERNAL_DNS1, type=CommandType.STRING, description="the first internal DNS for the Zone")
    private String internalDns1;

    @Parameter(name=ApiConstants.INTERNAL_DNS2, type=CommandType.STRING, description="the second internal DNS for the Zone")
    private String internalDns2;

    @Parameter(name=ApiConstants.NAME, type=CommandType.STRING, description="the name of the Zone")
    private String zoneName;

    @Parameter(name=ApiConstants.VNET, type=CommandType.STRING, description="the VNET for the Zone")
    private String vnet;
    
//    @Parameter(name=ApiConstants.DOMAIN, type=CommandType.STRING, description="Domain name for the Vms in the zone")
//    private String domain;

//    @Parameter(name=ApiConstants.DOMAIN_ID, type=CommandType.LONG, description="the ID of the containing domain, null for public zones")
//    private Long domainId; 

    /////////////////////////////////////////////////////
    /////////////////// Accessors ///////////////////////
    /////////////////////////////////////////////////////

    public String getDns1() {
        return dns1;
    }

    public String getDns2() {
        return dns2;
    }

    public String getGuestCidrAddress() {
        return guestCidrAddress;
    }

    public Long getId() {
        return id;
    }

    public String getInternalDns1() {
        return internalDns1;
    }

    public String getInternalDns2() {
        return internalDns2;
    }

    public String getZoneName() {
        return zoneName;
    }

    public String getVnet() {
        return vnet;
    }
    
//    public String getDomain() {
//        return domain;
//    }

//	public Long getDomainId() {
//		return domainId;
//	}
//
//	public void setDomainId(Long domainId) {
//		this.domainId = domainId;
//	}
    /////////////////////////////////////////////////////
    /////////////// API Implementation///////////////////
    /////////////////////////////////////////////////////

    @Override
    public String getName() {
        return s_name;
    }

    @Override @SuppressWarnings("unchecked")
    public ZoneResponse getResponse() {
       
        DataCenterVO responseObject = (DataCenterVO)getResponseObject();
        if (responseObject != null) {
            ZoneResponse response = ApiResponseHelper.createZoneResponse(responseObject);
            response.setResponseName(getName());
            return response;
        } else {
        	throw new ServerApiException(BaseCmd.INTERNAL_ERROR, "Failed to update zone; internal error.");
        }
    }
}
