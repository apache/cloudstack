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

import org.apache.cloudstack.api.APICommand;
import org.apache.cloudstack.api.ApiConstants;
import org.apache.cloudstack.api.ApiErrorCode;
import org.apache.cloudstack.api.BaseCmd;
import org.apache.cloudstack.api.Parameter;
import org.apache.cloudstack.api.ServerApiException;
import org.apache.cloudstack.api.response.DomainResponse;
import org.apache.cloudstack.api.response.ProjectAccountResponse;
import org.apache.cloudstack.api.response.SecurityGroupResponse;
import org.apache.cloudstack.api.response.SuccessResponse;
import org.apache.cloudstack.context.CallContext;

import org.apache.log4j.Logger;

import com.cloud.exception.InvalidParameterValueException;
import com.cloud.exception.ResourceInUseException;

@APICommand(name = "deleteSecurityGroup", description="Deletes security group", responseObject=SuccessResponse.class)
public class DeleteSecurityGroupCmd extends BaseCmd {
    public static final Logger s_logger = Logger.getLogger(DeleteSecurityGroupCmd.class.getName());
    private static final String s_name = "deletesecuritygroupresponse";

    /////////////////////////////////////////////////////
    //////////////// API parameters /////////////////////
    /////////////////////////////////////////////////////

    @Parameter(name=ApiConstants.ACCOUNT, type=CommandType.STRING, description="the account of the security group. Must be specified with domain ID")
    private String accountName;

    @Parameter(name=ApiConstants.DOMAIN_ID, type=CommandType.UUID, description="the domain ID of account owning the security group", entityType=DomainResponse.class)
    private Long domainId;

    @Parameter(name=ApiConstants.PROJECT_ID, type=CommandType.UUID, description="the project of the security group", entityType=ProjectAccountResponse.class)
    private Long projectId;

    @Parameter(name=ApiConstants.ID, type=CommandType.UUID, description="The ID of the security group. Mutually exclusive with name parameter", entityType=SecurityGroupResponse.class)
    private Long id;

    @Parameter(name=ApiConstants.NAME, type=CommandType.STRING, description="The ID of the security group. Mutually exclusive with id parameter")
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

    public Long getProjectId() {
        return projectId;
    }

    public Long getId() {
        if (id != null && name != null) {
            throw new InvalidParameterValueException("name and id parameters are mutually exclusive");
        }

        if (name != null) {
            id = _responseGenerator.getSecurityGroupId(name, getEntityOwnerId());
            if (id == null) {
                throw new InvalidParameterValueException("Unable to find security group by name " + name + " for the account id=" + getEntityOwnerId());
            }
        }

        if (id == null) {
            throw new InvalidParameterValueException("Either id or name parameter is requred by deleteSecurityGroup command");
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
        Long accountId = finalyzeAccountId(accountName, domainId, projectId, true);
        if (accountId == null) {
            return CallContext.current().getCallingAccount().getId();
        }

        return accountId;
    }

    @Override
    public void execute(){
        try{
            boolean result = _securityGroupService.deleteSecurityGroup(this);
            if (result) {
                SuccessResponse response = new SuccessResponse(getCommandName());
                this.setResponseObject(response);
            } else {
                throw new ServerApiException(ApiErrorCode.INTERNAL_ERROR, "Failed to delete security group");
            }
        } catch (ResourceInUseException ex) {
            s_logger.warn("Exception: ", ex);
            throw new ServerApiException(ApiErrorCode.RESOURCE_IN_USE_ERROR, ex.getMessage());
        }
    }
}
