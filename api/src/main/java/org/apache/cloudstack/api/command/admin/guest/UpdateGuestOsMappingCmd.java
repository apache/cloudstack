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
package org.apache.cloudstack.api.command.admin.guest;

import org.apache.commons.lang3.BooleanUtils;

import org.apache.cloudstack.api.APICommand;
import org.apache.cloudstack.api.ApiCommandResourceType;
import org.apache.cloudstack.api.ApiConstants;
import org.apache.cloudstack.api.ApiErrorCode;
import org.apache.cloudstack.api.BaseAsyncCmd;
import org.apache.cloudstack.api.Parameter;
import org.apache.cloudstack.api.ServerApiException;
import org.apache.cloudstack.api.response.GuestOsMappingResponse;

import com.cloud.event.EventTypes;
import com.cloud.storage.GuestOSHypervisor;
import com.cloud.user.Account;

@APICommand(name = "updateGuestOsMapping", description = "Updates the information about Guest OS to Hypervisor specific name mapping", responseObject = GuestOsMappingResponse.class,
        since = "4.4.0", requestHasSensitiveInfo = false, responseHasSensitiveInfo = false)
public class UpdateGuestOsMappingCmd extends BaseAsyncCmd {


    /////////////////////////////////////////////////////
    //////////////// API parameters /////////////////////
    /////////////////////////////////////////////////////

    @Parameter(name = ApiConstants.ID, type = CommandType.UUID, entityType = GuestOsMappingResponse.class, required = true, description = "UUID of the Guest OS to hypervisor name Mapping")
    private Long id;

    @Parameter(name = ApiConstants.OS_NAME_FOR_HYPERVISOR, type = CommandType.STRING, required = true, description = "Hypervisor specific name for this Guest OS")
    private String osNameForHypervisor;

    @Parameter(name = ApiConstants.OS_MAPPING_CHECK_ENABLED, type = CommandType.BOOLEAN, required = false, description = "When set to true, checks for the correct guest os mapping name in the provided hypervisor (supports VMware and XenServer only. At least one hypervisor host with the version specified must be available. Default version will not work.)", since = "4.19.0")
    private Boolean osMappingCheckEnabled;

/////////////////////////////////////////////////////
    /////////////////// Accessors ///////////////////////
    /////////////////////////////////////////////////////

    public Long getId() {
        return id;
    }

    public String getOsNameForHypervisor() {
        return osNameForHypervisor;
    }

    public Boolean getOsMappingCheckEnabled() {
        return BooleanUtils.toBooleanDefaultIfNull(osMappingCheckEnabled, false);
    }

    @Override
    public void execute() {
        GuestOSHypervisor guestOsMapping = _mgr.updateGuestOsMapping(this);
        if (guestOsMapping != null) {
            GuestOsMappingResponse response = _responseGenerator.createGuestOSMappingResponse(guestOsMapping);
            response.setResponseName(getCommandName());
            setResponseObject(response);
        }
        else {
            throw new ServerApiException(ApiErrorCode.INTERNAL_ERROR, "Failed to update guest OS mapping");
        }

    }

    @Override
    public long getEntityOwnerId() {
        return Account.ACCOUNT_ID_SYSTEM;
    }

    @Override
    public String getEventDescription() {
        return "Updating Guest OS Mapping: " + getId();
    }

    @Override
    public String getEventType() {
        return EventTypes.EVENT_GUEST_OS_MAPPING_UPDATE;
    }

    @Override
    public ApiCommandResourceType getApiResourceType() {
        return ApiCommandResourceType.GuestOsMapping;
    }

}
