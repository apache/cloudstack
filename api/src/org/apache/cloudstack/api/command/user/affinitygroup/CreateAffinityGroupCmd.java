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
package org.apache.cloudstack.api.command.user.affinitygroup;

import org.apache.cloudstack.affinity.AffinityGroup;
import org.apache.cloudstack.affinity.AffinityGroupResponse;
import org.apache.cloudstack.api.APICommand;
import org.apache.cloudstack.api.ApiCommandJobType;
import org.apache.cloudstack.api.ApiConstants;
import org.apache.cloudstack.api.ApiErrorCode;
import org.apache.cloudstack.api.BaseAsyncCreateCmd;
import org.apache.cloudstack.api.Parameter;
import org.apache.cloudstack.api.ServerApiException;
import org.apache.cloudstack.api.response.DomainResponse;
import org.apache.cloudstack.context.CallContext;

import org.apache.log4j.Logger;

import com.cloud.event.EventTypes;
import com.cloud.exception.ResourceAllocationException;
import com.cloud.user.Account;

@APICommand(name = "createAffinityGroup", responseObject = AffinityGroupResponse.class, description = "Creates an affinity/anti-affinity group")
public class CreateAffinityGroupCmd extends BaseAsyncCreateCmd {
    public static final Logger s_logger = Logger.getLogger(CreateAffinityGroupCmd.class.getName());

    private static final String s_name = "createaffinitygroupresponse";

    // ///////////////////////////////////////////////////
    // ////////////// API parameters /////////////////////
    // ///////////////////////////////////////////////////

    @Parameter(name = ApiConstants.ACCOUNT, type = CommandType.STRING, description = "an account for the affinity group. Must be used with domainId.")
    private String accountName;

    @Parameter(name = ApiConstants.DOMAIN_ID, type = CommandType.UUID, description = "domainId of the account owning the affinity group", entityType = DomainResponse.class)
    private Long domainId;

    @Parameter(name = ApiConstants.DESCRIPTION, type = CommandType.STRING, description = "optional description of the affinity group")
    private String description;

    @Parameter(name = ApiConstants.NAME, type = CommandType.STRING, required = true, description = "name of the affinity group")
    private String affinityGroupName;

    @Parameter(name = ApiConstants.TYPE, type = CommandType.STRING, required = true, description = "Type of the affinity group from the available affinity/anti-affinity group types")
    private String affinityGroupType;


    // ///////////////////////////////////////////////////
    // ///////////////// Accessors ///////////////////////
    // ///////////////////////////////////////////////////

    public String getAccountName() {
        return accountName;
    }

    public String getDescription() {
        return description;
    }

    public Long getDomainId() {
        return domainId;
    }

    public String getAffinityGroupName() {
        return affinityGroupName;
    }

    public String getAffinityGroupType() {
        return affinityGroupType;
    }

    // ///////////////////////////////////////////////////
    // ///////////// API Implementation///////////////////
    // ///////////////////////////////////////////////////

    @Override
    public String getCommandName() {
        return s_name;
    }

    @Override
    public long getEntityOwnerId() {
        Account account = CallContext.current().getCallingAccount();
        if ((account == null) || isAdmin(account.getType())) {
            if ((domainId != null) && (accountName != null)) {
                Account userAccount = _responseGenerator.findAccountByNameDomain(accountName, domainId);
                if (userAccount != null) {
                    return userAccount.getId();
                }
            }
        }

        if (account != null) {
            return account.getId();
        }

        return Account.ACCOUNT_ID_SYSTEM; // no account info given, parent this
                                          // command to SYSTEM so ERROR events
                                          // are tracked
    }

    @Override
    public void execute() {
        AffinityGroup group = _affinityGroupService.getAffinityGroup(getEntityId());
        if (group != null) {
            AffinityGroupResponse response = _responseGenerator.createAffinityGroupResponse(group);
            response.setResponseName(getCommandName());
            this.setResponseObject(response);
        } else {
            throw new ServerApiException(ApiErrorCode.INTERNAL_ERROR, "Failed to create affinity group:"
                    + affinityGroupName);
        }
    }

    @Override
    public void create() throws ResourceAllocationException {
        AffinityGroup result = _affinityGroupService.createAffinityGroup(accountName, domainId, affinityGroupName,
                affinityGroupType, description);
        if (result != null) {
            setEntityId(result.getId());
            setEntityUuid(result.getUuid());
        } else {
            throw new ServerApiException(ApiErrorCode.INTERNAL_ERROR, "Failed to create affinity group entity" + affinityGroupName);
        }

    }

    @Override
    public String getEventType() {
        return EventTypes.EVENT_AFFINITY_GROUP_CREATE;
    }

    @Override
    public String getEventDescription() {
        return "creating Affinity Group";
    }

    @Override
    public String getCreateEventType() {
        return EventTypes.EVENT_AFFINITY_GROUP_CREATE;
    }

    @Override
    public String getCreateEventDescription() {
        return "creating Affinity Group";
    }

    @Override
    public ApiCommandJobType getInstanceType() {
        return ApiCommandJobType.AffinityGroup;
    }

}
