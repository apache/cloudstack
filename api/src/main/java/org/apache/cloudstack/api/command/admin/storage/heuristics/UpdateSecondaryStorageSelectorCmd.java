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
package org.apache.cloudstack.api.command.admin.storage.heuristics;

import com.cloud.user.Account;
import org.apache.cloudstack.acl.RoleType;
import org.apache.cloudstack.api.APICommand;
import org.apache.cloudstack.api.ApiConstants;
import org.apache.cloudstack.api.ApiErrorCode;
import org.apache.cloudstack.api.BaseCmd;
import org.apache.cloudstack.api.Parameter;
import org.apache.cloudstack.api.ServerApiException;
import org.apache.cloudstack.api.response.SecondaryStorageHeuristicsResponse;
import org.apache.cloudstack.secstorage.heuristics.Heuristic;

@APICommand(name = "updateSecondaryStorageSelector", description = "Updates an existing secondary storage selector.", since = "4.19.0", responseObject =
        SecondaryStorageHeuristicsResponse.class, requestHasSensitiveInfo = false, entityType = {Heuristic.class}, responseHasSensitiveInfo = false, authorized = {RoleType.Admin})
public class UpdateSecondaryStorageSelectorCmd extends BaseCmd {
    @Parameter(name = ApiConstants.ID, type = BaseCmd.CommandType.UUID, entityType = SecondaryStorageHeuristicsResponse.class, required = true,
            description = "The unique identifier of the secondary storage selector.")
    private Long id;

    @Parameter(name = ApiConstants.HEURISTIC_RULE, required = true, type = BaseCmd.CommandType.STRING, description = "The heuristic rule, in JavaScript language. It is required " +
            "that it returns the UUID of a secondary storage pool. An example of a rule is `if (snapshot.hypervisorType === 'KVM') { '7832f261-c602-4e8e-8580-2496ffbbc45d'; " +
            "}` would allocate all snapshots with the KVM hypervisor to the specified secondary storage UUID.", length = 65535)
    private String heuristicRule;

    public Long getId() {
        return id;
    }

    public String getHeuristicRule() {
        return heuristicRule;
    }

    @Override
    public void execute()  {
        Heuristic heuristic = _storageService.updateSecondaryStorageHeuristic(this);

        if (heuristic == null) {
            throw new ServerApiException(ApiErrorCode.INTERNAL_ERROR, "Failed to update the secondary storage selector.");
        }

        SecondaryStorageHeuristicsResponse response = _responseGenerator.createSecondaryStorageSelectorResponse(heuristic);
        response.setResponseName(getCommandName());
        this.setResponseObject(response);
    }

    @Override
    public long getEntityOwnerId() {
        return Account.ACCOUNT_ID_SYSTEM;
    }
}
