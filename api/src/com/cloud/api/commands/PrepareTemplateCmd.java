/*  Copyright (C) 2010 Cloud.com, Inc.  All rights reserved.
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

import java.util.List;

import org.apache.log4j.Logger;

import com.cloud.api.ApiConstants;
import com.cloud.api.BaseCmd;
import com.cloud.api.IdentityMapper;
import com.cloud.api.Implementation;
import com.cloud.api.Parameter;
import com.cloud.api.response.ListResponse;
import com.cloud.api.response.TemplateResponse;
import com.cloud.template.VirtualMachineTemplate;
import com.cloud.user.Account;

@Implementation(responseObject=TemplateResponse.class, description="load template into primary storage")
public class PrepareTemplateCmd extends BaseCmd {
    public static final Logger s_logger = Logger.getLogger(PrepareTemplateCmd.class.getName());

    private static final String s_name = "preparetemplateresponse";

    /////////////////////////////////////////////////////
    //////////////// API parameters /////////////////////
    /////////////////////////////////////////////////////

    @IdentityMapper(entityTableName="data_center")
    @Parameter(name=ApiConstants.ZONE_ID, required=true, type=CommandType.LONG, description="zone ID of the template to be prepared in primary storage(s).")
    private Long zoneId;
    
    @IdentityMapper(entityTableName="vm_template")
    @Parameter(name=ApiConstants.TEMPLATE_ID, required=true, type=CommandType.LONG, description="template ID of the template to be prepared in primary storage(s).")
    private Long templateId;


    /////////////////////////////////////////////////////
    /////////////////// Accessors ///////////////////////
    /////////////////////////////////////////////////////
    
    public Long getZoneId() {
    	return zoneId;
    }
    
    public Long getTemplateId() {
        return templateId;
    }

    /////////////////////////////////////////////////////
    /////////////// API Implementation///////////////////
    /////////////////////////////////////////////////////

    @Override
    public String getCommandName() {
        return s_name;
    }
    
    @Override
    public long getEntityOwnerId() {
        return Account.ACCOUNT_ID_SYSTEM;
    }
    
    @Override
    public void execute() {
        ListResponse<TemplateResponse> response = new ListResponse<TemplateResponse>();
    	
    	VirtualMachineTemplate vmTemplate = _templateService.prepareTemplate(templateId, zoneId);
    	List<TemplateResponse> templateResponses = _responseGenerator.createTemplateResponses(vmTemplate.getId(), zoneId, true);
        response.setResponses(templateResponses);
        response.setResponseName(getCommandName());              
        this.setResponseObject(response);
    }
}

