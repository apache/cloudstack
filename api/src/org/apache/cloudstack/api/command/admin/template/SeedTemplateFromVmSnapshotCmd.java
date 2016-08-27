/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package org.apache.cloudstack.api.command.admin.template;

import org.apache.log4j.Logger;

import org.apache.cloudstack.acl.SecurityChecker.AccessType;
import org.apache.cloudstack.api.ACL;
import org.apache.cloudstack.api.APICommand;
import org.apache.cloudstack.api.ApiCommandJobType;
import org.apache.cloudstack.api.ApiConstants;
import org.apache.cloudstack.api.ApiErrorCode;
import org.apache.cloudstack.api.BaseAsyncCmd;
import org.apache.cloudstack.api.Parameter;
import org.apache.cloudstack.api.ServerApiException;
import org.apache.cloudstack.api.response.StoragePoolResponse;
import org.apache.cloudstack.api.response.VMSnapshotResponse;
import org.apache.cloudstack.api.response.VmSnapshotTemplateResponse;
import org.apache.cloudstack.api.response.ZoneResponse;

import com.cloud.event.EventTypes;
import com.cloud.user.Account;

@APICommand(name = "seedTemplateFromVmSnapshot", responseObject = VmSnapshotTemplateResponse.class, description = "seed template from vm snapshot on primary storage",
        requestHasSensitiveInfo = false, responseHasSensitiveInfo = false)
public class SeedTemplateFromVmSnapshotCmd extends BaseAsyncCmd {
    public static final Logger s_logger = Logger.getLogger(SeedTemplateFromVmSnapshotCmd.class);

    private static final String s_name = "seed_template_from_vmsnapshot_response";

    /////////////////////////////////////////////////////
    //////////////// API parameters /////////////////////
    /////////////////////////////////////////////////////

    @Parameter(name = ApiConstants.ZONE_ID,
            type = CommandType.UUID,
            entityType = ZoneResponse.class,
            required = true,
            description = "ID of zone where seeding has to be done in primary storage(s).")
    private Long zoneId;

    @ACL(accessType = AccessType.OperateEntry)
    @Parameter(name = ApiConstants.VM_SNAPSHOT_ID,
            type = CommandType.UUID,
            entityType = VMSnapshotResponse.class,
            required = true,
            description = "VM Snapshot ID to be used to prepare (seed) the template in primary storage.")
    private Long vmSnapshotId;

    @ACL(accessType = AccessType.OperateEntry)
    @Parameter(name = ApiConstants.STORAGE_ID,
            type = CommandType.UUID,
            entityType = StoragePoolResponse.class,
            required = false,
            description = "storage pool ID of the primary storage pool where template should be seeded. Currently seeding is possible only to storage pool containing the vm snapshot.")
    private Long storageId;

    /////////////////////////////////////////////////////
    /////////////////// Accessors ///////////////////////
    /////////////////////////////////////////////////////

    public Long getZoneId() {
        return zoneId;
    }

    public Long getVmSnapshotId() {
        return vmSnapshotId;
    }

    public Long getStorageId() {
        return storageId;
    }

    /////////////////////////////////////////////////////
    /////////////// API Implementation///////////////////
    /////////////////////////////////////////////////////

    @Override
    public String getCommandName() {
        return s_name;
    }

    @Override
    public long getEntityOwnerId() {
        return Account.ACCOUNT_ID_SYSTEM;
    }

    @Override
    public ApiCommandJobType getInstanceType() {
        return ApiCommandJobType.VmSnapshot;
    }

    @Override
    public Long getInstanceId() {
        return getVmSnapshotId();
    }

    @Override
    public void execute() {
        VmSnapshotTemplateResponse response;
        try {
            _templateService.seedTemplateFromVmSnapshot(vmSnapshotId, zoneId, storageId);
            response = _responseGenerator.createVmSnapshotTemplateResponse(vmSnapshotId, storageId);
            if (response != null) {
                response.setResponseName(getCommandName());
                response.setObjectName("VmSnapshotTemplate");
            } else {
                throw new ServerApiException(ApiErrorCode.INTERNAL_ERROR, "Failed to seed template from specified vm snapshot.");
            }
            setResponseObject(response);
        } catch (IllegalArgumentException ex) {
            throw new IllegalArgumentException(ex.getMessage());
        } catch (Exception ex) {
            throw new ServerApiException(ApiErrorCode.INTERNAL_ERROR, ex.getMessage());
        }
    }

    @Override
    public String getEventType() {
        return EventTypes.EVENT_TEMPLATE_FROM_VM_SNAPSHOT_CREATE;
    }

    @Override
    public String getEventDescription() {
        return "Seeding template" + ((getVmSnapshotId() == null) ? "" : " from vm snapshot: " + getVmSnapshotId());
    }

}
