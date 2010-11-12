/**
 *  Copyright (C) 2010 Cloud.com, Inc.  All rights reserved.
 * 
 * This software is licensed under the GNU General License v3 or later.
 * 
 * It is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General License as published by
 * the Free Software Foundation, either version 3 of the License, or any later version.
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General License for more details.
 * 
 * You should have received a copy of the GNU General License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 * 
 */
package com.cloud.api.commands;

import java.net.URISyntaxException;

import org.apache.log4j.Logger;

import com.cloud.api.ApiConstants;
import com.cloud.api.ApiDBUtils;
import com.cloud.api.BaseAsyncCmd;
import com.cloud.api.BaseCmd;
import com.cloud.api.Implementation;
import com.cloud.api.Parameter;
import com.cloud.api.ServerApiException;
import com.cloud.api.response.ExtractResponse;
import com.cloud.event.EventTypes;
import com.cloud.exception.ConcurrentOperationException;
import com.cloud.exception.InsufficientAddressCapacityException;
import com.cloud.exception.InsufficientCapacityException;
import com.cloud.exception.InvalidParameterValueException;
import com.cloud.exception.PermissionDeniedException;
import com.cloud.exception.ResourceUnavailableException;
import com.cloud.storage.UploadVO;
import com.cloud.storage.VolumeVO;
import com.cloud.user.Account;

@Implementation(description="Extracts volume", responseObject=ExtractResponse.class)
public class ExtractVolumeCmd extends BaseAsyncCmd {
	public static final Logger s_logger = Logger.getLogger(ExtractVolumeCmd.class.getName());

    private static final String s_name = "extractvolumeresponse";

    /////////////////////////////////////////////////////
    //////////////// API parameters /////////////////////
    /////////////////////////////////////////////////////

    //FIXME - add description
    @Parameter(name=ApiConstants.ID, type=CommandType.LONG, required=true, description="the ID of the volume")
    private Long id;

    //FIXME - add description
    @Parameter(name=ApiConstants.URL, type=CommandType.STRING, required=false, description="the url to which the volume would be extracted")
    private String url;

    //FIXME - add description
    @Parameter(name=ApiConstants.ZONE_ID, type=CommandType.LONG, required=true, description="the ID of the zone where the volume is located")
    private Long zoneId;
    
    @Parameter(name=ApiConstants.MODE, type=CommandType.STRING, required=true, description="the mode of extraction - HTTP_DOWNLOAD or FTP_UPLOAD")
    private String mode;

    /////////////////////////////////////////////////////
    /////////////////// Accessors ///////////////////////
    /////////////////////////////////////////////////////

    public Long getId() {
        return id;
    }

    public String getUrl() {
        return url;
    }

    public Long getZoneId() {
        return zoneId;
    }
    
    public String getMode() {
        return mode;
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
        VolumeVO volume = ApiDBUtils.findVolumeById(getId());
        if (volume != null) {
            return volume.getAccountId();
        }

        // invalid id, parent this command to SYSTEM so ERROR events are tracked
        return Account.ACCOUNT_ID_SYSTEM;
    }

    @Override
    public String getEventType() {
        return EventTypes.EVENT_VOLUME_EXTRACT;
    }

    @Override
    public String getEventDescription() {
        return  "Extraction job";
    }
	
    @Override
    public void execute() throws ServerApiException, InvalidParameterValueException, PermissionDeniedException, InsufficientAddressCapacityException, InsufficientCapacityException, ConcurrentOperationException, ResourceUnavailableException{
        try {
            Long uploadId = BaseCmd._mgr.extractVolume(this);
            UploadVO uploadInfo = ApiDBUtils.findUploadById(uploadId);
            
            ExtractResponse response = new ExtractResponse();
            response.setResponseName(getName());
            response.setObjectName("volume");
            response.setId(id);
            response.setName(ApiDBUtils.findVolumeById(id).getName());        
            response.setZoneId(zoneId);
            response.setZoneName(ApiDBUtils.findZoneById(zoneId).getName());
            response.setMode(mode);
            response.setUploadId(uploadId);
            response.setState(uploadInfo.getUploadState().toString());
            response.setAccountId(getAccountId());        
            //FIX ME - Need to set the url once the gson jar is upgraded since it is throwing an error right now.
            response.setUrl(uploadInfo.getUploadUrl().replaceAll("/", "%2F"));
            this.setResponseObject(response);
        } catch (URISyntaxException ex) {
            throw new ServerApiException(BaseCmd.INTERNAL_ERROR, ex.getMessage());
        }
    }
}
