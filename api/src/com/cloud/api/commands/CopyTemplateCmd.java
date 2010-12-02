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
import com.cloud.api.BaseAsyncCmd;
import com.cloud.api.BaseCmd;
import com.cloud.api.Implementation;
import com.cloud.api.Parameter;
import com.cloud.api.ServerApiException;
import com.cloud.api.response.TemplateResponse;
import com.cloud.async.AsyncJob;
import com.cloud.event.EventTypes;
import com.cloud.exception.StorageUnavailableException;
import com.cloud.template.VirtualMachineTemplate;
import com.cloud.user.Account;

@Implementation(description="Copies a template from one zone to another.", responseObject=TemplateResponse.class)
public class CopyTemplateCmd extends BaseAsyncCmd {
	public static final Logger s_logger = Logger.getLogger(CopyTemplateCmd.class.getName());
    private static final String s_name = "copytemplateresponse";

    /////////////////////////////////////////////////////
    //////////////// API parameters /////////////////////
    /////////////////////////////////////////////////////

    @Parameter(name=ApiConstants.DESTINATION_ZONE_ID, type=CommandType.LONG, required=true, description="ID of the zone the template is being copied to.")
    private Long destZoneId;

    @Parameter(name=ApiConstants.ID, type=CommandType.LONG, required=true, description="Template ID.")
    private Long id;

    @Parameter(name=ApiConstants.SOURCE_ZONE_ID, type=CommandType.LONG, required=true, description="ID of the zone the template is currently hosted on.")
    private Long sourceZoneId;


    /////////////////////////////////////////////////////
    /////////////////// Accessors ///////////////////////
    /////////////////////////////////////////////////////

    public Long getDestinationZoneId() {
        return destZoneId;
    }

    public Long getId() {
        return id;
    }

    public Long getSourceZoneId() {
        return sourceZoneId;
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

    @Override
    public long getAccountId() {
        VirtualMachineTemplate template = _entityMgr.findById(VirtualMachineTemplate.class, getId());
        if (template != null) {
            return template.getAccountId();
        }

        // bad id given, parent this command to SYSTEM so ERROR events are tracked
        return Account.ACCOUNT_ID_SYSTEM;
    }

    @Override
    public String getEventType() {
        return EventTypes.EVENT_TEMPLATE_COPY;
    }

    @Override
    public String getEventDescription() {
        return  "copying template: " + getId() + " from zone: " + getSourceZoneId() + " to zone: " + getDestinationZoneId();
    }
    
    public AsyncJob.Type getInstanceType() {
    	return AsyncJob.Type.Template;
    }
    
    public Long getInstanceId() {
    	return getId();
    }

    @Override
    public void execute(){
        try {
            VirtualMachineTemplate template = _templateService.copyTemplate(this);
            TemplateResponse templateResponse = _responseGenerator.createTemplateResponse(template, destZoneId);
            templateResponse.setResponseName(getName());
            
            this.setResponseObject(templateResponse);
        } catch (StorageUnavailableException ex) {
            throw new ServerApiException(BaseCmd.RESOURCE_UNAVAILABLE_ERROR, ex.getMessage());
        }
    }
}

