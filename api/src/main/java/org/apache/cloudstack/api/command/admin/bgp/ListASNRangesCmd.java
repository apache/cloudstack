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
package org.apache.cloudstack.api.command.admin.bgp;

import com.cloud.bgp.ASNumberRange;
import com.cloud.exception.ConcurrentOperationException;
import com.cloud.exception.InsufficientCapacityException;
import com.cloud.exception.NetworkRuleConflictException;
import com.cloud.exception.ResourceAllocationException;
import com.cloud.exception.ResourceUnavailableException;
import com.cloud.user.Account;
import org.apache.cloudstack.acl.RoleType;
import org.apache.cloudstack.api.APICommand;
import org.apache.cloudstack.api.ApiConstants;
import org.apache.cloudstack.api.ApiErrorCode;
import org.apache.cloudstack.api.BaseCmd;
import org.apache.cloudstack.api.BaseListCmd;
import org.apache.cloudstack.api.Parameter;
import org.apache.cloudstack.api.ServerApiException;
import org.apache.cloudstack.api.response.ASNRangeResponse;
import org.apache.cloudstack.api.response.ListResponse;
import org.apache.cloudstack.api.response.ZoneResponse;

import java.util.ArrayList;
import java.util.List;

@APICommand(name = "listASNRanges",
        description = "List Autonomous Systems Number Ranges",
        responseObject = ASNRangeResponse.class,
        entityType = {ASNumberRange.class},
        since = "4.20.0",
        authorized = {RoleType.Admin})
public class ListASNRangesCmd extends BaseListCmd {

    @Parameter(name = ApiConstants.ZONE_ID, type = BaseCmd.CommandType.UUID, entityType = ZoneResponse.class,
            description = "the zone ID")
    private Long zoneId;

    @Override
    public void execute() throws ResourceUnavailableException, InsufficientCapacityException, ServerApiException, ConcurrentOperationException, ResourceAllocationException, NetworkRuleConflictException {
        try {
            List<ASNumberRange> ranges = bgpService.listASNumberRanges(zoneId);
            ListResponse<ASNRangeResponse> response = new ListResponse<>();
            List<ASNRangeResponse> responses = new ArrayList<>();
            for (ASNumberRange asnRange : ranges) {
                responses.add(_responseGenerator.createASNumberRangeResponse(asnRange));
            }
            response.setResponses(responses);
            response.setResponseName(getCommandName());
            setResponseObject(response);
        } catch (Exception e) {
            String msg = String.format("Error listing AS Number Ranges: %s", e.getMessage());
            throw new ServerApiException(ApiErrorCode.INTERNAL_ERROR, msg);
        }
    }

    public Long getZoneId() {
        return zoneId;
    }

    @Override
    public long getEntityOwnerId() {
        return Account.ACCOUNT_ID_SYSTEM;
    }
}
