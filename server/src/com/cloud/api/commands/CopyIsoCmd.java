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
import com.cloud.api.response.ApiResponseSerializer;
import com.cloud.api.response.TemplateResponse;
import com.cloud.storage.GuestOS;
import com.cloud.storage.VMTemplateHostVO;
import com.cloud.storage.VMTemplateStorageResourceAssoc.Status;
import com.cloud.storage.VMTemplateVO;
import com.cloud.user.Account;
import com.cloud.user.UserContext;

@Implementation(method="copyIso", manager=Manager.TemplateManager)
public class CopyIsoCmd extends BaseAsyncCmd {
	public static final Logger s_logger = Logger.getLogger(CopyIsoCmd.class.getName());
    private static final String s_name = "copyisoresponse";

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
	public String getResponse() {
        TemplateResponse isoResponse = new TemplateResponse();
        VMTemplateVO iso = (VMTemplateVO)getResponseObject();
        
        if (iso != null) {
            isoResponse.setId(iso.getId());
            isoResponse.setName(iso.getName());
            isoResponse.setDisplayText(iso.getDisplayText());
            isoResponse.setPublic(iso.isPublicTemplate());
            isoResponse.setBootable(iso.isBootable());
            isoResponse.setFeatured(iso.isFeatured());
            isoResponse.setCrossZones(iso.isCrossZones());
            isoResponse.setCreated(iso.getCreated());
            isoResponse.setZoneId(destZoneId);
            isoResponse.setZoneName(ApiDBUtils.findZoneById(destZoneId).getName());
             
            GuestOS os = ApiDBUtils.findGuestOSById(iso.getGuestOSId());
            if (os != null) {
                isoResponse.setOsTypeId(os.getId());
                isoResponse.setOsTypeName(os.getDisplayName());
            } else {
                isoResponse.setOsTypeId(-1L);
                isoResponse.setOsTypeName("");
            }
                
            // add account ID and name
            Account owner = ApiDBUtils.findAccountById(iso.getAccountId());
            if (owner != null) {
                isoResponse.setAccount(owner.getAccountName());
                isoResponse.setDomainId(owner.getDomainId());
                isoResponse.setDomainName(ApiDBUtils.findDomainById(owner.getDomainId()).getName());
            }
            
            //set status 
            Account account = (Account)UserContext.current().getAccountObject();
            boolean isAdmin = false;
            if ((account == null) || (account.getType() == Account.ACCOUNT_TYPE_ADMIN) || (account.getType() == Account.ACCOUNT_TYPE_DOMAIN_ADMIN)) {
                isAdmin = true;
            }
    		
    		//Return download status for admin users
            VMTemplateHostVO templateHostRef = ApiDBUtils.findTemplateHostRef(iso.getId(), destZoneId);
            
    		if (isAdmin || iso.getAccountId() == account.getId().longValue()) {
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
                    isoResponse.setStatus(templateStatus);
                } else if (templateHostRef.getDownloadState() == VMTemplateHostVO.Status.DOWNLOADED) {
                	isoResponse.setStatus("Download Complete");
                } else {
                	isoResponse.setStatus("Successfully Installed");
                }
            }
    		
    		isoResponse.setReady(templateHostRef.getDownloadState() == VMTemplateHostVO.Status.DOWNLOADED);
            
        } else {
        	throw new ServerApiException(BaseCmd.INTERNAL_ERROR, "Failed to copy iso");
        }

        isoResponse.setResponseName(getName());
        return ApiResponseSerializer.toSerializedString(isoResponse);
	}

}
