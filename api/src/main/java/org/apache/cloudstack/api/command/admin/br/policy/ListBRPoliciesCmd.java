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
import com.cloud.exception.InvalidParameterValueException;
import com.cloud.utils.exception.CloudRuntimeException;
import org.apache.cloudstack.acl.RoleType;
import org.apache.cloudstack.api.APICommand;
import org.apache.cloudstack.api.ApiConstants;
import org.apache.cloudstack.api.ApiErrorCode;
import org.apache.cloudstack.api.BaseListCmd;
import org.apache.cloudstack.api.Parameter;
import org.apache.cloudstack.api.ServerApiException;
import org.apache.cloudstack.api.response.ListResponse;
import org.apache.cloudstack.br.BRManager;
import org.apache.cloudstack.api.response.BRPolicyResponse;
import org.apache.cloudstack.framework.br.BRPolicy;
import org.apache.commons.collections.CollectionUtils;

import javax.inject.Inject;
import java.util.ArrayList;
import java.util.List;

@APICommand(name = ListBRPoliciesCmd.APINAME,
        description = "Lists mapped Backup policies",
        responseObject = BRPolicyResponse.class, since = "4.12.0",
        authorized = {RoleType.Admin})
public class ListBRPoliciesCmd extends BaseListCmd {

    public static final String APINAME = "listBRPolicies";

    @Parameter(name = ApiConstants.BR_PROVIDER_ID,
            type = CommandType.STRING,
            required = true,
            description = "Backup Recovery Provider ID")
    private String providerId;

    public String getProviderId() {
        return providerId;
    }

    @Inject
    BRManager brManager;

    @Override
    public void execute() throws ServerApiException, ConcurrentOperationException {
        try {
            ListResponse<BRPolicyResponse> response = new ListResponse<BRPolicyResponse>();
            List<BRPolicyResponse> responses = new ArrayList<>();
            List<BRPolicy> policies = brManager.listBRPolicies(providerId);
            if (CollectionUtils.isNotEmpty(policies)) {
                for (BRPolicy policy : policies) {
                    responses.add(brManager.createBackupPolicyResponse(policy));
                }
            }
            response.setResponses(responses, responses.size());
            response.setObjectName("brpolicies");
            response.setResponseName(getCommandName());
            setResponseObject(response);
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

}
