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
import com.cloud.api.IdentityMapper;
import com.cloud.api.Implementation;
import com.cloud.api.Parameter;
import com.cloud.api.ServerApiException;
import com.cloud.api.response.TrafficTypeResponse;
import com.cloud.event.EventTypes;
import com.cloud.exception.ResourceAllocationException;
import com.cloud.network.PhysicalNetworkTrafficType;
import com.cloud.user.Account;
import com.cloud.user.UserContext;

@Implementation(description="Adds traffic type to a physical network", responseObject=TrafficTypeResponse.class)
public class AddTrafficTypeCmd extends BaseAsyncCreateCmd {
    public static final Logger s_logger = Logger.getLogger(AddTrafficTypeCmd.class.getName());

    private static final String s_name = "addtraffictyperesponse";

    /////////////////////////////////////////////////////
    //////////////// API parameters /////////////////////
    /////////////////////////////////////////////////////
    
    @IdentityMapper(entityTableName="physical_network")
    @Parameter(name=ApiConstants.PHYSICAL_NETWORK_ID, type=CommandType.LONG, required=true, description="the Physical Network ID")
    private Long physicalNetworkId;
    
    @Parameter(name=ApiConstants.TRAFFIC_TYPE, type=CommandType.STRING, required=true, description="the trafficType to be added to the physical network")
    private String trafficType;
    
    @Parameter(name=ApiConstants.XEN_NETWORK_LABEL, type=CommandType.STRING, description="The network name label of the physical device dedicated to this traffic on a XenServer host")
    private String xenLabel;
    
    @Parameter(name=ApiConstants.KVM_NETWORK_LABEL, type=CommandType.STRING, description="The network name label of the physical device dedicated to this traffic on a KVM host")
    private String kvmLabel;
    
    @Parameter(name=ApiConstants.VMWARE_NETWORK_LABEL, type=CommandType.STRING, description="The network name label of the physical device dedicated to this traffic on a VMware host")
    private String vmwareLabel;
        
    @Parameter(name=ApiConstants.VLAN, type=CommandType.STRING, description="The VLAN id to be used for Management traffic by VMware host")
    private String vlan;

    /////////////////////////////////////////////////////
    /////////////////// Accessors ///////////////////////
    /////////////////////////////////////////////////////
    
    @Override
    public String getEntityTable() {
        return "physical_network_traffic_types";
    }

    public Long getPhysicalNetworkId() {
        return physicalNetworkId;
    }

    public String getTrafficType() {
        return trafficType;
    }

    public String getXenLabel() {
        return xenLabel;
    }

    public String getKvmLabel() {
        return kvmLabel;
    }

    public String getVmwareLabel() {
        return vmwareLabel;
    }
    
    public String getSimulatorLabel() {
    	//simulators will have no labels
    	return ""; 
    }

    public void setVlan(String vlan) {
        this.vlan = vlan;
    }

    public String getVlan() {
        return vlan;
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
        UserContext.current().setEventDetails("TrafficType Id: "+getEntityId());
        PhysicalNetworkTrafficType result = _networkService.getPhysicalNetworkTrafficType(getEntityId());
        if (result != null) {
            TrafficTypeResponse response = _responseGenerator.createTrafficTypeResponse(result);
            response.setResponseName(getCommandName());
            this.setResponseObject(response);
        }else {
            throw new ServerApiException(BaseCmd.INTERNAL_ERROR, "Failed to add traffic type to physical network");
        }
    }

    @Override
    public void create() throws ResourceAllocationException {
        PhysicalNetworkTrafficType result = _networkService.addTrafficTypeToPhysicalNetwork(getPhysicalNetworkId(), getTrafficType(), getXenLabel(), getKvmLabel(), getVmwareLabel(), getSimulatorLabel(), getVlan());
        if (result != null) {
            setEntityId(result.getId());
        } else {
            throw new ServerApiException(BaseCmd.INTERNAL_ERROR, "Failed to add traffic type to physical network");
        }        
    }

    @Override
    public String getEventType() {
        return EventTypes.EVENT_TRAFFIC_TYPE_CREATE;
    }
    
    @Override
    public String getEventDescription() {
        return  "Adding physical network traffic type: " + getEntityId();
    }
}
