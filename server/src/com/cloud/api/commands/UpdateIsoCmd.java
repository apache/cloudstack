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

import com.cloud.api.ApiDBUtils;
import com.cloud.api.BaseCmd;
import com.cloud.api.Implementation;
import com.cloud.api.ServerApiException;
import com.cloud.api.response.TemplateResponse;
import com.cloud.exception.ConcurrentOperationException;
import com.cloud.exception.InsufficientAddressCapacityException;
import com.cloud.exception.InsufficientCapacityException;
import com.cloud.exception.InvalidParameterValueException;
import com.cloud.exception.PermissionDeniedException;
import com.cloud.exception.ResourceUnavailableException;
import com.cloud.storage.VMTemplateVO;

@Implementation(description="Updates an ISO file.", responseObject=TemplateResponse.class)
public class UpdateIsoCmd extends UpdateTemplateOrIsoCmd {
    public static final Logger s_logger = Logger.getLogger(UpdateIsoCmd.class.getName());
    private static final String s_name = "updateisoresponse";

    /////////////////////////////////////////////////////
    /////////////////// Accessors ///////////////////////
    /////////////////////////////////////////////////////
    
    public Boolean isPasswordEnabled() {
        return null;
    }
    
    public String getFormat() {
        return null;
    }

    /////////////////////////////////////////////////////
    /////////////// API Implementation///////////////////
    /////////////////////////////////////////////////////

    @Override
    public String getName() {
        return s_name;
    }
    
    @Override
    public void execute() throws ServerApiException, InvalidParameterValueException, PermissionDeniedException, InsufficientAddressCapacityException, InsufficientCapacityException, ConcurrentOperationException, ResourceUnavailableException{
        VMTemplateVO result = BaseCmd._mgr.updateTemplate(this);
        TemplateResponse response = new TemplateResponse();
        if (result != null) {
            response.setId(result.getId());
            response.setName(result.getName());
            response.setDisplayText(result.getDisplayText());
            response.setPublic(result.isPublicTemplate());
            response.setCreated(result.getCreated());
            response.setFormat(result.getFormat());
            response.setOsTypeId(result.getGuestOSId());
            response.setOsTypeName(ApiDBUtils.findGuestOSById(result.getGuestOSId()).getDisplayName());
            response.setBootable(result.isBootable());
            response.setObjectName("iso");
            response.setResponseName(getName());
            this.setResponseObject(response);
        } else {
            throw new ServerApiException(BaseCmd.INTERNAL_ERROR, "Failed to update iso");
        }
    }
}
