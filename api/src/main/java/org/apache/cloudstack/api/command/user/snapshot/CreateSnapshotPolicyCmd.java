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

import org.apache.cloudstack.acl.RoleType;
import org.apache.cloudstack.api.APICommand;
import org.apache.cloudstack.api.ApiCommandResourceType;
import org.apache.cloudstack.api.ApiConstants;
import org.apache.cloudstack.api.ApiErrorCode;
import org.apache.cloudstack.api.BaseCmd;
import org.apache.cloudstack.api.Parameter;
import org.apache.cloudstack.api.ServerApiException;
import org.apache.cloudstack.api.response.SnapshotPolicyResponse;
import org.apache.cloudstack.api.response.VolumeResponse;
import org.apache.commons.collections.MapUtils;

import com.cloud.exception.InvalidParameterValueException;
import com.cloud.exception.PermissionDeniedException;
import com.cloud.projects.Project;
import com.cloud.storage.Volume;
import com.cloud.storage.snapshot.SnapshotPolicy;
import com.cloud.user.Account;

@APICommand(name = "createSnapshotPolicy", description = "Creates a snapshot policy for the account.", responseObject = SnapshotPolicyResponse.class,
        requestHasSensitiveInfo = false, responseHasSensitiveInfo = false)
public class CreateSnapshotPolicyCmd extends BaseCmd {


    /////////////////////////////////////////////////////
    //////////////// API parameters /////////////////////
    /////////////////////////////////////////////////////

    @Parameter(name = ApiConstants.INTERVAL_TYPE, type = CommandType.STRING, required = true, description = "valid values are HOURLY, DAILY, WEEKLY, and MONTHLY")
    private String intervalType;

    @Parameter(name = ApiConstants.MAX_SNAPS, type = CommandType.INTEGER, required = true, description = "maximum number of snapshots to retain")
    private Integer maxSnaps;

    @Parameter(name = ApiConstants.SCHEDULE, type = CommandType.STRING, required = true, description = "time the snapshot is scheduled to be taken. " + "Format is:"
        + "* if HOURLY, MM" + "* if DAILY, MM:HH" + "* if WEEKLY, MM:HH:DD (1-7)" + "* if MONTHLY, MM:HH:DD (1-28)")
    private String schedule;

    @Parameter(name = ApiConstants.TIMEZONE,
               type = CommandType.STRING,
               required = true,
               description = "Specifies a timezone for this command. For more information on the timezone parameter, see Time Zone Format.")
    private String timezone;

    @Parameter(name = ApiConstants.VOLUME_ID, type = CommandType.UUID, entityType = VolumeResponse.class, required = true, description = "the ID of the disk volume")
    private Long volumeId;

    @Parameter(name = ApiConstants.FOR_DISPLAY, type = CommandType.BOOLEAN, description = "an optional field, whether to the display the policy to the end user or not", since = "4.4", authorized = {RoleType.Admin})
    private Boolean display;

    @Parameter(name = ApiConstants.TAGS, type = CommandType.MAP, description = "Map of tags (key/value pairs)")
    private Map tags;

    /////////////////////////////////////////////////////
    /////////////////// Accessors ///////////////////////
    /////////////////////////////////////////////////////

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

    @Override
    public boolean isDisplay() {
        if(display == null)
            return true;
        else
            return display;
    }

    /////////////////////////////////////////////////////
    /////////////// API Implementation///////////////////
    /////////////////////////////////////////////////////

    @Override
    public long getEntityOwnerId() {
        Volume volume = _entityMgr.findById(Volume.class, getVolumeId());
        if (volume == null) {
            throw new InvalidParameterValueException("Unable to find volume by id=" + volumeId);
        }

        Account account = _accountService.getAccount(volume.getAccountId());
        //Can create templates for enabled projects/accounts only
        if (account.getType() == Account.Type.PROJECT) {
            Project project = _projectService.findByProjectAccountId(volume.getAccountId());
            if (project.getState() != Project.State.Active) {
                PermissionDeniedException ex =
                    new PermissionDeniedException("Can't add resources to the specified project id in state=" + project.getState() + " as it's no longer active");
                ex.addProxyObject(project.getUuid(), "projectId");
                throw ex;
            }
        } else if (account.getState() == Account.State.DISABLED) {
            throw new PermissionDeniedException("The owner of template is disabled: " + account);
        }

        return volume.getAccountId();
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

    @Override
    public void execute() {
        SnapshotPolicy result = _snapshotService.createPolicy(this, _accountService.getAccount(getEntityOwnerId()));
        if (result != null) {
            SnapshotPolicyResponse response = _responseGenerator.createSnapshotPolicyResponse(result);
            response.setResponseName(getCommandName());
            this.setResponseObject(response);
        } else {
            throw new ServerApiException(ApiErrorCode.INTERNAL_ERROR, "Failed to create snapshot policy");
        }
    }

    @Override
    public Long getApiResourceId() {
        return getVolumeId();
    }

    @Override
    public ApiCommandResourceType getApiResourceType() {
        return ApiCommandResourceType.Volume;
    }
}
