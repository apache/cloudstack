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

import java.util.ArrayList;
import java.util.List;

import org.apache.log4j.Logger;

import com.cloud.api.ApiConstants;
import com.cloud.api.BaseCmd;
import com.cloud.api.BaseListCmd;
import com.cloud.api.IdentityMapper;
import com.cloud.api.Implementation;
import com.cloud.api.Parameter;
import com.cloud.api.PlugService;
import com.cloud.api.ServerApiException;
import com.cloud.api.response.ListResponse;
import com.cloud.api.response.NetworkResponse;
import com.cloud.exception.ConcurrentOperationException;
import com.cloud.exception.InsufficientCapacityException;
import com.cloud.exception.InvalidParameterValueException;
import com.cloud.exception.ResourceAllocationException;
import com.cloud.exception.ResourceUnavailableException;
import com.cloud.network.Network;
import com.cloud.network.element.CiscoNexusVSMElementService;
import com.cloud.utils.exception.CloudRuntimeException;
import com.cloud.network.PortProfile;

@Implementation(responseObject=NetworkResponse.class, description="lists network that are configured on a Cisco 1000v VSM device")
public class ListCiscoNexusVSMNetworksCmd extends BaseListCmd {

    public static final Logger s_logger = Logger.getLogger(ListCiscoNexusVSMNetworksCmd.class.getName());
    private static final String s_name = "listcisconexusvsmnetworkscmdresponse";
    @PlugService CiscoNexusVSMElementService _ciscoNexusVSMService;

    /////////////////////////////////////////////////////
    //////////////// API parameters /////////////////////
    /////////////////////////////////////////////////////

    @IdentityMapper(entityTableName="external_switch_management_devices")
    @Parameter(name=ApiConstants.EXTERNAL_SWITCH_MGMT_DEVICE_ID, type=CommandType.LONG, required = true, description="Cisco Nexus 1000v VSM device ID")
    private Long vsmDeviceId;

    /////////////////////////////////////////////////////
    /////////////////// Accessors ///////////////////////
    /////////////////////////////////////////////////////

    public Long getvsmDeviceId() {
        return vsmDeviceId;
    }

    /////////////////////////////////////////////////////
    /////////////// API Implementation///////////////////
    /////////////////////////////////////////////////////

    @Override
    public void execute() throws ResourceUnavailableException, InsufficientCapacityException, ServerApiException, ConcurrentOperationException, ResourceAllocationException {
    	/**
        try {
        	// may need to change Network to something else.
            List<? extends PortProfile> networks  = _ciscoNexusVSMService.listNetworks(this);
            ListResponse<NetworkResponse> response = new ListResponse<NetworkResponse>();
            List<NetworkResponse> networkResponses = new ArrayList<NetworkResponse>();

            if (networks != null && !networks.isEmpty()) {
                for (Network network : networks) {
                    NetworkResponse networkResponse = _responseGenerator.createNetworkResponse(network);
                    networkResponses.add(networkResponse);
                }
            }

            response.setResponses(networkResponses);
            response.setResponseName(getCommandName());
            this.setResponseObject(response);
        }  catch (InvalidParameterValueException invalidParamExcp) {
            throw new ServerApiException(BaseCmd.PARAM_ERROR, invalidParamExcp.getMessage());
        } catch (CloudRuntimeException runtimeExcp) {
            throw new ServerApiException(BaseCmd.INTERNAL_ERROR, runtimeExcp.getMessage());
        } **/
    }

    @Override
    public String getCommandName() {
        return s_name;
    }
}
