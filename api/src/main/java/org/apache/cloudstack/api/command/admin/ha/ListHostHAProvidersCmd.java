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

package org.apache.cloudstack.api.command.admin.ha;

import com.cloud.exception.ConcurrentOperationException;
import com.cloud.exception.InsufficientCapacityException;
import com.cloud.exception.NetworkRuleConflictException;
import com.cloud.exception.ResourceAllocationException;
import com.cloud.exception.ResourceUnavailableException;
import com.cloud.user.Account;
import com.google.common.base.Enums;
import org.apache.cloudstack.acl.RoleType;
import org.apache.cloudstack.api.APICommand;
import org.apache.cloudstack.api.ApiConstants;
import org.apache.cloudstack.api.ApiErrorCode;
import org.apache.cloudstack.api.BaseCmd;
import org.apache.cloudstack.api.Parameter;
import org.apache.cloudstack.api.ServerApiException;
import org.apache.cloudstack.api.response.HAProviderResponse;
import org.apache.cloudstack.api.response.HostHAResponse;
import org.apache.cloudstack.api.response.ListResponse;
import org.apache.cloudstack.ha.HAConfigManager;
import org.apache.cloudstack.ha.HAResource;

import javax.inject.Inject;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

@APICommand(name = "listHostHAProviders", description = "Lists HA providers", responseObject = HostHAResponse.class,
        requestHasSensitiveInfo = false, responseHasSensitiveInfo = false,
        since = "4.11", authorized = {RoleType.Admin})
public final class ListHostHAProvidersCmd extends BaseCmd {

    @Inject
    private HAConfigManager haConfigManager;

    /////////////////////////////////////////////////////
    //////////////// API parameters /////////////////////
    /////////////////////////////////////////////////////

    @Parameter(name = ApiConstants.HYPERVISOR, type = CommandType.STRING, required = true,
            description = "Hypervisor type of the resource")
    private String hypervisorType;

    /////////////////////////////////////////////////////
    /////////////////// Accessors ///////////////////////
    /////////////////////////////////////////////////////

    public HAResource.ResourceSubType getHypervisorType() {
        return HAResource.ResourceSubType.valueOf(hypervisorType);
    }

    /////////////////////////////////////////////////////
    /////////////// API Implementation///////////////////
    /////////////////////////////////////////////////////

    @Override
    public long getEntityOwnerId() {
        return Account.ACCOUNT_ID_SYSTEM;
    }

    private void setupResponse(final List<String> hostHAProviderList) {
        final ListResponse<HAProviderResponse> response = new ListResponse<>();
        final List<HAProviderResponse> hostHAResponses = new ArrayList<>();
        for (final String provider : hostHAProviderList) {
            final HAProviderResponse haProviderResponse = new HAProviderResponse();
            haProviderResponse.setProvider(provider);
            hostHAResponses.add(haProviderResponse);
        }
        response.setResponses(hostHAResponses);
        response.setResponseName(getCommandName());
        setResponseObject(response);
    }

    @Override
    public void execute() throws ResourceUnavailableException, InsufficientCapacityException, ServerApiException, ConcurrentOperationException, ResourceAllocationException, NetworkRuleConflictException {
        if (!Enums.getIfPresent(HAResource.ResourceSubType.class, hypervisorType).isPresent()) {
            throw new ServerApiException(ApiErrorCode.PARAM_ERROR, "Invalid or unsupported host hypervisor type provided. Supported types are: " + Arrays.toString(HAResource.ResourceSubType.values()));
        }
        final List<String> hostHAProviders = haConfigManager.listHAProviders(HAResource.ResourceType.Host, getHypervisorType());
        setupResponse(hostHAProviders);
    }
}
