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

import java.util.ArrayList;
import java.util.List;

import javax.inject.Inject;

import org.apache.cloudstack.acl.RoleType;
import org.apache.cloudstack.api.APICommand;
import org.apache.cloudstack.api.ApiConstants;
import org.apache.cloudstack.api.BaseCmd;
import org.apache.cloudstack.api.Parameter;
import org.apache.cloudstack.api.response.BackupProviderResponse;
import org.apache.cloudstack.api.response.ListResponse;
import org.apache.cloudstack.backup.BackupManager;
import org.apache.cloudstack.backup.BackupProvider;

import com.cloud.user.Account;

@APICommand(name = "listBackupProviders",
        description = "Lists Backup and Recovery providers",
        responseObject = BackupProviderResponse.class, since = "4.14.0",
        authorized = {RoleType.Admin})
public class ListBackupProvidersCmd extends BaseCmd {

    @Inject
    private BackupManager backupManager;

    /////////////////////////////////////////////////////
    //////////////// API parameters /////////////////////
    /////////////////////////////////////////////////////

    @Parameter(name = ApiConstants.NAME, type = CommandType.STRING, description = "List Backup and Recovery provider by name")
    private String name;

    /////////////////////////////////////////////////////
    /////////////////// Accessors ///////////////////////
    /////////////////////////////////////////////////////

    public String getName() {
        return name;
    }

    @Override
    public long getEntityOwnerId() {
        return Account.ACCOUNT_ID_SYSTEM;
    }

    /////////////////////////////////////////////////////
    /////////////// API Implementation///////////////////
    /////////////////////////////////////////////////////

    private void setupResponse(final List<BackupProvider> providers) {
        final ListResponse<BackupProviderResponse> response = new ListResponse<>();
        final List<BackupProviderResponse> responses = new ArrayList<>();
        for (final BackupProvider provider : providers) {
            if (provider == null || (getName() != null && !provider.getName().equals(getName()))) {
                continue;
            }
            final BackupProviderResponse backupProviderResponse = new BackupProviderResponse();
            backupProviderResponse.setName(provider.getName());
            backupProviderResponse.setDescription(provider.getDescription());
            backupProviderResponse.setObjectName("providers");
            responses.add(backupProviderResponse);
        }
        response.setResponses(responses);
        response.setResponseName(getCommandName());
        setResponseObject(response);
    }

    @Override
    public void execute() {
        List<BackupProvider> providers = backupManager.listBackupProviders();
        setupResponse(providers);
    }
}
