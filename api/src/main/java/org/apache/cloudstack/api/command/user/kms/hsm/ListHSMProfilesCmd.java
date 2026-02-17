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

package org.apache.cloudstack.api.command.user.kms.hsm;

import org.apache.cloudstack.acl.RoleType;
import org.apache.cloudstack.api.APICommand;
import org.apache.cloudstack.api.ApiConstants;
import org.apache.cloudstack.api.BaseListProjectAndAccountResourcesCmd;
import org.apache.cloudstack.api.Parameter;
import org.apache.cloudstack.api.response.HSMProfileResponse;
import org.apache.cloudstack.api.response.ListResponse;
import org.apache.cloudstack.api.response.ZoneResponse;
import org.apache.cloudstack.kms.KMSManager;

import javax.inject.Inject;

@APICommand(name = "listHSMProfiles", description = "Lists HSM profiles", responseObject = HSMProfileResponse.class,
            requestHasSensitiveInfo = false, responseHasSensitiveInfo = true, since = "4.23.0",
            authorized = {RoleType.Admin, RoleType.ResourceAdmin, RoleType.DomainAdmin, RoleType.User})
public class ListHSMProfilesCmd extends BaseListProjectAndAccountResourcesCmd {

    @Inject
    private KMSManager kmsManager;

    @Parameter(name = ApiConstants.ID, type = CommandType.UUID, entityType = HSMProfileResponse.class,
               description = "the HSM profile ID")
    private Long id;

    @Parameter(name = ApiConstants.ZONE_ID, type = CommandType.UUID, entityType = ZoneResponse.class,
               description = "the zone ID")
    private Long zoneId;

    @Parameter(name = ApiConstants.PROTOCOL, type = CommandType.STRING, description = "the protocol of the HSM profile")
    private String protocol;

    @Parameter(name = ApiConstants.ENABLED, type = CommandType.BOOLEAN, description = "list only enabled profiles")
    private Boolean enabled;

    @Parameter(name = ApiConstants.IS_SYSTEM,
               type = CommandType.BOOLEAN,
               description = "when true, non-admin users see only system (global) profiles")
    private Boolean isSystem;

    public Long getId() {
        return id;
    }

    public Long getZoneId() {
        return zoneId;
    }

    public String getProtocol() {
        return protocol;
    }

    public Boolean getEnabled() {
        return enabled;
    }

    public Boolean getIsSystem() {
        return isSystem;
    }

    @Override
    public void execute() {
        ListResponse<HSMProfileResponse> response = kmsManager.listHSMProfiles(this);
        response.setResponseName(getCommandName());
        setResponseObject(response);
    }
}
