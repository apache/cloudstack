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

import org.apache.cloudstack.api.ApiConstants;
import org.apache.cloudstack.api.BaseListCmd;
import org.apache.cloudstack.api.Parameter;
import org.apache.cloudstack.api.response.ListResponse;
import org.apache.cloudstack.api.response.SnapshotScheduleResponse;

import com.cloud.storage.snapshot.SnapshotSchedule;

//@APICommand(description="Lists recurring snapshot schedule", responseObject=SnapshotScheduleResponse.class)
public class ListRecurringSnapshotScheduleCmd extends BaseListCmd {
    private static final String s_name = "listrecurringsnapshotscheduleresponse";

    /////////////////////////////////////////////////////
    //////////////// API parameters /////////////////////
    /////////////////////////////////////////////////////

    @Parameter(name = ApiConstants.SNAPSHOT_POLICY_ID, type = CommandType.LONG, description = "lists recurring snapshots by snapshot policy ID")
    private Long snapshotPolicyId;

    @Parameter(name = ApiConstants.VOLUME_ID, type = CommandType.LONG, required = true, description = "list recurring snapshots by volume ID")
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
    public void execute() {
        List<? extends SnapshotSchedule> snapshotSchedules = _snapshotService.findRecurringSnapshotSchedule(this);
        ListResponse<SnapshotScheduleResponse> response = new ListResponse<SnapshotScheduleResponse>();
        List<SnapshotScheduleResponse> snapshotScheduleResponses = new ArrayList<SnapshotScheduleResponse>();
        for (SnapshotSchedule snapshotSchedule : snapshotSchedules) {
            SnapshotScheduleResponse snapSchedResponse = _responseGenerator.createSnapshotScheduleResponse(snapshotSchedule);
            snapshotScheduleResponses.add(snapSchedResponse);
        }

        response.setResponses(snapshotScheduleResponses);
        response.setResponseName(getCommandName());
        this.setResponseObject(response);
    }
}
