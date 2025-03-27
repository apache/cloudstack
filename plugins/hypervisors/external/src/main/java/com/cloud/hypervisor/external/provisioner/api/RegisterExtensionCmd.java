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

import com.cloud.agent.manager.ExternalAgentManager;
import com.cloud.exception.ConcurrentOperationException;
import com.cloud.exception.InsufficientCapacityException;
import com.cloud.exception.NetworkRuleConflictException;
import com.cloud.exception.ResourceAllocationException;
import com.cloud.exception.ResourceUnavailableException;
import com.cloud.hypervisor.external.provisioner.vo.Extension;
import com.cloud.user.Account;
import com.cloud.vm.VmDetailConstants;
import org.apache.cloudstack.api.APICommand;
import org.apache.cloudstack.api.ApiConstants;
import org.apache.cloudstack.api.BaseCmd;
import org.apache.cloudstack.api.Parameter;
import org.apache.cloudstack.api.ServerApiException;
import org.apache.cloudstack.api.response.PodResponse;
import org.apache.cloudstack.api.response.SuccessResponse;

import javax.inject.Inject;
import java.util.HashMap;
import java.util.Map;

@APICommand(name = RegisterExtensionCmd.APINAME, description = "register the external extension",
        responseObject = SuccessResponse.class, responseHasSensitiveInfo = false, since = "4.21.0")
public class RegisterExtensionCmd extends BaseCmd {

    @Inject
    ExternalAgentManager _externalMgr;

    public static final String APINAME = "registerExtension";

    /////////////////////////////////////////////////////
    //////////////// API parameters /////////////////////
    /////////////////////////////////////////////////////

    @Parameter(name = ApiConstants.NAME, type = CommandType.STRING, required = true,
            description = "Name of the extension")
    private String name;

    @Parameter(name = ApiConstants.TYPE, type = CommandType.STRING, required = true,
            description = "Type of the extension. Supported value is orchestrate")
    private String type;

    @Parameter(name = ApiConstants.POD_ID, type = CommandType.UUID, required = true,
            entityType = PodResponse.class, description = "the Pod ID for the extension to register in")
    private Long podId;

    @Parameter(name = ApiConstants.EXTERNAL_DETAILS, type = CommandType.MAP,
            description = "Details in key/value pairs using format externaldetails[i].keyname=keyvalue. Example: externaldetails[0].endpoint.url=urlvalue")
    protected Map externalDetails;

    /////////////////////////////////////////////////////
    /////////////////// Accessors ///////////////////////
    /////////////////////////////////////////////////////

    public String getName() {
        return name;
    }

    public String getType() {
        return type;
    }

    public Long getPodId() {
        return podId;
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

    public Map getDetails() {
        return externalDetails;
    }

    /////////////////////////////////////////////////////
    /////////////// API Implementation///////////////////
    /////////////////////////////////////////////////////

    @Override
    public void execute() throws ResourceUnavailableException, InsufficientCapacityException, ServerApiException, ConcurrentOperationException, ResourceAllocationException, NetworkRuleConflictException {
        Extension externalOrchestrator = _externalMgr.registerExternalOrchestrator(this);
        ExtensionResponse response = new ExtensionResponse(name, type, podId, externalOrchestrator.getUuid(), getExternalDetails());
        response.setResponseName(getCommandName());
        response.setObjectName(ApiConstants.EXTENSION);
        setResponseObject(response);
    }

    @Override
    public long getEntityOwnerId() {
        return Account.ACCOUNT_ID_SYSTEM;
    }
}
