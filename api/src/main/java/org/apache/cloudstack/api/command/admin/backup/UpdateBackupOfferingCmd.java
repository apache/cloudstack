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
package org.apache.cloudstack.api.command.admin.backup;

import javax.inject.Inject;

import org.apache.cloudstack.api.APICommand;
import org.apache.cloudstack.api.ApiCommandResourceType;
import org.apache.cloudstack.api.ApiConstants;
import org.apache.cloudstack.api.ApiErrorCode;
import org.apache.cloudstack.api.BaseCmd;
import org.apache.cloudstack.api.Parameter;
import org.apache.cloudstack.api.ServerApiException;
import org.apache.cloudstack.api.command.offering.DomainAndZoneIdResolver;
import org.apache.cloudstack.api.response.BackupOfferingResponse;
import org.apache.cloudstack.backup.BackupManager;
import org.apache.cloudstack.backup.BackupOffering;
import org.apache.cloudstack.utils.reflectiontostringbuilderutils.ReflectionToStringBuilderUtils;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.StringUtils;

import com.cloud.exception.InvalidParameterValueException;
import com.cloud.user.Account;
import com.cloud.utils.exception.CloudRuntimeException;

import java.util.List;
import java.util.function.LongFunction;

@APICommand(name = "updateBackupOffering", description = "Updates a backup offering.", responseObject = BackupOfferingResponse.class,
requestHasSensitiveInfo = false, responseHasSensitiveInfo = false, since = "4.16.0")
public class UpdateBackupOfferingCmd extends BaseCmd implements DomainAndZoneIdResolver {

    @Inject
    private BackupManager backupManager;

    /////////////////////////////////////////////////////
    //////////////// API parameters /////////////////////
    /////////////////////////////////////////////////////
    @Parameter(name = ApiConstants.ID, type = CommandType.UUID, entityType = BackupOfferingResponse.class, required = true, description = "The ID of the Backup Offering to be updated")
    private Long id;

    @Parameter(name = ApiConstants.DESCRIPTION, type = CommandType.STRING, description = "The description of the Backup Offering to be updated")
    private String description;

    @Parameter(name = ApiConstants.NAME, type = CommandType.STRING, description = "The name of the Backup Offering to be updated")
    private String name;

    @Parameter(name = ApiConstants.ALLOW_USER_DRIVEN_BACKUPS, type = CommandType.BOOLEAN, description = "Whether to allow user driven backups or not")
    private Boolean allowUserDrivenBackups;

    @Parameter(name = ApiConstants.DOMAIN_ID,
            type = CommandType.STRING,
            description = "the ID of the containing domain(s) as comma separated string, public for public offerings",
            since = "4.23.0",
            length = 4096)
    private String domainIds;

    /////////////////////////////////////////////////////
    /////////////////// Accessors ///////////////////////
    /////////////////////////////////////////////////////
    public Long getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getDescription() {
        return description;
    }

    public Boolean getAllowUserDrivenBackups() {
        return allowUserDrivenBackups;
    }

    /////////////////////////////////////////////////////
    /////////////// API Implementation///////////////////
    /////////////////////////////////////////////////////
    @Override
    public void execute() {
        try {
            if (StringUtils.isAllEmpty(getName(), getDescription()) && getAllowUserDrivenBackups() == null && CollectionUtils.isEmpty(getDomainIds())) {
                throw new InvalidParameterValueException(String.format("Can't update Backup Offering [id: %s] because there are no parameters to be updated, at least one of the",
                        "following should be informed: name, description or allowUserDrivenBackups.", id));
            }

            BackupOffering result = backupManager.updateBackupOffering(this);

            if (result == null) {
                throw new ServerApiException(ApiErrorCode.INTERNAL_ERROR, String.format("Failed to update backup offering %s.",
                        ReflectionToStringBuilderUtils.reflectOnlySelectedFields(this, "id", "name", "description", "allowUserDrivenBackups")));
            }
            BackupOfferingResponse response = _responseGenerator.createBackupOfferingResponse(result);
            response.setResponseName(getCommandName());
            this.setResponseObject(response);
        } catch (CloudRuntimeException e) {
            ApiErrorCode paramError = e instanceof InvalidParameterValueException ? ApiErrorCode.PARAM_ERROR : ApiErrorCode.INTERNAL_ERROR;
            logger.error("Failed to update Backup Offering [id: {}] due to: [{}].", id, e.getMessage(), e);
            throw new ServerApiException(paramError, e.getMessage());
        }
    }

    public List<Long> getDomainIds() {
        // backupManager may be null in unit tests where the command is spied without injection.
        // Avoid creating a method reference to a null receiver which causes NPE. When backupManager
        // is null, pass null as the defaultDomainsProvider so resolveDomainIds will simply return
        // an empty list or parse the explicit domainIds string.
        LongFunction<List<Long>> defaultDomainsProvider = null;
        if (backupManager != null) {
            defaultDomainsProvider = backupManager::getBackupOfferingDomains;
        }
        return resolveDomainIds(domainIds, id, defaultDomainsProvider, "backup offering");
    }

    @Override
    public long getEntityOwnerId() {
        return Account.ACCOUNT_ID_SYSTEM;
    }

    @Override
    public Long getApiResourceId() {
        return id;
    }

    @Override
    public ApiCommandResourceType getApiResourceType() {
        return ApiCommandResourceType.BackupOffering;
    }
}
