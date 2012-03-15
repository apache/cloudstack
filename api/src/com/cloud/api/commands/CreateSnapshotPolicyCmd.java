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
import com.cloud.api.BaseCmd;
import com.cloud.api.IdentityMapper;
import com.cloud.api.Implementation;
import com.cloud.api.Parameter;
import com.cloud.api.ServerApiException;
import com.cloud.api.response.SnapshotPolicyResponse;
import com.cloud.exception.InvalidParameterValueException;
import com.cloud.exception.PermissionDeniedException;
import com.cloud.projects.Project;
import com.cloud.storage.Volume;
import com.cloud.storage.snapshot.SnapshotPolicy;
import com.cloud.user.Account;

@Implementation(description="Creates a snapshot policy for the account.", responseObject=SnapshotPolicyResponse.class)
public class CreateSnapshotPolicyCmd extends BaseCmd {
    public static final Logger s_logger = Logger.getLogger(CreateSnapshotPolicyCmd.class.getName());

    private static final String s_name = "createsnapshotpolicyresponse";

    /////////////////////////////////////////////////////
    //////////////// API parameters /////////////////////
    /////////////////////////////////////////////////////

    @Parameter(name=ApiConstants.INTERVAL_TYPE, type=CommandType.STRING, required=true, description="valid values are HOURLY, DAILY, WEEKLY, and MONTHLY")
    private String intervalType;

    @Parameter(name=ApiConstants.MAX_SNAPS, type=CommandType.INTEGER, required=true, description="maximum number of snapshots to retain")
    private Integer maxSnaps;

    @Parameter(name=ApiConstants.SCHEDULE, type=CommandType.STRING, required=true, description="time the snapshot is scheduled to be taken. " +
    																				"Format is:" +
    																				"* if HOURLY, MM" +
    																				"* if DAILY, MM:HH" +
    																				"* if WEEKLY, MM:HH:DD (1-7)" +
    																				"* if MONTHLY, MM:HH:DD (1-28)")
    private String schedule;

    @Parameter(name=ApiConstants.TIMEZONE, type=CommandType.STRING, required=true, description="Specifies a timezone for this command. For more information on the timezone parameter, see Time Zone Format.")
    private String timezone;

    @IdentityMapper(entityTableName="volumes")
    @Parameter(name=ApiConstants.VOLUME_ID, type=CommandType.LONG, required=true, description="the ID of the disk volume")
    private Long volumeId;


    /////////////////////////////////////////////////////
    /////////////////// Accessors ///////////////////////
    /////////////////////////////////////////////////////

    public String getIntervalType() {
        return intervalType;
    }

    public Integer getMaxSnaps() {
        return maxSnaps;
    }

    public String getSchedule() {
        return schedule;
    }

    public String getTimezone() {
        return timezone;
    }

    public Long getVolumeId() {
        return volumeId;
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
        Volume volume = _entityMgr.findById(Volume.class, getVolumeId());
        if (volume == null) {
        	throw new InvalidParameterValueException("Unable to find volume by id=" + volumeId);
        }
        
        Account account = _accountService.getAccount(volume.getAccountId());
        //Can create templates for enabled projects/accounts only
        if (account.getType() == Account.ACCOUNT_TYPE_PROJECT) {
        	Project project = _projectService.findByProjectAccountId(volume.getAccountId());
            if (project.getState() != Project.State.Active) {
            	PermissionDeniedException ex = new PermissionDeniedException("Can't add resources to the specified project id in state=" + project.getState() + " as it's no longer active");
                ex.addProxyObject(project, project.getId(), "projectId");
                throw ex;
            }
        } else if (account.getState() == Account.State.disabled) {
            throw new PermissionDeniedException("The owner of template is disabled: " + account);
        }
        
        return volume.getAccountId();
    }
    
    @Override
    public void execute(){
        SnapshotPolicy result = _snapshotService.createPolicy(this, _accountService.getAccount(getEntityOwnerId()));
        if (result != null) {
            SnapshotPolicyResponse response = _responseGenerator.createSnapshotPolicyResponse(result);
            response.setResponseName(getCommandName());
            this.setResponseObject(response);
        } else {
            throw new ServerApiException(BaseCmd.INTERNAL_ERROR, "Failed to create snapshot policy");
        }
    }
}
