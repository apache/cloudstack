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

import com.cloud.dc.ASNumber;
import com.cloud.dc.BGPService;
import com.cloud.exception.ConcurrentOperationException;
import com.cloud.exception.InsufficientCapacityException;
import com.cloud.exception.NetworkRuleConflictException;
import com.cloud.exception.ResourceAllocationException;
import com.cloud.exception.ResourceUnavailableException;
import com.cloud.utils.Pair;
import org.apache.cloudstack.api.APICommand;
import org.apache.cloudstack.api.ApiConstants;
import org.apache.cloudstack.api.ApiErrorCode;
import org.apache.cloudstack.api.BaseCmd;
import org.apache.cloudstack.api.BaseListCmd;
import org.apache.cloudstack.api.Parameter;
import org.apache.cloudstack.api.ServerApiException;
import org.apache.cloudstack.api.response.ASNRangeResponse;
import org.apache.cloudstack.api.response.ASNumberResponse;
import org.apache.cloudstack.api.response.ListResponse;
import org.apache.cloudstack.api.response.ZoneResponse;

import javax.inject.Inject;
import java.util.ArrayList;
import java.util.List;

@APICommand(name = "listASNumbers",
        description = "List Autonomous Systems Numbers",
        responseObject = ASNumberResponse.class,
        since = "4.20.0")
public class ListASNumbersCmd extends BaseListCmd {

    @Parameter(name = ApiConstants.ZONE_ID, type = BaseCmd.CommandType.UUID, entityType = ZoneResponse.class,
            description = "the zone ID")
    private Long zoneId;

    @Parameter(name = ApiConstants.ASN_RANGE_ID, type = BaseCmd.CommandType.UUID, entityType = ASNRangeResponse.class,
            description = "the AS Number range ID")
    private Long asNumberRangeId;

    @Parameter(name = ApiConstants.AS_NUMBER, type = CommandType.INTEGER, entityType = ASNumberResponse.class,
            description = "AS number")
    private Integer asNumber;

    @Parameter(name = ApiConstants.IS_ALLOCATED, type = CommandType.BOOLEAN,
            description = "to indicate if the AS number is allocated to any network")
    private Boolean allocated;

    public Long getZoneId() {
        return zoneId;
    }

    public Long getAsNumberRangeId() {
        return asNumberRangeId;
    }

    public Boolean getAllocated() {
        return allocated;
    }

    public Integer getAsNumber() { return asNumber; }

    @Inject
    private BGPService bgpService;

    @Override
    public void execute() throws ResourceUnavailableException, InsufficientCapacityException, ServerApiException, ConcurrentOperationException, ResourceAllocationException, NetworkRuleConflictException {
        try {
            Pair<List<ASNumber>, Integer> pair = bgpService.listASNumbers(this);
            List<ASNumber> asNumbers = pair.first();
            ListResponse<ASNumberResponse> response = new ListResponse<>();
            List<ASNumberResponse> responses = new ArrayList<>();
            for (ASNumber asn : asNumbers) {
                responses.add(_responseGenerator.createASNumberResponse(asn));
            }
            response.setResponses(responses, pair.second());
            response.setResponseName(getCommandName());
            setResponseObject(response);
        } catch (Exception e) {
            String msg = String.format("Error listing AS Numbers for the range %s on the zone %s: %s", asNumberRangeId, zoneId, e.getMessage());
            throw new ServerApiException(ApiErrorCode.INTERNAL_ERROR, msg);
        }

    }
}
