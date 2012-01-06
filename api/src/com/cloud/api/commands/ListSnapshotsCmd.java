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

import com.cloud.api.ApiConstants;
import com.cloud.api.BaseListProjectAndAccountResourcesCmd;
import com.cloud.api.IdentityMapper;
import com.cloud.api.Implementation;
import com.cloud.api.Parameter;
import com.cloud.api.response.ListResponse;
import com.cloud.api.response.SnapshotResponse;
import com.cloud.async.AsyncJob;
import com.cloud.storage.Snapshot;

@Implementation(description="Lists all available snapshots for the account.", responseObject=SnapshotResponse.class)
public class ListSnapshotsCmd extends BaseListProjectAndAccountResourcesCmd {
	public static final Logger s_logger = Logger.getLogger(ListSnapshotsCmd.class.getName());

    private static final String s_name = "listsnapshotsresponse";

    /////////////////////////////////////////////////////
    //////////////// API parameters /////////////////////
    /////////////////////////////////////////////////////

    @IdentityMapper(entityTableName="snapshots")
    @Parameter(name=ApiConstants.ID, type=CommandType.LONG, description="lists snapshot by snapshot ID")
    private Long id;

    @Parameter(name=ApiConstants.INTERVAL_TYPE, type=CommandType.STRING, description="valid values are HOURLY, DAILY, WEEKLY, and MONTHLY.")
    private String intervalType;

    @Parameter(name=ApiConstants.NAME, type=CommandType.STRING, description="lists snapshot by snapshot name")
    private String snapshotName;

    @Parameter(name=ApiConstants.SNAPSHOT_TYPE, type=CommandType.STRING, description="valid values are MANUAL or RECURRING.")
    private String snapshotType;

    @IdentityMapper(entityTableName="volumes")
    @Parameter(name=ApiConstants.VOLUME_ID, type=CommandType.LONG, description="the ID of the disk volume")
    private Long volumeId;

    /////////////////////////////////////////////////////
    /////////////////// Accessors ///////////////////////
    /////////////////////////////////////////////////////

    public Long getId() {
        return id;
    }

    public String getIntervalType() {
        return intervalType;
    }

    public String getSnapshotName() {
        return snapshotName;
    }

    public String getSnapshotType() {
        return snapshotType;
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
    
    public AsyncJob.Type getInstanceType() {
    	return AsyncJob.Type.Snapshot;
    }

    @Override
    public void execute(){
        List<? extends Snapshot> result = _snapshotService.listSnapshots(this);
        ListResponse<SnapshotResponse> response = new ListResponse<SnapshotResponse>();
        List<SnapshotResponse> snapshotResponses = new ArrayList<SnapshotResponse>();
        for (Snapshot snapshot : result) {
            SnapshotResponse snapshotResponse = _responseGenerator.createSnapshotResponse(snapshot);
            snapshotResponse.setObjectName("snapshot");
            snapshotResponses.add(snapshotResponse);
        }
        response.setResponses(snapshotResponses);
        response.setResponseName(getCommandName());
        
        this.setResponseObject(response);
    }
}
