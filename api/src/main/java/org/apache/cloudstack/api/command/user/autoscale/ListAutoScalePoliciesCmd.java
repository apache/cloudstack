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


import org.apache.cloudstack.api.APICommand;
import org.apache.cloudstack.api.ApiConstants;
import org.apache.cloudstack.api.BaseListProjectAndAccountResourcesCmd;
import org.apache.cloudstack.api.Parameter;
import org.apache.cloudstack.api.response.AutoScalePolicyResponse;
import org.apache.cloudstack.api.response.AutoScaleVmGroupResponse;
import org.apache.cloudstack.api.response.ConditionResponse;
import org.apache.cloudstack.api.response.ListResponse;

import com.cloud.network.as.AutoScalePolicy;

@APICommand(name = "listAutoScalePolicies", description = "Lists autoscale policies.", responseObject = AutoScalePolicyResponse.class, entityType = {AutoScalePolicy.class},
        requestHasSensitiveInfo = false, responseHasSensitiveInfo = false)
public class ListAutoScalePoliciesCmd extends BaseListProjectAndAccountResourcesCmd {


    // ///////////////////////////////////////////////////
    // ////////////// API parameters /////////////////////
    // ///////////////////////////////////////////////////

    @Parameter(name = ApiConstants.ID, type = CommandType.UUID, entityType = AutoScalePolicyResponse.class, description = "the ID of the autoscale policy")
    private Long id;

    @Parameter(name = ApiConstants.NAME, type = CommandType.STRING, description = "the name of the autoscale policy", since = "4.18.0")
    private String name;

    @Parameter(name = ApiConstants.CONDITION_ID, type = CommandType.UUID, entityType = ConditionResponse.class, description = "the ID of the condition of the policy")
    private Long conditionId;

    @Parameter(name = ApiConstants.ACTION,
               type = CommandType.STRING,
               description = "the action to be executed if all the conditions evaluate to true for the specified duration.")
    private String action;

    @Parameter(name = ApiConstants.VMGROUP_ID, type = CommandType.UUID, entityType = AutoScaleVmGroupResponse.class, description = "the ID of the autoscale vm group")
    private Long vmGroupId;

    // ///////////////////////////////////////////////////
    // ///////////////// Accessors ///////////////////////
    // ///////////////////////////////////////////////////

    public Long getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public Long getConditionId() {
        return conditionId;
    }

    public String getAction() {
        return action;
    }

    public Long getVmGroupId() {
        return vmGroupId;
    }

    // ///////////////////////////////////////////////////
    // ///////////// API Implementation///////////////////
    // ///////////////////////////////////////////////////

    @Override
    public void execute() {
        List<? extends AutoScalePolicy> autoScalePolicies = _autoScaleService.listAutoScalePolicies(this);
        ListResponse<AutoScalePolicyResponse> response = new ListResponse<AutoScalePolicyResponse>();
        List<AutoScalePolicyResponse> responses = new ArrayList<AutoScalePolicyResponse>();
        if (autoScalePolicies != null) {
            for (AutoScalePolicy autoScalePolicy : autoScalePolicies) {
                AutoScalePolicyResponse autoScalePolicyResponse = _responseGenerator.createAutoScalePolicyResponse(autoScalePolicy);
                autoScalePolicyResponse.setObjectName("autoscalepolicy");
                responses.add(autoScalePolicyResponse);
            }
        }
        response.setResponses(responses);
        response.setResponseName(getCommandName());
        setResponseObject(response);
    }

}
