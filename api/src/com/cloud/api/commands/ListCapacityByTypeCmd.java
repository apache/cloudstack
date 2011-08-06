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
import com.cloud.api.Implementation;
import com.cloud.api.Parameter;
import com.cloud.api.response.CapacityResponse;
import com.cloud.api.response.ListResponse;
import com.cloud.capacity.Capacity;

@Implementation(description="Lists capacity By Type.", responseObject=CapacityResponse.class)
public class ListCapacityByTypeCmd extends BaseListCmd {

    public static final Logger s_logger = Logger.getLogger(ListCapacityCmd.class.getName());
    private static final DecimalFormat s_percentFormat = new DecimalFormat("##.##");

    private static final String s_name = "listcapacityresponse";

    /////////////////////////////////////////////////////
    //////////////// API parameters /////////////////////
    /////////////////////////////////////////////////////

    @Parameter(name=ApiConstants.TYPE, type=CommandType.INTEGER, required=true, description="lists capacity by type")
    private Integer type;    

    @Parameter(name=ApiConstants.CLUSTER_ID, type=CommandType.LONG, description="lists capacity by the Cluster ID")
    private Long clusterId;
    
    @Parameter(name=ApiConstants.POD_ID, type=CommandType.LONG, description="lists capacity by the Pod ID")
    private Long podId;    

    @Parameter(name=ApiConstants.ZONE_ID, type=CommandType.LONG, description="lists capacity by the Zone ID")
    private Long zoneId;


    /////////////////////////////////////////////////////
    /////////////////// Accessors ///////////////////////
    /////////////////////////////////////////////////////
    
    public Integer getType() {
        return type;
    }

    public Long getClusterId() {
        return clusterId;
    }
    
    public Long getPodId() {
        return podId;
    }    

    public Long getZoneId() {
        return zoneId;
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
        List<? extends Capacity> result = _mgr.listCapacityByType(this);
        ListResponse<CapacityResponse> response = new ListResponse<CapacityResponse>();
        List<CapacityResponse> capacityResponses = _responseGenerator.createCapacityResponse(result, s_percentFormat);
        response.setResponses(capacityResponses);
        response.setResponseName(getCommandName());
        this.setResponseObject(response);
    }
}
