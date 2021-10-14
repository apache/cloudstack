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
package org.apache.cloudstack.network.tungsten.api.command;

import com.cloud.event.EventTypes;
import com.cloud.exception.ConcurrentOperationException;
import com.cloud.exception.InsufficientCapacityException;
import com.cloud.exception.NetworkRuleConflictException;
import com.cloud.exception.ResourceAllocationException;
import com.cloud.exception.ResourceUnavailableException;
import com.cloud.user.Account;
import org.apache.cloudstack.api.APICommand;
import org.apache.cloudstack.api.ApiConstants;
import org.apache.cloudstack.api.ApiErrorCode;
import org.apache.cloudstack.api.BaseAsyncCmd;
import org.apache.cloudstack.api.Parameter;
import org.apache.cloudstack.api.ServerApiException;
import org.apache.cloudstack.api.response.ZoneResponse;
import org.apache.cloudstack.network.tungsten.api.response.TungstenFabricRoutingPolicyTermResponse;
import org.apache.cloudstack.network.tungsten.model.RoutingPolicyPrefix;
import org.apache.cloudstack.network.tungsten.model.RoutingPolicyThenTerm;
import org.apache.cloudstack.network.tungsten.service.TungstenService;
import org.apache.log4j.Logger;

import javax.inject.Inject;
import java.util.ArrayList;
import java.util.List;

@APICommand(name = AddTungstenFabricRoutingPolicyTermCmd.APINAME, description = "add Tungsten-Fabric static route to network",
        responseObject = TungstenFabricRoutingPolicyTermResponse.class, requestHasSensitiveInfo = false,
        responseHasSensitiveInfo = false)
public class AddTungstenFabricRoutingPolicyTermCmd extends BaseAsyncCmd {
    public static final Logger s_logger = Logger.getLogger(AddTungstenFabricRoutingPolicyTermCmd.class.getName());
    public static final String APINAME = "addTungstenFabricRoutingPolicyTerm";

    @Parameter(name = ApiConstants.ZONE_ID, type = CommandType.UUID, entityType = ZoneResponse.class, required = true, description = "the ID of zone")
    private Long zoneId;

    @Parameter(name = ApiConstants.TUNGSTEN_ROUTING_POLICY_UUID, type = CommandType.STRING, required = true, description = "Tungsten-Fabric routing policy uuid")
    private String routingPolicyUuid;

    @Parameter(name = ApiConstants.TUNGSTEN_ROUTING_POLICY_FROM_TERM_COMMUNITIES, type = CommandType.LIST, collectionType = CommandType.STRING, description = "Tungsten-Fabric routing policy communities list")
    private List<String> communities;

    @Parameter(name = ApiConstants.TUNGSTEN_ROUTING_POLICY_MATCH_ALL, type = CommandType.BOOLEAN, description = "Tungsten-Fabric routing policy match all communities")
    private boolean matchAll;

    @Parameter(name = ApiConstants.TUNGSTEN_ROUTING_POLICY_PROTOCOL, type = CommandType.LIST, collectionType = CommandType.STRING, description = "Tungsten-Fabric routing policy protocol list")
    private List<String> protocolList;

    @Parameter(name = ApiConstants.TUNGSTEN_ROUTING_POLICY_FROM_TERM_PREFIX_LIST,
            type = CommandType.LIST, collectionType = CommandType.STRING,
            description = "routing policy prefix list")
    private List<String> prefixList;

    @Parameter(name = ApiConstants.TUNGSTEN_ROUTING_POLICY_THEN_TERM_LIST,
            type = CommandType.LIST, collectionType = CommandType.STRING,
            description = "routing policy then term list")
    private List<String> thenTermList;


    @Inject
    TungstenService tungstenService;

    @Override
    public void execute() throws ResourceUnavailableException, InsufficientCapacityException, ServerApiException, ConcurrentOperationException, ResourceAllocationException, NetworkRuleConflictException {
        List<RoutingPolicyPrefix> routingPolicyPrefixList = new ArrayList<>();
        for(String item : prefixList) {
            String [] prefix = item.split("&");
            RoutingPolicyPrefix routingPolicyPrefix = new RoutingPolicyPrefix(prefix[0], prefix[1]);
            routingPolicyPrefixList.add(routingPolicyPrefix);
        }
        List<RoutingPolicyThenTerm> thenTerms = new ArrayList<>();
        for(String item : thenTermList) {
            String [] thenTerm = item.split("&");
            RoutingPolicyThenTerm routingPolicyThenTerm = new RoutingPolicyThenTerm(thenTerm[0], thenTerm[1], thenTerm[2]);
            thenTerms.add(routingPolicyThenTerm);
        }
        TungstenFabricRoutingPolicyTermResponse routingPolicyTermResponse = tungstenService.addRoutingPolicyTerm(zoneId, routingPolicyUuid,
                communities, matchAll, protocolList, routingPolicyPrefixList, thenTerms);
        if (routingPolicyTermResponse == null) {
            throw new ServerApiException(ApiErrorCode.INTERNAL_ERROR, "Failed to add terms to Tungsten-Fabric routing policy");
        } else {
            routingPolicyTermResponse.setResponseName(getCommandName());
            setResponseObject(routingPolicyTermResponse);
        }
    }

    @Override
    public String getEventType() {
        return EventTypes.EVENT_TUNGSTEN_ADD_ROUTING_POLICY_TERM;
    }

    @Override
    public String getEventDescription() {
        return "add Tungsten-Fabric routing policy term";
    }

    @Override
    public String getCommandName() {
        return APINAME.toLowerCase() + BaseAsyncCmd.RESPONSE_SUFFIX;
    }

    @Override
    public long getEntityOwnerId() {
        return Account.ACCOUNT_ID_SYSTEM;
    }
}
