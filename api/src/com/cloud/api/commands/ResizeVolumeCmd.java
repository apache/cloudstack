// Licensed to the Apache Software Foundation (ASF) under one
// or more contributor license agreements.  See the NOTICE file
// distributed with this work for additional information
// regarding copyright ownership.  The ASF licenses this file
// to you under the Apache License, Version 2.0 (the
// "License"); you may not use this file except in compliance
// with the License.  You may obtain a copy of the License at
//
//   http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing,
// software distributed under the License is distributed on an
// "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
// KIND, either express or implied.  See the License for the
// specific language governing permissions and limitations
// under the License.
package com.cloud.api.commands;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;
import java.util.regex.Matcher;

import org.apache.log4j.Logger;

import com.cloud.api.ApiConstants;
import com.cloud.api.BaseListTaggedResourcesCmd;
import com.cloud.api.BaseCmd;
import com.cloud.api.BaseAsyncCmd;
import com.cloud.api.IdentityMapper;
import com.cloud.api.Implementation;
import com.cloud.api.Parameter;
import com.cloud.api.response.ListResponse;
import com.cloud.api.response.VolumeResponse;
import com.cloud.api.ServerApiException;
import com.cloud.exception.InvalidParameterValueException;
import com.cloud.exception.PermissionDeniedException;
import com.cloud.exception.ResourceAllocationException;
import com.cloud.async.AsyncJob;
import com.cloud.event.EventTypes;
import com.cloud.projects.Project;
import com.cloud.storage.Volume;
import com.cloud.user.Account;
import com.cloud.user.UserContext;


@Implementation(description="Resizes a volume", responseObject=VolumeResponse.class)
public class ResizeVolumeCmd extends BaseAsyncCmd {
    public static final Logger s_logger = Logger.getLogger(ResizeVolumeCmd.class.getName());

    private static final String s_name = "resizevolumeresponse";

    /////////////////////////////////////////////////////
    //////////////// API parameters /////////////////////
    /////////////////////////////////////////////////////

    @IdentityMapper(entityTableName="volumes")
    @Parameter(name=ApiConstants.ID, type=CommandType.LONG, description="the ID of the disk volume")
    private Long id;

    @Parameter(name=ApiConstants.SIZE, type=CommandType.LONG, required=false, description="New volume size in G")
    private Long size;

    @Parameter(name=ApiConstants.SHRINK_OK, type=CommandType.BOOLEAN, required=false, description="Verify OK to Shrink")
    private boolean shrinkOk;

    @IdentityMapper(entityTableName="disk_offering")
    @Parameter(name=ApiConstants.DISK_OFFERING_ID, type=CommandType.LONG, required=false, description="new disk offering id")
    private Long newDiskOfferingId;
    
    /////////////////////////////////////////////////////
    /////////////////// Accessors ///////////////////////
    /////////////////////////////////////////////////////

    public Long getEntityId() {
        return id;
    }
    
    public Long getSize() {
        return size;
    }

    public boolean getShrinkOk() {
        return shrinkOk;
    }

    public Long getNewDiskOfferingId() {
        return newDiskOfferingId;
    }

    
    /////////////////////////////////////////////////////
    /////////////// API Implementation///////////////////
    /////////////////////////////////////////////////////

    @Override
    public String getCommandName() {
        return s_name;
    }
    
    @Override
    public AsyncJob.Type getInstanceType() {
    	return AsyncJob.Type.Volume;
    }

    public static String getResultObjectName() {
        return "volume";
    }

   @Override
    public long getEntityOwnerId() {

        Volume volume = _entityMgr.findById(Volume.class, getEntityId());
        if (volume == null) {
                throw new InvalidParameterValueException("Unable to find volume by id=" + id);
        }

        Account account = _accountService.getAccount(volume.getAccountId());
        //Can resize volumes for enabled projects/accounts only
        if (account.getType() == Account.ACCOUNT_TYPE_PROJECT) {
                Project project = _projectService.findByProjectAccountId(volume.getAccountId());
            if (project.getState() != Project.State.Active) {
                throw new PermissionDeniedException("Can't add resources to  project id=" + project.getId() + " in state=" + project.getState() + " as it's no longer active");
            }
        } else if (account.getState() == Account.State.disabled) {
            throw new PermissionDeniedException("The owner of volume " + id + "  is disabled: " + account);
        }

        return volume.getAccountId();
    }


    @Override
    public String getEventType() {
        return EventTypes.EVENT_VOLUME_RESIZE;
    }

    @Override
    public String getEventDescription() {
        return "Volume Id: " + getEntityId() + " to size " + getSize() + "G" ;
    }
    
    @Override
    public void execute(){
        UserContext.current().setEventDetails("Volume Id: " + getEntityId() + " to size " + getSize() + "G");
    	Volume volume = _storageService.resizeVolume(this);
    	if (volume != null) {
            VolumeResponse response = _responseGenerator.createVolumeResponse(volume);
            //FIXME - have to be moved to ApiResponseHelper
            response.setResponseName(getCommandName());
            this.setResponseObject(response);
        } else {
            throw new ServerApiException(BaseCmd.INTERNAL_ERROR, "Failed to resize volume");
        }
    }
}
