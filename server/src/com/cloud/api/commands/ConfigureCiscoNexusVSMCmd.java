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
//import com.cloud.api.response.NetscalerLoadBalancerResponse;
import com.cloud.api.response.CiscoNexusVSMResponse;
import com.cloud.event.EventTypes;
import com.cloud.exception.ConcurrentOperationException;
import com.cloud.exception.InsufficientCapacityException;
import com.cloud.exception.InvalidParameterValueException;
import com.cloud.exception.ResourceAllocationException;
import com.cloud.exception.ResourceUnavailableException;
//import com.cloud.network.ExternalLoadBalancerDeviceVO;
import com.cloud.network.CiscoNexusVSMDeviceVO;
//import com.cloud.network.element.NetscalerLoadBalancerElementService;
import com.cloud.network.element.CiscoNexusVSMElementService;
import com.cloud.user.UserContext;
import com.cloud.utils.exception.CloudRuntimeException;

@Implementation(responseObject=CiscoNexusVSMResponse.class, description="configures a netscaler load balancer device")
public class ConfigureCiscoNexusVSMCmd extends BaseAsyncCmd {

    public static final Logger s_logger = Logger.getLogger(ConfigureCiscoNexusVSMCmd.class.getName());
    private static final String s_name = "configurecisconexusvsmresponse";
    @PlugService CiscoNexusVSMElementService _ciscoNexusVSMService;

    /////////////////////////////////////////////////////
    //////////////// API parameters /////////////////////
    /////////////////////////////////////////////////////

    @IdentityMapper(entityTableName="external_virtual_switch_management_devices")
    @Parameter(name=ApiConstants.EXTERNAL_SWITCH_MGMT_DEVICE_ID, type=CommandType.LONG, required=true, description="Cisco Nexus 1000v VSM device ID")
    private Long vsmDeviceId;

    // As of now, not sure what to configure the n1kv VSM device with! So we'll just place a note here to pass in the parameters we'd like to
    // configure.
    
    /////////////////////////////////////////////////////
    /////////////////// Accessors ///////////////////////
    /////////////////////////////////////////////////////

    public Long getCiscoNexusVSMDeviceId() {
        return vsmDeviceId;
    }

    // We'll define more accessor methods in case we add more parameters above.
    
    /////////////////////////////////////////////////////
    /////////////// API Implementation///////////////////
    /////////////////////////////////////////////////////

    @Override
    public void execute() throws ResourceUnavailableException, InsufficientCapacityException, ServerApiException, ConcurrentOperationException, ResourceAllocationException {
    	/**
        try { 
            CiscoNexusVSMDeviceVO ciscoNexusVSMDeviceVO = _ciscoNexusVSMService.configureCiscoNexusVSM(this);
            if (ciscoNexusVSMDeviceVO != null) {
                CiscoNexusVSMResponse response = _ciscoNexusVSMService.createCiscoNexusVSMResponse(ciscoNexusVSMDeviceVO);
                response.setObjectName("cisconexusvsm");
                response.setResponseName(getCommandName());
                this.setResponseObject(response);
            } else {
                throw new ServerApiException(BaseAsyncCmd.INTERNAL_ERROR, "Failed to configure netscaler load balancer due to internal error.");
            }
        }  catch (InvalidParameterValueException invalidParamExcp) {
            throw new ServerApiException(BaseCmd.PARAM_ERROR, invalidParamExcp.getMessage());
        } catch (CloudRuntimeException runtimeExcp) {
            throw new ServerApiException(BaseCmd.INTERNAL_ERROR, runtimeExcp.getMessage());
        } **/
    }

    @Override
    public String getEventDescription() {
        return "Configuring a Cisco Nexus VSM device";
    }

    @Override
    public String getEventType() {
        return EventTypes.EVENT_EXTERNAL_SWITCH_MGMT_DEVICE_CONFIGURE;
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
