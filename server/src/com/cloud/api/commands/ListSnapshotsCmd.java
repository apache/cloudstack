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

import com.cloud.api.ApiDBUtils;
import com.cloud.api.BaseListCmd;
import com.cloud.api.Implementation;
import com.cloud.api.Parameter;
import com.cloud.api.response.ListResponse;
import com.cloud.api.response.SnapshotResponse;
import com.cloud.async.AsyncJobVO;
import com.cloud.storage.Snapshot;
import com.cloud.storage.Snapshot.SnapshotType;
import com.cloud.storage.SnapshotVO;
import com.cloud.storage.VolumeVO;
import com.cloud.user.Account;

@Implementation(method="listSnapshots", description="Lists all available snapshots for the account.")
public class ListSnapshotsCmd extends BaseListCmd {
	public static final Logger s_logger = Logger.getLogger(ListSnapshotsCmd.class.getName());

    private static final String s_name = "listsnapshotsresponse";

    /////////////////////////////////////////////////////
    //////////////// API parameters /////////////////////
    /////////////////////////////////////////////////////

    @Parameter(name="account", type=CommandType.STRING, description="lists snapshot belongig to the specified account. Must be used with the domainId parameter.")
    private String accountName;

    @Parameter(name="domainid", type=CommandType.LONG, description="the domain ID. If used with the account parameter, lists snapshots for the specified account in this domain.")
    private Long domainId;

    @Parameter(name="id", type=CommandType.LONG, description="lists snapshot by snapshot ID")
    private Long id;

    @Parameter(name="intervalType", type=CommandType.STRING, description="valid values are HOURLY, DAILY, WEEKLY, and MONTHLY.")
    private String intervalType;

    @Parameter(name="name", type=CommandType.STRING, description="lists snapshot by snapshot name")
    private String snapshotName;

    @Parameter(name="snapshottype", type=CommandType.STRING, description="valid values are MANUAL or RECURRING.")
    private String snapshotType;

    @Parameter(name="volumeid", type=CommandType.LONG, description="the ID of the disk volume")
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
    public String getName() {
        return s_name;
    }

    @Override @SuppressWarnings("unchecked")
    public ListResponse<SnapshotResponse> getResponse() {
        List<SnapshotVO> snapshots = (List<SnapshotVO>)getResponseObject();

        ListResponse<SnapshotResponse> response = new ListResponse<SnapshotResponse>();
        List<SnapshotResponse> snapshotResponses = new ArrayList<SnapshotResponse>();
        for (Snapshot snapshot : snapshots) {
            SnapshotResponse snapshotResponse = new SnapshotResponse();
            snapshotResponse.setId(snapshot.getId());

            Account acct = ApiDBUtils.findAccountById(Long.valueOf(snapshot.getAccountId()));
            if (acct != null) {
                snapshotResponse.setAccountName(acct.getAccountName());
                snapshotResponse.setDomainId(acct.getDomainId());
                snapshotResponse.setDomainName(ApiDBUtils.findDomainById(acct.getDomainId()).getName());
            }

            VolumeVO volume = ApiDBUtils.findVolumeById(snapshot.getVolumeId());
            String snapshotTypeStr = SnapshotType.values()[snapshot.getSnapshotType()].name();
            snapshotResponse.setSnapshotType(snapshotTypeStr);
            snapshotResponse.setVolumeId(snapshot.getVolumeId());
            snapshotResponse.setVolumeName(volume.getName());
            snapshotResponse.setVolumeType(volume.getVolumeType().name());
            snapshotResponse.setCreated(snapshot.getCreated());
            snapshotResponse.setName(snapshot.getName());

            AsyncJobVO asyncJob = ApiDBUtils.findInstancePendingAsyncJob("snapshot", snapshot.getId());
            if (asyncJob != null) {
                snapshotResponse.setJobId(asyncJob.getId());
                snapshotResponse.setJobStatus(asyncJob.getStatus());
            }
            snapshotResponse.setIntervalType(ApiDBUtils.getSnapshotIntervalTypes(snapshot.getId()));

            snapshotResponse.setResponseName("snapshot");
            snapshotResponses.add(snapshotResponse);
        }

        response.setResponses(snapshotResponses);
        response.setResponseName(getName());
        return response;
    }
}
