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

import org.apache.cloudstack.api.APICommand;
import org.apache.cloudstack.api.ApiCommandJobType;
import org.apache.cloudstack.api.ApiConstants;
import org.apache.cloudstack.api.ApiErrorCode;
import org.apache.cloudstack.api.BaseAsyncCmd;
import org.apache.cloudstack.api.Parameter;
import org.apache.cloudstack.api.ServerApiException;
import org.apache.cloudstack.api.response.AccountResponse;
import org.apache.cloudstack.api.response.FirewallRuleResponse;
import org.apache.cloudstack.api.response.SuccessResponse;
import org.apache.cloudstack.context.CallContext;

import org.apache.log4j.Logger;

import com.cloud.event.EventTypes;
import com.cloud.exception.InvalidParameterValueException;
import com.cloud.network.rules.FirewallRule;

@APICommand(name = "deleteIpForwardingRule", description="Deletes an ip forwarding rule", responseObject=SuccessResponse.class)
public class DeleteIpForwardingRuleCmd extends BaseAsyncCmd {
    public static final Logger s_logger = Logger.getLogger(DeleteIpForwardingRuleCmd.class.getName());

    private static final String s_name = "deleteipforwardingruleresponse";

    /////////////////////////////////////////////////////
    //////////////// API parameters /////////////////////
    /////////////////////////////////////////////////////

    @Parameter(name=ApiConstants.ID, type=CommandType.UUID, entityType = FirewallRuleResponse.class,
            required=true, description="the id of the forwarding rule")
    private Long id;

    // unexposed parameter needed for events logging
    @Parameter(name=ApiConstants.ACCOUNT_ID, type=CommandType.UUID, entityType = AccountResponse.class,
            expose=false)
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
    public void execute(){
        CallContext.current().setEventDetails("Rule Id: "+id);
        boolean result = _firewallService.revokeRelatedFirewallRule(id, true);
        result = result && _rulesService.revokeStaticNatRule(id, true);

        if (result) {
            SuccessResponse response = new SuccessResponse(getCommandName());
            this.setResponseObject(response);
        } else {
            throw new ServerApiException(ApiErrorCode.INTERNAL_ERROR, "Failed to delete ip forwarding rule");
        }
    }

    @Override
    public long getEntityOwnerId() {
        if (ownerId == null) {
            FirewallRule rule = _entityMgr.findById(FirewallRule.class, id);
            if (rule == null) {
                throw new InvalidParameterValueException("Unable to find static nat rule by id: " + id);
            } else {
                ownerId = rule.getAccountId();
            }
        }
        return ownerId;
    }

    @Override
    public String getEventType() {
        return EventTypes.EVENT_NET_RULE_DELETE;
    }

    @Override
    public String getEventDescription() {
        return  ("Deleting an ipforwarding 1:1 NAT rule id:"+id);
    }

    @Override
    public String getSyncObjType() {
        return BaseAsyncCmd.networkSyncObject;
    }

    @Override
    public Long getSyncObjId() {
        return _rulesService.getFirewallRule(id).getNetworkId();
    }

    @Override
    public ApiCommandJobType getInstanceType() {
        return ApiCommandJobType.FirewallRule;
    }

}
