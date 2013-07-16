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

import org.apache.cloudstack.affinity.AffinityGroupResponse;
import org.apache.cloudstack.api.APICommand;
import org.apache.cloudstack.api.ApiCommandJobType;
import org.apache.cloudstack.api.ApiConstants;
import org.apache.cloudstack.api.ApiErrorCode;
import org.apache.cloudstack.api.BaseAsyncCmd;
import org.apache.cloudstack.api.Parameter;
import org.apache.cloudstack.api.ServerApiException;
import org.apache.cloudstack.api.response.DomainResponse;
import org.apache.cloudstack.api.response.SuccessResponse;
import org.apache.cloudstack.context.CallContext;

import org.apache.log4j.Logger;

import com.cloud.event.EventTypes;
import com.cloud.exception.InvalidParameterValueException;
import com.cloud.user.Account;

@APICommand(name = "deleteAffinityGroup", description = "Deletes affinity group", responseObject = SuccessResponse.class)
public class DeleteAffinityGroupCmd extends BaseAsyncCmd {
    public static final Logger s_logger = Logger.getLogger(DeleteAffinityGroupCmd.class.getName());
    private static final String s_name = "deleteaffinitygroupresponse";

    /////////////////////////////////////////////////////
    //////////////// API parameters /////////////////////
    /////////////////////////////////////////////////////

    @Parameter(name = ApiConstants.ACCOUNT, type = CommandType.STRING, description = "the account of the affinity group. Must be specified with domain ID")
    private String accountName;

    @Parameter(name = ApiConstants.DOMAIN_ID, type = CommandType.UUID, description = "the domain ID of account owning the affinity group", entityType = DomainResponse.class)
    private Long domainId;

    @Parameter(name = ApiConstants.ID, type = CommandType.UUID, description = "The ID of the affinity group. Mutually exclusive with name parameter", entityType = AffinityGroupResponse.class)
    private Long id;

    @Parameter(name = ApiConstants.NAME, type = CommandType.STRING, description = "The name of the affinity group. Mutually exclusive with id parameter")
    private String name;


    /////////////////////////////////////////////////////
    /////////////////// Accessors ///////////////////////
    /////////////////////////////////////////////////////

    public String getAccountName() {
        return accountName;
    }

    public Long getDomainId() {
        return domainId;
    }


    public Long getId() {
        if (id != null && name != null) {
            throw new InvalidParameterValueException("name and id parameters are mutually exclusive");
        }

        if (name != null) {
            id = _responseGenerator.getAffinityGroupId(name, getEntityOwnerId());
            if (id == null) {
                throw new InvalidParameterValueException("Unable to find affinity group by name " + name
                        + " for the account id=" + getEntityOwnerId());
            }
        }

        if (id == null) {
            throw new InvalidParameterValueException(
                    "Either id or name parameter is requred by deleteAffinityGroup command");
        }

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
    public void execute(){
        boolean result = _affinityGroupService.deleteAffinityGroup(id, accountName, domainId, name);
        if (result) {
            SuccessResponse response = new SuccessResponse(getCommandName());
            this.setResponseObject(response);
        } else {
            throw new ServerApiException(ApiErrorCode.INTERNAL_ERROR, "Failed to delete affinity group");
        }
    }

    @Override
    public String getEventType() {
        return EventTypes.EVENT_AFFINITY_GROUP_DELETE;
    }

    @Override
    public String getEventDescription() {
        return "Deleting Affinity Group";
    }

    @Override
    public ApiCommandJobType getInstanceType() {
        return ApiCommandJobType.AffinityGroup;
    }
}
