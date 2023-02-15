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


import org.apache.cloudstack.api.response.ProjectResponse;
import org.apache.log4j.Logger;
import org.apache.cloudstack.acl.SecurityChecker.AccessType;
import org.apache.cloudstack.affinity.AffinityGroup;
import org.apache.cloudstack.affinity.AffinityGroupResponse;
import org.apache.cloudstack.api.ACL;
import org.apache.cloudstack.api.APICommand;
import org.apache.cloudstack.api.ApiCommandResourceType;
import org.apache.cloudstack.api.ApiConstants;
import org.apache.cloudstack.api.ApiErrorCode;
import org.apache.cloudstack.api.BaseAsyncCmd;
import org.apache.cloudstack.api.Parameter;
import org.apache.cloudstack.api.ServerApiException;
import org.apache.cloudstack.api.response.DomainResponse;
import org.apache.cloudstack.api.response.SuccessResponse;
import org.apache.cloudstack.context.CallContext;

import com.cloud.event.EventTypes;
import com.cloud.user.Account;

@APICommand(name = "deleteAffinityGroup", description = "Deletes affinity group", responseObject = SuccessResponse.class, entityType = {AffinityGroup.class},
        requestHasSensitiveInfo = false, responseHasSensitiveInfo = false)
public class DeleteAffinityGroupCmd extends BaseAsyncCmd {
    public static final Logger s_logger = Logger.getLogger(DeleteAffinityGroupCmd.class.getName());

    /////////////////////////////////////////////////////
    //////////////// API parameters /////////////////////
    /////////////////////////////////////////////////////

    @Parameter(name = ApiConstants.ACCOUNT, type = CommandType.STRING, description = "the account of the affinity group. Must be specified with domain ID")
    private String accountName;

    @Parameter(name = ApiConstants.DOMAIN_ID,
               type = CommandType.UUID,
               description = "the domain ID of account owning the affinity group",
               entityType = DomainResponse.class)
    private Long domainId;

    @ACL(accessType = AccessType.OperateEntry)
    @Parameter(name = ApiConstants.ID,
               type = CommandType.UUID,
               description = "The ID of the affinity group. Mutually exclusive with name parameter",
               entityType = AffinityGroupResponse.class)
    private Long id;

    @Parameter(name = ApiConstants.NAME, type = CommandType.STRING, description = "The name of the affinity group. Mutually exclusive with ID parameter")
    private String name;

    @Parameter(name = ApiConstants.PROJECT_ID, type = CommandType.UUID, description = "the project of the affinity group", entityType = ProjectResponse.class)
    private Long projectId;

    /////////////////////////////////////////////////////
    /////////////////// Accessors ///////////////////////
    /////////////////////////////////////////////////////

    public String getAccountName() {
        return accountName;
    }

    public Long getDomainId() {
        return domainId;
    }

    public Long getProjectId() {
        return projectId;
    }

    public Long getId() {
        return id;
    }

    /////////////////////////////////////////////////////
    /////////////// API Implementation///////////////////
    /////////////////////////////////////////////////////

    @Override
    public long getEntityOwnerId() {
        Account caller = CallContext.current().getCallingAccount();

        //For domain wide affinity groups (if the affinity group processor type allows it)
        if(projectId == null && domainId != null && accountName == null && _accountService.isRootAdmin(caller.getId())){
            return Account.ACCOUNT_ID_SYSTEM;
        }
        Account owner = _accountService.finalizeOwner(caller, accountName, domainId, projectId);
        if(owner == null){
            return caller.getAccountId();
        }
        return owner.getAccountId();
    }

    @Override
    public void execute() {
        boolean result = _affinityGroupService.deleteAffinityGroup(id, accountName, projectId, domainId, name);
        if (result) {
            SuccessResponse response = new SuccessResponse(getCommandName());
            setResponseObject(response);
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
    public Long getApiResourceId() {
        return getId();
    }

    @Override
    public ApiCommandResourceType getApiResourceType() {
        return ApiCommandResourceType.AffinityGroup;
    }
}
