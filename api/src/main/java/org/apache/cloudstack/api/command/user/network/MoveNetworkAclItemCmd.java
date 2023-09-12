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
package org.apache.cloudstack.api.command.user.network;

import org.apache.cloudstack.api.APICommand;
import org.apache.cloudstack.api.ApiCommandResourceType;
import org.apache.cloudstack.api.ApiConstants;
import org.apache.cloudstack.api.BaseAsyncCustomIdCmd;
import org.apache.cloudstack.api.Parameter;
import org.apache.cloudstack.api.response.NetworkACLItemResponse;
import org.apache.cloudstack.context.CallContext;
import org.apache.log4j.Logger;

import com.cloud.event.EventTypes;
import com.cloud.network.vpc.NetworkACLItem;
import com.cloud.user.Account;

@APICommand(name = "moveNetworkAclItem", description = "Move an ACL rule to a position bettwen two other ACL rules of the same ACL network list", responseObject = NetworkACLItemResponse.class, requestHasSensitiveInfo = false, responseHasSensitiveInfo = false)
public class MoveNetworkAclItemCmd extends BaseAsyncCustomIdCmd {

    public static final Logger s_logger = Logger.getLogger(MoveNetworkAclItemCmd.class.getName());
    private static final String s_name = "moveNetworkAclItemResponse";

    @Parameter(name = ApiConstants.ID, type = CommandType.STRING, required = true, description = "The ID of the network ACL rule that is being moved to a new position.")
    private String uuidRuleBeingMoved;

    @Parameter(name = ApiConstants.PREVIOUS_ACL_RULE_ID, type = CommandType.STRING, description = "The ID of the first rule that is right before the new position where the rule being moved is going to be placed. This value can be 'NULL' if the rule is being moved to the first position of the network ACL list.")
    private String previousAclRuleUuid;

    @Parameter(name = ApiConstants.NEXT_ACL_RULE_ID, type = CommandType.STRING, description = "The ID of the rule that is right after the new position where the rule being moved is going to be placed. This value can be 'NULL' if the rule is being moved to the last position of the network ACL list.")
    private String nextAclRuleUuid;

    @Parameter(name = ApiConstants.MOVE_ACL_CONSISTENCY_HASH, type = CommandType.STRING, description = "Md5 hash used to check the consistency of the ACL rule list before applying the ACL rule move. This check is useful to manage concurrency problems that may happen when multiple users are editing the same ACL rule listing. The parameter is not required. Therefore, if the user does not send it, they assume the risk of moving ACL rules without checking the consistency of the access control list before executing the move. We use MD5 hash function on a String that is composed of all UUIDs of the ACL rules in concatenated in their respective order (order defined via 'number' field).")
    private String aclConsistencyHash;

    @Override
    public void execute() {
        CallContext.current().setEventDetails(getEventDescription());

        NetworkACLItem aclItem = _networkACLService.moveNetworkAclRuleToNewPosition(this);

        NetworkACLItemResponse aclResponse = _responseGenerator.createNetworkACLItemResponse(aclItem);
        setResponseObject(aclResponse);
        aclResponse.setResponseName(getCommandName());
    }

    public String getUuidRuleBeingMoved() {
        return uuidRuleBeingMoved;
    }

    public String getPreviousAclRuleUuid() {
        return previousAclRuleUuid;
    }

    public String getNextAclRuleUuid() {
        return nextAclRuleUuid;
    }

    @Override
    public void checkUuid() {
        if (this.getCustomId() != null) {
            _uuidMgr.checkUuid(this.getCustomId(), NetworkACLItem.class);
        }
    }

    @Override
    public String getEventType() {
        return EventTypes.EVENT_NETWORK_ACL_ITEM_UPDATE;
    }

    @Override
    public String getEventDescription() {
        return String.format("Placing network ACL item [%s] between [%s] and [%s].", uuidRuleBeingMoved, previousAclRuleUuid, nextAclRuleUuid);
    }

    @Override
    public String getCommandName() {
        return s_name;
    }

    @Override
    public long getEntityOwnerId() {
        Account caller = CallContext.current().getCallingAccount();
        return caller.getAccountId();
    }

    public String getAclConsistencyHash() {
        return aclConsistencyHash;
    }

    @Override
    public ApiCommandResourceType getApiResourceType() {
        return ApiCommandResourceType.NetworkAclItem;
    }
}
