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

import com.cloud.api.BaseCmd;
import com.cloud.api.BaseCmd.Manager;
import com.cloud.api.Implementation;
import com.cloud.api.Parameter;
import com.cloud.api.ServerApiException;
import com.cloud.api.response.ZoneResponse;
import com.cloud.dc.DataCenterVO;

@Implementation(method="updateZone", manager=Manager.ConfigManager, description="Updates a Zone.")
public class UpdateZoneCmd extends BaseCmd {
    public static final Logger s_logger = Logger.getLogger(UpdateZoneCmd.class.getName());

    private static final String s_name = "updatezoneresponse";

    /////////////////////////////////////////////////////
    //////////////// API parameters /////////////////////
    /////////////////////////////////////////////////////

    @Parameter(name="dns1", type=CommandType.STRING, description="the first DNS for the Zone")
    private String dns1;

    @Parameter(name="dns2", type=CommandType.STRING, description="the second DNS for the Zone")
    private String dns2;

    @Parameter(name="guestcidraddress", type=CommandType.STRING, description="the guest CIDR address for the Zone")
    private String guestCidrAddress;

    @Parameter(name="id", type=CommandType.LONG, required=true, description="the ID of the Zone")
    private Long id;

    @Parameter(name="internaldns1", type=CommandType.STRING, description="the first internal DNS for the Zone")
    private String internalDns1;

    @Parameter(name="internaldns2", type=CommandType.STRING, description="the second internal DNS for the Zone")
    private String internalDns2;

    @Parameter(name="name", type=CommandType.STRING, description="the name of the Zone")
    private String zoneName;

    @Parameter(name="vnet", type=CommandType.STRING, description="the VNET for the Zone")
    private String vnet;
    
    @Parameter(name="domain", type=CommandType.STRING, description="Domain name for the Vms in the zone")
    private String domain;

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
    
    public String getDomain() {
        return domain;
    }

    /////////////////////////////////////////////////////
    /////////////// API Implementation///////////////////
    /////////////////////////////////////////////////////

    @Override
    public String getName() {
        return s_name;
    }

    @Override @SuppressWarnings("unchecked")
    public ZoneResponse getResponse() {
        ZoneResponse response = new ZoneResponse();

        DataCenterVO responseObject = (DataCenterVO)getResponseObject();
        if (responseObject != null) {
            response.setStatus("true");
            response.setDisplayText("Successfully updated zone");
            response.setId(responseObject.getId());
            response.setGuestCidrAddress(responseObject.getGuestNetworkCidr());
            response.setDns1(responseObject.getDns1());
            response.setDns2(responseObject.getDns2());
            response.setInternalDns1(responseObject.getInternalDns1());
            response.setInternalDns2(responseObject.getInternalDns2());
            response.setName(responseObject.getName());
            response.setVlan(responseObject.getVnet());
            response.setDomain(responseObject.getDomain());
        } else {
        	throw new ServerApiException(BaseCmd.INTERNAL_ERROR, "Failed to update zone; internal error.");
        }
        
        response.setResponseName(getName());
        return response;
    }
}
