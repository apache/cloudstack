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
import com.cloud.api.ServerApiException;
import com.cloud.api.response.TrafficTypeResponse;
import com.cloud.async.AsyncJob;
import com.cloud.event.EventTypes;
import com.cloud.network.PhysicalNetworkTrafficType;
import com.cloud.user.Account;

@Implementation(description="Updates traffic type of a physical network", responseObject=TrafficTypeResponse.class, since="3.0.0")
public class UpdateTrafficTypeCmd extends BaseAsyncCmd {
    public static final Logger s_logger = Logger.getLogger(UpdateTrafficTypeCmd.class.getName());

    private static final String s_name = "updatetraffictyperesponse";

    /////////////////////////////////////////////////////
    //////////////// API parameters /////////////////////
    /////////////////////////////////////////////////////
    
    @IdentityMapper(entityTableName="physical_network_traffic_types")
    @Parameter(name=ApiConstants.ID, type=CommandType.LONG, required=true, description="traffic type id")
    private Long id;
    
    @Parameter(name=ApiConstants.XEN_NETWORK_LABEL, type=CommandType.STRING, description="The network name label of the physical device dedicated to this traffic on a XenServer host")
    private String xenLabel;
    
    @Parameter(name=ApiConstants.KVM_NETWORK_LABEL, type=CommandType.STRING, description="The network name label of the physical device dedicated to this traffic on a KVM host")
    private String kvmLabel;
    
    @Parameter(name=ApiConstants.VMWARE_NETWORK_LABEL, type=CommandType.STRING, description="The network name label of the physical device dedicated to this traffic on a VMware host")
    private String vmwareLabel;
    

    /////////////////////////////////////////////////////
    /////////////////// Accessors ///////////////////////
    /////////////////////////////////////////////////////
    
    public Long getId() {
        return id;
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
        PhysicalNetworkTrafficType result = _networkService.updatePhysicalNetworkTrafficType(getId(), getXenLabel(), getKvmLabel(), getVmwareLabel());
        if (result != null) {
            TrafficTypeResponse response = _responseGenerator.createTrafficTypeResponse(result);
            response.setResponseName(getCommandName());
            this.setResponseObject(response);
        }else {
            throw new ServerApiException(BaseCmd.INTERNAL_ERROR, "Failed to update traffic type");
        }
    }

    @Override
    public String getEventDescription() {
        return  "Updating Traffic Type: " + getId();
    }
    
    @Override
    public String getEventType() {
        return EventTypes.EVENT_TRAFFIC_TYPE_UPDATE;
    }

    @Override
    public AsyncJob.Type getInstanceType() {
        return AsyncJob.Type.TrafficType;
    }
}
