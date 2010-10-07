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
import com.cloud.service.ServiceOfferingVO;

@Implementation(method="createServiceOffering", manager=Manager.ConfigManager, description="Creates a service offering.")
public class CreateServiceOfferingCmd extends BaseCmd {
	public static final Logger s_logger = Logger.getLogger(CreateServiceOfferingCmd.class.getName());
	private static final String _name = "createserviceofferingresponse";

    /////////////////////////////////////////////////////
    //////////////// API parameters /////////////////////
    /////////////////////////////////////////////////////

    @Parameter(name="cpunumber", type=CommandType.LONG, required=true)
    private Long cpuNumber;

    @Parameter(name="cpuspeed", type=CommandType.LONG, required=true)
    private Long cpuSpeed;

    @Parameter(name="displaytext", type=CommandType.STRING, required=true)
    private String displayText;

    @Parameter(name="memory", type=CommandType.LONG, required=true)
    private Long memory;

    @Parameter(name="name", type=CommandType.STRING, required=true)
    private String serviceOfferingName;

    @Parameter(name="offerha", type=CommandType.BOOLEAN)
    private Boolean offerHa;

    @Parameter(name="storagetype", type=CommandType.STRING)
    private String storageType;

    @Parameter(name="tags", type=CommandType.STRING)
    private String tags;

    @Parameter(name="usevirtualnetwork", type=CommandType.BOOLEAN)
    private Boolean useVirtualNetwork;

    /////////////////////////////////////////////////////
    /////////////////// Accessors ///////////////////////
    /////////////////////////////////////////////////////

    public Long getCpuNumber() {
        return cpuNumber;
    }

    public Long getCpuSpeed() {
        return cpuSpeed;
    }

    public String getDisplayText() {
        return displayText;
    }

    public Long getMemory() {
        return memory;
    }

    public String getServiceOfferingName() {
        return serviceOfferingName;
    }

    public Boolean getOfferHa() {
        return offerHa;
    }

    public String getStorageType() {
        return storageType;
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
		return _name;
	}

	@Override @SuppressWarnings("unchecked")
    public ServiceOfferingResponse getResponse() {
	    ServiceOfferingVO offering = (ServiceOfferingVO)getResponseObject();

	    ServiceOfferingResponse response = new ServiceOfferingResponse();
	    response.setId(offering.getId());
	    response.setName(offering.getName());
	    response.setDisplayText(offering.getDisplayText());
	    response.setCpuNumber(offering.getCpu());
	    response.setCpuSpeed(offering.getSpeed());
	    response.setCreated(offering.getCreated());
	    response.setMemory(offering.getRamSize());
	    response.setOfferHa(offering.getOfferHA());
	    response.setStorageType(offering.getUseLocalStorage() ? "local" : "shared");
	    response.setTags(offering.getTags());

        response.setResponseName(getName());
        return response;
	}
}
