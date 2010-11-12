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
import com.cloud.api.ApiDBUtils;
import com.cloud.api.BaseAsyncCmd;
import com.cloud.api.BaseCmd;
import com.cloud.api.Implementation;
import com.cloud.api.Parameter;
import com.cloud.api.ServerApiException;
import com.cloud.api.response.SnapshotResponse;
import com.cloud.event.EventTypes;
import com.cloud.exception.ConcurrentOperationException;
import com.cloud.exception.InsufficientAddressCapacityException;
import com.cloud.exception.InsufficientCapacityException;
import com.cloud.exception.InvalidParameterValueException;
import com.cloud.exception.PermissionDeniedException;
import com.cloud.exception.ResourceAllocationException;
import com.cloud.storage.Snapshot.SnapshotType;
import com.cloud.storage.SnapshotVO;
import com.cloud.storage.VolumeVO;
import com.cloud.user.Account;

@Implementation(description="Creates an instant snapshot of a volume.", responseObject=SnapshotResponse.class)
public class CreateSnapshotInternalCmd extends BaseAsyncCmd {
    public static final Logger s_logger = Logger.getLogger(CreateSnapshotInternalCmd.class.getName());
    private static final String s_name = "createsnapshotresponse";

    /////////////////////////////////////////////////////
    //////////////// API parameters /////////////////////
    /////////////////////////////////////////////////////

    @Parameter(name=ApiConstants.POLICY_ID, type=CommandType.LONG)
    private Long policyId;

    @Parameter(name=ApiConstants.VOLUME_ID, type=CommandType.LONG, required=true)
    private Long volumeId;

    /////////////////////////////////////////////////////
    /////////////////// Accessors ///////////////////////
    /////////////////////////////////////////////////////

    public Long getPolicyId() {
        return policyId;
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
        return "snapshot";
    }

    @Override
    public long getAccountId() {
        VolumeVO volume = ApiDBUtils.findVolumeById(getVolumeId());
        if (volume != null) {
            return volume.getAccountId();
        }

        // bad id given, parent this command to SYSTEM so ERROR events are tracked
        return Account.ACCOUNT_ID_SYSTEM;
    }

    @Override
    public String getEventType() {
        return EventTypes.EVENT_SNAPSHOT_CREATE;
    }

    @Override
    public String getEventDescription() {
        return  "creating snapshot for volume: " + getVolumeId();
    }

    @Override
    public void execute() throws ServerApiException, InvalidParameterValueException, PermissionDeniedException, InsufficientAddressCapacityException, InsufficientCapacityException, ConcurrentOperationException{
        try {
            SnapshotVO snapshot = BaseCmd._snapshotMgr.createSnapshotInternal(this);
            SnapshotResponse response = new SnapshotResponse();
            response.setId(snapshot.getId());

            Account account = ApiDBUtils.findAccountById(snapshot.getAccountId());
            if (account != null) {
                response.setAccountName(account.getAccountName());
                response.setDomainId(account.getDomainId());
                response.setDomainName(ApiDBUtils.findDomainById(account.getDomainId()).getName());
            }

            VolumeVO volume = ApiDBUtils.findVolumeById(snapshot.getVolumeId());
            String snapshotTypeStr = SnapshotType.values()[snapshot.getSnapshotType()].name();
            response.setSnapshotType(snapshotTypeStr);
            response.setVolumeId(snapshot.getVolumeId());
            response.setVolumeName(volume.getName());
            response.setVolumeType(volume.getVolumeType().toString());
            response.setCreated(snapshot.getCreated());
            response.setName(snapshot.getName());
            response.setObjectName("snapshot");
            response.setResponseName(getName());
            this.setResponseObject(response);
        } catch (ResourceAllocationException ex) {
            throw new ServerApiException(BaseCmd.INTERNAL_ERROR, ex.getMessage());
        }
    }
}
