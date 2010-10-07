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
import com.cloud.api.ServerApiException;
import com.cloud.api.response.TemplateResponse;
import com.cloud.storage.VMTemplateVO;

@Implementation(method="updateTemplate", manager=Manager.ManagementServer, description="Updates an ISO file.")
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
    
    @SuppressWarnings("unchecked")
    public TemplateResponse getResponse() {
        TemplateResponse response = new TemplateResponse();
        VMTemplateVO responseObject = (VMTemplateVO)getResponseObject();
        if (responseObject != null) {
            response.setId(responseObject.getId());
            response.setName(responseObject.getName());
            response.setDisplayText(responseObject.getDisplayText());
            response.setPublic(responseObject.isPublicTemplate());
            response.setCreated(responseObject.getCreated());
            response.setFormat(responseObject.getFormat());
            response.setOsTypeId(responseObject.getGuestOSId());
            response.setBootable(responseObject.isBootable());
            
        } else {
            throw new ServerApiException(BaseCmd.INTERNAL_ERROR, "Failed to update iso");
        }

        response.setResponseName(getName());
        return response;
    }
}
