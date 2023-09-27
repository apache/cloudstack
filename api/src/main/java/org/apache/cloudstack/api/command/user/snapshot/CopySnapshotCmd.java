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

import java.util.ArrayList;
import java.util.List;

import org.apache.cloudstack.acl.RoleType;
import org.apache.cloudstack.api.APICommand;
import org.apache.cloudstack.api.ApiCommandResourceType;
import org.apache.cloudstack.api.ApiConstants;
import org.apache.cloudstack.api.ApiErrorCode;
import org.apache.cloudstack.api.BaseAsyncCmd;
import org.apache.cloudstack.api.Parameter;
import org.apache.cloudstack.api.ResponseObject;
import org.apache.cloudstack.api.ServerApiException;
import org.apache.cloudstack.api.command.user.UserCmd;
import org.apache.cloudstack.api.response.SnapshotResponse;
import org.apache.cloudstack.api.response.ZoneResponse;
import org.apache.cloudstack.context.CallContext;
import org.apache.commons.collections.CollectionUtils;
import org.apache.log4j.Logger;

import com.cloud.dc.DataCenter;
import com.cloud.event.EventTypes;
import com.cloud.exception.ResourceAllocationException;
import com.cloud.exception.ResourceUnavailableException;
import com.cloud.exception.StorageUnavailableException;
import com.cloud.storage.Snapshot;
import com.cloud.user.Account;

@APICommand(name = "copySnapshot", description = "Copies a snapshot from one zone to another.",
        responseObject = SnapshotResponse.class, responseView = ResponseObject.ResponseView.Restricted,
        requestHasSensitiveInfo = false, responseHasSensitiveInfo = false, since = "4.19.0",
        authorized = {RoleType.Admin,  RoleType.ResourceAdmin, RoleType.DomainAdmin, RoleType.User})
public class CopySnapshotCmd extends BaseAsyncCmd implements UserCmd {
    public static final Logger s_logger = Logger.getLogger(CopySnapshotCmd.class.getName());

    /////////////////////////////////////////////////////
    //////////////// API parameters /////////////////////
    /////////////////////////////////////////////////////

    @Parameter(name = ApiConstants.ID, type = CommandType.UUID,
            entityType = SnapshotResponse.class, required = true, description = "the ID of the snapshot.")
    private Long id;

    @Parameter(name = ApiConstants.SOURCE_ZONE_ID,
            type = CommandType.UUID,
            entityType = ZoneResponse.class,
            description = "The ID of the zone in which the snapshot is currently present. " +
                    "If not specified then the zone of snapshot's volume will be used.")
    private Long sourceZoneId;

    @Parameter(name = ApiConstants.DESTINATION_ZONE_ID,
            type = CommandType.UUID,
            entityType = ZoneResponse.class,
            required = false,
            description = "The ID of the zone the snapshot is being copied to.")
    protected Long destZoneId;

    @Parameter(name = ApiConstants.DESTINATION_ZONE_ID_LIST,
            type=CommandType.LIST,
            collectionType = CommandType.UUID,
            entityType = ZoneResponse.class,
            required = false,
            description = "A comma-separated list of IDs of the zones that the snapshot needs to be copied to. " +
                    "Specify this list if the snapshot needs to copied to multiple zones in one go. " +
                    "Do not specify destzoneid and destzoneids together, however one of them is required.")
    protected List<Long> destZoneIds;

    /////////////////////////////////////////////////////
    /////////////////// Accessors ///////////////////////
    /////////////////////////////////////////////////////


    public Long getId() {
        return id;
    }

    public Long getSourceZoneId() {
        return sourceZoneId;
    }

    public List<Long> getDestinationZoneIds() {
        if (destZoneIds != null && destZoneIds.size() != 0) {
            return destZoneIds;
        }
        if (destZoneId != null) {
            List < Long > destIds = new ArrayList<>();
            destIds.add(destZoneId);
            return destIds;
        }
        return null;
    }

    @Override
    public String getEventType() {
        return EventTypes.EVENT_SNAPSHOT_COPY;
    }

    @Override
    public String getEventDescription() {
        StringBuilder descBuilder = new StringBuilder();
        if (getDestinationZoneIds() != null) {
            for (Long destId : getDestinationZoneIds()) {
                descBuilder.append(", ");
                descBuilder.append(_uuidMgr.getUuid(DataCenter.class, destId));
            }
            if (descBuilder.length() > 0) {
                descBuilder.deleteCharAt(0);
            }
        }

        return  "copying snapshot: " + _uuidMgr.getUuid(Snapshot.class, getId()) + ((descBuilder.length() > 0) ? " to zones: " + descBuilder.toString() : "");
    }

    @Override
    public ApiCommandResourceType getApiResourceType() {
        return ApiCommandResourceType.Snapshot;
    }

    @Override
    public Long getApiResourceId() {
        return getId();
    }

    @Override
    public long getEntityOwnerId() {
        Snapshot snapshot = _entityMgr.findById(Snapshot.class, getId());
        if (snapshot != null) {
            return snapshot.getAccountId();
        }
        return Account.ACCOUNT_ID_SYSTEM;
    }

    @Override
    public void execute() throws ResourceUnavailableException {
        try {
            if (destZoneId == null && CollectionUtils.isEmpty(destZoneIds))
                throw new ServerApiException(ApiErrorCode.PARAM_ERROR,
                        "Either destzoneid or destzoneids parameters have to be specified.");

            if (destZoneId != null && CollectionUtils.isNotEmpty(destZoneIds))
                throw new ServerApiException(ApiErrorCode.PARAM_ERROR,
                        "Both destzoneid and destzoneids cannot be specified at the same time.");

            CallContext.current().setEventDetails(getEventDescription());
            Snapshot snapshot = _snapshotService.copySnapshot(this);

            if (snapshot != null) {
                SnapshotResponse response = _queryService.listSnapshot(this);
                response.setResponseName(getCommandName());
                setResponseObject(response);
            } else {
                throw new ServerApiException(ApiErrorCode.INTERNAL_ERROR, "Failed to copy snapshot");
            }
        } catch (StorageUnavailableException ex) {
            s_logger.warn("Exception: ", ex);
            throw new ServerApiException(ApiErrorCode.RESOURCE_UNAVAILABLE_ERROR, ex.getMessage());
        } catch (ResourceAllocationException ex) {
            s_logger.warn("Exception: ", ex);
            throw new ServerApiException(ApiErrorCode.RESOURCE_ALLOCATION_ERROR, ex.getMessage());
        }

    }
}
