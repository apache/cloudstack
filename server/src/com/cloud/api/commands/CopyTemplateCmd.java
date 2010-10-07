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
import com.cloud.api.BaseCmd.Manager;
import com.cloud.api.Implementation;
import com.cloud.api.Parameter;
import com.cloud.api.ServerApiException;
import com.cloud.api.response.TemplateResponse;
import com.cloud.event.EventTypes;
import com.cloud.storage.GuestOS;
import com.cloud.storage.VMTemplateHostVO;
import com.cloud.storage.VMTemplateStorageResourceAssoc.Status;
import com.cloud.storage.VMTemplateVO;
import com.cloud.user.Account;
import com.cloud.user.UserContext;

@Implementation(method="copyTemplate", manager=Manager.TemplateManager, description="Copies a template from one zone to another.")
public class CopyTemplateCmd extends BaseAsyncCmd {
	public static final Logger s_logger = Logger.getLogger(CopyTemplateCmd.class.getName());
    private static final String s_name = "copytemplateresponse";

    /////////////////////////////////////////////////////
    //////////////// API parameters /////////////////////
    /////////////////////////////////////////////////////

    @Parameter(name="destzoneid", type=CommandType.LONG, required=true)
    private Long destZoneId;

    @Parameter(name="id", type=CommandType.LONG, required=true)
    private Long id;

    @Parameter(name="sourcezoneid", type=CommandType.LONG, required=true)
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
        VMTemplateVO template = ApiDBUtils.findTemplateById(getId());
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

	@Override @SuppressWarnings("unchecked")
	public TemplateResponse getResponse() {
        TemplateResponse templateResponse = new TemplateResponse();
        VMTemplateVO template = (VMTemplateVO)getResponseObject();
        
        if (template != null) {
            templateResponse.setId(template.getId());
            templateResponse.setName(template.getName());
            templateResponse.setDisplayText(template.getDisplayText());
            templateResponse.setPublic(template.isPublicTemplate());
            templateResponse.setBootable(template.isBootable());
            templateResponse.setFeatured(template.isFeatured());
            templateResponse.setCrossZones(template.isCrossZones());
            templateResponse.setCreated(template.getCreated());
            templateResponse.setFormat(template.getFormat());
            templateResponse.setPasswordEnabled(template.getEnablePassword());
            templateResponse.setZoneId(destZoneId);
            templateResponse.setZoneName(ApiDBUtils.findZoneById(destZoneId).getName());
             
            GuestOS os = ApiDBUtils.findGuestOSById(template.getGuestOSId());
            if (os != null) {
                templateResponse.setOsTypeId(os.getId());
                templateResponse.setOsTypeName(os.getDisplayName());
            } else {
                templateResponse.setOsTypeId(-1L);
                templateResponse.setOsTypeName("");
            }
                
            // add account ID and name
            Account owner = ApiDBUtils.findAccountById(template.getAccountId());
            if (owner != null) {
                templateResponse.setAccount(owner.getAccountName());
                templateResponse.setDomainId(owner.getDomainId());
                templateResponse.setDomainName(ApiDBUtils.findDomainById(owner.getDomainId()).getName());
            }
            
            //set status 
            Account account = (Account)UserContext.current().getAccountObject();
            boolean isAdmin = false;
            if ((account == null) || (account.getType() == Account.ACCOUNT_TYPE_ADMIN) || (account.getType() == Account.ACCOUNT_TYPE_DOMAIN_ADMIN)) {
                isAdmin = true;
            }
    		
    		//Return download status for admin users
            VMTemplateHostVO templateHostRef = ApiDBUtils.findTemplateHostRef(template.getId(), destZoneId);
            
    		if (isAdmin || template.getAccountId() == account.getId()) {
                if (templateHostRef.getDownloadState()!=Status.DOWNLOADED) {
                    String templateStatus = "Processing";
                    if (templateHostRef.getDownloadState() == VMTemplateHostVO.Status.DOWNLOAD_IN_PROGRESS) {
                        if (templateHostRef.getDownloadPercent() == 100) {
                            templateStatus = "Installing Template";
                        } else {
                            templateStatus = templateHostRef.getDownloadPercent() + "% Downloaded";
                        }
                    } else {
                        templateStatus = templateHostRef.getErrorString();
                    }
                    templateResponse.setStatus(templateStatus);
                } else if (templateHostRef.getDownloadState() == VMTemplateHostVO.Status.DOWNLOADED) {
                	templateResponse.setStatus("Download Complete");
                } else {
                	templateResponse.setStatus("Successfully Installed");
                }
            }
    		
    		templateResponse.setReady(templateHostRef != null && templateHostRef.getDownloadState() == VMTemplateHostVO.Status.DOWNLOADED);
            
        } else {
        	throw new ServerApiException(BaseCmd.INTERNAL_ERROR, "Failed to copy template");
        }
        
        templateResponse.setResponseName(getName());
        return templateResponse;
	}
}

