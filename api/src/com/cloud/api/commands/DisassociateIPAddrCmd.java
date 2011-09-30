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
import com.cloud.async.AsyncJob;
import com.cloud.event.EventTypes;
import com.cloud.exception.InvalidParameterValueException;
import com.cloud.network.IpAddress;
import com.cloud.user.UserContext;

@Implementation(description="Disassociates an ip address from the account.", responseObject=SuccessResponse.class)
public class DisassociateIPAddrCmd extends BaseAsyncCmd {
    public static final Logger s_logger = Logger.getLogger(DisassociateIPAddrCmd.class.getName());

    private static final String s_name = "disassociateipaddressresponse";

    /////////////////////////////////////////////////////
    //////////////// API parameters /////////////////////
    /////////////////////////////////////////////////////

    @Parameter(name=ApiConstants.ID, type=CommandType.LONG, required=true, description="the id of the public ip address to disassociate")
    private Long id;

    // unexposed parameter needed for events logging
    @Parameter(name=ApiConstants.ACCOUNT_ID, type=CommandType.LONG, expose=false)
    private Long ownerId;
    /////////////////////////////////////////////////////
    /////////////////// Accessors ///////////////////////
    /////////////////////////////////////////////////////

    public Long getIpAddressId() {
        return id;
    }

    /////////////////////////////////////////////////////
    /////////////// API Implementation///////////////////
    /////////////////////////////////////////////////////

    @Override
    public String getCommandName() {
        return s_name;
    }

    @Override
    public void execute(){
        UserContext.current().setEventDetails("Ip Id: "+getIpAddressId());
        boolean result = _networkService.disassociateIpAddress(id);
        if (result) {
            SuccessResponse response = new SuccessResponse(getCommandName());
            this.setResponseObject(response);
        } else {
            throw new ServerApiException(BaseCmd.INTERNAL_ERROR, "Failed to disassociate ip address");
        }
    }
    
    @Override
    public String getEventType() {
        return EventTypes.EVENT_NET_IP_RELEASE;
    }

    @Override
    public String getEventDescription() {
        return  ("Disassociating ip address with id=" + id);
    }
    
    @Override
    public long getEntityOwnerId() {
        if (ownerId == null) {
            IpAddress ip = getIpAddress(id);
            if (ip == null) {
                throw new InvalidParameterValueException("Unable to find ip address by id=" + id);
            }
            ownerId = ip.getAccountId();
        }
        return ownerId;
    }
    
    @Override
    public String getSyncObjType() {
        return BaseAsyncCmd.networkSyncObject;
    }

    @Override
    public Long getSyncObjId() {
        IpAddress ip = getIpAddress(id);
        return ip.getAssociatedWithNetworkId();
    }
    
    private IpAddress getIpAddress(long id) {
        IpAddress ip = _entityMgr.findById(IpAddress.class, id);
        
        if (ip == null) {
            throw new InvalidParameterValueException("Unable to find ip address by id=" + id);
        } else {
            return ip;
        }
    }
    
    @Override
    public AsyncJob.Type getInstanceType() {
        return AsyncJob.Type.IpAddress;
    }
    
    @Override
    public Long getInstanceId() {
        return getIpAddressId();
    }
}
