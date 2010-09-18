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
import com.cloud.api.response.ApiResponseSerializer;
import com.cloud.api.response.IPAddressResponse;
import com.cloud.dc.Vlan.VlanType;
import com.cloud.dc.VlanVO;
import com.cloud.network.IPAddressVO;
import com.cloud.user.Account;
import com.cloud.user.UserContext;

@Implementation(method="associateIP", manager=Manager.NetworkManager)
public class AssociateIPAddrCmd extends BaseCmd {
    public static final Logger s_logger = Logger.getLogger(AssociateIPAddrCmd.class.getName());
    private static final String s_name = "associateipaddressresponse";

    /////////////////////////////////////////////////////
    //////////////// API parameters /////////////////////
    /////////////////////////////////////////////////////

    @Parameter(name="account", type=CommandType.STRING)
    private String accountName;

    @Parameter(name="domainid", type=CommandType.LONG)
    private Long domainId;

    @Parameter(name="zoneid", type=CommandType.LONG, required=true)
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

    public Long getZoneId() {
        return zoneId;
    }


    /////////////////////////////////////////////////////
    /////////////// API Implementation///////////////////
    /////////////////////////////////////////////////////

    public String getName() {
        return s_name;
    }

    public static String getResultObjectName() {
    	return "addressinfo";
    }
    
    public String getResponse() {
    	IPAddressVO ipAddress = (IPAddressVO)getResponseObject();

        VlanVO vlan  = ApiDBUtils.findVlanById(ipAddress.getVlanDbId());
        boolean forVirtualNetworks = vlan.getVlanType().equals(VlanType.VirtualNetwork);

        IPAddressResponse ipResponse = new IPAddressResponse();
        ipResponse.setIpAddress(ipAddress.getAddress());
        if (ipAddress.getAllocated() != null) {
            ipResponse.setAllocated(ipAddress.getAllocated());
        }
        ipResponse.setZoneId(ipAddress.getDataCenterId());
        ipResponse.setZoneName(ApiDBUtils.findZoneById(ipAddress.getDataCenterId()).getName());
        ipResponse.setSourceNat(ipAddress.isSourceNat());

        //get account information
        Account accountTemp = ApiDBUtils.findAccountById(ipAddress.getAccountId());
        if (accountTemp !=null){
            ipResponse.setAccountName(accountTemp.getAccountName());
            ipResponse.setDomainId(accountTemp.getDomainId());
            ipResponse.setDomainName(ApiDBUtils.findDomainById(accountTemp.getDomainId()).getName());
        } 
        
        ipResponse.setForVirtualNetwork(forVirtualNetworks);

        //show this info to admin only
        Account account = (Account)UserContext.current().getAccountObject();
        if ((account == null)  || isAdmin(account.getType())) {
            ipResponse.setVlanId(ipAddress.getVlanDbId());
            ipResponse.setVlanName(ApiDBUtils.findVlanById(ipAddress.getVlanDbId()).getVlanId());
        }

        ipResponse.setResponseName(getName());
        return ApiResponseSerializer.toSerializedString(ipResponse);
    }
}
