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
import com.cloud.api.ApiResponseHelper;
import com.cloud.api.BaseCmd;
import com.cloud.api.Implementation;
import com.cloud.api.Parameter;
import com.cloud.api.ServerApiException;
import com.cloud.api.response.DiskOfferingResponse;
import com.cloud.configuration.ConfigurationManager;
import com.cloud.exception.ConcurrentOperationException;
import com.cloud.exception.InsufficientAddressCapacityException;
import com.cloud.exception.InsufficientCapacityException;
import com.cloud.exception.InvalidParameterValueException;
import com.cloud.exception.PermissionDeniedException;
import com.cloud.offering.DiskOffering;
import com.cloud.storage.DiskOfferingVO;

@Implementation(method="createDiskOffering", manager=ConfigurationManager.class, description="Creates a disk offering.")
public class CreateDiskOfferingCmd extends BaseCmd {
    public static final Logger s_logger = Logger.getLogger(CreateDiskOfferingCmd.class.getName());

    private static final String s_name = "creatediskofferingresponse";

    /////////////////////////////////////////////////////
    //////////////// API parameters /////////////////////
    /////////////////////////////////////////////////////

    @Parameter(name=ApiConstants.DISK_SIZE, type=CommandType.LONG, required=false, description="disk size of the disk offering in GB")
    private Long diskSize;

    @Parameter(name=ApiConstants.DISPLAY_TEXT, type=CommandType.STRING, required=true, description="alternate display text of the disk offering")
    private String displayText;

    @Parameter(name=ApiConstants.NAME, type=CommandType.STRING, required=true, description="name of the disk offering")
    private String offeringName;

    @Parameter(name=ApiConstants.TAGS, type=CommandType.STRING, description="tags for the disk offering")
    private String tags;

    @Parameter(name=ApiConstants.CUSTOMIZED, type=CommandType.BOOLEAN, description="whether disk offering is custom or not")
    private Boolean customized;
    /////////////////////////////////////////////////////
    /////////////////// Accessors ///////////////////////
    /////////////////////////////////////////////////////

    public Long getDiskSize() {
        return diskSize;
    }

    public String getDisplayText() {
        return displayText;
    }

    public String getOfferingName() {
        return offeringName;
    }

    public String getTags() {
        return tags;
    }

    public Boolean isCustomized(){
    	return customized;
    }
    
    /////////////////////////////////////////////////////
    /////////////// API Implementation///////////////////
    /////////////////////////////////////////////////////

    @Override
    public String getName() {
        return s_name;
    }

    @Override @SuppressWarnings("unchecked")
    public DiskOfferingResponse getResponse() {
        DiskOfferingVO offering = (DiskOfferingVO)getResponseObject();
        if (offering != null) {
            DiskOfferingResponse response = ApiResponseHelper.createDiskOfferingResponse(offering);
            response.setResponseName(getName());
            return response;
        } else {
            throw new ServerApiException(BaseCmd.INTERNAL_ERROR, "Failed to create disk offering");
        } 
    }
    
    @Override
    public Object execute() throws ServerApiException, InvalidParameterValueException, PermissionDeniedException, InsufficientAddressCapacityException, InsufficientCapacityException, ConcurrentOperationException{
        DiskOffering result = _configService.createDiskOffering(this);
        return result;
    }
}
