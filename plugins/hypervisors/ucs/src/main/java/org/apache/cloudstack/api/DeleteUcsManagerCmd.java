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

import javax.inject.Inject;


import org.apache.cloudstack.api.response.SuccessResponse;
import org.apache.cloudstack.api.response.UcsManagerResponse;

import com.cloud.exception.ConcurrentOperationException;
import com.cloud.exception.InsufficientCapacityException;
import com.cloud.exception.NetworkRuleConflictException;
import com.cloud.exception.ResourceAllocationException;
import com.cloud.exception.ResourceUnavailableException;
import com.cloud.ucs.manager.UcsManager;
import com.cloud.user.Account;
import com.cloud.utils.exception.CloudRuntimeException;

@APICommand(name = "deleteUcsManager", description = "Delete a Ucs manager", responseObject = SuccessResponse.class,
        requestHasSensitiveInfo = false, responseHasSensitiveInfo = false)
public class DeleteUcsManagerCmd extends BaseCmd {

    @Inject
    private UcsManager mgr;

    @Parameter(name = ApiConstants.UCS_MANAGER_ID,
               type = BaseCmd.CommandType.UUID,
               description = "ucs manager id",
               entityType = UcsManagerResponse.class,
               required = true)
    private Long ucsManagerId;

    public Long getUcsManagerId() {
        return ucsManagerId;
    }

    @Override
    public void execute() throws ResourceUnavailableException, InsufficientCapacityException, ServerApiException, ConcurrentOperationException,
        ResourceAllocationException, NetworkRuleConflictException {
        try {
            mgr.deleteUcsManager(ucsManagerId);
            SuccessResponse rsp = new SuccessResponse();
            rsp.setResponseName(getCommandName());
            rsp.setObjectName("success");
            this.setResponseObject(rsp);
        } catch (Exception e) {
            logger.debug(e.getMessage(), e);
            throw new CloudRuntimeException(e);
        }
    }

    @Override
    public String getCommandName() {
        return "deleteUcsManagerResponse";
    }

    @Override
    public long getEntityOwnerId() {
        return Account.ACCOUNT_ID_SYSTEM;
    }
}
