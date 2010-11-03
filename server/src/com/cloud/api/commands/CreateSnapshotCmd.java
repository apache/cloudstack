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
import com.cloud.api.ApiResponseHelper;
import com.cloud.api.BaseAsyncCreateCmd;
import com.cloud.api.BaseCmd;
import com.cloud.api.Implementation;
import com.cloud.api.Parameter;
import com.cloud.api.ServerApiException;
import com.cloud.api.response.SnapshotResponse;
import com.cloud.event.EventTypes;
import com.cloud.storage.SnapshotVO;
import com.cloud.storage.VolumeVO;
import com.cloud.storage.snapshot.SnapshotManager;
import com.cloud.user.Account;

@Implementation(createMethod="createSnapshotDB", method="createSnapshot", manager=SnapshotManager.class, description="Creates an instant snapshot of a volume.")
public class CreateSnapshotCmd extends BaseAsyncCreateCmd {
	public static final Logger s_logger = Logger.getLogger(CreateSnapshotCmd.class.getName());
	private static final String s_name = "createsnapshotresponse";

    /////////////////////////////////////////////////////
    //////////////// API parameters /////////////////////
    /////////////////////////////////////////////////////

    @Parameter(name=ApiConstants.ACCOUNT, type=CommandType.STRING, description="The account of the snapshot. The account parameter must be used with the domainId parameter.")
    private String accountName;

    @Parameter(name=ApiConstants.DOMAIN_ID, type=CommandType.LONG, description="The domain ID of the snapshot. If used with the account parameter, specifies a domain for the account associated with the disk volume.")
    private Long domainId;

    @Parameter(name=ApiConstants.VOLUME_ID, type=CommandType.LONG, required=true, description="The ID of the disk volume")
    private Long volumeId;

    /////////////////////////////////////////////////////
    /////////////////// Accessors ///////////////////////
    /////////////////////////////////////////////////////

    public String getAccountName() {
        return accountName;
    }

    public Long getDomainId() {
        return domainId;
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

    @Override @SuppressWarnings("unchecked")
    public SnapshotResponse getResponse() {
        SnapshotVO snapshot = (SnapshotVO)getResponseObject();

        if (snapshot != null) {
            SnapshotResponse response = ApiResponseHelper.createSnapshotResponse(snapshot);
            response.setResponseName(getName());
            return response;
        }

        throw new ServerApiException(BaseCmd.INTERNAL_ERROR, "Failed to create snapshot due to an internal error creating snapshot for volume " + volumeId);
    }
}
