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
import com.cloud.api.BaseAsyncCmd;
import com.cloud.api.BaseCmd;
import com.cloud.api.Implementation;
import com.cloud.api.Parameter;
import com.cloud.api.ServerApiException;
import com.cloud.api.response.SuccessResponse;
import com.cloud.event.EventTypes;
import com.cloud.storage.VMTemplateVO;
import com.cloud.template.TemplateManager;
import com.cloud.user.Account;

@Implementation(method="deleteIso", manager=TemplateManager.class, description="Deletes an ISO file.")
public class DeleteIsoCmd extends BaseAsyncCmd {
    public static final Logger s_logger = Logger.getLogger(DeleteIsoCmd.class.getName());
    private static final String s_name = "deleteisosresponse";

    /////////////////////////////////////////////////////
    //////////////// API parameters /////////////////////
    /////////////////////////////////////////////////////

    @Parameter(name="id", type=CommandType.LONG, required=true, description="the ID of the ISO file")
    private Long id;

    @Parameter(name="zoneid", type=CommandType.LONG, description="the ID of the zone of the ISO file. If not specified, the ISO will be deleted from all the zones")
    private Long zoneId;


    /////////////////////////////////////////////////////
    /////////////////// Accessors ///////////////////////
    /////////////////////////////////////////////////////

    public Long getId() {
        return id;
    }

    public Long getZoneId() {
        return zoneId;
    }

    /////////////////////////////////////////////////////
    /////////////// API Implementation///////////////////
    /////////////////////////////////////////////////////

    @Override
    public String getName() {
        return s_name;
    }
    
    public static String getStaticName() {
        return s_name;
    }
    
    @Override @SuppressWarnings("unchecked")
    public SuccessResponse getResponse() {
        SuccessResponse response = new SuccessResponse();
        Boolean responseObject = (Boolean)getResponseObject();
      
        if (responseObject != null) {
        	response.setSuccess(responseObject);
        } else {
            throw new ServerApiException(BaseCmd.INTERNAL_ERROR, "Failed to delete iso");
        }

        response.setResponseName(getName());
        return response;
    }

    @Override
    public long getAccountId() {
        VMTemplateVO iso = ApiDBUtils.findTemplateById(getId());
        if (iso != null) {
            return iso.getAccountId();
        }

        return Account.ACCOUNT_ID_SYSTEM;
    }

    @Override
    public String getEventType() {
        return EventTypes.EVENT_ISO_DELETE;
    }

    @Override
    public String getEventDescription() {
        return "Deleting iso " + getId();
    }
}
