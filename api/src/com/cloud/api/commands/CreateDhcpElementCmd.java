/**
 *  Copyright (C) 2011 Citrix Systems, Inc.  All rights reserved.
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
import com.cloud.api.BaseAsyncCreateCmd;
import com.cloud.api.BaseCmd;
import com.cloud.api.Implementation;
import com.cloud.api.Parameter;
import com.cloud.api.PlugService;
import com.cloud.api.ServerApiException;
import com.cloud.api.response.SuccessResponse;
import com.cloud.network.VirtualRouterElements;
import com.cloud.network.element.DhcpElementService;
import com.cloud.event.EventTypes;
import com.cloud.exception.ResourceAllocationException;
import com.cloud.user.Account;
import com.cloud.user.UserContext;

@Implementation(responseObject=SuccessResponse.class, description="Create a dhcp element.")
public class CreateDhcpElementCmd extends BaseAsyncCreateCmd {
	public static final Logger s_logger = Logger.getLogger(CreateDhcpElementCmd.class.getName());
    private static final String s_name = "createdhcpelementresponse";
    
    @PlugService
    private DhcpElementService _service;

    /////////////////////////////////////////////////////
    //////////////// API parameters /////////////////////
    /////////////////////////////////////////////////////

    @Parameter(name=ApiConstants.NETWORK_SERVICE_PROVIDER_ID, type=CommandType.LONG, required=true, description="the network service provider ID of the dhcp element")
    private Long nspId;

    /////////////////////////////////////////////////////
    /////////////////// Accessors ///////////////////////
    /////////////////////////////////////////////////////

    public void setNspId(Long nspId) {
        this.nspId = nspId;
    }

    public Long getNspId() {
        return nspId;
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
    public void execute(){
        UserContext.current().setEventDetails("DHCP element Id: "+getEntityId());
        VirtualRouterElements result = _service.getCreatedElement(getEntityId());
        if (result != null) {
            SuccessResponse response = new SuccessResponse();
            response.setResponseName(getCommandName());
            response.setSuccess(true);
            this.setResponseObject(response);
        }else {
            throw new ServerApiException(BaseCmd.INTERNAL_ERROR, "Failed to add Virtual Router entity to physical network");
        }
    }

    @Override
    public void create() throws ResourceAllocationException {
        VirtualRouterElements result = _service.addElement(getNspId());
        if (result != null) {
            setEntityId(result.getId());
        } else {
            throw new ServerApiException(BaseCmd.INTERNAL_ERROR, "Failed to add Virtual Router entity to physical network");
        }        
    }

    @Override
    public String getEventType() {
        return EventTypes.EVENT_SERVICE_PROVIDER_CREATE;
    }
    
    @Override
    public String getEventDescription() {
        return  "Adding physical network ServiceProvider Dhcp server: " + getEntityId();
    }
}
