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

import com.cloud.api.BaseCmd;
import com.cloud.api.BaseCmd.Manager;
import com.cloud.api.Implementation;
import com.cloud.api.Parameter;
import com.cloud.api.response.ApiResponseSerializer;
import com.cloud.api.response.SnapshotPolicyResponse;
import com.cloud.storage.SnapshotPolicyVO;

@Implementation(method="createPolicy", manager=Manager.SnapshotManager)
public class CreateSnapshotPolicyCmd extends BaseCmd {
    public static final Logger s_logger = Logger.getLogger(CreateSnapshotPolicyCmd.class.getName());

    private static final String s_name = "createsnapshotpolicyresponse";

    /////////////////////////////////////////////////////
    //////////////// API parameters /////////////////////
    /////////////////////////////////////////////////////

    @Parameter(name="account", type=CommandType.STRING)
    private String accountName;

    @Parameter(name="domainid", type=CommandType.LONG)
    private Long domainId;

    @Parameter(name="intervaltype", type=CommandType.STRING, required=true)
    private String intervalType;

    @Parameter(name="maxsnaps", type=CommandType.INTEGER, required=true)
    private Integer maxSnaps;

    @Parameter(name="schedule", type=CommandType.STRING, required=true)
    private String schedule;

    @Parameter(name="timezone", type=CommandType.STRING, required=true)
    private String timezone;

    @Parameter(name="volumeid", type=CommandType.LONG, required=true)
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
    public String getName() {
        return s_name;
    }

    @Override
    public String getResponse() {
        SnapshotPolicyVO snapshotPolicy = (SnapshotPolicyVO)getResponseObject();

        SnapshotPolicyResponse response = new SnapshotPolicyResponse();
        response.setId(snapshotPolicy.getId());
        response.setIntervalType(snapshotPolicy.getInterval());
        response.setMaxSnaps(snapshotPolicy.getMaxSnaps());
        response.setSchedule(snapshotPolicy.getSchedule());
        response.setVolumeId(snapshotPolicy.getVolumeId());

        response.setResponseName(getName());
        return ApiResponseSerializer.toSerializedString(response);
    }
}
