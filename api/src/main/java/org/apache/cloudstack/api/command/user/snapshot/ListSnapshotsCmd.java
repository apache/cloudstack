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
package org.apache.cloudstack.api.command.user.snapshot;

import java.util.List;

import org.apache.cloudstack.api.APICommand;
import org.apache.cloudstack.api.ApiCommandResourceType;
import org.apache.cloudstack.api.ApiConstants;
import org.apache.cloudstack.api.BaseListTaggedResourcesCmd;
import org.apache.cloudstack.api.Parameter;
import org.apache.cloudstack.api.ResponseObject;
import org.apache.cloudstack.api.response.ListResponse;
import org.apache.cloudstack.api.response.SnapshotResponse;
import org.apache.cloudstack.api.response.VolumeResponse;
import org.apache.cloudstack.api.response.ZoneResponse;
import org.apache.log4j.Logger;

import com.cloud.storage.Snapshot;

@APICommand(name = "listSnapshots", description = "Lists all available snapshots for the account.", responseObject = SnapshotResponse.class, entityType = {
        Snapshot.class }, responseView = ResponseObject.ResponseView.Restricted, requestHasSensitiveInfo = false, responseHasSensitiveInfo = false)
public class ListSnapshotsCmd extends BaseListTaggedResourcesCmd {
    public static final Logger s_logger = Logger.getLogger(ListSnapshotsCmd.class.getName());


    /////////////////////////////////////////////////////
    //////////////// API parameters /////////////////////
    /////////////////////////////////////////////////////

    @Parameter(name = ApiConstants.ID, type = CommandType.UUID, entityType = SnapshotResponse.class, description = "lists snapshot by snapshot ID")
    private Long id;

    @Parameter(name=ApiConstants.IDS, type=CommandType.LIST, collectionType=CommandType.UUID, entityType=SnapshotResponse.class, description="the IDs of the snapshots, mutually exclusive with id", since = "4.9")
    private List<Long> ids;

    @Parameter(name = ApiConstants.INTERVAL_TYPE, type = CommandType.STRING, description = "valid values are HOURLY, DAILY, WEEKLY, and MONTHLY.")
    private String intervalType;

    @Parameter(name = ApiConstants.NAME, type = CommandType.STRING, description = "lists snapshot by snapshot name")
    private String snapshotName;

    @Parameter(name = ApiConstants.SNAPSHOT_TYPE, type = CommandType.STRING, description = "valid values are MANUAL or RECURRING.")
    private String snapshotType;

    @Parameter(name = ApiConstants.VOLUME_ID, type = CommandType.UUID, entityType = VolumeResponse.class, description = "the ID of the disk volume")
    private Long volumeId;

    @Parameter(name = ApiConstants.ZONE_ID, type = CommandType.UUID, entityType = ZoneResponse.class, description = "list snapshots by zone id")
    private Long zoneId;

    @Parameter(name = ApiConstants.SHOW_UNIQUE, type = CommandType.BOOLEAN, description = "If set to false, list templates across zones and their storages", since = "4.19.0")
    private Boolean showUnique;

    @Parameter(name = ApiConstants.LOCATION_TYPE, type = CommandType.STRING, description = "list snapshots by location type. Used only when showunique=false. " +
            "Valid location types: 'primary', 'secondary'. Default is empty", since = "4.19.0")
    private String locationType;

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

    public Long getZoneId() {
        return zoneId;
    }

    public boolean isShowUnique() {
        if (Boolean.FALSE.equals(showUnique)) {
            return false;
        }
        return true;
    }

    public String getLocationType() {
        if (!isShowUnique()) {
            return locationType;
        }
        return null;
    }

    public Long getImageStoreId() {
        return null;
    }

    public Long getStoragePoolId() {
        return null;
    }

    /////////////////////////////////////////////////////
    /////////////// API Implementation///////////////////
    /////////////////////////////////////////////////////

    @Override
    public ApiCommandResourceType getApiResourceType() {
        return ApiCommandResourceType.Snapshot;
    }

    @Override
    public void execute() {
        ListResponse<SnapshotResponse> response = _queryService.listSnapshots(this);
        response.setResponseName(getCommandName());

        setResponseObject(response);
    }

    public List<Long> getIds() {
        return ids;
    }
}
