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
import com.cloud.host.Host;
import com.cloud.user.Account;
import org.apache.cloudstack.acl.RoleType;
import org.apache.cloudstack.api.APICommand;
import org.apache.cloudstack.api.ApiArgValidator;
import org.apache.cloudstack.api.ApiConstants;
import org.apache.cloudstack.api.BaseCmd;
import org.apache.cloudstack.api.Parameter;
import org.apache.cloudstack.api.ServerApiException;
import org.apache.cloudstack.api.response.HostHAResponse;
import org.apache.cloudstack.api.response.HostResponse;
import org.apache.cloudstack.api.response.ListResponse;
import org.apache.cloudstack.ha.HAConfig;
import org.apache.cloudstack.ha.HAConfigManager;
import org.apache.cloudstack.ha.HAResource;

import javax.inject.Inject;
import java.util.ArrayList;
import java.util.List;

@APICommand(name = "listHostHAResources", description = "Lists host HA resources", responseObject = HostHAResponse.class,
        requestHasSensitiveInfo = false, responseHasSensitiveInfo = false,
        since = "4.11", authorized = {RoleType.Admin})
public final class ListHostHAResourcesCmd extends BaseCmd {

    @Inject
    private HAConfigManager haConfigManager;

    /////////////////////////////////////////////////////
    //////////////// API parameters /////////////////////
    /////////////////////////////////////////////////////

    @Parameter(name = ApiConstants.HOST_ID, type = CommandType.UUID, entityType = HostResponse.class,
            description = "List by host ID", validations = {ApiArgValidator.PositiveNumber})
    private Long hostId;

    /////////////////////////////////////////////////////
    /////////////////// Accessors ///////////////////////
    /////////////////////////////////////////////////////

    public Long getHostId() {
        return hostId;
    }

    /////////////////////////////////////////////////////
    /////////////// API Implementation///////////////////
    /////////////////////////////////////////////////////

    @Override
    public long getEntityOwnerId() {
        return Account.ACCOUNT_ID_SYSTEM;
    }

    private void setupResponse(final List<HAConfig> hostHAConfigList) {
        final ListResponse<HostHAResponse> response = new ListResponse<>();
        final List<HostHAResponse> hostHAResponses = new ArrayList<>();
        for (final HAConfig config : hostHAConfigList) {
            final Host host = _resourceService.getHost(config.getResourceId());
            if (host == null) {
                continue;
            }
            final HostHAResponse hostHAResponse = new HostHAResponse();
            hostHAResponse.setId(host.getUuid());
            hostHAResponse.setEnabled(config.isEnabled());
            hostHAResponse.setHaState(config.getState());
            hostHAResponse.setProvider(config.getHaProvider());
            hostHAResponses.add(hostHAResponse);
        }
        response.setResponses(hostHAResponses);
        response.setResponseName(getCommandName());
        setResponseObject(response);
    }

    @Override
    public void execute() throws ResourceUnavailableException, InsufficientCapacityException, ServerApiException, ConcurrentOperationException, ResourceAllocationException, NetworkRuleConflictException {
        final List<HAConfig> hostHAConfig = haConfigManager.listHAResources(getHostId(), HAResource.ResourceType.Host);
        setupResponse(hostHAConfig);
    }
}
