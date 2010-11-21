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
import com.cloud.api.BaseAsyncCreateCmd;
import com.cloud.api.BaseCmd;
import com.cloud.api.Implementation;
import com.cloud.api.Parameter;
import com.cloud.api.ServerApiException;
import com.cloud.api.response.StoragePoolResponse;
import com.cloud.api.response.TemplateResponse;
import com.cloud.event.EventTypes;
import com.cloud.storage.Snapshot;
import com.cloud.storage.Volume;
import com.cloud.template.VirtualMachineTemplate;
import com.cloud.user.Account;

@Implementation(responseObject=StoragePoolResponse.class, description="Creates a template of a virtual machine. " +
																															"The virtual machine must be in a STOPPED state. " +
																															"A template created from this command is automatically designated as a private template visible to the account that created it.")
public class CreateTemplateCmd extends BaseAsyncCreateCmd {
    public static final Logger s_logger = Logger.getLogger(CreateTemplateCmd.class.getName());
    private static final String s_name = "createtemplateresponse";

    /////////////////////////////////////////////////////
    //////////////// API parameters /////////////////////
    /////////////////////////////////////////////////////

    @Parameter(name=ApiConstants.BITS, type=CommandType.INTEGER)
    private Integer bits;

    @Parameter(name=ApiConstants.DISPLAY_TEXT, type=CommandType.STRING, required=true, description="the display text of the template. This is usually used for display purposes.")
    private String displayText;

    @Parameter(name=ApiConstants.IS_FEATURED, type=CommandType.BOOLEAN, description="true if this template is a featured template, false otherwise")
    private Boolean featured;

    @Parameter(name=ApiConstants.IS_PUBLIC, type=CommandType.BOOLEAN, description="true if this template is a public template, false otherwise")
    private Boolean publicTemplate;

    @Parameter(name=ApiConstants.NAME, type=CommandType.STRING, required=true, description="the name of the template")
    private String templateName;

    @Parameter(name=ApiConstants.OS_TYPE_ID, type=CommandType.LONG, required=true, description="the ID of the OS Type that best represents the OS of this template.")
    private Long osTypeId;

    @Parameter(name=ApiConstants.PASSWORD_ENABLED, type=CommandType.BOOLEAN, description="true if the template supports the password reset feature; default is false")
    private Boolean passwordEnabled;

    @Parameter(name=ApiConstants.REQUIRES_HVM, type=CommandType.BOOLEAN, description="true if the template requres HVM, false otherwise")
    private Boolean requiresHvm;

    @Parameter(name=ApiConstants.SNAPSHOT_ID, type=CommandType.LONG, description="the ID of the snapshot the template is being created from")
    private Long snapshotId;

    @Parameter(name=ApiConstants.VOLUME_ID, type=CommandType.LONG, description="the ID of the disk volume the template is being created from")
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
    public long getAccountId() {
        Long volumeId = getVolumeId();
        Long snapshotId = getSnapshotId();
        if (volumeId != null) {
            Volume volume = _entityMgr.findById(Volume.class, volumeId);
            if (volume != null) {
                return volume.getAccountId();
            }
        } else {
            Snapshot snapshot = _entityMgr.findById(Snapshot.class, snapshotId);
            if (snapshot != null) {
                return snapshot.getAccountId();
            }
        }

        // bad id given, parent this command to SYSTEM so ERROR events are tracked
        return Account.ACCOUNT_ID_SYSTEM;
    }

    @Override
    public String getEventType() {
        return EventTypes.EVENT_TEMPLATE_CREATE;
    }

    @Override
    public String getEventDescription() {
        return  "creating template: " + getTemplateName();
    }

    @Override
    public void callCreate(){
        VirtualMachineTemplate template = _userVmService.createPrivateTemplateRecord(this);
        if (template != null){
            this.setId(template.getId());
        } else {
            throw new ServerApiException(BaseCmd.INTERNAL_ERROR, "Failed to create a template");
        }
    }
    
    @Override
    public void execute(){
        VirtualMachineTemplate template = _userVmService.createPrivateTemplate(this);
        if (template != null) {
            TemplateResponse response = _responseGenerator.createTemplateResponse(template, snapshotId, volumeId);
            response.setResponseName(getName());
            
            this.setResponseObject(response);
        } else {
            throw new ServerApiException(BaseCmd.INTERNAL_ERROR, "Failed to create template");
        }
    }
}
