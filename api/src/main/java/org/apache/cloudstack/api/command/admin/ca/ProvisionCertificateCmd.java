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

package org.apache.cloudstack.api.command.admin.ca;

import javax.inject.Inject;

import org.apache.cloudstack.acl.RoleType;
import org.apache.cloudstack.api.APICommand;
import org.apache.cloudstack.api.ApiCommandResourceType;
import org.apache.cloudstack.api.ApiConstants;
import org.apache.cloudstack.api.ApiErrorCode;
import org.apache.cloudstack.api.BaseAsyncCmd;
import org.apache.cloudstack.api.BaseCmd;
import org.apache.cloudstack.api.Parameter;
import org.apache.cloudstack.api.ServerApiException;
import org.apache.cloudstack.api.response.HostResponse;
import org.apache.cloudstack.api.response.SuccessResponse;
import org.apache.cloudstack.ca.CAManager;
import org.apache.cloudstack.context.CallContext;

import com.cloud.event.EventTypes;
import com.cloud.host.Host;

@APICommand(name = ProvisionCertificateCmd.APINAME,
        description = "Issues and propagates client certificate on a connected host/agent using configured CA plugin",
        responseObject = SuccessResponse.class,
        requestHasSensitiveInfo = false,
        responseHasSensitiveInfo = false,
        since = "4.11.0",
        authorized = {RoleType.Admin})
public class ProvisionCertificateCmd extends BaseAsyncCmd {
    public static final String APINAME = "provisionCertificate";

    @Inject
    private CAManager caManager;

    /////////////////////////////////////////////////////
    //////////////// API parameters /////////////////////
    /////////////////////////////////////////////////////

    @Parameter(name = ApiConstants.HOST_ID, type = CommandType.UUID, required = true, entityType = HostResponse.class,
            description = "The host/agent uuid to which the certificate has to be provisioned (issued and propagated)")
    private Long hostId;

    @Parameter(name = ApiConstants.RECONNECT, type = CommandType.BOOLEAN,
            description = "Whether to attempt reconnection with host/agent after successful deployment of certificate. When option is not provided, configured global setting is used")
    private Boolean reconnect;

    @Parameter(name = ApiConstants.PROVIDER, type = CommandType.STRING,
            description = "Name of the CA service provider, otherwise the default configured provider plugin will be used")
    private String provider;

    /////////////////////////////////////////////////////
    /////////////////// Accessors ///////////////////////
    /////////////////////////////////////////////////////

    public Long getHostId() {
        return hostId;
    }

    public Boolean getReconnect() {
        return reconnect;
    }

    public String getProvider() {
        return provider;
    }

    /////////////////////////////////////////////////////
    /////////////// API Implementation///////////////////
    /////////////////////////////////////////////////////

    @Override
    public void execute() {
        final Host host = _resourceService.getHost(getHostId());
        if (host == null) {
            throw new ServerApiException(ApiErrorCode.PARAM_ERROR, "Unable to find host by ID: " + getHostId());
        }

        boolean result = caManager.provisionCertificate(host, getReconnect(), getProvider());
        SuccessResponse response = new SuccessResponse(getCommandName());
        response.setSuccess(result);
        setResponseObject(response);
    }

    @Override
    public String getCommandName() {
        return APINAME.toLowerCase() + BaseCmd.RESPONSE_SUFFIX;
    }

    @Override
    public long getEntityOwnerId() {
        return CallContext.current().getCallingAccount().getId();
    }

    @Override
    public String getEventType() {
        return EventTypes.EVENT_CA_CERTIFICATE_PROVISION;
    }

    @Override
    public String getEventDescription() {
        return "provisioning certificate for host id=" + hostId + " using provider=" + provider;
    }

    @Override
    public ApiCommandResourceType getApiResourceType() {
        return ApiCommandResourceType.Host;
    }
}
