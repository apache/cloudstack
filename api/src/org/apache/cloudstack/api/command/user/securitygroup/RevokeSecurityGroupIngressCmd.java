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
package org.apache.cloudstack.api.command.user.securitygroup;

import org.apache.log4j.Logger;

import org.apache.cloudstack.api.APICommand;
import org.apache.cloudstack.api.ApiCommandJobType;
import org.apache.cloudstack.api.ApiConstants;
import org.apache.cloudstack.api.ApiErrorCode;
import org.apache.cloudstack.api.BaseAsyncCmd;
import org.apache.cloudstack.api.Parameter;
import org.apache.cloudstack.api.ServerApiException;
import org.apache.cloudstack.api.response.SecurityGroupRuleResponse;
import org.apache.cloudstack.api.response.SuccessResponse;

import com.cloud.event.EventTypes;
import com.cloud.network.security.SecurityGroup;
import com.cloud.network.security.SecurityRule;
import com.cloud.user.Account;

@APICommand(name = "revokeSecurityGroupIngress", responseObject = SuccessResponse.class, description = "Deletes a particular ingress rule from this security group")
public class RevokeSecurityGroupIngressCmd extends BaseAsyncCmd {
    public static final Logger s_logger = Logger.getLogger(RevokeSecurityGroupIngressCmd.class.getName());

    private static final String s_name = "revokesecuritygroupingressresponse";

    // ///////////////////////////////////////////////////
    // ////////////// API parameters /////////////////////
    // ///////////////////////////////////////////////////

    @Parameter(name = ApiConstants.ID, type = CommandType.UUID, required = true, description = "The ID of the ingress rule", entityType = SecurityGroupRuleResponse.class)
    private Long id;

    // ///////////////////////////////////////////////////
    // ///////////////// Accessors ///////////////////////
    // ///////////////////////////////////////////////////

    public Long getId() {
        return id;
    }

    // ///////////////////////////////////////////////////
    // ///////////// API Implementation///////////////////
    // ///////////////////////////////////////////////////

    @Override
    public String getCommandName() {
        return s_name;
    }

    public static String getResultObjectName() {
        return "revokesecuritygroupingress";
    }

    @Override
    public long getEntityOwnerId() {
        SecurityRule rule = _entityMgr.findById(SecurityRule.class, getId());
        if (rule != null) {
            SecurityGroup group = _entityMgr.findById(SecurityGroup.class, rule.getSecurityGroupId());
            if (group != null) {
                return group.getAccountId();
            }
        }

        return Account.ACCOUNT_ID_SYSTEM; // no account info given, parent this command to SYSTEM so ERROR events are tracked
    }

    @Override
    public String getEventType() {
        return EventTypes.EVENT_SECURITY_GROUP_REVOKE_INGRESS;
    }

    @Override
    public String getEventDescription() {
        return "revoking ingress rule id: " + getId();
    }

    @Override
    public void execute() {
        boolean result = _securityGroupService.revokeSecurityGroupIngress(this);
        if (result) {
            SuccessResponse response = new SuccessResponse(getCommandName());
            this.setResponseObject(response);
        } else {
            throw new ServerApiException(ApiErrorCode.INTERNAL_ERROR, "Failed to revoke security group ingress rule");
        }
    }

    @Override
    public ApiCommandJobType getInstanceType() {
        return ApiCommandJobType.SecurityGroup;
    }

    @Override
    public Long getInstanceId() {
        return getId();
    }
}
