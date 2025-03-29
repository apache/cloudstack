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

package com.cloud.hypervisor.external.provisioner.api;

import com.cloud.agent.api.RunCustomActionAnswer;
import com.cloud.agent.manager.ExternalAgentManager;
import com.cloud.event.EventTypes;
import com.cloud.exception.ConcurrentOperationException;
import com.cloud.exception.InsufficientCapacityException;
import com.cloud.exception.NetworkRuleConflictException;
import com.cloud.exception.ResourceAllocationException;
import com.cloud.exception.ResourceUnavailableException;
import com.cloud.user.Account;
import com.cloud.vm.VmDetailConstants;
import org.apache.cloudstack.extension.CustomActionResponse;
import org.apache.cloudstack.api.APICommand;
import org.apache.cloudstack.api.ApiConstants;
import org.apache.cloudstack.api.ApiErrorCode;
import org.apache.cloudstack.api.BaseAsyncCmd;
import org.apache.cloudstack.api.Parameter;
import org.apache.cloudstack.api.ServerApiException;

import javax.inject.Inject;
import java.util.HashMap;
import java.util.Map;

@APICommand(name = RunCustomActionCmd.APINAME, description = "Run the custom action",
        responseObject = CustomActionResponse.class, responseHasSensitiveInfo = false, since = "4.21.0")
public class RunCustomActionCmd extends BaseAsyncCmd {

    public static final String APINAME = "runCustomAction";

    @Inject
    ExternalAgentManager _externalMgr;

    /////////////////////////////////////////////////////
    //////////////// API parameters /////////////////////
    /////////////////////////////////////////////////////

    @Parameter(name = ApiConstants.NAME, type = CommandType.STRING, required = true,
            description = "Name of the custom action")
    private String actionName;

    @Parameter(name = ApiConstants.EXTENSION_ID, type = CommandType.UUID, required = true,
            entityType = ExtensionResponse.class, description = "the extension id used to call the custom action")
    private Long extensionId;

    @Parameter(name = ApiConstants.RESOURCE_ID, type = CommandType.STRING, required = true,
            description = "UUID of the resource to register the extension with")
    private String resourceId;

    @Parameter(name = ApiConstants.RESOURCE_TYPE, type = CommandType.STRING, required = true,
            description = "Type of the resource")
    private String resourceType;

    @Parameter(name = ApiConstants.EXTERNAL_DETAILS, type = CommandType.MAP, description = "Details in key/value pairs using format externaldetails[i].keyname=keyvalue. Example: externaldetails[0].endpoint.url=urlvalue", since = "4.21.0")
    protected Map externalDetails;

    /////////////////////////////////////////////////////
    /////////////////// Accessors ///////////////////////
    /////////////////////////////////////////////////////

    public String getActionName() {
        return actionName;
    }


    public Long getExtensionId() {
        return extensionId;
    }

    public String getResourceId() {
        return resourceId;
    }

    public String getResourceType() {
        return resourceType;
    }

    public Map<String, String> getExternalDetails() {
        Map<String, String> customparameterMap = convertDetailsToMap(externalDetails);
        Map<String, String> details = new HashMap<>();
        for (String key : customparameterMap.keySet()) {
            String value = customparameterMap.get(key);
            details.put(VmDetailConstants.EXTERNAL_DETAIL_PREFIX + key, value);
        }
        return details;
    }

    /////////////////////////////////////////////////////
    /////////////// API Implementation///////////////////
    /////////////////////////////////////////////////////

    @Override
    public void execute() throws ResourceUnavailableException, InsufficientCapacityException, ServerApiException, ConcurrentOperationException, ResourceAllocationException, NetworkRuleConflictException {
        RunCustomActionAnswer answer = _externalMgr.runCustomAction(this);
        if (answer.getResult()) {
            CustomActionResponse response = new CustomActionResponse();
            response.setActionName(this.actionName);
            response.setDetails(answer.getRunDetails());
            response.setResponseName(getCommandName());
            response.setObjectName("CustomAction");
            setResponseObject(response);
        } else {
            throw new ServerApiException(ApiErrorCode.INTERNAL_ERROR, "Failed to run custom action");
        }
    }

    @Override
    public long getEntityOwnerId() {
        return Account.ACCOUNT_ID_SYSTEM;
    }

    @Override
    public String getEventType() {
        return EventTypes.EVENT_CUSTOM_ACTION;
    }

    @Override
    public String getEventDescription() {
        return "Running custom action";
    }
}
