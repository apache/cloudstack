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
package org.apache.cloudstack.api.command.admin.outofbandmanagement;

import com.cloud.exception.ConcurrentOperationException;
import com.cloud.exception.InsufficientCapacityException;
import com.cloud.exception.NetworkRuleConflictException;
import com.cloud.exception.ResourceAllocationException;
import com.cloud.exception.ResourceUnavailableException;
import com.cloud.host.Host;
import com.google.common.collect.ImmutableMap;
import org.apache.cloudstack.acl.RoleType;
import org.apache.cloudstack.api.APICommand;
import org.apache.cloudstack.api.ApiArgValidator;
import org.apache.cloudstack.api.ApiCommandResourceType;
import org.apache.cloudstack.api.ApiConstants;
import org.apache.cloudstack.api.ApiErrorCode;
import org.apache.cloudstack.api.BaseCmd;
import org.apache.cloudstack.api.Parameter;
import org.apache.cloudstack.api.ServerApiException;
import org.apache.cloudstack.api.response.HostResponse;
import org.apache.cloudstack.api.response.OutOfBandManagementResponse;
import org.apache.cloudstack.context.CallContext;
import org.apache.cloudstack.outofbandmanagement.OutOfBandManagement;
import org.apache.cloudstack.outofbandmanagement.OutOfBandManagementService;
import org.apache.commons.lang3.StringUtils;

import javax.inject.Inject;

@APICommand(name = "configureOutOfBandManagement", description = "Configures a host's out-of-band management interface",
        responseObject = OutOfBandManagementResponse.class, requestHasSensitiveInfo = true, responseHasSensitiveInfo = false,
        since = "4.9.0", authorized = {RoleType.Admin})
public class ConfigureOutOfBandManagementCmd extends BaseCmd {

    @Inject
    private OutOfBandManagementService outOfBandManagementService;

    /////////////////////////////////////////////////////
    //////////////// API parameters /////////////////////
    /////////////////////////////////////////////////////

    @Parameter(name = ApiConstants.HOST_ID, type = CommandType.UUID, entityType = HostResponse.class, required = true,
            validations = {ApiArgValidator.PositiveNumber}, description = "the ID of the host")
    private Long hostId;

    @Parameter(name = ApiConstants.DRIVER, type = CommandType.STRING, required = true, description = "the host management interface driver, for example: ipmitool")
    private String driver;

    @Parameter(name = ApiConstants.ADDRESS, type = CommandType.STRING, required = true, description = "the host management interface IP address")
    private String address;

    @Parameter(name = ApiConstants.PORT, type = CommandType.STRING, required = true, description = "the host management interface port")
    private String port;

    @Parameter(name = ApiConstants.USERNAME, type = CommandType.STRING, required = true, description = "the host management interface user")
    private String username;

    @Parameter(name = ApiConstants.PASSWORD, type = CommandType.STRING, required = true, description = "the host management interface password")
    private String password;

    /////////////////////////////////////////////////////
    /////////////// API Implementation///////////////////
    /////////////////////////////////////////////////////

    @Override
    public void execute() throws ResourceUnavailableException, InsufficientCapacityException, ServerApiException, ConcurrentOperationException, ResourceAllocationException, NetworkRuleConflictException {
        final Host host = _resourceService.getHost(getHostId());
        if (host == null) {
            throw new ServerApiException(ApiErrorCode.PARAM_ERROR, "Unable to find host by ID: " + getHostId());
        }
        CallContext.current().putContextParameter(Host.class, host.getUuid());
        final OutOfBandManagementResponse response = outOfBandManagementService.configure(host, getHostPMOptions());
        response.setId(host.getUuid());
        response.setResponseName(getCommandName());
        setResponseObject(response);
    }

    @Override
    public long getEntityOwnerId() {
        return CallContext.current().getCallingAccountId();
    }

    public Long getHostId() {
        return hostId;
    }

    public final ImmutableMap<OutOfBandManagement.Option, String> getHostPMOptions() {
        final ImmutableMap.Builder<OutOfBandManagement.Option, String> builder = ImmutableMap.builder();
        putOptionIfIsNotEmpty(builder, OutOfBandManagement.Option.DRIVER, driver);
        putOptionIfIsNotEmpty(builder, OutOfBandManagement.Option.ADDRESS, address);
        putOptionIfIsNotEmpty(builder, OutOfBandManagement.Option.PORT, port);
        putOptionIfIsNotEmpty(builder, OutOfBandManagement.Option.USERNAME, username);
        putOptionIfIsNotEmpty(builder, OutOfBandManagement.Option.PASSWORD, password);
        return builder.build();
    }

    protected void putOptionIfIsNotEmpty(ImmutableMap.Builder<OutOfBandManagement.Option, String> builder, OutOfBandManagement.Option option, String value) {
        if (StringUtils.isNotEmpty(value)) {
            builder.put(option, value);
        }
    }

    @Override
    public Long getApiResourceId() {
        return getHostId();
    }

    @Override
    public ApiCommandResourceType getApiResourceType() {
        return ApiCommandResourceType.Host;
    }
}
