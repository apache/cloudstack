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
import com.cloud.api.ApiResponseHelper;
import com.cloud.api.BaseCmd;
import com.cloud.api.Implementation;
import com.cloud.api.Parameter;
import com.cloud.api.ServerApiException;
import com.cloud.api.response.SnapshotPolicyResponse;
import com.cloud.exception.ConcurrentOperationException;
import com.cloud.exception.InsufficientAddressCapacityException;
import com.cloud.exception.InsufficientCapacityException;
import com.cloud.exception.InvalidParameterValueException;
import com.cloud.exception.PermissionDeniedException;
import com.cloud.storage.SnapshotPolicyVO;

@Implementation(description="Creates a snapshot policy for the account.", responseObject=SnapshotPolicyResponse.class)
public class CreateSnapshotPolicyCmd extends BaseCmd {
    public static final Logger s_logger = Logger.getLogger(CreateSnapshotPolicyCmd.class.getName());

    private static final String s_name = "createsnapshotpolicyresponse";

    /////////////////////////////////////////////////////
    //////////////// API parameters /////////////////////
    /////////////////////////////////////////////////////

    @Parameter(name=ApiConstants.ACCOUNT, type=CommandType.STRING, description="The account of the snapshot policy. The account parameter must be used with the domainId parameter.")
    private String accountName;

    @Parameter(name=ApiConstants.DOMAIN_ID, type=CommandType.LONG, description="The domain ID of the snapshot. If used with the account parameter, specifies a domain for the account associated with the snapshot policy.")
    private Long domainId;

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

    @Parameter(name=ApiConstants.VOLUME_ID, type=CommandType.LONG, required=true, description="the ID of the disk volume")
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
    public void execute() throws ServerApiException, InvalidParameterValueException, PermissionDeniedException, InsufficientAddressCapacityException, InsufficientCapacityException, ConcurrentOperationException{
        SnapshotPolicyVO result = BaseCmd._snapshotMgr.createPolicy(this);
        SnapshotPolicyResponse response = ApiResponseHelper.createSnapshotPolicyResponse(result);
        response.setResponseName(getName());
        this.setResponseObject(response);
    }
}
