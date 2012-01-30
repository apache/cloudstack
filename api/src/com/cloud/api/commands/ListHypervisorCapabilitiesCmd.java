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
import com.cloud.api.response.HypervisorCapabilitiesResponse;
import com.cloud.api.response.ListResponse;
import com.cloud.hypervisor.Hypervisor.HypervisorType;
import com.cloud.hypervisor.HypervisorCapabilities;

@Implementation(description="Lists all hypervisor capabilities.", responseObject=HypervisorCapabilitiesResponse.class, since="3.0.0")
public class ListHypervisorCapabilitiesCmd extends BaseListCmd {
    public static final Logger s_logger = Logger.getLogger(ListHypervisorCapabilitiesCmd.class.getName());

    private static final String s_name = "listhypervisorcapabilitiesresponse";

    /////////////////////////////////////////////////////
    //////////////// API parameters /////////////////////
    /////////////////////////////////////////////////////

    @IdentityMapper(entityTableName="hypervisor_capabilities")
    @Parameter(name=ApiConstants.ID, type=CommandType.LONG, description="ID of the hypervisor capability")
    private Long id;

    @Parameter(name=ApiConstants.HYPERVISOR, type=CommandType.STRING, description="the hypervisor for which to restrict the search")
    private String hypervisor;


    /////////////////////////////////////////////////////
    /////////////////// Accessors ///////////////////////
    /////////////////////////////////////////////////////

    public Long getId() {
        return id;
    }

    public HypervisorType getHypervisor() {
        if(hypervisor != null){
            return HypervisorType.getType(hypervisor);
        }else{
            return null;
        }
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
        List<? extends HypervisorCapabilities> hpvCapabilities = _mgr.listHypervisorCapabilities(getId(), getHypervisor(), getKeyword(), this.getStartIndex(), this.getPageSizeVal());
        ListResponse<HypervisorCapabilitiesResponse> response = new ListResponse<HypervisorCapabilitiesResponse>();
        List<HypervisorCapabilitiesResponse> hpvCapabilitiesResponses = new ArrayList<HypervisorCapabilitiesResponse>();
        for (HypervisorCapabilities capability : hpvCapabilities) {
            HypervisorCapabilitiesResponse hpvCapabilityResponse = _responseGenerator.createHypervisorCapabilitiesResponse(capability);
            hpvCapabilityResponse.setObjectName("hypervisorCapabilities");
            hpvCapabilitiesResponses.add(hpvCapabilityResponse);
        }

        response.setResponses(hpvCapabilitiesResponses);
        response.setResponseName(getCommandName());
        this.setResponseObject(response);
    }
}
