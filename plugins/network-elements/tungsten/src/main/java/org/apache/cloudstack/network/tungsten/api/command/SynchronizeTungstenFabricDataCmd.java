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
package org.apache.cloudstack.network.tungsten.api.command;

import com.cloud.exception.ConcurrentOperationException;
import com.cloud.exception.InsufficientCapacityException;
import com.cloud.exception.NetworkRuleConflictException;
import com.cloud.exception.ResourceAllocationException;
import com.cloud.exception.ResourceUnavailableException;
import com.cloud.user.Account;
import org.apache.cloudstack.api.APICommand;
import org.apache.cloudstack.api.ApiConstants;
import org.apache.cloudstack.api.ApiErrorCode;
import org.apache.cloudstack.api.BaseCmd;
import org.apache.cloudstack.api.Parameter;
import org.apache.cloudstack.api.ServerApiException;
import org.apache.cloudstack.api.response.SuccessResponse;
import org.apache.cloudstack.network.tungsten.api.response.TungstenFabricProviderResponse;
import org.apache.cloudstack.network.tungsten.service.TungstenService;
import org.apache.log4j.Logger;

import javax.inject.Inject;

@APICommand(name = SynchronizeTungstenFabricDataCmd.APINAME, description = "Synchronize Tungsten-Fabric data",
    responseObject = SuccessResponse.class, requestHasSensitiveInfo = false, responseHasSensitiveInfo = false)
public class SynchronizeTungstenFabricDataCmd extends BaseCmd {
    public static final Logger s_logger = Logger.getLogger(SynchronizeTungstenFabricDataCmd.class.getName());
    public static final String APINAME = "synchronizeTungstenFabricData";

    @Parameter(name = ApiConstants.ID, type = CommandType.UUID, entityType = TungstenFabricProviderResponse.class,
        required = true, description = "provider id")
    private Long tungstenProviderId;

    @Inject
    TungstenService tungstenService;

    @Override
    public void execute() throws ResourceUnavailableException, InsufficientCapacityException, ServerApiException,
        ConcurrentOperationException, ResourceAllocationException, NetworkRuleConflictException {
        boolean result = tungstenService.synchronizeTungstenData(tungstenProviderId);
        if (result) {
            SuccessResponse successResponse = new SuccessResponse(getCommandName());
            setResponseObject(successResponse);
        } else {
            throw new ServerApiException(ApiErrorCode.INTERNAL_ERROR, "Unable to synchronize Tungsten-Fabric data");
        }
    }

    @Override
    public String getCommandName() {
        return APINAME.toLowerCase() + BaseCmd.RESPONSE_SUFFIX;
    }

    @Override
    public long getEntityOwnerId() {
        return Account.ACCOUNT_ID_SYSTEM;
    }

    public Long getTungstenProviderId() {
        return tungstenProviderId;
    }

    public void setTungstenProviderId(final Long tungstenProviderId) {
        this.tungstenProviderId = tungstenProviderId;
    }
}
