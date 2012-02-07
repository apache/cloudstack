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

import java.text.DecimalFormat;
import java.util.List;

import org.apache.log4j.Logger;

import com.cloud.api.ApiConstants;
import com.cloud.api.BaseListCmd;
import com.cloud.api.IdentityMapper;
import com.cloud.api.Implementation;
import com.cloud.api.Parameter;
import com.cloud.api.response.CapacityResponse;
import com.cloud.api.response.ListResponse;
import com.cloud.capacity.Capacity;
import com.cloud.exception.InvalidParameterValueException;

@Implementation(description="Lists all the system wide capacities.", responseObject=CapacityResponse.class)
public class ListCapacityCmd extends BaseListCmd {

    public static final Logger s_logger = Logger.getLogger(ListCapacityCmd.class.getName());
    private static final DecimalFormat s_percentFormat = new DecimalFormat("##.##");

    private static final String s_name = "listcapacityresponse";

    /////////////////////////////////////////////////////
    //////////////// API parameters /////////////////////
    /////////////////////////////////////////////////////
 
    @IdentityMapper(entityTableName="data_center")
    @Parameter(name=ApiConstants.ZONE_ID, type=CommandType.LONG, description="lists capacity by the Zone ID")
    private Long zoneId;

    @IdentityMapper(entityTableName="host_pod_ref")
    @Parameter(name=ApiConstants.POD_ID, type=CommandType.LONG, description="lists capacity by the Pod ID")
    private Long podId;
    
    @IdentityMapper(entityTableName="cluster")
    @Parameter(name=ApiConstants.CLUSTER_ID, type=CommandType.LONG, since="3.0.0", description="lists capacity by the Cluster ID")
    private Long clusterId;

    @Parameter(name=ApiConstants.FETCH_LATEST, type=CommandType.BOOLEAN, since="3.0.0", description="recalculate capacities and fetch the latest")
    private Boolean fetchLatest;
    
    @Parameter(name=ApiConstants.LIST_TOP_USED, type=CommandType.BOOLEAN, since="3.0.0", description="if true then lists the top consumed enabled resources at Zone/Pod/Cluster level ordered by percentage.")
    private Boolean listTopUsed;
    
    @Parameter(name=ApiConstants.TYPE, type=CommandType.INTEGER, description="lists capacity by type" +
    																		 "* CAPACITY_TYPE_MEMORY = 0" +
    																		 "* CAPACITY_TYPE_CPU = 1" +
    																		 "* CAPACITY_TYPE_STORAGE = 2" +
    																		 "* CAPACITY_TYPE_STORAGE_ALLOCATED = 3" +
    																		 "* CAPACITY_TYPE_VIRTUAL_NETWORK_PUBLIC_IP = 4" +
    																		 "* CAPACITY_TYPE_PRIVATE_IP = 5" +
    																		 "* CAPACITY_TYPE_SECONDARY_STORAGE = 6" +
    																		 "* CAPACITY_TYPE_VLAN = 7" +
    																		 "* CAPACITY_TYPE_DIRECT_ATTACHED_PUBLIC_IP = 8" +
    																		 "* CAPACITY_TYPE_LOCAL_STORAGE = 9.")

    private Integer type;
    
    @Parameter(name=ApiConstants.SORT_BY, type=CommandType.STRING, description="Sort the results. Available values: Usage")
    private String sortBy;

    /////////////////////////////////////////////////////
    /////////////////// Accessors ///////////////////////
    /////////////////////////////////////////////////////

    public Long getZoneId() {
        return zoneId;
    }
    
    public Long getPodId() {
        return podId;
    }

    public Long getClusterId() {
		return clusterId;
	}

	public Boolean getFetchLatest() {
		return fetchLatest;
	}

	public Integer getType() {
        return type;
    }
	
    public String getSortBy() {
        if (sortBy != null) {
            if (sortBy.equalsIgnoreCase("usage")) {
                return sortBy;
            } else {
                throw new InvalidParameterValueException("Only Usage value is supported for sortBy parameter in Acton release");
            }
        }
        
        return null;
    }

    public Boolean getlistTopUsed() {
        return listTopUsed;
    }

    /////////////////////////////////////////////////////
    /////////////// API Implementation///////////////////
    /////////////////////////////////////////////////////

    @Override
    public String getCommandName() {
        return s_name;
    }
    
    @Override
    public void execute(){
        List<? extends Capacity> result = null;
        if (getSortBy() != null) {
            result = _mgr.listTopConsumedResources(this);
        } else {
            result = _mgr.listCapacities(this);
        }
        
        ListResponse<CapacityResponse> response = new ListResponse<CapacityResponse>();
        List<CapacityResponse> capacityResponses = _responseGenerator.createCapacityResponse(result, s_percentFormat);
        response.setResponses(capacityResponses);
        response.setResponseName(getCommandName());
        this.setResponseObject(response);
    }
}
