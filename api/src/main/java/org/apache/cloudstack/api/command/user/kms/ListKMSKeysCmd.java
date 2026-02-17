/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package org.apache.cloudstack.api.command.user.kms;

import org.apache.cloudstack.acl.RoleType;
import org.apache.cloudstack.api.APICommand;
import org.apache.cloudstack.api.ApiConstants;
import org.apache.cloudstack.api.BaseListProjectAndAccountResourcesCmd;
import org.apache.cloudstack.api.Parameter;
import org.apache.cloudstack.api.ResponseObject.ResponseView;
import org.apache.cloudstack.api.command.user.UserCmd;
import org.apache.cloudstack.api.response.HSMProfileResponse;
import org.apache.cloudstack.api.response.KMSKeyResponse;
import org.apache.cloudstack.api.response.ListResponse;
import org.apache.cloudstack.api.response.ZoneResponse;
import org.apache.cloudstack.kms.KMSManager;

import javax.inject.Inject;

@APICommand(name = "listKMSKeys",
            description = "Lists KMS keys available to the caller",
            responseObject = KMSKeyResponse.class,
            responseView = ResponseView.Restricted,
            since = "4.23.0",
            authorized = {RoleType.Admin, RoleType.ResourceAdmin, RoleType.DomainAdmin, RoleType.User},
            requestHasSensitiveInfo = false,
            responseHasSensitiveInfo = false)
public class ListKMSKeysCmd extends BaseListProjectAndAccountResourcesCmd implements UserCmd {
    private static final String s_name = "listkmskeysresponse";

    @Inject
    private KMSManager kmsManager;

    @Parameter(name = ApiConstants.ID,
               type = CommandType.UUID,
               entityType = KMSKeyResponse.class,
               description = "List KMS key by UUID")
    private Long id;

    @Parameter(name = ApiConstants.PURPOSE,
               type = CommandType.STRING,
               description = "Filter by purpose: volume, tls")
    private String purpose;

    @Parameter(name = ApiConstants.ZONE_ID,
               type = CommandType.UUID,
               entityType = ZoneResponse.class,
               description = "Filter by zone ID")
    private Long zoneId;

    @Parameter(name = ApiConstants.ENABLED,
               type = CommandType.BOOLEAN,
               description = "Filter by enabled status")
    private Boolean enabled;

    @Parameter(name = ApiConstants.HSM_PROFILE_ID,
               type = CommandType.UUID,
               entityType = HSMProfileResponse.class,
               description = "Filter by HSM profile ID")
    private Long hsmProfileId;

    public Long getId() {
        return id;
    }

    public String getPurpose() {
        return purpose;
    }

    public Long getZoneId() {
        return zoneId;
    }

    public Boolean getEnabled() {
        return enabled;
    }

    public Long getHsmProfileId() {
        return hsmProfileId;
    }

    @Override
    public void execute() {
        ListResponse<KMSKeyResponse> listResponse = kmsManager.listKMSKeys(this);
        listResponse.setResponseName(getCommandName());
        setResponseObject(listResponse);
    }

    @Override
    public String getCommandName() {
        return s_name;
    }
}
