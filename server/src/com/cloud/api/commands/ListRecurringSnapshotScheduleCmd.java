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

import com.cloud.api.BaseCmd.Manager;
import com.cloud.api.BaseListCmd;
import com.cloud.api.Implementation;
import com.cloud.api.Parameter;
import com.cloud.api.ResponseObject;
import com.cloud.api.response.ListResponse;
import com.cloud.api.response.SnapshotScheduleResponse;
import com.cloud.storage.SnapshotScheduleVO;

@Implementation(method="findRecurringSnapshotSchedule", manager=Manager.SnapshotManager)
public class ListRecurringSnapshotScheduleCmd extends BaseListCmd {
    private static final String s_name = "listrecurringsnapshotscheduleresponse";

    /////////////////////////////////////////////////////
    //////////////// API parameters /////////////////////
    /////////////////////////////////////////////////////

    @Parameter(name="snapshotpolicyid", type=CommandType.LONG)
    private Long snapshotPolicyId;

    @Parameter(name="volumeid", type=CommandType.LONG, required=true)
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
    public String getName() {
        return s_name;
    }

    @Override @SuppressWarnings("unchecked")
    public ResponseObject getResponse() {
        List<SnapshotScheduleVO> snapshotSchedules = (List<SnapshotScheduleVO>)getResponseObject();

        ListResponse response = new ListResponse();
        List<SnapshotScheduleResponse> snapshotScheduleResponses = new ArrayList<SnapshotScheduleResponse>();
        for (SnapshotScheduleVO snapshotSchedule : snapshotSchedules) {
            SnapshotScheduleResponse snapSchedResponse = new SnapshotScheduleResponse();
            snapSchedResponse.setId(snapshotSchedule.getId());
            snapSchedResponse.setVolumeId(snapshotSchedule.getVolumeId());
            snapSchedResponse.setSnapshotPolicyId(snapshotSchedule.getPolicyId());
            snapSchedResponse.setScheduled(snapshotSchedule.getScheduledTimestamp());

            snapSchedResponse.setResponseName("snapshot");
            snapshotScheduleResponses.add(snapSchedResponse);
        }

        response.setResponses(snapshotScheduleResponses);
        response.setResponseName(getName());
        return response;
    }
}
