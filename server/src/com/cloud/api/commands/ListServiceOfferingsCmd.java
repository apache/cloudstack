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

import com.cloud.api.BaseListCmd;
import com.cloud.api.Implementation;
import com.cloud.api.Parameter;
import com.cloud.api.response.ServiceOfferingResponse;
import com.cloud.offering.ServiceOffering.GuestIpType;
import com.cloud.serializer.SerializerHelper;
import com.cloud.service.ServiceOfferingVO;

@Implementation(method="searchForServiceOfferings")
public class ListServiceOfferingsCmd extends BaseListCmd {
    public static final Logger s_logger = Logger.getLogger(ListServiceOfferingsCmd.class.getName());

    private static final String s_name = "listserviceofferingsresponse";

    /////////////////////////////////////////////////////
    //////////////// API parameters /////////////////////
    /////////////////////////////////////////////////////

    @Parameter(name="id", type=CommandType.LONG)
    private Long id;

    @Parameter(name="name", type=CommandType.STRING)
    private String serviceOfferingName;

    @Parameter(name="virtualmachineid", type=CommandType.LONG)
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
    public String getResponse() {
        List<ServiceOfferingVO> offerings = (List<ServiceOfferingVO>)getResponseObject();

        List<ServiceOfferingResponse> response = new ArrayList<ServiceOfferingResponse>();
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

            response.add(offeringResponse);
        }

        return SerializerHelper.toSerializedString(response);
    }
}
