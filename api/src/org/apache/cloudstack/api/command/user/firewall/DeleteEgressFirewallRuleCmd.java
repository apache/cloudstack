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
// under the License.package org.apache.cloudstack.api.command.user.firewall;

package org.apache.cloudstack.api.command.user.firewall;

import org.apache.cloudstack.api.APICommand;

import org.apache.log4j.Logger;

import org.apache.cloudstack.api.ApiCommandJobType;
import org.apache.cloudstack.api.ApiConstants;
import org.apache.cloudstack.api.ApiErrorCode;
import org.apache.cloudstack.api.BaseAsyncCmd;
import org.apache.cloudstack.api.BaseCmd;
import org.apache.cloudstack.api.Parameter;
import org.apache.cloudstack.api.ServerApiException;
import org.apache.cloudstack.api.response.AccountResponse;
import org.apache.cloudstack.api.response.FirewallRuleResponse;
import org.apache.cloudstack.api.response.SuccessResponse;
import org.apache.cloudstack.context.CallContext;

import com.cloud.event.EventTypes;
import com.cloud.exception.InvalidParameterValueException;
import com.cloud.exception.ResourceUnavailableException;
import com.cloud.network.rules.FirewallRule;

@APICommand(name = "deleteEgressFirewallRule", description="Deletes an ggress firewall rule", responseObject=SuccessResponse.class)
public class DeleteEgressFirewallRuleCmd extends BaseAsyncCmd {
    public static final Logger s_logger = Logger.getLogger(DeleteEgressFirewallRuleCmd.class.getName());
    private static final String s_name = "deleteegressfirewallruleresponse";

    /////////////////////////////////////////////////////
    //////////////// API parameters /////////////////////
    /////////////////////////////////////////////////////

    
    @Parameter(name=ApiConstants.ID, type=CommandType.UUID, entityType = FirewallRuleResponse.class, required=true, description="the ID of the firewall rule")
    private Long id;

    // unexposed parameter needed for events logging
    @Parameter(name=ApiConstants.ACCOUNT_ID, type=CommandType.UUID, entityType = AccountResponse.class, expose=false)
    private Long ownerId;
    /////////////////////////////////////////////////////
    /////////////////// Accessors ///////////////////////
    /////////////////////////////////////////////////////

    public Long getId() {
        return id;
    }

    /////////////////////////////////////////////////////
    /////////////// API Implementation///////////////////
    /////////////////////////////////////////////////////
    @Override
    public String getCommandName() {
        return s_name;
    }

    @Override
    public String getEventType() {
        return EventTypes.EVENT_FIREWALL_CLOSE;
    }

    @Override
    public String getEventDescription() {
        return  ("Deleting egress firewall rule id=" + id);
    }

    @Override
    public long getEntityOwnerId() {
        if (ownerId == null) {
            FirewallRule rule = _entityMgr.findById(FirewallRule.class, id);
            if (rule == null) {
                throw new InvalidParameterValueException("Unable to find egress firewall rule by id");
            } else {
                ownerId = _entityMgr.findById(FirewallRule.class, id).getAccountId();
            }
        }
        return ownerId;
    }

    @Override
    public void execute() throws ResourceUnavailableException {
        CallContext.current().setEventDetails("Rule Id: " + id);
        boolean result = _firewallService.revokeFirewallRule(id, true);

        if (result) {
            SuccessResponse response = new SuccessResponse(getCommandName());
            this.setResponseObject(response);
        } else {
            throw new ServerApiException(ApiErrorCode.INTERNAL_ERROR, "Failed to delete egress firewall rule");
        }
    }


    @Override
    public String getSyncObjType() {
        return BaseAsyncCmd.networkSyncObject;
    }

    @Override
    public Long getSyncObjId() {
        return _firewallService.getFirewallRule(id).getNetworkId();
    }

    @Override
    public ApiCommandJobType getInstanceType() {
        return ApiCommandJobType.FirewallRule;
    }
}
