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
import com.cloud.api.response.ListResponse;
import com.cloud.api.response.ServiceOfferingResponse;
import com.cloud.offering.NetworkOffering.GuestIpType;
import com.cloud.service.ServiceOfferingVO;

@Implementation(method="searchForServiceOfferings", description="Lists all available service offerings.")
public class ListServiceOfferingsCmd extends BaseListCmd {
    public static final Logger s_logger = Logger.getLogger(ListServiceOfferingsCmd.class.getName());

    private static final String s_name = "listserviceofferingsresponse";

    /////////////////////////////////////////////////////
    //////////////// API parameters /////////////////////
    /////////////////////////////////////////////////////

    @Parameter(name=ApiConstants.ID, type=CommandType.LONG, description="ID of the service offering")
    private Long id;

    @Parameter(name=ApiConstants.NAME, type=CommandType.STRING, description="name of the service offering")
    private String serviceOfferingName;

    @Parameter(name=ApiConstants.VIRTUAL_MACHINE_ID, type=CommandType.LONG, description="the ID of the virtual machine. Pass this in if you want to see the available service offering that a virtual machine can be changed to.")
    private Long virtualMachineId;

    /////////////////////////////////////////////////////
    /////////////////// Accessors ///////////////////////
    /////////////////////////////////////////////////////

    public Long getId() {
        return id;
    }

    public String getServiceOfferingName() {
        return serviceOfferingName;
    }

    public Long getVirtualMachineId() {
        return virtualMachineId;
    }

    /////////////////////////////////////////////////////
    /////////////// API Implementation///////////////////
    /////////////////////////////////////////////////////

    @Override
    public String getName() {
        return s_name;
    }

    @Override @SuppressWarnings("unchecked")
    public ListResponse<ServiceOfferingResponse> getResponse() {
        List<ServiceOfferingVO> offerings = (List<ServiceOfferingVO>)getResponseObject();

        ListResponse<ServiceOfferingResponse> response = new ListResponse<ServiceOfferingResponse>();
        List<ServiceOfferingResponse> offeringResponses = new ArrayList<ServiceOfferingResponse>();
        for (ServiceOfferingVO offering : offerings) {
            ServiceOfferingResponse offeringResponse = new ServiceOfferingResponse();
            offeringResponse.setId(offering.getId());
            offeringResponse.setName(offering.getName());
            offeringResponse.setDisplayText(offering.getDisplayText());
            offeringResponse.setCpuNumber(offering.getCpu());
            offeringResponse.setCpuSpeed(offering.getSpeed());
            offeringResponse.setMemory(offering.getRamSize());
            offeringResponse.setCreated(offering.getCreated());
            offeringResponse.setStorageType(offering.getUseLocalStorage() ? "local" : "shared");
            offeringResponse.setOfferHa(offering.getOfferHA());
            offeringResponse.setUseVirtualNetwork(offering.getGuestIpType().equals(GuestIpType.Virtualized));
            offeringResponse.setTags(offering.getTags());

            offeringResponse.setResponseName("serviceoffering");
            offeringResponses.add(offeringResponse);
        }

        response.setResponses(offeringResponses);
        response.setResponseName(getName());
        return response;
    }
}
