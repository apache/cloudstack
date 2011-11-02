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
import com.cloud.api.IdentityMapper;
import com.cloud.api.Implementation;
import com.cloud.api.Parameter;
import com.cloud.api.BaseCmd.CommandType;
import com.cloud.api.response.ListResponse;
import com.cloud.api.response.ZoneResponse;
import com.cloud.dc.DataCenter;

@Implementation(description="Lists zones", responseObject=ZoneResponse.class)
public class ListZonesByCmd extends BaseListCmd {
    public static final Logger s_logger = Logger.getLogger(ListZonesByCmd.class.getName());

    private static final String s_name = "listzonesresponse";

    /////////////////////////////////////////////////////
    //////////////// API parameters /////////////////////
    /////////////////////////////////////////////////////
    @IdentityMapper(entityTableName="data_center")
    @Parameter(name=ApiConstants.ID, type=CommandType.LONG, description="the ID of the zone")
    private Long id;

    @Parameter(name=ApiConstants.AVAILABLE, type=CommandType.BOOLEAN, description="true if you want to retrieve all available Zones. False if you only want to return the Zones from which you have at least one VM. Default is false.")
    private Boolean available;

    @IdentityMapper(entityTableName="domain")
    @Parameter(name=ApiConstants.DOMAIN_ID, type=CommandType.LONG, description="the ID of the domain associated with the zone")
    private Long domainId;
    
    @Parameter(name=ApiConstants.SHOW_CAPACITIES, type=CommandType.BOOLEAN, description="flag to display the capacity of the zones")
    private Boolean showCapacities;
    
    /////////////////////////////////////////////////////
    /////////////////// Accessors ///////////////////////
    /////////////////////////////////////////////////////
    
    public Long getId() {
        return id;
    }

    public Boolean isAvailable() {
        return available;
    }
    
    public Long getDomainId(){
    	return domainId;
    }

    public Boolean getShowCapacities() {
		return showCapacities;
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
        List<? extends DataCenter> dataCenters = _mgr.listDataCenters(this);
        ListResponse<ZoneResponse> response = new ListResponse<ZoneResponse>();
        List<ZoneResponse> zoneResponses = new ArrayList<ZoneResponse>();
        for (DataCenter dataCenter : dataCenters) {
            ZoneResponse zoneResponse = _responseGenerator.createZoneResponse(dataCenter, showCapacities);
            zoneResponse.setObjectName("zone");
            zoneResponses.add(zoneResponse);
        }

        response.setResponses(zoneResponses);
        response.setResponseName(getCommandName());
        this.setResponseObject(response);
    }
}
