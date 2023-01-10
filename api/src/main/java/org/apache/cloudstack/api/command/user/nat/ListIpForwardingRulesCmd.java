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
package org.apache.cloudstack.api.command.user.nat;

import java.util.ArrayList;
import java.util.List;


import org.apache.cloudstack.api.APICommand;
import org.apache.cloudstack.api.ApiConstants;
import org.apache.cloudstack.api.BaseListProjectAndAccountResourcesCmd;
import org.apache.cloudstack.api.Parameter;
import org.apache.cloudstack.api.response.FirewallRuleResponse;
import org.apache.cloudstack.api.response.IPAddressResponse;
import org.apache.cloudstack.api.response.IpForwardingRuleResponse;
import org.apache.cloudstack.api.response.ListResponse;
import org.apache.cloudstack.api.response.UserVmResponse;

import com.cloud.network.rules.FirewallRule;
import com.cloud.network.rules.StaticNatRule;
import com.cloud.utils.Pair;

@APICommand(name = "listIpForwardingRules", description = "List the IP forwarding rules", responseObject = FirewallRuleResponse.class,
        requestHasSensitiveInfo = false, responseHasSensitiveInfo = false)
public class ListIpForwardingRulesCmd extends BaseListProjectAndAccountResourcesCmd {


    /////////////////////////////////////////////////////
    //////////////// API parameters /////////////////////
    /////////////////////////////////////////////////////

    @Parameter(name = ApiConstants.IP_ADDRESS_ID,
               type = CommandType.UUID,
               entityType = IPAddressResponse.class,
               description = "list the rule belonging to this public IP address")
    private Long publicIpAddressId;

    @Parameter(name = ApiConstants.ID, type = CommandType.UUID, entityType = FirewallRuleResponse.class, description = "Lists rule with the specified ID.")
    private Long id;

    @Parameter(name = ApiConstants.VIRTUAL_MACHINE_ID,
               type = CommandType.UUID,
               entityType = UserVmResponse.class,
               description = "Lists all rules applied to the specified VM.")
    private Long vmId;

    /////////////////////////////////////////////////////
    /////////////////// Accessors ///////////////////////
    /////////////////////////////////////////////////////

    /////////////////////////////////////////////////////
    /////////////// API Implementation///////////////////
    /////////////////////////////////////////////////////
    public Long getPublicIpAddressId() {
        return publicIpAddressId;
    }

    public Long getId() {
        return id;
    }

    public Long getVmId() {
        return vmId;
    }

    @Override
    public void execute() {
        Pair<List<? extends FirewallRule>, Integer> result =
            _rulesService.searchStaticNatRules(publicIpAddressId, id, vmId, this.getStartIndex(), this.getPageSizeVal(), this.getAccountName(), this.getDomainId(),
                this.getProjectId(), this.isRecursive(), this.listAll());
        ListResponse<IpForwardingRuleResponse> response = new ListResponse<IpForwardingRuleResponse>();
        List<IpForwardingRuleResponse> ipForwardingResponses = new ArrayList<IpForwardingRuleResponse>();
        for (FirewallRule rule : result.first()) {
            StaticNatRule staticNatRule = _rulesService.buildStaticNatRule(rule, false);
            IpForwardingRuleResponse resp = _responseGenerator.createIpForwardingRuleResponse(staticNatRule);
            if (resp != null) {
                ipForwardingResponses.add(resp);
            }
        }
        response.setResponses(ipForwardingResponses, result.second());
        response.setResponseName(getCommandName());
        this.setResponseObject(response);
    }

}
