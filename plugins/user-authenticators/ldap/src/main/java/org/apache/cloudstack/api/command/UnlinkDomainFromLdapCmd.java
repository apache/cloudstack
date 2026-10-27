/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.cloudstack.api.command;

import com.cloud.exception.ConcurrentOperationException;
import com.cloud.exception.InsufficientCapacityException;
import com.cloud.exception.NetworkRuleConflictException;
import com.cloud.exception.ResourceAllocationException;
import com.cloud.exception.ResourceUnavailableException;
import com.cloud.user.Account;
import org.apache.cloudstack.api.APICommand;
import org.apache.cloudstack.api.ApiConstants;
import org.apache.cloudstack.api.BaseCmd;
import org.apache.cloudstack.api.Parameter;
import org.apache.cloudstack.api.ServerApiException;
import org.apache.cloudstack.api.response.SuccessResponse;
import org.apache.cloudstack.api.response.DomainResponse;
import org.apache.cloudstack.ldap.LdapManager;

import javax.inject.Inject;

@APICommand(name = "unlinkDomainFromLdap", description = "remove the linkage of a Domain to a group or OU in ldap",
        responseObject = SuccessResponse.class, since = "4.23.0", requestHasSensitiveInfo = false, responseHasSensitiveInfo = false)
public class UnlinkDomainFromLdapCmd extends BaseCmd {
    @Parameter(name = ApiConstants.DOMAIN_ID, type = CommandType.UUID, required = true, entityType = DomainResponse.class,
            description = "The ID of the Domain which has to be unlinked from LDAP.")
    private Long domainId;

    @Inject
    private LdapManager _ldapManager;

    public Long getDomainId() {
        return domainId;
    }

    @Override
    public void execute() throws ResourceUnavailableException, InsufficientCapacityException, ServerApiException, ConcurrentOperationException, ResourceAllocationException, NetworkRuleConflictException {
        boolean rc = _ldapManager.unlinkDomainFromLdap(this);
        SuccessResponse response = new SuccessResponse(getCommandName());
        response.setSuccess(rc);
        if (rc) {
            response.setDisplayText("Domain unlinked from LDAP successfully");
        } else {
            response.setDisplayText("Failed to unlink domain from LDAP");
        }
        setResponseObject(response);
    }

    @Override
    public long getEntityOwnerId() {
        return Account.ACCOUNT_ID_SYSTEM;
    }
}
