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

import com.cloud.api.ApiDBUtils;
import com.cloud.api.BaseCmd;
import com.cloud.api.BaseCmd.Manager;
import com.cloud.api.Implementation;
import com.cloud.api.Parameter;
import com.cloud.api.response.ListResponse;
import com.cloud.api.response.TemplateResponse;
import com.cloud.dc.DataCenterVO;
import com.cloud.storage.GuestOS;
import com.cloud.storage.VMTemplateHostVO;
import com.cloud.storage.VMTemplateStorageResourceAssoc.Status;
import com.cloud.storage.VMTemplateVO;
import com.cloud.user.Account;

@Implementation(method="registerTemplate", manager=Manager.TemplateManager)
public class RegisterTemplateCmd extends BaseCmd {
	public static final Logger s_logger = Logger.getLogger(RegisterTemplateCmd.class.getName());

    private static final String s_name = "registertemplateresponse";

    /////////////////////////////////////////////////////
    //////////////// API parameters /////////////////////
    /////////////////////////////////////////////////////

    @Parameter(name="bits", type=CommandType.INTEGER)
    private Integer bits;

    @Parameter(name="displaytext", type=CommandType.STRING, required=true)
    private String displayText;

    @Parameter(name="format", type=CommandType.STRING, required=true)
    private String format;

    @Parameter(name="isfeatured", type=CommandType.BOOLEAN)
    private Boolean featured;

    @Parameter(name="ispublic", type=CommandType.BOOLEAN)
    private Boolean publicTemplate;

    @Parameter(name="name", type=CommandType.STRING, required=true)
    private String templateName;

    @Parameter(name="ostypeid", type=CommandType.LONG, required=true)
    private Long osTypeId;

    @Parameter(name="passwordenabled", type=CommandType.BOOLEAN)
    private Boolean passwordEnabled;

    @Parameter(name="requireshvm", type=CommandType.BOOLEAN)
    private Boolean requiresHvm;

    @Parameter(name="url", type=CommandType.STRING, required=true)
    private String url;

    @Parameter(name="zoneid", type=CommandType.LONG, required=true)
    private Long zoneId;

    /////////////////////////////////////////////////////
    /////////////////// Accessors ///////////////////////
    /////////////////////////////////////////////////////

    public Integer getBits() {
        return bits;
    }

    public String getDisplayText() {
        return displayText;
    }

    public String getFormat() {
        return format;
    }

    public Boolean isFeatured() {
        return featured;
    }

    public Boolean isPublic() {
        return publicTemplate;
    }

    public String getTemplateName() {
        return templateName;
    }

    public Long getOsTypeId() {
        return osTypeId;
    }

    public Boolean isPasswordEnabled() {
        return passwordEnabled;
    }

    public Boolean getRequiresHvm() {
        return requiresHvm;
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

        if (zoneId != null) {
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
            templateResponse.setCrossZones(template.isCrossZones());

            VMTemplateHostVO isoHostRef = ApiDBUtils.findTemplateHostRef(template.getId(), zone.getId());
            if (isoHostRef != null) {
                templateResponse.setCreated(isoHostRef.getCreated());
                templateResponse.setReady(isoHostRef.getDownloadState() == Status.DOWNLOADED);
            }

            templateResponse.setFeatured(template.isFeatured());
            templateResponse.setPasswordEnabled(template.getEnablePassword());
            templateResponse.setFormat(template.getFormat());
            templateResponse.setStatus("Processing");

            GuestOS os = ApiDBUtils.findGuestOSById(template.getGuestOSId());
            if (os != null) {
                templateResponse.setOsTypeId(os.getId());
                templateResponse.setOsTypeName(os.getDisplayName());
            } else {
                templateResponse.setOsTypeId(-1L);
                templateResponse.setOsTypeName("");
            }
              
            Account owner = ApiDBUtils.findAccountById(template.getAccountId());
            if (owner != null) {
                templateResponse.setAccountId(owner.getId());
                templateResponse.setAccount(owner.getAccountName());
                templateResponse.setDomainId(owner.getDomainId());
            }

            templateResponse.setZoneId(zone.getId());
            templateResponse.setZoneName(zone.getName());
            templateResponse.setResponseName("template");

            responses.add(templateResponse);
        }
        response.setResponseName(getName());
        response.setResponses(responses);
        return response;
	}
}
