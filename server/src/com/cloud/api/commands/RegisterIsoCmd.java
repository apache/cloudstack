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

import java.util.ArrayList;
import java.util.List;

import org.apache.log4j.Logger;

import com.cloud.api.ApiConstants;
import com.cloud.api.ApiDBUtils;
import com.cloud.api.BaseCmd;
import com.cloud.api.Implementation;
import com.cloud.api.Parameter;
import com.cloud.api.response.ListResponse;
import com.cloud.api.response.TemplateResponse;
import com.cloud.dc.DataCenterVO;
import com.cloud.storage.VMTemplateHostVO;
import com.cloud.storage.VMTemplateStorageResourceAssoc.Status;
import com.cloud.storage.VMTemplateVO;
import com.cloud.template.TemplateManager;
import com.cloud.user.Account;

@Implementation(method="registerIso", manager=TemplateManager.class, description="Registers an existing ISO into the Cloud.com Cloud.")
public class RegisterIsoCmd extends BaseCmd {
    public static final Logger s_logger = Logger.getLogger(RegisterIsoCmd.class.getName());

    private static final String s_name = "registerisoresponse";

    /////////////////////////////////////////////////////
    //////////////// API parameters /////////////////////
    /////////////////////////////////////////////////////

    @Parameter(name=ApiConstants.BOOTABLE, type=CommandType.BOOLEAN, description="true if this ISO is bootable")
    private Boolean bootable;

    @Parameter(name=ApiConstants.DISPLAY_TEXT, type=CommandType.STRING, required=true, description="the display text of the ISO. This is usually used for display purposes.")
    private String displayText;

    @Parameter(name=ApiConstants.IS_FEATURED, type=CommandType.BOOLEAN, description="true if you want this ISO to be featured")
    private Boolean featured;

    @Parameter(name=ApiConstants.IS_PUBLIC, type=CommandType.BOOLEAN, description="true if you want to register the ISO to be publicly available to all users, false otherwise.")
    private Boolean publicIso;

    @Parameter(name=ApiConstants.NAME, type=CommandType.STRING, required=true, description="the name of the ISO")
    private String isoName;

    @Parameter(name=ApiConstants.OS_TYPE_ID, type=CommandType.LONG, required=true, description="the ID of the OS Type that best represents the OS of this ISO")
    private Long osTypeId;

    @Parameter(name=ApiConstants.URL, type=CommandType.STRING, required=true, description="the URL to where the ISO is currently being hosted")
    private String url;

    @Parameter(name=ApiConstants.ZONE_ID, type=CommandType.LONG, required=true, description="the ID of the zone you wish to register the ISO to.")
    private Long zoneId;

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

    /////////////////////////////////////////////////////
    /////////////// API Implementation///////////////////
    /////////////////////////////////////////////////////

    @Override
    public String getName() {
        return s_name;
    }

	@Override @SuppressWarnings("unchecked")
	public ListResponse<TemplateResponse> getResponse() {
	    VMTemplateVO template = (VMTemplateVO)getResponseObject();

	    ListResponse<TemplateResponse> response = new ListResponse<TemplateResponse>();
	    List<TemplateResponse> responses = new ArrayList<TemplateResponse>();
	    List<DataCenterVO> zones = null;

	    if ((zoneId != null) && (zoneId != -1)) {
	        zones = new ArrayList<DataCenterVO>();
	        zones.add(ApiDBUtils.findZoneById(zoneId));
	    } else {
	        zones = ApiDBUtils.listZones();   
	    }

	    for (DataCenterVO zone : zones) {
	        TemplateResponse templateResponse = new TemplateResponse();
	        templateResponse.setId(template.getId());
	        templateResponse.setName(template.getName());
	        templateResponse.setDisplayText(template.getDisplayText());
	        templateResponse.setPublic(template.isPublicTemplate());

	        VMTemplateHostVO isoHostRef = ApiDBUtils.findTemplateHostRef(template.getId(), zone.getId());
	        if (isoHostRef != null) {
	            templateResponse.setCreated(isoHostRef.getCreated());
	            templateResponse.setReady(isoHostRef.getDownloadState() == Status.DOWNLOADED);
	        }

	        templateResponse.setFeatured(template.isFeatured());
            templateResponse.setBootable(template.isBootable());
            templateResponse.setOsTypeId(template.getGuestOSId());
            templateResponse.setOsTypeName(ApiDBUtils.findGuestOSById(template.getGuestOSId()).getDisplayName());
              
            Account owner = ApiDBUtils.findAccountById(template.getAccountId());
            if (owner != null) {
                templateResponse.setAccountId(owner.getId());
                templateResponse.setAccount(owner.getAccountName());
                templateResponse.setDomainId(owner.getDomainId());
            }

            templateResponse.setZoneId(zone.getId());
            templateResponse.setZoneName(zone.getName());
            templateResponse.setResponseName("iso");

            responses.add(templateResponse);
	    }
        response.setResponseName(getName());
        response.setResponses(responses);
        return response;
	}
}
