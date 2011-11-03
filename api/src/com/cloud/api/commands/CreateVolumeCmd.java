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
import com.cloud.api.IdentityMapper;
import com.cloud.api.Implementation;
import com.cloud.api.Parameter;
import com.cloud.api.ServerApiException;
import com.cloud.api.BaseCmd.CommandType;
import com.cloud.api.response.VolumeResponse;
import com.cloud.async.AsyncJob;
import com.cloud.event.EventTypes;
import com.cloud.exception.ResourceAllocationException;
import com.cloud.storage.Volume;
import com.cloud.user.Account;
import com.cloud.user.UserContext;

@Implementation(responseObject=VolumeResponse.class, description="Creates a disk volume from a disk offering. This disk volume must still be attached to a virtual machine to make use of it.")
public class CreateVolumeCmd extends BaseAsyncCreateCmd {
	public static final Logger s_logger = Logger.getLogger(CreateVolumeCmd.class.getName());
    private static final String s_name = "createvolumeresponse";

    /////////////////////////////////////////////////////
    //////////////// API parameters /////////////////////
    /////////////////////////////////////////////////////

    @Parameter(name=ApiConstants.ACCOUNT, type=CommandType.STRING, description="the account associated with the disk volume. Must be used with the domainId parameter.")
    private String accountName;
    
    @IdentityMapper(entityTableName="projects")
    @Parameter(name=ApiConstants.PROJECT_ID, type=CommandType.LONG, description="the project associated with the volume. Mutually exclusive with account parameter")
    private Long projectId;
    
    @IdentityMapper(entityTableName="domain")
    @Parameter(name=ApiConstants.DOMAIN_ID, type=CommandType.LONG, description="the domain ID associated with the disk offering. If used with the account parameter returns the disk volume associated with the account for the specified domain.")
    private Long domainId;

    @IdentityMapper(entityTableName="disk_offering")
    @Parameter(name=ApiConstants.DISK_OFFERING_ID,required = false, type=CommandType.LONG, description="the ID of the disk offering. Either diskOfferingId or snapshotId must be passed in.")
    private Long diskOfferingId;

    @Parameter(name=ApiConstants.NAME, type=CommandType.STRING, required=true, description="the name of the disk volume")
    private String volumeName;

    @Parameter(name=ApiConstants.SIZE, type=CommandType.LONG, description="Arbitrary volume size")
    private Long size;

    @IdentityMapper(entityTableName="snapshots")
    @Parameter(name=ApiConstants.SNAPSHOT_ID, type=CommandType.LONG, description="the snapshot ID for the disk volume. Either diskOfferingId or snapshotId must be passed in.")
    private Long snapshotId;

    @IdentityMapper(entityTableName="data_center")
    @Parameter(name=ApiConstants.ZONE_ID, type=CommandType.LONG, description="the ID of the availability zone")
    private Long zoneId;


    /////////////////////////////////////////////////////
    /////////////////// Accessors ///////////////////////
    /////////////////////////////////////////////////////
    
    public String getEntityTable() {
    	return "volumes";
    }

    public String getAccountName() {
        return accountName;
    }

    public Long getDiskOfferingId() {
        return diskOfferingId;
    }

    public Long getDomainId() {
        return domainId;
    }

    public String getVolumeName() {
        return volumeName;
    }

    public Long getSize() {
        return size;
    }

    public Long getSnapshotId() {
        return snapshotId;
    }

    public Long getZoneId() {
        return zoneId;
    }

    private Long getProjectId() {
        return projectId;
    }

    /////////////////////////////////////////////////////
    /////////////// API Implementation///////////////////
    /////////////////////////////////////////////////////
    @Override
    public String getCommandName() {
        return s_name;
    }
    
    public static String getResultObjectName() {
    	return "volume";
    }
    
    public AsyncJob.Type getInstanceType() {
    	return AsyncJob.Type.Volume;
    }
    
    @Override
    public long getEntityOwnerId() {
        Long accountId = getAccountId(accountName, domainId, projectId);
        if (accountId == null) {
            return UserContext.current().getCaller().getId();
        }
        
        return accountId;
    }

    @Override
    public String getEventType() {
        return EventTypes.EVENT_VOLUME_CREATE;
    }

    @Override
    public String getEventDescription() {
        return  "creating volume: " + getVolumeName() + ((getSnapshotId() == null) ? "" : " from snapshot: " + getSnapshotId());
    }
    
    @Override
    public void create() throws ResourceAllocationException{

        Volume volume = _storageService.allocVolume(this);
        if (volume != null) {
            this.setEntityId(volume.getId());
        } else {
            throw new ServerApiException(BaseCmd.INTERNAL_ERROR, "Failed to create volume");
        }  
    }
    
    @Override
    public void execute(){
        UserContext.current().setEventDetails("Volume Id: "+getEntityId()+((getSnapshotId() == null) ? "" : " from snapshot: " + getSnapshotId()));
        Volume volume = _storageService.createVolume(this);
        if (volume != null) {
            VolumeResponse response = _responseGenerator.createVolumeResponse(volume);
            //FIXME - have to be moved to ApiResponseHelper
            response.setSnapshotId(getSnapshotId());  // if the volume was created from a snapshot, snapshotId will be set so we pass it back in the response
            response.setResponseName(getCommandName());
            this.setResponseObject(response);
        } else {
            throw new ServerApiException(BaseCmd.INTERNAL_ERROR, "Failed to create a volume");
        }
    }
}
