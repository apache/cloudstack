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
import com.cloud.api.ApiDBUtils;
import com.cloud.api.BaseCmd;
import com.cloud.api.Implementation;
import com.cloud.api.Parameter;
import com.cloud.api.ServerApiException;
import com.cloud.api.response.DiskOfferingResponse;
import com.cloud.api.response.ServiceOfferingResponse;
import com.cloud.api.response.SuccessResponse;
import com.cloud.configuration.ConfigurationManager;
import com.cloud.offering.NetworkOffering.GuestIpType;
import com.cloud.service.ServiceOfferingVO;
import com.cloud.storage.DiskOfferingVO;

@Implementation(method="updateDiskOffering", manager=ConfigurationManager.class, description="Updates a disk offering.")
public class UpdateDiskOfferingCmd extends BaseCmd{
    public static final Logger s_logger = Logger.getLogger(UpdateDiskOfferingCmd.class.getName());
    private static final String s_name = "updatediskofferingresponse";

    /////////////////////////////////////////////////////
    //////////////// API parameters /////////////////////
    /////////////////////////////////////////////////////

    @Parameter(name=ApiConstants.DISPLAY_TEXT, type=CommandType.STRING, description="updates alternate display text of the disk offering with this value")
    private String displayText;

    @Parameter(name=ApiConstants.ID, type=CommandType.LONG, required=true, description="ID of the disk offering")
    private Long id;

    @Parameter(name=ApiConstants.NAME, type=CommandType.STRING, description="updates name of the disk offering with this value")
    private String diskOfferingName;

    @Parameter(name=ApiConstants.TAGS, type=CommandType.STRING, description="update tags of the disk offering with this value")
    private String tags;

    /////////////////////////////////////////////////////
    /////////////////// Accessors ///////////////////////
    /////////////////////////////////////////////////////

    public String getDisplayText() {
        return displayText;
    }

    public Long getId() {
        return id;
    }

    public String getDiskOfferingName() {
        return diskOfferingName;
    }

    public String getTags() {
        return tags;
    }

    /////////////////////////////////////////////////////
    /////////////// API Implementation///////////////////
    /////////////////////////////////////////////////////

    @Override
    public String getName() {
        return s_name;
    }
    
//    @SuppressWarnings("unchecked")
//    public SuccessResponse getResponse() {
//        SuccessResponse response = new SuccessResponse();
//        DiskOfferingVO responseObject = (DiskOfferingVO)getResponseObject();
//        if (responseObject != null) {
//            response.setSuccess(Boolean.TRUE);
//        } else {
//            throw new ServerApiException(BaseCmd.INTERNAL_ERROR, "Failed to update disk offering");
//        }
//
//        response.setResponseName(getName());
//        return response;
//    }
//    
    @Override @SuppressWarnings("unchecked")
    public DiskOfferingResponse getResponse() {
        DiskOfferingVO offering = (DiskOfferingVO) getResponseObject();

        DiskOfferingResponse response = new DiskOfferingResponse();
        response.setId(offering.getId());
        response.setName(offering.getName());
        response.setDisplayText(offering.getDisplayText());
        response.setTags(offering.getTags());
        response.setCreated(offering.getCreated());
        response.setDiskSize(offering.getDiskSize());
        if(offering.getDomainId() != null){
        	response.setDomain(ApiDBUtils.findDomainById(offering.getDomainId()).getName());
        	response.setDomainId(offering.getDomainId());
        }
        response.setMirrored(offering.isMirrored());
        response.setTags(offering.getTags());
        response.setResponseName(getName());
        return response;
    }
}
