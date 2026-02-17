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

import com.cloud.exception.ConcurrentOperationException;
import com.cloud.exception.InsufficientCapacityException;
import com.cloud.exception.NetworkRuleConflictException;
import com.cloud.exception.ResourceAllocationException;
import com.cloud.exception.ResourceUnavailableException;
import com.cloud.utils.StringUtils;
import org.apache.cloudstack.acl.RoleType;
import org.apache.cloudstack.api.APICommand;
import org.apache.cloudstack.api.ApiConstants;
import org.apache.cloudstack.api.ApiErrorCode;
import org.apache.cloudstack.api.BaseCmd;
import org.apache.cloudstack.api.Parameter;
import org.apache.cloudstack.api.ServerApiException;
import org.apache.cloudstack.api.response.DomainResponse;
import org.apache.cloudstack.api.response.HSMProfileResponse;
import org.apache.cloudstack.api.response.ProjectResponse;
import org.apache.cloudstack.api.response.ZoneResponse;
import org.apache.cloudstack.context.CallContext;
import org.apache.cloudstack.framework.kms.KMSException;
import org.apache.cloudstack.kms.HSMProfile;
import org.apache.cloudstack.kms.KMSManager;
import org.apache.commons.collections.MapUtils;

import javax.inject.Inject;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

@APICommand(name = "addHSMProfile", description = "Adds a new HSM profile", responseObject = HSMProfileResponse.class,
            requestHasSensitiveInfo = true, responseHasSensitiveInfo = true, since = "4.23.0",
            authorized = {RoleType.Admin, RoleType.ResourceAdmin, RoleType.DomainAdmin, RoleType.User})
public class AddHSMProfileCmd extends BaseCmd {

    @Inject
    private KMSManager kmsManager;

    @Parameter(name = ApiConstants.NAME, type = CommandType.STRING, required = true,
               description = "the name of the HSM profile")
    private String name;

    @Parameter(name = ApiConstants.PROTOCOL, type = CommandType.STRING,
               description = "the protocol of the HSM profile (PKCS11, KMIP, etc.). Default is 'pkcs11'")
    private String protocol;

    @Parameter(name = ApiConstants.ZONE_ID, type = CommandType.UUID, entityType = ZoneResponse.class,
               description = "the zone ID where the HSM profile is available. If null, global scope (for admin only)")
    private Long zoneId;

    @Parameter(name = ApiConstants.DOMAIN_ID, type = CommandType.UUID, entityType = DomainResponse.class,
               description = "the domain ID where the HSM profile is available")
    private Long domainId;

    @Parameter(name = ApiConstants.ACCOUNT, type = CommandType.STRING,
               description = "the account name of the HSM profile owner. Must be used with domainId.")
    private String accountName;

    @Parameter(name = ApiConstants.PROJECT_ID, type = CommandType.UUID, entityType = ProjectResponse.class,
               description = "the ID of the project to add the HSM profile for")
    private Long projectId;

    @Parameter(name = "system", type = CommandType.BOOLEAN,
               description = "whether this is a system HSM profile available to all users globally (root admin only). "
                             + "Default is false")
    private Boolean system;

    @Parameter(name = ApiConstants.VENDOR_NAME, type = CommandType.STRING, description = "the vendor name of the HSM")
    private String vendorName;

    @Parameter(name = ApiConstants.DETAILS, type = CommandType.MAP,
               description = "HSM configuration details (protocol specific)")
    private Map<String, String> details;

    public String getName() {
        return name;
    }

    public String getProtocol() {
        if (StringUtils.isBlank(protocol)) {
            return "pkcs11";
        }
        return protocol;
    }

    public Long getZoneId() {
        return zoneId;
    }

    public Long getDomainId() {
        return domainId;
    }

    public String getAccountName() {
        return accountName;
    }

    public Long getProjectId() {
        return projectId;
    }

    public Boolean isSystem() {
        return system != null && system;
    }

    public String getVendorName() {
        return vendorName;
    }

    public Map<String, String> getDetails() {
        Map<String, String> detailsMap = new HashMap<>();
        if (MapUtils.isNotEmpty(details)) {
            Collection<?> props = details.values();
            for (Object prop : props) {
                HashMap<String, String> detail = (HashMap<String, String>) prop;
                for (Map.Entry<String, String> entry : detail.entrySet()) {
                    detailsMap.put(entry.getKey(), entry.getValue());
                }
            }
        }
        return detailsMap;
    }

    @Override
    public void execute() throws ResourceUnavailableException, InsufficientCapacityException, ServerApiException,
            ConcurrentOperationException, ResourceAllocationException, NetworkRuleConflictException {
        try {
            HSMProfile profile = kmsManager.addHSMProfile(this);
            HSMProfileResponse response = kmsManager.createHSMProfileResponse(profile);
            response.setResponseName(getCommandName());
            setResponseObject(response);
        } catch (KMSException e) {
            throw new ServerApiException(ApiErrorCode.INTERNAL_ERROR, e.getMessage());
        }
    }

    @Override
    public long getEntityOwnerId() {
        Long accountId = _accountService.finalizeAccountId(accountName, domainId, projectId, true);
        if (accountId != null) {
            return accountId;
        }
        return CallContext.current().getCallingAccount().getId();
    }
}
