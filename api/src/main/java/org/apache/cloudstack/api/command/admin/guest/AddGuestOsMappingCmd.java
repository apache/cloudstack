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
import org.apache.cloudstack.api.BaseAsyncCreateCmd;
import org.apache.cloudstack.api.Parameter;
import org.apache.cloudstack.api.ServerApiException;
import org.apache.cloudstack.api.response.GuestOSResponse;
import org.apache.cloudstack.api.response.GuestOsMappingResponse;

import com.cloud.event.EventTypes;
import com.cloud.storage.GuestOSHypervisor;
import com.cloud.user.Account;

@APICommand(name = "addGuestOsMapping", description = "Adds a guest OS name to hypervisor OS name mapping", responseObject = GuestOsMappingResponse.class,
        since = "4.4.0", requestHasSensitiveInfo = false, responseHasSensitiveInfo = false)
public class AddGuestOsMappingCmd extends BaseAsyncCreateCmd {


    /////////////////////////////////////////////////////
    //////////////// API parameters /////////////////////
    /////////////////////////////////////////////////////

    @Parameter(name = ApiConstants.OS_TYPE_ID, type = CommandType.UUID, required = false, entityType = GuestOSResponse.class, description = "UUID of Guest OS type. Either the UUID or Display Name must be passed")
    private Long osTypeId;

    @Parameter(name = ApiConstants.OS_DISPLAY_NAME, type = CommandType.STRING, required = false, description = "Display Name of Guest OS standard type. Either Display Name or UUID must be passed")
    private String osStdName;

    @Parameter(name = ApiConstants.HYPERVISOR, type = CommandType.STRING, required = true, description = "Hypervisor type. One of : XenServer, KVM, VMWare")
    private String hypervisor;

    @Parameter(name = ApiConstants.HYPERVISOR_VERSION, type = CommandType.STRING, required = true, description = "Hypervisor version to create the mapping. Use 'default' for default versions. Please check hypervisor capabilities for correct version")
    private String hypervisorVersion;

    @Parameter(name = ApiConstants.OS_NAME_FOR_HYPERVISOR, type = CommandType.STRING, required = true, description = "OS name specific to the hypervisor")
    private String osNameForHypervisor;

    @Parameter(name = ApiConstants.OS_MAPPING_CHECK_ENABLED, type = CommandType.BOOLEAN, required = false, description = "When set to true, checks for the correct guest os mapping name in the provided hypervisor (supports VMware and XenServer only. At least one hypervisor host with the version specified must be available. Default version will not work.)", since = "4.19.0")
    private Boolean osMappingCheckEnabled;

    @Parameter(name = ApiConstants.FORCED, type = CommandType.BOOLEAN, required = false, description = "Forces add user defined guest os mapping, overrides any existing user defined mapping", since = "4.19.0")
    private Boolean forced;

    /////////////////////////////////////////////////////
    /////////////////// Accessors ///////////////////////
    /////////////////////////////////////////////////////

    public Long getOsTypeId() {
        return osTypeId;
    }

    public String getOsStdName() {
        return osStdName;
    }

    public String getHypervisor() {
        return hypervisor;
    }

    public String getHypervisorVersion() {
        return hypervisorVersion;
    }

    public String getOsNameForHypervisor() {
        return osNameForHypervisor;
    }

    public Boolean getOsMappingCheckEnabled() {
        return BooleanUtils.toBooleanDefaultIfNull(osMappingCheckEnabled, false);
    }

    public boolean isForced() {
        return BooleanUtils.toBooleanDefaultIfNull(forced, false);
    }

    /////////////////////////////////////////////////////
    /////////////// API Implementation///////////////////
    /////////////////////////////////////////////////////

    @Override
    public long getEntityOwnerId() {
        return Account.ACCOUNT_ID_SYSTEM;
    }

    @Override
    public void create() {
        GuestOSHypervisor guestOsMapping = _mgr.addGuestOsMapping(this);
        if (guestOsMapping != null) {
            setEntityId(guestOsMapping.getId());
            setEntityUuid(guestOsMapping.getUuid());
        } else {
            throw new ServerApiException(ApiErrorCode.INTERNAL_ERROR, "Failed to add guest OS mapping entity");
        }
    }

    @Override
    public void execute() {
        GuestOSHypervisor guestOsMapping = _mgr.getAddedGuestOsMapping(getEntityId());
        if (guestOsMapping != null) {
            GuestOsMappingResponse response = _responseGenerator.createGuestOSMappingResponse(guestOsMapping);
            response.setResponseName(getCommandName());
            setResponseObject(response);
        } else {
            throw new ServerApiException(ApiErrorCode.INTERNAL_ERROR, "Failed to add guest OS mapping");
        }
    }

    @Override
    public String getEventType() {
        return EventTypes.EVENT_GUEST_OS_MAPPING_ADD;
    }

    @Override
    public String getEventDescription() {
        return "adding a new guest OS mapping Id: " + getEntityId();
    }

    @Override
    public ApiCommandResourceType getApiResourceType() {
        return ApiCommandResourceType.GuestOsMapping;
    }

    @Override
    public String getCreateEventType() {
        return EventTypes.EVENT_GUEST_OS_MAPPING_ADD;
    }

    @Override
    public String getCreateEventDescription() {
        return "adding new guest OS mapping";
    }
}
