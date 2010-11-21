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
import com.cloud.api.Implementation;
import com.cloud.api.Parameter;
import com.cloud.api.ServerApiException;
import com.cloud.api.response.ServiceOfferingResponse;
import com.cloud.offering.ServiceOffering;

@Implementation(description="Creates a service offering.", responseObject=ServiceOfferingResponse.class)
public class CreateServiceOfferingCmd extends BaseCmd {
	public static final Logger s_logger = Logger.getLogger(CreateServiceOfferingCmd.class.getName());
	private static final String _name = "createserviceofferingresponse";

    /////////////////////////////////////////////////////
    //////////////// API parameters /////////////////////
    /////////////////////////////////////////////////////

    @Parameter(name=ApiConstants.CPU_NUMBER, type=CommandType.LONG, required=true, description="the CPU number of the service offering")
    private Long cpuNumber;

    @Parameter(name=ApiConstants.CPU_SPEED, type=CommandType.LONG, required=true, description="the CPU speed of the service offering in MHz.")
    private Long cpuSpeed;

    @Parameter(name=ApiConstants.DISPLAY_TEXT, type=CommandType.STRING, required=true, description="the display text of the service offering")
    private String displayText;

    @Parameter(name=ApiConstants.MEMORY, type=CommandType.LONG, required=true, description="the total memory of the service offering in MB")
    private Long memory;

    @Parameter(name=ApiConstants.NAME, type=CommandType.STRING, required=true, description="the name of the service offering")
    private String serviceOfferingName;

    @Parameter(name=ApiConstants.OFFER_HA, type=CommandType.BOOLEAN, description="the HA for the service offering")
    private Boolean offerHa;

    @Parameter(name=ApiConstants.STORAGE_TYPE, type=CommandType.STRING, description="the storage type of the service offering. Values are local and shared.")
    private String storageType;

    @Parameter(name=ApiConstants.TAGS, type=CommandType.STRING, description="the tags for this service offering.")
    private String tags;

    @Parameter(name=ApiConstants.USE_VIRTUAL_NETWORK, type=CommandType.BOOLEAN, description="if true, the VM created will use default virtual networking. If false, the VM created will use a direct attached networking model. The default value is true.")
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

    @Override
    public void execute(){
        ServiceOffering result = _configService.createServiceOffering(this);
        if (result != null) {
            ServiceOfferingResponse response = _responseGenerator.createServiceOfferingResponse(result);
            response.setResponseName(getName());
            this.setResponseObject(response);
        } else {
            throw new ServerApiException(BaseCmd.INTERNAL_ERROR, "Failed to create service offering");
        }
    }
}
