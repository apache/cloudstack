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

import com.cloud.exception.ResourceAllocationException;
import org.apache.cloudstack.acl.RoleType;
import org.apache.cloudstack.api.APICommand;
import org.apache.cloudstack.api.ApiCommandResourceType;
import org.apache.cloudstack.api.ApiConstants;
import org.apache.cloudstack.api.ApiErrorCode;
import org.apache.cloudstack.api.BaseCmd;
import org.apache.cloudstack.api.Parameter;
import org.apache.cloudstack.api.ServerApiException;
import org.apache.cloudstack.api.command.user.UserCmd;
import org.apache.cloudstack.api.response.DomainResponse;
import org.apache.cloudstack.api.response.HSMProfileResponse;
import org.apache.cloudstack.api.response.KMSKeyResponse;
import org.apache.cloudstack.api.response.ProjectResponse;
import org.apache.cloudstack.api.response.ZoneResponse;
import org.apache.cloudstack.context.CallContext;
import org.apache.cloudstack.framework.kms.KMSException;
import org.apache.cloudstack.kms.KMSManager;

import javax.inject.Inject;

@APICommand(name = "createKMSKey",
            description = "Creates a new KMS key (Key Encryption Key) for encryption",
            responseObject = KMSKeyResponse.class,
            since = "4.23.0",
            authorized = {RoleType.Admin, RoleType.ResourceAdmin, RoleType.DomainAdmin, RoleType.User},
            requestHasSensitiveInfo = false,
            responseHasSensitiveInfo = false)
public class CreateKMSKeyCmd extends BaseCmd implements UserCmd {

    @Inject
    private KMSManager kmsManager;

    @Parameter(name = ApiConstants.NAME,
               required = true,
               type = CommandType.STRING,
               description = "Name of the KMS key")
    private String name;

    @Parameter(name = ApiConstants.DESCRIPTION,
               type = CommandType.STRING,
               description = "Description of the KMS key")
    private String description;

    @Parameter(name = ApiConstants.PURPOSE,
               type = CommandType.STRING,
               description = "Purpose of the key: volume, tls. (default: volume)")
    private String purpose;

    @Parameter(name = ApiConstants.ZONE_ID,
               required = true,
               type = CommandType.UUID,
               entityType = ZoneResponse.class,
               description = "Zone ID where the key will be valid")
    private Long zoneId;

    @Parameter(name = ApiConstants.ACCOUNT,
               type = CommandType.STRING,
               description = "Account name (for creating keys for child accounts - requires domain admin or admin)")
    private String accountName;

    @Parameter(name = ApiConstants.DOMAIN_ID,
               type = CommandType.UUID,
               entityType = DomainResponse.class,
               description = "Domain ID (for creating keys for child accounts - requires domain admin or admin)")
    private Long domainId;

    @Parameter(name = ApiConstants.PROJECT_ID,
               type = CommandType.UUID,
               entityType = ProjectResponse.class,
               description = "ID of the project to create the KMS key for")
    private Long projectId;

    @Parameter(name = ApiConstants.KEY_BITS,
               type = CommandType.INTEGER,
               description = "Key size in bits: 128, 192, or 256 (default: 256)")
    private Integer keyBits;

    @Parameter(name = ApiConstants.HSM_PROFILE_ID,
               type = CommandType.UUID,
               entityType = HSMProfileResponse.class,
               required = true,
               description = "ID of HSM profile to create key in")
    private Long hsmProfileId;

    public String getName() {
        return name;
    }

    public String getDescription() {
        return description;
    }

    public String getPurpose() {
        return purpose == null ? "volume" : purpose;
    }

    public Long getZoneId() {
        return zoneId;
    }

    public String getAccountName() {
        return accountName;
    }

    public Long getDomainId() {
        return domainId;
    }

    public Long getProjectId() {
        return projectId;
    }

    public Integer getKeyBits() {
        return keyBits != null ? keyBits : 256;
    }

    public Long getHsmProfileId() {
        return hsmProfileId;
    }

    @Override
    public void execute() throws ResourceAllocationException {
        try {
            KMSKeyResponse response = kmsManager.createKMSKey(this);
            response.setResponseName(getCommandName());
            setResponseObject(response);
        } catch (KMSException e) {
            throw new ServerApiException(ApiErrorCode.INTERNAL_ERROR,
                    "Failed to create KMS key: " + e.getMessage());
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

    @Override
    public ApiCommandResourceType getApiResourceType() {
        return ApiCommandResourceType.KmsKey;
    }
}
