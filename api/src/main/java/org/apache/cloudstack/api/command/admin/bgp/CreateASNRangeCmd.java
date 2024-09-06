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
import org.apache.cloudstack.api.Parameter;
import org.apache.cloudstack.api.ServerApiException;
import org.apache.cloudstack.api.response.ASNRangeResponse;
import org.apache.cloudstack.api.response.ZoneResponse;

@APICommand(name = "createASNRange",
        description = "Creates a range of Autonomous Systems for BGP Dynamic Routing",
        responseObject = ASNRangeResponse.class,
        entityType = {ASNumberRange.class},
        since = "4.20.0",
        authorized = {RoleType.Admin})
public class CreateASNRangeCmd extends BaseCmd {

    @Parameter(name = ApiConstants.ZONE_ID, type = BaseCmd.CommandType.UUID, entityType = ZoneResponse.class,
            description = "the zone ID", required = true)
    private Long zoneId;

    @Parameter(name = ApiConstants.START_ASN, type = CommandType.LONG, required=true, description = "the start AS Number")
    private Long startASNumber;

    @Parameter(name = ApiConstants.END_ASN, type = CommandType.LONG, required=true, description = "the end AS Number")
    private Long endASNumber;

    @Override
    public void execute() throws ResourceUnavailableException, InsufficientCapacityException, ServerApiException, ConcurrentOperationException, ResourceAllocationException, NetworkRuleConflictException {
        try {
            ASNumberRange asnRange = bgpService.createASNumberRange(zoneId, startASNumber, endASNumber);
            ASNRangeResponse response = _responseGenerator.createASNumberRangeResponse(asnRange);
            response.setResponseName(getCommandName());
            setResponseObject(response);
        } catch (Exception e) {
            String msg = String.format("Cannot create AS Number Range %s-%s for zone %s: %s", startASNumber, endASNumber, zoneId, e.getMessage());
            throw new ServerApiException(ApiErrorCode.INTERNAL_ERROR, msg);
        }
    }

    public Long getZoneId() {
        return zoneId;
    }

    public Long getStartASNumber() {
        return startASNumber;
    }

    public Long getEndASNumber() {
        return endASNumber;
    }

    @Override
    public long getEntityOwnerId() {
        return Account.ACCOUNT_ID_SYSTEM;
    }
}
