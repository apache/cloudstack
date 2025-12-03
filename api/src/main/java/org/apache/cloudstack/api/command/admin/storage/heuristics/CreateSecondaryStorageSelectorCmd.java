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
import org.apache.cloudstack.api.response.SecondaryStorageHeuristicsResponse;
import org.apache.cloudstack.api.response.ZoneResponse;
import org.apache.cloudstack.api.ServerApiException;
import org.apache.cloudstack.secstorage.heuristics.Heuristic;

import static org.apache.cloudstack.api.ApiConstants.HEURISTIC_TYPE_VALID_OPTIONS;

@APICommand(name = "createSecondaryStorageSelector", description = "Creates a secondary storage selector, described by the heuristic rule.", since = "4.19.0", responseObject =
        SecondaryStorageHeuristicsResponse.class, entityType = {Heuristic.class}, requestHasSensitiveInfo = false, responseHasSensitiveInfo = false, authorized = {RoleType.Admin})
public class CreateSecondaryStorageSelectorCmd extends BaseCmd{

    @Parameter(name = ApiConstants.NAME, required = true, type = CommandType.STRING, description = "The name identifying the heuristic rule.")
    private String name;

    @Parameter(name = ApiConstants.DESCRIPTION, required = true, type = BaseCmd.CommandType.STRING, description = "The description of the heuristic rule.")
    private String description;

    @Parameter(name = ApiConstants.ZONE_ID, required = true, entityType = ZoneResponse.class, type = BaseCmd.CommandType.UUID, description = "The zone in which the heuristic " +
            "rule will be applied.")
    private Long zoneId;

    @Parameter(name = ApiConstants.TYPE, required = true, type = BaseCmd.CommandType.STRING, description =
            "The resource type directed to a specific secondary storage by the selector. " + HEURISTIC_TYPE_VALID_OPTIONS)
    private String type;

    @Parameter(name = ApiConstants.HEURISTIC_RULE, required = true, type = BaseCmd.CommandType.STRING, description = "The heuristic rule, in JavaScript language. It is required " +
            "that it returns the UUID of a secondary storage pool. An example of a rule is `if (snapshot.hypervisorType === 'KVM') { '7832f261-c602-4e8e-8580-2496ffbbc45d'; " +
            "}` would allocate all snapshots with the KVM hypervisor to the specified secondary storage UUID.", length = 65535)
    private String heuristicRule;

    public String getName() {
        return name;
    }

    public String getDescription() {
        return description;
    }

    public Long getZoneId() {
        return zoneId;
    }

    public String getType() {
        return type;
    }

    public String getHeuristicRule() {
        return heuristicRule;
    }

    @Override
    public void execute()  {
        Heuristic heuristic = _storageService.createSecondaryStorageHeuristic(this);

        if (heuristic == null) {
            throw new ServerApiException(ApiErrorCode.INTERNAL_ERROR, "Failed to create a secondary storage selector.");
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
