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
//
package org.apache.cloudstack.api;

import javax.inject.Inject;


import org.apache.cloudstack.api.response.ListResponse;
import org.apache.cloudstack.api.response.UcsManagerResponse;
import org.apache.cloudstack.api.response.UcsProfileResponse;

import com.cloud.exception.ConcurrentOperationException;
import com.cloud.exception.InsufficientCapacityException;
import com.cloud.exception.NetworkRuleConflictException;
import com.cloud.exception.ResourceAllocationException;
import com.cloud.exception.ResourceUnavailableException;
import com.cloud.ucs.manager.UcsManager;
import com.cloud.user.Account;

@APICommand(name = "listUcsProfiles", description = "List profile in ucs manager", responseObject = UcsProfileResponse.class,
        requestHasSensitiveInfo = false, responseHasSensitiveInfo = false)
public class ListUcsProfileCmd extends BaseListCmd {

    @Inject
    UcsManager mgr;

    @Parameter(name = ApiConstants.UCS_MANAGER_ID,
               type = CommandType.UUID,
               entityType = UcsManagerResponse.class,
               description = "the id for the ucs manager",
               required = true)
    private Long ucsManagerId;

    public Long getUcsManagerId() {
        return ucsManagerId;
    }

    public void setUcsManagerId(Long ucsManagerId) {
        this.ucsManagerId = ucsManagerId;
    }

    @Override
    public void execute() throws ResourceUnavailableException, InsufficientCapacityException, ServerApiException, ConcurrentOperationException,
        ResourceAllocationException, NetworkRuleConflictException {
        try {
            ListResponse<UcsProfileResponse> response = mgr.listUcsProfiles(this);
            response.setResponseName(getCommandName());
            response.setObjectName("ucsprofiles");
            this.setResponseObject(response);
        } catch (Exception e) {
            logger.warn("Exception: ", e);
            throw new ServerApiException(ApiErrorCode.INTERNAL_ERROR, e.getMessage());
        }
    }

    @Override
    public String getCommandName() {
        return "listucsprofileresponse";
    }

    @Override
    public long getEntityOwnerId() {
        return Account.ACCOUNT_ID_SYSTEM;
    }

}
