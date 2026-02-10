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

import com.cloud.dc.DataCenter;
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
import org.apache.cloudstack.api.response.DomainResponse;
import org.apache.cloudstack.api.response.KMSKeyResponse;
import org.apache.cloudstack.api.response.ZoneResponse;
import org.apache.cloudstack.framework.kms.KMSException;
import org.apache.cloudstack.kms.KMSManager;

import javax.inject.Inject;

@APICommand(name = "migrateVolumesToKMS",
            description = "Migrates passphrase-based volumes to KMS (admin only)",
            responseObject = AsyncJobResponse.class,
            since = "4.23.0",
            authorized = {RoleType.Admin},
            requestHasSensitiveInfo = false,
            responseHasSensitiveInfo = false)
public class MigrateVolumesToKMSCmd extends BaseAsyncCmd {
    private static final String s_name = "migratevolumestokmsresponse";

    @Inject
    private KMSManager kmsManager;

    /////////////////////////////////////////////////////
    //////////////// API parameters /////////////////////
    /////////////////////////////////////////////////////

    @Parameter(name = ApiConstants.ZONE_ID,
               required = true,
               type = CommandType.UUID,
               entityType = ZoneResponse.class,
               description = "Zone ID")
    private Long zoneId;

    @Parameter(name = ApiConstants.ACCOUNT,
               type = CommandType.STRING,
               description = "Migrate volumes for specific account")
    private String accountName;

    @Parameter(name = ApiConstants.DOMAIN_ID,
               type = CommandType.UUID,
               entityType = DomainResponse.class,
               description = "Domain ID")
    private Long domainId;

    @Parameter(name = ApiConstants.ID,
               required = true,
               type = CommandType.UUID,
               entityType = KMSKeyResponse.class,
               description = "KMS Key ID to use for migrating volumes")
    private Long kmsKeyId;

    /////////////////////////////////////////////////////
    /////////////////// Accessors ///////////////////////
    /////////////////////////////////////////////////////

    public Long getZoneId() {
        return zoneId;
    }

    public String getAccountName() {
        return accountName;
    }

    public Long getDomainId() {
        return domainId;
    }

    public Long getKmsKeyId() {
        return kmsKeyId;
    }

    /////////////////////////////////////////////////////
    /////////////// API Implementation///////////////////
    /////////////////////////////////////////////////////

    @Override
    public void execute() {
        try {
            kmsManager.migrateVolumesToKMS(this);
        } catch (KMSException e) {
            throw new ServerApiException(ApiErrorCode.INTERNAL_ERROR,
                    "Failed to migrate volumes to KMS: " + e.getMessage());
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
        return com.cloud.event.EventTypes.EVENT_VOLUME_MIGRATE_TO_KMS;
    }

    @Override
    public String getEventDescription() {
        return "Migrating volumes to KMS for zone: " + _uuidMgr.getUuid(DataCenter.class, zoneId);
    }

    @Override
    public ApiCommandResourceType getApiResourceType() {
        return ApiCommandResourceType.Zone;
    }

    @Override
    public Long getApiResourceId() {
        return zoneId;
    }
}
