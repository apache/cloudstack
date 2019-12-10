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

package org.apache.cloudstack.api.command.admin.router;

import java.util.List;

import org.apache.cloudstack.api.APICommand;
import org.apache.cloudstack.api.ApiConstants;
import org.apache.cloudstack.api.ApiErrorCode;
import org.apache.cloudstack.api.BaseCmd;
import org.apache.cloudstack.api.BaseListCmd;
import org.apache.cloudstack.api.Parameter;
import org.apache.cloudstack.api.ServerApiException;
import org.apache.cloudstack.api.response.DomainRouterResponse;
import org.apache.cloudstack.api.response.ListResponse;
import org.apache.cloudstack.api.response.RouterHealthCheckResultsResponse;
import org.apache.cloudstack.context.CallContext;
import org.apache.log4j.Logger;

import com.cloud.exception.InvalidParameterValueException;
import com.cloud.exception.ResourceUnavailableException;
import com.cloud.network.RouterHealthCheckResult;
import com.cloud.network.router.VirtualRouter;
import com.cloud.user.Account;
import com.cloud.vm.VirtualMachine;

@APICommand(name = ListRouterHealthCheckResultsCmd.APINAME,
        responseObject = RouterHealthCheckResultsResponse.class,
        description = "Starts a router.",
        entityType = {VirtualMachine.class},
        requestHasSensitiveInfo = false,
        responseHasSensitiveInfo = false,
        since = "4.13.1")
public class ListRouterHealthCheckResultsCmd extends BaseListCmd {
    public static final Logger s_logger = Logger.getLogger(ListRouterHealthCheckResultsCmd.class.getName());
    public static final String APINAME = "listRouterHealthCheckResults";

    /////////////////////////////////////////////////////
    //////////////// API parameters /////////////////////
    /////////////////////////////////////////////////////

    @Parameter(name = ApiConstants.ID, type = CommandType.UUID, entityType = DomainRouterResponse.class,
            required = true, description = "the ID of the router")
    private Long id;

    @Parameter(name = "performfreshchecks", type = CommandType.BOOLEAN, description = "if true is passed for this parameter, " +
            "health checks are performed on the fly. Else last performed checks data is fetched")
    private Boolean performFreshChecks;

    /////////////////////////////////////////////////////
    /////////////////// Accessors ///////////////////////
    /////////////////////////////////////////////////////

    public Long getId() {
        return id;
    }

    public boolean shouldPerformFreshChecks() {
        return performFreshChecks == null ? false : performFreshChecks.booleanValue();
    }

    /////////////////////////////////////////////////////
    /////////////// API Implementation///////////////////
    /////////////////////////////////////////////////////

    @Override
    public String getCommandName() {
        return APINAME.toLowerCase() + BaseCmd.RESPONSE_SUFFIX;
    }

    @Override
    public long getEntityOwnerId() {
        VirtualRouter router = _entityMgr.findById(VirtualRouter.class, getId());
        if (router != null) {
            return router.getAccountId();
        }

        return Account.ACCOUNT_ID_SYSTEM;
    }

    @Override
    public void execute() throws ResourceUnavailableException, InvalidParameterValueException, ServerApiException {
        CallContext.current().setEventDetails("Router Id: " + this._uuidMgr.getUuid(VirtualMachine.class, getId()));
        VirtualRouter router = _routerService.findRouter(getId());
        if (router == null || router.getRole() != VirtualRouter.Role.VIRTUAL_ROUTER) {
            throw new InvalidParameterValueException("Can't find router by id");
        }

        List<RouterHealthCheckResult> result = _routerService.fetchRouterHealthCheckResults(getId(), shouldPerformFreshChecks());
        if (result != null) {
            List<RouterHealthCheckResultsResponse> healthChecks = _responseGenerator.createHealthCheckResponse(router, result);
            ListResponse<RouterHealthCheckResultsResponse> routerResponse = new ListResponse<>();
            routerResponse.setResponses(healthChecks);
            routerResponse.setObjectName("routerhealthchecks");
            routerResponse.setResponseName(getCommandName());
            setResponseObject(routerResponse);
        } else {
            throw new ServerApiException(ApiErrorCode.INTERNAL_ERROR, "Failed to start router");
        }
    }
}
