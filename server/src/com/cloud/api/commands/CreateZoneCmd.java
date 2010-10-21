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
import com.cloud.api.BaseCmd.CommandType;
import com.cloud.api.BaseCmd.Manager;
import com.cloud.api.Implementation;
import com.cloud.api.Parameter;
import com.cloud.api.response.ZoneResponse;
import com.cloud.dc.DataCenterVO;

@Implementation(method="createZone", manager=Manager.ConfigManager, description="Creates a Zone.")
public class CreateZoneCmd extends BaseCmd {
    public static final Logger s_logger = Logger.getLogger(CreateZoneCmd.class.getName());

    private static final String s_name = "createzoneresponse";

    /////////////////////////////////////////////////////
    //////////////// API parameters /////////////////////
    /////////////////////////////////////////////////////
    @Parameter(name="dns1", type=CommandType.STRING, required=true, description="the first DNS for the Zone")
    private String dns1;

    @Parameter(name="dns2", type=CommandType.STRING, description="the second DNS for the Zone")
    private String dns2;

    @Parameter(name="guestcidraddress", type=CommandType.STRING, required=true, description="the guest CIDR address for the Zone")
    private String guestCidrAddress;

    @Parameter(name="internaldns1", type=CommandType.STRING, required=true, description="the first internal DNS for the Zone")
    private String internalDns1;

    @Parameter(name="internaldns2", type=CommandType.STRING, description="the second internal DNS for the Zone")
    private String internalDns2;

    @Parameter(name="name", type=CommandType.STRING, required=true, description="the name of the Zone")
    private String zoneName;
    
    //FIXME - this parameter is called "vnet" in updateZone. Have to figure out which one is right
    @Parameter(name="vlan", type=CommandType.STRING, description="the VNET for the Zone")
    private String vlan;
    
    @Parameter(name="domain", type=CommandType.STRING, description="Domain name for the Vms in the zone")
    private String domain;

    @Parameter(name="domainid", type=CommandType.LONG, description="the ID of the containing domain, null for public zones")
    private Long domainId; 
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

    public String getInternalDns1() {
        return internalDns1;
    }

    public String getInternalDns2() {
        return internalDns2;
    }

    public String getZoneName() {
        return zoneName;
    }

    public String getVlan() {
        return vlan;
    }
    
    public String getDomain() {
        return domain;
    }

    public Long getDomainId(){
    	return domainId;
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
        DataCenterVO zone = (DataCenterVO)getResponseObject();

        ZoneResponse response = new ZoneResponse();
        response.setId(zone.getId());
        response.setName(zone.getName());
        response.setDns1(zone.getDns1());
        response.setDns2(zone.getDns2());
        response.setInternalDns1(zone.getInternalDns1());
        response.setInternalDns2(zone.getInternalDns2());
        response.setVlan(zone.getVnet());
        response.setGuestCidrAddress(zone.getGuestNetworkCidr());
        response.setDomain(zone.getDomain());
        response.setResponseName(getName());
        response.setDomainId(zone.getDomainId());
        return response;
    }
}
