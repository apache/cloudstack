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
import com.cloud.api.IdentityMapper;
import com.cloud.api.Implementation;
import com.cloud.api.Parameter;
import com.cloud.api.ServerApiException;
import com.cloud.api.response.PodResponse;
import com.cloud.dc.Pod;
import com.cloud.user.Account;

@Implementation(description="Creates a new Pod.", responseObject=PodResponse.class)
public class CreatePodCmd extends BaseCmd {
    public static final Logger s_logger = Logger.getLogger(CreatePodCmd.class.getName());

    private static final String s_name = "createpodresponse";

    /////////////////////////////////////////////////////
    //////////////// API parameters /////////////////////
    /////////////////////////////////////////////////////
    @Parameter(name=ApiConstants.NAME, type=CommandType.STRING, required=true, description="the name of the Pod")
    private String podName;
    
    @IdentityMapper(entityTableName="data_center")
    @Parameter(name=ApiConstants.ZONE_ID, type=CommandType.LONG, required=true, description="the Zone ID in which the Pod will be created	")
    private Long zoneId;

    @Parameter(name=ApiConstants.START_IP, type=CommandType.STRING, required=true, description="the starting IP address for the Pod")
    private String startIp;
    
    @Parameter(name=ApiConstants.END_IP, type=CommandType.STRING, description="the ending IP address for the Pod")
    private String endIp;
    
    @Parameter(name=ApiConstants.NETMASK, type=CommandType.STRING, required=true, description="the netmask for the Pod")
    private String netmask;

    @Parameter(name=ApiConstants.GATEWAY, type=CommandType.STRING, required=true, description="the gateway for the Pod")
    private String gateway;
    
    @Parameter(name=ApiConstants.ALLOCATION_STATE, type=CommandType.STRING, description="Allocation state of this Pod for allocation of new resources")
    private String allocationState;

    /////////////////////////////////////////////////////
    /////////////////// Accessors ///////////////////////
    /////////////////////////////////////////////////////

    public String getNetmask() {
        return netmask;
    }

    public String getEndIp() {
        return endIp;
    }

    public String getGateway() {
        return gateway;
    }

    public String getPodName() {
        return podName;
    }

    public String getStartIp() {
        return startIp;
    }

    public Long getZoneId() {
        return zoneId;
    }

    public String getAllocationState() {
    	return allocationState;
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
        Pod result = _configService.createPod(getZoneId(), getPodName(), getStartIp(), getEndIp(), getGateway(), getNetmask(), getAllocationState());
        if (result != null) {
            PodResponse response = _responseGenerator.createPodResponse(result, false);
            response.setResponseName(getCommandName());
            this.setResponseObject(response);
        } else {
            throw new ServerApiException(BaseCmd.INTERNAL_ERROR, "Failed to create pod");
        }
    }
}
