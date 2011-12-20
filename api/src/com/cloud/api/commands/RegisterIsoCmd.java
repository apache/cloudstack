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

import java.util.List;

import org.apache.log4j.Logger;

import com.cloud.api.ApiConstants;
import com.cloud.api.BaseCmd;
import com.cloud.api.IdentityMapper;
import com.cloud.api.Implementation;
import com.cloud.api.Parameter;
import com.cloud.api.ServerApiException;
import com.cloud.api.BaseCmd.CommandType;
import com.cloud.api.response.ListResponse;
import com.cloud.api.response.TemplateResponse;
import com.cloud.exception.InvalidParameterValueException;
import com.cloud.exception.ResourceAllocationException;
import com.cloud.template.VirtualMachineTemplate;
import com.cloud.user.Account;
import com.cloud.user.UserContext;

@Implementation(responseObject=TemplateResponse.class, description="Registers an existing ISO into the Cloud.com Cloud.")
public class RegisterIsoCmd extends BaseCmd {
    public static final Logger s_logger = Logger.getLogger(RegisterIsoCmd.class.getName());

    private static final String s_name = "registerisoresponse";

    /////////////////////////////////////////////////////
    //////////////// API parameters /////////////////////
    /////////////////////////////////////////////////////

    @Parameter(name=ApiConstants.BOOTABLE, type=CommandType.BOOLEAN, description="true if this ISO is bootable. If not passed explicitly its assumed to be true")
    private Boolean bootable;

    @Parameter(name=ApiConstants.DISPLAY_TEXT, type=CommandType.STRING, required=true, description="the display text of the ISO. This is usually used for display purposes.", length=4096)
    private String displayText;

    @Parameter(name=ApiConstants.IS_FEATURED, type=CommandType.BOOLEAN, description="true if you want this ISO to be featured")
    private Boolean featured;

    @Parameter(name=ApiConstants.IS_PUBLIC, type=CommandType.BOOLEAN, description="true if you want to register the ISO to be publicly available to all users, false otherwise.")
    private Boolean publicIso;

    @Parameter(name=ApiConstants.IS_EXTRACTABLE, type=CommandType.BOOLEAN, description="true if the iso or its derivatives are extractable; default is false")
    private Boolean extractable;
    
    @Parameter(name=ApiConstants.NAME, type=CommandType.STRING, required=true, description="the name of the ISO")
    private String isoName;

    @IdentityMapper(entityTableName="guest_os")
    @Parameter(name=ApiConstants.OS_TYPE_ID, type=CommandType.LONG, description="the ID of the OS Type that best represents the OS of this ISO. If the iso is bootable this parameter needs to be passed")
    private Long osTypeId;

    @Parameter(name=ApiConstants.URL, type=CommandType.STRING, required=true, description="the URL to where the ISO is currently being hosted")
    private String url;

    @IdentityMapper(entityTableName="data_center")
    @Parameter(name=ApiConstants.ZONE_ID, type=CommandType.LONG, required=true, description="the ID of the zone you wish to register the ISO to.")
    private Long zoneId;        
    
    @IdentityMapper(entityTableName="domain")
    @Parameter(name=ApiConstants.DOMAIN_ID, type=CommandType.LONG, description="an optional domainId. If the account parameter is used, domainId must also be used.")
    private Long domainId;

    @Parameter(name=ApiConstants.ACCOUNT, type=CommandType.STRING, description="an optional account name. Must be used with domainId.")
    private String accountName;
    
    @Parameter(name=ApiConstants.CHECKSUM, type=CommandType.STRING, description="the MD5 checksum value of this ISO")
    private String checksum;
    
    @IdentityMapper(entityTableName="projects")
    @Parameter(name=ApiConstants.PROJECT_ID, type=CommandType.LONG, description="Register iso for the project")
    private Long projectId;
    
    /////////////////////////////////////////////////////
    /////////////////// Accessors ///////////////////////
    /////////////////////////////////////////////////////

    public Boolean isBootable() {
        return bootable;
    }

    public String getDisplayText() {
        return displayText;
    }

    public Boolean isFeatured() {
        return featured;
    }

    public Boolean isPublic() {
        return publicIso;
    }

    public Boolean isExtractable() {
        return extractable;
    }
    
    public String getIsoName() {
        return isoName;
    }

    public Long getOsTypeId() {
        return osTypeId;
    }

    public String getUrl() {
        return url;
    }

    public Long getZoneId() {
        return zoneId;
    }

    public Long getDomainId() {
		return domainId;
	}

	public String getAccountName() {
		return accountName;
	}

    public String getChecksum() {
        return checksum;
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
        Long accountId = finalyzeAccountId(accountName, domainId, projectId, true);
        if (accountId == null) {
            return UserContext.current().getCaller().getId();
        }
        
        return accountId;
    }	
	
    @Override
    public void execute() throws ResourceAllocationException{
        VirtualMachineTemplate template = _templateService.registerIso(this);
        if (template != null) {
            ListResponse<TemplateResponse> response = new ListResponse<TemplateResponse>();
            List<TemplateResponse> templateResponses = _responseGenerator.createIsoResponses(template.getId(), zoneId, false);
            response.setResponses(templateResponses);
            response.setResponseName(getCommandName());              
            this.setResponseObject(response);
        } else {
            throw new ServerApiException(BaseCmd.INTERNAL_ERROR, "Failed to register iso");
        }
      
    }
}
