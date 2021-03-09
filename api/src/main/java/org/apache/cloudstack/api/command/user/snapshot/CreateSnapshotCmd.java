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

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import org.apache.cloudstack.api.APICommand;
import org.apache.cloudstack.api.ApiCommandJobType;
import org.apache.cloudstack.api.ApiConstants;
import org.apache.cloudstack.api.ApiErrorCode;
import org.apache.cloudstack.api.BaseAsyncCmd;
import org.apache.cloudstack.api.BaseAsyncCreateCmd;
import org.apache.cloudstack.api.Parameter;
import org.apache.cloudstack.api.ServerApiException;
import org.apache.cloudstack.api.response.DomainResponse;
import org.apache.cloudstack.api.response.SnapshotPolicyResponse;
import org.apache.cloudstack.api.response.SnapshotResponse;
import org.apache.cloudstack.api.response.VolumeResponse;
import org.apache.commons.collections.MapUtils;
import org.apache.log4j.Logger;

import com.cloud.event.EventTypes;
import com.cloud.exception.InvalidParameterValueException;
import com.cloud.exception.PermissionDeniedException;
import com.cloud.exception.ResourceAllocationException;
import com.cloud.projects.Project;
import com.cloud.storage.Snapshot;
import com.cloud.storage.Volume;
import com.cloud.user.Account;
import com.cloud.utils.exception.CloudRuntimeException;

@APICommand(name = "createSnapshot", description = "Creates an instant snapshot of a volume.", responseObject = SnapshotResponse.class, entityType = {Snapshot.class},
        requestHasSensitiveInfo = false, responseHasSensitiveInfo = false)
public class CreateSnapshotCmd extends BaseAsyncCreateCmd {
    public static final Logger s_logger = Logger.getLogger(CreateSnapshotCmd.class.getName());
    private static final String s_name = "createsnapshotresponse";

    // ///////////////////////////////////////////////////
    // ////////////// API parameters /////////////////////
    // ///////////////////////////////////////////////////

    @Parameter(name = ApiConstants.ACCOUNT,
               type = CommandType.STRING,
               description = "The account of the snapshot. The account parameter must be used with the domainId parameter.")
    private String accountName;

    @Parameter(name = ApiConstants.DOMAIN_ID,
               type = CommandType.UUID,
               entityType = DomainResponse.class,
            description = "The domain ID of the snapshot. If used with the account parameter, specifies a domain for the account associated with the disk volume.")
    private Long domainId;

    @Parameter(name = ApiConstants.VOLUME_ID, type = CommandType.UUID, entityType = VolumeResponse.class, required = true, description = "The ID of the disk volume")
    private Long volumeId;

    @Parameter(name = ApiConstants.POLICY_ID,
               type = CommandType.UUID,
               entityType = SnapshotPolicyResponse.class,
            description = "policy id of the snapshot, if this is null, then use MANUAL_POLICY.")
    private Long policyId;

    @Parameter(name = ApiConstants.SNAPSHOT_QUIESCEVM, type = CommandType.BOOLEAN, required = false, description = "quiesce vm if true")
    private Boolean quiescevm;

    @Parameter(name = ApiConstants.LOCATION_TYPE, type = CommandType.STRING, required = false, description = "Currently applicable only for managed storage. " +
            "Valid location types: 'primary', 'secondary'. Default = 'primary'.")
    private String locationType;

    @Parameter(name = ApiConstants.NAME, type = CommandType.STRING, description = "the name of the snapshot")
    private String snapshotName;

    @Parameter(name = ApiConstants.ASYNC_BACKUP, type = CommandType.BOOLEAN, required = false, description = "asynchronous backup if true")
    private Boolean asyncBackup;

    @Parameter(name = ApiConstants.TAGS, type = CommandType.MAP, description = "Map of tags (key/value pairs)")
    private Map tags;

    private String syncObjectType = BaseAsyncCmd.snapshotHostSyncObject;

    // ///////////////////////////////////////////////////
    // ///////////////// Accessors ///////////////////////
    // ///////////////////////////////////////////////////

    public Boolean getQuiescevm() {
        if (quiescevm == null) {
            return false;
        } else {
            return quiescevm;
        }
    }

    public String getAccountName() {
        return accountName;
    }

    public Long getDomainId() {
        return domainId;
    }

    public Long getVolumeId() {
        return volumeId;
    }

    public String getSnapshotName() {
        return snapshotName;
    }

    public Long getPolicyId() {
        if (policyId != null) {
            return policyId;
        } else {
            return Snapshot.MANUAL_POLICY_ID;
        }
    }

    public Map<String, String> getTags() {
        Map<String, String> tagsMap = new HashMap<>();
        if (MapUtils.isNotEmpty(tags)) {
            for (Map<String, String> services : (Collection<Map<String, String>>)tags.values()) {
                String key = services.get("key");
                String value = services.get("value");
                tagsMap.put(key, value);
            }
        }
        return tagsMap;
    }

    private Long getHostId() {
        Volume volume = _entityMgr.findById(Volume.class, getVolumeId());
        if (volume == null) {
            throw new InvalidParameterValueException("Unable to find volume by id");
        }
        return _snapshotService.getHostIdForSnapshotOperation(volume);
    }

    // ///////////////////////////////////////////////////
    // ///////////// API Implementation///////////////////
    // ///////////////////////////////////////////////////

    @Override
    public String getCommandName() {
        return s_name;
    }

    public static String getResultObjectName() {
        return ApiConstants.SNAPSHOT;
    }

    @Override
    public long getEntityOwnerId() {

        Volume volume = _entityMgr.findById(Volume.class, getVolumeId());
        if (volume == null) {
            throw new InvalidParameterValueException("Unable to find volume by id=" + volumeId);
        }

        Account account = _accountService.getAccount(volume.getAccountId());
        //Can create templates for enabled projects/accounts only
        if (account.getType() == Account.ACCOUNT_TYPE_PROJECT) {
            Project project = _projectService.findByProjectAccountId(volume.getAccountId());
            if (project.getState() != Project.State.Active) {
                throw new PermissionDeniedException("Can't add resources to the project id=" + project.getId() + " in state=" + project.getState() +
                    " as it's no longer active");
            }
        } else if (account.getState() == Account.State.disabled) {
            throw new PermissionDeniedException("The owner of template is disabled: " + account);
        }

        return volume.getAccountId();
    }

    @Override
    public String getEventType() {
        return EventTypes.EVENT_SNAPSHOT_CREATE;
    }

    @Override
    public String getEventDescription() {
        return "creating snapshot for volume: " + getVolumeUuid();
    }

    @Override
    public ApiCommandJobType getInstanceType() {
        return ApiCommandJobType.Snapshot;
    }

    @Override
    public void create() throws ResourceAllocationException {
        Snapshot snapshot = _volumeService.allocSnapshot(getVolumeId(), getPolicyId(), getSnapshotName(), getLocationType());
        if (snapshot != null) {
            setEntityId(snapshot.getId());
            setEntityUuid(snapshot.getUuid());
        } else {
            throw new ServerApiException(ApiErrorCode.INTERNAL_ERROR, "Failed to create snapshot for volume" + getVolumeUuid());
        }
    }

    @Override
    public void execute() {
        Snapshot snapshot;
        try {
            snapshot =
                _volumeService.takeSnapshot(getVolumeId(), getPolicyId(), getEntityId(), _accountService.getAccount(getEntityOwnerId()), getQuiescevm(), getLocationType(), getAsyncBackup(), getTags());

            if (snapshot != null) {
                SnapshotResponse response = _responseGenerator.createSnapshotResponse(snapshot);
                response.setResponseName(getCommandName());
                setResponseObject(response);
            } else {
                throw new ServerApiException(ApiErrorCode.INTERNAL_ERROR, String.format("Snapshot from volume [%s] was not found in database.", getVolumeUuid()));
            }
        } catch (Exception e) {
            String errorMessage = "Failed to create snapshot due to an internal error creating snapshot for volume " + getVolumeUuid();
            s_logger.error(errorMessage, e);
            throw new ServerApiException(ApiErrorCode.INTERNAL_ERROR, errorMessage);
        }
    }

    private Snapshot.LocationType getLocationType() {

        if (Snapshot.LocationType.values() == null || Snapshot.LocationType.values().length == 0 || locationType == null) {
            return null;
        }

        try {
            String lType = locationType.trim().toUpperCase();
            return Snapshot.LocationType.valueOf(lType);
        } catch (IllegalArgumentException e) {
            String errMesg = "Invalid locationType " + locationType + "Specified for volume " + getVolumeId()
                        + " Valid values are: primary,secondary ";
            s_logger.warn(errMesg);
            throw  new CloudRuntimeException(errMesg);
        }
    }

    @Override
    public String getSyncObjType() {
        if (getSyncObjId() != null) {
            return syncObjectType;
        }
        return null;
    }

    @Override
    public Long getSyncObjId() {
        if (getHostId() != null) {
            return getHostId();
        }
        return null;
    }

    public Boolean getAsyncBackup() {
        if (asyncBackup == null) {
            return false;
        } else {
            return asyncBackup;
        }
    }

    protected String getVolumeUuid() {
        return _uuidMgr.getUuid(Volume.class, getVolumeId());
    }
}
