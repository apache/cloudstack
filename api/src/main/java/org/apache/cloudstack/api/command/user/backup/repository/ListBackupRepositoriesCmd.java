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

package org.apache.cloudstack.api.command.user.backup.repository;

import com.cloud.exception.ConcurrentOperationException;
import com.cloud.exception.InsufficientCapacityException;
import com.cloud.exception.NetworkRuleConflictException;
import com.cloud.exception.ResourceAllocationException;
import com.cloud.exception.ResourceUnavailableException;
import com.cloud.utils.Pair;
import org.apache.cloudstack.acl.RoleType;
import org.apache.cloudstack.api.APICommand;
import org.apache.cloudstack.api.ApiConstants;
import org.apache.cloudstack.api.ApiErrorCode;
import org.apache.cloudstack.api.BaseListCmd;
import org.apache.cloudstack.api.Parameter;
import org.apache.cloudstack.api.ServerApiException;
import org.apache.cloudstack.api.response.BackupRepositoryResponse;
import org.apache.cloudstack.api.response.ListResponse;
import org.apache.cloudstack.api.response.ZoneResponse;
import org.apache.cloudstack.backup.BackupRepository;
import org.apache.cloudstack.backup.BackupRepositoryService;

import javax.inject.Inject;
import java.util.ArrayList;
import java.util.List;

@APICommand(name = "listBackupRepositories",
        description = "Lists all backup repositories",
        responseObject = BackupRepositoryResponse.class, since = "4.20.0",
        authorized = {RoleType.Admin})
public class ListBackupRepositoriesCmd extends BaseListCmd {

    @Inject
    BackupRepositoryService backupRepositoryService;

    /////////////////////////////////////////////////////
    //////////////// API parameters /////////////////////
    /////////////////////////////////////////////////////
    @Parameter(name = ApiConstants.NAME, type = CommandType.STRING, description = "name of the backup repository")
    private String name;

    @Parameter(name = ApiConstants.ZONE_ID,
            type = CommandType.UUID,
            entityType = ZoneResponse.class,
            description = "ID of the zone where the backup repository is to be added")
    private Long zoneId;

    @Parameter(name = ApiConstants.PROVIDER, type = CommandType.STRING, description = "the backup repository provider")
    private String provider;

    @Parameter(name = ApiConstants.ID, type = CommandType.UUID, entityType = BackupRepositoryResponse.class, description = "ID of the backup repository")
    private Long id;

    /////////////////////////////////////////////////////
    //////////////// Accessors //////////////////////////
    /////////////////////////////////////////////////////


    public String getName() {
        return name;
    }

    public Long getZoneId() {
        return zoneId;
    }

    public String getProvider() {
        return provider;
    }

    public Long getId() {
        return id;
    }

    @Override
    public void execute() throws ResourceUnavailableException, InsufficientCapacityException, ServerApiException, ConcurrentOperationException, ResourceAllocationException, NetworkRuleConflictException {
        try {
            Pair<List<BackupRepository>, Integer> repositoriesPair = backupRepositoryService.listBackupRepositories(this);
            List<BackupRepository> backupRepositories = repositoriesPair.first();
            ListResponse<BackupRepositoryResponse> response = new ListResponse<>();
            List<BackupRepositoryResponse> responses = new ArrayList<>();
            for (BackupRepository repository : backupRepositories) {
                responses.add(_responseGenerator.createBackupRepositoryResponse(repository));
            }
            response.setResponses(responses, repositoriesPair.second());
            response.setResponseName(getCommandName());
            setResponseObject(response);
        } catch (Exception e) {
            String msg = String.format("Error listing backup repositories, due to: %s", e.getMessage());
            throw new ServerApiException(ApiErrorCode.INTERNAL_ERROR, msg);
        }

    }
}
