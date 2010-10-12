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

import com.cloud.api.BaseCmd;
import com.cloud.api.BaseCmd.Manager;
import com.cloud.api.Implementation;
import com.cloud.api.Parameter;
import com.cloud.api.response.ServiceOfferingResponse;
import com.cloud.offering.NetworkOffering.GuestIpType;
import com.cloud.service.ServiceOfferingVO;

@Implementation(method="updateServiceOffering", manager=Manager.ConfigManager, description="Updates a service offering.")
public class UpdateServiceOfferingCmd extends BaseCmd {
    public static final Logger s_logger = Logger.getLogger(UpdateServiceOfferingCmd.class.getName());
    private static final String s_name = "updateserviceofferingresponse";

    /////////////////////////////////////////////////////
    //////////////// API parameters /////////////////////
    /////////////////////////////////////////////////////

    @Parameter(name="displaytext", type=CommandType.STRING, description="the display text of the service offering to be updated")
    private String displayText;

    @Parameter(name="id", type=CommandType.LONG, required=true, description="the ID of the service offering to be updated")
    private Long id;

    @Parameter(name="name", type=CommandType.STRING, description="the name of the service offering to be updated")
    private String serviceOfferingName;
    
    @Parameter(name="offerha", type=CommandType.BOOLEAN, description="the HA of the service offering to be updated")
    private Boolean offerHa;
    
    @Parameter(name="tags", type=CommandType.STRING, description="the tags for this service offering.")
    private String tags;

    @Parameter(name="usevirtualnetwork", type=CommandType.BOOLEAN, description="if true, the VM created from the offering will use default virtual networking. If false, the VM created will use a direct attached networking model. The default value is true.")
    private Boolean useVirtualNetwork;

    /////////////////////////////////////////////////////
    /////////////////// Accessors ///////////////////////
    /////////////////////////////////////////////////////

    public String getDisplayText() {
        return displayText;
    }

    public Long getId() {
        return id;
    }

    public String getServiceOfferingName() {
        return serviceOfferingName;
    }

    public Boolean getOfferHa() {
        return offerHa;
    }

    public String getTags() {
        return tags;
    }

    public Boolean getUseVirtualNetwork() {
        return useVirtualNetwork;
    }

    /////////////////////////////////////////////////////
    /////////////// API Implementation///////////////////
    /////////////////////////////////////////////////////

    @Override
    public String getName() {
        return s_name;
    }

    @Override @SuppressWarnings("unchecked")
    public ServiceOfferingResponse getResponse() {
        ServiceOfferingVO offering = (ServiceOfferingVO) getResponseObject();

        ServiceOfferingResponse response = new ServiceOfferingResponse();
        response.setId(offering.getId());
        response.setName(offering.getName());
        response.setDisplayText(offering.getDisplayText());
        response.setCpuNumber(offering.getCpu());
        response.setCpuSpeed(offering.getSpeed());
        response.setCreated(offering.getCreated());
        String storageType = offering.getUseLocalStorage() ? "local" : "shared";
        response.setStorageType(storageType);
        response.setOfferHa(offering.getOfferHA());
        response.setUseVirtualNetwork(offering.getGuestIpType().equals(GuestIpType.Virtualized));
        response.setTags(offering.getTags());

        response.setResponseName(getName());
        return response;
    }
}
