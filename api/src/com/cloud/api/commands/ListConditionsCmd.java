//       Licensed to the Apache Software Foundation (ASF) under one
//       or more contributor license agreements.  See the NOTICE file
//       distributed with this work for additional information
//       regarding copyright ownership.  The ASF licenses this file
//       to you under the Apache License, Version 2.0 (the
//       "License"); you may not use this file except in compliance
//       with the License.  You may obtain a copy of the License at
//
//         http://www.apache.org/licenses/LICENSE-2.0
//
//       Unless required by applicable law or agreed to in writing,
//       software distributed under the License is distributed on an
//       "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
//       KIND, either express or implied.  See the License for the
//       specific language governing permissions and limitations
//       under the License.

package com.cloud.api.commands;

import java.util.ArrayList;
import java.util.List;

import org.apache.log4j.Logger;

import com.cloud.api.ApiConstants;
import com.cloud.api.BaseListAccountResourcesCmd;
import com.cloud.api.IdentityMapper;
import com.cloud.api.Implementation;
import com.cloud.api.Parameter;
import com.cloud.api.response.ConditionResponse;
import com.cloud.api.response.CounterResponse;
import com.cloud.api.response.ListResponse;
import com.cloud.network.as.Condition;

@Implementation(description = "List Conditions for the specific user", responseObject = CounterResponse.class)
public class ListConditionsCmd extends BaseListAccountResourcesCmd {
    public static final Logger s_logger = Logger.getLogger(ListConditionsCmd.class.getName());
    private static final String s_name = "listconditionsresponse";

    // ///////////////////////////////////////////////////
    // ////////////// API parameters /////////////////////
    // ///////////////////////////////////////////////////

    @IdentityMapper(entityTableName = "conditions")
    @Parameter(name = ApiConstants.ID, type = CommandType.LONG, required = false, description = "ID of the Condition.")
    private Long id;

    @Parameter(name = ApiConstants.COUNTER_ID, type = CommandType.LONG, required = false, description = "Counter-id of the condition.")
    private Long counterId;

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

    @Override
    public String getCommandName() {
        return s_name;
    }

}