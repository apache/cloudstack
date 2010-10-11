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

import com.cloud.api.ApiDBUtils;
import com.cloud.api.BaseCmd;
import com.cloud.api.BaseCmd.Manager;
import com.cloud.api.Implementation;
import com.cloud.api.Parameter;
import com.cloud.api.response.VlanIpRangeResponse;
import com.cloud.dc.VlanVO;

@Implementation(method="createVlanAndPublicIpRange", manager=Manager.ConfigManager, description="Creates a VLAN IP range.")
public class CreateVlanIpRangeCmd extends BaseCmd {
	public static final Logger s_logger = Logger.getLogger(CreateVlanIpRangeCmd.class.getName());

    private static final String s_name = "createvlaniprangeresponse";

    /////////////////////////////////////////////////////
    //////////////// API parameters /////////////////////
    /////////////////////////////////////////////////////

    @Parameter(name="account", type=CommandType.STRING, description="account who will own the VLAN. If VLAN is Zone wide, this parameter should be ommited")
    private String accountName;

    @Parameter(name="domainid", type=CommandType.LONG, description="domain ID of the account owning a VLAN")
    private Long domainId;

    @Parameter(name="endip", type=CommandType.STRING, description="the ending IP address in the VLAN IP range")
    private String endIp;

    @Parameter(name="forvirtualnetwork", type=CommandType.BOOLEAN, description="true if VLAN is of Virtual type, false if Direct")
    private Boolean forVirtualNetwork;

    @Parameter(name="gateway", type=CommandType.STRING, required=true, description="the gateway of the VLAN IP range")
    private String gateway;

    @Parameter(name="netmask", type=CommandType.STRING, required=true, description="the netmask of the VLAN IP range")
    private String netmask;

    @Parameter(name="podid", type=CommandType.LONG, description="optional parameter. Have to be specified for Direct Untagged vlan only.")
    private Long podId;

    @Parameter(name="startip", type=CommandType.STRING, required=true, description="the beginning IP address in the VLAN IP range")
    private String startIp;

    @Parameter(name="vlan", type=CommandType.STRING, description="the ID or VID of the VLAN. Default is an \"untagged\" VLAN.")
    private String vlan;

    @Parameter(name="zoneid", type=CommandType.LONG, required=true, description="	the Zone ID of the VLAN IP range")
    private Long zoneId;


    /////////////////////////////////////////////////////
    /////////////////// Accessors ///////////////////////
    /////////////////////////////////////////////////////

    public String getAccountName() {
        return accountName;
    }

    public Long getDomainId() {
        return domainId;
    }

    public String getEndIp() {
        return endIp;
    }

    public Boolean isForVirtualNetwork() {
        return forVirtualNetwork;
    }

    public String getGateway() {
        return gateway;
    }

    public String getNetmask() {
        return netmask;
    }

    public Long getPodId() {
        return podId;
    }

    public String getStartIp() {
        return startIp;
    }

    public String getVlan() {
        return vlan;
    }

    public Long getZoneId() {
        return zoneId;
    }


    /////////////////////////////////////////////////////
    /////////////// API Implementation///////////////////
    /////////////////////////////////////////////////////

    @Override
    public String getName() {
        return s_name;
    }

    @Override @SuppressWarnings("unchecked")
    public VlanIpRangeResponse getResponse() {
        VlanVO vlan = (VlanVO)getResponseObject();

        String domainNameResponse = null;
        if ((accountName != null) && (domainId != null)) {
            domainNameResponse = ApiDBUtils.findDomainById(domainId).getName();
        }

        VlanIpRangeResponse response = new VlanIpRangeResponse();
        response.setAccountName(accountName);
        response.setDescription(vlan.getIpRange());
        response.setDomainId(domainId);
        response.setEndIp(endIp);
        response.setForVirtualNetwork(forVirtualNetwork);
        response.setGateway(vlan.getVlanGateway());
        response.setId(vlan.getId());
        response.setNetmask(vlan.getVlanNetmask());
        response.setPodId(podId);
        response.setStartIp(startIp);
        response.setVlan(vlan.getVlanId());
        response.setZoneId(vlan.getDataCenterId());
        response.setDomainName(domainNameResponse);
        if (podId != null) {
            response.setPodName(ApiDBUtils.findPodById(podId).getName());
        }

        response.setResponseName(getName());
        return response;
    }
}
