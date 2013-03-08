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

import javax.inject.Inject;

import org.apache.cloudstack.api.APICommand;
import org.apache.cloudstack.api.ApiConstants;
import org.apache.cloudstack.api.ApiErrorCode;
import org.apache.cloudstack.api.BaseAsyncCmd;
import org.apache.cloudstack.api.Parameter;
import org.apache.cloudstack.api.ServerApiException;
import org.apache.cloudstack.api.response.DomainResponse;
import org.apache.cloudstack.api.response.SuccessResponse;
import org.apache.cloudstack.region.RegionService;
import org.apache.log4j.Logger;

import com.cloud.domain.Domain;
import com.cloud.event.EventTypes;
import com.cloud.user.Account;
import com.cloud.user.UserContext;

@APICommand(name = "deleteDomain", description="Deletes a specified domain", responseObject=SuccessResponse.class)
public class DeleteDomainCmd extends BaseAsyncCmd {
    public static final Logger s_logger = Logger.getLogger(DeleteDomainCmd.class.getName());
    private static final String s_name = "deletedomainresponse";

    /////////////////////////////////////////////////////
    //////////////// API parameters /////////////////////
    /////////////////////////////////////////////////////

    @Parameter(name=ApiConstants.ID, type=CommandType.UUID, entityType=DomainResponse.class,
            required=true, description="ID of domain to delete")
    private Long id;

    @Parameter(name=ApiConstants.CLEANUP, type=CommandType.BOOLEAN, description="true if all domain resources (child domains, accounts) have to be cleaned up, false otherwise")
    private Boolean cleanup;

    @Inject RegionService _regionService;

    /////////////////////////////////////////////////////
    /////////////////// Accessors ///////////////////////
    /////////////////////////////////////////////////////

    public Long getId() {
        return id;
    }

    public Boolean getCleanup() {
        return cleanup;
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
        Domain domain = _entityMgr.findById(Domain.class, getId());
        if (domain != null) {
            return domain.getAccountId();
        }

        return Account.ACCOUNT_ID_SYSTEM; // no account info given, parent this command to SYSTEM so ERROR events are tracked
    }

    @Override
    public String getEventType() {
        return EventTypes.EVENT_DOMAIN_DELETE;
    }

    @Override
    public String getEventDescription() {
        return  "deleting domain: " + getId();
    }

    @Override
    public void execute(){
        UserContext.current().setEventDetails("Domain Id: "+getId());
        boolean result = _regionService.deleteDomain(this);
        if (result) {
            SuccessResponse response = new SuccessResponse(getCommandName());
            this.setResponseObject(response);
        } else {
            throw new ServerApiException(ApiErrorCode.INTERNAL_ERROR, "Failed to delete domain");
        }
    }
}
