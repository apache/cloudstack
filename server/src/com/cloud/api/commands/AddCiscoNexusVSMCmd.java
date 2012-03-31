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
import com.cloud.api.BaseAsyncCmd;
import com.cloud.api.BaseCmd;
import com.cloud.api.IdentityMapper;
import com.cloud.api.Implementation;
import com.cloud.api.Parameter;
import com.cloud.api.PlugService;
import com.cloud.api.ServerApiException;
import com.cloud.api.response.CiscoNexusVSMResponse;
import com.cloud.event.EventTypes;
import com.cloud.exception.ConcurrentOperationException;
import com.cloud.exception.InsufficientCapacityException;
import com.cloud.exception.InvalidParameterValueException;
import com.cloud.exception.ResourceAllocationException;
import com.cloud.exception.ResourceUnavailableException;
import com.cloud.network.CiscoNexusVSMDeviceVO;
import com.cloud.network.element.CiscoNexusVSMElementService;
import com.cloud.user.UserContext;
import com.cloud.utils.exception.CloudRuntimeException;

@Implementation(responseObject=CiscoNexusVSMResponse.class, description="Adds a Cisco Nexus 1000v Virtual Switch Manager device")
public class AddCiscoNexusVSMCmd extends BaseAsyncCmd {

    public static final Logger s_logger = Logger.getLogger(AddCiscoNexusVSMCmd.class.getName());
    private static final String s_name = "addciscon1kvvsmresponse";
    @PlugService CiscoNexusVSMElementService _ciscoNexusVSMService;

    /////////////////////////////////////////////////////
    //////////////// API parameters /////////////////////
    /////////////////////////////////////////////////////

    @IdentityMapper(entityTableName="external_virtual_switch_management_devices")
    @Parameter(name=ApiConstants.IP_ADDRESS, type=CommandType.STRING, required = true, description="IP Address of the Cisco Nexus 1000v VSM appliance.")
    private String ipaddr;

    @Parameter(name=ApiConstants.ZONE_ID, type=CommandType.STRING, required = true, description="Id of the zone in which the Cisco Nexus 1000v VSM appliance.")
    private long zoneId;
    
    @Parameter(name=ApiConstants.USERNAME, type=CommandType.STRING, required = true, description="username to reach the Cisco Nexus 1000v VSM device")
    private String username;
    
    @Parameter(name=ApiConstants.PASSWORD, type=CommandType.STRING, required = true, description="password to reach the Cisco Nexus 1000v VSM device")
    private String password;
    
    @Parameter(name=ApiConstants.NAME, type=CommandType.STRING, required = false, description="name of Cisco Nexus 1000v VSM device")
    private String vsmName;

    /////////////////////////////////////////////////////
    /////////////////// Accessors ///////////////////////
    /////////////////////////////////////////////////////

    public String getIpAddr() {
        return ipaddr;
    }

    public String getUsername() {
        return username;
    }

    public String getPassword() {
        return password;
    }

    public String getVSMName() {
    	return vsmName;
    }
    
    public long getZoneId() {
    	return zoneId;
    }

    /////////////////////////////////////////////////////
    /////////////// API Implementation///////////////////
    /////////////////////////////////////////////////////

    @Override
    public void execute() throws ResourceUnavailableException, InsufficientCapacityException, ServerApiException, ConcurrentOperationException, ResourceAllocationException {
        try {
            CiscoNexusVSMDeviceVO vsmDeviceVO = _ciscoNexusVSMService.addCiscoNexusVSM(this);
            if (vsmDeviceVO != null) {
                CiscoNexusVSMResponse response = _ciscoNexusVSMService.createCiscoNexusVSMResponse(vsmDeviceVO);
                response.setObjectName("cisconexusvsm");
                response.setResponseName(getCommandName());
                this.setResponseObject(response);
            } else {
                throw new ServerApiException(BaseAsyncCmd.INTERNAL_ERROR, "Failed to add Cisco Nexus Virtual Switch Manager due to internal error.");
            }
        }  catch (InvalidParameterValueException invalidParamExcp) {
            throw new ServerApiException(BaseCmd.PARAM_ERROR, invalidParamExcp.getMessage());
        } catch (CloudRuntimeException runtimeExcp) {
            throw new ServerApiException(BaseCmd.INTERNAL_ERROR, runtimeExcp.getMessage());
        }
    }

    @Override
    public String getEventDescription() {
        return "Adding a Cisco Nexus VSM device";
    }

    @Override
    public String getEventType() {
        return EventTypes.EVENT_EXTERNAL_SWITCH_MGMT_DEVICE_ADD;
    }
 
    @Override
    public String getCommandName() {
        return s_name;
    }

    @Override
    public long getEntityOwnerId() {
        return UserContext.current().getCaller().getId();
    }
}
