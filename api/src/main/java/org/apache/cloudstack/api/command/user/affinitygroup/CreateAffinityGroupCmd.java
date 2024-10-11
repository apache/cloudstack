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
import org.apache.cloudstack.api.ApiCommandResourceType;
import org.apache.cloudstack.api.ApiConstants;
import org.apache.cloudstack.api.ApiErrorCode;
import org.apache.cloudstack.api.BaseAsyncCreateCmd;
import org.apache.cloudstack.api.Parameter;
import org.apache.cloudstack.api.ServerApiException;
import org.apache.cloudstack.api.response.DomainResponse;
import org.apache.cloudstack.api.response.ProjectResponse;
import org.apache.cloudstack.context.CallContext;

import com.cloud.event.EventTypes;
import com.cloud.exception.ResourceAllocationException;
import com.cloud.user.Account;

@APICommand(name = "createAffinityGroup", responseObject = AffinityGroupResponse.class, description = "Creates an affinity/anti-affinity group", entityType = {AffinityGroup.class},
        requestHasSensitiveInfo = false, responseHasSensitiveInfo = false)
public class CreateAffinityGroupCmd extends BaseAsyncCreateCmd {


    // ///////////////////////////////////////////////////
    // ////////////// API parameters /////////////////////
    // ///////////////////////////////////////////////////

    @Parameter(name = ApiConstants.ACCOUNT, type = CommandType.STRING, description = "an account for the affinity group. Must be used with domainId.")
    private String accountName;

    @Parameter(name = ApiConstants.DOMAIN_ID,
               type = CommandType.UUID,
               description = "domainId of the account owning the affinity group",
               entityType = DomainResponse.class)
    private Long domainId;

    @Parameter(name = ApiConstants.PROJECT_ID,
               type = CommandType.UUID,
               entityType = ProjectResponse.class,
               description = "create affinity group for project")
    private Long projectId;

    @Parameter(name = ApiConstants.DESCRIPTION, type = CommandType.STRING, description = "optional description of the affinity group")
    private String description;

    @Parameter(name = ApiConstants.NAME, type = CommandType.STRING, required = true, description = "name of the affinity group")
    private String affinityGroupName;

    @Parameter(name = ApiConstants.TYPE,
               type = CommandType.STRING,
               required = true,
               description = "Type of the affinity group from the available affinity/anti-affinity group types")
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

    public Long getProjectId() {
        return projectId;
    }

    // ///////////////////////////////////////////////////
    // ///////////// API Implementation///////////////////
    // ///////////////////////////////////////////////////

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
        AffinityGroup group = _affinityGroupService.getAffinityGroup(getEntityId());
        if (group != null) {
            AffinityGroupResponse response = _responseGenerator.createAffinityGroupResponse(group);
            response.setResponseName(getCommandName());
            setResponseObject(response);
        } else {
            throw new ServerApiException(ApiErrorCode.INTERNAL_ERROR, "Failed to create affinity group:" + affinityGroupName);
        }
    }

    @Override
    public void create() throws ResourceAllocationException {
        AffinityGroup result = _affinityGroupService.createAffinityGroup(this);
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
    public ApiCommandResourceType getApiResourceType() {
        return ApiCommandResourceType.AffinityGroup;
    }

}
