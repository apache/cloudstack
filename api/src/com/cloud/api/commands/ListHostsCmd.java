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

import java.util.ArrayList;
import java.util.List;

import org.apache.log4j.Logger;

import com.cloud.api.ApiConstants;
import com.cloud.api.BaseListCmd;
import com.cloud.api.Implementation;
import com.cloud.api.Parameter;
import com.cloud.api.response.HostResponse;
import com.cloud.api.response.ListResponse;
import com.cloud.async.AsyncJob;
import com.cloud.host.Host;
import com.cloud.utils.Pair;

@Implementation(description="Lists hosts.", responseObject=HostResponse.class)
public class ListHostsCmd extends BaseListCmd {
    public static final Logger s_logger = Logger.getLogger(ListHostsCmd.class.getName());

    private static final String s_name = "listhostsresponse";

    /////////////////////////////////////////////////////
    //////////////// API parameters /////////////////////
    /////////////////////////////////////////////////////

    @Parameter(name=ApiConstants.CLUSTER_ID, type=CommandType.LONG, description="lists hosts existing in particular cluster")
    private Long clusterId;

    @Parameter(name=ApiConstants.ID, type=CommandType.LONG, description="the id of the host")
    private Long id;

    @Parameter(name=ApiConstants.NAME, type=CommandType.STRING, description="the name of the host")
    private String hostName;

    @Parameter(name=ApiConstants.POD_ID, type=CommandType.LONG, description="the Pod ID for the host")
    private Long podId;

    @Parameter(name=ApiConstants.STATE, type=CommandType.STRING, description="the state of the host")
    private String state;

    @Parameter(name=ApiConstants.TYPE, type=CommandType.STRING, description="the host type")
    private String type;

    @Parameter(name=ApiConstants.ZONE_ID, type=CommandType.LONG, description="the Zone ID for the host")
    private Long zoneId;

    @Parameter(name=ApiConstants.VIRTUAL_MACHINE_ID, type=CommandType.LONG, required=false, description="lists hosts in the same cluster as this VM and flag hosts with enough CPU/RAm to host this VM")
    private Long virtualMachineId;
    
    @Parameter(name=ApiConstants.ALLOCATION_STATE, type=CommandType.STRING, description="list hosts by allocation state")
    private String allocationState;   
    
    @Parameter(name=ApiConstants.DETAILS, type=CommandType.INTEGER, description="give details.  1 = minimal; 2 = include static info; 3 = include events; 4 = include allocation and statistics")
    private Integer details;

    /////////////////////////////////////////////////////
    /////////////////// Accessors ///////////////////////
    /////////////////////////////////////////////////////

    public Long getClusterId() {
        return clusterId;
    }

    public Long getId() {
        return id;
    }

    public String getHostName() {
        return hostName;
    }

    public Long getPodId() {
        return podId;
    }

    public String getState() {
        return state;
    }

    public String getType() {
        return type;
    }

    public Long getZoneId() {
        return zoneId;
    }

    public Long getVirtualMachineId() {
        return virtualMachineId;
    }
    
    public String getAllocationState() {
    	return allocationState;
    } 
    
    public Integer getDetails() {
        return details;
    }
    
    /////////////////////////////////////////////////////
    /////////////// API Implementation///////////////////
    /////////////////////////////////////////////////////

    @Override
    public String getCommandName() {
        return s_name;
    }
    
    @Override
    public AsyncJob.Type getInstanceType() {
    	return AsyncJob.Type.Host;
    }

    @Override
    public void execute(){
    	List<? extends Host> result = new ArrayList<Host>();
		List<Long> hostIdsWithCapacity = new ArrayList<Long>();
    	 
    	if(getVirtualMachineId() != null){
            Pair<List<? extends Host>, List<Long>> hostsForMigration = _mgr.listHostsForMigrationOfVM(getVirtualMachineId(), this.getStartIndex(), this.getPageSizeVal());
            result = hostsForMigration.first();
            hostIdsWithCapacity = hostsForMigration.second();
    	}else{
    		result = _mgr.searchForServers(this);
    	}

        ListResponse<HostResponse> response = new ListResponse<HostResponse>();
        List<HostResponse> hostResponses = new ArrayList<HostResponse>();
        if (details == null) {
            details = 5;
        }
        for (Host host : result) {
            HostResponse hostResponse = _responseGenerator.createHostResponseTemporary(host, details);
            Boolean hasEnoughCapacity = false;
            if(hostIdsWithCapacity.contains(host.getId())){
            	hasEnoughCapacity = true;
            }
            hostResponse.setHasEnoughCapacity(hasEnoughCapacity);
            hostResponse.setObjectName("host");
            hostResponses.add(hostResponse);
        }

        response.setResponses(hostResponses);
        response.setResponseName(getCommandName());
        this.setResponseObject(response);
    }
}
