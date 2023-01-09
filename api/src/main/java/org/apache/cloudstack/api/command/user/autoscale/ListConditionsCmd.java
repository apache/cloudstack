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

package org.apache.cloudstack.api.command.user.autoscale;

import java.util.ArrayList;
import java.util.List;

import org.apache.log4j.Logger;

import org.apache.cloudstack.api.APICommand;
import org.apache.cloudstack.api.ApiConstants;
import org.apache.cloudstack.api.BaseListProjectAndAccountResourcesCmd;
import org.apache.cloudstack.api.Parameter;
import org.apache.cloudstack.api.response.AutoScalePolicyResponse;
import org.apache.cloudstack.api.response.ConditionResponse;
import org.apache.cloudstack.api.response.CounterResponse;
import org.apache.cloudstack.api.response.ListResponse;

import com.cloud.network.as.Condition;

@APICommand(name = "listConditions", description = "List Conditions for VM auto scaling", responseObject = ConditionResponse.class,
        requestHasSensitiveInfo = false, responseHasSensitiveInfo = false)
public class ListConditionsCmd extends BaseListProjectAndAccountResourcesCmd {
    public static final Logger s_logger = Logger.getLogger(ListConditionsCmd.class.getName());

    // ///////////////////////////////////////////////////
    // ////////////// API parameters /////////////////////
    // ///////////////////////////////////////////////////

    @Parameter(name = ApiConstants.ID, type = CommandType.UUID, entityType = ConditionResponse.class, required = false, description = "ID of the Condition.")
    private Long id;

    @Parameter(name = ApiConstants.COUNTER_ID,
               type = CommandType.UUID,
               entityType = CounterResponse.class,
               required = false,
               description = "Counter-id of the condition.")
    private Long counterId;

    @Parameter(name = ApiConstants.POLICY_ID, type = CommandType.UUID, entityType = AutoScalePolicyResponse.class, description = "the ID of the policy")
    private Long policyId;

    // ///////////////////////////////////////////////////
    // ///////////// API Implementation///////////////////
    // ///////////////////////////////////////////////////

    @Override
    public void execute() {
        List<? extends Condition> conditions = null;
        conditions = _autoScaleService.listConditions(this);
        ListResponse<ConditionResponse> response = new ListResponse<ConditionResponse>();
        List<ConditionResponse> cndnResponses = new ArrayList<ConditionResponse>();
        for (Condition cndn : conditions) {
            ConditionResponse cndnResponse = _responseGenerator.createConditionResponse(cndn);
            cndnResponses.add(cndnResponse);
        }

        response.setResponses(cndnResponses);
        response.setResponseName(getCommandName());
        this.setResponseObject(response);
    }

    // /////////////////////////////////////////////////
    // ///////////////// Accessors ///////////////////////
    // ///////////////////////////////////////////////////

    public Long getId() {
        return id;
    }

    public Long getCounterId() {
        return counterId;
    }

    public Long getPolicyId() {
        return policyId;
    }

    }
