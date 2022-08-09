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
package org.apache.cloudstack.api.command.user.firewall;

import org.apache.log4j.Logger;

import org.apache.cloudstack.acl.SecurityChecker.AccessType;
import org.apache.cloudstack.api.ACL;
import org.apache.cloudstack.api.APICommand;
import org.apache.cloudstack.api.ApiCommandResourceType;
import org.apache.cloudstack.api.ApiConstants;
import org.apache.cloudstack.api.ApiErrorCode;
import org.apache.cloudstack.api.BaseAsyncCmd;
import org.apache.cloudstack.api.Parameter;
import org.apache.cloudstack.api.ServerApiException;
import org.apache.cloudstack.api.response.AccountResponse;
import org.apache.cloudstack.api.response.FirewallRuleResponse;
import org.apache.cloudstack.api.response.SuccessResponse;
import org.apache.cloudstack.context.CallContext;

import com.cloud.event.EventTypes;
import com.cloud.exception.InvalidParameterValueException;
import com.cloud.network.rules.PortForwardingRule;

@APICommand(name = "deletePortForwardingRule", description = "Deletes a port forwarding rule", responseObject = SuccessResponse.class, entityType = {PortForwardingRule.class},
        requestHasSensitiveInfo = false, responseHasSensitiveInfo = false)
public class DeletePortForwardingRuleCmd extends BaseAsyncCmd {
    public static final Logger s_logger = Logger.getLogger(DeletePortForwardingRuleCmd.class.getName());
    private static final String s_name = "deleteportforwardingruleresponse";

    /////////////////////////////////////////////////////
    //////////////// API parameters /////////////////////
    /////////////////////////////////////////////////////

    @ACL(accessType = AccessType.OperateEntry)
    @Parameter(name = ApiConstants.ID,
               type = CommandType.UUID,
               entityType = FirewallRuleResponse.class,
               required = true,
               description = "the ID of the port forwarding rule")
    private Long id;

    // unexposed parameter needed for events logging
    @Parameter(name = ApiConstants.ACCOUNT_ID, type = CommandType.UUID, entityType = AccountResponse.class, expose = false)
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
        return EventTypes.EVENT_NET_RULE_DELETE;
    }

    @Override
    public String getEventDescription() {
        return ("Deleting port forwarding rule for ID=" + id);
    }

    @Override
    public long getEntityOwnerId() {
        if (ownerId == null) {
            PortForwardingRule rule = _entityMgr.findById(PortForwardingRule.class, id);
            if (rule == null) {
                throw new InvalidParameterValueException("Unable to find port forwarding rule by ID=" + id);
            } else {
                ownerId = _entityMgr.findById(PortForwardingRule.class, id).getAccountId();
            }

        }
        return ownerId;
    }

    @Override
    public void execute() {
        CallContext.current().setEventDetails("Rule ID: " + id);
        //revoke corresponding firewall rule first
        boolean result = _firewallService.revokeRelatedFirewallRule(id, true);
        result = result && _rulesService.revokePortForwardingRule(id, true);

        if (result) {
            SuccessResponse response = new SuccessResponse(getCommandName());
            setResponseObject(response);
        } else {
            throw new ServerApiException(ApiErrorCode.INTERNAL_ERROR, "Failed to delete port forwarding rule");
        }
    }

    @Override
    public String getSyncObjType() {
        return BaseAsyncCmd.networkSyncObject;
    }

    @Override
    public Long getSyncObjId() {
        return _entityMgr.findById(PortForwardingRule.class, id).getNetworkId();
    }

    @Override
    public ApiCommandResourceType getApiResourceType() {
        return ApiCommandResourceType.FirewallRule;
    }
}
