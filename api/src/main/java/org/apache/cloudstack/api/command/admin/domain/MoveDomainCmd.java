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
package org.apache.cloudstack.api.command.admin.domain;

import com.cloud.domain.Domain;
import com.cloud.exception.ResourceAllocationException;
import com.cloud.user.Account;
import org.apache.cloudstack.acl.RoleType;
import org.apache.cloudstack.api.APICommand;
import org.apache.cloudstack.api.ApiConstants;
import org.apache.cloudstack.api.ApiErrorCode;
import org.apache.cloudstack.api.BaseCmd;
import org.apache.cloudstack.api.Parameter;
import org.apache.cloudstack.api.ServerApiException;
import org.apache.cloudstack.api.response.DomainResponse;

@APICommand(name = "moveDomain", description = "Moves a domain and its children to a new parent domain.", since = "4.19.0.0", responseObject = DomainResponse.class,
 requestHasSensitiveInfo = false, responseHasSensitiveInfo = false, authorized = {RoleType.Admin})
public class MoveDomainCmd extends BaseCmd {

    private static final String APINAME = "moveDomain";
    @Parameter(name = ApiConstants.DOMAIN_ID, type = CommandType.UUID, required = true, entityType = DomainResponse.class, description = "The ID of the domain to be moved.")
    private Long domainId;

    @Parameter(name = ApiConstants.PARENT_DOMAIN_ID, type = CommandType.UUID, required = true, entityType = DomainResponse.class,
            description = "The ID of the new parent domain of the domain to be moved.")
    private Long parentDomainId;

    public Long getDomainId() {
        return domainId;
    }

    public Long getParentDomainId() {
        return parentDomainId;
    }

    @Override
    public String getCommandName() {
        return APINAME.toLowerCase() + BaseCmd.RESPONSE_SUFFIX;
    }

    @Override
    public long getEntityOwnerId() {
        return Account.ACCOUNT_ID_SYSTEM;
    }

    @Override
    public void execute() throws ResourceAllocationException {
        Domain domain = _domainService.moveDomainAndChildrenToNewParentDomain(this);

        if (domain != null) {
            DomainResponse response = _responseGenerator.createDomainResponse(domain);
            response.setResponseName(getCommandName());
            this.setResponseObject(response);
        } else {
            throw new ServerApiException(ApiErrorCode.INTERNAL_ERROR, "Failed to move the domain.");
        }
    }
}
