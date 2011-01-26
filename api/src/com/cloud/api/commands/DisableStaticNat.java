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
import com.cloud.api.BaseAsyncCmd;
import com.cloud.api.BaseCmd;
import com.cloud.api.Implementation;
import com.cloud.api.Parameter;
import com.cloud.api.ServerApiException;
import com.cloud.api.response.SuccessResponse;
import com.cloud.event.EventTypes;
import com.cloud.exception.ResourceUnavailableException;
import com.cloud.network.IpAddress;
import com.cloud.utils.net.Ip;

@Implementation(description="Disables static rule for given ip address", responseObject=SuccessResponse.class)
public class DisableStaticNat extends BaseAsyncCmd {
    public static final Logger s_logger = Logger.getLogger(DeletePortForwardingRuleCmd.class.getName());
    private static final String s_name = "disablestaticnatresponse";

    /////////////////////////////////////////////////////
    //////////////// API parameters /////////////////////
    /////////////////////////////////////////////////////

    @Parameter(name=ApiConstants.IP_ADDRESS, type=CommandType.STRING, required=true, description="the public IP address for which static nat feature is being disableed")
    private String ipAddress;
    
    /////////////////////////////////////////////////////
    /////////////////// Accessors ///////////////////////
    /////////////////////////////////////////////////////

    public String getIpAddress() {
        return ipAddress;
    }
    
    /////////////////////////////////////////////////////
    /////////////// API Implementation///////////////////
    /////////////////////////////////////////////////////
    @Override
    public String getCommandName() {
        return s_name;
    }
    
    @Override
    public String getEventType() {
        return EventTypes.EVENT_DISABLE_STATIC_NAT;
    }

    @Override
    public String getEventDescription() {
        return  ("Disabling static nat for ip=" + ipAddress);
    }
    
    @Override
    public long getEntityOwnerId() {
        return _entityMgr.findById(IpAddress.class, ipAddress).getAccountId();
    }
    
    @Override
    public void execute() throws ResourceUnavailableException {
        boolean result = _rulesService.disableOneToOneNat(new Ip(ipAddress));
        
        if (result) {
            SuccessResponse response = new SuccessResponse(getCommandName());
            this.setResponseObject(response);
        } else {
            throw new ServerApiException(BaseCmd.INTERNAL_ERROR, "Failed to disable static nat");
        }
    }
}
