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
package org.apache.cloudstack.api.command.user.address;

import org.apache.cloudstack.api.ApiCommandResourceType;

import org.apache.cloudstack.acl.RoleType;
import org.apache.cloudstack.api.APICommand;
import org.apache.cloudstack.api.ApiConstants;
import org.apache.cloudstack.api.ApiErrorCode;
import org.apache.cloudstack.api.BaseCmd;
import org.apache.cloudstack.api.Parameter;
import org.apache.cloudstack.api.ResponseObject.ResponseView;
import org.apache.cloudstack.api.ServerApiException;
import org.apache.cloudstack.api.command.user.UserCmd;
import org.apache.cloudstack.api.response.DomainResponse;
import org.apache.cloudstack.api.response.IPAddressResponse;
import org.apache.cloudstack.api.response.ProjectResponse;
import org.apache.cloudstack.context.CallContext;

import com.cloud.exception.ConcurrentOperationException;
import com.cloud.exception.InvalidParameterValueException;
import com.cloud.exception.PermissionDeniedException;
import com.cloud.exception.ResourceAllocationException;
import com.cloud.exception.ResourceUnavailableException;
import com.cloud.network.IpAddress;
import com.cloud.projects.Project;
import com.cloud.user.Account;

@APICommand(name = "reserveIpAddress",
        description = "Reserve a public IP to an account.",
        since = "4.17",
        responseObject = IPAddressResponse.class,
        responseView = ResponseView.Restricted,
        requestHasSensitiveInfo = false,
        responseHasSensitiveInfo = false,
        authorized = {RoleType.Admin, RoleType.ResourceAdmin, RoleType.DomainAdmin, RoleType.User})
public class ReserveIPAddrCmd extends BaseCmd implements UserCmd {

    /////////////////////////////////////////////////////
    //////////////// API parameters /////////////////////
    /////////////////////////////////////////////////////

    @Parameter(name = ApiConstants.ACCOUNT,
            type = CommandType.STRING,
            description = "the account to reserve with this IP address")
    private String accountName;

    @Parameter(name = ApiConstants.DOMAIN_ID,
            type = CommandType.UUID,
            entityType = DomainResponse.class,
            description = "the ID of the domain to reserve with this IP address")
    private Long domainId;

    @Parameter(name = ApiConstants.PROJECT_ID,
            type = CommandType.UUID,
            entityType = ProjectResponse.class,
            description = "the ID of the project to reserve with this IP address")
    private Long projectId;

    @Parameter(name = ApiConstants.FOR_DISPLAY,
            type = CommandType.BOOLEAN,
            description = "an optional field, whether to the display the IP to the end user or not",
            authorized = {RoleType.Admin})
    private Boolean display;

    @Parameter(name = ApiConstants.ID,
            type = CommandType.UUID,
            entityType = IPAddressResponse.class,
            required = true,
            description = "the ID of the public IP address to reserve")
    private Long id;

    /////////////////////////////////////////////////////
    /////////////////// Accessors ///////////////////////
    /////////////////////////////////////////////////////

    public String getAccountName() {
        if (accountName != null) {
            return accountName;
        }
        return CallContext.current().getCallingAccount().getAccountName();
    }

    public long getDomainId() {
        if (domainId != null) {
            return domainId;
        }
        return CallContext.current().getCallingAccount().getDomainId();
    }

    public Long getIpAddressId() {
        return id;
    }

    @Override
    public boolean isDisplay() {
        if (display == null)
            return true;
        else
            return display;
    }

    @Override
    public long getEntityOwnerId() {
        Account caller = CallContext.current().getCallingAccount();
        if (accountName != null && domainId != null) {
            Account account = _accountService.finalizeOwner(caller, accountName, domainId, projectId);
            return account.getId();
        } else if (projectId != null) {
            Project project = _projectService.getProject(projectId);
            if (project != null) {
                if (project.getState() == Project.State.Active) {
                    return project.getProjectAccountId();
                } else {
                    throw new PermissionDeniedException("Can't add resources to the project with specified projectId in state=" + project.getState() +
                            " as it's no longer active");
                }
            } else {
                throw new InvalidParameterValueException("Unable to find project by ID");
            }
        }

        return caller.getAccountId();
    }

    /////////////////////////////////////////////////////
    /////////////// API Implementation///////////////////
    /////////////////////////////////////////////////////

    @Override
    public void execute() throws ResourceUnavailableException, ResourceAllocationException, ConcurrentOperationException {
        IpAddress result = _networkService.reserveIpAddress(_accountService.getAccount(getEntityOwnerId()), isDisplay(), getIpAddressId());
        if (result != null) {
            IPAddressResponse ipResponse = _responseGenerator.createIPAddressResponse(getResponseView(), result);
            ipResponse.setResponseName(getCommandName());
            setResponseObject(ipResponse);
        } else {
            throw new ServerApiException(ApiErrorCode.INTERNAL_ERROR, "Failed to reserve IP address");
        }
    }

    @Override
    public Long getApiResourceId() {
        return getIpAddressId();
    }

    @Override
    public ApiCommandResourceType getApiResourceType() {
        return ApiCommandResourceType.IpAddress;
    }
}
