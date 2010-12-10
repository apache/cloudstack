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
import com.cloud.api.BaseCmd;
import com.cloud.api.Implementation;
import com.cloud.api.Parameter;
import com.cloud.api.ServerApiException;
import com.cloud.api.response.NetworkOfferingResponse;
import com.cloud.offering.NetworkOffering;

@Implementation(description="Creates a network offering.", responseObject=NetworkOfferingResponse.class)
public class CreateNetworkOfferingCmd extends BaseCmd {
    public static final Logger s_logger = Logger.getLogger(CreateNetworkOfferingCmd.class.getName());
    private static final String _name = "createnetworkofferingresponse";

    /////////////////////////////////////////////////////
    //////////////// API parameters /////////////////////
    /////////////////////////////////////////////////////

    @Parameter(name=ApiConstants.NAME, type=CommandType.STRING, required=true, description="the name of the network offering")
    private String networkOfferingName;
    
    @Parameter(name=ApiConstants.DISPLAY_TEXT, type=CommandType.STRING, required=true, description="the display text of the network offering")
    private String displayText;
    
    @Parameter(name=ApiConstants.TYPE, type=CommandType.STRING, required=true, description="type of the network. Supported types Virtual, Direct")
    private String type;
    
    @Parameter(name=ApiConstants.TRAFFIC_TYPE, type=CommandType.STRING, required=true, description="the traffic type for the network offering, supported types are Public, Management, Control, Guest, Vlan or Storage.")
    private String traffictype;
    
    @Parameter(name=ApiConstants.MAX_CONNECTIONS, type=CommandType.INTEGER, description="maximum number of concurrent connections supported by the network offering")
    private Integer maxConnections;
    
    @Parameter(name=ApiConstants.TAGS, type=CommandType.STRING, description="the tags for the network offering.")
    private String tags; 
    
    @Parameter(name=ApiConstants.SPECIFY_VLAN, type=CommandType.BOOLEAN, description="true is network offering supports vlans")
    private Boolean specifyVlan; 

    /////////////////////////////////////////////////////
    /////////////////// Accessors ///////////////////////
    /////////////////////////////////////////////////////
    
    public String getNetworkOfferingName() {
        return networkOfferingName;
    }
    
    public String getDisplayText() {
        return displayText;
    }

    public String getTags() {
        return tags;
    }

    public String getType() {
        return type;
    }

    public String getTraffictype() {
        return traffictype;
    }

    public Integer getMaxconnections() {
        return maxConnections;
    }
    
    public Boolean getSpecifyVlan() {
        return specifyVlan;
    }

    /////////////////////////////////////////////////////
    /////////////// API Implementation///////////////////
    /////////////////////////////////////////////////////
    @Override
    public String getCommandName() {
        return _name;
    }

    @Override
    public void execute(){
        NetworkOffering result = _configService.createNetworkOffering(this);
        if (result != null) {
            NetworkOfferingResponse response = _responseGenerator.createNetworkOfferingResponse(result);
            response.setResponseName(getCommandName());
            this.setResponseObject(response);
        } else {
            throw new ServerApiException(BaseCmd.INTERNAL_ERROR, "Failed to create network offering");
        }
    }
}
