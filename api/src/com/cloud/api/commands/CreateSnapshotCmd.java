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
import com.cloud.api.response.SnapshotResponse;
import com.cloud.async.AsyncJob;
import com.cloud.event.EventTypes;
import com.cloud.exception.ResourceAllocationException;
import com.cloud.storage.Snapshot;
import com.cloud.storage.Volume;
import com.cloud.user.Account;
import com.cloud.user.UserContext;

@Implementation(description = "Creates an instant snapshot of a volume.", responseObject = SnapshotResponse.class)
public class CreateSnapshotCmd extends BaseAsyncCreateCmd {
    public static final Logger s_logger = Logger.getLogger(CreateSnapshotCmd.class.getName());
    private static final String s_name = "createsnapshotresponse";

    // ///////////////////////////////////////////////////
    // ////////////// API parameters /////////////////////
    // ///////////////////////////////////////////////////

    @Parameter(name = ApiConstants.ACCOUNT, type = CommandType.STRING, description = "The account of the snapshot. The account parameter must be used with the domainId parameter.")
    private String accountName;

    @IdentityMapper(entityTableName="domain")
    @Parameter(name = ApiConstants.DOMAIN_ID, type = CommandType.LONG, description = "The domain ID of the snapshot. If used with the account parameter, specifies a domain for the account associated with the disk volume.")
    private Long domainId;

    @IdentityMapper(entityTableName="volumes")
    @Parameter(name = ApiConstants.VOLUME_ID, type = CommandType.LONG, required = true, description = "The ID of the disk volume")
    private Long volumeId;

    @IdentityMapper(entityTableName="snapshot_policy")
    @Parameter(name = ApiConstants.POLICY_ID, type = CommandType.LONG, description = "policy id of the snapshot, if this is null, then use MANUAL_POLICY.")
    private Long policyId;

    // ///////////////////////////////////////////////////
    // ///////////////// Accessors ///////////////////////
    // ///////////////////////////////////////////////////

    public String getAccountName() {
        return accountName;
    }

    public Long getDomainId() {
        return domainId;
    }

    public Long getVolumeId() {
        return volumeId;
    }

    public Long getPolicyId() {
        if (policyId != null) {
            return policyId;
        } else {
            return Snapshot.MANUAL_POLICY_ID;
        }
    }

    // ///////////////////////////////////////////////////
    // ///////////// API Implementation///////////////////
    // ///////////////////////////////////////////////////

    @Override
    public String getCommandName() {
        return s_name;
    }

    public static String getResultObjectName() {
        return "snapshot";
    }

    @Override
    public long getEntityOwnerId() {
        Volume volume = _entityMgr.findById(Volume.class, getVolumeId());
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
        return "creating snapshot for volume: " + getVolumeId();
    }

    @Override
    public AsyncJob.Type getInstanceType() {
        return AsyncJob.Type.Snapshot;
    }

    @Override
    public void create() throws ResourceAllocationException {
        Snapshot snapshot = _snapshotService.allocSnapshot(getVolumeId(), getPolicyId());
        if (snapshot != null) {
            this.setEntityId(snapshot.getId());
        } else {
            throw new ServerApiException(BaseCmd.INTERNAL_ERROR, "Failed to create snapshot");
        }
    }

    @Override
    public void execute() {
        UserContext.current().setEventDetails("Volume Id: "+getVolumeId());
        Snapshot snapshot = _snapshotService.createSnapshot(getVolumeId(), getPolicyId(), getEntityId());
        if (snapshot != null) {
            SnapshotResponse response = _responseGenerator.createSnapshotResponse(snapshot);
            response.setResponseName(getCommandName());
            this.setResponseObject(response);
        } else {
            throw new ServerApiException(BaseCmd.INTERNAL_ERROR, "Failed to create snapshot due to an internal error creating snapshot for volume " + volumeId);
        }
    }
}
