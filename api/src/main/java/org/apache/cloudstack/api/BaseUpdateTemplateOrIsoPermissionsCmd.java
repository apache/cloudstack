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
package org.apache.cloudstack.api;

import java.util.List;


import org.apache.cloudstack.api.response.ProjectResponse;
import org.apache.cloudstack.api.response.SuccessResponse;
import org.apache.cloudstack.api.response.TemplateResponse;

import com.cloud.exception.InvalidParameterValueException;

public abstract class BaseUpdateTemplateOrIsoPermissionsCmd extends BaseCmd {
    protected String _name = getResponseName();

    // ///////////////////////////////////////////////////
    // ////////////// API parameters /////////////////////
    // ///////////////////////////////////////////////////


    protected String getResponseName() {
        return "updatetemplateorisopermissionsresponse";
    }

    @Parameter(name = ApiConstants.ACCOUNTS,
               type = CommandType.LIST,
               collectionType = CommandType.STRING,
               description = "a comma delimited list of accounts within caller's domain. If specified, \"op\" parameter has to be passed in.")
    private List<String> accountNames;

    @Parameter(name = ApiConstants.ID, type = CommandType.UUID, entityType = TemplateResponse.class, required = true, description = "the template ID")
    private Long id;

    @Parameter(name = ApiConstants.IS_FEATURED, type = CommandType.BOOLEAN, description = "true for featured template/iso, false otherwise")
    private Boolean featured;

    @Parameter(name = ApiConstants.IS_PUBLIC, type = CommandType.BOOLEAN, description = "true for public template/iso, false for private templates/isos")
    private Boolean isPublic;

    @Parameter(name = ApiConstants.IS_EXTRACTABLE,
               type = CommandType.BOOLEAN,
               description = "true if the template/iso is extractable, false other wise. Can be set only by root admin")
    private Boolean isExtractable;

    @Parameter(name = ApiConstants.OP, type = CommandType.STRING, description = "permission operator (add, remove, reset)")
    private String operation;

    @Parameter(name = ApiConstants.PROJECT_IDS,
               type = CommandType.LIST,
               collectionType = CommandType.UUID,
               entityType = ProjectResponse.class,
               description = "a comma delimited list of projects. If specified, \"op\" parameter has to be passed in.")
    private List<Long> projectIds;

    // ///////////////////////////////////////////////////
    // ///////////////// Accessors ///////////////////////
    // ///////////////////////////////////////////////////

    public List<String> getAccountNames() {
        if (accountNames != null && projectIds != null) {
            throw new InvalidParameterValueException("Accounts and projectIds can't be specified together");
        }
        return accountNames;
    }

    public Long getId() {
        return id;
    }

    public Boolean isFeatured() {
        return featured;
    }

    public Boolean isPublic() {
        return isPublic;
    }

    public Boolean isExtractable() {
        return isExtractable;
    }

    public String getOperation() {
        return operation;
    }

    public List<Long> getProjectIds() {
        if (accountNames != null && projectIds != null) {
            throw new InvalidParameterValueException("Accounts and projectIds can't be specified together");
        }
        return projectIds;
    }

    // ///////////////////////////////////////////////////
    // ///////////// API Implementation///////////////////
    // ///////////////////////////////////////////////////

    @Override
    public String getCommandName() {
        return _name;
    }

    @Override
    public void execute() {
        boolean result = _templateService.updateTemplateOrIsoPermissions(this);
        if (result) {
            SuccessResponse response = new SuccessResponse(getCommandName());
            setResponseObject(response);
        } else {
            throw new ServerApiException(ApiErrorCode.INTERNAL_ERROR, "Failed to update template/iso permissions");
        }
    }
}
