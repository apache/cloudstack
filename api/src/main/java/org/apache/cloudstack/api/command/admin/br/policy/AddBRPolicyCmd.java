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

package org.apache.cloudstack.api.command.admin.br.policy;

import com.cloud.exception.ConcurrentOperationException;
import com.cloud.exception.InsufficientCapacityException;
import com.cloud.exception.InvalidParameterValueException;
import com.cloud.exception.NetworkRuleConflictException;
import com.cloud.exception.ResourceAllocationException;
import com.cloud.exception.ResourceUnavailableException;
import com.cloud.utils.exception.CloudRuntimeException;
import org.apache.cloudstack.acl.RoleType;
import org.apache.cloudstack.api.APICommand;
import org.apache.cloudstack.api.ApiConstants;
import org.apache.cloudstack.api.ApiErrorCode;
import org.apache.cloudstack.api.BaseCmd;
import org.apache.cloudstack.api.Parameter;
import org.apache.cloudstack.api.ServerApiException;
import org.apache.cloudstack.context.CallContext;
import org.apache.cloudstack.br.BRManager;
import org.apache.cloudstack.api.response.BRPolicyResponse;
import org.apache.cloudstack.framework.br.BRPolicy;

import javax.inject.Inject;

@APICommand(name = AddBRPolicyCmd.APINAME,
        description = "Adds a Backup policy",
        responseObject = BRPolicyResponse.class, since = "4.12.0",
        authorized = {RoleType.Admin})
public class AddBRPolicyCmd extends BaseCmd {

    public static final String APINAME = "addBRPolicy";

    @Inject
    BRManager brManager;

    @Parameter(name = ApiConstants.NAME, type = CommandType.STRING, required = true, description = "the name of the policy")
    private String policyName;

    @Parameter(name = ApiConstants.BR_POLICY_ID,
            type = CommandType.STRING,
            required = true,
            description = "Backup Recovery Provider ID")
    private String policyId;

    @Parameter(name = ApiConstants.BR_PROVIDER_ID,
            type = CommandType.STRING,
            required = true,
            description = "Backup Recovery Provider ID")
    private String providerId;

    public String getPolicyName() {
        return policyName;
    }

    public String getPolicyId() {
        return policyId;
    }

    public String getProviderId() {
        return providerId;
    }

    @Override
    public void execute() throws ResourceUnavailableException, InsufficientCapacityException, ServerApiException, ConcurrentOperationException, ResourceAllocationException, NetworkRuleConflictException {
        try {
            BRPolicy policy = brManager.addBRPolicy(policyId, policyName, providerId);
            if (policy != null) {
                BRPolicyResponse response = brManager.createBackupPolicyResponse(policy);
                response.setObjectName("brpolicy");
                response.setResponseName(getCommandName());
                setResponseObject(response);
            } else {
                throw new ServerApiException(ApiErrorCode.INTERNAL_ERROR, "Failed to add a Backup policy");
            }
        } catch (InvalidParameterValueException e) {
            throw new ServerApiException(ApiErrorCode.PARAM_ERROR, e.getMessage());
        } catch (CloudRuntimeException e) {
            throw new ServerApiException(ApiErrorCode.INTERNAL_ERROR, e.getMessage());
        }
    }

    @Override
    public String getCommandName() {
        return APINAME.toLowerCase() + RESPONSE_SUFFIX;
    }

    @Override
    public long getEntityOwnerId() {
        return CallContext.current().getCallingAccount().getId();
    }
}
