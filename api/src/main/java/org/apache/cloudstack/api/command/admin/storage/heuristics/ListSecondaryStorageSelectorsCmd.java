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

import org.apache.cloudstack.acl.RoleType;
import org.apache.cloudstack.api.APICommand;
import org.apache.cloudstack.api.ApiConstants;
import org.apache.cloudstack.api.BaseListCmd;
import org.apache.cloudstack.api.Parameter;
import org.apache.cloudstack.api.response.ListResponse;
import org.apache.cloudstack.api.response.SecondaryStorageHeuristicsResponse;
import org.apache.cloudstack.api.response.ZoneResponse;
import org.apache.cloudstack.secstorage.heuristics.Heuristic;

import static org.apache.cloudstack.api.ApiConstants.HEURISTIC_TYPE_VALID_OPTIONS;

@APICommand(name = "listSecondaryStorageSelectors", description = "Lists the secondary storage selectors and their rules.", since = "4.19.0", responseObject =
        SecondaryStorageHeuristicsResponse.class, requestHasSensitiveInfo = false, entityType = {Heuristic.class}, responseHasSensitiveInfo = false, authorized = {RoleType.Admin})
public class ListSecondaryStorageSelectorsCmd extends BaseListCmd {

    @Parameter(name = ApiConstants.ZONE_ID, required = true, entityType = ZoneResponse.class, type = CommandType.UUID, description = "The zone ID to be used in the search filter.")
    private Long zoneId;

    @Parameter(name = ApiConstants.TYPE, type = CommandType.STRING, description =
            "Whether to filter the selectors by type and, if so, which one. " + HEURISTIC_TYPE_VALID_OPTIONS)
    private String type;

    @Parameter(name = ApiConstants.SHOW_REMOVED, type = CommandType.BOOLEAN, description = "Show removed heuristics.")
    private boolean showRemoved = false;

    public Long getZoneId() {
        return zoneId;
    }

    public String getType() {
        return type;
    }

    public boolean isShowRemoved() {
        return showRemoved;
    }

    @Override
    public void execute()  {
        ListResponse<SecondaryStorageHeuristicsResponse> response = _queryService.listSecondaryStorageSelectors(this);
        response.setResponseName(getCommandName());
        this.setResponseObject(response);
    }
}
