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
import com.cloud.api.BaseCmd.CommandType;
import com.cloud.api.response.ServiceOfferingResponse;
import com.cloud.offering.ServiceOffering;

@Implementation(description="Updates a service offering.", responseObject=ServiceOfferingResponse.class)
public class UpdateServiceOfferingCmd extends BaseCmd {
    public static final Logger s_logger = Logger.getLogger(UpdateServiceOfferingCmd.class.getName());
    private static final String s_name = "updateserviceofferingresponse";

    /////////////////////////////////////////////////////
    //////////////// API parameters /////////////////////
    /////////////////////////////////////////////////////

    @Parameter(name=ApiConstants.DISPLAY_TEXT, type=CommandType.STRING, description="the display text of the service offering to be updated")
    private String displayText;

    @Parameter(name=ApiConstants.ID, type=CommandType.LONG, required=true, description="the ID of the service offering to be updated")
    private Long id;

    @Parameter(name=ApiConstants.NAME, type=CommandType.STRING, description="the name of the service offering to be updated")
    private String serviceOfferingName;
    
    @Parameter(name=ApiConstants.OFFER_HA, type=CommandType.BOOLEAN, description="the HA of the service offering to be updated")
    private Boolean offerHa;
    
//    @Parameter(name=ApiConstants.TAGS, type=CommandType.STRING, description="the tags for this service offering.")
//    private String tags;

    @Parameter(name=ApiConstants.USE_VIRTUAL_NETWORK, type=CommandType.BOOLEAN, description="if true, the VM created from the offering will use default virtual networking. If false, the VM created will use a direct attached networking model. The default value is true.")
    private Boolean useVirtualNetwork;
    
    @Parameter(name=ApiConstants.DOMAIN_ID, type=CommandType.LONG, description="the ID of the containing domain, null for public offerings")
    private Long domainId; 

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

//    public String getTags() {
//        return tags;
//    }

    public Boolean getUseVirtualNetwork() {
        return useVirtualNetwork;
    }

    
    public Long getDomainId() {
		return domainId;
	}
    
    /////////////////////////////////////////////////////
    /////////////// API Implementation///////////////////
    /////////////////////////////////////////////////////


	@Override
    public String getName() {
        return s_name;
    }

    @Override
    public void execute(){
    	//Note
    	//Once an offering is created, we cannot update the domainId field (keeping consistent with zones logic)
        ServiceOffering result = _configService.updateServiceOffering(this);
        if (result != null){
            ServiceOfferingResponse response = _responseGenerator.createServiceOfferingResponse(result);
            response.setResponseName(getName());
            this.setResponseObject(response);
        } else {
            throw new ServerApiException(BaseCmd.INTERNAL_ERROR, "Failed to update service offering");
        }
    }
}
