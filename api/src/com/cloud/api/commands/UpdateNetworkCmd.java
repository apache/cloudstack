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

import java.util.List;

import org.apache.log4j.Logger;

import com.cloud.api.ApiConstants;
import com.cloud.api.BaseAsyncCmd;
import com.cloud.api.BaseCmd;
import com.cloud.api.Implementation;
import com.cloud.api.Parameter;
import com.cloud.api.ServerApiException;
import com.cloud.api.response.NetworkResponse;
import com.cloud.event.EventTypes;
import com.cloud.exception.ConcurrentOperationException;
import com.cloud.exception.InsufficientCapacityException;
import com.cloud.exception.InvalidParameterValueException;
import com.cloud.network.Network;
import com.cloud.user.UserContext;

@Implementation(description="Updates a network", responseObject=NetworkResponse.class)
public class UpdateNetworkCmd extends BaseAsyncCmd {
    public static final Logger s_logger = Logger.getLogger(UpdateNetworkCmd.class.getName());

    private static final String s_name = "updatenetworkresponse";

    /////////////////////////////////////////////////////
    //////////////// API parameters /////////////////////
    /////////////////////////////////////////////////////
    @Parameter(name=ApiConstants.ID, type=CommandType.LONG, required=true, description="the ID of the network")
    private Long id;
    
    @Parameter(name=ApiConstants.NAME, type=CommandType.STRING, description="the new name for the network")
    private String name;
    
    @Parameter(name=ApiConstants.DISPLAY_TEXT, type=CommandType.STRING, description="the new display text for the network")
    private String displayText;
    
    @Parameter(name=ApiConstants.TAGS, type=CommandType.LIST, collectionType=CommandType.STRING, description="tags for the network")
    private List<String> tags;
    
    @Parameter(name=ApiConstants.NETWORK_DOMAIN, type=CommandType.STRING, description="network domain")
    private String networkDomain;
    
    @Parameter(name=ApiConstants.NETWORK_OFFERING_ID, type=CommandType.LONG, description="network offering ID - upgrade network to the new network offering")
    private Long networkOfferingId;
  
    /////////////////////////////////////////////////////
    /////////////////// Accessors ///////////////////////
    /////////////////////////////////////////////////////
   
    public Long getId() {
        return id;
    }
    
    public String getNetworkName() {
        return name;
    }
    
    public String getDisplayText() {
        return displayText;
    }
    
    public List<String> getTags() {
        return tags;
    }
    
    private String getNetworkDomain() {
        return networkDomain;
    }
    
    private Long getNetworkOfferingId() {
        return networkOfferingId;
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
        Network network = _networkService.getNetwork(id);
        if (network == null) {
            throw new InvalidParameterValueException("Networkd id=" + id + " doesn't exist");
        } else {
            return _networkService.getNetwork(id).getAccountId();
        }
    }
    
    @Override
    public void execute() throws InsufficientCapacityException, ConcurrentOperationException{
        Network result = _networkService.updateNetwork(getId(), getNetworkName(), getDisplayText(), tags, UserContext.current().getCaller(), getNetworkDomain(), getNetworkOfferingId());
        if (result != null) {
            NetworkResponse response = _responseGenerator.createNetworkResponse(result);
            response.setResponseName(getCommandName());
            this.setResponseObject(response);
        }else {
            throw new ServerApiException(BaseCmd.INTERNAL_ERROR, "Failed to update network");
        }
    }
    
    public String getEventDescription() {
        return  "Updating network: " + getId();
    }
    
    @Override
    public String getEventType() {
        return EventTypes.EVENT_NETWORK_UPDATE;
    }
}
