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

import com.cloud.api.BaseAsyncCreateCmd;
import com.cloud.api.BaseCmd.Manager;
import com.cloud.api.Implementation;
import com.cloud.api.Parameter;
import com.cloud.api.response.TemplateResponse;
import com.cloud.serializer.SerializerHelper;
import com.cloud.storage.VMTemplateVO;

@Implementation(method="createPrivateTemplate", createMethod="createPrivateTemplateRecord", manager=Manager.UserVmManager)
public class CreateTemplateCmd extends BaseAsyncCreateCmd {
    public static final Logger s_logger = Logger.getLogger(CreateTemplateCmd.class.getName());
    private static final String s_name = "createtemplateresponse";

    /////////////////////////////////////////////////////
    //////////////// API parameters /////////////////////
    /////////////////////////////////////////////////////

    @Parameter(name="bits", type=CommandType.INTEGER)
    private Integer bits;

    @Parameter(name="displaytext", type=CommandType.STRING, required=true)
    private String displayText;

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

    @Parameter(name="snapshotid", type=CommandType.LONG)
    private Long snapshotId;

    @Parameter(name="volumeid", type=CommandType.LONG)
    private Long volumeId;


    /////////////////////////////////////////////////////
    /////////////////// Accessors ///////////////////////
    /////////////////////////////////////////////////////

    public Integer getBits() {
        return bits;
    }

    public String getDisplayText() {
        return displayText;
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

    public Long getSnapshotId() {
        return snapshotId;
    }

    public Long getVolumeId() {
        return volumeId;
    }


    /////////////////////////////////////////////////////
    /////////////// API Implementation///////////////////
    /////////////////////////////////////////////////////

    @Override
    public String getName() {
        return s_name;
    }
    
    public static String getResultObjectName() {
    	return "template";  
    }

    @Override
    public String getResponse() {
        VMTemplateVO template = (VMTemplateVO)getResponseObject();

        TemplateResponse response = new TemplateResponse();
        response.setId(template.getId());
        response.setName(template.getName());
        response.setDisplayText(template.getDisplayText());
        response.setPublic(template.isPublicTemplate());
        response.setPasswordEnabled(template.getEnablePassword());
        response.setCrossZones(template.isCrossZones());

        // TODO: implement
//        VMTemplateHostVO templateHostRef = managerServer.findTemplateHostRef(template.getId(), volume.getDataCenterId());
//        response.setCreated(templateHostRef.getCreated());
//        response.setReady(templateHostRef != null && templateHostRef.getDownloadState() == Status.DOWNLOADED);
//        if (os != null) {
//            resultObject.setOsTypeId(os.getId());
//            resultObject.setOsTypeName(os.getDisplayName());
//        } else {
//            resultObject.setOsTypeId(-1L);
//            resultObject.setOsTypeName("");
//        }
//        Account owner = managerServer.findAccountById(template.getAccountId());
//        if (owner != null) {
//            resultObject.setAccount(owner.getAccountName());
//            resultObject.setDomainId(owner.getDomainId());
//            resultObject.setDomainName(managerServer.findDomainIdById(owner.getDomainId()).getName());
//        }
//        DataCenterVO zone = managerServer.findDataCenterById(dataCenterId);
//        if (zone != null) {
//            resultObject.setZoneId(zone.getId());
//            resultObject.setZoneName(zone.getName());
//        }

        return SerializerHelper.toSerializedString(response);
    }
}
