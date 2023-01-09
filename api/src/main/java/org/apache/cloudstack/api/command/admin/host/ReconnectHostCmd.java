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
package org.apache.cloudstack.api.command.admin.host;

import org.apache.cloudstack.api.APICommand;
import org.apache.cloudstack.api.ApiCommandResourceType;
import org.apache.cloudstack.api.ApiConstants;
import org.apache.cloudstack.api.ApiErrorCode;
import org.apache.cloudstack.api.BaseAsyncCmd;
import org.apache.cloudstack.api.Parameter;
import org.apache.cloudstack.api.ServerApiException;
import org.apache.cloudstack.api.response.HostResponse;
import org.apache.cloudstack.context.CallContext;
import org.apache.log4j.Logger;

import com.cloud.event.EventTypes;
import com.cloud.exception.AgentUnavailableException;
import com.cloud.exception.InvalidParameterValueException;
import com.cloud.host.Host;
import com.cloud.user.Account;
import com.cloud.utils.exception.CloudRuntimeException;

@APICommand(name = "reconnectHost", description = "Reconnects a host.", responseObject = HostResponse.class, requestHasSensitiveInfo = false, responseHasSensitiveInfo = false)
public class ReconnectHostCmd extends BaseAsyncCmd {
    public static final Logger s_logger = Logger.getLogger(ReconnectHostCmd.class.getName());


    /////////////////////////////////////////////////////
    //////////////// API parameters /////////////////////
    /////////////////////////////////////////////////////

    @Parameter(name = ApiConstants.ID, type = CommandType.UUID, entityType = HostResponse.class, required = true, description = "the host ID")
    private Long id;

    /////////////////////////////////////////////////////
    /////////////////// Accessors ///////////////////////
    /////////////////////////////////////////////////////

    public Long getId() {
        return id;
    }

    /////////////////////////////////////////////////////
    /////////////// API Implementation///////////////////
    /////////////////////////////////////////////////////

    public static String getResultObjectName() {
        return "host";
    }

    @Override
    public long getEntityOwnerId() {
        Account account = CallContext.current().getCallingAccount();
        if (account != null) {
            return account.getId();
        }

        return Account.ACCOUNT_ID_SYSTEM; // no account info given, parent this command to SYSTEM so ERROR events are tracked
    }

    @Override
    public String getEventType() {
        return EventTypes.EVENT_HOST_RECONNECT;
    }

    @Override
    public String getEventDescription() {
        return "reconnecting host: " + getId();
    }

    @Override
    public ApiCommandResourceType getApiResourceType() {
        return ApiCommandResourceType.Host;
    }

    @Override
    public Long getApiResourceId() {
        return getId();
    }

    @Override
    public void execute() {
        try {
            Host result = _resourceService.reconnectHost(this);
            HostResponse response = _responseGenerator.createHostResponse(result);
            response.setResponseName(getCommandName());
            this.setResponseObject(response);
        } catch (InvalidParameterValueException e) {
            throw new ServerApiException(ApiErrorCode.PARAM_ERROR, e.getMessage());
        } catch (CloudRuntimeException e) {
            throw new ServerApiException(ApiErrorCode.INTERNAL_ERROR, e.getMessage());
        } catch (AgentUnavailableException e) {
            throw new ServerApiException(ApiErrorCode.RESOURCE_UNAVAILABLE_ERROR, e.getMessage());
        }
    }
}
