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
package org.apache.cloudstack.api.command.user.project;

import org.apache.log4j.Logger;

import org.apache.cloudstack.api.APICommand;
import org.apache.cloudstack.api.ApiConstants;
import org.apache.cloudstack.api.ApiErrorCode;
import org.apache.cloudstack.api.BaseAsyncCreateCmd;
import org.apache.cloudstack.api.Parameter;
import org.apache.cloudstack.api.ServerApiException;
import org.apache.cloudstack.api.response.DomainResponse;
import org.apache.cloudstack.api.response.ProjectResponse;
import org.apache.cloudstack.context.CallContext;

import com.cloud.event.EventTypes;
import com.cloud.exception.InvalidParameterValueException;
import com.cloud.exception.ResourceAllocationException;
import com.cloud.projects.Project;
import com.cloud.user.Account;

@APICommand(name = "createProject", description = "Creates a project", responseObject = ProjectResponse.class, since = "3.0.0",
        requestHasSensitiveInfo = false, responseHasSensitiveInfo = false)
public class CreateProjectCmd extends BaseAsyncCreateCmd {
    public static final Logger s_logger = Logger.getLogger(CreateProjectCmd.class.getName());

    private static final String s_name = "createprojectresponse";

    // ///////////////////////////////////////////////////
    // ////////////// API parameters /////////////////////
    // ///////////////////////////////////////////////////

    @Parameter(name = ApiConstants.ACCOUNT, type = CommandType.STRING, description = "account who will be Admin for the project")
    private String accountName;

    @Parameter(name = ApiConstants.DOMAIN_ID, type = CommandType.UUID, entityType = DomainResponse.class, description = "domain ID of the account owning a project")
    private Long domainId;

    @Parameter(name = ApiConstants.NAME, type = CommandType.STRING, required = true, description = "name of the project")
    private String name;

    @Parameter(name = ApiConstants.DISPLAY_TEXT, type = CommandType.STRING, required = true, description = "display text of the project")
    private String displayText;

    // ///////////////////////////////////////////////////
    // ///////////////// Accessors ///////////////////////
    // ///////////////////////////////////////////////////

    public String getAccountName() {
        if (accountName != null) {
            return accountName;
        } else {
            return CallContext.current().getCallingAccount().getAccountName();
        }
    }

    public Long getDomainId() {
        if (domainId != null) {
            return domainId;
        } else {
            return CallContext.current().getCallingAccount().getDomainId();
        }

    }

    public String getName() {
        return name;
    }

    public String getDisplayText() {
        return displayText;
    }

    @Override
    public String getCommandName() {
        return s_name;
    }

    @Override
    public long getEntityOwnerId() {
        Account caller = CallContext.current().getCallingAccount();

        if ((accountName != null && domainId == null) || (domainId != null && accountName == null)) {
            throw new InvalidParameterValueException("Account name and domain id must be specified together");
        }

        if (accountName != null) {
            return _accountService.finalizeOwner(caller, accountName, domainId, null).getId();
        }

        return caller.getId();
    }

    // ///////////////////////////////////////////////////
    // ///////////// API Implementation///////////////////
    // ///////////////////////////////////////////////////

    @Override
    public void execute() {
        Project project = _projectService.enableProject(this.getEntityId());
        if (project != null) {
            ProjectResponse response = _responseGenerator.createProjectResponse(project);
            response.setResponseName(getCommandName());
            this.setResponseObject(response);
        } else {
            throw new ServerApiException(ApiErrorCode.INTERNAL_ERROR, "Failed to create a project");
        }
    }

    @Override
    public void create() throws ResourceAllocationException {
        CallContext.current().setEventDetails("Project Name: " + getName());
        Project project = _projectService.createProject(getName(), getDisplayText(), getAccountName(), getDomainId());
        if (project != null) {
            this.setEntityId(project.getId());
            this.setEntityUuid(project.getUuid());
        } else {
            throw new ServerApiException(ApiErrorCode.INTERNAL_ERROR, "Failed to create a project");
        }
    }

    @Override
    public String getEventType() {
        return EventTypes.EVENT_PROJECT_CREATE;
    }

    @Override
    public String getEventDescription() {
        return "creating project";
    }

}
