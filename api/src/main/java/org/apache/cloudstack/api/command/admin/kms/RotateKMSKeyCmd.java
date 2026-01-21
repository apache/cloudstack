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
package org.apache.cloudstack.api.command.admin.kms;

import com.cloud.user.Account;
import org.apache.cloudstack.acl.RoleType;
import org.apache.cloudstack.api.APICommand;
import org.apache.cloudstack.api.ApiCommandResourceType;
import org.apache.cloudstack.api.ApiConstants;
import org.apache.cloudstack.api.ApiErrorCode;
import org.apache.cloudstack.api.BaseAsyncCmd;
import org.apache.cloudstack.api.Parameter;
import org.apache.cloudstack.api.ServerApiException;
import org.apache.cloudstack.api.response.AsyncJobResponse;
import org.apache.cloudstack.api.response.HSMProfileResponse;
import org.apache.cloudstack.api.response.KMSKeyResponse;
import org.apache.cloudstack.framework.kms.KMSException;
import org.apache.cloudstack.kms.KMSManager;

import javax.inject.Inject;

@APICommand(name = "rotateKMSKey",
            description = "Rotates KEK by creating new version and scheduling gradual re-encryption (admin only)",
            responseObject = AsyncJobResponse.class,
            since = "4.23.0",
            authorized = {RoleType.Admin},
            requestHasSensitiveInfo = false,
            responseHasSensitiveInfo = false)
public class RotateKMSKeyCmd extends BaseAsyncCmd {
    private static final String s_name = "rotatekmskeyresponse";

    @Inject
    private KMSManager kmsManager;

    @Parameter(name = ApiConstants.ID,
               required = true,
               type = CommandType.UUID,
               entityType = KMSKeyResponse.class,
               description = "KMS Key UUID to rotate")
    private Long id;

    @Parameter(name = ApiConstants.KEY_BITS,
               type = CommandType.INTEGER,
               description = "Key size for new KEK (default: same as current)")
    private Integer keyBits;

    @Parameter(name = ApiConstants.HSM_PROFILE_ID,
            type = CommandType.UUID,
            entityType = HSMProfileResponse.class,
               description = "The target HSM profile ID for the new KEK version. If provided, migrates the key to this HSM.")
    private String hsmProfile;

    public Long getId() {
        return id;
    }

    public Integer getKeyBits() {
        return keyBits;
    }

    public String getHsmProfile() {
        return hsmProfile;
    }

    @Override
    public void execute() {
        try {
            kmsManager.rotateKMSKey(this);
        } catch (KMSException e) {
            throw new ServerApiException(ApiErrorCode.INTERNAL_ERROR,
                    "Failed to rotate KMS key: " + e.getMessage());
        }
    }

    @Override
    public String getCommandName() {
        return s_name;
    }

    @Override
    public long getEntityOwnerId() {
        return Account.ACCOUNT_ID_SYSTEM;
    }

    @Override
    public String getEventType() {
        return com.cloud.event.EventTypes.EVENT_KMS_KEK_ROTATE;
    }

    @Override
    public String getEventDescription() {
        return "Rotating KMS key: " + _uuidMgr.getUuid(KMSKeyResponse.class, id);
    }

    @Override
    public ApiCommandResourceType getApiResourceType() {
        return ApiCommandResourceType.KmsKey;
    }

    @Override
    public Long getApiResourceId() {
        return id;
    }
}
