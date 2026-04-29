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
package org.apache.cloudstack.api.command.user.volume;

import java.util.Arrays;

import org.apache.cloudstack.acl.RoleType;
import org.apache.cloudstack.api.APICommand;
import org.apache.cloudstack.api.ApiCommandResourceType;
import org.apache.cloudstack.api.ApiConstants;
import org.apache.cloudstack.api.ApiErrorCode;
import org.apache.cloudstack.api.BaseAsyncCmd;
import org.apache.cloudstack.api.Parameter;
import org.apache.cloudstack.api.ResponseObject.ResponseView;
import org.apache.cloudstack.api.ServerApiException;
import org.apache.cloudstack.api.response.VolumeResponse;
import org.apache.cloudstack.context.CallContext;
import org.apache.commons.lang3.EnumUtils;

import com.cloud.event.EventTypes;
import com.cloud.exception.InvalidParameterValueException;
import com.cloud.exception.ResourceAllocationException;
import com.cloud.storage.Volume;
import com.cloud.user.Account;
import com.cloud.utils.Pair;
import com.cloud.utils.StringUtils;

@APICommand(name = "checkVolume", description = "Check the volume for any errors or leaks and also repairs when repair parameter is passed, this is currently supported for KVM only", responseObject = VolumeResponse.class, entityType = {Volume.class},
        since = "4.19.1",
        authorized = {RoleType.Admin, RoleType.ResourceAdmin, RoleType.DomainAdmin, RoleType.User})
public class CheckAndRepairVolumeCmd extends BaseAsyncCmd {

    private static final String s_name = "checkandrepairvolumeresponse";

    /////////////////////////////////////////////////////
    //////////////// API parameters /////////////////////
    /////////////////////////////////////////////////////

    @Parameter(name = ApiConstants.ID, type = CommandType.UUID, entityType = VolumeResponse.class, required = true, description = "The ID of the volume")
    private Long id;

    @Parameter(name = ApiConstants.REPAIR, type = CommandType.STRING, required = false, description = "parameter to repair the volume, leaks or all are the possible values")
    private String repair;

    /////////////////////////////////////////////////////
    /////////////////// Accessors ///////////////////////
    /////////////////////////////////////////////////////

    public enum RepairValues {
        LEAKS, ALL
    }

    public Long getId() {
        return id;
    }

    public String getRepair() {
        if (org.apache.commons.lang3.StringUtils.isNotEmpty(repair)) {
            RepairValues repairType = EnumUtils.getEnumIgnoreCase(RepairValues.class, repair);
            if (repairType == null) {
                throw new InvalidParameterValueException(String.format("Repair parameter can only take the following values: %s", Arrays.toString(RepairValues.values())));
            }
            return repair.toLowerCase();
        }
        return null;
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
        Volume volume = _entityMgr.findById(Volume.class, getId());
        if (volume != null) {
            return volume.getAccountId();
        }

        return Account.ACCOUNT_ID_SYSTEM; // no account info given, parent this command to SYSTEM so ERROR events are tracked
    }

    @Override
    public String getEventType() {
        return EventTypes.EVENT_VOLUME_CHECK;
    }

    @Override
    public String getEventDescription() {
        return String.format("check and repair operation on volume: %s", this._uuidMgr.getUuid(Volume.class, getId()));
    }

    @Override
    public Long getApiResourceId() {
        return id;
    }

    @Override
    public ApiCommandResourceType getApiResourceType() {
        return ApiCommandResourceType.Volume;
    }

    @Override
    public void execute() throws ResourceAllocationException {
        CallContext.current().setEventDetails("Volume Id: " + getId());
        Pair<String, String> result = _volumeService.checkAndRepairVolume(this);
        Volume volume = _responseGenerator.findVolumeById(getId());
        if (result != null) {
            VolumeResponse response = _responseGenerator.createVolumeResponse(ResponseView.Full, volume);
            response.setVolumeCheckResult(StringUtils.parseJsonToMap(result.first()));
            if (getRepair() != null) {
                response.setVolumeRepairResult(StringUtils.parseJsonToMap(result.second()));
            }
            response.setResponseName(getCommandName());
            setResponseObject(response);
        } else {
            throw new ServerApiException(ApiErrorCode.INTERNAL_ERROR, "Failed to check volume and repair");
        }
    }
}
