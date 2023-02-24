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

import javax.inject.Inject;

import org.apache.cloudstack.api.APICommand;
import org.apache.cloudstack.api.ApiCommandResourceType;
import org.apache.cloudstack.api.ApiConstants;
import org.apache.cloudstack.api.ApiErrorCode;
import org.apache.cloudstack.api.BaseAsyncCmd;
import org.apache.cloudstack.api.Parameter;
import org.apache.cloudstack.api.ServerApiException;
import org.apache.cloudstack.api.response.OvsProviderResponse;
import org.apache.cloudstack.context.CallContext;
import org.apache.log4j.Logger;

import com.cloud.event.EventTypes;
import com.cloud.exception.ConcurrentOperationException;
import com.cloud.exception.InsufficientCapacityException;
import com.cloud.exception.ResourceUnavailableException;
import com.cloud.network.OvsProvider;
import com.cloud.network.element.VirtualRouterElementService;
import com.cloud.user.Account;

@APICommand(name = "configureOvsElement", responseObject = OvsProviderResponse.class, description = "Configures an ovs element.",
        requestHasSensitiveInfo = false, responseHasSensitiveInfo = false)
public class ConfigureOvsElementCmd extends BaseAsyncCmd {
    public static final Logger s_logger = Logger
        .getLogger(ConfigureOvsElementCmd.class.getName());
    @Inject
    private List<VirtualRouterElementService> _service;

    // ///////////////////////////////////////////////////
    // ////////////// API parameters /////////////////////
    // ///////////////////////////////////////////////////

    @Parameter(name = ApiConstants.ID, type = CommandType.UUID, entityType = OvsProviderResponse.class, required = true, description = "the ID of the ovs provider")
    private Long id;

    @Parameter(name = ApiConstants.ENABLED, type = CommandType.BOOLEAN, required = true, description = "Enabled/Disabled the service provider")
    private Boolean enabled;

    // ///////////////////////////////////////////////////
    // ///////////////// Accessors ///////////////////////
    // ///////////////////////////////////////////////////
    public void setId(Long id) {
        this.id = id;
    }

    public Long getId() {
        return id;
    }

    public void setEnabled(Boolean enabled) {
        this.enabled = enabled;
    }

    public Boolean getEnabled() {
        return enabled;
    }

    // ///////////////////////////////////////////////////
    // ///////////// API Implementation///////////////////
    // ///////////////////////////////////////////////////

    public static String getResultObjectName() {
        return "boolean";
    }

    @Override
    public long getEntityOwnerId() {
        return Account.ACCOUNT_ID_SYSTEM;
    }

    @Override
    public String getEventType() {
        return EventTypes.EVENT_NETWORK_ELEMENT_CONFIGURE;
    }

    @Override
    public String getEventDescription() {
        return "configuring ovs provider: " + id;
    }

    @Override
    public ApiCommandResourceType getApiResourceType() {
        return ApiCommandResourceType.None;
    }

    @Override
    public Long getApiResourceId() {
        return id;
    }

    @Override
    public void execute() throws ConcurrentOperationException,
        ResourceUnavailableException, InsufficientCapacityException {
        CallContext.current().setEventDetails("Ovs element: " + id);
        OvsProvider result = _service.get(0).configure(this);
        if (result != null) {
            OvsProviderResponse ovsResponse = _responseGenerator
                .createOvsProviderResponse(result);
            ovsResponse.setResponseName(getCommandName());
            this.setResponseObject(ovsResponse);
        } else {
            throw new ServerApiException(ApiErrorCode.INTERNAL_ERROR,
                "Failed to configure the ovs provider");
        }
    }
}
