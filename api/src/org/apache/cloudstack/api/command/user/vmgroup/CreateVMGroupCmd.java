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
package org.apache.cloudstack.api.command.user.vmgroup;

import org.apache.cloudstack.api.APICommand;
import org.apache.cloudstack.api.ApiConstants;
import org.apache.cloudstack.api.ApiErrorCode;
import org.apache.cloudstack.api.BaseCmd;
import org.apache.cloudstack.api.Parameter;
import org.apache.cloudstack.api.ServerApiException;
import org.apache.cloudstack.api.response.DomainResponse;
import org.apache.cloudstack.api.response.InstanceGroupResponse;
import org.apache.cloudstack.api.response.ProjectAccountResponse;
import org.apache.cloudstack.context.CallContext;

import org.apache.log4j.Logger;

import com.cloud.vm.InstanceGroup;

@APICommand(name = "createInstanceGroup", description = "Creates a vm group", responseObject = InstanceGroupResponse.class)
public class CreateVMGroupCmd extends BaseCmd {
    public static final Logger s_logger = Logger.getLogger(CreateVMGroupCmd.class.getName());

    private static final String s_name = "createinstancegroupresponse";

    // ///////////////////////////////////////////////////
    // ////////////// API parameters /////////////////////
    // ///////////////////////////////////////////////////

    @Parameter(name = ApiConstants.NAME, type = CommandType.STRING, required = true, description = "the name of the instance group")
    private String groupName;

    @Parameter(name = ApiConstants.ACCOUNT, type = CommandType.STRING,
            description = "the account of the instance group. The account parameter must be used with the domainId parameter.")
    private String accountName;

    @Parameter(name = ApiConstants.DOMAIN_ID, type=CommandType.UUID, entityType=DomainResponse.class,
            description = "the domain ID of account owning the instance group")
    private Long domainId;

    @Parameter(name = ApiConstants.PROJECT_ID, type=CommandType.UUID, entityType=ProjectAccountResponse.class,
            description = "The project of the instance group")
    private Long projectId;

    // ///////////////////////////////////////////////////
    // ///////////////// Accessors ///////////////////////
    // ///////////////////////////////////////////////////

    public String getGroupName() {
        return groupName;
    }

    public String getAccountName() {
        return accountName;
    }

    public Long getDomainId() {
        return domainId;
    }

    public Long getProjectId() {
        return projectId;
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
        Long accountId = finalyzeAccountId(accountName, domainId, projectId, true);
        if (accountId == null) {
            return CallContext.current().getCallingAccount().getId();
        }

        return accountId;
    }

    @Override
    public void execute() {
        InstanceGroup result = _userVmService.createVmGroup(this);
        if (result != null) {
            InstanceGroupResponse response = _responseGenerator.createInstanceGroupResponse(result);
            response.setResponseName(getCommandName());
            this.setResponseObject(response);
        } else {
            throw new ServerApiException(ApiErrorCode.INTERNAL_ERROR, "Failed to create vm instance group");
        }
    }
}
