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
package org.apache.cloudstack.api.command.iam;

import javax.inject.Inject;

import org.apache.log4j.Logger;

import org.apache.cloudstack.iam.IAMApiService;
import org.apache.cloudstack.api.ACL;
import org.apache.cloudstack.api.APICommand;
import org.apache.cloudstack.api.ApiCommandJobType;
import org.apache.cloudstack.api.ApiConstants;
import org.apache.cloudstack.api.ApiErrorCode;
import org.apache.cloudstack.api.BaseAsyncCreateCmd;
import org.apache.cloudstack.api.Parameter;
import org.apache.cloudstack.api.ServerApiException;
import org.apache.cloudstack.api.response.DomainResponse;
import org.apache.cloudstack.api.response.iam.IAMPolicyResponse;
import org.apache.cloudstack.context.CallContext;
import org.apache.cloudstack.iam.api.IAMPolicy;

import com.cloud.event.EventTypes;
import com.cloud.exception.ResourceAllocationException;
import com.cloud.user.Account;

@APICommand(name = "createIAMPolicy", responseObject = IAMPolicyResponse.class, description = "Creates an iam policy")
public class CreateIAMPolicyCmd extends BaseAsyncCreateCmd {
    public static final Logger s_logger = Logger.getLogger(CreateIAMPolicyCmd.class.getName());

    private static final String s_name = "createiampolicyresponse";

    @Inject
    public IAMApiService _iamApiSrv;

    // ///////////////////////////////////////////////////
    // ////////////// API parameters /////////////////////
    // ///////////////////////////////////////////////////

    @Parameter(name = ApiConstants.ACCOUNT, type = CommandType.STRING, description = "an account for the iam policy. Must be used with domainId.")
    private String accountName;

    @Parameter(name = ApiConstants.DOMAIN_ID, type = CommandType.UUID, description = "domainId of the account owning the iam policy", entityType = DomainResponse.class)
    private Long domainId;

    @Parameter(name = ApiConstants.DESCRIPTION, type = CommandType.STRING, description = "optional description of the iam policy")
    private String description;

    @Parameter(name = ApiConstants.NAME, type = CommandType.STRING, required = true, description = "name of the iam policy")
    private String name;

    @ACL
    @Parameter(name = ApiConstants.IAM_PARENT_POLICY_ID, type = CommandType.UUID, description = "The ID of parent iam policy.", entityType = IAMPolicyResponse.class)
    private Long parentPolicyId;


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

    public String getName() {
        return name;
    }

    public Long getParentPolicyId() {
        return parentPolicyId;
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
        if ((account == null) || _accountService.isAdmin(account.getId())) {
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
        IAMPolicy policy = _entityMgr.findById(IAMPolicy.class, getEntityId());
        if (policy != null) {
            IAMPolicyResponse response = _iamApiSrv.createIAMPolicyResponse(policy);
            response.setResponseName(getCommandName());
            setResponseObject(response);
        } else {
            throw new ServerApiException(ApiErrorCode.INTERNAL_ERROR, "Failed to create iam policy:" + name);
        }
    }

    @Override
    public void create() throws ResourceAllocationException {
        Account account = CallContext.current().getCallingAccount();
        IAMPolicy result = _iamApiSrv.createIAMPolicy(account, name, description, parentPolicyId);
        if (result != null) {
            setEntityId(result.getId());
            setEntityUuid(result.getUuid());
        } else {
            throw new ServerApiException(ApiErrorCode.INTERNAL_ERROR, "Failed to create iam policy entity" + name);
        }

    }

    @Override
    public String getEventType() {
        return EventTypes.EVENT_IAM_POLICY_CREATE;
    }

    @Override
    public String getEventDescription() {
        return "creating IAM policy";
    }

    @Override
    public String getCreateEventType() {
        return EventTypes.EVENT_IAM_POLICY_CREATE;
    }

    @Override
    public String getCreateEventDescription() {
        return "creating IAM policy";
    }

    @Override
    public ApiCommandJobType getInstanceType() {
        return ApiCommandJobType.IAMPolicy;
    }

}
