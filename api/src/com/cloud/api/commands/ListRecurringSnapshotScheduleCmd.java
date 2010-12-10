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

import com.cloud.api.ApiConstants;
import com.cloud.api.BaseListCmd;
import com.cloud.api.Implementation;
import com.cloud.api.Parameter;
import com.cloud.api.response.ListResponse;
import com.cloud.api.response.SnapshotScheduleResponse;
import com.cloud.storage.snapshot.SnapshotSchedule;

@Implementation(description="Lists recurring snapshot schedule", responseObject=SnapshotScheduleResponse.class)
public class ListRecurringSnapshotScheduleCmd extends BaseListCmd {
    private static final String s_name = "listrecurringsnapshotscheduleresponse";

    /////////////////////////////////////////////////////
    //////////////// API parameters /////////////////////
    /////////////////////////////////////////////////////

    @Parameter(name=ApiConstants.SNAPSHOT_POLICY_ID, type=CommandType.LONG, description="lists recurring snapshots by snapshot policy ID")
    private Long snapshotPolicyId;

    @Parameter(name=ApiConstants.VOLUME_ID, type=CommandType.LONG, required=true, description="list recurring snapshots by volume ID")
    private Long volumeId;

    /////////////////////////////////////////////////////
    /////////////////// Accessors ///////////////////////
    /////////////////////////////////////////////////////

    public Long getSnapshotPolicyId() {
        return snapshotPolicyId;
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
    public void execute(){
        List<? extends SnapshotSchedule> snapshotSchedules = _snapshotMgr.findRecurringSnapshotSchedule(this);
        ListResponse<SnapshotScheduleResponse> response = new ListResponse<SnapshotScheduleResponse>();
        List<SnapshotScheduleResponse> snapshotScheduleResponses = new ArrayList<SnapshotScheduleResponse>();
        for (SnapshotSchedule snapshotSchedule : snapshotSchedules) {
            SnapshotScheduleResponse snapSchedResponse = new SnapshotScheduleResponse();
            snapSchedResponse.setId(snapshotSchedule.getId());
            snapSchedResponse.setVolumeId(snapshotSchedule.getVolumeId());
            snapSchedResponse.setSnapshotPolicyId(snapshotSchedule.getPolicyId());
            snapSchedResponse.setScheduled(snapshotSchedule.getScheduledTimestamp());

            snapSchedResponse.setObjectName("snapshot");
            snapshotScheduleResponses.add(snapSchedResponse);
        }

        response.setResponses(snapshotScheduleResponses);
        response.setResponseName(getCommandName());
        this.setResponseObject(response);
    }
}
