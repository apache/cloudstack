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
package org.apache.cloudstack.api.command.admin.backup;

import javax.inject.Inject;

import org.apache.cloudstack.acl.RoleType;
import org.apache.cloudstack.api.APICommand;
import org.apache.cloudstack.api.ApiConstants;
import org.apache.cloudstack.api.ApiErrorCode;
import org.apache.cloudstack.api.BaseAsyncCmd;
import org.apache.cloudstack.api.BaseCmd;
import org.apache.cloudstack.api.Parameter;
import org.apache.cloudstack.api.ServerApiException;
import org.apache.cloudstack.api.response.BackupOfferingResponse;
import org.apache.cloudstack.api.response.ZoneResponse;
import org.apache.cloudstack.backup.BackupManager;
import org.apache.cloudstack.backup.BackupOffering;
import org.apache.cloudstack.context.CallContext;

import com.cloud.event.EventTypes;
import com.cloud.exception.ConcurrentOperationException;
import com.cloud.exception.InsufficientCapacityException;
import com.cloud.exception.InvalidParameterValueException;
import com.cloud.exception.NetworkRuleConflictException;
import com.cloud.exception.ResourceAllocationException;
import com.cloud.exception.ResourceUnavailableException;
import com.cloud.utils.exception.CloudRuntimeException;

@APICommand(name = "cloneBackupOffering",
        description = "Clones a backup offering from an existing offering",
        responseObject = BackupOfferingResponse.class, since = "4.14.0",
        authorized = {RoleType.Admin})
public class CloneBackupOfferingCmd extends BaseAsyncCmd {

    @Inject
    protected BackupManager backupManager;

    /////////////////////////////////////////////////////
    //////////////// API parameters /////////////////////
    ////////////////////////////////////////////////////

    @Parameter(name = ApiConstants.SOURCE_OFFERING_ID, type = BaseCmd.CommandType.UUID,
            required = true, description = "The ID of the source backup offering to clone from")
    private Long sourceOfferingId;

    @Parameter(name = ApiConstants.NAME, type = BaseCmd.CommandType.STRING, required = false,
            description = "The name of the cloned offering")
    private String name;

    @Parameter(name = ApiConstants.DESCRIPTION, type = BaseCmd.CommandType.STRING, required = false,
            description = "The description of the cloned offering")
    private String description;

    @Parameter(name = ApiConstants.EXTERNAL_ID, type = BaseCmd.CommandType.STRING, required = false,
            description = "The backup offering ID (from backup provider side)")
    private String externalId;

    @Parameter(name = ApiConstants.ZONE_ID, type = BaseCmd.CommandType.UUID, entityType = ZoneResponse.class,
            description = "The zone ID", required = false)
    private Long zoneId;

    @Parameter(name = ApiConstants.ALLOW_USER_DRIVEN_BACKUPS, type = BaseCmd.CommandType.BOOLEAN,
            description = "Whether users are allowed to create adhoc backups and backup schedules", required = false)
    private Boolean userDrivenBackups;

    /////////////////////////////////////////////////////
    /////////////////// Accessors ///////////////////////
    /////////////////////////////////////////////////////

    public Long getSourceOfferingId() {
        return sourceOfferingId;
    }

    public String getName() {
        return name;
    }

    public String getExternalId() {
        return externalId;
    }

    public Long getZoneId() {
        return zoneId;
    }

    public String getDescription() {
        return description;
    }

    public Boolean getUserDrivenBackups() {
        return userDrivenBackups;
    }

    /////////////////////////////////////////////////////
    /////////////// API Implementation///////////////////
    /////////////////////////////////////////////////////

    @Override
    public void execute() throws ResourceUnavailableException, InsufficientCapacityException, ServerApiException, ConcurrentOperationException, ResourceAllocationException, NetworkRuleConflictException {
        try {
            BackupOffering policy = backupManager.cloneBackupOffering(this);
            if (policy == null) {
                throw new ServerApiException(ApiErrorCode.INTERNAL_ERROR, "Failed to clone backup offering");
            }
            BackupOfferingResponse response = _responseGenerator.createBackupOfferingResponse(policy);
            response.setResponseName(getCommandName());
            setResponseObject(response);
        } catch (InvalidParameterValueException e) {
            throw new ServerApiException(ApiErrorCode.PARAM_ERROR, e.getMessage());
        } catch (CloudRuntimeException e) {
            throw new ServerApiException(ApiErrorCode.INTERNAL_ERROR, e.getMessage());
        }
    }

    @Override
    public long getEntityOwnerId() {
        return CallContext.current().getCallingAccount().getId();
    }

    @Override
    public String getEventType() {
        return EventTypes.EVENT_VM_BACKUP_CLONE_OFFERING;
    }

    @Override
    public String getEventDescription() {
        return "Cloning backup offering: " + name + " from source offering: " + (sourceOfferingId == null ? "" : sourceOfferingId.toString());
    }
}
